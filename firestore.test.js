const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

const PRINCIPAL_UID = "principal_uid_1";
const TEACHER_UID = "teacher_uid_2";
const STUDENT_UID = "student_uid_3";
const PARENT_UID = "parent_uid_4";
const ALUMNI_UID = "alumni_uid_5";
const OTHER_STAFF_UID = "other_staff_uid_6";
const NON_TEACHING_UID = "non_teaching_uid_7";
const PENDING_UID = "pending_uid_8";
const REJECTED_UID = "rejected_uid_9";

const pastDate = () => new Date(Date.now() - 2000);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

async function seedApprovedUser(uid, role, fullName = "SAV Member") {
  const ts = pastDate();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(uid).set({
      userId: uid,
      fullName,
      email: `${uid}@sav.edu.in`,
      phone: "9876543210",
      role,
      approvalStatus: "APPROVED",
      verificationId: `ID-${uid}`,
      houseOrDepartment: "Nalanda House",
      classOrDesignation: role,
      emailVerified: true,
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("directory").doc(uid).set({
      userId: uid,
      fullName,
      role,
      houseOrDepartment: "Nalanda House",
      classOrDesignation: role,
      approvalStatus: "APPROVED",
      createdAt: ts,
      updatedAt: ts,
    });
  });
}

test("Unauthenticated users cannot read or write any SAV FAMILY collection", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("users").doc(STUDENT_UID).get());
  await assertFails(unauth.collection("directory").get());
  await assertFails(unauth.collection("notices").get());
  await assertFails(unauth.collection("academic_items").get());
  await assertFails(unauth.collection("school_events").get());
  await assertFails(unauth.collection("messages").where("channelType", "==", "CHANNEL").where("channelId", "==", "ALL_COMMUNITY").get());
  await assertFails(unauth.collection("service_requests").where("requesterId", "==", STUDENT_UID).get());
});

test("Registration: non-bootstrap user cannot self-approve; bootstrap admin can register as approved Principal", async () => {
  const studentDb = testEnv.authenticatedContext(STUDENT_UID, {
    email: "student@example.com",
    email_verified: true,
  }).firestore();
  const ts = pastDate();

  // Attempt self-approval -> MUST FAIL
  await assertFails(
    studentDb.collection("users").doc(STUDENT_UID).set({
      userId: STUDENT_UID,
      fullName: "Aarav Kumar",
      email: "student@example.com",
      phone: "9876543210",
      role: "STUDENT",
      approvalStatus: "APPROVED",
      verificationId: "ADM-2025-101",
      houseOrDepartment: "Vikramshila",
      classOrDesignation: "Class X-A",
      emailVerified: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Valid PENDING registration -> MUST SUCCEED
  await assertSucceeds(
    studentDb.collection("users").doc(STUDENT_UID).set({
      userId: STUDENT_UID,
      fullName: "Aarav Kumar",
      email: "student@example.com",
      phone: "9876543210",
      role: "STUDENT",
      approvalStatus: "PENDING",
      verificationId: "ADM-2025-101",
      houseOrDepartment: "Vikramshila",
      classOrDesignation: "Class X-A",
      emailVerified: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Bootstrap admin with verified email can self-approve as PRINCIPAL
  const adminDb = testEnv.authenticatedContext(PRINCIPAL_UID, {
    email: "chandragupt0040@gmail.com",
    email_verified: true,
  }).firestore();

  await assertSucceeds(
    adminDb.collection("users").doc(PRINCIPAL_UID).set({
      userId: PRINCIPAL_UID,
      fullName: "Dr. Principal SAV",
      email: "chandragupt0040@gmail.com",
      phone: "9800000001",
      role: "PRINCIPAL",
      approvalStatus: "APPROVED",
      verificationId: "EMP-PRIN-001",
      houseOrDepartment: "Administration",
      classOrDesignation: "Principal",
      emailVerified: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Spoofed unverified email claiming bootstrap email -> MUST FAIL
  const spoofDb = testEnv.authenticatedContext("spoof_uid", {
    email: "chandragupt0040@gmail.com",
    email_verified: false,
  }).firestore();

  await assertFails(
    spoofDb.collection("users").doc("spoof_uid").set({
      userId: "spoof_uid",
      fullName: "Spoof",
      email: "chandragupt0040@gmail.com",
      phone: "9800000002",
      role: "PRINCIPAL",
      approvalStatus: "APPROVED",
      verificationId: "EMP-999",
      houseOrDepartment: "Admin",
      classOrDesignation: "Principal",
      emailVerified: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("PII Isolation & Approval Workflow: students cannot read other users' PII; Principal can approve/reject; rejected user can resubmit", async () => {
  await seedApprovedUser(PRINCIPAL_UID, "PRINCIPAL", "Dr. Principal");
  await seedApprovedUser(STUDENT_UID, "STUDENT", "Aarav Kumar");
  await seedApprovedUser(TEACHER_UID, "TEACHER", "Prof. Verma");

  const ts = pastDate();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(PENDING_UID).set({
      userId: PENDING_UID,
      fullName: "Pending Applicant",
      email: "pending@sav.edu.in",
      phone: "9811112222",
      role: "PARENT",
      approvalStatus: "PENDING",
      verificationId: "WARD-2024-09",
      houseOrDepartment: "Parent Body",
      classOrDesignation: "Father",
      emailVerified: true,
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const studentDb = testEnv.authenticatedContext(STUDENT_UID).firestore();
  const teacherDb = testEnv.authenticatedContext(TEACHER_UID).firestore();
  const principalDb = testEnv.authenticatedContext(PRINCIPAL_UID).firestore();
  const pendingDb = testEnv.authenticatedContext(PENDING_UID).firestore();

  // Student and Teacher cannot read another user's private /users/{id} doc or list /users
  await assertFails(studentDb.collection("users").doc(TEACHER_UID).get());
  await assertFails(teacherDb.collection("users").doc(STUDENT_UID).get());
  await assertFails(studentDb.collection("users").get());
  await assertFails(teacherDb.collection("users").get());

  // Approved members CAN list PII-free /directory
  await assertSucceeds(studentDb.collection("directory").get());

  // Pending user CANNOT read /directory or /notices
  await assertFails(pendingDb.collection("directory").get());
  await assertFails(pendingDb.collection("notices").get());

  // Teacher CANNOT approve a pending user
  await assertFails(
    teacherDb.collection("users").doc(PENDING_UID).update({
      approvalStatus: "APPROVED",
      reviewedBy: TEACHER_UID,
      updatedAt: pastDate(),
    })
  );

  // Principal rejects pending user with reason
  await assertSucceeds(
    principalDb.collection("users").doc(PENDING_UID).update({
      approvalStatus: "REJECTED",
      rejectionReason: "Ward admission number mismatch",
      reviewedBy: PRINCIPAL_UID,
      updatedAt: pastDate(),
    })
  );

  // Rejected user can edit & resubmit (sets approvalStatus back to PENDING, cannot change role or set APPROVED)
  await assertFails(
    pendingDb.collection("users").doc(PENDING_UID).update({
      approvalStatus: "APPROVED",
      updatedAt: pastDate(),
    })
  );
  await assertSucceeds(
    pendingDb.collection("users").doc(PENDING_UID).update({
      verificationId: "WARD-2024-99",
      approvalStatus: "PENDING",
      rejectionReason: "",
      updatedAt: pastDate(),
    })
  );

  // Principal approves
  await assertSucceeds(
    principalDb.collection("users").doc(PENDING_UID).update({
      approvalStatus: "APPROVED",
      reviewedBy: PRINCIPAL_UID,
      updatedAt: pastDate(),
    })
  );
});

test("Role-enforced Notices & Academic Items: students cannot post official circulars or create assignments; alumni cannot read assignments", async () => {
  await seedApprovedUser(PRINCIPAL_UID, "PRINCIPAL", "Dr. Principal");
  await seedApprovedUser(TEACHER_UID, "TEACHER", "Prof. Verma");
  await seedApprovedUser(STUDENT_UID, "STUDENT", "Aarav Kumar");
  await seedApprovedUser(ALUMNI_UID, "ALUMNI", "Riya Batch 2016");

  const studentDb = testEnv.authenticatedContext(STUDENT_UID).firestore();
  const teacherDb = testEnv.authenticatedContext(TEACHER_UID).firestore();
  const principalDb = testEnv.authenticatedContext(PRINCIPAL_UID).firestore();
  const alumniDb = testEnv.authenticatedContext(ALUMNI_UID).firestore();
  const ts = pastDate();

  // Student cannot create OFFICIAL_CIRCULAR or spoof authorRole
  await assertFails(
    studentDb.collection("notices").doc("n1").set({
      authorId: STUDENT_UID,
      authorName: "Aarav Kumar",
      authorRole: "STUDENT",
      category: "OFFICIAL_CIRCULAR",
      title: "Fake Holiday",
      body: "Tomorrow is a holiday!",
      targetRole: "ALL",
      pinned: true,
      likesCount: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Student CAN create COMMUNITY_POST (pinned=false)
  await assertSucceeds(
    studentDb.collection("notices").doc("n2").set({
      authorId: STUDENT_UID,
      authorName: "Aarav Kumar",
      authorRole: "STUDENT",
      category: "COMMUNITY_POST",
      title: "Science Club Meetup",
      body: "Robotics showcase this Saturday in Hall B.",
      targetRole: "ALL",
      pinned: false,
      likesCount: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Principal CAN create pinned OFFICIAL_CIRCULAR
  await assertSucceeds(
    principalDb.collection("notices").doc("n3").set({
      authorId: PRINCIPAL_UID,
      authorName: "Dr. Principal",
      authorRole: "PRINCIPAL",
      category: "OFFICIAL_CIRCULAR",
      title: "Annual Convocation Notice",
      body: "All houses assemble at 8:00 AM.",
      targetRole: "ALL",
      pinned: true,
      likesCount: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Teacher CAN create academic item; Student CANNOT
  await assertFails(
    studentDb.collection("academic_items").doc("a1").set({
      teacherId: STUDENT_UID,
      teacherName: "Aarav Kumar",
      subject: "Physics",
      targetClass: "Class X",
      title: "Fake Exam",
      description: "Cancelled",
      dueDateLabel: "Mon",
      itemType: "EXAM_SCHEDULE",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    teacherDb.collection("academic_items").doc("a2").set({
      teacherId: TEACHER_UID,
      teacherName: "Prof. Verma",
      subject: "Physics",
      targetClass: "Class X",
      title: "Optics Problem Set",
      description: "Complete exercises 1 through 15.",
      dueDateLabel: "Fri, Nov 27",
      itemType: "ASSIGNMENT",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Student CAN read academic_items; Alumni CANNOT
  await assertSucceeds(studentDb.collection("academic_items").get());
  await assertFails(alumniDb.collection("academic_items").get());
});

test("Role-gated Messaging & Service Requests enforcement", async () => {
  await seedApprovedUser(STUDENT_UID, "STUDENT", "Aarav");
  await seedApprovedUser(TEACHER_UID, "TEACHER", "Prof. Verma");
  await seedApprovedUser(OTHER_STAFF_UID, "OTHER_STAFF", "Hostel Warden");

  const studentDb = testEnv.authenticatedContext(STUDENT_UID).firestore();
  const teacherDb = testEnv.authenticatedContext(TEACHER_UID).firestore();
  const staffDb = testEnv.authenticatedContext(OTHER_STAFF_UID).firestore();
  const ts = pastDate();

  // Student CANNOT read or post in FACULTY_STAFF channel
  await assertFails(
    studentDb.collection("messages")
      .where("channelType", "==", "CHANNEL")
      .where("channelId", "==", "FACULTY_STAFF")
      .get()
  );
  await assertFails(
    studentDb.collection("messages").doc("m1").set({
      senderId: STUDENT_UID,
      senderName: "Aarav",
      senderRole: "STUDENT",
      channelType: "CHANNEL",
      channelId: "FACULTY_STAFF",
      participantIds: [],
      body: "Sneaking in",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Teacher CAN post & read FACULTY_STAFF channel
  await assertSucceeds(
    teacherDb.collection("messages").doc("m2").set({
      senderId: TEACHER_UID,
      senderName: "Prof. Verma",
      senderRole: "TEACHER",
      channelType: "CHANNEL",
      channelId: "FACULTY_STAFF",
      participantIds: [],
      body: "Department meeting at 4 PM.",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    teacherDb.collection("messages")
      .where("channelType", "==", "CHANNEL")
      .where("channelId", "==", "FACULTY_STAFF")
      .get()
  );

  // Student submits HOSTEL_WELFARE request
  await assertSucceeds(
    studentDb.collection("service_requests").doc("sr1").set({
      requesterId: STUDENT_UID,
      requesterName: "Aarav",
      requesterRole: "STUDENT",
      category: "HOSTEL_WELFARE",
      title: "Study Lamp Repair",
      details: "Room 12 desk lamp flickering.",
      status: "OPEN",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Other Staff (Warden) can resolve HOSTEL_WELFARE request; student cannot resolve their own
  await assertFails(
    studentDb.collection("service_requests").doc("sr1").update({
      status: "RESOLVED",
      updatedAt: pastDate(),
    })
  );
  await assertSucceeds(
    staffDb.collection("service_requests").doc("sr1").update({
      status: "RESOLVED",
      resolutionNote: "Replaced bulb and switch.",
      resolvedBy: OTHER_STAFF_UID,
      updatedAt: pastDate(),
    })
  );
});
