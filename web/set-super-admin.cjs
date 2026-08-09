// One-time script: elevate a real account to President / General Secretary.
// Run: node set-super-admin.cjs  (from the /web folder)
//
// Credentials are read from environment variables — never hardcode a real
// email/password pair in source control. Example:
//   SUPER_ADMIN_EMAIL=you@example.com SUPER_ADMIN_PASSWORD='...' node set-super-admin.cjs

const { initializeApp } = require('firebase/app');
const { getAuth, signInWithEmailAndPassword } = require('firebase/auth');
const { getFirestore, doc, setDoc } = require('firebase/firestore');

const firebaseConfig = {
  apiKey: 'AIzaSyBt1CUvopFzQC6STqQ-lJ1R9GG5dvS-sXI',
  authDomain: 'kunjachaya-club.firebaseapp.com',
  projectId: 'kunjachaya-club',
  storageBucket: 'kunjachaya-club.firebasestorage.app',
  messagingSenderId: '668738359171',
  appId: '1:668738359171:web:033a0787646aca6077f0b6',
};

const TARGET_EMAIL = process.env.SUPER_ADMIN_EMAIL;
const PASSWORD = process.env.SUPER_ADMIN_PASSWORD;

async function main() {
  if (!TARGET_EMAIL || !PASSWORD) {
    console.error('\n❌  Set SUPER_ADMIN_EMAIL and SUPER_ADMIN_PASSWORD environment variables first.');
    console.error('    Example: SUPER_ADMIN_EMAIL=you@example.com SUPER_ADMIN_PASSWORD=\'...\' node set-super-admin.cjs\n');
    process.exit(1);
  }

  const app  = initializeApp(firebaseConfig);
  const auth = getAuth(app);
  const db   = getFirestore(app);

  console.log(`\n🔐  Signing in as ${TARGET_EMAIL}…`);
  const cred = await signInWithEmailAndPassword(auth, TARGET_EMAIL, PASSWORD);
  const uid  = cred.user.uid;
  console.log(`✅  Signed in. UID = ${uid}`);

  const today = new Date().toISOString().slice(0, 10);
  const patch = {
    id:                  uid,
    primaryContact:      TARGET_EMAIL,
    phone:               TARGET_EMAIL,
    nameEn:              'Club President',
    nameBn:              'ক্লাব সভাপতি',
    memberClass:         'FOUNDING',
    membershipStatus:    'Active',
    committeePost:       'PRESIDENT',
    isStandingCouncil:   true,
    canManageNotices:    true,
    canManageComplaints: true,
    canManageMembers:    true,
    canManageFinancials: true,
    canDeleteItems:      true,
    joinedDate:          today,
  };

  console.log('\n📝  Writing President fields to Firestore…');
  await setDoc(doc(db, 'users', uid), patch, { merge: true });

  console.log(`\n✅  ${TARGET_EMAIL} is now President (ধারা-১৭ authority).`);
  console.log('    Sign out and back in on the web app to activate the Admin Portal.');
  process.exit(0);
}

main().catch((err) => {
  console.error('\n❌  Error:', err.code || err.message || err);
  process.exit(1);
});
