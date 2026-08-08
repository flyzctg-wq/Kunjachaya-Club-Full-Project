import React, { useState, useEffect } from 'react';
import Header from './components/Header';
import BottomNav from './components/BottomNav';
import DashboardView from './views/DashboardView';
import FinancialsView from './views/FinancialsView';
import NoticesView from './views/NoticesView';
import ComplaintsView from './views/ComplaintsView';
import EventsView from './views/EventsView';
import DirectoryView from './views/DirectoryView';
import AdminPortalView from './views/AdminPortalView';
import DevDocsView from './views/DevDocsView';
import ProfileView from './views/ProfileView';
import AuthModal from './views/AuthModal';

import { watchAuthState } from './services/authService';
import {
  subscribeUsers,
  subscribeAllFinancials,
  subscribeUserFinancials,
  subscribeAnnouncements,
  subscribeComplaints,
  subscribeUserComplaints,
  subscribeEvents,
  subscribeActivityLogs,
  findUserByContact,
} from './services/firestoreService';
import { isExecutiveCommittee, hasComplaintPermission } from './roles';

export default function App() {
  const [lang, setLang] = useState('en');
  const [currentUser, setCurrentUser] = useState(null);
  const [authLoading, setAuthLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('dashboard');
  const [isDarkMode, setIsDarkMode] = useState(false);
  const [showAuthModal, setShowAuthModal] = useState(false);

  // Real Firestore-backed state — same collections & fields as the Android app.
  const [users, setUsers] = useState([]);
  const [financials, setFinancials] = useState([]);
  const [notices, setNotices] = useState([]);
  const [complaints, setComplaints] = useState([]);
  const [events, setEvents] = useState([]);
  const [activityLogs, setActivityLogs] = useState([]);

  // Restore the real Firebase Auth session, if any.
  useEffect(() => {
    const unsub = watchAuthState((firebaseUser) => {
      if (firebaseUser) {
        const identity = firebaseUser.email || firebaseUser.phoneNumber || '';
        const fallback = {
          id: firebaseUser.uid,
          phone: identity,
          nameEn: firebaseUser.displayName || identity.split('@')[0] || 'Member',
          nameBn: firebaseUser.displayName || identity.split('@')[0] || 'সদস্য',
          primaryContact: identity,
          membershipStatus: 'Pending',
          memberClass: 'NEW',
          committeePost: '',
          canManageNotices: false, canManageComplaints: false, canManageMembers: false,
          canManageFinancials: false, canDeleteItems: false,
          joinedDate: new Date().toISOString().slice(0, 10),
        };
        setCurrentUser((prev) => prev || fallback);
        setAuthLoading(false);

        // Fetch full profile document from Firestore in background
        findUserByContact(identity)
          .then((member) => { if (member) setCurrentUser(member); })
          .catch((e) => console.warn('Background member fetch error:', e));
      } else {
        setCurrentUser(null);
        setAuthLoading(false);
      }
    });
    return unsub;
  }, []);

  // Live Firestore subscriptions — real data only, no mock fallback.
  useEffect(() => {
    const unsub = subscribeUsers(setUsers, (e) => console.error('users stream', e));
    return unsub;
  }, []);

  useEffect(() => {
    const unsub = subscribeAnnouncements(setNotices, (e) => console.error('announcements stream', e));
    return unsub;
  }, []);

  useEffect(() => {
    const unsub = subscribeEvents(setEvents, (e) => console.error('events stream', e));
    return unsub;
  }, []);

  useEffect(() => {
    if (!currentUser) { setFinancials([]); return; }
    const unsub = isExecutiveCommittee(currentUser)
      ? subscribeAllFinancials(setFinancials, (e) => console.error('financials stream', e))
      : subscribeUserFinancials(currentUser.id, setFinancials, (e) => console.error('financials stream', e));
    return unsub;
  }, [currentUser?.id, currentUser?.committeePost]);

  useEffect(() => {
    if (!currentUser) { setComplaints([]); return; }
    const unsub = hasComplaintPermission(currentUser)
      ? subscribeComplaints(setComplaints, (e) => console.error('complaints stream', e))
      : subscribeUserComplaints(currentUser.id, setComplaints, (e) => console.error('complaints stream', e));
    return unsub;
  }, [currentUser?.id, currentUser?.canManageComplaints, currentUser?.committeePost]);

  useEffect(() => {
    if (!currentUser || !isExecutiveCommittee(currentUser)) { setActivityLogs([]); return; }
    const unsub = subscribeActivityLogs(setActivityLogs, (e) => console.error('activity logs stream', e));
    return unsub;
  }, [currentUser?.id, currentUser?.committeePost]);

  useEffect(() => {
    if (isDarkMode) {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }, [isDarkMode]);

  if (authLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-surface dark:bg-inverse-surface text-on-surface-variant font-body-md">
        {lang === 'bn' ? 'লোড হচ্ছে...' : 'Loading...'}
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-surface dark:bg-inverse-surface text-on-surface dark:text-inverse-on-surface transition-colors">

      <Header
        lang={lang}
        setLang={setLang}
        currentUser={currentUser}
        setCurrentUser={setCurrentUser}
        setShowAuthModal={setShowAuthModal}
      />

      <main className="pt-20 pb-32 px-4 max-w-2xl mx-auto space-y-6 min-h-screen">
        {!currentUser ? (
          <div className="text-center py-24">
            <p className="text-on-surface-variant mb-4">
              {lang === 'bn' ? 'চালিয়ে যেতে সাইন ইন করুন' : 'Sign in to continue'}
            </p>
            <button
              onClick={() => setShowAuthModal(true)}
              className="px-6 py-3 bg-primary text-white rounded-full font-label-lg active:scale-95 transition-transform shadow-md"
            >
              {lang === 'bn' ? 'সাইন ইন করুন' : 'Sign In'}
            </button>
          </div>
        ) : (
          <>
            {activeTab === 'dashboard' && (
              <DashboardView
                lang={lang}
                currentUser={currentUser}
                setActiveTab={setActiveTab}
                financials={financials}
                notices={notices}
                events={events}
                complaints={complaints}
              />
            )}

            {activeTab === 'financials' && (
              <FinancialsView
                lang={lang}
                currentUser={currentUser}
                financials={financials}
              />
            )}

            {activeTab === 'notices' && (
              <NoticesView
                lang={lang}
                notices={notices}
              />
            )}

            {activeTab === 'complaints' && (
              <ComplaintsView
                lang={lang}
                currentUser={currentUser}
                complaints={complaints}
              />
            )}

            {activeTab === 'events' && (
              <EventsView
                lang={lang}
                currentUser={currentUser}
                events={events}
              />
            )}

            {activeTab === 'directory' && (
              <DirectoryView
                lang={lang}
                users={users}
              />
            )}

            {activeTab === 'profile' && (
              <ProfileView
                lang={lang}
                currentUser={currentUser}
                setCurrentUser={setCurrentUser}
                setActiveTab={setActiveTab}
              />
            )}

            {activeTab === 'admin' && isExecutiveCommittee(currentUser) && (
              <AdminPortalView
                lang={lang}
                currentUser={currentUser}
                users={users}
                notices={notices}
                financials={financials}
                complaints={complaints}
                activityLogs={activityLogs}
              />
            )}

            {activeTab === 'devdocs' && isExecutiveCommittee(currentUser) && (
              <DevDocsView />
            )}
          </>
        )}
      </main>

      {currentUser && (
        <BottomNav activeTab={activeTab} setActiveTab={setActiveTab} lang={lang} />
      )}

      {showAuthModal && (
        <AuthModal
          lang={lang}
          setCurrentUser={setCurrentUser}
          setShowAuthModal={setShowAuthModal}
        />
      )}

    </div>
  );
}
