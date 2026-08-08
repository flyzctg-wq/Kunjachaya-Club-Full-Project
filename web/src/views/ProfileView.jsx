import React, { useState, useRef } from 'react';
import Icon from '../components/Icon';
import { translations } from '../translations';
import { signOut } from '../services/authService';
import { updateUser } from '../services/firestoreService';
import { storage } from '../firebase';
import { ref, uploadBytes, getDownloadURL } from 'firebase/storage';
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
  const [isUploadingPhoto, setIsUploadingPhoto] = useState(false);
  const [photoError, setPhotoError] = useState('');
  const fileInputRef = useRef(null);

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

  const handlePhotoChange = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (file.size > 5 * 1024 * 1024) {
      setPhotoError(lang === 'bn' ? 'ছবির সাইজ ৫ MB-এর বেশি হবে না' : 'Photo must be under 5 MB');
      return;
    }
    setPhotoError('');
    setIsUploadingPhoto(true);
    try {
      const storageRef = ref(storage, `profile_pics/${currentUser.id}`);
      await uploadBytes(storageRef, file);
      const url = await getDownloadURL(storageRef);
      await updateUser(currentUser.id, { profilePicUrl: url });
      setCurrentUser((prev) => ({ ...prev, profilePicUrl: url }));
    } catch (err) {
      console.error('Photo upload error:', err);
      setPhotoError(lang === 'bn' ? 'ছবি আপলোড ব্যর্থ হয়েছে' : 'Photo upload failed');
    } finally {
      setIsUploadingPhoto(false);
    }
  };

  if (isEditing) {
    return (
      <div>
        <div className="flex items-center gap-4 mb-6">
          <button onClick={() => setIsEditing(false)} className="p-2 rounded-full hover:bg-surface-variant transition-colors">
            <Icon name="arrow_back" />
          </button>
          <h2 className="font-title-lg text-title-lg">{lang === 'bn' ? 'প্রোফাইল সম্পাদনা' : 'Edit Profile'}</h2>
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
            {lang === 'bn' ? 'বাতিল' : 'Cancel'}
          </button>
          <button
            onClick={handleSave}
            disabled={isSaving}
            className="flex-1 h-14 rounded-full bg-primary text-white font-label-lg shadow-lg shadow-primary/20 active:scale-95 transition-all disabled:opacity-60"
          >
            {isSaving ? (lang === 'bn' ? 'সংরক্ষণ হচ্ছে...' : 'Saving...') : (lang === 'bn' ? 'পরিবর্তন সংরক্ষণ' : 'Save Changes')}
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

          {/* Avatar with upload button */}
          <div className="relative group">
            <div
              className="w-24 h-24 rounded-full border-4 border-secondary-container overflow-hidden bg-surface-variant flex items-center justify-center cursor-pointer"
              onClick={() => fileInputRef.current?.click()}
              title={lang === 'bn' ? 'ছবি পরিবর্তন করুন' : 'Change photo'}
            >
              {isUploadingPhoto ? (
                <div className="flex flex-col items-center justify-center gap-1">
                  <div className="w-6 h-6 border-2 border-primary border-t-transparent rounded-full animate-spin" />
                  <span className="text-[9px] text-on-surface-variant">Uploading</span>
                </div>
              ) : currentUser.profilePicUrl ? (
                <img
                  src={currentUser.profilePicUrl}
                  alt={currentUser.nameEn || 'Profile'}
                  className="w-full h-full object-cover"
                />
              ) : (
                <Icon name="person" className="text-5xl text-on-surface-variant" />
              )}
            </div>

            {/* Camera overlay on hover */}
            <div
              onClick={() => fileInputRef.current?.click()}
              className="absolute inset-0 rounded-full bg-black/30 opacity-0 group-hover:opacity-100 transition-opacity cursor-pointer flex items-center justify-center"
            >
              <Icon name="photo_camera" className="text-white text-2xl" />
            </div>

            {/* Status badge */}
            <span className="absolute bottom-1 right-1 bg-primary text-white text-[10px] font-bold px-2 py-0.5 rounded-full border-2 border-white uppercase">
              {currentUser.membershipStatus}
            </span>

            <input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              className="hidden"
              onChange={handlePhotoChange}
            />
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
            {photoError && (
              <p className="text-error text-[11px] mt-1">{photoError}</p>
            )}
            <button
              onClick={() => fileInputRef.current?.click()}
              disabled={isUploadingPhoto}
              className="mt-2 flex items-center gap-1 text-primary text-[11px] font-medium hover:underline disabled:opacity-50"
            >
              <Icon name="photo_camera" className="text-[14px]" />
              {lang === 'bn' ? 'ছবি পরিবর্তন করুন' : 'Change photo'}
            </button>
          </div>
        </div>

        <div className="mt-8 pt-6 border-t border-outline-variant flex justify-end">
          <button
            onClick={() => setIsEditing(true)}
            className="flex items-center gap-2 bg-primary text-white px-6 py-2.5 rounded-full font-label-lg text-label-lg active:scale-95 transition-transform shadow-md"
          >
            <Icon name="edit" className="text-sm" />
            {lang === 'bn' ? 'প্রোফাইল সম্পাদনা' : 'Edit Profile'}
          </button>
        </div>
      </div>

      {/* Details Bento Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-8">
        <div className="bg-white dark:bg-surface-container rounded-2xl p-5 shadow-[0_2px_8px_rgba(0,0,0,0.04)] border border-outline-variant/20">
          <p className="flex items-center gap-2 font-title-sm text-title-sm text-on-surface-variant uppercase tracking-wider mb-4">
            <Icon name="person" className="text-sm" />
            {lang === 'bn' ? 'মৌলিক তথ্য' : 'Basic Information'}
          </p>
          <div className="space-y-3">
            <Detail label={lang === 'bn' ? 'নাম (ইংরেজি/বাংলা)' : 'Name (English/Bengali)'} value={`${currentUser.nameEn || '—'} / ${currentUser.nameBn || '—'}`} />
            <Detail label={lang === 'bn' ? 'জন্ম তারিখ' : 'Date of Birth'} value={currentUser.dob || '—'} />
            <Detail label={lang === 'bn' ? 'পেশা' : 'Profession'} value={currentUser.professionEn || '—'} />
          </div>
        </div>
        <div className="bg-white dark:bg-surface-container rounded-2xl p-5 shadow-[0_2px_8px_rgba(0,0,0,0.04)] border border-outline-variant/20">
          <p className="flex items-center gap-2 font-title-sm text-title-sm text-on-surface-variant uppercase tracking-wider mb-4">
            <Icon name="location_on" className="text-sm" />
            {lang === 'bn' ? 'বাসস্থান তথ্য' : 'Residence Info'}
          </p>
          <div className="space-y-3">
            <Detail
              label={lang === 'bn' ? 'ইউনিট ঠিকানা' : 'Unit Address'}
              value={[currentUser.holding, currentUser.floor, currentUser.block, currentUser.road].filter(Boolean).join(', ') || '—'}
            />
            <Detail label={lang === 'bn' ? 'সদস্যতার তারিখ' : 'Resident Since'} value={currentUser.joinedDate || '—'} />
            <Detail label={lang === 'bn' ? 'জরুরি যোগাযোগ' : 'Emergency Contact'} value={currentUser.emergencyContact || '—'} />
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

function FormField({ label, value, onChange, type = 'text', placeholder = '' }) {
  return (
    <div className="relative">
      <label className="absolute top-2 left-4 text-xs font-medium text-primary">{label}</label>
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
