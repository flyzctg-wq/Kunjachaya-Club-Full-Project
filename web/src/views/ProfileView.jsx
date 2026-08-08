import React, { useState } from 'react';
import Icon from '../components/Icon';
import { translations } from '../translations';
import { signOut } from '../services/authService';
import { updateUser } from '../services/firestoreService';
import {
  isExecutiveCommittee,
  committeePostLabel,
  memberClassLabel,
} from '../roles';

export default function ProfileView({ lang, currentUser, setCurrentUser, setActiveTab }) {
  const t = translations[lang];
  const [isEditing, setIsEditing] = useState(false);
  const [form, setForm] = useState({
    nameEn: currentUser?.nameEn || '',
    nameBn: currentUser?.nameBn || '',
    dob: currentUser?.dob || '',
    bloodGroup: currentUser?.bloodGroup || '',
    professionEn: currentUser?.professionEn || '',
    block: currentUser?.block || '',
    floor: currentUser?.floor || '',
    holding: currentUser?.holding || '',
    road: currentUser?.road || '',
    emergencyContact: currentUser?.emergencyContact || '',
  });
  const [isSaving, setIsSaving] = useState(false);

  if (!currentUser) return null;

  const handleSave = async () => {
    setIsSaving(true);
    // 1. Immediately update local state & exit edit mode
    setCurrentUser((prev) => ({ ...prev, ...form }));
    setIsEditing(false);

    // 2. Sync to Firestore with background timeout
    try {
      await Promise.race([
        updateUser(currentUser.id, form),
        new Promise((_, reject) => setTimeout(() => reject(new Error('Save timeout')), 5000))
      ]);
    } catch (err) {
      console.warn('Background profile save notice:', err);
    } finally {
      setIsSaving(false);
    }
  };

  if (isEditing) {
    return (
      <div>
        <div className="flex items-center gap-4 mb-6">
          <button onClick={() => setIsEditing(false)} className="p-2 rounded-full hover:bg-surface-variant transition-colors">
            <Icon name="arrow_back" />
          </button>
          <h2 className="font-title-lg text-title-lg">Edit Resident Profile</h2>
        </div>

        <div className="space-y-4">
          <FormField label="Full Name (English)" value={form.nameEn} onChange={(v) => setForm({ ...form, nameEn: v })} />
          <FormField label="নাম (বাংলায়)" value={form.nameBn} onChange={(v) => setForm({ ...form, nameBn: v })} />
          <div className="grid grid-cols-2 gap-4">
            <FormField label="Date of Birth" type="date" value={form.dob} onChange={(v) => setForm({ ...form, dob: v })} />
            <FormField label="Blood Group" value={form.bloodGroup} onChange={(v) => setForm({ ...form, bloodGroup: v })} placeholder="e.g. B+" />
          </div>
          <FormField label="Profession" value={form.professionEn} onChange={(v) => setForm({ ...form, professionEn: v })} />
          <div className="grid grid-cols-3 gap-3">
            <FormField label="Block" value={form.block} onChange={(v) => setForm({ ...form, block: v })} />
            <FormField label="Floor" value={form.floor} onChange={(v) => setForm({ ...form, floor: v })} />
            <FormField label="Holding" value={form.holding} onChange={(v) => setForm({ ...form, holding: v })} />
          </div>
          <FormField label="Road" value={form.road} onChange={(v) => setForm({ ...form, road: v })} />
          <FormField label="Emergency Contact" type="tel" value={form.emergencyContact} onChange={(v) => setForm({ ...form, emergencyContact: v })} />
        </div>

        <div className="flex gap-4 pt-6">
          <button
            onClick={() => setIsEditing(false)}
            className="flex-1 h-14 rounded-full border border-outline text-on-surface-variant font-label-lg active:scale-95 transition-all"
          >
            Cancel
          </button>
          <button
            onClick={handleSave}
            disabled={isSaving}
            className="flex-1 h-14 rounded-full bg-primary text-white font-label-lg shadow-lg shadow-primary/20 active:scale-95 transition-all disabled:opacity-60"
          >
            {isSaving ? 'Saving...' : 'Save Changes'}
          </button>
        </div>
      </div>
    );
  }

  return (
    <div>
      {/* Resident ID Card */}
      <div className="bg-white dark:bg-surface-container rounded-[24px] shadow-sm p-6 relative overflow-hidden border border-outline-variant/30 mb-8">
        <div className="absolute top-0 right-0 w-32 h-32 bg-primary/5 rounded-full -mr-16 -mt-16 blur-3xl" />
        <div className="flex items-center gap-6 relative z-10">
          <div className="relative">
            <div className="w-24 h-24 rounded-full border-4 border-secondary-container overflow-hidden bg-surface-variant flex items-center justify-center">
              <Icon name="person" className="text-5xl text-on-surface-variant" />
            </div>
            <span className="absolute bottom-1 right-1 bg-primary text-white text-[10px] font-bold px-2 py-0.5 rounded-full border-2 border-white uppercase">
              {currentUser.membershipStatus}
            </span>
          </div>
          <div>
            <h2 className="font-title-lg text-title-lg text-on-surface">
              {lang === 'bn' ? (currentUser.nameBn || currentUser.nameEn) : (currentUser.nameEn || currentUser.nameBn)}
            </h2>
            <p className="font-body-md text-on-surface-variant">ID: {currentUser.id?.slice(0, 12)}</p>
            <div className="mt-2 flex gap-2 flex-wrap">
              {currentUser.bloodGroup && (
                <span className="bg-primary-container text-on-primary-container text-[11px] px-3 py-1 rounded-full font-medium">
                  {currentUser.bloodGroup}
                </span>
              )}
              <span className="bg-secondary-container text-on-secondary-container text-[11px] px-3 py-1 rounded-full font-medium">
                {isExecutiveCommittee(currentUser) ? committeePostLabel(currentUser.committeePost, lang) : memberClassLabel(currentUser.memberClass, lang)}
              </span>
            </div>
          </div>
        </div>

        <div className="mt-8 pt-6 border-t border-outline-variant flex justify-end">
          <button
            onClick={() => setIsEditing(true)}
            className="flex items-center gap-2 bg-primary text-white px-6 py-2.5 rounded-full font-label-lg text-label-lg active:scale-95 transition-transform shadow-md"
          >
            <Icon name="edit" className="text-sm" />
            Edit Profile
          </button>
        </div>
      </div>

      {/* Details Bento Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-8">
        <div className="bg-white dark:bg-surface-container rounded-xl p-5 shadow-[0_2px_8px_rgba(0,0,0,0.04)] border border-outline-variant/20">
          <h3 className="text-primary font-semibold text-sm mb-4 uppercase tracking-wider flex items-center gap-2">
            <Icon name="person_outline" className="text-lg" />
            Basic Information
          </h3>
          <div className="space-y-4">
            <Detail label="Name (English/Bengali)" value={`${currentUser.nameEn || '—'} / ${currentUser.nameBn || '—'}`} />
            <Detail label="Date of Birth" value={currentUser.dob || '—'} />
            <Detail label="Profession" value={currentUser.professionEn || '—'} />
          </div>
        </div>

        <div className="bg-white dark:bg-surface-container rounded-xl p-5 shadow-[0_2px_8px_rgba(0,0,0,0.04)] border border-outline-variant/20">
          <h3 className="text-primary font-semibold text-sm mb-4 uppercase tracking-wider flex items-center gap-2">
            <Icon name="home_pin" className="text-lg" />
            Residence Info
          </h3>
          <div className="space-y-4">
            <Detail label="Unit Address" value={[currentUser.block && `Block ${currentUser.block}`, currentUser.floor, currentUser.holding && `Holding ${currentUser.holding}`].filter(Boolean).join(', ') || '—'} />
            <Detail label="Resident Since" value={currentUser.joinedDate || '—'} />
            <Detail label="Emergency Contact" value={currentUser.emergencyContact || '—'} valueClassName="text-error" />
          </div>
        </div>
      </div>

      {/* Quick links */}
      <div className="space-y-3 mb-8">
        <QuickLink icon="payments" label={t.navFinancials} onClick={() => setActiveTab('financials')} />
        <QuickLink icon="support_agent" label={t.navComplaints} onClick={() => setActiveTab('complaints')} />
        {isExecutiveCommittee(currentUser) && (
          <QuickLink icon="shield" label={t.adminDashboard} onClick={() => setActiveTab('admin')} />
        )}
      </div>

      <button
        onClick={async () => { await signOut(); setCurrentUser(null); }}
        className="w-full h-14 rounded-full border border-error text-error font-label-lg flex items-center justify-center gap-2 active:scale-95 transition-all"
      >
        <Icon name="logout" />
        {t.logout}
      </button>
    </div>
  );
}

function FormField({ label, value, onChange, type = 'text', placeholder }) {
  return (
    <div className="relative">
      <label className="absolute -top-2 left-3 px-1 bg-surface dark:bg-inverse-surface text-[10px] font-semibold text-primary uppercase">{label}</label>
      <input
        type={type}
        value={value}
        placeholder={placeholder}
        onChange={(e) => onChange(e.target.value)}
        className="w-full h-14 bg-surface-container-low border-b-2 border-primary rounded-t-lg px-4 pt-2 font-medium focus:ring-0 focus:border-primary-container outline-none transition-all"
      />
    </div>
  );
}

function Detail({ label, value, valueClassName = '' }) {
  return (
    <div>
      <p className="text-xs text-on-surface-variant">{label}</p>
      <p className={`font-medium ${valueClassName}`}>{value}</p>
    </div>
  );
}

function QuickLink({ icon, label, onClick }) {
  return (
    <button
      onClick={onClick}
      className="w-full flex items-center justify-between bg-white dark:bg-surface-container p-4 rounded-xl border border-outline-variant/20 shadow-[0_2px_8px_rgba(0,0,0,0.04)] active:scale-[0.98] transition-all"
    >
      <div className="flex items-center gap-3">
        <div className="p-2 bg-secondary-container rounded-lg text-on-secondary-container">
          <Icon name={icon} />
        </div>
        <span className="font-label-lg text-on-surface">{label}</span>
      </div>
      <Icon name="chevron_right" className="text-on-surface-variant" />
    </button>
  );
}
