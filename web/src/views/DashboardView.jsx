import React from 'react';
import Icon from '../components/Icon';
import { translations } from '../translations';
import { isExecutiveCommittee } from '../roles';

export default function DashboardView({
  lang,
  currentUser,
  setActiveTab,
  financials,
  notices,
  events,
  complaints,
}) {
  const t = translations[lang];

  const pendingDues = financials.filter(f => f.type === 'Due' && (f.status === 'Pending' || f.status === 'Failed'));
  const totalPendingAmount = pendingDues.reduce((acc, curr) => acc + (curr.amount || 0), 0);
  const displayName = currentUser ? (lang === 'bn' ? currentUser.nameBn : currentUser.nameEn) || currentUser.phone : 'Resident';
  const latestNotice = notices[0];
  const nextEvent = events[0];
  const openTickets = complaints.filter(c => c.status !== 'Resolved');

  return (
    <>
      {/* Greeting Section */}
      <section className="flex items-center justify-between">
        <div className="space-y-1">
          <h2 className="font-headline-lg-mobile text-headline-lg-mobile text-on-surface">
            {t.welcomeMsg}, {displayName}
          </h2>
          <p className="font-body-md text-body-md text-on-surface-variant">
            {currentUser?.holding
              ? `${t.flat}: ${currentUser.holding}${currentUser.block ? ', Block ' + currentUser.block : ''}`
              : 'Welcome home to your sanctuary.'}
          </p>
        </div>
        <div className="relative">
          <div className="w-16 h-16 rounded-full border-4 border-white shadow-md bg-surface-container-high flex items-center justify-center">
            <Icon name="person" className="text-3xl text-on-surface-variant" />
          </div>
          <div className="absolute bottom-0 right-0 w-4 h-4 bg-primary rounded-full border-2 border-white" />
        </div>
      </section>

      {/* Quick-stat Bento Grid */}
      <section className="grid grid-cols-2 gap-4">
        {/* Dues Status */}
        <div
          onClick={() => setActiveTab('financials')}
          className="col-span-1 bg-white dark:bg-surface-container p-4 rounded-2xl card-shadow flex flex-col justify-between min-h-[140px] border border-surface-variant/50 cursor-pointer"
        >
          <div className="flex justify-between items-start">
            <Icon name="account_balance_wallet" className="text-primary p-2 bg-secondary-container rounded-xl" />
            <span className={`px-3 py-1 rounded-full font-label-sm text-[10px] ${totalPendingAmount > 0 ? 'bg-error/10 text-error' : 'bg-primary/10 text-primary'}`}>
              {totalPendingAmount > 0 ? 'DUE' : 'CLEAR'}
            </span>
          </div>
          <div>
            <p className="font-label-sm text-on-surface-variant">{t.totalDues}</p>
            <p className="font-title-lg text-primary font-bold">
              {totalPendingAmount > 0 ? `৳${totalPendingAmount.toLocaleString()}` : 'Paid'}
            </p>
          </div>
        </div>

        {/* Next Event */}
        <div
          onClick={() => setActiveTab('events')}
          className="col-span-1 bg-primary text-white p-4 rounded-2xl shadow-lg flex flex-col justify-between min-h-[140px] cursor-pointer"
        >
          <div className="flex justify-between items-start">
            <Icon name="event" className="text-on-primary-container p-2 bg-white/20 rounded-xl" />
          </div>
          <div>
            <p className="font-label-sm text-on-primary-container opacity-80 truncate">
              {nextEvent ? (lang === 'bn' ? nextEvent.titleBn : nextEvent.titleEn) : 'No events yet'}
            </p>
            <p className="font-title-lg font-bold">{nextEvent ? nextEvent.date : '—'}</p>
          </div>
        </div>

        {/* Notice Snippet */}
        {latestNotice && (
          <div
            onClick={() => setActiveTab('notices')}
            className="col-span-2 bg-secondary-container p-4 rounded-2xl flex items-center gap-4 cursor-pointer"
          >
            <div className="flex-shrink-0 w-12 h-12 bg-white rounded-xl flex items-center justify-center text-primary">
              <Icon name="campaign" filled />
            </div>
            <div className="flex-1 min-w-0">
              <p className="font-label-sm text-on-secondary-container font-bold uppercase tracking-wider">Latest Notice</p>
              <p className="font-body-md text-on-secondary-container line-clamp-1">
                {lang === 'bn' ? latestNotice.titleBn : latestNotice.titleEn}
              </p>
            </div>
            <Icon name="chevron_right" className="text-on-secondary-container/50" />
          </div>
        )}
      </section>

      {/* Shortcut Tiles */}
      <section className="space-y-4">
        <h3 className="font-title-lg text-title-lg text-on-surface-variant px-1">Quick Actions</h3>
        <div className="grid grid-cols-2 gap-4 auto-rows-[minmax(120px,auto)]">
          <ShortcutTile icon="newspaper" label={t.navNotices} onClick={() => setActiveTab('notices')} />
          <ShortcutTile icon="support_agent" label={t.navComplaints} badge={openTickets.length || null} onClick={() => setActiveTab('complaints')} />
          <ShortcutTile icon="payments" label={t.navFinancials} onClick={() => setActiveTab('financials')} />
          <ShortcutTile icon="contact_phone" label={t.navDirectory} onClick={() => setActiveTab('directory')} />

          <button
            onClick={() => setActiveTab('events')}
            className="col-span-2 flex items-center justify-between bg-white dark:bg-surface-container rounded-2xl card-shadow p-6 transition-all hover:bg-surface-container-low active:scale-95 border border-surface-variant/30"
          >
            <div className="flex items-center gap-4">
              <Icon name="calendar_today" className="text-primary text-3xl" />
              <span className="font-label-lg text-on-surface">Event Calendar</span>
            </div>
            <Icon name="arrow_forward" className="text-on-surface-variant" />
          </button>

          {isExecutiveCommittee(currentUser) && (
            <button
              onClick={() => setActiveTab('admin')}
              className="col-span-2 flex items-center justify-between bg-tertiary text-white rounded-2xl shadow-lg p-6 transition-all active:scale-95"
            >
              <div className="flex items-center gap-4">
                <Icon name="shield" className="text-3xl" />
                <span className="font-label-lg">{t.adminDashboard}</span>
              </div>
              <Icon name="arrow_forward" />
            </button>
          )}
        </div>
      </section>

      {/* Resident ID Preview */}
      <section
        onClick={() => setActiveTab('profile')}
        className="bg-surface-container p-6 rounded-[24px] border border-outline-variant/30 relative overflow-hidden group cursor-pointer"
      >
        <div className="absolute top-0 right-0 w-32 h-32 bg-primary/5 rounded-full -mr-16 -mt-16 transition-transform group-hover:scale-110" />
        <div className="flex items-center gap-4 mb-4">
          <Icon name="badge" className="text-primary" />
          <h4 className="font-title-lg text-primary font-bold">Resident Pass</h4>
        </div>
        <div className="flex items-end justify-between">
          <div className="space-y-1">
            <p className="font-label-sm text-on-surface-variant">
              {currentUser?.holding ? `Unit ${currentUser.holding}` : 'Complete your profile'}
            </p>
            <p className="font-body-lg text-on-surface font-semibold tracking-wide">{displayName}</p>
          </div>
          <div className="bg-white p-2 rounded-lg shadow-sm">
            <Icon name="qr_code_2" className="text-4xl text-on-surface" />
          </div>
        </div>
      </section>
    </>
  );
}

function ShortcutTile({ icon, label, badge, onClick }) {
  return (
    <button
      onClick={onClick}
      className="relative flex flex-col items-center justify-center bg-white dark:bg-surface-container rounded-2xl card-shadow p-6 transition-all hover:bg-surface-container-low active:scale-95 border border-surface-variant/30"
    >
      {badge ? (
        <span className="absolute top-3 right-3 min-w-[20px] h-5 px-1.5 bg-error text-on-error text-[10px] font-bold rounded-full flex items-center justify-center">
          {badge}
        </span>
      ) : null}
      <Icon name={icon} className="text-primary text-3xl mb-3" />
      <span className="font-label-lg text-on-surface">{label}</span>
    </button>
  );
}
