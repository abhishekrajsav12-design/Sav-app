package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.auth.onGoogleSignInClicked
import com.example.data.model.ApprovalStatus
import com.example.data.model.UserProfile
import com.example.ui.components.ApprovalStatusPill
import com.example.ui.components.SavErrorBanner
import com.example.ui.components.SavRoleBadge
import com.example.ui.theme.SavCrimson
import com.example.ui.theme.SavCrimsonContainer
import com.example.ui.theme.SavEmerald
import com.example.ui.theme.SavEmeraldContainer
import com.example.ui.theme.SavGoldBright
import com.example.ui.theme.SavGoldPrimary
import com.example.ui.theme.SavNavyDeep
import com.example.ui.theme.SavNavyPrimary
import com.example.ui.theme.SavPureWhite

@Composable
fun WelcomeScreen(
    onCreateNewAccount: () -> Unit,
    onLoginExisting: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SavNavyDeep)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(310.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_sav_campus_hero),
                    contentDescription = "Simultala Awasiya Vidyalaya Campus",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    SavNavyDeep.copy(alpha = 0.25f),
                                    SavNavyDeep.copy(alpha = 0.72f),
                                    SavNavyDeep
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_sav_emblem),
                        contentDescription = "SAV FAMILY Crest",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                    )
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        color = SavGoldPrimary.copy(alpha = 0.2f),
                        contentColor = SavGoldBright,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = "SIMULTALA AWASIYA VIDYALAYA",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Welcome to SAV FAMILY",
                    style = MaterialTheme.typography.displaySmall,
                    color = SavPureWhite,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "One Family, One Community",
                    style = MaterialTheme.typography.titleMedium,
                    color = SavGoldBright,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "The private school social network and campus governance portal connecting Principal, Faculty, Students, Parents, Alumni, and Staff.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFCBD5E1),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(28.dp))

                Button(
                    onClick = onCreateNewAccount,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("welcome_create_account_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SavGoldPrimary,
                        contentColor = SavNavyDeep
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Outlined.PersonAdd, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Create New Account",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onLoginExisting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("welcome_login_btn"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SavPureWhite),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Login, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Already Have an Account / Login",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(24.dp))
                Surface(
                    color = Color.White.copy(alpha = 0.06f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.VerifiedUser,
                            contentDescription = null,
                            tint = SavGoldBright,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Every account is identity-verified and approved by Simultala Awasiya Vidyalaya administration.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun LoginScreen(
    isCreatingAccount: Boolean,
    rememberMe: Boolean,
    onRememberMeChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onAuthSuccess: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showForgotDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("auth_back_btn")) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = if (isCreatingAccount) "Create SAV FAMILY Account" else "Member Login",
                style = MaterialTheme.typography.titleLarge
            )
        }

        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_sav_emblem),
                        contentDescription = "SAV Emblem",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isCreatingAccount) "Verify Your Identity Email" else "Welcome Back to SAV",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = "Simultala Awasiya Vidyalaya",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))
                Text(
                    text = if (isCreatingAccount) {
                        "Sign in with your Google email to verify your email address and begin role-based SAV registration. If this email is already registered, you will be redirected to your existing profile."
                    } else {
                        "Sign in with your registered Google account to check verification, school approval status, and open your role dashboard."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Remember Me",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Keep me signed in automatically on this device",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = rememberMe,
                        onCheckedChange = onRememberMeChange,
                        modifier = Modifier.testTag("remember_me_switch")
                    )
                }

                if (errorMessage != null) {
                    Spacer(Modifier.height(14.dp))
                    SavErrorBanner(message = errorMessage!!)
                }

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onAuthError = { msg ->
                                isLoading = false
                                errorMessage = msg
                            },
                            scope = scope,
                            onAuthCancelled = {
                                isLoading = false
                            }
                        )
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("google_login_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SavNavyPrimary,
                        contentColor = SavPureWhite
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = SavPureWhite,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (isCreatingAccount) "Continue with Google to Register" else "Sign in with Google",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { showForgotDialog = true },
                        modifier = Modifier.testTag("forgot_password_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Forgot Password / Account Help")
                    }
                }
            }
        }
    }

    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = { showForgotDialog = false },
            title = { Text("Account & Password Recovery") },
            text = {
                Text(
                    "SAV FAMILY authenticates members exclusively through verified Google Sign-In for institutional security. If you forgot your Google password or cannot access your email, use Google Account Recovery (accounts.google.com/signin/recovery) and then return here to sign in and restore your SAV profile."
                )
            },
            confirmButton = {
                TextButton(onClick = { showForgotDialog = false }) {
                    Text("Got it")
                }
            }
        )
    }
}

@Composable
fun DuplicateAccountScreen(
    existingProfile: UserProfile,
    onContinueToExistingAccount: () -> Unit,
    onSignOut: () -> Unit
) {
    var showHelp by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 540.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = SavGoldPrimary,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Account Already Exists",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "An SAV FAMILY registration already exists for ${existingProfile.email}. Duplicate accounts are not allowed. Your saved profile and role details are intact.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = existingProfile.fullName,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SavRoleBadge(existingProfile.parsedRole())
                            ApprovalStatusPill(existingProfile.parsedStatus())
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onContinueToExistingAccount,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("duplicate_continue_login_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SavNavyPrimary,
                        contentColor = SavPureWhite
                    )
                ) {
                    Text("Login & Open Saved Account")
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = { showHelp = true }) {
                        Text("Forgot Password / Help")
                    }
                    TextButton(onClick = onSignOut) {
                        Text("Use Different Google Account")
                    }
                }
            }
        }
    }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text("Account Access Help") },
            text = {
                Text("Since you are already signed in with ${existingProfile.email}, tap 'Login & Open Saved Account' to go directly to your ${existingProfile.parsedStatus().label} status or dashboard without re-registering.")
            },
            confirmButton = {
                TextButton(onClick = { showHelp = false }) { Text("Close") }
            }
        )
    }
}

@Composable
fun PendingApprovalScreen(
    profile: UserProfile,
    emailVerified: Boolean,
    onRefreshStatus: () -> Unit,
    onSignOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(SavGoldPrimary.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.HourglassTop,
                            contentDescription = null,
                            tint = SavGoldPrimary
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Registration Pending",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = "Awaiting School Administration Review",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                Surface(
                    color = if (emailVerified) SavEmeraldContainer else SavCrimsonContainer,
                    contentColor = if (emailVerified) SavEmerald else SavCrimson,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (emailVerified) Icons.Outlined.CheckCircle else Icons.Outlined.ReportProblem,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (emailVerified) {
                                "Email Verified (${profile.email}) — School Approval Pending"
                            } else {
                                "Email Verification Incomplete — Please verify your Google email"
                            },
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text(
                    text = "A verified email confirms your address, while SAV School Approval verifies your institutional identity (${profile.verificationId}). The Principal or Vice Principal will review your application shortly.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(16.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Applicant: ${profile.fullName}", style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SavRoleBadge(profile.parsedRole())
                            ApprovalStatusPill(ApprovalStatus.PENDING)
                        }
                        Text(
                            "${profile.parsedRole().departmentLabel}: ${profile.houseOrDepartment}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "${profile.parsedRole().designationLabel}: ${profile.classOrDesignation}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onRefreshStatus,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("pending_refresh_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = SavNavyPrimary)
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Refresh Status")
                    }
                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("pending_signout_btn")
                    ) {
                        Text("Sign Out")
                    }
                }
            }
        }
    }
}

@Composable
fun RejectedApplicationScreen(
    profile: UserProfile,
    isSubmitting: Boolean,
    errorMessage: String?,
    onResubmit: (
        fullName: String,
        phone: String,
        verificationId: String,
        houseOrDepartment: String,
        classOrDesignation: String,
        wardOrSecondaryInfo: String,
        bio: String
    ) -> Unit,
    onSignOut: () -> Unit
) {
    val role = profile.parsedRole()
    var editing by remember { mutableStateOf(false) }
    var fullName by remember(profile.id) { mutableStateOf(profile.fullName) }
    var phone by remember(profile.id) { mutableStateOf(profile.phone) }
    var verificationId by remember(profile.id) { mutableStateOf(profile.verificationId) }
    var department by remember(profile.id) { mutableStateOf(profile.houseOrDepartment) }
    var designation by remember(profile.id) { mutableStateOf(profile.classOrDesignation) }
    var secondary by remember(profile.id) { mutableStateOf(profile.wardOrSecondaryInfo.orEmpty()) }
    var bio by remember(profile.id) { mutableStateOf(profile.bio.orEmpty()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                ApprovalStatusPill(ApprovalStatus.REJECTED)
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Application Requires Revision",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(Modifier.height(10.dp))
                Surface(
                    color = SavCrimsonContainer,
                    contentColor = SavCrimson,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Administration Review Note:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = profile.rejectionReason?.takeIf { it.isNotBlank() }
                                ?: "Please verify your institutional ID and role details before resubmitting.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                if (!editing) {
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = { editing = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("rejected_edit_resubmit_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = SavNavyPrimary)
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Edit & Resubmit Application")
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Sign Out")
                    }
                } else {
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = verificationId,
                        onValueChange = { verificationId = it },
                        label = { Text(role.verificationHint) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = department,
                        onValueChange = { department = it },
                        label = { Text(role.departmentLabel) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = designation,
                        onValueChange = { designation = it },
                        label = { Text(role.designationLabel) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (role.secondaryLabel != null) {
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = secondary,
                            onValueChange = { secondary = it },
                            label = { Text(role.secondaryLabel) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        label = { Text("Short Bio (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    if (errorMessage != null) {
                        Spacer(Modifier.height(12.dp))
                        SavErrorBanner(errorMessage)
                    }

                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { editing = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                onResubmit(
                                    fullName, phone, verificationId,
                                    department, designation, secondary, bio
                                )
                            },
                            enabled = !isSubmitting,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("resubmit_confirm_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = SavNavyPrimary)
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(Modifier.size(18.dp), color = SavPureWhite, strokeWidth = 2.dp)
                            } else {
                                Text("Resubmit")
                            }
                        }
                    }
                }
            }
        }
    }
}
