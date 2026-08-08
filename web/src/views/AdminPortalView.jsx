import React, { useState } from 'react';
import Icon from '../components/Icon';
import { translations } from '../translations';
import { addAnnouncement, addFinancialRecord, updateComplaintStatus, updateMembershipStatus, addActivityLog } from '../services/firestoreService';
import { memberClassLabel } from '../roles';

export default function AdminPortalView({
  lang,
  currentUser,
  notices,
  users,
  financials,
  complaints,
  activityLogs,
}) {
  const t = translations[lang];

  const [noticeTitle, setNoticeTitle] = useState('');
  const [noticeBody, setNoticeBody] = useState('');
  const [noticeCategory, setNoticeCategory] = useState('Urgent Notice');
  const [isPostingNotice, setIsPostingNotice] = useState(false);

  const [duesMonth, setDuesMonth] = useState('');
  const [duesAmount, setDuesAmount] = useState(100);
  const [isIssuingDues, setIsIssuingDues] = useState(false);

  const [adminStatusMsg, setAdminStatusMsg] = useState('');

  const activeMembers = (users || []).filter(u => u.membershipStatus === 'Active');
  const pendingMembers = (users || []).filter(u => u.membershipStatus === 'Pending');
  const totalCollected = (financials || [])
    .filter(f => f.status === 'Completed')
    .reduce((sum, f) => sum + (f.amount || 0), 0);
  const openComplaints = (complaints || []).filter(c => c.status !== 'Resolved');

  const flash = (msg) => {
    setAdminStatusMsg(msg);
    setTimeout(() => setAdminStatusMsg(''), 4000);
  };

  const logAction = async (titleEn, titleBn, detailsEn) => {
    await addActivityLog({
      actionType: 'ADMIN_ACTION',
      adminId: currentUser?.id || '',
      adminName: currentUser?.nameEn || '',
      titleEn, titleBn,
      detailsEn, detailsBn: detailsEn,
      timestamp: new Date().toISOString(),
      targetId: '',
    });
  };

  const handlePostNotice = async (e) => {
    e.preventDefault();
    if (!noticeTitle.trim() || !noticeBody.trim()) return;
    setIsPostingNotice(true);
    try {
      await addAnnouncement({
        titleEn: noticeTitle, titleBn: noticeTitle,
        descriptionEn: noticeBody, descriptionBn: noticeBody,
        categoryEn: noticeCategory, categoryBn: noticeCategory,
        date: new Date().toISOString().slice(0, 10),
        priority: noticeCategory === 'Urgent Notice' ? 'High' : 'Medium',
      });
      await logAction(`Posted notice: ${noticeTitle}`, `নোটিশ প্রকাশিত: ${noticeTitle}`, noticeBody);
      setNoticeTitle(''); setNoticeBody('');
      flash('Notice published to Firestore for all residents.');
    } catch (err) {
      flash(err.message || 'Failed to publish notice');
    } finally {
      setIsPostingNotice(false);
    }
  };

  const handleGenerateDues = async (e) => {
    e.preventDefault();
    if (!duesMonth.trim() || activeMembers.length === 0) return;
    setIsIssuingDues(true);
    try {
      await Promise.all(activeMembers.map((member) =>
        addFinancialRecord({
          userId: member.id,
          titleEn: `Monthly Membership Dues - ${duesMonth}`,
          titleBn: `মাসিক চাঁদা - ${duesMonth}`,
          amount: Number(duesAmount),
          type: 'Due',
          monthYear: duesMonth,
          date: new Date().toISOString().slice(0, 10),
          paymentGateway: '', transactionId: '', status: 'Pending',
        })
      ));
      await logAction(`Issued monthly dues for ${duesMonth}`, `মাসিক চাঁদা জারি: ${duesMonth}`, `৳${duesAmount} issued to ${activeMembers.length} active members`);
      flash(`Issued ৳${duesAmount} dues for ${duesMonth} to ${activeMembers.length} active members.`);
      setDuesMonth('');
    } catch (err) {
      flash(err.message || 'Failed to issue dues');
    } finally {
      setIsIssuingDues(false);
    }
  };

  const handleResolveComplaint = async (c) => {
    try {
      await updateComplaintStatus(c._docId, 'Resolved', 'Resolved by the Executive Committee.', 'কার্যনির্বাহী কমিটি কর্তৃক সমাধান করা হয়েছে।', new Date().toISOString());
      await logAction(`Resolved complaint: ${c.titleEn}`, `অভিযোগ সমাধান: ${c.titleEn}`, '');
    } catch (err) {
      flash(err.message || 'Failed to update complaint');
    }
  };

  const handleApproveMember = async (member) => {
    try {
      await updateMembershipStatus(member.id, 'Active');
      await logAction(`Approved membership: ${member.nameEn}`, `সদস্যপদ অনুমোদিত: ${member.nameBn || member.nameEn}`, `ধারা-১০ অনুযায়ী অনুমোদিত`);
      flash(`${member.nameEn || member.phone} approved as an active member (ধারা-১০).`);
    } catch (err) {
      flash(err.message || 'Failed to approve member');
    }
  };

  return (
    <>
      <div className="flex items-center gap-2 mb-6">
        <span className="px-2 py-0.5 bg-primary-container text-on-primary-container text-[10px] uppercase font-bold rounded tracking-wider">
          Admin Mode
        </span>
        <h2 className="font-title-lg text-title-lg text-on-surface">{t.adminDashboard}</h2>
      </div>

      {/* Stats Bento Grid */}
      <section className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-8">
        <StatCard icon="group" iconBg="bg-secondary-container" iconColor="text-on-secondary-container" label="Active Members" value={activeMembers.length} valueColor="text-primary" />
        <StatCard icon="campaign" iconBg="bg-error-container" iconColor="text-on-error-container" label="Open Complaints" value={openComplaints.length} valueColor="text-error" />
        <StatCard icon="payments" iconBg="bg-tertiary-container" iconColor="text-on-tertiary-container" label="Total Collected" value={`৳${totalCollected.toLocaleString()}`} valueColor="text-tertiary" />
      </section>

      {adminStatusMsg && (
        <div className="p-4 bg-primary text-white rounded-xl font-bold text-sm flex items-center gap-2 shadow-lg mb-6">
          <Icon name="check_circle" filled />
          <span>{adminStatusMsg}</span>
        </div>
      )}

      {/* Pending Member Approvals */}
      {pendingMembers.length > 0 && (
        <div className="bg-surface-container-lowest p-6 rounded-xl border border-outline-variant/30 shadow-[0_2px_8px_rgba(0,0,0,0.04)] space-y-3 mb-6">
          <h3 className="font-title-lg text-title-lg text-on-surface flex items-center gap-2">
            <Icon name="how_to_reg" className="text-primary" />
            Pending Approvals ({pendingMembers.length})
          </h3>
          <div className="divide-y divide-outline-variant/20">
            {pendingMembers.map((m) => (
              <div key={m.id} className="py-3 flex items-center justify-between gap-4">
                <div>
                  <p className="font-bold text-sm text-on-surface">{m.nameEn || m.phone}</p>
                  <span className="text-xs text-on-surface-variant">{m.phone} • {memberClassLabel(m.memberClass, lang)}</span>
                </div>
                <button
                  onClick={() => handleApproveMember(m)}
                  className="px-4 py-2 bg-primary text-white text-xs font-bold rounded-full active:scale-95 transition-transform shrink-0"
                >
                  Approve
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Admin Tools Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
        <div className="bg-surface-container-lowest p-6 rounded-xl border border-outline-variant/30 shadow-[0_2px_8px_rgba(0,0,0,0.04)] space-y-4">
          <div className="flex items-center gap-2 font-title-lg text-title-lg text-on-surface border-b border-outline-variant/20 pb-3">
            <Icon name="campaign" className="text-primary" />
            <span>{t.postNotice}</span>
          </div>
          <form onSubmit={handlePostNotice} className="space-y-3">
            <input type="text" required value={noticeTitle} onChange={(e) => setNoticeTitle(e.target.value)} placeholder="e.g. Annual General Body Meeting" className="w-full px-4 py-3 bg-surface-container rounded-xl border-none focus:ring-2 focus:ring-primary-container text-body-md" />
            <select value={noticeCategory} onChange={(e) => setNoticeCategory(e.target.value)} className="w-full px-4 py-3 bg-surface-container rounded-xl border-none focus:ring-2 focus:ring-primary-container text-body-md">
              <option value="Urgent Notice">Urgent Notice</option>
              <option value="Maintenance">Maintenance Alert</option>
              <option value="General News">General Announcement</option>
            </select>
            <textarea rows="3" required value={noticeBody} onChange={(e) => setNoticeBody(e.target.value)} placeholder="Notice details..." className="w-full px-4 py-3 bg-surface-container rounded-xl border-none focus:ring-2 focus:ring-primary-container text-body-md" />
            <button type="submit" disabled={isPostingNotice} className="w-full py-3 bg-primary text-white font-label-lg rounded-full flex items-center justify-center gap-2 shadow-md active:scale-95 transition-all disabled:opacity-60">
              {isPostingNotice ? <Icon name="progress_activity" className="animate-spin" /> : <Icon name="send" />}
              Publish Notice
            </button>
          </form>
        </div>

        <div className="bg-surface-container-lowest p-6 rounded-xl border border-outline-variant/30 shadow-[0_2px_8px_rgba(0,0,0,0.04)] space-y-4">
          <div className="flex items-center gap-2 font-title-lg text-title-lg text-on-surface border-b border-outline-variant/20 pb-3">
            <Icon name="account_balance_wallet" className="text-primary" />
            <span>{t.generateMonthlyDues}</span>
          </div>
          <form onSubmit={handleGenerateDues} className="space-y-3">
            <input type="text" required value={duesMonth} onChange={(e) => setDuesMonth(e.target.value)} placeholder="e.g. August 2026" className="w-full px-4 py-3 bg-surface-container rounded-xl border-none focus:ring-2 focus:ring-primary-container text-body-md" />
            <div>
              <input type="number" required value={duesAmount} onChange={(e) => setDuesAmount(e.target.value)} className="w-full px-4 py-3 bg-surface-container rounded-xl border-none focus:ring-2 focus:ring-primary-container text-body-md" />
              <p className="text-[11px] text-on-surface-variant mt-1">Constitution default: ৳100/month (ধারা-১০)</p>
            </div>
            <div className="p-3 bg-surface-container rounded-xl text-xs text-on-surface-variant">
              This will generate individual ledger invoices for all {activeMembers.length} active members.
            </div>
            <button type="submit" disabled={isIssuingDues || activeMembers.length === 0} className="w-full py-3 bg-tertiary text-white font-label-lg rounded-full flex items-center justify-center gap-2 shadow-md active:scale-95 transition-all disabled:opacity-60">
              {isIssuingDues ? <Icon name="progress_activity" className="animate-spin" /> : <Icon name="add" />}
              Issue Billing Invoice
            </button>
          </form>
        </div>
      </div>

      {/* Pending Tickets */}
      <div className="bg-surface-container-lowest p-6 rounded-xl border border-outline-variant/30 shadow-[0_2px_8px_rgba(0,0,0,0.04)] space-y-4">
        <h3 className="font-title-lg text-title-lg text-on-surface">Pending Resident Tickets ({openComplaints.length})</h3>
        <div className="divide-y divide-outline-variant/20">
          {openComplaints.map(c => (
            <div key={c._docId || c.id} className="py-3 flex items-center justify-between gap-4">
              <div>
                <span className="text-xs font-bold text-on-surface-variant">{c.holdingNo ? `${c.holdingNo} • ` : ''}{c.categoryEn}</span>
                <p className="font-bold text-sm text-on-surface">{c.titleEn}</p>
              </div>
              <button onClick={() => handleResolveComplaint(c)} className="px-4 py-2 bg-primary text-white text-xs font-bold rounded-full active:scale-95 transition-transform shrink-0">
                Mark Resolved
              </button>
            </div>
          ))}
          {openComplaints.length === 0 && (
            <p className="py-6 text-center text-sm text-on-surface-variant">No open tickets.</p>
          )}
        </div>
      </div>
    </>
  );
}

function StatCard({ icon, iconBg, iconColor, label, value, valueColor }) {
  return (
    <div className="bg-surface-container-lowest p-6 rounded-xl shadow-[0_2px_8px_rgba(0,0,0,0.04)] border border-outline-variant/30 flex flex-col gap-4 hover:shadow-lg transition-all duration-300">
      <div className={`w-fit p-3 ${iconBg} rounded-lg`}>
        <Icon name={icon} className={iconColor} />
      </div>
      <div>
        <p className="text-on-surface-variant font-label-lg">{label}</p>
        <h2 className={`font-display-lg text-headline-lg ${valueColor}`}>{value}</h2>
      </div>
    </div>
  );
}
