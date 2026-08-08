import React from 'react';
import Icon from './Icon';
import { translations } from '../translations';
import { signOut } from '../services/authService';
import { isExecutiveCommittee } from '../roles';

export default function Header({ lang, setLang, currentUser, setCurrentUser, setShowAuthModal }) {
  const t = translations[lang];

  return (
    <header className="fixed top-0 w-full z-50 bg-surface/80 dark:bg-inverse-surface/80 glass-nav flex justify-between items-center px-gutter h-16 shadow-sm">
      <div className="flex items-center gap-3">
        <img src="/logo.png" alt="Kunjachaya Club Logo" className="w-10 h-10 object-contain drop-shadow-sm" />
        <h1 className="font-display-lg text-headline-lg-mobile text-primary dark:text-primary-fixed tracking-tight">
          {t.appName || 'Kunjachaya'}
        </h1>
      </div>

      <div className="flex items-center gap-2">
        <button
          onClick={() => setLang(lang === 'en' ? 'bn' : 'en')}
          className="font-label-lg text-label-lg bg-secondary-container text-on-secondary-container px-4 py-2 rounded-full active:scale-95 transition-transform"
        >
          EN/বাংলা
        </button>

        {currentUser ? (
          <button
            onClick={async () => { await signOut(); setCurrentUser(null); }}
            className="p-2 rounded-full text-error hover:bg-error-container/40 transition"
            title={t.logout}
          >
            <Icon name="logout" />
          </button>
        ) : (
          <button
            onClick={() => setShowAuthModal(true)}
            className="font-label-lg text-label-lg bg-primary text-on-primary px-4 py-2 rounded-full active:scale-95 transition-transform"
          >
            {t.login}
          </button>
        )}
      </div>
    </header>
  );
}
