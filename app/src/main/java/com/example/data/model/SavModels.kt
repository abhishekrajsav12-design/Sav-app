package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class SavRole(
    val displayName: String,
    val shortLabel: String,
    val description: String,
    val verificationHint: String,
    val departmentLabel: String,
    val designationLabel: String,
    val secondaryLabel: String?
) {
    PRINCIPAL(
        displayName = "Principal",
        shortLabel = "Principal",
        description = "Institutional head with approval governance, circulars, and full school oversight.",
        verificationHint = "Principal Employee ID (e.g. SAV-PRIN-001)",
        departmentLabel = "Administrative Wing",
        designationLabel = "Tenure / Office Title",
        secondaryLabel = "Official Extension / Chamber"
    ),
    VICE_PRINCIPAL(
        displayName = "Vice Principal",
        shortLabel = "Vice Principal",
        description = "Academic & campus leadership, registration approvals, and staff coordination.",
        verificationHint = "Vice Principal Employee ID (e.g. SAV-VP-002)",
        departmentLabel = "Academic & Admin Wing",
        designationLabel = "Portfolio / Office Title",
        secondaryLabel = "Supervising Wing"
    ),
    TEACHER(
        displayName = "Teacher",
        shortLabel = "Teacher",
        description = "Faculty workspace for assignments, exam schedules, parent queries, and updates.",
        verificationHint = "Faculty ID (e.g. SAV-FAC-104)",
        departmentLabel = "Subject Department",
        designationLabel = "Classes Taught (e.g. IX–XII)",
        secondaryLabel = "Assigned House / Mentorship"
    ),
    PARENT(
        displayName = "Parent",
        shortLabel = "Parent",
        description = "Guardian portal for ward progress, school notices, and teacher communication.",
        verificationHint = "Ward's SAV Admission No. (e.g. SAV-ADM-2023-45)",
        departmentLabel = "Ward's House",
        designationLabel = "Relation to Student",
        secondaryLabel = "Ward's Full Name & Class"
    ),
    STUDENT(
        displayName = "Student",
        shortLabel = "Student",
        description = "Student campus hub for coursework, house events, clubs, and welfare requests.",
        verificationHint = "SAV Admission / Roll No. (e.g. SAV-2024-088)",
        departmentLabel = "Residential House",
        designationLabel = "Class & Section",
        secondaryLabel = "Hostel Block / Club"
    ),
    ALUMNI(
        displayName = "Alumni",
        shortLabel = "Alumni",
        description = "Simultala alumni network, mentorship spotlights, and homecoming events.",
        verificationHint = "Alumni Reg. / Batch Roll No. (e.g. SAV-ALU-2016-12)",
        departmentLabel = "Graduation Batch Year",
        designationLabel = "Current Profession / Institution",
        secondaryLabel = "City / Mentorship Area"
    ),
    OTHER_STAFF(
        displayName = "Other Staff Member",
        shortLabel = "Other Staff",
        description = "Hostel wardens, librarians, counselors, and lab specialists.",
        verificationHint = "Staff ID (e.g. SAV-STF-210)",
        departmentLabel = "Campus Unit (Hostel / Library / Lab)",
        designationLabel = "Role / Specialization",
        secondaryLabel = "Assigned Block / Duty Shift"
    ),
    NON_TEACHING_STAFF(
        displayName = "Non-Teaching Staff",
        shortLabel = "Non-Teaching",
        description = "Administration, accounts, transport, and campus facility operations.",
        verificationHint = "Staff ID (e.g. SAV-NTS-305)",
        departmentLabel = "Operations Unit (Admin / Transport / Maintenance)",
        designationLabel = "Designation",
        secondaryLabel = "Duty Location"
    );

    companion object {
        fun fromRaw(raw: String?): SavRole =
            entries.firstOrNull { it.name == raw } ?: STUDENT
    }
}

enum class ApprovalStatus(val label: String) {
    PENDING("Pending School Approval"),
    APPROVED("Approved Member"),
    REJECTED("Application Rejected");

    companion object {
        fun fromRaw(raw: String?): ApprovalStatus =
            entries.firstOrNull { it.name == raw } ?: PENDING
    }
}

enum class NoticeCategory(val label: String) {
    OFFICIAL_CIRCULAR("Official Circular"),
    ACADEMIC_UPDATE("Academic Update"),
    ALUMNI_SPOTLIGHT("Alumni Spotlight"),
    CAMPUS_OPERATIONS("Campus Operations"),
    COMMUNITY_POST("Community Post");

    companion object {
        fun fromRaw(raw: String?): NoticeCategory =
            entries.firstOrNull { it.name == raw } ?: COMMUNITY_POST
    }
}

enum class AcademicItemType(val label: String) {
    ASSIGNMENT("Assignment"),
    EXAM_SCHEDULE("Exam Schedule"),
    STUDY_MATERIAL("Study Material");

    companion object {
        fun fromRaw(raw: String?): AcademicItemType =
            entries.firstOrNull { it.name == raw } ?: ASSIGNMENT
    }
}

enum class EventAudience(val label: String) {
    ALL("All SAV Family"),
    STUDENTS_PARENTS("Students & Parents"),
    STAFF("Faculty & Staff"),
    ALUMNI("Alumni Network");

    companion object {
        fun fromRaw(raw: String?): EventAudience =
            entries.firstOrNull { it.name == raw } ?: ALL
    }
}

enum class ServiceRequestCategory(val label: String) {
    LEAVE_REQUEST("Leave Request"),
    HOSTEL_WELFARE("Hostel & Welfare"),
    FACILITY_MAINTENANCE("Facility Maintenance"),
    PARENT_INQUIRY("Parent Inquiry"),
    ACADEMIC_SUPPORT("Academic Support");

    companion object {
        fun fromRaw(raw: String?): ServiceRequestCategory =
            entries.firstOrNull { it.name == raw } ?: ACADEMIC_SUPPORT
    }
}

enum class ServiceRequestStatus(val label: String) {
    OPEN("Open"),
    IN_REVIEW("In Review"),
    RESOLVED("Resolved"),
    DECLINED("Declined");

    companion object {
        fun fromRaw(raw: String?): ServiceRequestStatus =
            entries.firstOrNull { it.name == raw } ?: OPEN
    }
}

enum class CommunityChannel(
    val channelId: String,
    val title: String,
    val subtitle: String,
    val allowedRoles: Set<SavRole>
) {
    ALL_COMMUNITY(
        channelId = "ALL_COMMUNITY",
        title = "SAV Family Commons",
        subtitle = "Open to all verified SAV members",
        allowedRoles = SavRole.entries.toSet()
    ),
    FACULTY_STAFF(
        channelId = "FACULTY_STAFF",
        title = "Faculty & Staff Desk",
        subtitle = "Leadership, teachers, and campus staff",
        allowedRoles = setOf(
            SavRole.PRINCIPAL,
            SavRole.VICE_PRINCIPAL,
            SavRole.TEACHER,
            SavRole.OTHER_STAFF,
            SavRole.NON_TEACHING_STAFF
        )
    ),
    PARENTS_TEACHERS(
        channelId = "PARENTS_TEACHERS",
        title = "Parents–Teachers Bridge",
        subtitle = "Academic updates & parent coordination",
        allowedRoles = setOf(
            SavRole.PRINCIPAL,
            SavRole.VICE_PRINCIPAL,
            SavRole.TEACHER,
            SavRole.PARENT
        )
    ),
    STUDENT_CAMPUS(
        channelId = "STUDENT_CAMPUS",
        title = "Student Hostel & Clubs",
        subtitle = "Campus life, houses, and student mentorship",
        allowedRoles = setOf(
            SavRole.PRINCIPAL,
            SavRole.VICE_PRINCIPAL,
            SavRole.TEACHER,
            SavRole.STUDENT,
            SavRole.OTHER_STAFF
        )
    ),
    ALUMNI_CIRCLE(
        channelId = "ALUMNI_CIRCLE",
        title = "Alumni Heritage Circle",
        subtitle = "Graduates, mentors, and faculty",
        allowedRoles = setOf(
            SavRole.PRINCIPAL,
            SavRole.VICE_PRINCIPAL,
            SavRole.TEACHER,
            SavRole.ALUMNI
        )
    );

    companion object {
        fun accessibleFor(role: SavRole): List<CommunityChannel> =
            entries.filter { role in it.allowedRoles }
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = SavRole.STUDENT.name,
    val approvalStatus: String = ApprovalStatus.PENDING.name,
    val verificationId: String = "",
    val houseOrDepartment: String = "",
    val classOrDesignation: String = "",
    val wardOrSecondaryInfo: String? = null,
    val bio: String? = null,
    val rejectionReason: String? = null,
    val reviewedBy: String? = null,
    val emailVerified: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun parsedRole(): SavRole = SavRole.fromRaw(role)
    fun parsedStatus(): ApprovalStatus = ApprovalStatus.fromRaw(approvalStatus)
}

@Keep
@IgnoreExtraProperties
data class DirectoryEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val role: String = SavRole.STUDENT.name,
    val houseOrDepartment: String = "",
    val classOrDesignation: String = "",
    val wardOrSecondaryInfo: String? = null,
    val bio: String? = null,
    val approvalStatus: String = ApprovalStatus.PENDING.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun parsedRole(): SavRole = SavRole.fromRaw(role)
    fun parsedStatus(): ApprovalStatus = ApprovalStatus.fromRaw(approvalStatus)
}

@Keep
@IgnoreExtraProperties
data class Notice(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorRole: String = SavRole.PRINCIPAL.name,
    val category: String = NoticeCategory.COMMUNITY_POST.name,
    val title: String = "",
    val body: String = "",
    val targetRole: String = "ALL",
    val pinned: Boolean = false,
    val likesCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun parsedAuthorRole(): SavRole = SavRole.fromRaw(authorRole)
    fun parsedCategory(): NoticeCategory = NoticeCategory.fromRaw(category)
}

@Keep
@IgnoreExtraProperties
data class AcademicItem(
    @DocumentId val id: String = "",
    val teacherId: String = "",
    val teacherName: String = "",
    val subject: String = "",
    val targetClass: String = "",
    val title: String = "",
    val description: String = "",
    val dueDateLabel: String = "",
    val itemType: String = AcademicItemType.ASSIGNMENT.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun parsedType(): AcademicItemType = AcademicItemType.fromRaw(itemType)
}

@Keep
@IgnoreExtraProperties
data class SchoolEvent(
    @DocumentId val id: String = "",
    val organizerId: String = "",
    val organizerName: String = "",
    val organizerRole: String = SavRole.PRINCIPAL.name,
    val title: String = "",
    val description: String = "",
    val eventDateLabel: String = "",
    val location: String = "",
    val audience: String = EventAudience.ALL.name,
    val rsvpCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun parsedOrganizerRole(): SavRole = SavRole.fromRaw(organizerRole)
    fun parsedAudience(): EventAudience = EventAudience.fromRaw(audience)
}

@Keep
@IgnoreExtraProperties
data class SchoolMessage(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderRole: String = SavRole.STUDENT.name,
    val channelType: String = "CHANNEL",
    val channelId: String = "",
    val participantIds: List<String> = emptyList(),
    val body: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun parsedSenderRole(): SavRole = SavRole.fromRaw(senderRole)
}

@Keep
@IgnoreExtraProperties
data class ServiceRequest(
    @DocumentId val id: String = "",
    val requesterId: String = "",
    val requesterName: String = "",
    val requesterRole: String = SavRole.STUDENT.name,
    val category: String = ServiceRequestCategory.ACADEMIC_SUPPORT.name,
    val title: String = "",
    val details: String = "",
    val status: String = ServiceRequestStatus.OPEN.name,
    val resolutionNote: String? = null,
    val resolvedBy: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun parsedRequesterRole(): SavRole = SavRole.fromRaw(requesterRole)
    fun parsedCategory(): ServiceRequestCategory = ServiceRequestCategory.fromRaw(category)
    fun parsedStatus(): ServiceRequestStatus = ServiceRequestStatus.fromRaw(status)
}
