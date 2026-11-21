package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AcademicItem
import com.example.data.model.AcademicItemType
import com.example.data.model.ApprovalStatus
import com.example.data.model.DirectoryEntry
import com.example.data.model.SavRole
import com.example.data.model.ServiceRequest
import com.example.data.model.ServiceRequestCategory
import com.example.data.model.ServiceRequestStatus
import com.example.data.model.UserProfile
import com.example.ui.components.ApprovalStatusPill
import com.example.ui.components.RequestStatusPill
import com.example.ui.components.SavEmptyState
import com.example.ui.components.SavRoleBadge
import com.example.ui.theme.SavCrimson
import com.example.ui.theme.SavEmerald
import com.example.ui.theme.SavGoldPrimary
import com.example.ui.theme.SavNavyPrimary
import com.example.ui.theme.SavPureWhite

private enum class CampusSubTab(val title: String) {
    APPROVALS("Approvals"),
    ACADEMICS("Academics"),
    REQUESTS("Requests"),
    DIRECTORY("Directory")
}

@Composable
fun CampusManagementScreen(
    profile: UserProfile,
    requestedSubTabIndex: Int,
    applications: List<UserProfile>,
    academicItems: List<AcademicItem>,
    serviceRequests: List<ServiceRequest>,
    directory: List<DirectoryEntry>,
    showCreateAcademicDialog: Boolean,
    showCreateRequestDialog: Boolean,
    onOpenCreateAcademic: () -> Unit,
    onDismissCreateAcademic: () -> Unit,
    onOpenCreateRequest: () -> Unit,
    onDismissCreateRequest: () -> Unit,
    onReviewApplication: (String, ApprovalStatus, String) -> Unit,
    onCreateAcademicItem: (String, String, String, String, String, AcademicItemType) -> Unit,
    onDeleteAcademicItem: (String) -> Unit,
    onCreateServiceRequest: (ServiceRequestCategory, String, String) -> Unit,
    onUpdateServiceRequestStatus: (String, ServiceRequestStatus, String) -> Unit,
    onMessageMember: (DirectoryEntry) -> Unit
) {
    val role = profile.parsedRole()
    val tabs = remember(role) {
        buildList {
            if (role in setOf(SavRole.PRINCIPAL, SavRole.VICE_PRINCIPAL)) {
                add(CampusSubTab.APPROVALS)
            }
            if (role in setOf(
                    SavRole.PRINCIPAL,
                    SavRole.VICE_PRINCIPAL,
                    SavRole.TEACHER,
                    SavRole.STUDENT,
                    SavRole.PARENT
                )
            ) {
                add(CampusSubTab.ACADEMICS)
            }
            add(CampusSubTab.REQUESTS)
            add(CampusSubTab.DIRECTORY)
        }
    }

    var selectedIndex by remember(requestedSubTabIndex, tabs) {
        mutableIntStateOf(requestedSubTabIndex.coerceIn(0, tabs.lastIndex))
    }
    val activeTab = tabs[selectedIndex]

    val canPostAcademic = role in setOf(SavRole.PRINCIPAL, SavRole.VICE_PRINCIPAL, SavRole.TEACHER)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScrollableTabRow(
                selectedTabIndex = selectedIndex,
                edgePadding = 16.dp
            ) {
                tabs.forEachIndexed { index, tab ->
                    val badgeCount = if (tab == CampusSubTab.APPROVALS) {
                        applications.count { it.parsedStatus() == ApprovalStatus.PENDING }
                    } else 0
                    Tab(
                        selected = selectedIndex == index,
                        onClick = { selectedIndex = index },
                        text = {
                            Text(
                                if (badgeCount > 0) "${tab.title} ($badgeCount)" else tab.title
                            )
                        }
                    )
                }
            }

            when (activeTab) {
                CampusSubTab.APPROVALS -> ApprovalsQueueContent(
                    applications = applications,
                    onReview = onReviewApplication
                )

                CampusSubTab.ACADEMICS -> AcademicsHubContent(
                    role = role,
                    userId = profile.userId,
                    items = academicItems,
                    onDelete = onDeleteAcademicItem
                )

                CampusSubTab.REQUESTS -> ServiceRequestsContent(
                    role = role,
                    userId = profile.userId,
                    requests = serviceRequests,
                    onUpdateStatus = onUpdateServiceRequestStatus
                )

                CampusSubTab.DIRECTORY -> DirectoryContent(
                    currentUserId = profile.userId,
                    directory = directory,
                    onMessageMember = onMessageMember
                )
            }
        }

        if ((activeTab == CampusSubTab.ACADEMICS && canPostAcademic) || activeTab == CampusSubTab.REQUESTS) {
            FloatingActionButton(
                onClick = {
                    if (activeTab == CampusSubTab.ACADEMICS) onOpenCreateAcademic()
                    else onOpenCreateRequest()
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("campus_fab"),
                containerColor = SavGoldPrimary,
                contentColor = SavNavyPrimary
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Create item")
            }
        }
    }

    if (showCreateAcademicDialog) {
        CreateAcademicItemDialog(
            onDismiss = onDismissCreateAcademic,
            onSubmit = { subject, targetClass, title, desc, due, type ->
                onCreateAcademicItem(subject, targetClass, title, desc, due, type)
            }
        )
    }

    if (showCreateRequestDialog) {
        CreateServiceRequestDialog(
            role = role,
            onDismiss = onDismissCreateRequest,
            onSubmit = onCreateServiceRequest
        )
    }
}

@Composable
private fun ApprovalsQueueContent(
    applications: List<UserProfile>,
    onReview: (String, ApprovalStatus, String) -> Unit
) {
    var statusFilter by remember { mutableStateOf<ApprovalStatus?>(ApprovalStatus.PENDING) }
    var rejectTarget by remember { mutableStateOf<UserProfile?>(null) }
    var rejectionReason by remember { mutableStateOf("") }

    val filtered = remember(applications, statusFilter) {
        if (statusFilter == null) applications
        else applications.filter { it.parsedStatus() == statusFilter }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = statusFilter == null,
                    onClick = { statusFilter = null },
                    label = { Text("All (${applications.size})") }
                )
            }
            items(ApprovalStatus.entries) { status ->
                val count = applications.count { it.parsedStatus() == status }
                FilterChip(
                    selected = statusFilter == status,
                    onClick = { statusFilter = status },
                    label = { Text("${status.name.lowercase().replaceFirstChar { it.uppercase() }} ($count)") }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (filtered.isEmpty()) {
                item {
                    SavEmptyState(
                        title = "No Applications in This Filter",
                        description = "New registration requests requiring school identity verification will appear here."
                    )
                }
            } else {
                items(filtered, key = { it.userId }) { applicant ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SavRoleBadge(applicant.parsedRole())
                                ApprovalStatusPill(applicant.parsedStatus())
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(applicant.fullName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "${applicant.email} • ${applicant.phone}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Verification ID: ${applicant.verificationId}",
                                style = MaterialTheme.typography.labelMedium,
                                color = SavNavyPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${applicant.parsedRole().departmentLabel}: ${applicant.houseOrDepartment} • ${applicant.classOrDesignation}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (!applicant.wardOrSecondaryInfo.isNullOrBlank()) {
                                Text(
                                    text = applicant.wardOrSecondaryInfo,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (applicant.parsedStatus() != ApprovalStatus.APPROVED) {
                                Spacer(Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = { onReview(applicant.userId, ApprovalStatus.APPROVED, "") },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = SavEmerald,
                                            contentColor = SavPureWhite
                                        )
                                    ) {
                                        Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Approve")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            rejectTarget = applicant
                                            rejectionReason = ""
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SavCrimson)
                                    ) {
                                        Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Reject")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (rejectTarget != null) {
        AlertDialog(
            onDismissRequest = { rejectTarget = null },
            title = { Text("Decline Application") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Provide clear feedback so ${rejectTarget!!.fullName} can correct and resubmit.")
                    OutlinedTextField(
                        value = rejectionReason,
                        onValueChange = { rejectionReason = it },
                        label = { Text("Rejection Reason") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReview(rejectTarget!!.userId, ApprovalStatus.REJECTED, rejectionReason)
                        rejectTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SavCrimson)
                ) {
                    Text("Confirm Reject")
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectTarget = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun AcademicsHubContent(
    role: SavRole,
    userId: String,
    items: List<AcademicItem>,
    onDelete: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (items.isEmpty()) {
            item {
                SavEmptyState(
                    title = "No Academic Items Yet",
                    description = "Assignments, exam schedules, and study materials published by SAV teachers will appear here.",
                    useHallArt = true
                )
            }
        } else {
            items(items, key = { it.id }) { item ->
                val canDelete = item.teacherId == userId || role in setOf(SavRole.PRINCIPAL, SavRole.VICE_PRINCIPAL)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = "${item.parsedType().label} • ${item.targetClass}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                text = item.dueDateLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = SavGoldPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("${item.subject}: ${item.title}", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "By ${item.teacherName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (canDelete) {
                                IconButton(onClick = { onDelete(item.id) }) {
                                    Icon(Icons.Outlined.Delete, contentDescription = "Delete academic item")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceRequestsContent(
    role: SavRole,
    userId: String,
    requests: List<ServiceRequest>,
    onUpdateStatus: (String, ServiceRequestStatus, String) -> Unit
) {
    var reviewTarget by remember { mutableStateOf<ServiceRequest?>(null) }
    var resolutionNote by remember { mutableStateOf("") }

    fun canHandleRequest(req: ServiceRequest): Boolean {
        if (role in setOf(SavRole.PRINCIPAL, SavRole.VICE_PRINCIPAL)) return true
        val cat = req.parsedCategory()
        if (role == SavRole.TEACHER && cat in setOf(ServiceRequestCategory.PARENT_INQUIRY, ServiceRequestCategory.ACADEMIC_SUPPORT)) {
            return true
        }
        if (role in setOf(SavRole.OTHER_STAFF, SavRole.NON_TEACHING_STAFF) &&
            cat in setOf(ServiceRequestCategory.HOSTEL_WELFARE, ServiceRequestCategory.FACILITY_MAINTENANCE)
        ) {
            return true
        }
        return false
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (requests.isEmpty()) {
            item {
                SavEmptyState(
                    title = "No Requests Found",
                    description = "Submit leave requests, hostel welfare tickets, facility repairs, or parent inquiries using the + button."
                )
            }
        } else {
            items(requests, key = { it.id }) { req ->
                val handlerAllowed = canHandleRequest(req) &&
                    (role in setOf(SavRole.PRINCIPAL, SavRole.VICE_PRINCIPAL) ||
                        req.parsedStatus() !in setOf(ServiceRequestStatus.RESOLVED, ServiceRequestStatus.DECLINED))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = req.parsedCategory().label,
                                style = MaterialTheme.typography.labelMedium,
                                color = SavGoldPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            RequestStatusPill(req.parsedStatus())
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(req.title, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = req.details,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Submitted by ${req.requesterName} (${req.parsedRequesterRole().shortLabel})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!req.resolutionNote.isNullOrBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Desk Note: ${req.resolutionNote}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                        if (handlerAllowed && req.requesterId != userId) {
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = {
                                    reviewTarget = req
                                    resolutionNote = req.resolutionNote.orEmpty()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Update Ticket Status")
                            }
                        }
                    }
                }
            }
        }
    }

    if (reviewTarget != null) {
        AlertDialog(
            onDismissRequest = { reviewTarget = null },
            title = { Text("Manage Request") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(reviewTarget!!.title, style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(
                        value = resolutionNote,
                        onValueChange = { resolutionNote = it },
                        label = { Text("Resolution / Action Note") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            onUpdateStatus(reviewTarget!!.id, ServiceRequestStatus.IN_REVIEW, resolutionNote)
                            reviewTarget = null
                        }
                    ) { Text("In Review") }
                    Button(
                        onClick = {
                            onUpdateStatus(reviewTarget!!.id, ServiceRequestStatus.RESOLVED, resolutionNote)
                            reviewTarget = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SavEmerald)
                    ) { Text("Resolve") }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onUpdateStatus(reviewTarget!!.id, ServiceRequestStatus.DECLINED, resolutionNote)
                        reviewTarget = null
                    }
                ) { Text("Decline", color = SavCrimson) }
            }
        )
    }
}

@Composable
private fun DirectoryContent(
    currentUserId: String,
    directory: List<DirectoryEntry>,
    onMessageMember: (DirectoryEntry) -> Unit
) {
    var roleFilter by remember { mutableStateOf<SavRole?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(directory, roleFilter, searchQuery) {
        directory.filter { entry ->
            val roleOk = roleFilter == null || entry.parsedRole() == roleFilter
            val queryOk = searchQuery.isBlank() ||
                entry.fullName.contains(searchQuery, ignoreCase = true) ||
                entry.houseOrDepartment.contains(searchQuery, ignoreCase = true) ||
                entry.classOrDesignation.contains(searchQuery, ignoreCase = true)
            roleOk && queryOk
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            placeholder = { Text("Search SAV members, house, batch...") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = roleFilter == null,
                    onClick = { roleFilter = null },
                    label = { Text("All (${directory.size})") }
                )
            }
            items(SavRole.entries) { r ->
                FilterChip(
                    selected = roleFilter == r,
                    onClick = { roleFilter = if (roleFilter == r) null else r },
                    label = { Text(r.shortLabel) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (filtered.isEmpty()) {
                item {
                    SavEmptyState(
                        title = "No Matching Members",
                        description = "Try clearing the search query or role filter."
                    )
                }
            } else {
                items(filtered, key = { it.userId }) { member ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(member.fullName, style = MaterialTheme.typography.titleSmall)
                                Spacer(Modifier.height(4.dp))
                                SavRoleBadge(member.parsedRole())
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "${member.houseOrDepartment} • ${member.classOrDesignation}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (member.userId != currentUserId) {
                                IconButton(onClick = { onMessageMember(member) }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Forum,
                                        contentDescription = "Message ${member.fullName}",
                                        tint = SavNavyPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateAcademicItemDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, String, String, AcademicItemType) -> Unit
) {
    var subject by remember { mutableStateOf("") }
    var targetClass by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dueDateLabel by remember { mutableStateOf("") }
    var itemType by remember { mutableStateOf(AcademicItemType.ASSIGNMENT) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Publish Academic Item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(AcademicItemType.entries) { type ->
                        FilterChip(
                            selected = itemType == type,
                            onClick = { itemType = type },
                            label = { Text(type.label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = targetClass,
                    onValueChange = { targetClass = it },
                    label = { Text("Target Class (e.g. Class X-A)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dueDateLabel,
                    onValueChange = { dueDateLabel = it },
                    label = { Text("Due / Exam Date") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Instructions / Syllabus") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(subject, targetClass, title, description, dueDateLabel, itemType) },
                enabled = subject.trim().length >= 2 && targetClass.trim().isNotEmpty() &&
                    title.trim().length >= 3 && description.trim().length >= 3 && dueDateLabel.trim().length >= 2,
                colors = ButtonDefaults.buttonColors(containerColor = SavNavyPrimary)
            ) {
                Text("Publish")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun CreateServiceRequestDialog(
    role: SavRole,
    onDismiss: () -> Unit,
    onSubmit: (ServiceRequestCategory, String, String) -> Unit
) {
    val defaultCategory = remember(role) {
        when (role) {
            SavRole.PARENT -> ServiceRequestCategory.PARENT_INQUIRY
            SavRole.STUDENT -> ServiceRequestCategory.HOSTEL_WELFARE
            SavRole.NON_TEACHING_STAFF -> ServiceRequestCategory.FACILITY_MAINTENANCE
            else -> ServiceRequestCategory.LEAVE_REQUEST
        }
    }
    var category by remember { mutableStateOf(defaultCategory) }
    var title by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Submit SAV Request") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ServiceRequestCategory.entries) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Subject") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text("Details") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(category, title, details) },
                enabled = title.trim().length >= 3 && details.trim().length >= 5,
                colors = ButtonDefaults.buttonColors(containerColor = SavNavyPrimary)
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
