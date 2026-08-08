package com.example.data.repository

import android.app.Activity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/**
 * Repository for Firebase Auth providing phone-based OTP authentication and user session management
 * for Kunjachaya Club.
 */
class FirebaseAuthRepository {

    private val auth: FirebaseAuth?
        get() = com.example.util.FirebaseManager.auth ?: run {
            try {
                FirebaseAuth.getInstance()
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    /**
     * Get currently authenticated Firebase User.
     */
    val currentUser: FirebaseUser?
        get() = auth?.currentUser

    /**
     * Real-time stream of the authentication state (returns FirebaseUser or null on logout).
     */
    fun getAuthStateStream(): Flow<FirebaseUser?> {
        val firebaseAuth = auth ?: return emptyFlow()
        return callbackFlow {
            val listener = FirebaseAuth.AuthStateListener { fa ->
                trySend(fa.currentUser)
            }
            firebaseAuth.addAuthStateListener(listener)
            awaitClose { firebaseAuth.removeAuthStateListener(listener) }
        }
    }

    /**
     * Send phone OTP code to the given phone number using Firebase PhoneAuthOptions.
     */
    fun sendPhoneOtp(
        activity: Activity,
        phoneNumber: String,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks,
        forceResendingToken: PhoneAuthProvider.ForceResendingToken? = null
    ) {
        val firebaseAuth = auth ?: return
        val optionsBuilder = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)

        if (forceResendingToken != null) {
            optionsBuilder.setForceResendingToken(forceResendingToken)
        }

        PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
    }

    /**
     * Sign in using the verification ID and the 6-digit OTP code received via SMS.
     */
    suspend fun signInWithOtp(verificationId: String, code: String): Result<FirebaseUser?> = runCatching {
        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        signInWithCredential(credential).getOrThrow()
    }

    /**
     * Sign in directly with PhoneAuthCredential (e.g. from instant verification).
     */
    suspend fun signInWithCredential(credential: PhoneAuthCredential): Result<FirebaseUser?> = runCatching {
        val firebaseAuth = auth ?: throw IllegalStateException("FirebaseAuth not initialized")
        val authResult = firebaseAuth.signInWithCredential(credential).await()
        authResult.user
    }

    /**
     * Sign out the current user session.
     */
    fun signOut() {
        auth?.signOut()
    }
}
