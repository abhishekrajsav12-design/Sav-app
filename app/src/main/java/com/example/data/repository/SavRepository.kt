package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.AcademicItem
import com.example.data.model.AcademicItemType
import com.example.data.model.ApprovalStatus
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
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

class DuplicateRegistrationException(val existingProfile: UserProfile) :
    IllegalStateException("An account is already registered for this Google email.")

class SavRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun <T> Flow<T>.logAndRethrowFirestoreErrors(
        uid: String,
        opType: OperationType,
        path: String
    ): Flow<T> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, opType, path)
        }
        throw error
    }

    fun observeCurrentUserProfile(uid: String): Flow<UserProfile?> = flow {
        if (uid.isBlank() || auth.currentUser?.uid != uid) {
            emit(null)
            return@flow
        }
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .logAndRethrowFirestoreErrors(uid, OperationType.GET, path)
        )
    }

    suspend fun fetchCurrentUserProfile(uid: String): Result<UserProfile?> {
        val path = "users/$uid"
        return try {
            val activeUid = requireUserId()
            check(activeUid == uid) { "Session user mismatch." }
            val snapshot = db.collection("users").document(uid).get().await()
            val profile = if (snapshot.exists()) {
                snapshot.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            } else null
            Result.success(profile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun submitRegistration(
        fullName: String,
        email: String,
        phone: String,
        role: SavRole,
        verificationId: String,
        houseOrDepartment: String,
        classOrDesignation: String,
        wardOrSecondaryInfo: String,
        bio: String,
        emailVerified: Boolean
    ): Result<UserProfile> {
        var path = "users"
        return try {
            val uid = requireUserId()
            path = "users/$uid"
            val existingSnap = db.collection("users").document(uid).get().await()
            if (existingSnap.exists()) {
                val existing = existingSnap.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (existing != null) {
                    return Result.failure(DuplicateRegistrationException(existing))
                }
            }

            val cleanName = fullName.trim().take(80)
            val cleanEmail = email.trim().take(120)
            val cleanPhone = phone.trim().take(20)
            val cleanVerId = verificationId.trim().take(60)
            val cleanDept = houseOrDepartment.trim().take(80)
            val cleanDesig = classOrDesignation.trim().take(80)
            val cleanSecondary = wardOrSecondaryInfo.trim().take(120).takeIf { it.isNotBlank() }
            val cleanBio = bio.trim().take(300).takeIf { it.isNotBlank() }

            require(cleanName.length >= 2) { "Full name must be at least 2 characters." }
            require(cleanEmail.length >= 3) { "Valid email is required." }
            require(cleanPhone.length >= 7) { "Phone number must be at least 7 digits." }
            require(cleanVerId.length >= 2) { "Identity verification ID is required." }
            require(cleanDept.isNotEmpty()) { "${role.departmentLabel} is required." }
            require(cleanDesig.isNotEmpty()) { "${role.designationLabel} is required." }

            val isBootstrapPrincipal =
                cleanEmail.equals("chandragupt0040@gmail.com", ignoreCase = true) &&
                    emailVerified &&
                    role == SavRole.PRINCIPAL

            val initialStatus = if (isBootstrapPrincipal) {
                ApprovalStatus.APPROVED.name
            } else {
                ApprovalStatus.PENDING.name
            }

            val userPayload = mapOf<String, Any?>(
                "userId" to uid,
                "fullName" to cleanName,
                "email" to cleanEmail,
                "phone" to cleanPhone,
                "role" to role.name,
                "approvalStatus" to initialStatus,
                "verificationId" to cleanVerId,
                "houseOrDepartment" to cleanDept,
                "classOrDesignation" to cleanDesig,
                "wardOrSecondaryInfo" to cleanSecondary,
                "bio" to cleanBio,
                "emailVerified" to emailVerified,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>

            val dirPayload = mapOf<String, Any?>(
                "userId" to uid,
                "fullName" to cleanName,
                "role" to role.name,
                "houseOrDepartment" to cleanDept,
                "classOrDesignation" to cleanDesig,
                "wardOrSecondaryInfo" to cleanSecondary,
                "bio" to cleanBio,
                "approvalStatus" to initialStatus,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>

            val batch = db.batch()
            batch.set(db.collection("users").document(uid), userPayload)
            batch.set(db.collection("directory").document(uid), dirPayload)
            batch.commit().await()

            Result.success(
                UserProfile(
                    id = uid,
                    userId = uid,
                    fullName = cleanName,
                    email = cleanEmail,
                    phone = cleanPhone,
                    role = role.name,
                    approvalStatus = initialStatus,
                    verificationId = cleanVerId,
                    houseOrDepartment = cleanDept,
                    classOrDesignation = cleanDesig,
                    wardOrSecondaryInfo = cleanSecondary,
                    bio = cleanBio,
                    emailVerified = emailVerified,
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now()
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun resubmitRejectedRegistration(
        fullName: String,
        phone: String,
        verificationId: String,
        houseOrDepartment: String,
        classOrDesignation: String,
        wardOrSecondaryInfo: String,
        bio: String,
        emailVerified: Boolean
    ): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            path = "users/$uid"
            val cleanName = fullName.trim().take(80)
            val cleanPhone = phone.trim().take(20)
            val cleanVerId = verificationId.trim().take(60)
            val cleanDept = houseOrDepartment.trim().take(80)
            val cleanDesig = classOrDesignation.trim().take(80)
            val cleanSecondary = wardOrSecondaryInfo.trim().take(120).takeIf { it.isNotBlank() }
            val cleanBio = bio.trim().take(300).takeIf { it.isNotBlank() }

            require(cleanName.length >= 2) { "Full name must be at least 2 characters." }
            require(cleanPhone.length >= 7) { "Phone number must be at least 7 digits." }
            require(cleanVerId.length >= 2) { "Identity verification ID is required." }

            val userUpdates = mutableMapOf<String, Any>(
                "fullName" to cleanName,
                "phone" to cleanPhone,
                "verificationId" to cleanVerId,
                "houseOrDepartment" to cleanDept,
                "classOrDesignation" to cleanDesig,
                "emailVerified" to emailVerified,
                "approvalStatus" to ApprovalStatus.PENDING.name,
                "rejectionReason" to "",
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (cleanSecondary != null) userUpdates["wardOrSecondaryInfo"] = cleanSecondary
            if (cleanBio != null) userUpdates["bio"] = cleanBio

            val dirUpdates = mutableMapOf<String, Any>(
                "fullName" to cleanName,
                "houseOrDepartment" to cleanDept,
                "classOrDesignation" to cleanDesig,
                "approvalStatus" to ApprovalStatus.PENDING.name,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (cleanSecondary != null) dirUpdates["wardOrSecondaryInfo"] = cleanSecondary
            if (cleanBio != null) dirUpdates["bio"] = cleanBio

            val batch = db.batch()
            batch.update(db.collection("users").document(uid), userUpdates)
            batch.update(db.collection("directory").document(uid), dirUpdates)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateOwnProfile(
        fullName: String,
        phone: String,
        houseOrDepartment: String,
        classOrDesignation: String,
        wardOrSecondaryInfo: String,
        bio: String
    ): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            path = "users/$uid"
            val cleanName = fullName.trim().take(80)
            val cleanPhone = phone.trim().take(20)
            val cleanDept = houseOrDepartment.trim().take(80)
            val cleanDesig = classOrDesignation.trim().take(80)
            val cleanSecondary = wardOrSecondaryInfo.trim().take(120).takeIf { it.isNotBlank() }
            val cleanBio = bio.trim().take(300).takeIf { it.isNotBlank() }

            require(cleanName.length >= 2) { "Full name must be at least 2 characters." }
            require(cleanPhone.length >= 7) { "Phone number must be at least 7 digits." }

            val userUpdates = mutableMapOf<String, Any>(
                "fullName" to cleanName,
                "phone" to cleanPhone,
                "houseOrDepartment" to cleanDept,
                "classOrDesignation" to cleanDesig,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (cleanSecondary != null) userUpdates["wardOrSecondaryInfo"] = cleanSecondary
            if (cleanBio != null) userUpdates["bio"] = cleanBio

            val dirUpdates = mutableMapOf<String, Any>(
                "fullName" to cleanName,
                "houseOrDepartment" to cleanDept,
                "classOrDesignation" to cleanDesig,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (cleanSecondary != null) dirUpdates["wardOrSecondaryInfo"] = cleanSecondary
            if (cleanBio != null) dirUpdates["bio"] = cleanBio

            val batch = db.batch()
            batch.update(db.collection("users").document(uid), userUpdates)
            batch.update(db.collection("directory").document(uid), dirUpdates)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeAllApplicationsForLeadership(uid: String): Flow<List<UserProfile>> = flow {
        val path = "users"
        emitAll(
            db.collection("users")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logAndRethrowFirestoreErrors(uid, OperationType.LIST, path)
        )
    }

    suspend fun reviewApplication(
        targetUid: String,
        decision: ApprovalStatus,
        rejectionReason: String = ""
    ): Result<Unit> {
        val path = "users/$targetUid"
        return try {
            val reviewerUid = requireUserId()
            require(targetUid.isNotBlank()) { "Applicant ID cannot be blank." }
            val cleanReason = if (decision == ApprovalStatus.REJECTED) {
                rejectionReason.trim().take(400).ifBlank { "Verification details require revision." }
            } else ""

            val batch = db.batch()
            batch.update(
                db.collection("users").document(targetUid),
                mapOf(
                    "approvalStatus" to decision.name,
                    "rejectionReason" to cleanReason,
                    "reviewedBy" to reviewerUid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                db.collection("directory").document(targetUid),
                mapOf(
                    "approvalStatus" to decision.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeDirectory(uid: String): Flow<List<DirectoryEntry>> = flow {
        val path = "directory"
        emitAll(
            db.collection("directory")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        DirectoryEntry::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).filter { it.parsedStatus() == ApprovalStatus.APPROVED }
                        .sortedBy { it.fullName.lowercase() }
                }
                .logAndRethrowFirestoreErrors(uid, OperationType.LIST, path)
        )
    }

    fun observeNotices(uid: String): Flow<List<Notice>> = flow {
        val path = "notices"
        emitAll(
            db.collection("notices")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        Notice::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<Notice> { it.pinned }
                            .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
                    )
                }
                .logAndRethrowFirestoreErrors(uid, OperationType.LIST, path)
        )
    }

    suspend fun createNotice(
        authorName: String,
        authorRole: SavRole,
        category: NoticeCategory,
        title: String,
        body: String,
        targetRole: String,
        pinned: Boolean
    ): Result<Unit> {
        var path = "notices"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().take(120)
            val cleanBody = body.trim().take(2000)
            require(cleanTitle.length >= 3) { "Title must be at least 3 characters." }
            require(cleanBody.length >= 5) { "Content must be at least 5 characters." }

            val docRef = db.collection("notices").document()
            path = docRef.path
            val payload = mapOf(
                "authorId" to uid,
                "authorName" to authorName.trim().take(80),
                "authorRole" to authorRole.name,
                "category" to category.name,
                "title" to cleanTitle,
                "body" to cleanBody,
                "targetRole" to targetRole,
                "pinned" to pinned,
                "likesCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleNoticeLike(noticeId: String, increment: Boolean): Result<Unit> {
        val path = "notices/$noticeId"
        return try {
            requireUserId()
            db.collection("notices").document(noticeId).update(
                mapOf(
                    "likesCount" to FieldValue.increment(if (increment) 1L else -1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteNotice(noticeId: String): Result<Unit> {
        val path = "notices/$noticeId"
        return try {
            requireUserId()
            db.collection("notices").document(noticeId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observeAcademicItems(uid: String): Flow<List<AcademicItem>> = flow {
        val path = "academic_items"
        emitAll(
            db.collection("academic_items")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        AcademicItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logAndRethrowFirestoreErrors(uid, OperationType.LIST, path)
        )
    }

    suspend fun createAcademicItem(
        teacherName: String,
        subject: String,
        targetClass: String,
        title: String,
        description: String,
        dueDateLabel: String,
        itemType: AcademicItemType
    ): Result<Unit> {
        var path = "academic_items"
        return try {
            val uid = requireUserId()
            val cleanSubject = subject.trim().take(60)
            val cleanClass = targetClass.trim().take(40)
            val cleanTitle = title.trim().take(120)
            val cleanDesc = description.trim().take(1500)
            val cleanDue = dueDateLabel.trim().take(40)
            require(cleanSubject.length >= 2) { "Subject must be at least 2 characters." }
            require(cleanClass.isNotEmpty()) { "Target class is required." }
            require(cleanTitle.length >= 3) { "Title must be at least 3 characters." }
            require(cleanDesc.length >= 3) { "Description must be at least 3 characters." }
            require(cleanDue.length >= 2) { "Due date / schedule label is required." }

            val docRef = db.collection("academic_items").document()
            path = docRef.path
            val payload = mapOf(
                "teacherId" to uid,
                "teacherName" to teacherName.trim().take(80),
                "subject" to cleanSubject,
                "targetClass" to cleanClass,
                "title" to cleanTitle,
                "description" to cleanDesc,
                "dueDateLabel" to cleanDue,
                "itemType" to itemType.name,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteAcademicItem(itemId: String): Result<Unit> {
        val path = "academic_items/$itemId"
        return try {
            requireUserId()
            db.collection("academic_items").document(itemId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observeSchoolEvents(uid: String): Flow<List<SchoolEvent>> = flow {
        val path = "school_events"
        emitAll(
            db.collection("school_events")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        SchoolEvent::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logAndRethrowFirestoreErrors(uid, OperationType.LIST, path)
        )
    }

    suspend fun createSchoolEvent(
        organizerName: String,
        organizerRole: SavRole,
        title: String,
        description: String,
        eventDateLabel: String,
        location: String,
        audience: EventAudience
    ): Result<Unit> {
        var path = "school_events"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().take(120)
            val cleanDesc = description.trim().take(1200)
            val cleanDate = eventDateLabel.trim().take(60)
            val cleanLoc = location.trim().take(100)
            require(cleanTitle.length >= 3) { "Event title must be at least 3 characters." }
            require(cleanDesc.length >= 5) { "Event description must be at least 5 characters." }
            require(cleanDate.length >= 2) { "Event date is required." }
            require(cleanLoc.length >= 2) { "Location is required." }

            val docRef = db.collection("school_events").document()
            path = docRef.path
            val payload = mapOf(
                "organizerId" to uid,
                "organizerName" to organizerName.trim().take(80),
                "organizerRole" to organizerRole.name,
                "title" to cleanTitle,
                "description" to cleanDesc,
                "eventDateLabel" to cleanDate,
                "location" to cleanLoc,
                "audience" to audience.name,
                "rsvpCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleEventRsvp(eventId: String, going: Boolean): Result<Unit> {
        val path = "school_events/$eventId"
        return try {
            requireUserId()
            db.collection("school_events").document(eventId).update(
                mapOf(
                    "rsvpCount" to FieldValue.increment(if (going) 1L else -1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeChannelMessages(uid: String, channelId: String): Flow<List<SchoolMessage>> = flow {
        val path = "messages"
        emitAll(
            db.collection("messages")
                .whereEqualTo("channelType", "CHANNEL")
                .whereEqualTo("channelId", channelId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        SchoolMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .logAndRethrowFirestoreErrors(uid, OperationType.LIST, path)
        )
    }

    fun observeDirectMessages(uid: String): Flow<List<SchoolMessage>> = flow {
        val path = "messages"
        emitAll(
            db.collection("messages")
                .whereEqualTo("channelType", "DIRECT")
                .whereArrayContains("participantIds", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        SchoolMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .logAndRethrowFirestoreErrors(uid, OperationType.LIST, path)
        )
    }

    suspend fun sendChannelMessage(
        senderName: String,
        senderRole: SavRole,
        channelId: String,
        body: String
    ): Result<Unit> {
        var path = "messages"
        return try {
            val uid = requireUserId()
            val cleanBody = body.trim().take(1000)
            require(cleanBody.isNotEmpty()) { "Message cannot be empty." }

            val docRef = db.collection("messages").document()
            path = docRef.path
            val payload = mapOf(
                "senderId" to uid,
                "senderName" to senderName.trim().take(80),
                "senderRole" to senderRole.name,
                "channelType" to "CHANNEL",
                "channelId" to channelId,
                "participantIds" to emptyList<String>(),
                "body" to cleanBody,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun sendDirectMessage(
        senderName: String,
        senderRole: SavRole,
        recipientId: String,
        body: String
    ): Result<Unit> {
        var path = "messages"
        return try {
            val uid = requireUserId()
            require(recipientId.isNotBlank() && recipientId != uid) { "Select a valid recipient." }
            val cleanBody = body.trim().take(1000)
            require(cleanBody.isNotEmpty()) { "Message cannot be empty." }
            val participants = listOf(uid, recipientId).sorted()
            val threadId = "${participants[0].take(60)}_${participants[1].take(60)}"

            val docRef = db.collection("messages").document()
            path = docRef.path
            val payload = mapOf(
                "senderId" to uid,
                "senderName" to senderName.trim().take(80),
                "senderRole" to senderRole.name,
                "channelType" to "DIRECT",
                "channelId" to threadId,
                "participantIds" to participants,
                "body" to cleanBody,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    fun observeServiceRequests(uid: String, role: SavRole): Flow<List<ServiceRequest>> = flow {
        val path = "service_requests"
        val ownFlow = db.collection("service_requests")
            .whereEqualTo("requesterId", uid)
            .snapshots()
            .map {
                it.toObjects(
                    ServiceRequest::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

        val combinedFlow = when (role) {
            SavRole.PRINCIPAL, SavRole.VICE_PRINCIPAL -> {
                db.collection("service_requests")
                    .snapshots()
                    .map {
                        it.toObjects(
                            ServiceRequest::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    }
            }
            SavRole.TEACHER -> {
                val teacherQueueFlow = db.collection("service_requests")
                    .whereIn(
                        "category",
                        listOf(
                            ServiceRequestCategory.PARENT_INQUIRY.name,
                            ServiceRequestCategory.ACADEMIC_SUPPORT.name
                        )
                    )
                    .snapshots()
                    .map {
                        it.toObjects(
                            ServiceRequest::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    }
                combine(ownFlow, teacherQueueFlow) { own, queue ->
                    (own + queue).distinctBy { it.id }
                }
            }
            SavRole.OTHER_STAFF, SavRole.NON_TEACHING_STAFF -> {
                val staffQueueFlow = db.collection("service_requests")
                    .whereIn(
                        "category",
                        listOf(
                            ServiceRequestCategory.HOSTEL_WELFARE.name,
                            ServiceRequestCategory.FACILITY_MAINTENANCE.name
                        )
                    )
                    .snapshots()
                    .map {
                        it.toObjects(
                            ServiceRequest::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    }
                combine(ownFlow, staffQueueFlow) { own, queue ->
                    (own + queue).distinctBy { it.id }
                }
            }
            else -> ownFlow
        }

        emitAll(
            combinedFlow
                .map { list -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } }
                .logAndRethrowFirestoreErrors(uid, OperationType.LIST, path)
        )
    }

    suspend fun createServiceRequest(
        requesterName: String,
        requesterRole: SavRole,
        category: ServiceRequestCategory,
        title: String,
        details: String
    ): Result<Unit> {
        var path = "service_requests"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().take(120)
            val cleanDetails = details.trim().take(1000)
            require(cleanTitle.length >= 3) { "Request title must be at least 3 characters." }
            require(cleanDetails.length >= 5) { "Request details must be at least 5 characters." }

            val docRef = db.collection("service_requests").document()
            path = docRef.path
            val payload = mapOf(
                "requesterId" to uid,
                "requesterName" to requesterName.trim().take(80),
                "requesterRole" to requesterRole.name,
                "category" to category.name,
                "title" to cleanTitle,
                "details" to cleanDetails,
                "status" to ServiceRequestStatus.OPEN.name,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateServiceRequestStatus(
        requestId: String,
        status: ServiceRequestStatus,
        resolutionNote: String
    ): Result<Unit> {
        val path = "service_requests/$requestId"
        return try {
            val uid = requireUserId()
            val cleanNote = resolutionNote.trim().take(400)
            val updates = mutableMapOf<String, Any>(
                "status" to status.name,
                "resolvedBy" to uid,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (cleanNote.isNotEmpty()) {
                updates["resolutionNote"] = cleanNote
            }
            db.collection("service_requests").document(requestId).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }
}
