// Mirrors the auth logic in app/src/main/java/com/example/ui/viewmodel/ClubViewModel.kt
// (loginWithEmail / registerWithEmail): real Firebase Auth only, no local
// shortcut, no fabricated identity data, no hardcoded privileged accounts.
// New sign-ins start as Pending / MemberClass.NEW and wait for a real
// Executive Committee member to approve membership (ধারা-১০).
import {
  signInWithEmailAndPassword,
  createUserWithEmailAndPassword,
  updateProfile,
  onAuthStateChanged,
  signOut as firebaseSignOut,
} from 'firebase/auth';
import { auth } from '../firebase';
import { findUserByContact, getUserById, saveUser } from './firestoreService';
import { MemberClass } from '../roles';

export function watchAuthState(callback) {
  return onAuthStateChanged(auth, callback);
}

export async function signIn(email, password) {
  const cred = await signInWithEmailAndPassword(auth, email, password);
  try {
    const realMember = await getUserById(cred.user.uid);
    if (realMember) return realMember;
  } catch (e) {
    console.warn('getUserById failed on signIn:', e);
  }
  return resolveMemberForFirebaseUser(cred.user);
}

export async function register(name, email, phone, password) {
  const cred = await createUserWithEmailAndPassword(auth, email, password);
  if (name) {
    try {
      await updateProfile(cred.user, { displayName: name });
    } catch (e) {
      console.warn('updateProfile failed:', e);
    }
  }

  try {
    const existing = await findUserByContact(phone || email);
    if (existing) return existing;
  } catch (e) {
    console.warn('findUserByContact check failed:', e);
  }

  // No fabricated identity, address, or NID — the resident fills in their
  // real profile, and an Executive Committee member approves membership.
  const newUser = {
    id: cred.user.uid,
    phone: phone || email,
    nameEn: name || '',
    nameBn: name || '',
    dob: '', bloodGroup: '', professionEn: '', professionBn: '',
    road: '', block: '', floor: '', holding: '',
    primaryContact: phone || email,
    emergencyContact: phone || '',
    fatherOrSpouseNameEn: '', fatherOrSpouseNameBn: '',
    motherNameEn: '', motherNameBn: '',
    familyMembersCount: 1,
    membershipStatus: 'Pending',
    memberClass: MemberClass.NEW,
    committeePost: '',
    isStandingCouncil: false,
    canManageNotices: false, canManageComplaints: false, canManageMembers: false,
    canManageFinancials: false, canDeleteItems: false,
    profilePicUrl: '', nidFrontUrl: '', nidBackUrl: '',
    joinedDate: new Date().toISOString().slice(0, 10),
  };

  try {
    await saveUser(newUser);
  } catch (e) {
    console.warn('saveUser failed:', e);
  }
  return newUser;
}

export async function signOut() {
  await firebaseSignOut(auth);
}

function resolveMemberForFirebaseUser(firebaseUser) {
  if (!firebaseUser) return null;

  const identity = firebaseUser.email || firebaseUser.phoneNumber || '';
  const fallback = {
    id: firebaseUser.uid,
    phone: identity || '',
    nameEn: firebaseUser.displayName || identity.split('@')[0] || 'Member',
    nameBn: firebaseUser.displayName || identity.split('@')[0] || 'সদস্য',
    primaryContact: identity || '',
    membershipStatus: 'Pending',
    memberClass: MemberClass.NEW,
    committeePost: '',
    canManageNotices: false, canManageComplaints: false, canManageMembers: false,
    canManageFinancials: false, canDeleteItems: false,
    joinedDate: new Date().toISOString().slice(0, 10),
  };

  // Perform Firestore document lookup & sync in background without blocking UI
  (async () => {
    try {
      const byId = await getUserById(firebaseUser.uid);
      if (!byId) {
        await saveUser(fallback);
      }
    } catch (e) {
      console.warn('Background Firestore profile sync skipped:', e);
    }
  })();

  return fallback;
}
