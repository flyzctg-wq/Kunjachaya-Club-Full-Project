import React, { useState, useMemo } from 'react';
import Icon from '../components/Icon';
import { translations } from '../translations';

// Only real, verified national emergency numbers for Bangladesh — no
// fabricated on-site staff, guard, or technician contacts. Any club-specific
// emergency contacts (caretaker, security desk, on-call electrician, etc.)
// must be entered as real data by the Executive Committee before appearing
// here; this app never invents them.
const nationalEmergencyNumbers = [
  { roleEn: 'National Emergency (Police / Fire / Ambulance)', roleBn: 'জাতীয় জরুরি সেবা', phone: '999' },
  { roleEn: 'Fire Service & Civil Defence', roleBn: 'ফায়ার সার্ভিস', phone: '16163' },
];

export default function DirectoryView({ lang, users }) {
  const t = translations[lang];
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedBlock, setSelectedBlock] = useState('ALL');

  const members = (users || []).filter(u => u.membershipStatus === 'Active');
  const blocks = useMemo(() => {
    const set = new Set(members.map(m => m.block).filter(Boolean));
    return Array.from(set).sort();
  }, [members]);

  const filteredMembers = members.filter(m => {
    const q = searchQuery.toLowerCase();
    const matchesQuery = (m.nameEn || '').toLowerCase().includes(q) ||
      (m.nameBn || '').toLowerCase().includes(q) ||
      (m.holding || '').toLowerCase().includes(q) ||
      (m.floor || '').toLowerCase().includes(q);
    const matchesBlock = selectedBlock === 'ALL' || m.block === selectedBlock;
    return matchesQuery && matchesBlock;
  });

  return (
    <>
      {/* Emergency Hotlines — real national numbers only */}
      <div className="bg-tertiary text-white p-5 rounded-2xl shadow-lg mb-lg">
        <div className="flex items-center gap-2 mb-3">
          <Icon name="emergency" filled className="text-xl" />
          <h3 className="font-title-lg text-title-lg">{t.emergencyContacts}</h3>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          {nationalEmergencyNumbers.map((item, idx) => (
            <div key={idx} className="bg-white/10 p-3 rounded-xl flex items-center justify-between">
              <div>
                <p className="text-xs font-bold text-white/80">{lang === 'bn' ? item.roleBn : item.roleEn}</p>
                <p className="text-sm font-mono">{item.phone}</p>
              </div>
              <a href={`tel:${item.phone}`} className="p-2 bg-white/20 rounded-full active:scale-90 transition-transform">
                <Icon name="call" />
              </a>
            </div>
          ))}
        </div>
        <p className="text-[11px] text-white/70 mt-3">
          {lang === 'bn'
            ? 'ক্লাবের নিজস্ব যোগাযোগ নম্বর যোগ করতে কার্যনির্বাহী কমিটির সাথে যোগাযোগ করুন।'
            : "The club's own security/maintenance contacts aren't set up yet."}
        </p>
      </div>

      <section className="mb-lg">
        <div className="flex flex-col gap-2 mb-md">
          <h2 className="font-display-lg text-headline-lg text-on-surface">{t.navDirectory}</h2>
          <p className="font-body-md text-on-surface-variant">Connect with your community neighbors.</p>
        </div>

        <div className="relative flex items-center w-full mb-4">
          <Icon name="search" className="absolute left-4 text-on-surface-variant" />
          <input
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-12 pr-4 py-4 rounded-xl bg-surface-container border-none focus:ring-2 focus:ring-primary font-body-md shadow-sm outline-none"
            placeholder={t.searchMember}
            type="text"
          />
        </div>

        <div className="flex gap-2 overflow-x-auto pb-4 no-scrollbar">
          <button
            onClick={() => setSelectedBlock('ALL')}
            className={`px-4 py-2 rounded-full font-label-lg whitespace-nowrap active:scale-95 transition-transform ${
              selectedBlock === 'ALL' ? 'bg-primary-container text-on-primary-container' : 'bg-surface-container-high text-on-surface-variant'
            }`}
          >
            All Members ({members.length})
          </button>
          {blocks.map((b) => (
            <button
              key={b}
              onClick={() => setSelectedBlock(b)}
              className={`px-4 py-2 rounded-full font-label-lg whitespace-nowrap active:scale-95 transition-transform ${
                selectedBlock === b ? 'bg-primary-container text-on-primary-container' : 'bg-surface-container-high text-on-surface-variant'
              }`}
            >
              Block {b}
            </button>
          ))}
        </div>
      </section>

      <div className="space-y-3">
        {filteredMembers.map((m) => (
          <div key={m.id} className="bg-surface-container-lowest p-md rounded-xl flex items-center gap-4 shadow-[0_2px_8px_rgba(0,0,0,0.04)]">
            <div className="w-16 h-16 rounded-full flex-shrink-0 bg-secondary-container flex items-center justify-center">
              <Icon name="person" className="text-3xl text-on-secondary-container" />
            </div>
            <div className="flex-grow min-w-0">
              <h3 className="font-title-lg text-on-surface truncate">
                {lang === 'bn' ? (m.nameBn || m.nameEn) : (m.nameEn || m.nameBn)}
              </h3>
              <div className="flex flex-wrap gap-x-3 gap-y-1 mt-1 text-on-surface-variant font-body-md text-sm">
                {m.holding && (
                  <span className="flex items-center gap-1">
                    <Icon name="apartment" className="text-[18px]" />
                    Holding {m.holding}
                  </span>
                )}
                {(m.block || m.floor) && (
                  <span className="flex items-center gap-1">
                    <Icon name="layers" className="text-[18px]" />
                    {[m.block && `Block ${m.block}`, m.floor].filter(Boolean).join(', ')}
                  </span>
                )}
              </div>
            </div>
            <div className="flex gap-1.5 shrink-0">
              <a href={`tel:${m.phone}`} className="w-10 h-10 rounded-full bg-surface-container-low flex items-center justify-center text-primary active:scale-90 transition-transform">
                <Icon name="call" />
              </a>
              <a href={`https://wa.me/${(m.phone || '').replace(/[^0-9]/g, '')}`} target="_blank" rel="noreferrer" className="w-10 h-10 rounded-full bg-surface-container-low flex items-center justify-center text-primary active:scale-90 transition-transform">
                <Icon name="chat" />
              </a>
            </div>
          </div>
        ))}

        {filteredMembers.length === 0 && (
          <div className="text-center py-16 text-on-surface-variant">
            <Icon name="group_off" className="text-4xl mb-2 opacity-40" />
            <p>No members found.</p>
          </div>
        )}
      </div>
    </>
  );
}
