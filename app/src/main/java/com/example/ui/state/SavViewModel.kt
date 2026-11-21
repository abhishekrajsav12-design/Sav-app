package com.example.ui.state

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AcademicItem
import com.example.data.model.AcademicItemType
import com.example.data.model.ApprovalStatus
import com.example.data.model.CommunityChannel
import com.example.data.model.DirectoryEntry
import com.example.data.model.EventAudience
import com.example.data.model.Notice
import com.example.data.model.NoticeCategory
import com.example.data.model.SavRole
import com.example.data.model.SchoolEvent
import com.example.data.model.SchoolMessage
import com.example.data.model.ServiceRequest
import com.example.data.model.ServiceRequestCategory
import com.example.data.model.ServiceRequestStatus
import com.example.data.model.UserProfile
import com.example.data.repository.DuplicateRegistrationException
import com.example.data.repository.SavRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class AuthEntryIntent {
    WELCOME,
    LOGIN,
    CREATE_ACCOUNT
}

sealed interface GateRouteState {
    data object Loading : GateRouteState
    data class Unauthenticated(val intent: AuthEntryIntent) : GateRouteState
    data class NeedsRegistration(val email: String, val displayName: String, val emailVerified: Boolean) : GateRouteState
    data class DuplicateAccountNotice(val existingProfile: UserProfile) : GateRouteState
    data class PendingApproval(val profile: UserProfile) : GateRouteState
    data class RejectedApplication(val profile: UserProfile) : GateRouteState
    data class ApprovedDashboard(val profile: UserProfile) : GateRouteState
    data class Error(val message: String) : GateRouteState
}

class SavGateViewModel(
    private val repository: SavRepository,
    private val uid: String,
    private val authEmail: String,
    private val authDisplayName: String,
    private val initialEmailVerified: Boolean,
    private val entryIntent: AuthEntryIntent
) : ViewModel() {

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    private val _duplicateDetected = MutableStateFlow<UserProfile?>(null)
    private val _emailVerifiedState = MutableStateFlow(initialEmailVerified)
    val emailVerifiedState: StateFlow<Boolean> = _emailVerifiedState.asStateFlow()

    val profileState: StateFlow<UiState<UserProfile?>> = repository.observeCurrentUserProfile(uid)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w("SavGateVM", "Error observing profile", root)
            emit(UiState.Error(root.message ?: "Unable to verify SAV FAMILY registration."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    fun refreshEmailVerification(isVerifiedNow: Boolean) {
        _emailVerifiedState.value = isVerifiedNow
    }

    fun clearActionError() {
        _actionError.value = null
    }

    fun dismissDuplicateNotice() {
        _duplicateDetected.value = null
    }

    fun resolveGateState(profileUi: UiState<UserProfile?>, overrideToRegistration: Boolean): GateRouteState {
        val dup = _duplicateDetected.value
        if (dup != null) return GateRouteState.DuplicateAccountNotice(dup)

        return when (profileUi) {
            is UiState.Loading -> GateRouteState.Loading
            is UiState.Error -> GateRouteState.Error(profileUi.message)
            is UiState.Success -> {
                val profile = profileUi.data
                if (profile == null) {
                    GateRouteState.NeedsRegistration(
                        email = authEmail,
                        displayName = authDisplayName,
                        emailVerified = _emailVerifiedState.value
                    )
                } else if (entryIntent == AuthEntryIntent.CREATE_ACCOUNT && !overrideToRegistration) {
                    // Existing user picked "Create New Account" -> guard against duplicate!
                    GateRouteState.DuplicateAccountNotice(profile)
                } else {
                    when (profile.parsedStatus()) {
                        ApprovalStatus.APPROVED -> GateRouteState.ApprovedDashboard(profile)
                        ApprovalStatus.PENDING -> GateRouteState.PendingApproval(profile)
                        ApprovalStatus.REJECTED -> GateRouteState.RejectedApplication(profile)
                    }
                }
            }
        }
    }

    fun submitRegistration(
        fullName: String,
        phone: String,
        role: SavRole,
        verificationId: String,
        houseOrDepartment: String,
        classOrDesignation: String,
        wardOrSecondaryInfo: String,
        bio: String,
        onSuccess: (UserProfile) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            _actionError.value = null
            val result = repository.submitRegistration(
                fullName = fullName,
                email = authEmail,
                phone = phone,
                role = role,
                verificationId = verificationId,
                houseOrDepartment = houseOrDepartment,
                classOrDesignation = classOrDesignation,
                wardOrSecondaryInfo = wardOrSecondaryInfo,
                bio = bio,
                emailVerified = _emailVerifiedState.value
            )
            _isSubmitting.value = false
            result.onSuccess { created ->
                onSuccess(created)
            }.onFailure { err ->
                if (err is DuplicateRegistrationException) {
                    _duplicateDetected.value = err.existingProfile
                } else {
                    _actionError.value = err.localizedMessage ?: "Failed to submit registration."
                }
            }
        }
    }

    fun resubmitApplication(
        fullName: String,
        phone: String,
        verificationId: String,
        houseOrDepartment: String,
        classOrDesignation: String,
        wardOrSecondaryInfo: String,
        bio: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            _actionError.value = null
            val result = repository.resubmitRejectedRegistration(
                fullName = fullName,
                phone = phone,
                verificationId = verificationId,
                houseOrDepartment = houseOrDepartment,
                classOrDesignation = classOrDesignation,
                wardOrSecondaryInfo = wardOrSecondaryInfo,
                bio = bio,
                emailVerified = _emailVerifiedState.value
            )
            _isSubmitting.value = false
            result.onSuccess { onSuccess() }
                .onFailure { _actionError.value = it.localizedMessage ?: "Failed to resubmit application." }
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SavMainViewModel(
    private val repository: SavRepository,
    private val currentUserId: String,
    initialProfile: UserProfile
) : ViewModel() {

    private val role = initialProfile.parsedRole()
    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _selectedChannel = MutableStateFlow(CommunityChannel.ALL_COMMUNITY)
    val selectedChannel: StateFlow<CommunityChannel> = _selectedChannel.asStateFlow()

    private val _likedNoticeIds = MutableStateFlow<Set<String>>(emptySet())
    val likedNoticeIds: StateFlow<Set<String>> = _likedNoticeIds.asStateFlow()

    private val _rsvpedEventIds = MutableStateFlow<Set<String>>(emptySet())
    val rsvpedEventIds: StateFlow<Set<String>> = _rsvpedEventIds.asStateFlow()

    private fun <T> Flow<T>.toUiStateFlow(tag: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w("SavMainVM:$tag", "Stream error", root)
                emit(UiState.Error(root.message ?: "Unable to load $tag"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    val profileState: StateFlow<UiState<UserProfile?>> =
        repository.observeCurrentUserProfile(currentUserId).toUiStateFlow("profile")

    val directoryState: StateFlow<UiState<List<DirectoryEntry>>> =
        repository.observeDirectory(currentUserId).toUiStateFlow("directory")

    val noticesState: StateFlow<UiState<List<Notice>>> =
        repository.observeNotices(currentUserId).toUiStateFlow("notices")

    val eventsState: StateFlow<UiState<List<SchoolEvent>>> =
        repository.observeSchoolEvents(currentUserId).toUiStateFlow("events")

    val academicItemsState: StateFlow<UiState<List<AcademicItem>>> =
        if (role in setOf(
                SavRole.PRINCIPAL,
                SavRole.VICE_PRINCIPAL,
                SavRole.TEACHER,
                SavRole.STUDENT,
                SavRole.PARENT
            )
        ) {
            repository.observeAcademicItems(currentUserId).toUiStateFlow("academics")
        } else {
            flowOf(emptyList<AcademicItem>()).toUiStateFlow("academics")
        }

    val applicationsState: StateFlow<UiState<List<UserProfile>>> =
        if (role in setOf(SavRole.PRINCIPAL, SavRole.VICE_PRINCIPAL)) {
            repository.observeAllApplicationsForLeadership(currentUserId).toUiStateFlow("applications")
        } else {
            flowOf(emptyList<UserProfile>()).toUiStateFlow("applications")
        }

    val serviceRequestsState: StateFlow<UiState<List<ServiceRequest>>> =
        repository.observeServiceRequests(currentUserId, role).toUiStateFlow("service_requests")

    val channelMessagesState: StateFlow<UiState<List<SchoolMessage>>> =
        _selectedChannel.flatMapLatest { channel ->
            if (role in channel.allowedRoles) {
                repository.observeChannelMessages(currentUserId, channel.channelId)
            } else {
                flowOf(emptyList())
            }
        }.toUiStateFlow("channel_messages")

    val directMessagesState: StateFlow<UiState<List<SchoolMessage>>> =
        repository.observeDirectMessages(currentUserId).toUiStateFlow("direct_messages")

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun selectChannel(channel: CommunityChannel) {
        if (role in channel.allowedRoles) {
            _selectedChannel.value = channel
        }
    }

    fun reviewApplication(targetUid: String, decision: ApprovalStatus, rejectionReason: String = "") {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.reviewApplication(targetUid, decision, rejectionReason)
            _isBusy.value = false
            res.onSuccess {
                _feedbackMessage.value = if (decision == ApprovalStatus.APPROVED) {
                    "Member approved and granted access to SAV FAMILY."
                } else {
                    "Application declined with feedback sent to applicant."
                }
            }.onFailure {
                _feedbackMessage.value = it.localizedMessage ?: "Failed to update application status."
            }
        }
    }

    fun createNotice(
        authorName: String,
        category: NoticeCategory,
        title: String,
        body: String,
        targetRole: String,
        pinned: Boolean,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.createNotice(authorName, role, category, title, body, targetRole, pinned)
            _isBusy.value = false
            res.onSuccess {
                _feedbackMessage.value = "Published to SAV FAMILY feed."
                onComplete()
            }.onFailure {
                _feedbackMessage.value = it.localizedMessage ?: "Unable to publish post."
            }
        }
    }

    fun toggleNoticeLike(notice: Notice) {
        val currentlyLiked = notice.id in _likedNoticeIds.value
        if (currentlyLiked && notice.likesCount <= 0L) return
        viewModelScope.launch {
            val res = repository.toggleNoticeLike(notice.id, !currentlyLiked)
            res.onSuccess {
                _likedNoticeIds.value = if (currentlyLiked) {
                    _likedNoticeIds.value - notice.id
                } else {
                    _likedNoticeIds.value + notice.id
                }
            }.onFailure {
                _feedbackMessage.value = it.localizedMessage ?: "Failed to update reaction."
            }
        }
    }

    fun deleteNotice(noticeId: String) {
        viewModelScope.launch {
            val res = repository.deleteNotice(noticeId)
            res.onSuccess { _feedbackMessage.value = "Post removed." }
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Unable to delete post." }
        }
    }

    fun createAcademicItem(
        teacherName: String,
        subject: String,
        targetClass: String,
        title: String,
        description: String,
        dueDateLabel: String,
        itemType: AcademicItemType,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.createAcademicItem(
                teacherName, subject, targetClass, title, description, dueDateLabel, itemType
            )
            _isBusy.value = false
            res.onSuccess {
                _feedbackMessage.value = "${itemType.label} published."
                onComplete()
            }.onFailure {
                _feedbackMessage.value = it.localizedMessage ?: "Failed to publish academic item."
            }
        }
    }

    fun deleteAcademicItem(itemId: String) {
        viewModelScope.launch {
            val res = repository.deleteAcademicItem(itemId)
            res.onSuccess { _feedbackMessage.value = "Academic item deleted." }
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Unable to delete item." }
        }
    }

    fun createSchoolEvent(
        organizerName: String,
        title: String,
        description: String,
        eventDateLabel: String,
        location: String,
        audience: EventAudience,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.createSchoolEvent(
                organizerName, role, title, description, eventDateLabel, location, audience
            )
            _isBusy.value = false
            res.onSuccess {
                _feedbackMessage.value = "Campus event added to SAV calendar."
                onComplete()
            }.onFailure {
                _feedbackMessage.value = it.localizedMessage ?: "Failed to create event."
            }
        }
    }

    fun toggleEventRsvp(event: SchoolEvent) {
        val going = event.id in _rsvpedEventIds.value
        if (going && event.rsvpCount <= 0L) return
        viewModelScope.launch {
            val res = repository.toggleEventRsvp(event.id, !going)
            res.onSuccess {
                _rsvpedEventIds.value = if (going) {
                    _rsvpedEventIds.value - event.id
                } else {
                    _rsvpedEventIds.value + event.id
                }
            }.onFailure {
                _feedbackMessage.value = it.localizedMessage ?: "Failed to update RSVP."
            }
        }
    }

    fun sendChannelMessage(senderName: String, body: String, onSent: () -> Unit) {
        val channel = _selectedChannel.value
        viewModelScope.launch {
            val res = repository.sendChannelMessage(senderName, role, channel.channelId, body)
            res.onSuccess { onSent() }
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Failed to send message." }
        }
    }

    fun sendDirectMessage(senderName: String, recipientId: String, body: String, onSent: () -> Unit) {
        viewModelScope.launch {
            val res = repository.sendDirectMessage(senderName, role, recipientId, body)
            res.onSuccess { onSent() }
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Failed to send direct message." }
        }
    }

    fun createServiceRequest(
        requesterName: String,
        category: ServiceRequestCategory,
        title: String,
        details: String,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.createServiceRequest(requesterName, role, category, title, details)
            _isBusy.value = false
            res.onSuccess {
                _feedbackMessage.value = "Request submitted to the concerned SAV desk."
                onComplete()
            }.onFailure {
                _feedbackMessage.value = it.localizedMessage ?: "Failed to submit request."
            }
        }
    }

    fun updateServiceRequestStatus(
        requestId: String,
        status: ServiceRequestStatus,
        resolutionNote: String,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.updateServiceRequestStatus(requestId, status, resolutionNote)
            _isBusy.value = false
            res.onSuccess {
                _feedbackMessage.value = "Request marked as ${status.label}."
                onComplete()
            }.onFailure {
                _feedbackMessage.value = it.localizedMessage ?: "Failed to update request."
            }
        }
    }

    fun updateOwnProfile(
        fullName: String,
        phone: String,
        houseOrDepartment: String,
        classOrDesignation: String,
        wardOrSecondaryInfo: String,
        bio: String,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.updateOwnProfile(
                fullName, phone, houseOrDepartment, classOrDesignation, wardOrSecondaryInfo, bio
            )
            _isBusy.value = false
            res.onSuccess {
                _feedbackMessage.value = "Profile updated."
                onComplete()
            }.onFailure {
                _feedbackMessage.value = it.localizedMessage ?: "Failed to update profile."
            }
        }
    }
}
