import React, { useState, useEffect } from 'react';
import Icon from '../components/Icon';
import {
  CommitteePost,
  MemberClass,
  committeePostLabels,
  memberClassLabels,
  isExecutiveCommittee,
  canAppointOfficers,
} from '../roles';
import { updateUserRoleAndPrivileges, addActivityLog } from '../services/firestoreService';

// ─── Constants ──────────────────────────────────────────────────────────────

const MEMBER_CLASS_META = [
  {
    key: MemberClass.GENERAL,
    icon: 'person',
    colorClass: 'text-secondary',
    bgClass: 'bg-secondary-container',
    borderClass: 'border-secondary',
    descEn: 'Active resident with full voting rights',
    descBn: 'ভোটাধিকারসহ সক্রিয় নিবাসী সদস্য',
  },
  {
    key: MemberClass.FOUNDING,
    icon: 'stars',
    colorClass: 'text-amber-600',
    bgClass: 'bg-amber-50',
    borderClass: 'border-amber-400',
    descEn: 'Automatically seats on the Standing Council (ধারা-১৩খ)',
    descBn: 'স্থায়ী পরিষদের সদস্য (ধারা-১৩খ)',
  },
  {
    key: MemberClass.LIFETIME,
    icon: 'workspace_premium',
    colorClass: 'text-amber-900',
    bgClass: 'bg-amber-100',
    borderClass: 'border-amber-700',
    descEn: 'May attend & speak, no vote or office (ধারা-৯খ)',
    descBn: 'উপস্থিত ও মতামত দিতে পারবেন, ভোটাধিকার নেই (ধারা-৯খ)',
  },
  {
    key: MemberClass.DONOR,
    icon: 'volunteer_activism',
    colorClass: 'text-teal-700',
    bgClass: 'bg-teal-50',
    borderClass: 'border-teal-400',
    descEn: 'May attend & speak, no vote or office (ধারা-৯খ)',
    descBn: 'উপস্থিত ও মতামত দিতে পারবেন, ভোটাধিকার নেই (ধারা-৯খ)',
  },
  {
    key: MemberClass.ADVISORY,
    icon: 'emoji_people',
    colorClass: 'text-purple-700',
    bgClass: 'bg-purple-50',
    borderClass: 'border-purple-400',
    descEn: 'Sits on the Advisory Council, max 15 (ধারা-১৩ক)',
    descBn: 'উপদেষ্টা পরিষদের সদস্য, সর্বোচ্চ ১৫ জন (ধারা-১৩ক)',
  },
  {
    key: MemberClass.NEW,
    icon: 'person_add',
    colorClass: 'text-teal-600',
    bgClass: 'bg-teal-50',
    borderClass: 'border-teal-400',
    descEn: 'Onboarding phase, awaiting EC approval (ধারা-১০)',
    descBn: 'অনবোর্ডিং ফেইজ, অনুমোদনের অপেক্ষায় (ধারা-১০)',
  },
];

const COMMITTEE_POSTS_ORDERED = [
  CommitteePost.PRESIDENT,
  CommitteePost.VICE_PRESIDENT,
  CommitteePost.GENERAL_SECRETARY,
  CommitteePost.ASSISTANT_GENERAL_SECRETARY,
  CommitteePost.TREASURER,
  CommitteePost.ORGANIZING_SECRETARY,
  CommitteePost.SOCIAL_WELFARE_SECRETARY,
  CommitteePost.LITERATURE_CULTURE_SECRETARY,
  CommitteePost.PUBLICITY_SECRETARY,
  CommitteePost.SPORTS_SECRETARY,
  CommitteePost.WOMENS_AFFAIRS_SECRETARY,
  CommitteePost.EXECUTIVE_MEMBER,
];

const TOP_POSTS = new Set([CommitteePost.PRESIDENT, CommitteePost.GENERAL_SECRETARY]);

function defaultPrivilegesForPost(post) {
  if (!post) return null;
  if (TOP_POSTS.has(post)) {
    return { canManageNotices: true, canManageComplaints: true, canManageMembers: true, canManageFinancials: true, canDeleteItems: true };
  }
  if (post === CommitteePost.TREASURER) return { canManageFinancials: true };
  if (post === CommitteePost.PUBLICITY_SECRETARY) return { canManageNotices: true };
  if (post === CommitteePost.SOCIAL_WELFARE_SECRETARY) return { canManageComplaints: true };
  return {};
}

// ─── Main Component ──────────────────────────────────────────────────────────

export default function RoleManagementPanel({ lang, currentUser, users, onClose }) {
  const [search, setSearch] = useState('');
  const [editingMember, setEditingMember] = useState(null);

  const canReassignPost = canAppointOfficers(currentUser);
  const isBn = lang === 'bn';

  const filteredUsers = (users || []).filter((u) => {
    if (!search.trim()) return true;
    const q = search.toLowerCase();
    return (
      (u.nameEn || '').toLowerCase().includes(q) ||
      (u.nameBn || '').includes(q) ||
      (u.phone || '').includes(q) ||
      (u.block || '').toLowerCase().includes(q) ||
      (u.holding || '').toLowerCase().includes(q)
    );
  });

  return (
    <div className="fixed inset-0 z-50 bg-black/40 backdrop-blur-sm flex items-stretch justify-end" onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}>
      <div className="bg-surface w-full max-w-lg flex flex-col shadow-2xl overflow-hidden animate-slide-in-right">

        {/* Header */}
        <div className="flex items-center gap-3 px-6 py-5 border-b border-outline-variant bg-surface-container-lowest shrink-0">
          <div className="p-2 bg-primary-container rounded-xl">
            <Icon name="admin_panel_settings" className="text-on-primary-container text-xl" />
          </div>
          <div className="flex-1 min-w-0">
            <h2 className="font-title-lg text-title-lg text-on-surface truncate">
              {isBn ? 'ভূমিকা ও প্রিভিলেজ ব্যবস্থাপনা' : 'Role & Privilege Management'}
            </h2>
            <p className="text-xs text-on-surface-variant">
              {isBn ? 'ধারা-১৭ অনুযায়ী — শুধুমাত্র সভাপতি/সাধারণ সম্পাদক' : 'ধারা-১৭ — President / General Secretary only for post reassignment'}
            </p>
          </div>
          <button onClick={onClose} className="p-2 rounded-full hover:bg-surface-variant transition-colors shrink-0">
            <Icon name="close" className="text-on-surface-variant" />
          </button>
        </div>

        {/* Search */}
        <div className="px-6 pt-4 pb-2 shrink-0">
          <div className="relative">
            <Icon name="search" className="absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant" />
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder={isBn ? 'নাম, ফোন বা ব্লক দিয়ে খুঁজুন...' : 'Search by name, phone or block...'}
              className="w-full pl-10 pr-4 py-3 rounded-xl bg-surface-container border border-outline-variant/30 focus:outline-none focus:ring-2 focus:ring-primary-container text-body-md"
            />
          </div>
          <p className="text-xs text-on-surface-variant mt-2 px-1">
            {filteredUsers.length} {isBn ? 'সদস্য পাওয়া গেছে' : 'members found'}
          </p>
        </div>

        {/* Member List */}
        <div className="flex-1 overflow-y-auto px-4 pb-6 space-y-2">
          {filteredUsers.map((member) => (
            <MemberRoleCard
              key={member.id}
              member={member}
              lang={lang}
              canReassignPost={canReassignPost}
              onEdit={() => setEditingMember(member)}
            />
          ))}
          {filteredUsers.length === 0 && (
            <div className="text-center py-16 text-on-surface-variant">
              <Icon name="manage_search" className="text-5xl mb-3 opacity-30" />
              <p className="font-label-lg">{isBn ? 'কোনো সদস্য পাওয়া যায়নি' : 'No members found'}</p>
            </div>
          )}
        </div>
      </div>

      {/* Edit Sheet — slides in on top */}
      {editingMember && (
        <EditRoleSheet
          member={editingMember}
          lang={lang}
          currentUser={currentUser}
          canReassignPost={canReassignPost}
          onClose={() => setEditingMember(null)}
          onSaved={(updated) => {
            setEditingMember(null);
          }}
        />
      )}
    </div>
  );
}

// ─── Member card in list ─────────────────────────────────────────────────────

function MemberRoleCard({ member, lang, canReassignPost, onEdit }) {
  const isBn = lang === 'bn';
  const postLabel = member.committeePost
    ? (committeePostLabels[member.committeePost]?.[lang] ?? member.committeePost)
    : null;
  const classLabel = memberClassLabels[member.memberClass]?.[lang] ?? member.memberClass;
  const isEC = isExecutiveCommittee(member);
  const isTop = TOP_POSTS.has(member.committeePost);

  return (
    <div className="flex items-center gap-3 bg-surface-container-lowest rounded-xl border border-outline-variant/20 px-4 py-3 hover:shadow-md transition-all group">
      {/* Avatar */}
      <div className="w-10 h-10 rounded-full bg-secondary-container flex items-center justify-center shrink-0 overflow-hidden">
        {member.profilePicUrl
          ? <img src={member.profilePicUrl} alt="" className="w-full h-full object-cover" />
          : <Icon name="person" className="text-on-secondary-container" />}
      </div>

      {/* Info */}
      <div className="flex-1 min-w-0">
        <p className="font-bold text-sm text-on-surface truncate">
          {isBn ? (member.nameBn || member.nameEn) : (member.nameEn || member.nameBn)}
        </p>
        <div className="flex flex-wrap gap-1.5 mt-1">
          {/* Status badge */}
          <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
            member.membershipStatus === 'Active'
              ? 'bg-green-100 text-green-700'
              : member.membershipStatus === 'Pending'
              ? 'bg-amber-100 text-amber-700'
              : 'bg-surface-variant text-on-surface-variant'
          }`}>
            {member.membershipStatus || 'Pending'}
          </span>
          {/* Class */}
          <span className="text-[10px] px-2 py-0.5 rounded-full bg-secondary-container text-on-secondary-container font-medium">
            {classLabel}
          </span>
          {/* Post */}
          {postLabel && (
            <span className={`text-[10px] px-2 py-0.5 rounded-full font-bold ${isTop ? 'bg-error-container text-on-error-container' : 'bg-primary-container text-on-primary-container'}`}>
              {postLabel}
            </span>
          )}
        </div>
        {/* Permission dots */}
        {isEC && (
          <div className="flex gap-1 mt-1.5">
            {[
              { key: 'canManageNotices', icon: 'campaign', label: 'Notices' },
              { key: 'canManageComplaints', icon: 'support_agent', label: 'Complaints' },
              { key: 'canManageMembers', icon: 'group', label: 'Members' },
              { key: 'canManageFinancials', icon: 'payments', label: 'Financials' },
              { key: 'canDeleteItems', icon: 'delete', label: 'Delete' },
            ].map(({ key, icon, label }) => (
              <span
                key={key}
                title={label}
                className={`w-5 h-5 rounded-full flex items-center justify-center ${member[key] ? 'bg-primary text-white' : 'bg-surface-variant text-on-surface-variant/40'}`}
              >
                <Icon name={icon} className="text-[9px]" />
              </span>
            ))}
          </div>
        )}
      </div>

      {/* Edit button */}
      <button
        onClick={onEdit}
        className="p-2 rounded-full opacity-0 group-hover:opacity-100 bg-surface-variant hover:bg-primary-container transition-all shrink-0"
        title={isBn ? 'ভূমিকা সম্পাদনা' : 'Edit role'}
      >
        <Icon name="edit" className="text-on-surface-variant text-sm" />
      </button>
    </div>
  );
}

// ─── Edit Role Sheet ─────────────────────────────────────────────────────────

function EditRoleSheet({ member, lang, currentUser, canReassignPost, onClose, onSaved }) {
  const isBn = lang === 'bn';

  const [selectedClass, setSelectedClass] = useState(member.memberClass || MemberClass.NEW);
  const [selectedPost, setSelectedPost] = useState(member.committeePost || '');
  const [canNotices, setCanNotices] = useState(!!member.canManageNotices);
  const [canComplaints, setCanComplaints] = useState(!!member.canManageComplaints);
  const [canMembers, setCanMembers] = useState(!!member.canManageMembers);
  const [canFinancials, setCanFinancials] = useState(!!member.canManageFinancials);
  const [canDelete, setCanDelete] = useState(!!member.canDeleteItems);
  const [isSaving, setIsSaving] = useState(false);
  const [statusMsg, setStatusMsg] = useState('');
  const [postLocked, setPostLocked] = useState(false);

  const isTopPost = TOP_POSTS.has(selectedPost);

  useEffect(() => {
    const defaults = defaultPrivilegesForPost(selectedPost);
    if (!defaults) return;
    if (TOP_POSTS.has(selectedPost)) {
      setCanNotices(true); setCanComplaints(true); setCanMembers(true);
      setCanFinancials(true); setCanDelete(true);
    } else {
      if (defaults.canManageFinancials) setCanFinancials(true);
      if (defaults.canManageNotices) setCanNotices(true);
      if (defaults.canManageComplaints) setCanComplaints(true);
    }
  }, [selectedPost]);

  const handlePostChange = (post) => {
    if (!canReassignPost) {
      setPostLocked(true);
      setTimeout(() => setPostLocked(false), 2500);
      return;
    }
    setSelectedPost(post);
  };

  const handleSave = async () => {
    setIsSaving(true);
    setStatusMsg('');
    try {
      await updateUserRoleAndPrivileges(member.id, {
        memberClass: selectedClass,
        committeePost: selectedPost,
        canManageNotices: canNotices,
        canManageComplaints: canComplaints,
        canManageMembers: canMembers,
        canManageFinancials: canFinancials,
        canDeleteItems: canDelete,
      });

      await addActivityLog({
        actionType: 'ROLE_UPDATE',
        adminId: currentUser?.id || '',
        adminName: currentUser?.nameEn || '',
        titleEn: `Role updated: ${member.nameEn || member.phone}`,
        titleBn: `ভূমিকা আপডেট: ${member.nameBn || member.nameEn || member.phone}`,
        detailsEn: `Class: ${selectedClass}, Post: ${selectedPost || 'None'}`,
        detailsBn: `শ্রেণি: ${selectedClass}, পদ: ${selectedPost || 'নেই'}`,
        targetId: member.id,
        timestamp: new Date().toISOString(),
      });

      setStatusMsg(isBn ? 'ফায়ারস্টোরে সংরক্ষিত হয়েছে ✓' : 'Saved to Firestore ✓');
      setTimeout(() => onSaved({ ...member, memberClass: selectedClass, committeePost: selectedPost, canManageNotices: canNotices, canManageComplaints: canComplaints, canManageMembers: canMembers, canManageFinancials: canFinancials, canDeleteItems: canDelete }), 900);
    } catch (err) {
      setStatusMsg(err.message || (isBn ? 'হালনাগাদ করতে ব্যর্থ হয়েছে' : 'Failed to update'));
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 z-[60] bg-black/50 backdrop-blur-sm flex items-end sm:items-center justify-center p-0 sm:p-4">
      <div className="bg-surface-container-lowest w-full max-w-lg rounded-t-3xl sm:rounded-3xl shadow-2xl max-h-[90vh] flex flex-col overflow-hidden">

        {/* Sheet Header */}
        <div className="flex items-start gap-3 p-6 border-b border-outline-variant shrink-0">
          <div className="w-12 h-12 rounded-full bg-secondary-container flex items-center justify-center overflow-hidden shrink-0">
            {member.profilePicUrl
              ? <img src={member.profilePicUrl} alt="" className="w-full h-full object-cover" />
              : <Icon name="person" className="text-on-secondary-container text-xl" />}
          </div>
          <div className="flex-1 min-w-0">
            <h3 className="font-title-lg text-title-lg text-on-surface truncate">
              {isBn ? (member.nameBn || member.nameEn) : (member.nameEn || member.nameBn)}
            </h3>
            <p className="text-xs text-on-surface-variant">
              ID: {member.id?.slice(0, 14)}… {member.phone ? `• ${member.phone}` : ''}
            </p>
          </div>
          <button onClick={onClose} className="p-2 rounded-full hover:bg-surface-variant shrink-0">
            <Icon name="close" className="text-on-surface-variant" />
          </button>
        </div>

        {/* Scrollable body */}
        <div className="flex-1 overflow-y-auto p-6 space-y-8">

          {/* ── Section 1: Member Class ── */}
          <section>
            <SectionHeader
              number="1"
              label={isBn ? 'সদস্য শ্রেণি (ধারা-৬)' : 'Member Class (ধারা-৬)'}
            />
            <div className="grid grid-cols-1 gap-2 mt-3">
              {MEMBER_CLASS_META.map((mc) => {
                const label = memberClassLabels[mc.key]?.[lang] ?? mc.key;
                const desc = isBn ? mc.descBn : mc.descEn;
                const selected = selectedClass === mc.key;
                return (
                  <RoleOption
                    key={mc.key}
                    icon={mc.icon}
                    iconColorClass={selected ? mc.colorClass : 'text-on-surface-variant'}
                    bgClass={selected ? mc.bgClass : 'bg-surface'}
                    borderClass={selected ? mc.borderClass : 'border-outline-variant/30'}
                    label={label}
                    desc={desc}
                    selected={selected}
                    onClick={() => setSelectedClass(mc.key)}
                  />
                );
              })}
            </div>
          </section>

          {/* ── Section 2: Committee Post ── */}
          <section>
            <SectionHeader
              number="2"
              label={isBn ? 'কার্যনির্বাহী পরিষদ পদ (ধারা-১৪, ঐচ্ছিক)' : 'Executive Committee Post (ধারা-১৪, optional)'}
            />
            {!canReassignPost && (
              <div className={`mt-2 flex items-center gap-2 px-3 py-2 rounded-lg text-xs font-medium transition-all ${postLocked ? 'bg-error-container text-on-error-container' : 'bg-surface-variant text-on-surface-variant'}`}>
                <Icon name="lock" className="text-sm" />
                {isBn
                  ? 'পদ পরিবর্তনের জন্য সভাপতি বা সাধারণ সম্পাদকের অনুমতি প্রয়োজন (ধারা-১৭)'
                  : 'Only the President or General Secretary can reassign a committee post (ধারা-১৭)'}
              </div>
            )}
            <div className="grid grid-cols-1 gap-2 mt-3">
              {/* No post option */}
              <RoleOption
                icon="person"
                iconColorClass={!selectedPost ? 'text-on-surface-variant' : 'text-on-surface-variant/50'}
                bgClass={!selectedPost ? 'bg-surface-variant' : 'bg-surface'}
                borderClass={!selectedPost ? 'border-outline' : 'border-outline-variant/30'}
                label={isBn ? 'কোনো কমিটি পদ নেই' : 'No committee post'}
                desc=""
                selected={!selectedPost}
                onClick={() => handlePostChange('')}
                disabled={!canReassignPost}
              />
              {COMMITTEE_POSTS_ORDERED.map((post) => {
                const label = committeePostLabels[post]?.[lang] ?? post;
                const isTop = TOP_POSTS.has(post);
                const selected = selectedPost === post;
                return (
                  <RoleOption
                    key={post}
                    icon={isTop ? 'verified_user' : 'security'}
                    iconColorClass={selected ? (isTop ? 'text-error' : 'text-primary') : 'text-on-surface-variant/50'}
                    bgClass={selected ? (isTop ? 'bg-error-container' : 'bg-primary-container') : 'bg-surface'}
                    borderClass={selected ? (isTop ? 'border-error' : 'border-primary') : 'border-outline-variant/30'}
                    label={label}
                    desc=""
                    selected={selected}
                    onClick={() => handlePostChange(post)}
                    disabled={!canReassignPost}
                  />
                );
              })}
            </div>
          </section>

          {/* ── Section 3: Moderation Privileges ── */}
          {selectedPost && (
            <section>
              <SectionHeader
                number="3"
                label={isBn ? 'নির্দিষ্ট মডারেশন প্রিভিলেজ' : 'Moderation Privileges'}
              />
              {isTopPost && (
                <div className="mt-2 flex items-center gap-2 px-3 py-2 rounded-lg text-xs font-medium bg-primary-container text-on-primary-container">
                  <Icon name="verified" className="text-sm" />
                  {isBn
                    ? 'সভাপতি ও সাধারণ সম্পাদক সকল প্রিভিলেজ স্বয়ংক্রিয়ভাবে পান (ধারা-১৭)'
                    : 'President & General Secretary automatically hold all privileges (ধারা-১৭)'}
                </div>
              )}
              <div className="mt-3 border border-outline-variant/30 rounded-xl overflow-hidden divide-y divide-outline-variant/20">
                {[
                  { label: isBn ? 'নোটিশ ও ব্রডকাস্ট ব্যবস্থাপনা' : 'Manage Notices & Broadcasts', icon: 'campaign', value: canNotices, set: setCanNotices },
                  { label: isBn ? 'অভিযোগ ও সার্ভিস রিকুয়েস্ট পর্যালোচনা' : 'Manage Complaints & Requests', icon: 'support_agent', value: canComplaints, set: setCanComplaints },
                  { label: isBn ? 'রেসিডেন্ট ডিরেক্টরি ও সদস্য অনুমোদন' : 'Manage Directory & Member Approvals', icon: 'group', value: canMembers, set: setCanMembers },
                  { label: isBn ? 'আর্থিক রেকর্ড ও রসিদ তৈরি' : 'Manage Financial Dues & PDF Receipts', icon: 'payments', value: canFinancials, set: setCanFinancials },
                  { label: isBn ? 'রেকর্ড মুছে ফেলার ক্ষমতা' : 'Delete Records Authority', icon: 'delete_forever', value: canDelete, set: setCanDelete },
                ].map(({ label, icon, value, set }) => (
                  <PrivilegeRow
                    key={label}
                    icon={icon}
                    label={label}
                    checked={value}
                    disabled={isTopPost}
                    onChange={set}
                  />
                ))}
              </div>
            </section>
          )}
        </div>

        {/* Footer */}
        <div className="p-5 border-t border-outline-variant bg-surface-container-lowest shrink-0 space-y-3">
          {statusMsg && (
            <p className={`text-sm font-medium text-center px-4 ${statusMsg.includes('✓') ? 'text-green-600' : 'text-error'}`}>
              {statusMsg}
            </p>
          )}
          <div className="flex gap-3">
            <button
              onClick={onClose}
              className="flex-1 h-12 rounded-full border border-outline text-on-surface-variant font-label-lg active:scale-95 transition-all"
            >
              {isBn ? 'বাতিল' : 'Cancel'}
            </button>
            <button
              onClick={handleSave}
              disabled={isSaving}
              className="flex-1 h-12 rounded-full bg-primary text-white font-label-lg shadow-lg shadow-primary/20 active:scale-95 transition-all disabled:opacity-60 flex items-center justify-center gap-2"
            >
              {isSaving && <Icon name="progress_activity" className="animate-spin text-sm" />}
              {isBn ? 'ফায়ারস্টোরে সংরক্ষণ করুন' : 'Save Privileges'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

// ─── Sub-components ──────────────────────────────────────────────────────────

function SectionHeader({ number, label }) {
  return (
    <div className="flex items-center gap-2">
      <span className="w-6 h-6 rounded-full bg-primary text-white text-xs font-bold flex items-center justify-center shrink-0">
        {number}
      </span>
      <p className="font-bold text-sm text-primary">{label}</p>
    </div>
  );
}

function RoleOption({ icon, iconColorClass, bgClass, borderClass, label, desc, selected, onClick, disabled = false }) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl border-2 text-left transition-all active:scale-[0.98] ${bgClass} ${borderClass} ${disabled ? 'opacity-60 cursor-not-allowed' : 'hover:brightness-95'}`}
    >
      <span className={`w-4 h-4 rounded-full border-2 flex items-center justify-center shrink-0 ${selected ? 'border-primary bg-primary' : 'border-outline-variant'}`}>
        {selected && <span className="w-2 h-2 rounded-full bg-white" />}
      </span>
      <Icon name={icon} className={`${iconColorClass} text-lg shrink-0`} />
      <div className="min-w-0">
        <p className="font-bold text-sm text-on-surface truncate">{label}</p>
        {desc && <p className="text-[11px] text-on-surface-variant leading-tight">{desc}</p>}
      </div>
    </button>
  );
}

function PrivilegeRow({ icon, label, checked, disabled, onChange }) {
  return (
    <label className={`flex items-center gap-3 px-4 py-3 ${disabled ? 'opacity-60' : 'cursor-pointer hover:bg-surface-variant/30 transition-colors'}`}>
      <div className={`w-5 h-5 rounded border-2 flex items-center justify-center shrink-0 transition-all ${checked ? 'bg-primary border-primary' : 'border-outline-variant'}`}>
        {checked && <Icon name="check" className="text-white text-[11px]" />}
      </div>
      <input
        type="checkbox"
        className="sr-only"
        checked={checked}
        disabled={disabled}
        onChange={(e) => !disabled && onChange(e.target.checked)}
      />
      <Icon name={icon} className={`${checked ? 'text-primary' : 'text-on-surface-variant'} text-lg shrink-0`} />
      <span className="text-sm font-medium text-on-surface">{label}</span>
    </label>
  );
}
