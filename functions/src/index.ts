import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import axios from "axios";

admin.initializeApp();
const db = admin.firestore();

// PipraPay Configuration — these MUST be set as real Firebase Functions
// config/secrets before checkout works. There is no usable default: a
// missing or placeholder key means the payment gateway genuinely isn't
// configured yet, and the function says so rather than pretending to
// succeed with a fake checkout link.
const PIPRAPAY_API_KEY = process.env.PIPRAPAY_API_KEY || "";
const PIPRAPAY_BASE_URL = process.env.PIPRAPAY_BASE_URL || "";

function isRealPipraPayConfig(): boolean {
  if (!PIPRAPAY_API_KEY || !PIPRAPAY_BASE_URL) return false;
  const placeholderMarkers = ["sandbox_piprapay_key", "your_api_key", "placeholder", "xxxx"];
  return !placeholderMarkers.some((m) => PIPRAPAY_API_KEY.toLowerCase().includes(m));
}

/**
 * Callable Cloud Function: Create PipraPay Checkout
 */
export const createPipraPayCheckout = functions.https.onCall(async (data, context) => {
  const { amount, title, userId, recordId, customerPhone, customerEmail } = data;

  if (!amount || !userId) {
    throw new functions.https.HttpsError("invalid-argument", "Amount and userId are required.");
  }

  if (!isRealPipraPayConfig()) {
    // Do NOT fabricate an order or a checkout link. A resident tapping "Pay"
    // should see a clear "payment isn't set up yet" message, never a fake
    // checkout screen that can't actually take their money (or worse, one
    // that lets the app mark a real due as paid with nothing collected).
    throw new functions.https.HttpsError(
      "failed-precondition",
      "PipraPay isn't configured yet. Set PIPRAPAY_API_KEY and PIPRAPAY_BASE_URL " +
      "to your real merchant credentials (firebase functions:config or a deployed " +
      "environment variable) before enabling online payments."
    );
  }

  const orderId = `ORD-${Date.now()}-${Math.floor(Math.random() * 1000)}`;

  // Save pending order to Firestore
  await db.collection("orders").doc(orderId).set({
    orderId,
    userId,
    recordId: recordId || "",
    amount: Number(amount),
    title: title || "Club Dues Payment",
    status: "pending",
    customerPhone: customerPhone || "",
    customerEmail: customerEmail || "",
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  });

  const redirectUrl = `https://us-central1-${process.env.GCP_PROJECT || "kunjachaya-club"}.cloudfunctions.net/piprapayRedirect?orderId=${orderId}`;
  const cancelUrl = `https://us-central1-${process.env.GCP_PROJECT || "kunjachaya-club"}.cloudfunctions.net/piprapayCancel?orderId=${orderId}`;
  const webhookUrl = `https://us-central1-${process.env.GCP_PROJECT || "kunjachaya-club"}.cloudfunctions.net/piprapayWebhook`;

  try {
    const response = await axios.post(`${PIPRAPAY_BASE_URL}/api/create-charge`, {
      api_key: PIPRAPAY_API_KEY,
      amount: Number(amount),
      order_id: orderId,
      full_name: customerEmail || "Club Member",
      email: customerEmail || "",
      phone: customerPhone || "",
      redirect_url: redirectUrl,
      cancel_url: cancelUrl,
      webhook_url: webhookUrl
    });

    if (response.data && response.data.checkout_url) {
      return {
        success: true,
        orderId,
        checkoutUrl: response.data.checkout_url
      };
    }

    // The real API responded but didn't hand back a usable checkout URL —
    // that's a genuine failure, not a case for a fabricated fallback link.
    await db.collection("orders").doc(orderId).update({ status: "failed" });
    throw new functions.https.HttpsError(
      "internal",
      "PipraPay did not return a checkout URL. Please try again shortly."
    );
  } catch (error: any) {
    functions.logger.error("PipraPay charge creation error:", error?.message || error);
    await db.collection("orders").doc(orderId).update({ status: "failed" }).catch(() => {});
    if (error instanceof functions.https.HttpsError) throw error;
    throw new functions.https.HttpsError(
      "unavailable",
      "Could not reach the PipraPay payment gateway. Please try again shortly."
    );
  }
});

/**
 * HTTPS Webhook: PipraPay Server-to-Server Payment Notification
 */
export const piprapayWebhook = functions.https.onRequest(async (req, res) => {
  const { order_id, status, p_status, transaction_id, payment_gateway } = req.body || {};
  const orderId = order_id || req.query.orderId;

  if (!orderId) {
    res.status(400).send("Missing orderId");
    return;
  }

  try {
    const isSuccess = status === "completed" || p_status === "completed" || status === "success";

    if (isSuccess) {
      const orderRef = db.collection("orders").doc(orderId as string);
      const orderDoc = await orderRef.get();

      if (orderDoc.exists) {
        const orderData = orderDoc.data();
        const recordId = orderData?.recordId;

        await orderRef.update({
          status: "completed",
          transactionId: transaction_id || `PP-${Date.now()}`,
          paymentGateway: payment_gateway || "PipraPay",
          updatedAt: admin.firestore.FieldValue.serverTimestamp()
        });

        // Update financial record in Firestore if associated
        if (recordId) {
          await db.collection("financials").doc(recordId).update({
            status: "Completed",
            transactionId: transaction_id || `PP-${Date.now()}`,
            paymentGateway: payment_gateway || "PipraPay",
            paymentDate: new Date().toISOString()
          });
        }
      }
    } else {
      await db.collection("orders").doc(orderId as string).update({
        status: "failed",
        updatedAt: admin.firestore.FieldValue.serverTimestamp()
      });
    }

    res.status(200).send({ status: "success" });
  } catch (err: any) {
    functions.logger.error("PipraPay webhook error:", err);
    res.status(500).send({ error: err.message });
  }
});

/**
 * HTTPS Redirect Handler: Redirects user back to Android App via Deep Link
 */
export const piprapayRedirect = functions.https.onRequest(async (req, res) => {
  const orderId = req.query.orderId || req.body?.order_id;
  const deepLink = `kunjachayaclub://payment-result?orderId=${orderId}&status=completed`;

  res.send(`
    <!DOCTYPE html>
    <html>
      <head>
        <title>Payment Successful</title>
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <style>
          body { font-family: system-ui, sans-serif; text-align: center; padding: 40px 20px; background: #f4f6f8; }
          .card { background: white; border-radius: 16px; padding: 32px; max-width: 400px; margin: auto; box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
          .btn { display: inline-block; background: #0d9488; color: white; padding: 12px 24px; border-radius: 8px; text-decoration: none; font-weight: bold; margin-top: 16px; }
        </style>
      </head>
      <body>
        <div class="card">
          <h2 style="color: #0d9488;">✔ Payment Successful!</h2>
          <p>Order ID: <strong>${orderId}</strong></p>
          <p>Redirecting back to Kunjachaya Club App...</p>
          <a class="btn" href="${deepLink}">Return to App</a>
        </div>
        <script>
          window.location.href = "${deepLink}";
        </script>
      </body>
    </html>
  `);
});

/**
 * HTTPS Cancel Handler: Redirects user back on cancellation
 */
export const piprapayCancel = functions.https.onRequest(async (req, res) => {
  const orderId = req.query.orderId || req.body?.order_id;
  const deepLink = `kunjachayaclub://payment-result?orderId=${orderId}&status=cancelled`;

  res.send(`
    <!DOCTYPE html>
    <html>
      <head>
        <title>Payment Cancelled</title>
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <style>
          body { font-family: system-ui, sans-serif; text-align: center; padding: 40px 20px; background: #f4f6f8; }
          .card { background: white; border-radius: 16px; padding: 32px; max-width: 400px; margin: auto; box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
          .btn { display: inline-block; background: #dc2626; color: white; padding: 12px 24px; border-radius: 8px; text-decoration: none; font-weight: bold; margin-top: 16px; }
        </style>
      </head>
      <body>
        <div class="card">
          <h2 style="color: #dc2626;">✖ Payment Cancelled</h2>
          <p>Order ID: <strong>${orderId}</strong></p>
          <a class="btn" href="${deepLink}">Return to App</a>
        </div>
        <script>
          window.location.href = "${deepLink}";
        </script>
      </body>
    </html>
  `);
});
