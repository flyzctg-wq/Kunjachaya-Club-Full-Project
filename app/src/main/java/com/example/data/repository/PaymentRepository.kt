package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class OrderStatus(
    val orderId: String = "",
    val userId: String = "",
    val recordId: String = "",
    val amount: Double = 0.0,
    val title: String = "",
    val status: String = "pending", // "pending", "completed", "failed", "cancelled"
    val transactionId: String = "",
    val paymentGateway: String = "PipraPay"
)

class PaymentRepository {
    private val firestore: FirebaseFirestore?
        get() = try {
            com.example.util.FirebaseManager.firestore ?: FirebaseFirestore.getInstance()
        } catch (e: Throwable) {
            null
        }

    private val functions: FirebaseFunctions?
        get() = try {
            FirebaseFunctions.getInstance()
        } catch (e: Throwable) {
            null
        }
    companion object {
        private const val TAG = "PaymentRepository"
    }

    /**
     * Launch PipraPay Checkout flow via Firebase Cloud Function and Chrome Custom Tab.
     * No local fallback: if the real Cloud Function can't produce a real
     * checkout URL (gateway not configured, network failure, etc.), this
     * fails loudly instead of opening a fabricated sandbox link that can't
     * actually take payment.
     */
    suspend fun launchPipraPayCheckout(
        context: Context,
        amount: Double,
        userId: String,
        recordId: String,
        title: String,
        customerPhone: String = "",
        customerEmail: String = "",
        onOrderIdCreated: (String) -> Unit = {}
    ): Result<String> = runCatching {
        val data = hashMapOf(
            "amount" to amount,
            "userId" to userId,
            "recordId" to recordId,
            "title" to title,
            "customerPhone" to customerPhone,
            "customerEmail" to customerEmail
        )

        val result = functions
            ?.getHttpsCallable("createPipraPayCheckout")
            ?.call(data)
            ?.await()
            ?: throw IllegalStateException("Firebase Functions is not available")

        @Suppress("UNCHECKED_CAST")
        val resData = result.data as? Map<String, Any>
        val checkoutUrl = resData?.get("checkoutUrl") as? String
        val orderId = resData?.get("orderId") as? String

        if (orderId == null || checkoutUrl == null) {
            throw IllegalStateException("PipraPay did not return a valid checkout session")
        }

        onOrderIdCreated(orderId)

        // Launch in Chrome Custom Tab
        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()

        customTabsIntent.launchUrl(context, Uri.parse(checkoutUrl))
        orderId
    }

    /**
     * Listen in real-time to Firestore order status changes.
     */
    fun observeOrderStatus(orderId: String): Flow<OrderStatus> = callbackFlow {
        val fs = firestore
        if (fs == null) {
            trySend(OrderStatus(orderId = orderId, status = "pending"))
            close()
            return@callbackFlow
        }
        val docRef = fs.collection("orders").document(orderId)
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error observing order status: ${error.message}")
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val order = OrderStatus(
                    orderId = snapshot.getString("orderId") ?: orderId,
                    userId = snapshot.getString("userId") ?: "",
                    recordId = snapshot.getString("recordId") ?: "",
                    amount = snapshot.getDouble("amount") ?: 0.0,
                    title = snapshot.getString("title") ?: "",
                    status = snapshot.getString("status") ?: "pending",
                    transactionId = snapshot.getString("transactionId") ?: "",
                    paymentGateway = snapshot.getString("paymentGateway") ?: "PipraPay"
                )
                trySend(order)
            }
        }
        awaitClose { listener.remove() }
    }

}
