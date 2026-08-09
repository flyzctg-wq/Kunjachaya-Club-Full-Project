# Kunjachaya Club — Resident Management Platform

A community management platform for **Kunjachaya Club**, built as two apps sharing one real Firebase backend: a Jetpack Compose **Android app** and a React **web app**. Covers member directory, notices, complaints, dues/payments via PipraPay, and events — governed by the club's actual written constitution rather than a generic admin-tier system.

---

## ⚠️ Status: reviewed and internally consistent, but never compiled here

Everything in this repo has been through careful manual review, cross-file consistency checks, and static syntax/brace-balance verification — this pass caught and fixed two real would-have-failed-the-build bugs (a stray brace in `FinancialsScreen.kt`, an orphaned Hilt module with the plugin never applied). **It has never been through a real compiler in this environment** — no network access here, so `gradle build` and `npm install && npm run build` have never actually been run *by this assistant*. The Gradle wrapper jar (`gradle/wrapper/gradle-wrapper.jar`) is now committed, so `./gradlew` should work out of the box on your machine — see [GRADLE_BUILD_GUIDE.md](./GRADLE_BUILD_GUIDE.md) for the debug keystore step it still needs. Before shipping, run a real build on both platforms and treat the first build as a normal first build: fix whatever it turns up.

---

## Table of Contents
- [Key Features](#key-features)
- [Governance Model (from the real constitution)](#governance-model-from-the-real-constitution)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Security Model](#security-model)
- [Documentation Index](#documentation-index)

---

## Key Features

### Accounts & Profile
- Real Firebase Auth: email/password + real SMS OTP (Android Phone Auth)
- Password reset ("Forgot Password?") on both Android and web
- Profile photo upload via Firebase Storage — supported on both Android and Web (via interactive profile photo uploader)
- New accounts always start `Pending` — no fabricated identity data, no shortcuts (ধারা-১০)

### Member Directory
- Real-time resident roster, filterable by block and search term
- Only the real national emergency hotlines (999, 16163) are shown — the app never invents club-specific staff contacts

### Notices & Announcements
- Executive Committee members with notice permission can publish; all residents read in real time

### Complaints / Service Requests
- Residents submit and track their own tickets; committee members with complaint permission resolve them
- No fake auto-replies — a resolution note only appears once a real person writes one

### Dues & Payments
- Real PipraPay Cloud Function checkout — no fake gateways, no simulated "processing" animations
- A payment is only ever marked `Completed` by the real PipraPay webhook (server-side, Admin SDK) — never by the client
- PDF receipts pull only real, stored transaction data; nothing is fabricated if a field is missing

### Events
- Real calendar view built from actual event dates in Firestore
- Per-user reminder toggle (no fake RSVP/attendee counts — that would need a feature that doesn't exist yet)

### Admin Portal (Executive Committee only)
- Publish notices, issue monthly dues to all active members, approve pending memberships (ধারা-১০), resolve complaints
- Assign committee posts and permission flags — restricted to President/General Secretary (ধারা-১৭); see [WORKFLOW.md](./WORKFLOW.md) for the full matrix
- All actions write to real Firestore documents and produce a real, attributed activity log entry

---

## Governance Model (from the real constitution)

Roles are **not** a generic "Super Admin / Admin / Member" ladder. They come directly from `kunjachaya-constitution-revised.docx`:

**Member classes (ধারা-৬):** New (pending) → General → Founding → Lifetime → Donor → Advisory

**Executive Committee — 15 seats (ধারা-১৪):** President, 2× Vice President, General Secretary, Assistant General Secretary, Treasurer, Organizing Secretary, Social Welfare Secretary, Literature & Culture Secretary, Publicity Secretary, Sports Secretary, Women's Affairs Secretary, 3× Executive Member

President and General Secretary carry the constitution's broadest authority (ধারা-১৭); other committee posts carry specific permission flags (`canManageNotices`, `canManageComplaints`, `canManageMembers`, `canManageFinancials`, `canDeleteItems`) that an existing committee member assigns. This exact model is implemented identically in both apps — `UserEntity.kt` (Android) and `roles.js` (web) — because they share one Firestore schema.

Default dues from the constitution: ৳200 one-time admission, ৳100/month (admin-editable in the Admin Portal).

---

## Architecture

| Layer | Android | Web |
|---|---|---|
| UI | Jetpack Compose, Material 3 | React + Tailwind, Stitch design system |
| State | Kotlin `StateFlow` | React hooks |
| Local cache | Room (offline-first) | — (Firestore's own offline cache) |
| Backend | **Firebase project `kunjachaya-club`** — shared by both apps |
| Auth | Firebase Auth: email/password, real SMS OTP (Phone Auth) | Firebase Auth: email/password |
| Data | Firestore: `users`, `financials`, `announcements`, `complaints`, `Events`, `ActivityLogs`, `orders` | same collections, same field names |
| Payments | Real PipraPay Cloud Function (`functions/src/index.ts`) — no fallback/fake success path | same function |
| Security | Firestore Security Rules (`firestore.rules`) enforce the constitution's permission model server-side | same rules, same backend |

Both apps read and write the **exact same documents** — a resident registered on Android sees the same Pending status, same dues, same notices on web, and vice versa.

---

## Project Structure

```
.
├── README.md / WORKFLOW.md / BUILD.md   # This documentation
├── GRADLE_BUILD_GUIDE.md / WEB_BUILD_GUIDE.md  # Focused per-platform build steps
├── firebase.json / .firebaserc / vercel.json   # Firebase + Vercel project config
├── firestore.rules                      # Server-side permission enforcement (see Security Model)
├── firestore.indexes.json               # Composite indexes required by app queries
├── functions/
│   └── src/index.ts                     # createPipraPayCheckout + webhook handlers (real gateway, no fake fallback)
├── scripts/
│   ├── SUPER_ADMIN_SETUP.md             # First-officer bootstrap: manual Firestore steps
│   └── (see web/set-super-admin.cjs)    # Same bootstrap, automated — reads credentials from env vars, never hardcoded
├── app/                                 # Android app
│   ├── google-services.json             # Real Firebase config (needs your real API key — see BUILD.md)
│   └── src/main/java/com/example/
│       ├── data/model/UserEntity.kt     # MemberClass + CommitteePost (source of truth for roles)
│       ├── data/repository/             # FirestoreRepository, PaymentRepository, ClubRepository
│       ├── ui/screens/, ui/viewmodel/   # Screens + ClubViewModel
│       └── util/FirebaseManager.kt      # Exposes isBackendConnected — never silently pretends
└── web/                                 # Web app (React + Vite)
    ├── set-super-admin.cjs              # First-officer bootstrap script (credentials via env vars only)
    └── src/
        ├── roles.js                     # JS mirror of UserEntity.kt's role logic
        ├── firebase.js                  # Real Firebase config
        ├── services/                    # firestoreService.js, authService.js
        └── views/                       # One file per screen, styled per the Stitch design system
```

---

## Getting Started

See [BUILD.md](./BUILD.md) for full setup, or the focused [GRADLE_BUILD_GUIDE.md](./GRADLE_BUILD_GUIDE.md) / [WEB_BUILD_GUIDE.md](./WEB_BUILD_GUIDE.md). Short version:

1. **Get your real Android API key** into `app/google-services.json` (currently has a placeholder marked `REPLACE_WITH_REAL_ANDROID_API_KEY`).
2. **Set real PipraPay merchant credentials** for the Cloud Function — without them, `createPipraPayCheckout` deliberately fails with a clear error rather than faking a checkout link.
3. **Deploy Firestore rules/indexes/functions**: `firebase deploy --only firestore:rules,firestore:indexes,functions`
4. **Build Android**: generate a debug keystore (`GRADLE_BUILD_GUIDE.md` §2), then `./gradlew :app:assembleDebug`
5. **Build web**: `cd web && npm install && npm run build`
6. **Bootstrap your first President/GS account**: see `scripts/SUPER_ADMIN_SETUP.md` — there's no seeded admin, on purpose.

---

## Security Model

App-level permission checks (what the UI shows/hides) are **not** the real security boundary — `firestore.rules` is. It enforces, server-side:
- Residents can read the directory and their own records, never someone else's financials or complaints (unless they hold real committee permission)
- No client, however modified, can grant itself a committee post, approve its own membership, or mark a payment `Completed`
- Only the Admin SDK (Cloud Function, driven by a real PipraPay webhook) can complete a payment

User documents are keyed by the real Firebase Auth UID (not an app-generated string), which is what makes rule-based self-identification (`request.auth.uid`) actually work.

---

## Documentation Index
- 📖 [**BUILD.md**](./BUILD.md) — Full setup: Firebase project, credentials, Cloud Functions, both app builds
- 🔧 [**GRADLE_BUILD_GUIDE.md**](./GRADLE_BUILD_GUIDE.md) — Focused Android/Gradle build steps: wrapper bootstrap, debug keystore, task reference, troubleshooting
- 🌐 [**WEB_BUILD_GUIDE.md**](./WEB_BUILD_GUIDE.md) — Focused web build steps: install, build, deploy (Vercel/Firebase), troubleshooting
- 📊 [**WORKFLOW.md**](./WORKFLOW.md) — Role matrix, user flows, and the real payment sequence
