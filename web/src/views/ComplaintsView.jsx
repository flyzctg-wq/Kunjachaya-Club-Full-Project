import React, { useState } from 'react';
import Icon from '../components/Icon';
import { translations } from '../translations';
import { addComplaint } from '../services/firestoreService';

const categoryIcons = {
  Plumbing: 'plumbing',
  Electrical: 'electric_bolt',
  Security: 'security',
  Elevator: 'elevator',
  Cleanliness: 'cleaning_services',
};

export default function ComplaintsView({ lang, currentUser, complaints }) {
  const t = translations[lang];

  const [showNewModal, setShowNewModal] = useState(false);
  const [category, setCategory] = useState('Plumbing');
  const [titleInput, setTitleInput] = useState('');
  const [descInput, setDescInput] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState('');

  const resolved = complaints.filter(c => c.status === 'Resolved');
  const pending = complaints.filter(c => c.status !== 'Resolved');

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!titleInput.trim() || !descInput.trim() || !currentUser) return;
    setIsSubmitting(true);
    setError('');
    try {
      const now = new Date().toISOString();
      await addComplaint({
        userId: currentUser.id,
        userNameEn: currentUser.nameEn || '',
        userNameBn: currentUser.nameBn || '',
        holdingNo: currentUser.holding || '',
        titleEn: titleInput,
        titleBn: titleInput,
        categoryEn: category,
        categoryBn: category,
        descriptionEn: descInput,
        descriptionBn: descInput,
        imageUrl: '',
        adminNoteEn: '',
        adminNoteBn: '',
        createdAt: now,
        updatedAt: now,
      });
      setTitleInput('');
      setDescInput('');
      setShowNewModal(false);
    } catch (err) {
      setError(err?.message || 'Failed to submit complaint');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <>
      <div className="mb-8">
        <h2 className="font-headline-lg text-headline-lg-mobile text-on-surface mb-2">{t.navComplaints}</h2>
        <p className="font-body-md text-body-md text-on-surface-variant">
          Track and manage your service requests for a peaceful living environment.
        </p>
      </div>

      <div className="grid grid-cols-3 gap-3 mb-8">
        <div className="bg-surface-container-low p-4 rounded-xl flex flex-col items-center">
          <span className="text-primary font-bold text-lg">{complaints.length}</span>
          <span className="font-label-sm text-label-sm text-on-surface-variant">Total</span>
        </div>
        <div className="bg-primary-container/10 p-4 rounded-xl flex flex-col items-center">
          <span className="text-primary font-bold text-lg">{pending.length}</span>
          <span className="font-label-sm text-label-sm text-on-surface-variant">Pending</span>
        </div>
        <div className="bg-secondary-container p-4 rounded-xl flex flex-col items-center">
          <span className="text-on-secondary-container font-bold text-lg">{resolved.length}</span>
          <span className="font-label-sm text-label-sm text-on-surface-variant">Resolved</span>
        </div>
      </div>

      <div className="space-y-4">
        {complaints.map((c) => {
          const isResolved = c.status === 'Resolved';
          return (
            <div
              key={c._docId || c.id}
              className={`p-5 rounded-[24px] card-shadow border relative overflow-hidden group active:scale-[0.98] transition-transform ${
                isResolved ? 'bg-surface-container-low opacity-80 border-surface-variant' : 'bg-white dark:bg-surface-container-lowest border-surface-variant/50'
              }`}
            >
              <div className="flex justify-between items-start mb-3">
                <div className="flex items-center gap-2">
                  <Icon name={categoryIcons[c.categoryEn] || 'build'} filled className={`text-sm ${isResolved ? 'text-on-surface-variant' : 'text-primary'}`} />
                  <span className={`font-label-lg text-label-lg tracking-wide ${isResolved ? 'text-on-surface-variant' : 'text-primary'}`}>
                    {(c.categoryEn || '').toUpperCase()}
                  </span>
                </div>
                <span className={`text-[11px] font-bold px-3 py-1 rounded-full uppercase tracking-wider ${
                  isResolved ? 'bg-secondary-container text-on-secondary-container'
                  : c.status === 'Under Review' ? 'bg-tertiary-container text-on-tertiary-container'
                  : 'bg-error-container text-on-error-container'
                }`}>
                  {c.status}
                </span>
              </div>
              <h3 className="font-title-lg text-title-lg text-on-surface mb-2">
                {lang === 'bn' ? (c.titleBn || c.titleEn) : c.titleEn}
              </h3>
              <p className="font-body-md text-body-md text-on-surface-variant line-clamp-2">
                {lang === 'bn' ? (c.descriptionBn || c.descriptionEn) : c.descriptionEn}
              </p>

              {(c.adminNoteEn || c.adminNoteBn) && (
                <div className="mt-3 p-3 bg-surface-container-low rounded-xl flex items-start gap-2">
                  <Icon name="forum" className="text-primary text-sm mt-0.5" />
                  <p className="text-label-sm font-label-sm text-on-surface-variant">
                    <span className="font-bold text-on-surface">Management: </span>
                    {lang === 'bn' ? (c.adminNoteBn || c.adminNoteEn) : c.adminNoteEn}
                  </p>
                </div>
              )}

              <div className="mt-4 flex items-center justify-between">
                <span className="text-label-sm text-on-surface-variant/70 font-label-sm italic">
                  {c.holdingNo ? `${c.holdingNo} • ` : ''}{(c.createdAt || '').slice(0, 10)}
                </span>
              </div>
            </div>
          );
        })}

        {complaints.length === 0 && (
          <div className="text-center py-16 text-on-surface-variant">
            <Icon name="support_agent" className="text-4xl mb-2 opacity-40" />
            <p>No complaints yet.</p>
          </div>
        )}
      </div>

      {/* FAB */}
      <button
        onClick={() => setShowNewModal(true)}
        className="fixed bottom-28 right-6 w-14 h-14 rounded-2xl bg-primary text-white shadow-xl flex items-center justify-center active:scale-90 transition-all z-40"
      >
        <Icon name="add" style={{ fontSize: '32px' }} />
      </button>

      {/* New Ticket Modal */}
      {showNewModal && (
        <div className="fixed inset-0 z-50 bg-inverse-surface/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-surface-container-lowest w-full max-w-lg rounded-[24px] p-6 shadow-2xl">
            <div className="flex items-center justify-between pb-4 border-b border-outline-variant/30">
              <div className="flex items-center gap-2 font-title-lg text-title-lg text-on-surface">
                <Icon name="support_agent" className="text-primary" />
                <span>{t.submitNewComplaint}</span>
              </div>
              <button onClick={() => setShowNewModal(false)} className="p-1 text-on-surface-variant hover:text-on-surface">
                <Icon name="close" />
              </button>
            </div>

            <form onSubmit={handleSubmit} className="mt-4 space-y-4">
              <div className="space-y-xs">
                <label className="font-label-lg text-on-surface-variant px-1">{t.issueCategory}</label>
                <select
                  value={category}
                  onChange={(e) => setCategory(e.target.value)}
                  className="w-full px-4 py-3 bg-surface-container rounded-xl border-none focus:ring-2 focus:ring-primary-container text-body-md"
                >
                  <option value="Plumbing">{t.plumbing}</option>
                  <option value="Electrical">{t.electrical}</option>
                  <option value="Security">{t.security}</option>
                  <option value="Elevator">{t.elevator}</option>
                  <option value="Cleanliness">{t.cleanliness}</option>
                </select>
              </div>

              <div className="space-y-xs">
                <label className="font-label-lg text-on-surface-variant px-1">{t.title}</label>
                <input
                  type="text"
                  required
                  value={titleInput}
                  onChange={(e) => setTitleInput(e.target.value)}
                  placeholder="e.g. Water leak in kitchen"
                  className="w-full px-4 py-3 bg-surface-container rounded-xl border-none focus:ring-2 focus:ring-primary-container text-body-md"
                />
              </div>

              <div className="space-y-xs">
                <label className="font-label-lg text-on-surface-variant px-1">{t.description}</label>
                <textarea
                  rows="3"
                  required
                  value={descInput}
                  onChange={(e) => setDescInput(e.target.value)}
                  placeholder="Provide details for technician..."
                  className="w-full px-4 py-3 bg-surface-container rounded-xl border-none focus:ring-2 focus:ring-primary-container text-body-md"
                />
              </div>

              {error && <p className="text-xs text-error">{error}</p>}

              <button
                type="submit"
                disabled={isSubmitting}
                className="w-full py-4 bg-primary text-white font-label-lg rounded-full flex items-center justify-center gap-2 shadow-lg active:scale-95 transition-all disabled:opacity-60"
              >
                {isSubmitting ? <Icon name="progress_activity" className="animate-spin" /> : <Icon name="send" />}
                <span>{t.submit}</span>
              </button>
            </form>
          </div>
        </div>
      )}
    </>
  );
}
