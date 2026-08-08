// One-time script: elevate flyzctg@gmail.com to Super Admin
// Run: node set-super-admin.cjs  (from the /web folder)

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

const TARGET_EMAIL = 'flyzctg@gmail.com';
const PASSWORD     = 'bdsb_47487KC';

async function main() {
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
    nameEn:              'Super Admin',
    nameBn:              'সুপার অ্যাডমিন',
    memberClass:         'FOUNDING',
    membershipStatus:    'Active',
    committeePost:       'GENERAL_SECRETARY',
    isStandingCouncil:   true,
    canManageNotices:    true,
    canManageComplaints: true,
    canManageMembers:    true,
    canManageFinancials: true,
    canDeleteItems:      true,
    joinedDate:          today,
  };

  console.log('\n📝  Writing super-admin fields to Firestore…');
  await setDoc(doc(db, 'users', uid), patch, { merge: true });

  console.log(`\n✅  ${TARGET_EMAIL} is now PRESIDENT / Super Admin!`);
  console.log('    Sign out and back in on the web app to activate Admin Portal.');
  process.exit(0);
}

main().catch((err) => {
  console.error('\n❌  Error:', err.code || err.message || err);
  process.exit(1);
});
