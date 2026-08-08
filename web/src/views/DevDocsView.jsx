import React, { useState } from 'react';
import Icon from '../components/Icon';

export default function DevDocsView() {
  const [copiedSection, setCopiedSection] = useState('');

  const copyToClipboard = (text, section) => {
    navigator.clipboard.writeText(text);
    setCopiedSection(section);
    setTimeout(() => setCopiedSection(''), 2500);
  };

  const firestoreSchema = `{
  "users": {
    "{uid}": {  // real Firebase Auth UID — NOT an app-generated string
      "id": "<same as {uid}>",
      "phone": "+8801XXXXXXXXX",
      "nameEn": "", "nameBn": "",
      "holding": "", "road": "", "block": "",
      "primaryContact": "",
      "membershipStatus": "Pending", // "Active" | "Pending" | "Suspended" | "Cancelled"
      "memberClass": "NEW", // NEW | GENERAL | FOUNDING | LIFETIME | DONOR | ADVISORY (ধারা-৬)
      "committeePost": "", // one of the 15 posts in ধারা-১৪, or "" if none
      "canManageNotices": false,
      "canManageComplaints": false,
      "canManageMembers": false,
      "canManageFinancials": false,
      "canDeleteItems": false,
      "joinedDate": "YYYY-MM-DD"
    }
  },
  "financials": {
    "{docId}": {
      "userId": "<real Firebase Auth UID>",
      "titleEn": "Monthly Membership Dues - August 2026",
      "amount": 100,
      "type": "Due", // "Due" | "Paid" | "Donation" | "Adjustment"
      "monthYear": "August 2026",
      "date": "YYYY-MM-DD",
      "status": "Pending", // "Pending" | "Completed" | "Failed"
      "paymentGateway": "PipraPay",
      "transactionId": ""
    }
  },
  "announcements": {
    "{docId}": {
      "titleEn": "", "titleBn": "",
      "descriptionEn": "", "descriptionBn": "",
      "categoryEn": "Urgent Notice",
      "priority": "High", // "High" | "Medium" | "Low"
      "date": "YYYY-MM-DD"
    }
  },
  "complaints": {
    "{docId}": {
      "userId": "<real Firebase Auth UID>",
      "holdingNo": "",
      "titleEn": "", "descriptionEn": "",
      "categoryEn": "Plumbing",
      "status": "Pending", // "Pending" | "Under Review" | "Resolved"
      "adminNoteEn": "",
      "createdAt": "ISO-8601", "updatedAt": "ISO-8601"
    }
  },
  "Events": {
    "{docId}": {
      "titleEn": "", "descriptionEn": "",
      "locationEn": "", "date": "YYYY-MM-DD", "time": "",
      "eventType": "", "amount": 0, "isReminderSet": false
    }
  },
  "orders": {
    "{orderId}": {
      "orderId": "", "userId": "", "recordId": "",
      "amount": 0, "status": "pending", // "pending" | "completed" | "failed"
      "transactionId": "", "paymentGateway": "PipraPay"
    }
  }
}`;

  const cloudFunctionCode = `// This is the ACTUAL deployed source — see functions/src/index.ts.
// createPipraPayCheckout throws a clear "failed-precondition" error if
// PIPRAPAY_API_KEY / PIPRAPAY_BASE_URL aren't set to real merchant
// credentials. There is no fake/sandbox fallback: a misconfigured gateway
// fails loudly instead of returning a fabricated checkout link.
//
// Deploy config (real credentials required, no default):
//   firebase functions:config:set piprapay.api_key="<real key>" \\
//     piprapay.base_url="<real PipraPay API base URL>"
//
// Exposed functions:
//   createPipraPayCheckout  - callable, creates a real order + checkout URL
//   piprapayWebhook         - HTTP, PipraPay's server-to-server IPN callback
//   piprapayRedirect        - HTTP, browser return URL after checkout
//   piprapayCancel          - HTTP, browser return URL on cancel
//
// Full source: functions/src/index.ts`;

  const iosCapacitorGuide = `# Deployment Guide

## 1. Web Hosting — Vercel (recommended)
This repo is a monorepo; vercel.json at the root builds only web/.
\`\`\`bash
npm install -g vercel
vercel --prod
\`\`\`
Or connect the GitHub repo in the Vercel dashboard — vercel.json
handles the build command, output directory, and SPA rewrites.
No environment variables needed: web/src/firebase.js already has
the real (non-secret) Firebase web config.

## 2. Web Hosting — Firebase Hosting (alternative)
\`\`\`bash
npm --prefix web run build
firebase deploy --only hosting,firestore:rules,firestore:indexes,functions
\`\`\`

## 3. iOS Native App via Capacitor Wrapper (optional, not yet set up in this repo)
To wrap the web app into a native iOS Xcode project:

\`\`\`bash
cd web
npm install @capacitor/core @capacitor/cli @capacitor/ios
npx cap init KunjachayaClub com.aistudio.kunjachayaclub.app --web-dir dist
npm run build
npx cap add ios
npx cap open ios
\`\`\`

Then in Xcode, select your Signing Team and run on a simulator or device.`;

  return (
    <>
      <div className="bg-surface-container-lowest p-6 rounded-xl border border-outline-variant/30 shadow-[0_2px_8px_rgba(0,0,0,0.04)] mb-6">
        <span className="px-2 py-0.5 bg-tertiary-container text-on-tertiary-container text-[10px] uppercase font-bold rounded tracking-wider">
          Architecture & Integration
        </span>
        <h2 className="font-title-lg text-title-lg text-on-surface mt-2">
          Developer Documentation & API Reference
        </h2>
        <p className="text-xs text-on-surface-variant mt-1">
          Real Firestore schema, the deployed PipraPay Cloud Function, and hosting/deployment setup
        </p>
      </div>

      <div className="space-y-6">

        <div className="bg-inverse-surface text-inverse-on-surface p-6 rounded-xl space-y-3 font-mono text-xs">
          <div className="flex items-center justify-between pb-2 border-b border-white/10">
            <div className="flex items-center gap-2 font-bold text-primary-fixed">
              <Icon name="database" className="text-[16px]" />
              <span>Firebase Firestore NoSQL Schema</span>
            </div>
            <button
              onClick={() => copyToClipboard(firestoreSchema, 'schema')}
              className="p-1.5 hover:bg-white/10 text-white/60 hover:text-white rounded transition flex items-center gap-1"
            >
              <Icon name={copiedSection === 'schema' ? 'check' : 'content_copy'} className="text-[14px]" />
              <span>{copiedSection === 'schema' ? 'Copied' : 'Copy'}</span>
            </button>
          </div>
          <pre className="overflow-x-auto text-white/70">{firestoreSchema}</pre>
        </div>

        <div className="bg-inverse-surface text-inverse-on-surface p-6 rounded-xl space-y-3 font-mono text-xs">
          <div className="flex items-center justify-between pb-2 border-b border-white/10">
            <div className="flex items-center gap-2 font-bold text-primary-fixed">
              <Icon name="dns" className="text-[16px]" />
              <span>PipraPay Cloud Function (functions/src/index.ts)</span>
            </div>
            <button
              onClick={() => copyToClipboard(cloudFunctionCode, 'function')}
              className="p-1.5 hover:bg-white/10 text-white/60 hover:text-white rounded transition flex items-center gap-1"
            >
              <Icon name={copiedSection === 'function' ? 'check' : 'content_copy'} className="text-[14px]" />
              <span>{copiedSection === 'function' ? 'Copied' : 'Copy'}</span>
            </button>
          </div>
          <pre className="overflow-x-auto text-white/70">{cloudFunctionCode}</pre>
        </div>

        <div className="bg-inverse-surface text-inverse-on-surface p-6 rounded-xl space-y-3 font-mono text-xs">
          <div className="flex items-center justify-between pb-2 border-b border-white/10">
            <div className="flex items-center gap-2 font-bold text-primary-fixed">
              <Icon name="rocket_launch" className="text-[16px]" />
              <span>Deployment Setup</span>
            </div>
            <button
              onClick={() => copyToClipboard(iosCapacitorGuide, 'ios')}
              className="p-1.5 hover:bg-white/10 text-white/60 hover:text-white rounded transition flex items-center gap-1"
            >
              <Icon name={copiedSection === 'ios' ? 'check' : 'content_copy'} className="text-[14px]" />
              <span>{copiedSection === 'ios' ? 'Copied' : 'Copy'}</span>
            </button>
          </div>
          <pre className="overflow-x-auto text-white/70">{iosCapacitorGuide}</pre>
        </div>

      </div>
    </>
  );
}
