import React, { useState, useMemo } from 'react';
import Icon from '../components/Icon';
import { translations } from '../translations';
import { updateEventReminder } from '../services/firestoreService';

const monthNames = ['January','February','March','April','May','June','July','August','September','October','November','December'];

export default function EventsView({ lang, currentUser, events }) {
  const t = translations[lang];
  const [viewDate, setViewDate] = useState(new Date());

  const eventDatesSet = useMemo(() => new Set(events.map(e => e.date)), [events]);

  const calendarDays = useMemo(() => {
    const year = viewDate.getFullYear();
    const month = viewDate.getMonth();
    const firstDay = new Date(year, month, 1);
    const startOffset = firstDay.getDay();
    const daysInMonth = new Date(year, month + 1, 0).getDate();
    const cells = [];
    for (let i = 0; i < startOffset; i++) cells.push(null);
    for (let d = 1; d <= daysInMonth; d++) {
      const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
      cells.push({ day: d, dateStr, hasEvent: eventDatesSet.has(dateStr) });
    }
    return cells;
  }, [viewDate, eventDatesSet]);

  const handleToggleReminder = async (evt) => {
    if (!evt._docId) return;
    try {
      await updateEventReminder(evt._docId, !evt.isReminderSet);
    } catch (err) {
      console.error('Failed to update reminder', err);
    }
  };

  const upcoming = [...events].sort((a, b) => (a.date || '').localeCompare(b.date || ''));

  return (
    <>
      {/* Month Calendar */}
      <section className="mt-4 mb-lg">
        <div className="bg-surface-container-lowest rounded-xl p-md shadow-[0_2px_8px_rgba(0,0,0,0.04)]">
          <div className="flex justify-between items-center mb-md px-2">
            <h2 className="font-title-lg text-title-lg text-on-surface">
              {monthNames[viewDate.getMonth()]} {viewDate.getFullYear()}
            </h2>
            <div className="flex gap-2">
              <button
                onClick={() => setViewDate(new Date(viewDate.getFullYear(), viewDate.getMonth() - 1, 1))}
                className="p-2 rounded-full hover:bg-surface-variant/50 transition-colors"
              >
                <Icon name="chevron_left" />
              </button>
              <button
                onClick={() => setViewDate(new Date(viewDate.getFullYear(), viewDate.getMonth() + 1, 1))}
                className="p-2 rounded-full hover:bg-surface-variant/50 transition-colors"
              >
                <Icon name="chevron_right" />
              </button>
            </div>
          </div>
          <div className="grid grid-cols-7 text-center mb-2">
            {['S', 'M', 'T', 'W', 'T', 'F', 'S'].map((d, i) => (
              <span key={i} className="text-on-surface-variant font-label-sm text-label-sm">{d}</span>
            ))}
          </div>
          <div className="grid grid-cols-7 text-center gap-y-2">
            {calendarDays.map((cell, i) => (
              <div key={i} className="relative py-2 flex justify-center items-center">
                {cell && (
                  cell.hasEvent
                    ? <span className="w-8 h-8 flex items-center justify-center bg-primary text-white rounded-full font-medium">{cell.day}</span>
                    : <span className="text-on-surface">{cell.day}</span>
                )}
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Upcoming Events List */}
      <section>
        <div className="flex justify-between items-center mb-md">
          <h3 className="font-title-lg text-title-lg text-on-surface">Upcoming Events</h3>
        </div>
        <div className="space-y-4">
          {upcoming.map((evt) => (
            <div
              key={evt._docId || evt.id}
              className="bg-surface-container-lowest rounded-xl overflow-hidden shadow-[0_2px_8px_rgba(0,0,0,0.04)] transition-transform active:scale-[0.98]"
            >
              <div className="p-md">
                <div className="flex justify-between items-start mb-1">
                  <h4 className="font-title-lg text-on-surface leading-tight">
                    {lang === 'bn' ? evt.titleBn : evt.titleEn}
                  </h4>
                  {evt.eventType && (
                    <span className="bg-secondary-container text-on-secondary-container px-3 py-1 rounded-full text-xs font-medium shrink-0 ml-2">
                      {evt.eventType}
                    </span>
                  )}
                </div>
                <p className="font-body-md text-body-md text-on-surface-variant mb-2 line-clamp-2">
                  {lang === 'bn' ? evt.descriptionBn : evt.descriptionEn}
                </p>
                <div className="space-y-1.5">
                  <div className="flex items-center text-on-surface-variant gap-2">
                    <Icon name="calendar_today" className="text-[18px]" />
                    <span className="font-body-md text-body-md">{evt.date} • {evt.time}</span>
                  </div>
                  <div className="flex items-center text-on-surface-variant gap-2">
                    <Icon name="location_on" className="text-[18px]" />
                    <span className="font-body-md text-body-md">{lang === 'bn' ? evt.locationBn : evt.locationEn}</span>
                  </div>
                  {evt.amount > 0 && (
                    <div className="flex items-center text-on-surface-variant gap-2">
                      <Icon name="payments" className="text-[18px]" />
                      <span className="font-body-md text-body-md">৳{evt.amount.toLocaleString()}</span>
                    </div>
                  )}
                </div>

                <div className="mt-4 flex items-center justify-end border-t border-outline-variant pt-3">
                  <button
                    onClick={() => handleToggleReminder(evt)}
                    className={`font-label-sm text-label-sm flex items-center gap-1.5 px-3 py-1.5 rounded-full transition-colors ${
                      evt.isReminderSet ? 'bg-primary text-white' : 'text-primary hover:bg-primary/10'
                    }`}
                  >
                    <Icon name={evt.isReminderSet ? 'notifications_active' : 'notifications_none'} filled={evt.isReminderSet} className="text-[16px]" />
                    {evt.isReminderSet ? 'Reminder Set' : 'Set Reminder'}
                  </button>
                </div>
              </div>
            </div>
          ))}

          {upcoming.length === 0 && (
            <div className="text-center py-16 text-on-surface-variant">
              <Icon name="event" className="text-4xl mb-2 opacity-40" />
              <p>No events scheduled.</p>
            </div>
          )}
        </div>
      </section>
    </>
  );
}
