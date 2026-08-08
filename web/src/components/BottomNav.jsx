import React from 'react';
import Icon from './Icon';

const items = [
  { key: 'dashboard', icon: 'home', labelEn: 'Home', labelBn: 'নীড়' },
  { key: 'events', icon: 'calendar_month', labelEn: 'Events', labelBn: 'অনুষ্ঠান' },
  { key: 'notices', icon: 'campaign', labelEn: 'Notices', labelBn: 'নোটিশ' },
  { key: 'directory', icon: 'group', labelEn: 'Members', labelBn: 'সদস্য' },
  { key: 'profile', icon: 'person', labelEn: 'Profile', labelBn: 'প্রোফাইল' },
];

export default function BottomNav({ activeTab, setActiveTab, lang }) {
  return (
    <nav className="fixed bottom-0 left-0 w-full flex justify-around items-center pt-3 pb-6 px-2 bg-surface dark:bg-surface-dim shadow-lg rounded-t-xl z-50">
      {items.map((item) => {
        const isActive = activeTab === item.key;
        return (
          <button
            key={item.key}
            onClick={() => setActiveTab(item.key)}
            className={`
              flex flex-col items-center justify-center transition-all duration-200 active:scale-90 p-2 rounded-full
              ${isActive ? 'bg-secondary-container text-on-secondary-container px-5' : 'text-on-surface-variant hover:bg-surface-variant/50'}
            `}
          >
            <Icon name={item.icon} filled={isActive} className={isActive ? 'text-primary' : ''} />
            <span className="font-label-sm text-label-sm">
              {lang === 'bn' ? item.labelBn : item.labelEn}
            </span>
          </button>
        );
      })}
    </nav>
  );
}
