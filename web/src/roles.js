// Mirrors app/src/main/java/com/example/data/model/UserEntity.kt exactly —
// both platforms must agree on membership classes, committee posts, and the
// permissions derived from them, since they share one Firestore 'users'
// collection.

export const MemberClass = {
  NEW: 'NEW',
  GENERAL: 'GENERAL',
  FOUNDING: 'FOUNDING',
  LIFETIME: 'LIFETIME',
  DONOR: 'DONOR',
  ADVISORY: 'ADVISORY',
};

export const CommitteePost = {
  PRESIDENT: 'PRESIDENT',
  VICE_PRESIDENT: 'VICE_PRESIDENT',
  GENERAL_SECRETARY: 'GENERAL_SECRETARY',
  ASSISTANT_GENERAL_SECRETARY: 'ASSISTANT_GENERAL_SECRETARY',
  TREASURER: 'TREASURER',
  ORGANIZING_SECRETARY: 'ORGANIZING_SECRETARY',
  SOCIAL_WELFARE_SECRETARY: 'SOCIAL_WELFARE_SECRETARY',
  LITERATURE_CULTURE_SECRETARY: 'LITERATURE_CULTURE_SECRETARY',
  PUBLICITY_SECRETARY: 'PUBLICITY_SECRETARY',
  SPORTS_SECRETARY: 'SPORTS_SECRETARY',
  WOMENS_AFFAIRS_SECRETARY: 'WOMENS_AFFAIRS_SECRETARY',
  EXECUTIVE_MEMBER: 'EXECUTIVE_MEMBER',
};

export const committeePostLabels = {
  [CommitteePost.PRESIDENT]: { en: 'President', bn: 'সভাপতি' },
  [CommitteePost.VICE_PRESIDENT]: { en: 'Vice President', bn: 'সহ-সভাপতি' },
  [CommitteePost.GENERAL_SECRETARY]: { en: 'General Secretary', bn: 'সাধারণ সম্পাদক' },
  [CommitteePost.ASSISTANT_GENERAL_SECRETARY]: { en: 'Asst. General Secretary', bn: 'সহ-সাধারণ সম্পাদক' },
  [CommitteePost.TREASURER]: { en: 'Treasurer', bn: 'কোষাধ্যক্ষ' },
  [CommitteePost.ORGANIZING_SECRETARY]: { en: 'Organizing Secretary', bn: 'সাংগঠনিক সম্পাদক' },
  [CommitteePost.SOCIAL_WELFARE_SECRETARY]: { en: 'Social Welfare Secretary', bn: 'সমাজকল্যাণ সম্পাদক' },
  [CommitteePost.LITERATURE_CULTURE_SECRETARY]: { en: 'Literature & Culture Secretary', bn: 'সাহিত্য ও সংস্কৃতি সম্পাদক' },
  [CommitteePost.PUBLICITY_SECRETARY]: { en: 'Publicity Secretary', bn: 'প্রচার সম্পাদক' },
  [CommitteePost.SPORTS_SECRETARY]: { en: 'Sports Secretary', bn: 'ক্রীড়া সম্পাদক' },
  [CommitteePost.WOMENS_AFFAIRS_SECRETARY]: { en: "Women's Affairs Secretary", bn: 'মহিলাবিষয়ক সম্পাদক' },
  [CommitteePost.EXECUTIVE_MEMBER]: { en: 'Executive Member', bn: 'কার্যকরী সদস্য' },
};

export const memberClassLabels = {
  [MemberClass.NEW]: { en: 'New Member', bn: 'নতুন সদস্য' },
  [MemberClass.GENERAL]: { en: 'General Member', bn: 'সাধারণ সদস্য' },
  [MemberClass.FOUNDING]: { en: 'Founding Member', bn: 'প্রতিষ্ঠাতা সদস্য' },
  [MemberClass.LIFETIME]: { en: 'Lifetime Member', bn: 'আজীবন সদস্য' },
  [MemberClass.DONOR]: { en: 'Donor Member', bn: 'দাতা সদস্য' },
  [MemberClass.ADVISORY]: { en: 'Advisory Member', bn: 'উপদেষ্টা সদস্য' },
};

export function committeePostLabel(post, lang = 'en') {
  if (!post) return '';
  return committeePostLabels[post]?.[lang] ?? '';
}

export function memberClassLabel(memberClass, lang = 'en') {
  return memberClassLabels[memberClass]?.[lang] ?? memberClassLabels[MemberClass.NEW][lang];
}

/** President or General Secretary carry the broadest constitutional authority (ধারা-১৭). */
export function isPresidentOrGeneralSecretary(user) {
  return user?.committeePost === CommitteePost.PRESIDENT || user?.committeePost === CommitteePost.GENERAL_SECRETARY;
}

/** Holds one of the 15 elected/co-opted Executive Committee seats (ধারা-১৩গ, ধারা-১৪). */
export function isExecutiveCommittee(user) {
  return !!user?.committeePost;
}

export function isFoundingMember(user) {
  return user?.memberClass === MemberClass.FOUNDING;
}

export function isStandingCouncilMember(user) {
  return !!user?.isStandingCouncil || isFoundingMember(user) || isPresidentOrGeneralSecretary(user);
}

export function isAdvisoryCouncilMember(user) {
  return user?.memberClass === MemberClass.ADVISORY;
}

export function isPendingApproval(user) {
  return user?.memberClass === MemberClass.NEW || (user?.membershipStatus ?? 'Pending').toLowerCase() === 'pending';
}

/** ধারা-৯(খ): Lifetime & Donor members may attend and speak, but not vote or hold office. */
export function hasVotingRights(user) {
  return (user?.membershipStatus ?? '').toLowerCase() === 'active' &&
    [MemberClass.FOUNDING, MemberClass.GENERAL].includes(user?.memberClass);
}

export function hasNoticePermission(user) {
  return isPresidentOrGeneralSecretary(user) || (isExecutiveCommittee(user) && !!user?.canManageNotices);
}

export function hasComplaintPermission(user) {
  return isPresidentOrGeneralSecretary(user) || (isExecutiveCommittee(user) && !!user?.canManageComplaints);
}

/** Account approval is formally an Executive Committee decision (ধারা-১০গ). */
export function hasMemberPermission(user) {
  return isPresidentOrGeneralSecretary(user) || (isExecutiveCommittee(user) && !!user?.canManageMembers);
}

export function hasFinancialPermission(user) {
  return isPresidentOrGeneralSecretary(user) || (isExecutiveCommittee(user) && !!user?.canManageFinancials);
}

export function hasDeletePermission(user) {
  return isPresidentOrGeneralSecretary(user) && !!user?.canDeleteItems;
}
