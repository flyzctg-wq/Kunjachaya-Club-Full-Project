package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Membership classes defined in the Kunjachaya Club constitution (ধারা-৬).
 */
enum class MemberClass {
    NEW,      // Application submitted, pending Executive Committee approval (ধারা-১০)
    GENERAL,  // সাধারণ সদস্য
    FOUNDING, // প্রতিষ্ঠাতা সদস্য — also seats on the Standing Council (স্থায়ী পরিষদ, ধারা-১৩খ)
    LIFETIME, // আজীবন সদস্য — one-time contribution (ধারা-৬ ঘ)
    DONOR,    // দাতা সদস্য — one-time contribution (ধারা-৬ ঙ)
    ADVISORY; // উপদেষ্টা সদস্য — up to 15 elders on the Advisory Council (ধারা-১৩ক)

    companion object {
        fun fromLabel(label: String?): MemberClass =
            entries.firstOrNull { it.name.equals(label, ignoreCase = true) } ?: NEW
    }
}

/**
 * The 15-seat Executive Committee (কার্যনির্বাহী পরিষদ) posts defined in ধারা-১৪.
 * Left blank for members who don't currently hold a committee seat.
 */
enum class CommitteePost {
    PRESIDENT,                     // সভাপতি — 1
    VICE_PRESIDENT,                // সহ-সভাপতি — 2
    GENERAL_SECRETARY,             // সাধারণ সম্পাদক — 1
    ASSISTANT_GENERAL_SECRETARY,   // সহ-সাধারণ সম্পাদক — 1
    TREASURER,                     // কোষাধ্যক্ষ — 1
    ORGANIZING_SECRETARY,          // সাংগঠনিক সম্পাদক — 1
    SOCIAL_WELFARE_SECRETARY,      // সমাজকল্যাণ সম্পাদক — 1
    LITERATURE_CULTURE_SECRETARY,  // সাহিত্য ও সংস্কৃতি সম্পাদক — 1
    PUBLICITY_SECRETARY,           // প্রচার সম্পাদক — 1
    SPORTS_SECRETARY,              // ক্রীড়া সম্পাদক — 1
    WOMENS_AFFAIRS_SECRETARY,      // মহিলাবিষয়ক সম্পাদক — 1
    EXECUTIVE_MEMBER;              // কার্যকরী সদস্য — 3

    companion object {
        fun fromLabel(label: String?): CommitteePost? =
            if (label.isNullOrBlank()) null
            else entries.firstOrNull { it.name.equals(label, ignoreCase = true) }
    }
}

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String = "",
    val phone: String = "",
    val nameEn: String = "",
    val nameBn: String = "",
    val dob: String = "",
    val bloodGroup: String = "",
    val professionEn: String = "",
    val professionBn: String = "",
    val road: String = "",
    val block: String = "",
    val floor: String = "",
    val holding: String = "",
    val primaryContact: String = "",
    val emergencyContact: String = "",
    val fatherOrSpouseNameEn: String = "",
    val fatherOrSpouseNameBn: String = "",
    val motherNameEn: String = "",
    val motherNameBn: String = "",
    val familyMembersCount: Int = 1,
    // "Active", "Pending", "Suspended", "Cancelled" (ধারা-৮, ধারা-১১)
    val membershipStatus: String = "Pending",
    // One of MemberClass.name — stored as a String for Room/Firestore portability
    val memberClass: String = MemberClass.NEW.name,
    // One of CommitteePost.name, or blank if the member holds no committee seat
    val committeePost: String = "",
    // Founding members plus the sitting President & General Secretary sit on
    // the Standing Council (স্থায়ী পরিষদ, ধারা-১৩খ). Founding members get this
    // automatically via isStandingCouncilMember(); this flag covers the rare
    // case of a Standing Council seat granted outside that default rule.
    val isStandingCouncil: Boolean = false,
    val canManageNotices: Boolean = false,
    val canManageComplaints: Boolean = false,
    val canManageMembers: Boolean = false,
    val canManageFinancials: Boolean = false,
    val canDeleteItems: Boolean = false,
    val profilePicUrl: String = "",
    val nidFrontUrl: String = "",
    val nidBackUrl: String = "",
    val joinedDate: String = ""
) {
    private fun post(): CommitteePost? = CommitteePost.fromLabel(committeePost)
    private fun cls(): MemberClass = MemberClass.fromLabel(memberClass)

    /** President or General Secretary carry the broadest constitutional authority (ধারা-১৭). */
    fun isPresidentOrGeneralSecretary(): Boolean =
        post() == CommitteePost.PRESIDENT || post() == CommitteePost.GENERAL_SECRETARY

    /** Holds one of the 15 elected/co-opted Executive Committee seats (ধারা-১৩গ, ধারা-১৪). */
    fun isExecutiveCommittee(): Boolean = post() != null

    fun isStandingCouncilMember(): Boolean =
        isStandingCouncil || isFoundingMember() || isPresidentOrGeneralSecretary()

    fun isAdvisoryCouncilMember(): Boolean = cls() == MemberClass.ADVISORY

    fun isFoundingMember(): Boolean = cls() == MemberClass.FOUNDING

    fun isGeneralMember(): Boolean = cls() == MemberClass.GENERAL

    fun isLifetimeMember(): Boolean = cls() == MemberClass.LIFETIME

    fun isDonorMember(): Boolean = cls() == MemberClass.DONOR

    fun isPendingApproval(): Boolean =
        cls() == MemberClass.NEW || membershipStatus.equals("Pending", ignoreCase = true)

    /** ধারা-৯(খ): Lifetime & Donor members may attend and speak, but not vote or hold office. */
    fun hasVotingRights(): Boolean =
        membershipStatus.equals("Active", ignoreCase = true) &&
                cls() in setOf(MemberClass.FOUNDING, MemberClass.GENERAL)

    fun hasNoticePermission(): Boolean =
        isPresidentOrGeneralSecretary() || (isExecutiveCommittee() && canManageNotices)

    fun hasComplaintPermission(): Boolean =
        isPresidentOrGeneralSecretary() || (isExecutiveCommittee() && canManageComplaints)

    /** Account approval is formally an Executive Committee decision (ধারা-১০গ). */
    fun hasMemberPermission(): Boolean =
        isPresidentOrGeneralSecretary() || (isExecutiveCommittee() && canManageMembers)

    /** The Treasurer keeps the books, but the President/Gen. Sec. retain oversight (ধারা-১৭.৫). */
    fun hasFinancialPermission(): Boolean =
        isPresidentOrGeneralSecretary() || (isExecutiveCommittee() && canManageFinancials)

    fun hasDeletePermission(): Boolean =
        isPresidentOrGeneralSecretary() && canDeleteItems

    /**
     * R5 Supreme Leader authority: Only President & General Secretary can appoint/demote R4 Officers
     * or grant/revoke administrative privilege flags.
     */
    fun canAppointOfficers(): Boolean = isPresidentOrGeneralSecretary()

    /**
     * Tiered Hierarchy Check (Kingshot Model):
     * - R5 (President/GS): Supreme authority to appoint/demote R4 Officers & manage all members.
     * - R4 (EC Officers with member perm): Can approve & manage R1-R3 lower ranks, but CANNOT touch R4/R5 officers.
     * - R1-R3: Read-only member access.
     */
    fun canModifyUserRole(target: UserEntity?): Boolean {
        if (target == null) return false
        if (isPresidentOrGeneralSecretary()) return true
        if (hasMemberPermission()) {
            return !target.isExecutiveCommittee()
        }
        return false
    }
}
