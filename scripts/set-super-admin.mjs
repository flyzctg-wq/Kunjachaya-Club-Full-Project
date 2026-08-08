/**
 * One-time script: elevate flyzctg@gmail.com to Super Admin (PRESIDENT)
 * Uses Firebase Web SDK (v9 modular) — same SDK the web app uses.
 *
 * Run from the project root:
 *   node --experimental-vm-modules scripts/set-super-admin.mjs
 * Or:
 *   cd web && node ../scripts/set-super-admin.mjs
 */

// Use the web app's firebase installation
import { createRequire } from 'module';
import { fileURLToPath } from 'url';
import path from 'path';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const webDir = path.resolve(__dirname, '../web/node_modules');

// Dynamically resolve firebase from the web app's node_modules
const require = createRequire(import.meta.url);

// Polyfill require for firebase ESM
process.chdir(path.resolve(__dirname, '../web'));

const { initializeApp } = await import('../web/node_modules/firebase/app/dist/index.esm.js').catch(() =>
  import(path.join(webDir, 'firebase/app/dist/index.esm.js'))
).catch(async () => {
  // Fallback: use dynamic import from web directory
  return import('firebase/app');
});

const { getAuth, signInWithEmailAndPassword } = await import('firebase/auth');
const { getFirestore, doc, setDoc } = await import('firebase/firestore');

const firebaseConfig = {
  apiKey: 'AIzaSyBt1CUvopFzQC6STqQ-lJ1R9GG5dvS-sXI',
  authDomain: 'kunjachaya-club.firebaseapp.com',
  projectId: 'kunjachaya-club',
  storageBucket: 'kunjachaya-club.firebasestorage.app',
  messagingSenderId: '668738359171',
  appId: '1:668738359171:web:033a0787646aca6077f0b6',
};

const app = initializeApp(firebaseConfig);
const auth = getAuth(app);
const db = getFirestore(app);

const TARGET_EMAIL = 'flyzctg@gmail.com';
const PASSWORD = 'bdsb_47487KC';

async function main() {
  console.log(`\n🔐  Signing in as ${TARGET_EMAIL}…`);
  const cred = await signInWithEmailAndPassword(auth, TARGET_EMAIL, PASSWORD);
  const uid = cred.user.uid;
  console.log(`✅  Signed in. UID = ${uid}`);

  const today = new Date().toISOString().slice(0, 10);
  const patch = {
    id: uid,
    primaryContact: TARGET_EMAIL,
    phone: TARGET_EMAIL,
    nameEn: 'Super Admin',
    nameBn: 'সুপার অ্যাডমিন',
    memberClass: 'FOUNDING',
    membershipStatus: 'Active',
    committeePost: 'PRESIDENT',
    isStandingCouncil: true,
    canManageNotices: true,
    canManageComplaints: true,
    canManageMembers: true,
    canManageFinancials: true,
    canDeleteItems: true,
    joinedDate: today,
  };

  console.log('\n📝  Writing super-admin fields to Firestore…');
  await setDoc(doc(db, 'users', uid), patch, { merge: true });

  console.log(`\n✅  Done! ${TARGET_EMAIL} is now PRESIDENT / Super Admin.`);
  console.log('    Sign out and back in on the web app to activate Admin Portal.');
  process.exit(0);
}

main().catch((err) => {
  console.error('\n❌  Error:', err.message || err);
  process.exit(1);
});
