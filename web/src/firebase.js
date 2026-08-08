// Real Firebase project config for Kunjachaya Club — the SAME Firebase
// project ("kunjachaya-club") that the Android app connects to, so both
// platforms read and write the exact same Firestore data.
//
// NOTE: a Firebase *web* config (apiKey included) is not a secret — it only
// identifies the project to Google's servers. Access control is enforced by
// Firestore Security Rules + Firebase Auth, not by hiding this object.
import { initializeApp } from "firebase/app";
import { getAuth } from "firebase/auth";
import { getFirestore } from "firebase/firestore";
import { getStorage } from "firebase/storage";
import { getFunctions } from "firebase/functions";
import { getAnalytics, isSupported as analyticsIsSupported } from "firebase/analytics";

const firebaseConfig = {
  apiKey: "AIzaSyBt1CUvopFzQC6STqQ-lJ1R9GG5dvS-sXI",
  authDomain: "kunjachaya-club.firebaseapp.com",
  projectId: "kunjachaya-club",
  storageBucket: "kunjachaya-club.firebasestorage.app",
  messagingSenderId: "668738359171",
  appId: "1:668738359171:web:033a0787646aca6077f0b6",
  measurementId: "G-8ML73K0HQ6",
};

export const firebaseApp = initializeApp(firebaseConfig);
export const auth = getAuth(firebaseApp);
export const db = getFirestore(firebaseApp);
export const storage = getStorage(firebaseApp);
export const functions = getFunctions(firebaseApp);

// Analytics only works in a real browser context, so guard it.
export let analytics = null;
analyticsIsSupported().then((supported) => {
  if (supported) analytics = getAnalytics(firebaseApp);
});
