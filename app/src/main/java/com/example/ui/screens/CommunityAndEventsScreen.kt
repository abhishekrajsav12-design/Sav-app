package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PushPin
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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import com.example.data.model.EventAudience
import com.example.data.model.Notice
import com.example.data.model.NoticeCategory
import com.example.data.model.SavRole
import com.example.data.model.SchoolEvent
import com.example.data.model.UserProfile
import com.example.ui.components.SavEmptyState
import com.example.ui.components.SavRoleBadge
import com.example.ui.components.formatSavTimestamp
import com.example.ui.theme.SavCrimson
import com.example.ui.theme.SavGoldPrimary
import com.example.ui.theme.SavGoldSoft
import com.example.ui.theme.SavNavyPrimary
import com.example.ui.theme.SavPureWhite

fun allowedNoticeCategoriesFor(role: SavRole): List<NoticeCategory> = buildList {
    if (role in setOf(SavRole.PRINCIPAL, SavRole.VICE_PRINCIPAL)) {
        add(NoticeCategory.OFFICIAL_CIRCULAR)
    }
    if (role in setOf(SavRole.PRINCIPAL, SavRole.VICE_PRINCIPAL, SavRole.TEACHER)) {
        add(NoticeCategory.ACADEMIC_UPDATE)
    }
    if (role in setOf(SavRole.PRINCIPAL, SavRole.VICE_PRINCIPAL, SavRole.OTHER_STAFF, SavRole.NON_TEACHING_STAFF)) {
        add(NoticeCategory.CAMPUS_OPERATIONS)
    }
    if (role in setOf(SavRole.PRINCIPAL, SavRole.VICE_PRINCIPAL, SavRole.ALUMNI)) {
        add(NoticeCategory.ALUMNI_SPOTLIGHT)
    }
    add(NoticeCategory.COMMUNITY_POST)
}

fun canCreateEvents(role: SavRole): Boolean =
    role in setOf(SavRole.PRINCIPAL, SavRole.VICE_PRINCIPAL, SavRole.TEACHER, SavRole.ALUMNI, SavRole.OTHER_STAFF)

@Composable
fun CommunityAndEventsScreen(
    profile: UserProfile,
    notices: List<Notice>,
    events: List<SchoolEvent>,
    likedNoticeIds: Set<String>,
    rsvpedEventIds: Set<String>,
    showComposeNoticeDialog: Boolean,
    onOpenComposeNotice: () -> Unit,
    onDismissComposeNotice: () -> Unit,
    onCreateNotice: (NoticeCategory, String, String, String, Boolean) -> Unit,
    onToggleLike: (Notice) -> Unit,
    onDeleteNotice: (String) -> Unit,
    onCreateEvent: (String, String, String, String, EventAudience) -> Unit,
    onToggleRsvp: (SchoolEvent) -> Unit
) {
    val role = profile.parsedRole()
    var subTab by remember { mutableIntStateOf(0) } // 0: Feed & Circulars, 1: Campus Events
    var selectedCategoryFilter by remember { mutableStateOf<NoticeCategory?>(null) }
    var showCreateEventDialog by remember { mutableStateOf(false) }

    val filteredNotices = remember(notices, selectedCategoryFilter) {
        if (selectedCategoryFilter == null) notices
        else notices.filter { it.parsedCategory() == selectedCategoryFilter }
    }

    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                SegmentedButton(
                    selected = subTab == 0,
                    onClick = { subTab = 0 },
                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                ) {
                    Text("Feed & Circulars")
                }
                SegmentedButton(
                    selected = subTab == 1,
                    onClick = { subTab = 1 },
                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                ) {
                    Text("Campus Events")
                }
            }

            if (subTab == 0) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategoryFilter == null,
                            onClick = { selectedCategoryFilter = null },
                            label = { Text("All") }
                        )
                    }
                    items(NoticeCategory.entries) { cat ->
                        FilterChip(
                            selected = selectedCategoryFilter == cat,
                            onClick = {
                                selectedCategoryFilter = if (selectedCategoryFilter == cat) null else cat
                            },
                            label = { Text(cat.label) }
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (filteredNotices.isEmpty()) {
                        item {
                            SavEmptyState(
                                title = "No Notices Yet",
                                description = "Share a community update or wait for school circulars to appear here."
                            )
                        }
                    } else {
                        items(filteredNotices, key = { it.id }) { notice ->
                            val isLiked = notice.id in likedNoticeIds
                            val canDelete = notice.authorId == profile.userId ||
                                role in setOf(SavRole.PRINCIPAL, SavRole.VICE_PRINCIPAL)
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
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            SavRoleBadge(notice.parsedAuthorRole())
                                            Surface(
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                shape = MaterialTheme.shapes.small
                                            ) {
                                                Text(
                                                    text = notice.parsedCategory().label,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                        if (notice.pinned) {
                                            Icon(
                                                imageVector = Icons.Outlined.PushPin,
                                                contentDescription = "Pinned",
                                                tint = SavGoldPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(10.dp))
                                    Text(notice.title, style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = notice.body,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = notice.authorName,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = formatSavTimestamp(notice.createdAt),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            TextButton(onClick = { onToggleLike(notice) }) {
                                                Icon(
                                                    imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                                    contentDescription = "Like",
                                                    tint = if (isLiked) SavCrimson else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text("${notice.likesCount}")
                                            }
                                            if (canDelete) {
                                                IconButton(onClick = { onDeleteNotice(notice.id) }) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Delete,
                                                        contentDescription = "Delete notice",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
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
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (events.isEmpty()) {
                        item {
                            SavEmptyState(
                                title = "No Upcoming Events",
                                description = "School assemblies, house competitions, and alumni gatherings will be listed here.",
                                useHallArt = true
                            )
                        }
                    } else {
                        items(events, key = { it.id }) { event ->
                            val isGoing = event.id in rsvpedEventIds
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
                                            color = SavGoldSoft,
                                            contentColor = SavNavyPrimary,
                                            shape = MaterialTheme.shapes.small
                                        ) {
                                            Text(
                                                text = event.eventDateLabel,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                        Text(
                                            text = event.parsedAudience().label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    Text(event.title, style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = event.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.LocationOn,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = SavGoldPrimary
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "${event.location} • Hosted by ${event.organizerName}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${event.rsvpCount} attending",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (isGoing) {
                                            OutlinedButton(onClick = { onToggleRsvp(event) }) {
                                                Icon(Icons.Outlined.EventAvailable, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text("Attending")
                                            }
                                        } else {
                                            Button(
                                                onClick = { onToggleRsvp(event) },
                                                colors = ButtonDefaults.buttonColors(containerColor = SavNavyPrimary)
                                            ) {
                                                Text("RSVP Going")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (subTab == 0 || canCreateEvents(role)) {
            FloatingActionButton(
                onClick = {
                    if (subTab == 0) onOpenComposeNotice() else showCreateEventDialog = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("community_fab"),
                containerColor = SavGoldPrimary,
                contentColor = SavNavyPrimary
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = if (subTab == 0) "Create post" else "Create event"
                )
            }
        }
    }

    if (showComposeNoticeDialog) {
        ComposeNoticeDialog(
            role = role,
            onDismiss = onDismissComposeNotice,
            onSubmit = onCreateNotice
        )
    }

    if (showCreateEventDialog) {
        CreateEventDialog(
            onDismiss = { showCreateEventDialog = false },
            onSubmit = { title, desc, dateLabel, loc, audience ->
                onCreateEvent(title, desc, dateLabel, loc, audience)
                showCreateEventDialog = false
            }
        )
    }
}

@Composable
private fun ComposeNoticeDialog(
    role: SavRole,
    onDismiss: () -> Unit,
    onSubmit: (NoticeCategory, String, String, String, Boolean) -> Unit
) {
    val allowedCategories = remember(role) { allowedNoticeCategoriesFor(role) }
    var category by remember { mutableStateOf(allowedCategories.first()) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var pinned by remember { mutableStateOf(false) }
    val canPin = role in setOf(SavRole.PRINCIPAL, SavRole.VICE_PRINCIPAL)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Publish to SAV FAMILY") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Category", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(allowedCategories) { cat ->
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
                    label = { Text("Headline") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Message / Announcement") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                if (canPin) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Pin to top of SAV Board", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = pinned, onCheckedChange = { pinned = it })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(category, title, body, "ALL", pinned) },
                enabled = title.trim().length >= 3 && body.trim().length >= 5,
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
private fun CreateEventDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, String, EventAudience) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dateLabel by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var audience by remember { mutableStateOf(EventAudience.ALL) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule Campus Event") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Event Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dateLabel,
                    onValueChange = { dateLabel = it },
                    label = { Text("Date & Time (e.g. Sat, 28 Nov • 9:00 AM)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Campus Venue") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Details") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(EventAudience.entries) { aud ->
                        FilterChip(
                            selected = audience == aud,
                            onClick = { audience = aud },
                            label = { Text(aud.label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(title, description, dateLabel, location, audience) },
                enabled = title.trim().length >= 3 && description.trim().length >= 5 &&
                    dateLabel.trim().length >= 2 && location.trim().length >= 2,
                colors = ButtonDefaults.buttonColors(containerColor = SavNavyPrimary)
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
