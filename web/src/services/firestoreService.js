// Mirrors app/src/main/java/com/example/data/repository/FirestoreRepository.kt —
// same collection names, same field names, so the web app and the Android
// app read and write the exact same real data. No mock data, no local-only
// fallback: every function here talks directly to Firestore.
import {
  collection,
  doc,
  onSnapshot,
  query,
  where,
  orderBy,
  addDoc,
  setDoc,
  updateDoc,
  deleteDoc,
  getDoc,
  getDocs,
} from 'firebase/firestore';
import { db } from '../firebase';

const usersCol = collection(db, 'users');
const financialsCol = collection(db, 'financials');
const announcementsCol = collection(db, 'announcements');
const complaintsCol = collection(db, 'complaints');
const activityLogsCol = collection(db, 'ActivityLogs');
const eventsCol = collection(db, 'Events');

// --- USERS ---

export function subscribeUsers(onData, onError) {
  return onSnapshot(usersCol, (snap) => {
    onData(snap.docs.map((d) => ({ ...d.data(), id: d.id })));
  }, onError);
}

export async function getUserById(userId) {
  const snap = await getDoc(doc(usersCol, userId));
  return snap.exists() ? { ...snap.data(), id: snap.id } : null;
}

export async function findUserByContact(contact) {
  if (!contact) return null;
  const byPhone = await getDocs(query(usersCol, where('phone', '==', contact)));
  if (!byPhone.empty) return { ...byPhone.docs[0].data(), id: byPhone.docs[0].id };
  const byPrimary = await getDocs(query(usersCol, where('primaryContact', '==', contact)));
  if (!byPrimary.empty) return { ...byPrimary.docs[0].data(), id: byPrimary.docs[0].id };
  return null;
}

export async function saveUser(user) {
  await setDoc(doc(usersCol, user.id), user, { merge: true });
}

export async function updateUser(userId, patch) {
  await setDoc(doc(usersCol, userId), patch, { merge: true });
}

export async function updateMembershipStatus(userId, status) {
  await setDoc(doc(usersCol, userId), { membershipStatus: status }, { merge: true });
}

export async function updateUserRoleAndPrivileges(userId, {
  memberClass, committeePost, canManageNotices, canManageComplaints,
  canManageMembers, canManageFinancials, canDeleteItems,
}) {
  await setDoc(doc(usersCol, userId), {
    memberClass, committeePost, canManageNotices, canManageComplaints,
    canManageMembers, canManageFinancials, canDeleteItems,
  }, { merge: true });
}

export async function deleteUser(userId) {
  await deleteDoc(doc(usersCol, userId));
}

// --- FINANCIALS ---

export function subscribeAllFinancials(onData, onError) {
  return onSnapshot(financialsCol, (snap) => {
    onData(snap.docs.map((d) => ({ ...d.data(), _docId: d.id })));
  }, onError);
}

export function subscribeUserFinancials(userId, onData, onError) {
  const q = query(financialsCol, where('userId', '==', userId), orderBy('date', 'desc'));
  return onSnapshot(q, (snap) => {
    onData(snap.docs.map((d) => ({ ...d.data(), _docId: d.id })));
  }, onError);
}

export async function addFinancialRecord(record) {
  const docRef = await addDoc(financialsCol, record);
  const numericId = hashToLong(docRef.id);
  await updateDoc(docRef, { id: numericId });
  return { ...record, id: numericId, _docId: docRef.id };
}

export async function updatePaymentStatus(recordDocId, status, transactionId, paymentGateway) {
  await updateDoc(doc(financialsCol, recordDocId), { status, transactionId, paymentGateway });
}

// --- ANNOUNCEMENTS / NOTICES ---

export function subscribeAnnouncements(onData, onError) {
  return onSnapshot(announcementsCol, (snap) => {
    onData(snap.docs.map((d) => ({ ...d.data(), _docId: d.id })));
  }, onError);
}

export async function addAnnouncement(announcement) {
  const docRef = await addDoc(announcementsCol, announcement);
  const numericId = hashToLong(docRef.id);
  await updateDoc(docRef, { id: numericId });
  return { ...announcement, id: numericId, _docId: docRef.id };
}

// --- COMPLAINTS ---

export function subscribeComplaints(onData, onError) {
  return onSnapshot(complaintsCol, (snap) => {
    onData(snap.docs.map((d) => ({ ...d.data(), _docId: d.id })));
  }, onError);
}

export function subscribeUserComplaints(userId, onData, onError) {
  const q = query(complaintsCol, where('userId', '==', userId));
  return onSnapshot(q, (snap) => {
    onData(snap.docs.map((d) => ({ ...d.data(), _docId: d.id })));
  }, onError);
}

export async function addComplaint(complaint) {
  const docRef = await addDoc(complaintsCol, { ...complaint, status: 'Pending' });
  const numericId = hashToLong(docRef.id);
  await updateDoc(docRef, { id: numericId });
  return { ...complaint, id: numericId, _docId: docRef.id, status: 'Pending' };
}

export async function updateComplaintStatus(complaintDocId, status, adminNoteEn, adminNoteBn, updatedAt) {
  await updateDoc(doc(complaintsCol, complaintDocId), { status, adminNoteEn, adminNoteBn, updatedAt });
}

// --- EVENTS ---

export function subscribeEvents(onData, onError) {
  return onSnapshot(eventsCol, (snap) => {
    onData(snap.docs.map((d) => ({ ...d.data(), _docId: d.id })));
  }, onError);
}

export async function addEvent(event) {
  const docRef = doc(eventsCol);
  const eventToSave = { ...event, id: event.id || docRef.id };
  await setDoc(docRef, eventToSave);
  return { ...eventToSave, _docId: docRef.id };
}

export async function updateEventReminder(eventDocId, isReminderSet) {
  await updateDoc(doc(eventsCol, eventDocId), { isReminderSet });
}

// --- ACTIVITY LOGS ---

export function subscribeActivityLogs(onData, onError) {
  return onSnapshot(activityLogsCol, (snap) => {
    onData(snap.docs.map((d) => ({ ...d.data(), _docId: d.id })));
  }, onError);
}

export async function addActivityLog(log) {
  const docRef = doc(activityLogsCol);
  const logToSave = { ...log, id: log.id || docRef.id };
  await setDoc(docRef, logToSave);
  return { ...logToSave, _docId: docRef.id };
}

// Mirrors Kotlin's `docRef.id.hashCode().toLong()` pattern used by Android
// when it needs a numeric id alongside Firestore's string document id.
function hashToLong(str) {
  let hash = 0;
  for (let i = 0; i < str.length; i++) {
    hash = (hash * 31 + str.charCodeAt(i)) | 0;
  }
  return hash;
}
