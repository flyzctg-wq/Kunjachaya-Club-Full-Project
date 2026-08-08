import React, { useState } from 'react';
import Icon from '../components/Icon';
import { translations } from '../translations';

const categoryStyle = {
  'Urgent Notice': { border: 'border-error', badgeBg: 'bg-error-container', badgeText: 'text-on-error-container', icon: 'report' },
  'Maintenance': { border: 'border-[#FFB300]', badgeBg: 'bg-[#FFF3E0]', badgeText: 'text-[#E65100]', icon: 'build' },
  'General News': { border: 'border-tertiary', badgeBg: 'bg-tertiary-container', badgeText: 'text-on-tertiary-container', icon: 'event' },
};

export default function NoticesView({ lang, notices }) {
  const t = translations[lang];
  const [selectedCategory, setSelectedCategory] = useState('ALL');

  const filtered = notices.filter((n) => {
    if (selectedCategory === 'ALL') return true;
    return (n.categoryEn || '').toUpperCase().includes(selectedCategory);
  });

  return (
    <>
      <section className="mb-xl">
        <h2 className="font-headline-lg-mobile text-headline-lg-mobile text-on-surface mb-xs">{t.navNotices}</h2>
        <p className="text-on-surface-variant max-w-md">
          Stay updated with the latest announcements and happenings in Kunjachaya Club.
        </p>
      </section>

      <section className="flex gap-2 overflow-x-auto pb-4 no-scrollbar -mx-gutter px-gutter">
        {[
          { key: 'ALL', label: 'All Notices' },
          { key: 'URGENT', label: 'Urgent' },
          { key: 'MAINTENANCE', label: 'Maintenance' },
          { key: 'GENERAL', label: 'General' },
        ].map((chip) => (
          <button
            key={chip.key}
            onClick={() => setSelectedCategory(chip.key)}
            className={`flex-shrink-0 px-4 py-2 rounded-full font-label-lg text-label-lg transition-colors ${
              selectedCategory === chip.key
                ? 'bg-primary-container text-on-primary-container shadow-sm'
                : 'bg-surface-container text-on-surface-variant hover:bg-surface-container-high'
            }`}
          >
            {chip.label}
          </button>
        ))}
      </section>

      <div className="space-y-md">
        {filtered.map((n) => {
          const style = categoryStyle[n.categoryEn] || categoryStyle['General News'];
          return (
            <article
              key={n._docId || n.id}
              className={`bg-surface-container-lowest p-md rounded-xl card-shadow border-l-4 ${style.border} active:scale-[0.98] transition-transform`}
            >
              <div className="flex justify-between items-start mb-sm">
                <span className={`px-3 py-1 rounded-full ${style.badgeBg} ${style.badgeText} text-label-sm font-label-sm flex items-center gap-1`}>
                  <Icon name={style.icon} className="text-[14px]" />
                  {lang === 'bn' ? n.categoryBn : n.categoryEn}
                </span>
                <time className="text-label-sm font-label-sm text-on-surface-variant">{n.date}</time>
              </div>
              <h3 className="font-title-lg text-title-lg text-on-surface mb-xs leading-tight">
                {lang === 'bn' ? n.titleBn : n.titleEn}
              </h3>
              <p className="text-on-surface-variant line-clamp-2 mb-md">
                {lang === 'bn' ? n.descriptionBn : n.descriptionEn}
              </p>
              <div className="flex items-center gap-2">
                <div className="w-6 h-6 rounded-full bg-secondary-container flex items-center justify-center">
                  <Icon name="verified_user" className="text-[14px] text-on-secondary-container" />
                </div>
                <span className="text-label-sm font-label-sm text-on-surface font-semibold uppercase tracking-wider">
                  Executive Committee
                </span>
              </div>
            </article>
          );
        })}

        {filtered.length === 0 && (
          <div className="text-center py-16 text-on-surface-variant">
            <Icon name="campaign" className="text-4xl mb-2 opacity-40" />
            <p>No notices yet.</p>
          </div>
        )}
      </div>
    </>
  );
}
