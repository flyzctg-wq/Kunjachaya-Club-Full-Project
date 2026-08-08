package com.example.ui.language

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

enum class Language {
    EN, BN
}

data class AppStrings(
    val clubName: String,
    val clubSubtitle: String,
    val dashboard: String,
    val profile: String,
    val complaints: String,
    val financials: String,
    val admin: String,
    val schemaDocs: String,
    val loginTitle: String,
    val loginSubtitle: String,
    val phoneNumber: String,
    val sendOtp: String,
    val enterOtp: String,
    val verifyAndLogin: String,
    val roleMember: String,
    val roleAdmin: String,
    val duesAmount: String,
    val totalPaid: String,
    val donations: String,
    val payDues: String,
    val makeDonation: String,
    val transactionHistory: String,
    val noticeBoard: String,
    val latestActivities: String,
    val personalInformation: String,
    val residentialAddress: String,
    val familyDetails: String,
    val documentsNid: String,
    val membershipStatus: String,
    val active: String,
    val pending: String,
    val underReview: String,
    val resolved: String
)

val EnglishAppStrings = AppStrings(
    clubName = "Kunjachaya Club",
    clubSubtitle = "Residential Society & Community",
    dashboard = "Dashboard",
    profile = "Profile",
    complaints = "Complaint Box",
    financials = "Financials",
    admin = "Admin Portal",
    schemaDocs = "Architecture & Schema",
    loginTitle = "Member Portal Access",
    loginSubtitle = "Enter your registered Bangladesh mobile number",
    phoneNumber = "Phone Number",
    sendOtp = "Send OTP Code",
    enterOtp = "Enter 6-digit OTP",
    verifyAndLogin = "Verify & Login",
    roleMember = "Member",
    roleAdmin = "Executive Admin",
    duesAmount = "Current Dues",
    totalPaid = "Total Paid",
    donations = "Donations Made",
    payDues = "Pay Dues",
    makeDonation = "Make Donation",
    transactionHistory = "Transaction History",
    noticeBoard = "Official Notices & Bulletins",
    latestActivities = "Community Activities Feed",
    personalInformation = "Personal Information",
    residentialAddress = "Residential Address",
    familyDetails = "Family & Dependents",
    documentsNid = "Identity Documents (NID)",
    membershipStatus = "Membership Status",
    active = "Active",
    pending = "Pending Approval",
    underReview = "Under Review",
    resolved = "Resolved"
)

val BengaliAppStrings = AppStrings(
    clubName = "কুঞ্জছায়া ক্লাব",
    clubSubtitle = "আবাসিক সোসাইটি ও কল্যাণ সমিতি",
    dashboard = "ড্যাশবোর্ড",
    profile = "প্রোফাইল",
    complaints = "অভিযোগ বক্স",
    financials = "অর্থায়ন",
    admin = "অ্যাডমিন পোর্টাল",
    schemaDocs = "আর্কিটেকচার ও স্কিমা",
    loginTitle = "সদস্য পোর্টাল প্রবেশ",
    loginSubtitle = "আপনার নিবন্ধিত মোবাইল নম্বর প্রবেশ করান",
    phoneNumber = "মোবাইল নম্বর",
    sendOtp = "ওটিপি কোড পাঠান",
    enterOtp = "৬-ডিজিটের ওটিপি প্রবেশ করান",
    verifyAndLogin = "যাচাই করুন ও প্রবেশ করুন",
    roleMember = "সাধারণ সদস্য",
    roleAdmin = "নির্বাহী অ্যাডমিন",
    duesAmount = "বর্তমান বকেয়া",
    totalPaid = "মোট পরিশোধিত",
    donations = "মোট অনুদান",
    payDues = "বকেয়া পরিশোধ করুন",
    makeDonation = "অনুদান দিন",
    transactionHistory = "লেনদেনের ইতিহাস",
    noticeBoard = "অফিসিয়াল নোটিশ ও বুলেটিন",
    latestActivities = "কমিউনিটি কার্যক্রম সংবাদ",
    personalInformation = "ব্যক্তিগত তথ্য",
    residentialAddress = "আবাসিক ঠিকানা",
    familyDetails = "পরিবার ও নির্ভরশীলাগণ",
    documentsNid = "জাতীয় পরিচয়পত্র (এনআইডি)",
    membershipStatus = "সদস্যপদ অবস্থা",
    active = "সক্রিয়",
    pending = "অনুমোদনের অপেক্ষায়",
    underReview = "পর্যালোচনাধীন",
    resolved = "সমাধানকৃত"
)

val LocalAppStrings = staticCompositionLocalOf { EnglishAppStrings }

@Composable
fun AppLanguageProvider(
    language: Language,
    content: @Composable () -> Unit
) {
    val strings = when (language) {
        Language.EN -> EnglishAppStrings
        Language.BN -> BengaliAppStrings
    }
    CompositionLocalProvider(LocalAppStrings provides strings) {
        content()
    }
}

val appStrings: AppStrings
    @Composable
    @ReadOnlyComposable
    get() = LocalAppStrings.current

object AppLanguage {
    fun get(lang: Language, en: String, bn: String): String {
        return if (lang == Language.BN) bn else en
    }

    // Common UI Text
    fun clubName(lang: Language) = get(lang, "Kunjachaya Club", "কুঞ্জছায়া ক্লাব")
    fun clubSubtitle(lang: Language) = get(lang, "Residential Society & Community", "আবাসিক সোসাইটি ও কল্যাণ সমিতি")
    
    // Navigation
    fun dashboard(lang: Language) = get(lang, "Dashboard", "ড্যাশবোর্ড")
    fun calendar(lang: Language) = get(lang, "Calendar", "ক্যালেন্ডার")
    fun notices(lang: Language) = get(lang, "Notices", "নোটিশ")
    fun directory(lang: Language) = get(lang, "Directory", "ডিরেক্টরি")
    fun profile(lang: Language) = get(lang, "Profile", "প্রোফাইল")
    fun complaints(lang: Language) = get(lang, "Complaint Box", "অভিযোগ বক্স")
    fun financials(lang: Language) = get(lang, "Financials", "অর্থায়ন")
    fun admin(lang: Language) = get(lang, "Admin Portal", "অ্যাডমিন পোর্টাল")
    fun schemaDocs(lang: Language) = get(lang, "Architecture & Schema", "আর্কিটেকচার ও স্কিমা")

    // Auth
    fun loginTitle(lang: Language) = get(lang, "Member Portal Access", "সদস্য পোর্টাল প্রবেশ")
    fun loginSubtitle(lang: Language) = get(lang, "Enter your registered Bangladesh mobile number", "আপনার নিবন্ধিত মোবাইল নম্বর প্রবেশ করান")
    fun phoneNumber(lang: Language) = get(lang, "Phone Number", "মোবাইল নম্বর")
    fun sendOtp(lang: Language) = get(lang, "Send OTP Code", "ওটিপি কোড পাঠান")
    fun enterOtp(lang: Language) = get(lang, "Enter 6-digit OTP", "৬-ডিজিটের ওটিপি প্রবেশ করান")
    fun verifyAndLogin(lang: Language) = get(lang, "Verify & Login", "যাচাই করুন ও প্রবেশ করুন")
    
    // Roles
    fun roleSuperAdmin(lang: Language) = get(lang, "President / General Secretary", "সভাপতি / সাধারণ সম্পাদক")
    fun roleAdmin(lang: Language) = get(lang, "Executive Committee", "কার্যনির্বাহী পরিষদ")
    fun roleFounding(lang: Language) = get(lang, "Founding Member", "প্রতিষ্ঠাতা সদস্য")
    fun roleGeneral(lang: Language) = get(lang, "General Member", "সাধারণ সদস্য")
    fun roleNew(lang: Language) = get(lang, "New Member", "নতুন সদস্য")
    fun roleMember(lang: Language) = get(lang, "General Member", "সাধারণ সদস্য")

    /** Localized label for one of the 15 Executive Committee posts (ধারা-১৪). */
    fun committeePostLabel(lang: Language, committeePost: String?): String {
        val post = com.example.data.model.CommitteePost.fromLabel(committeePost) ?: return ""
        return when (post) {
            com.example.data.model.CommitteePost.PRESIDENT -> get(lang, "President", "সভাপতি")
            com.example.data.model.CommitteePost.VICE_PRESIDENT -> get(lang, "Vice President", "সহ-সভাপতি")
            com.example.data.model.CommitteePost.GENERAL_SECRETARY -> get(lang, "General Secretary", "সাধারণ সম্পাদক")
            com.example.data.model.CommitteePost.ASSISTANT_GENERAL_SECRETARY -> get(lang, "Asst. General Secretary", "সহ-সাধারণ সম্পাদক")
            com.example.data.model.CommitteePost.TREASURER -> get(lang, "Treasurer", "কোষাধ্যক্ষ")
            com.example.data.model.CommitteePost.ORGANIZING_SECRETARY -> get(lang, "Organizing Secretary", "সাংগঠনিক সম্পাদক")
            com.example.data.model.CommitteePost.SOCIAL_WELFARE_SECRETARY -> get(lang, "Social Welfare Secretary", "সমাজকল্যাণ সম্পাদক")
            com.example.data.model.CommitteePost.LITERATURE_CULTURE_SECRETARY -> get(lang, "Literature & Culture Secretary", "সাহিত্য ও সংস্কৃতি সম্পাদক")
            com.example.data.model.CommitteePost.PUBLICITY_SECRETARY -> get(lang, "Publicity Secretary", "প্রচার সম্পাদক")
            com.example.data.model.CommitteePost.SPORTS_SECRETARY -> get(lang, "Sports Secretary", "ক্রীড়া সম্পাদক")
            com.example.data.model.CommitteePost.WOMENS_AFFAIRS_SECRETARY -> get(lang, "Women's Affairs Secretary", "মহিলাবিষয়ক সম্পাদক")
            com.example.data.model.CommitteePost.EXECUTIVE_MEMBER -> get(lang, "Executive Member", "কার্যকরী সদস্য")
        }
    }
    
    // Financials
    fun duesAmount(lang: Language) = get(lang, "Current Dues", "বর্তমান বকেয়া")
    fun totalPaid(lang: Language) = get(lang, "Total Paid", "মোট পরিশোধিত")
    fun donations(lang: Language) = get(lang, "Donations Made", "মোট অনুদান")
    fun payDues(lang: Language) = get(lang, "Pay Dues", "বকেয়া পরিশোধ করুন")
    fun makeDonation(lang: Language) = get(lang, "Make Donation", "অনুদান দিন")
    fun transactionHistory(lang: Language) = get(lang, "Transaction History", "লেনদেনের ইতিহাস")
    
    // Notices
    fun noticeBoard(lang: Language) = get(lang, "Official Notices & Bulletins", "অফিসিয়াল নোটিশ ও বুলেটিন")
    fun latestActivities(lang: Language) = get(lang, "Community Activities Feed", "কমিউনিটি কার্যক্রম সংবাদ")
    
    // Profile
    fun personalInformation(lang: Language) = get(lang, "Personal Information", "ব্যক্তিগত তথ্য")
    fun residentialAddress(lang: Language) = get(lang, "Residential Address", "আবাসিক ঠিকানা")
    fun familyDetails(lang: Language) = get(lang, "Family & Dependents", "পরিবার ও নির্ভরশীলাগণ")
    fun documentsNid(lang: Language) = get(lang, "Identity Documents (NID)", "জাতীয় পরিচয়পত্র (এনআইডি)")
    fun membershipStatus(lang: Language) = get(lang, "Membership Status", "সদস্যপদ অবস্থা")
    
    // Statuses
    fun active(lang: Language) = get(lang, "Active", "সক্রিয়")
    fun pending(lang: Language) = get(lang, "Pending Approval", "অনুমোদনের অপেক্ষায়")
    fun underReview(lang: Language) = get(lang, "Under Review", "পর্যালোচনাধীন")
    fun resolved(lang: Language) = get(lang, "Resolved", "সমাধানকৃত")
}
