package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.DirectoryEntry
import com.example.data.model.UserProfile
import com.example.ui.state.SavMainViewModel
import com.example.ui.state.UiState
import com.example.ui.theme.SavGoldPrimary
import com.example.ui.theme.SavNavyPrimary

private enum class MainTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    DASHBOARD("Home", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    COMMUNITY("Feed", Icons.Filled.Campaign, Icons.Outlined.Campaign),
    MESSAGES("Messages", Icons.Filled.Forum, Icons.Outlined.Forum),
    CAMPUS("Campus", Icons.Filled.Groups, Icons.Outlined.Groups),
    PROFILE("Profile", Icons.Filled.Person, Icons.Outlined.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppShell(
    viewModel: SavMainViewModel,
    initialProfile: UserProfile,
    rememberMe: Boolean,
    themeMode: String,
    onRememberMeChange: (Boolean) -> Unit,
    onThemeModeChange: (String) -> Unit,
    onSignOut: () -> Unit
) {
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val directoryState by viewModel.directoryState.collectAsStateWithLifecycle()
    val noticesState by viewModel.noticesState.collectAsStateWithLifecycle()
    val eventsState by viewModel.eventsState.collectAsStateWithLifecycle()
    val academicState by viewModel.academicItemsState.collectAsStateWithLifecycle()
    val applicationsState by viewModel.applicationsState.collectAsStateWithLifecycle()
    val requestsState by viewModel.serviceRequestsState.collectAsStateWithLifecycle()
    val channelMessagesState by viewModel.channelMessagesState.collectAsStateWithLifecycle()
    val directMessagesState by viewModel.directMessagesState.collectAsStateWithLifecycle()
    val selectedChannel by viewModel.selectedChannel.collectAsStateWithLifecycle()
    val likedNoticeIds by viewModel.likedNoticeIds.collectAsStateWithLifecycle()
    val rsvpedEventIds by viewModel.rsvpedEventIds.collectAsStateWithLifecycle()
    val feedbackMessage by viewModel.feedbackMessage.collectAsStateWithLifecycle()

    val profile = (profileState as? UiState.Success)?.data ?: initialProfile
    val directory = (directoryState as? UiState.Success)?.data.orEmpty()
    val notices = (noticesState as? UiState.Success)?.data.orEmpty()
    val events = (eventsState as? UiState.Success)?.data.orEmpty()
    val academicItems = (academicState as? UiState.Success)?.data.orEmpty()
    val applications = (applicationsState as? UiState.Success)?.data.orEmpty()
    val requests = (requestsState as? UiState.Success)?.data.orEmpty()
    val channelMessages = (channelMessagesState as? UiState.Success)?.data.orEmpty()
    val directMessages = (directMessagesState as? UiState.Success)?.data.orEmpty()

    val combinedError = listOfNotNull(
        (noticesState as? UiState.Error)?.message,
        (directoryState as? UiState.Error)?.message,
        (requestsState as? UiState.Error)?.message
    ).firstOrNull()

    val isLoading = noticesState is UiState.Loading && notices.isEmpty()

    var currentTab by remember { mutableStateOf(MainTab.DASHBOARD) }
    var campusSubTab by remember { mutableIntStateOf(0) }
    var preselectedPeer by remember { mutableStateOf<DirectoryEntry?>(null) }
    var showComposeNoticeDialog by remember { mutableStateOf(false) }
    var showCreateAcademicDialog by remember { mutableStateOf(false) }
    var showCreateRequestDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(feedbackMessage) {
        val msg = feedbackMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    BackHandler(enabled = currentTab != MainTab.DASHBOARD) {
        currentTab = MainTab.DASHBOARD
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_sav_emblem),
                                contentDescription = "SAV FAMILY",
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "SAV FAMILY",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = SavNavyPrimary
                                )
                                Text(
                                    text = "${profile.parsedRole().displayName} • ${currentTab.label}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SavGoldPrimary
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        MainTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail {
                        MainTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) }
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (currentTab) {
                        MainTab.DASHBOARD -> RoleDashboardScreen(
                            profile = profile,
                            directory = directory,
                            notices = notices,
                            events = events,
                            academicItems = academicItems,
                            applications = applications,
                            serviceRequests = requests,
                            isLoading = isLoading,
                            errorMessage = combinedError,
                            onOpenCommunity = { currentTab = MainTab.COMMUNITY },
                            onOpenMessages = { currentTab = MainTab.MESSAGES },
                            onOpenCampusTab = { subIdx ->
                                campusSubTab = subIdx
                                currentTab = MainTab.CAMPUS
                            },
                            onOpenComposeNotice = {
                                currentTab = MainTab.COMMUNITY
                                showComposeNoticeDialog = true
                            },
                            onOpenCreateAcademic = {
                                campusSubTab = if (profile.parsedRole().name in setOf("PRINCIPAL", "VICE_PRINCIPAL")) 1 else 0
                                currentTab = MainTab.CAMPUS
                                showCreateAcademicDialog = true
                            },
                            onOpenCreateRequest = {
                                campusSubTab = if (profile.parsedRole().name in setOf("PRINCIPAL", "VICE_PRINCIPAL")) 2
                                else if (profile.parsedRole().name in setOf("TEACHER", "STUDENT", "PARENT")) 1
                                else 0
                                currentTab = MainTab.CAMPUS
                                showCreateRequestDialog = true
                            }
                        )

                        MainTab.COMMUNITY -> CommunityAndEventsScreen(
                            profile = profile,
                            notices = notices,
                            events = events,
                            likedNoticeIds = likedNoticeIds,
                            rsvpedEventIds = rsvpedEventIds,
                            showComposeNoticeDialog = showComposeNoticeDialog,
                            onOpenComposeNotice = { showComposeNoticeDialog = true },
                            onDismissComposeNotice = { showComposeNoticeDialog = false },
                            onCreateNotice = { cat, title, body, targetRole, pinned ->
                                viewModel.createNotice(
                                    authorName = profile.fullName,
                                    category = cat,
                                    title = title,
                                    body = body,
                                    targetRole = targetRole,
                                    pinned = pinned,
                                    onComplete = { showComposeNoticeDialog = false }
                                )
                            },
                            onToggleLike = viewModel::toggleNoticeLike,
                            onDeleteNotice = viewModel::deleteNotice,
                            onCreateEvent = { title, desc, dateLabel, loc, aud ->
                                viewModel.createSchoolEvent(
                                    organizerName = profile.fullName,
                                    title = title,
                                    description = desc,
                                    eventDateLabel = dateLabel,
                                    location = loc,
                                    audience = aud,
                                    onComplete = {}
                                )
                            },
                            onToggleRsvp = viewModel::toggleEventRsvp
                        )

                        MainTab.MESSAGES -> MessagingScreen(
                            profile = profile,
                            selectedChannel = selectedChannel,
                            channelMessages = channelMessages,
                            directMessages = directMessages,
                            directory = directory,
                            preselectedDirectPeer = preselectedPeer,
                            onSelectChannel = viewModel::selectChannel,
                            onSendChannelMessage = { body, onSent ->
                                viewModel.sendChannelMessage(profile.fullName, body, onSent)
                            },
                            onSendDirectMessage = { recipientId, body, onSent ->
                                viewModel.sendDirectMessage(profile.fullName, recipientId, body, onSent)
                            }
                        )

                        MainTab.CAMPUS -> CampusManagementScreen(
                            profile = profile,
                            requestedSubTabIndex = campusSubTab,
                            applications = applications,
                            academicItems = academicItems,
                            serviceRequests = requests,
                            directory = directory,
                            showCreateAcademicDialog = showCreateAcademicDialog,
                            showCreateRequestDialog = showCreateRequestDialog,
                            onOpenCreateAcademic = { showCreateAcademicDialog = true },
                            onDismissCreateAcademic = { showCreateAcademicDialog = false },
                            onOpenCreateRequest = { showCreateRequestDialog = true },
                            onDismissCreateRequest = { showCreateRequestDialog = false },
                            onReviewApplication = viewModel::reviewApplication,
                            onCreateAcademicItem = { subject, targetClass, title, desc, due, type ->
                                viewModel.createAcademicItem(
                                    teacherName = profile.fullName,
                                    subject = subject,
                                    targetClass = targetClass,
                                    title = title,
                                    description = desc,
                                    dueDateLabel = due,
                                    itemType = type,
                                    onComplete = { showCreateAcademicDialog = false }
                                )
                            },
                            onDeleteAcademicItem = viewModel::deleteAcademicItem,
                            onCreateServiceRequest = { cat, title, details ->
                                viewModel.createServiceRequest(
                                    requesterName = profile.fullName,
                                    category = cat,
                                    title = title,
                                    details = details,
                                    onComplete = { showCreateRequestDialog = false }
                                )
                            },
                            onUpdateServiceRequestStatus = { reqId, status, note ->
                                viewModel.updateServiceRequestStatus(reqId, status, note) {}
                            },
                            onMessageMember = { member ->
                                preselectedPeer = member
                                currentTab = MainTab.MESSAGES
                            }
                        )

                        MainTab.PROFILE -> ProfileScreen(
                            profile = profile,
                            rememberMe = rememberMe,
                            themeMode = themeMode,
                            onRememberMeChange = onRememberMeChange,
                            onThemeModeChange = onThemeModeChange,
                            onUpdateProfile = { name, phone, dept, desig, sec, bio, done ->
                                viewModel.updateOwnProfile(name, phone, dept, desig, sec, bio, done)
                            },
                            onSignOut = onSignOut
                        )
                    }
                }
            }
        }
    }
}
