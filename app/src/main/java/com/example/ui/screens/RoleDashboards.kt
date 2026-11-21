package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.HowToReg
import androidx.compose.material.icons.outlined.PostAdd
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AcademicItem
import com.example.data.model.ApprovalStatus
import com.example.data.model.DirectoryEntry
import com.example.data.model.Notice
import com.example.data.model.SavRole
import com.example.data.model.SchoolEvent
import com.example.data.model.ServiceRequest
import com.example.data.model.ServiceRequestStatus
import com.example.data.model.UserProfile
import com.example.ui.components.RequestStatusPill
import com.example.ui.components.SavEmptyState
import com.example.ui.components.SavErrorBanner
import com.example.ui.components.SavRoleBadge
import com.example.ui.components.SavStatCard
import com.example.ui.components.formatSavTimestamp
import com.example.ui.theme.SavEmerald
import com.example.ui.theme.SavGoldBright
import com.example.ui.theme.SavGoldPrimary
import com.example.ui.theme.SavNavyDeep
import com.example.ui.theme.SavNavyElevated
import com.example.ui.theme.SavNavyPrimary
import com.example.ui.theme.SavPureWhite

@Composable
fun RoleDashboardScreen(
    profile: UserProfile,
    directory: List<DirectoryEntry>,
    notices: List<Notice>,
    events: List<SchoolEvent>,
    academicItems: List<AcademicItem>,
    applications: List<UserProfile>,
    serviceRequests: List<ServiceRequest>,
    isLoading: Boolean,
    errorMessage: String?,
    onOpenCommunity: () -> Unit,
    onOpenMessages: () -> Unit,
    onOpenCampusTab: (Int) -> Unit,
    onOpenComposeNotice: () -> Unit,
    onOpenCreateAcademic: () -> Unit,
    onOpenCreateRequest: () -> Unit
) {
    val role = profile.parsedRole()
    val pendingApps = applications.filter { it.parsedStatus() == ApprovalStatus.PENDING }
    val openRequests = serviceRequests.filter {
        it.parsedStatus() == ServiceRequestStatus.OPEN || it.parsedStatus() == ServiceRequestStatus.IN_REVIEW
    }
    val pinnedNotices = notices.filter { it.pinned }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            RoleHeroHeader(profile = profile, role = role)
        }

        if (errorMessage != null) {
            item { SavErrorBanner(errorMessage) }
        }

        if (isLoading && notices.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        // Distinct 8-role KPI stats & quick actions
        item {
            when (role) {
                SavRole.PRINCIPAL -> {
                    RoleStatsGrid(
                        stat1 = Triple("Pending Approvals", "${pendingApps.size}", "Awaiting sign-off", Icons.Outlined.HowToReg),
                        stat2 = Triple("SAV Members", "${directory.size}", "Verified community", Icons.Outlined.Groups),
                        stat3 = Triple("Open Requests", "${openRequests.size}", "Campus & staff desk", Icons.Outlined.SupportAgent),
                        stat4 = Triple("Circulars", "${notices.size}", "${pinnedNotices.size} pinned", Icons.Outlined.Campaign)
                    )
                    Spacer(Modifier.height(12.dp))
                    QuickActionRow(
                        primaryLabel = "Review Approvals (${pendingApps.size})",
                        primaryIcon = Icons.Outlined.HowToReg,
                        onPrimary = { onOpenCampusTab(0) },
                        secondaryLabel = "Issue Circular",
                        secondaryIcon = Icons.Outlined.Campaign,
                        onSecondary = onOpenComposeNotice
                    )
                }

                SavRole.VICE_PRINCIPAL -> {
                    RoleStatsGrid(
                        stat1 = Triple("Approvals Queue", "${pendingApps.size}", "Review identities", Icons.Outlined.HowToReg),
                        stat2 = Triple("Academic Items", "${academicItems.size}", "Active class posts", Icons.Outlined.School),
                        stat3 = Triple("Open Tickets", "${openRequests.size}", "Oversight queue", Icons.Outlined.SupportAgent),
                        stat4 = Triple("Campus Events", "${events.size}", "Scheduled", Icons.Outlined.Event)
                    )
                    Spacer(Modifier.height(12.dp))
                    QuickActionRow(
                        primaryLabel = "Manage Approvals",
                        primaryIcon = Icons.Outlined.HowToReg,
                        onPrimary = { onOpenCampusTab(0) },
                        secondaryLabel = "Publish Notice",
                        secondaryIcon = Icons.Outlined.Campaign,
                        onSecondary = onOpenComposeNotice
                    )
                }

                SavRole.TEACHER -> {
                    val myItems = academicItems.count { it.teacherId == profile.userId }
                    RoleStatsGrid(
                        stat1 = Triple("My Class Posts", "$myItems", "Assignments & notes", Icons.AutoMirrored.Outlined.Assignment),
                        stat2 = Triple("Student/Parent Queries", "${openRequests.size}", "Needs response", Icons.Outlined.SupportAgent),
                        stat3 = Triple("Faculty & Students", "${directory.size}", "SAV directory", Icons.Outlined.Groups),
                        stat4 = Triple("School Notices", "${notices.size}", "Latest updates", Icons.Outlined.Campaign)
                    )
                    Spacer(Modifier.height(12.dp))
                    QuickActionRow(
                        primaryLabel = "Post Assignment",
                        primaryIcon = Icons.Outlined.PostAdd,
                        onPrimary = onOpenCreateAcademic,
                        secondaryLabel = "Academic Update",
                        secondaryIcon = Icons.Outlined.Campaign,
                        onSecondary = onOpenComposeNotice
                    )
                }

                SavRole.PARENT -> {
                    WardSummaryCard(profile = profile)
                    Spacer(Modifier.height(12.dp))
                    RoleStatsGrid(
                        stat1 = Triple("Class Updates", "${academicItems.size}", "Homework & exams", Icons.Outlined.School),
                        stat2 = Triple("My Inquiries", "${serviceRequests.size}", "${openRequests.size} active", Icons.Outlined.SupportAgent),
                        stat3 = Triple("School Circulars", "${pinnedNotices.size}", "Pinned by Admin", Icons.Outlined.Campaign),
                        stat4 = Triple("Campus Events", "${events.size}", "Upcoming dates", Icons.Outlined.Event)
                    )
                    Spacer(Modifier.height(12.dp))
                    QuickActionRow(
                        primaryLabel = "View Ward Academics",
                        primaryIcon = Icons.Outlined.School,
                        onPrimary = { onOpenCampusTab(0) },
                        secondaryLabel = "Parent Inquiry",
                        secondaryIcon = Icons.Outlined.SupportAgent,
                        onSecondary = onOpenCreateRequest
                    )
                }

                SavRole.STUDENT -> {
                    RoleStatsGrid(
                        stat1 = Triple("Assignments & Exams", "${academicItems.size}", "Active coursework", Icons.AutoMirrored.Outlined.Assignment),
                        stat2 = Triple("Upcoming Events", "${events.size}", "House & campus", Icons.Outlined.Event),
                        stat3 = Triple("My Requests", "${serviceRequests.size}", "${openRequests.size} open", Icons.Outlined.SupportAgent),
                        stat4 = Triple("SAV Community", "${directory.size}", "Verified peers", Icons.Outlined.Groups)
                    )
                    Spacer(Modifier.height(12.dp))
                    QuickActionRow(
                        primaryLabel = "Coursework Hub",
                        primaryIcon = Icons.Outlined.School,
                        onPrimary = { onOpenCampusTab(0) },
                        secondaryLabel = "Hostel / Study Help",
                        secondaryIcon = Icons.Outlined.SupportAgent,
                        onSecondary = onOpenCreateRequest
                    )
                }

                SavRole.ALUMNI -> {
                    val alumniCount = directory.count { it.parsedRole() == SavRole.ALUMNI }
                    RoleStatsGrid(
                        stat1 = Triple("Alumni Circle", "$alumniCount", "Registered batches", Icons.Outlined.WorkspacePremium),
                        stat2 = Triple("Homecoming Events", "${events.size}", "Campus calendar", Icons.Outlined.Event),
                        stat3 = Triple("SAV Family", "${directory.size}", "Total verified", Icons.Outlined.Groups),
                        stat4 = Triple("Spotlights", "${notices.size}", "Community stories", Icons.Outlined.Campaign)
                    )
                    Spacer(Modifier.height(12.dp))
                    QuickActionRow(
                        primaryLabel = "Share Spotlight",
                        primaryIcon = Icons.Outlined.PostAdd,
                        onPrimary = onOpenComposeNotice,
                        secondaryLabel = "Alumni Channel",
                        secondaryIcon = Icons.Outlined.Forum,
                        onSecondary = onOpenMessages
                    )
                }

                SavRole.OTHER_STAFF -> {
                    RoleStatsGrid(
                        stat1 = Triple("Welfare & Campus", "${openRequests.size}", "Open student tickets", Icons.Outlined.SupportAgent),
                        stat2 = Triple("Staff Notices", "${notices.size}", "Operations feed", Icons.Outlined.Campaign),
                        stat3 = Triple("Campus Events", "${events.size}", "Logistics calendar", Icons.Outlined.Event),
                        stat4 = Triple("SAV Members", "${directory.size}", "Verified campus", Icons.Outlined.Groups)
                    )
                    Spacer(Modifier.height(12.dp))
                    QuickActionRow(
                        primaryLabel = "Operations Desk",
                        primaryIcon = Icons.Outlined.SupportAgent,
                        onPrimary = { onOpenCampusTab(0) },
                        secondaryLabel = "Staff Notice",
                        secondaryIcon = Icons.Outlined.Campaign,
                        onSecondary = onOpenComposeNotice
                    )
                }

                SavRole.NON_TEACHING_STAFF -> {
                    RoleStatsGrid(
                        stat1 = Triple("Facility Tickets", "${openRequests.size}", "Maintenance & ops", Icons.Outlined.Build),
                        stat2 = Triple("Total Tickets", "${serviceRequests.size}", "Assigned & mine", Icons.Outlined.SupportAgent),
                        stat3 = Triple("Campus Circulars", "${notices.size}", "Admin bulletins", Icons.Outlined.Campaign),
                        stat4 = Triple("Upcoming Events", "${events.size}", "Facility prep", Icons.Outlined.Event)
                    )
                    Spacer(Modifier.height(12.dp))
                    QuickActionRow(
                        primaryLabel = "Maintenance Queue",
                        primaryIcon = Icons.Outlined.Build,
                        onPrimary = { onOpenCampusTab(0) },
                        secondaryLabel = "Operations Post",
                        secondaryIcon = Icons.Outlined.Campaign,
                        onSecondary = onOpenComposeNotice
                    )
                }
            }
        }

        // Role-specific live preview section
        if (role in setOf(SavRole.PRINCIPAL, SavRole.VICE_PRINCIPAL) && pendingApps.isNotEmpty()) {
            item {
                SectionHeaderRow(
                    title = "Pending Verification Queue",
                    actionLabel = "Open Queue",
                    onAction = { onOpenCampusTab(0) }
                )
                Spacer(Modifier.height(8.dp))
                pendingApps.take(3).forEach { applicant ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(applicant.fullName, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${applicant.parsedRole().displayName} • ID: ${applicant.verificationId}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = { onOpenCampusTab(0) }) {
                                Text("Review")
                            }
                        }
                    }
                }
            }
        }

        if (role in setOf(SavRole.TEACHER, SavRole.STUDENT, SavRole.PARENT) && academicItems.isNotEmpty()) {
            item {
                SectionHeaderRow(
                    title = "Recent Academic Updates",
                    actionLabel = "View All",
                    onAction = { onOpenCampusTab(0) }
                )
                Spacer(Modifier.height(8.dp))
                academicItems.take(3).forEach { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${item.subject} • ${item.targetClass}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SavGoldPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = item.dueDateLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(item.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = item.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        if (role in setOf(SavRole.OTHER_STAFF, SavRole.NON_TEACHING_STAFF)) {
            item {
                SectionHeaderRow(
                    title = "Active Campus & Welfare Desk",
                    actionLabel = "Manage",
                    onAction = { onOpenCampusTab(0) }
                )
                Spacer(Modifier.height(8.dp))
                if (openRequests.isEmpty()) {
                    SavEmptyState(
                        title = "All Campus Tickets Clear",
                        description = "No open welfare or maintenance requests right now.",
                        useHallArt = true
                    )
                } else {
                    openRequests.take(3).forEach { req ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(req.title, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "${req.parsedCategory().label} • ${req.requesterName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                RequestStatusPill(req.parsedStatus())
                            }
                        }
                    }
                }
            }
        }

        // Latest SAV Circulars & Feed preview for every role
        item {
            SectionHeaderRow(
                title = "Latest SAV Notices & Feed",
                actionLabel = "Community",
                onAction = onOpenCommunity
            )
            Spacer(Modifier.height(8.dp))
            if (notices.isEmpty()) {
                SavEmptyState(
                    title = "Welcome to the SAV FAMILY Board",
                    description = "Official circulars, academic announcements, and community highlights will appear here."
                )
            } else {
                notices.take(3).forEach { notice ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SavRoleBadge(notice.parsedAuthorRole())
                                Text(
                                    text = formatSavTimestamp(notice.createdAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(notice.title, style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = notice.body,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleHeroHeader(profile: UserProfile, role: SavRole) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard_role_header"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = SavNavyPrimary)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(SavNavyDeep, SavNavyPrimary, SavNavyElevated)
                    )
                )
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_sav_emblem),
                    contentDescription = "SAV Emblem",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SIMULTALA AWASIYA VIDYALAYA",
                        style = MaterialTheme.typography.labelSmall,
                        color = SavGoldBright,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = profile.fullName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = SavPureWhite
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${role.displayName} • ${profile.houseOrDepartment}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SavRoleBadge(role)
                        Surface(
                            color = SavEmerald.copy(alpha = 0.25f),
                            contentColor = Color(0xFF86EFAC),
                            shape = CircleShape
                        ) {
                            Text(
                                text = "Verified SAV Member",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WardSummaryCard(profile: UserProfile) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Ward & Guardian Link",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = profile.wardOrSecondaryInfo?.takeIf { it.isNotBlank() } ?: "Registered Ward",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = "Admission ID: ${profile.verificationId} • House: ${profile.houseOrDepartment}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun RoleStatsGrid(
    stat1: Triple<String, String, String, ImageVector>,
    stat2: Triple<String, String, String, ImageVector>,
    stat3: Triple<String, String, String, ImageVector>,
    stat4: Triple<String, String, String, ImageVector>
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SavStatCard(stat1.first, stat1.second, stat1.third, stat1.fourth, Modifier.weight(1f))
            SavStatCard(stat2.first, stat2.second, stat2.third, stat2.fourth, Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SavStatCard(stat3.first, stat3.second, stat3.third, stat3.fourth, Modifier.weight(1f))
            SavStatCard(stat4.first, stat4.second, stat4.third, stat4.fourth, Modifier.weight(1f))
        }
    }
}

private data class Triple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
private fun QuickActionRow(
    primaryLabel: String,
    primaryIcon: ImageVector,
    onPrimary: () -> Unit,
    secondaryLabel: String,
    secondaryIcon: ImageVector,
    onSecondary: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = onPrimary,
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("dashboard_primary_action"),
            colors = ButtonDefaults.buttonColors(containerColor = SavNavyPrimary),
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(primaryIcon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(primaryLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        OutlinedButton(
            onClick = onSecondary,
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("dashboard_secondary_action"),
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(secondaryIcon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(secondaryLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SectionHeaderRow(title: String, actionLabel: String, onAction: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = onAction) {
            Text(actionLabel)
        }
    }
}
