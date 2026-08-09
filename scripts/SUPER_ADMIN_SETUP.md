# First President/GS Setup — Manual Firebase Console Steps

There's no seeded admin account in this project on purpose — the first real Executive Committee officer has to be set up by whoever controls the Firebase project, once, after that person has registered a real account through the app.

**Faster option:** `web/set-super-admin.cjs` does steps 2–5 below automatically. Run it with your own credentials:
```bash
cd web
SUPER_ADMIN_EMAIL=you@example.com SUPER_ADMIN_PASSWORD='...' node set-super-admin.cjs
```
Use the manual steps below only if you'd rather not run the script, or need to promote an account that isn't yours.

## 1. Register a real account first
Sign up through the Android or web app with a real email/password. It'll land as `memberClass: NEW`, `membershipStatus: Pending` — that's expected.

## 2. Find that account's real Firebase Auth UID
Firebase Console → Authentication → Users tab → copy the UID for that account.
https://console.firebase.google.com/project/kunjachaya-club/authentication/users

## 3. Open Firestore → `users` collection
https://console.firebase.google.com/project/kunjachaya-club/firestore/databases/-default-/data

Find the document whose ID matches the UID from step 2 (the app already created it on registration).

## 4. Edit these fields on that document

| Field Name           | Type    | Value                        |
|----------------------|---------|------------------------------|
| memberClass           | string  | FOUNDING                     |
| membershipStatus      | string  | Active                        |
| committeePost          | string  | PRESIDENT (or GENERAL_SECRETARY) |
| isStandingCouncil     | boolean | true                          |
| canManageNotices      | boolean | true                          |
| canManageComplaints   | boolean | true                          |
| canManageMembers      | boolean | true                          |
| canManageFinancials   | boolean | true                          |
| canDeleteItems         | boolean | true                          |

President and General Secretary carry equal, independent authority per ধারা-১৭ — pick whichever post matches the real person's actual role in the club.

## 5. Save, then deploy Firestore Security Rules if you haven't yet
```bash
firebase deploy --only firestore:rules
```

## 6. Sign back in
That account now has full Executive Committee authority and can approve subsequent members, post notices, and assign committee posts through the in-app Role dialog — no more manual Firestore editing needed after this one bootstrap step.
