# Kunjachaya Club — Full Build & Deployment Guide

Covers both apps plus the shared Firebase backend they run on. Read [Section 0](#0-before-you-start-real-credentials-needed) first — several steps below **cannot be skipped or faked**; the app is deliberately built to fail loudly rather than pretend to work without real credentials.

---

## 0. Before you start: real credentials needed

This project was cleaned of every fake/demo/placeholder code path it originally shipped with (seeded fake residents, a one-tap "become admin" login, a fake payment gateway that faked success). As a direct consequence, **it will not pretend to work without real setup**:

| What | Where it's needed | What happens if missing |
|---|---|---|
| Real Android Firebase API key | `app/google-services.json` → `current_key` (currently `REPLACE_WITH_REAL_ANDROID_API_KEY`) | App runs local-only; `FirebaseManager.isBackendConnected` reports `false`, no crash |
| Real PipraPay merchant API key + base URL | Cloud Functions config: `PIPRAPAY_API_KEY`, `PIPRAPAY_BASE_URL` | `createPipraPayCheckout` throws a clear `failed-precondition` error — no fake checkout link is ever generated |
| Firebase project with Auth + Firestore + Functions + Hosting enabled | Firebase Console | Nothing works until this exists |

The web Firebase config (`web/src/firebase.js`) is already filled in with real values for the `kunjachaya-club` project — that's not a secret, it's how any Firebase web client identifies itself; access control comes from the security rules, not from hiding this file.

---

## 1. Firebase Project Setup

1. In the [Firebase Console](https://console.firebase.google.com), confirm the project `kunjachaya-club` exists (project number `668738359171`).
2. Enable: **Authentication** (Email/Password + Phone providers), **Firestore Database**, **Cloud Functions**, **Hosting**, **Storage**.
3. Under Project Settings → your Android app, copy the real `current_key` (API key) into `app/google-services.json`, replacing `REPLACE_WITH_REAL_ANDROID_API_KEY`. Easiest path: download the real `google-services.json` from the console and drop it into `app/` directly.
4. Install the CLI if you don't have it: `npm install -g firebase-tools`, then `firebase login`.
5. From the repo root: `firebase use kunjachaya-club` (the `.firebaserc` alias is already set up).

---

## 2. Deploy Firestore Rules & Indexes

`firestore.rules` is what actually enforces the constitution's permission model — the app UI alone is not the security boundary. Deploy it before anyone uses either app against this project:

```bash
firebase deploy --only firestore:rules,firestore:indexes
```

Read `firestore.rules` before deploying — it documents, in comments, exactly what each rule enforces and why (e.g., why user documents must be keyed by the real Firebase Auth UID, why clients can never write `status: "Completed"` on a payment).

---

## 3. Cloud Functions (PipraPay integration)

```bash
cd functions
npm install
```

Set your **real** PipraPay merchant credentials (never commit these as literals in source):

```bash
firebase functions:config:set piprapay.api_key="<your real PipraPay API key>" \
  piprapay.base_url="<your real PipraPay API base URL>"
```

Or, if using 2nd-gen functions with environment variables, set `PIPRAPAY_API_KEY` / `PIPRAPAY_BASE_URL` in your deployment environment. `functions/src/index.ts` treats a missing or placeholder-looking key (containing `sandbox_piprapay_key`, `your_api_key`, `placeholder`, etc.) as "not configured" and rejects checkout attempts with a clear error — it will never fabricate a checkout URL.

Deploy:
```bash
npm run build
firebase deploy --only functions
```

You'll also need to register a real PipraPay webhook pointing at the deployed `piprapayWebhook` HTTPS function URL, per PipraPay's own merchant dashboard setup — that's what allows a payment to actually be marked `Completed`.

---

## 4. Android Build

**For the full step-by-step (wrapper bootstrap, debug keystore generation, task reference, troubleshooting), see [GRADLE_BUILD_GUIDE.md](./GRADLE_BUILD_GUIDE.md).** Quick summary below.

### Prerequisites
| Dependency | Version |
|---|---|
| JDK | 17+ |
| Android SDK | `compileSdk = 34`, `minSdk = 24`, `targetSdk = 34` |
| Kotlin | 1.9.x |
| Gradle | 8.x |

### Build
```bash
./gradlew :app:assembleDebug
```

**First time only:** this repo's Gradle wrapper is missing its binary launcher (`gradle/wrapper/gradle-wrapper.jar`) — that's a compiled binary, not something safe to hand-author, and it couldn't be generated in the sandboxed environment this project was built in (no network, no local Gradle install). `gradlew` and `gradle/wrapper/gradle-wrapper.properties` (pinned to Gradle 9.1.0, which AGP 9.1.1 requires) are already in place — you just need to generate the jar once:

- **Easiest:** open the project in Android Studio. It detects the wrapper config and regenerates the missing jar automatically on sync.
- **Or, if you have Gradle installed locally** (e.g. via [SDKMAN](https://sdkman.io) or Homebrew):
  ```bash
  gradle wrapper --gradle-version 9.1.0
  ```
  This is also what CI does automatically on every run (see `.github/workflows/build.yml`) — nothing else needs to change there.

After that, `./gradlew :app:assembleDebug` works normally on any machine, every time, without needing Gradle installed globally.

Full clean rebuild:
```bash
./gradlew clean :app:assembleDebug
```

### Tests
```bash
./gradlew :app:testDebugUnitTest          # JUnit + Robolectric
./gradlew :app:verifyRoborazziDebug       # Screenshot diff against stored baselines
./gradlew :app:recordRoborazziDebug       # Re-record baselines after an intentional UI change
```

### What "offline mode" actually means now
`FirebaseManager.kt` exposes a real `isBackendConnected: StateFlow<Boolean>`. When the API key is missing or a placeholder, the app runs on local Room storage only and this flag is `false` — the UI is expected to surface that honestly (rather than silently claiming to be synced, which is what the original code did). This is not an error state; it's a legitimate "not configured yet" state.

---

## 5. Web Build

**For the full step-by-step (config explanation, static verification, deploy options, troubleshooting), see [WEB_BUILD_GUIDE.md](./WEB_BUILD_GUIDE.md).** Quick summary below.

### Prerequisites
Node.js 18+, npm.

### Install & run locally
```bash
cd web
npm install
npm run dev
```

### Production build
```bash
npm run build      # outputs to web/dist
```

### Deploy to Vercel (recommended for the web app)
This repo is a monorepo (Android + web + Cloud Functions), so `vercel.json` at the repo root tells Vercel to build only `web/`:

```bash
npm install -g vercel
vercel          # first run: link/create the Vercel project, deploys a preview
vercel --prod   # deploy to production
```

Or connect the GitHub repo directly in the Vercel dashboard — no extra configuration needed, `vercel.json` handles the build command, output directory, and SPA rewrites automatically. The web app talks to Firebase (Auth/Firestore/Functions) purely client-side via the Firebase SDK, so hosting the frontend on Vercel while the backend stays on Firebase is completely normal — nothing else needs to change.

No environment variables need to be set in Vercel: `web/src/firebase.js` already contains the real, non-secret Firebase web config for the `kunjachaya-club` project (a Firebase web config is an identifier, not a credential — real access control comes from `firestore.rules`, not from hiding this file).

### Alternative: deploy to Firebase Hosting instead
```bash
firebase deploy --only hosting
```
(`firebase.json` is already configured to serve `web/dist` with SPA rewrites, if you'd rather keep everything on one platform.)

---

## 6. First-Run Checklist

After deploying rules, functions, and both app builds:

1. Register the **first real account** (email/password) through either app — it will land as `MemberClass.NEW`, `membershipStatus: "Pending"`.
2. **Manually promote that first account to President or General Secretary directly in the Firestore Console** (set `committeePost` to `PRESIDENT` or `GENERAL_SECRETARY` on that user's document). This is intentional — there is no seeded admin account, and there shouldn't be one; the club's real first officer needs to be set up by whoever controls the Firebase project.
3. Sign back in — that account now has full Executive Committee authority per ধারা-১৭ and can approve subsequent members, post notices, and assign committee posts through the in-app Admin Portal / Role dialog.
4. Test one real PipraPay checkout end-to-end (small amount) to confirm the webhook is wired correctly before relying on it for real dues collection.

---

## 7. Troubleshooting

- **Android: "Firebase API Key is not configured"** in Logcat → expected until you complete Section 1 step 3.
- **`createPipraPayCheckout` throws `failed-precondition`** → expected until you complete Section 3; this is the intended failure mode, not a bug.
- **Firestore permission-denied errors during testing** → check `firestore.rules` comments for the exact condition being enforced; a common cause is a user document missing `committeePost`/`memberClass` fields (make sure new accounts go through the app's real registration flow, not manually inserted test data).
- **Composite index errors on financials queries** → run `firebase deploy --only firestore:indexes` (Section 2).
