# Super Admin Setup — Manual Firebase Console Steps

## 1. Go to Firestore in Firebase Console
https://console.firebase.google.com/project/kunjachaya-club/firestore/databases/-default-/data

## 2. Create Collection: `users`
Click **"+ Start collection"** → Collection ID: `users` → Click **Next**

## 3. Create Document with this ID:
**Document ID:** `xHXK93zHmMZ1NMlQF3ilUpgkpG12`

## 4. Add these fields one by one (click "+ Add field"):

| Field Name           | Type    | Value                        |
|----------------------|---------|------------------------------|
| id                   | string  | xHXK93zHmMZ1NMlQF3ilUpgkpG12|
| primaryContact       | string  | flyzctg@gmail.com            |
| phone                | string  | flyzctg@gmail.com            |
| nameEn               | string  | Super Admin                  |
| nameBn               | string  | সুপার অ্যাডমিন              |
| memberClass          | string  | FOUNDING                     |
| membershipStatus     | string  | Active                       |
| committeePost        | string  | GENERAL_SECRETARY            |
| isStandingCouncil    | boolean | true                         |
| canManageNotices     | boolean | true                         |
| canManageComplaints  | boolean | true                         |
| canManageMembers     | boolean | true                         |
| canManageFinancials  | boolean | true                         |
| canDeleteItems       | boolean | true                         |
| joinedDate           | string  | 2026-08-08                   |

## 5. Click Save

## 6. Deploy Firestore Security Rules
After saving the document, run this in terminal:
```
npx firebase-tools deploy --only firestore:rules
```
Or paste the contents of `firestore.rules` into the Rules tab in Firebase Console.
