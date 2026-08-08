package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.language.AppLanguage
import com.example.ui.language.Language
import com.example.ui.viewmodel.ClubViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevDocsScreen(
    viewModel: ClubViewModel
) {
    val lang by viewModel.language.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(AppLanguage.schemaDocs(lang), fontWeight = FontWeight.Bold) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 0.dp
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.DataObject, contentDescription = null) },
                    text = { Text("Prompt 1: NoSQL Schema", fontSize = 11.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Folder, contentDescription = null) },
                    text = { Text("Prompt 2: Flutter/Compose UI", fontSize = 11.sp) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Payments, contentDescription = null) },
                    text = { Text("Prompt 3: Payment Gateway", fontSize = 11.sp) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        item {
                            Text("Firebase Firestore NoSQL JSON Schema (Kunjachaya Club)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Bilingual collections structure supporting Users, Financials, Announcements, and Complaints.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            CodeBlock(firestoreSchemaJson)
                        }
                    }
                    1 -> {
                        item {
                            Text("Mobile App Folder Architecture & State Management", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Clean Architecture folder hierarchy for Flutter/Dart & Jetpack Compose.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            CodeBlock(flutterFolderArchitecture)
                        }
                    }
                    2 -> {
                        item {
                            Text("Node.js Payment Gateway Cloud Function & Webhook", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Backend endpoint initiating dues/donations and updating user total paid on webhook trigger.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            CodeBlock(nodejsPaymentFunction)
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
fun CodeBlock(code: String) {
    Surface(
        color = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .padding(12.dp)
                .horizontalScroll(rememberScrollState())
        ) {
            Text(
                text = code,
                color = Color(0xFFD4D4D4),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}

val firestoreSchemaJson = """
{
  "collections": {
    "users": {
      "USR_101": {
        "id": "USR-101",
        "phone": "+8801712345678",
        "name": {
          "en": "Md. Rafiqul Islam",
          "bn": "মোঃ রফিকুল ইসলাম"
        },
        "dob": "1982-08-14",
        "bloodGroup": "O+",
        "profession": {
          "en": "Civil Engineer",
          "bn": "সিভিল ইঞ্জিনিয়ার"
        },
        "address": {
          "road": "Road 04",
          "block": "Block B",
          "floor": "3rd Floor",
          "holding": "Holding 42/A"
        },
        "contactNumbers": {
          "primary": "+8801712345678",
          "emergency": "+8801799887766"
        },
        "family": {
          "fatherOrSpouse": {
            "en": "Late Alhajj Nurul Islam",
            "bn": "মরহুম আলহাজ্ব নুরুল ইসলাম"
          },
          "mother": {
            "en": "Rokeya Begum",
            "bn": "রোকেয়া বেগম"
          },
          "count": 4
        },
        "membershipStatus": "Active",
        "role": "Member",
        "profilePicUrl": "https://storage.googleapis.com/kunjachaya/users/usr101.jpg",
        "nidUrl": {
          "front": "https://storage.googleapis.com/kunjachaya/nid/front101.jpg",
          "back": "https://storage.googleapis.com/kunjachaya/nid/back101.jpg"
        }
      }
    },
    "financials": {
      "FIN_501": {
        "userId": "USR-101",
        "type": "Due",
        "title": {
          "en": "Monthly Service Charge (July 2026)",
          "bn": "মাসিক সার্ভিস চার্জ (জুলাই ২০২৬)"
        },
        "amount": 1500.0,
        "monthYear": "July 2026",
        "status": "Pending",
        "createdAt": "2026-07-01T00:00:00Z"
      }
    },
    "announcements": {
      "ANN_301": {
        "title": {
          "en": "Tree Plantation Drive 2026",
          "bn": "বৃক্ষরোপণ অভিযান ২০২৬"
        },
        "category": "Urgent Notice",
        "priority": "High",
        "date": "2026-07-24"
      }
    },
    "complaints": {
      "CMP_801": {
        "userId": "USR-101",
        "category": "Water & Electricity",
        "title": "Street Light Fault",
        "status": "Resolved",
        "imageUrl": "https://storage.googleapis.com/kunjachaya/complaints/cmp801.jpg"
      }
    }
  }
}
""".trimIndent()

val flutterFolderArchitecture = """
lib/
├── main.dart
├── app_theme.dart
├── i18n/
│   ├── app_en.json
│   └── app_bn.json
├── models/
│   ├── user_model.dart
│   ├── financial_model.dart
│   ├── announcement_model.dart
│   └── complaint_model.dart
├── providers/ / viewmodels/
│   ├── auth_provider.dart
│   ├── language_provider.dart
│   ├── financials_provider.dart
│   └── complaint_provider.dart
└── ui/
    ├── screens/
    │   ├── auth_screen.dart
    │   ├── dashboard_screen.dart
    │   ├── profile_screen.dart
    │   ├── complaint_screen.dart
    │   └── financials_screen.dart
    └── widgets/
        ├── news_bulletin_ticker.dart
        └── financial_dues_card.dart
""".trimIndent()

val nodejsPaymentFunction = """
/**
 * Kunjachaya Club - PipraPay Automated Payment Gateway Integration
 * Repository: https://github.com/PipraPay/PipraPay.git
 */
const functions = require('firebase-functions');
const admin = require('firebase-admin');
const axios = require('axios');
admin.initializeApp();

const PIPRAPAY_API_KEY = process.env.PIPRAPAY_API_KEY || "piprapay_live_kunjachaya_2026";
const PIPRAPAY_BASE_URL = "https://piprapay.com/api/create-charge";

// 1. Create PipraPay Payment Checkout Charge
exports.initiatePipraPayPayment = functions.https.onRequest(async (req, res) => {
  const { userId, flatNo, amount, purpose, customerName, customerPhone } = req.body;
  const trx_id = "KCB_PIP_" + Date.now();

  try {
    // Record Pending Transaction in Firestore
    await admin.firestore().collection('financials').add({
      userId,
      flatNo,
      amount: parseFloat(amount),
      purpose: purpose || "Club Dues",
      transactionId: trx_id,
      paymentGateway: "PipraPay",
      status: 'Pending',
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });

    // Call PipraPay REST API endpoint
    const response = await axios.post(PIPRAPAY_BASE_URL, {
      api_key: PIPRAPAY_API_KEY,
      full_name: customerName || "Resident",
      phone: customerPhone,
      amount: parseFloat(amount),
      currency: "BDT",
      trx_id: trx_id,
      purpose: purpose || "Kunjachaya Maintenance Dues",
      redirect_url: "https://kunjachaya.org/payment/callback",
      webhook_url: "https://us-central1-kunjachaya.cloudfunctions.net/piprapayWebhook"
    });

    return res.status(200).json({
      success: true,
      trx_id,
      payment_url: response.data.payment_url || `https://piprapay.com/checkout/pay?trx=${"$"}{trx_id}`
    });
  } catch (error) {
    return res.status(500).json({ success: false, error: error.message });
  }
});

// 2. PipraPay Webhook Listener (Instant Automatic Payment Confirmation)
exports.piprapayWebhook = functions.https.onRequest(async (req, res) => {
  const { trx_id, status, amount, pp_trx_id, status_code } = req.body;

  if (status === 'COMPLETED' || status_code === '200') {
    const finQuery = await admin.firestore()
      .collection('financials')
      .where('transactionId', '==', trx_id)
      .limit(1)
      .get();

    if (!finQuery.empty) {
      const doc = finQuery.docs[0];
      const data = doc.data();

      // Mark Record Paid
      await doc.ref.update({
        status: 'Completed',
        paymentGateway: 'PipraPay Direct Gateway',
        piprapayRef: pp_trx_id || 'PIP-' + Date.now(),
        completedAt: admin.firestore.FieldValue.serverTimestamp()
      });

      // Update User Total Treasury Paid Balance
      if (data.userId) {
        await admin.firestore().collection('users').doc(data.userId).update({
          totalPaid: admin.firestore.FieldValue.increment(parseFloat(amount))
        });
      }
    }
  }
  return res.status(200).send({ status: 'ACKNOWLEDGED' });
});
""".trimIndent()
