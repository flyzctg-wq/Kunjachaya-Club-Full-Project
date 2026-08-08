import React, { useState } from 'react';
import Icon from '../components/Icon';
import { translations } from '../translations';
import { signIn, register } from '../services/authService';

export default function AuthModal({ lang, setCurrentUser, setShowAuthModal }) {
  const t = translations[lang];
  const [mode, setMode] = useState('signin'); // 'signin' | 'register'
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);
    try {
      const user = mode === 'signin'
        ? await signIn(email.trim(), password)
        : await register(name, email.trim(), phone, password);
      setCurrentUser(user);
      setShowAuthModal(false);
    } catch (err) {
      console.error('Auth error:', err);
      let raw = err?.message || '';
      let msg = raw;
      if (raw.includes('invalid-credential') || raw.includes('wrong-password') || raw.includes('user-not-found') || raw.includes('invalid-email')) {
        msg = lang === 'bn'
          ? 'ইমেইল অথবা পাসওয়ার্ড সঠিক নয়। নতুন একাউন্ট খুলতে উপরে "নিবন্ধন" ট্যাবে চাপ দিন।'
          : 'Invalid email or password. Click the "Register" tab to create a new account.';
      } else if (raw.includes('email-already-in-use')) {
        msg = lang === 'bn'
          ? 'এই ইমেইল দিয়ে ইতিমধ্যে একাউন্ট তৈরি করা আছে। অনুগ্রহ করে "সাইন ইন" ট্যাবে যান।'
          : 'An account already exists with this email. Please click the "Login" tab.';
      } else if (raw.includes('weak-password')) {
        msg = lang === 'bn'
          ? 'পাসওয়ার্ড অন্তত ৬ অক্ষরের হতে হবে।'
          : 'Password must be at least 6 characters.';
      }
      setError(msg || 'Authentication failed');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-inverse-surface/60 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-surface-container-lowest w-full max-w-md rounded-[32px] shadow-[0_2px_8px_rgba(0,0,0,0.04)] p-8">

        <button
          onClick={() => setShowAuthModal(false)}
          className="absolute mt-[-8px] ml-[calc(100%-40px)] p-1 text-on-surface-variant hover:text-on-surface"
        >
          <Icon name="close" />
        </button>

        {/* Branding */}
        <div className="text-center mb-lg">
          <img src="/logo.png" alt="Kunjachaya Club Logo" className="w-20 h-20 object-contain mx-auto mb-md drop-shadow-md" />
          <h1 className="font-display-lg text-headline-lg-mobile text-on-surface tracking-tight">Kunjachaya Club</h1>
          <p className="font-body-md text-on-surface-variant mt-xs">Welcome to your sanctuary</p>
        </div>

        {/* Tabs */}
        <div className="flex bg-surface-container-high p-1 rounded-full mb-lg">
          <button
            type="button"
            onClick={() => { setMode('signin'); setError(''); }}
            className={`flex-1 py-2 rounded-full font-label-lg transition-all ${mode === 'signin' ? 'bg-secondary-container text-on-secondary-container shadow-sm' : 'text-on-surface-variant hover:bg-surface-variant/50'}`}
          >
            {lang === 'bn' ? 'সাইন ইন' : 'Login'}
          </button>
          <button
            type="button"
            onClick={() => { setMode('register'); setError(''); }}
            className={`flex-1 py-2 rounded-full font-label-lg transition-all ${mode === 'register' ? 'bg-secondary-container text-on-secondary-container shadow-sm' : 'text-on-surface-variant hover:bg-surface-variant/50'}`}
          >
            {lang === 'bn' ? 'নিবন্ধন' : 'Register'}
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-md">
          {mode === 'register' && (
            <div className="space-y-xs">
              <label className="font-label-lg text-on-surface-variant px-md">Full Name</label>
              <div className="relative">
                <Icon name="person" className="absolute left-md top-1/2 -translate-y-1/2 text-on-surface-variant" />
                <input
                  type="text"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  required
                  className="w-full pl-12 pr-md py-4 rounded-xl bg-surface-container border-none focus:ring-2 focus:ring-primary-container text-body-lg placeholder:text-outline-variant"
                />
              </div>
            </div>
          )}

          <div className="space-y-xs">
            <label className="font-label-lg text-on-surface-variant px-md">Email</label>
            <div className="relative">
              <Icon name="mail" className="absolute left-md top-1/2 -translate-y-1/2 text-on-surface-variant" />
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                className="w-full pl-12 pr-md py-4 rounded-xl bg-surface-container border-none focus:ring-2 focus:ring-primary-container text-body-lg placeholder:text-outline-variant"
              />
            </div>
          </div>

          {mode === 'register' && (
            <div className="space-y-xs">
              <label className="font-label-lg text-on-surface-variant px-md">Phone Number</label>
              <div className="relative">
                <Icon name="phone" className="absolute left-md top-1/2 -translate-y-1/2 text-on-surface-variant" />
                <input
                  type="tel"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  placeholder="01XXX-XXXXXX"
                  className="w-full pl-12 pr-md py-4 rounded-xl bg-surface-container border-none focus:ring-2 focus:ring-primary-container text-body-lg placeholder:text-outline-variant"
                />
              </div>
            </div>
          )}

          <div className="space-y-xs">
            <label className="font-label-lg text-on-surface-variant px-md">Password</label>
            <div className="relative">
              <Icon name="lock" className="absolute left-md top-1/2 -translate-y-1/2 text-on-surface-variant" />
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                minLength={6}
                className="w-full pl-12 pr-md py-4 rounded-xl bg-surface-container border-none focus:ring-2 focus:ring-primary-container text-body-lg placeholder:text-outline-variant"
              />
            </div>
          </div>

          {error && <p className="text-xs text-error px-md">{error}</p>}

          <button
            type="submit"
            disabled={isLoading}
            className="w-full bg-primary text-white font-label-lg py-4 rounded-full shadow-lg active:scale-95 transition-all hover:brightness-110 flex items-center justify-center gap-2 disabled:opacity-60"
          >
            {isLoading && <Icon name="progress_activity" className="animate-spin" />}
            {mode === 'signin' ? (lang === 'bn' ? 'সাইন ইন করুন' : 'Sign In') : (lang === 'bn' ? 'নিবন্ধন করুন' : 'Create Account')}
          </button>
        </form>

        {mode === 'register' && (
          <div className="mt-lg bg-secondary-fixed text-on-secondary-fixed p-md rounded-xl flex items-start gap-md">
            <Icon name="info" filled className="mt-1" />
            <p className="font-label-lg">
              {lang === 'bn'
                ? 'নতুন অ্যাকাউন্ট "অনুমোদনের অপেক্ষায়" অবস্থায় শুরু হবে; কার্যনির্বাহী কমিটি অনুমোদনের পর সম্পূর্ণ অ্যাক্সেস পাবেন।'
                : 'New accounts start as "Pending" — full access unlocks once an Executive Committee member approves your membership.'}
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
