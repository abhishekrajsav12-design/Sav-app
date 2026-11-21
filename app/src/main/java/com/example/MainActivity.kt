package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.auth.attemptAutoSignIn
import com.example.data.auth.signOutUser
import com.example.data.preferences.SessionPreferences
import com.example.data.repository.SavRepository
import com.example.ui.components.SavErrorBanner
import com.example.ui.screens.DuplicateAccountScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MainAppShell
import com.example.ui.screens.PendingApprovalScreen
import com.example.ui.screens.RegistrationFlowScreen
import com.example.ui.screens.RejectedApplicationScreen
import com.example.ui.screens.WelcomeScreen
import com.example.ui.state.AuthEntryIntent
import com.example.ui.state.GateRouteState
import com.example.ui.state.SavGateViewModel
import com.example.ui.state.SavMainViewModel
import com.example.ui.theme.SavFamilyTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SavFamilyRoot()
        }
    }
}

@Composable
fun SavFamilyRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember(context) { SessionPreferences(context.applicationContext) }
    val rememberMe by prefs.rememberMe.collectAsStateWithLifecycle(initialValue = true)
    val themeMode by prefs.themeMode.collectAsStateWithLifecycle(initialValue = "SYSTEM")

    val isDark = when (themeMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemInDarkTheme()
    }

    SavFamilyTheme(darkTheme = isDark) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            SavAuthGate(
                rememberMe = rememberMe,
                themeMode = themeMode,
                onRememberMeChange = { enabled ->
                    scope.launch { prefs.setRememberMe(enabled) }
                },
                onThemeModeChange = { mode ->
                    scope.launch { prefs.setThemeMode(mode) }
                }
            )
        }
    }
}

@Composable
private fun SavAuthGate(
    rememberMe: Boolean,
    themeMode: String,
    onRememberMeChange: (Boolean) -> Unit,
    onThemeModeChange: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var entryIntent by remember { mutableStateOf(AuthEntryIntent.WELCOME) }
    var autoSignInChecked by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    LaunchedEffect(rememberMe) {
        if (!autoSignInChecked) {
            autoSignInChecked = true
            if (rememberMe && Firebase.auth.currentUser == null) {
                attemptAutoSignIn(
                    context = context,
                    credentialManager = credentialManager,
                    onAuthSuccess = { currentUser = Firebase.auth.currentUser },
                    onUnauthenticated = {},
                    scope = scope
                )
            }
        }
    }

    val user = currentUser
    if (user == null) {
        when (entryIntent) {
            AuthEntryIntent.WELCOME -> WelcomeScreen(
                onCreateNewAccount = { entryIntent = AuthEntryIntent.CREATE_ACCOUNT },
                onLoginExisting = { entryIntent = AuthEntryIntent.LOGIN }
            )

            AuthEntryIntent.CREATE_ACCOUNT -> LoginScreen(
                isCreatingAccount = true,
                rememberMe = rememberMe,
                onRememberMeChange = onRememberMeChange,
                onBack = { entryIntent = AuthEntryIntent.WELCOME },
                onAuthSuccess = { currentUser = Firebase.auth.currentUser }
            )

            AuthEntryIntent.LOGIN -> LoginScreen(
                isCreatingAccount = false,
                rememberMe = rememberMe,
                onRememberMeChange = onRememberMeChange,
                onBack = { entryIntent = AuthEntryIntent.WELCOME },
                onAuthSuccess = { currentUser = Firebase.auth.currentUser }
            )
        }
    } else {
        val uid = user.uid
        key(uid) {
            val gateViewModel: SavGateViewModel = viewModel(
                key = "gate_$uid",
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY])
                        val dbId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(dbId)
                        SavGateViewModel(
                            repository = SavRepository(db),
                            uid = uid,
                            authEmail = user.email.orEmpty(),
                            authDisplayName = user.displayName.orEmpty(),
                            initialEmailVerified = user.isEmailVerified,
                            entryIntent = entryIntent
                        )
                    }
                }
            )

            val profileUi by gateViewModel.profileState.collectAsStateWithLifecycle()
            val isSubmitting by gateViewModel.isSubmitting.collectAsStateWithLifecycle()
            val actionError by gateViewModel.actionError.collectAsStateWithLifecycle()
            val emailVerified by gateViewModel.emailVerifiedState.collectAsStateWithLifecycle()
            var proceedPastDuplicate by remember(uid) { mutableStateOf(false) }

            val handleSignOut: () -> Unit = {
                signOutUser(credentialManager, scope) {
                    entryIntent = AuthEntryIntent.WELCOME
                    currentUser = null
                }
            }

            when (val route = gateViewModel.resolveGateState(profileUi, proceedPastDuplicate)) {
                is GateRouteState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(12.dp))
                            Text("Loading SAV FAMILY profile...")
                        }
                    }
                }

                is GateRouteState.Unauthenticated -> {
                    WelcomeScreen(
                        onCreateNewAccount = { entryIntent = AuthEntryIntent.CREATE_ACCOUNT },
                        onLoginExisting = { entryIntent = AuthEntryIntent.LOGIN }
                    )
                }

                is GateRouteState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SavErrorBanner(route.message)
                            OutlinedButton(onClick = handleSignOut) {
                                Text("Sign Out")
                            }
                        }
                    }
                }

                is GateRouteState.DuplicateAccountNotice -> {
                    DuplicateAccountScreen(
                        existingProfile = route.existingProfile,
                        onContinueToExistingAccount = {
                            gateViewModel.dismissDuplicateNotice()
                            proceedPastDuplicate = true
                        },
                        onSignOut = handleSignOut
                    )
                }

                is GateRouteState.NeedsRegistration -> {
                    RegistrationFlowScreen(
                        email = route.email,
                        initialDisplayName = route.displayName,
                        emailVerified = route.emailVerified,
                        isSubmitting = isSubmitting,
                        actionError = actionError,
                        onRefreshEmailVerification = gateViewModel::refreshEmailVerification,
                        onSubmitRegistration = { fullName, phone, role, verId, dept, desig, sec, bio ->
                            gateViewModel.submitRegistration(
                                fullName = fullName,
                                phone = phone,
                                role = role,
                                verificationId = verId,
                                houseOrDepartment = dept,
                                classOrDesignation = desig,
                                wardOrSecondaryInfo = sec,
                                bio = bio,
                                onSuccess = { proceedPastDuplicate = true }
                            )
                        },
                        onSignOut = handleSignOut
                    )
                }

                is GateRouteState.PendingApproval -> {
                    PendingApprovalScreen(
                        profile = route.profile,
                        emailVerified = emailVerified,
                        onRefreshStatus = {
                            scope.launch {
                                try {
                                    Firebase.auth.currentUser?.reload()?.await()
                                    gateViewModel.refreshEmailVerification(
                                        Firebase.auth.currentUser?.isEmailVerified == true
                                    )
                                } catch (_: Exception) {
                                }
                            }
                        },
                        onSignOut = handleSignOut
                    )
                }

                is GateRouteState.RejectedApplication -> {
                    RejectedApplicationScreen(
                        profile = route.profile,
                        isSubmitting = isSubmitting,
                        errorMessage = actionError,
                        onResubmit = { fullName, phone, verId, dept, desig, sec, bio ->
                            gateViewModel.resubmitApplication(
                                fullName = fullName,
                                phone = phone,
                                verificationId = verId,
                                houseOrDepartment = dept,
                                classOrDesignation = desig,
                                wardOrSecondaryInfo = sec,
                                bio = bio
                            )
                        },
                        onSignOut = handleSignOut
                    )
                }

                is GateRouteState.ApprovedDashboard -> {
                    val mainViewModel: SavMainViewModel = viewModel(
                        key = "main_${uid}_${route.profile.role}",
                        factory = viewModelFactory {
                            initializer {
                                val app = checkNotNull(this[APPLICATION_KEY])
                                val dbId = app.getString(R.string.firestore_database_id)
                                val db = FirebaseFirestore.getInstance(dbId)
                                SavMainViewModel(
                                    repository = SavRepository(db),
                                    currentUserId = uid,
                                    initialProfile = route.profile
                                )
                            }
                        }
                    )

                    MainAppShell(
                        viewModel = mainViewModel,
                        initialProfile = route.profile,
                        rememberMe = rememberMe,
                        themeMode = themeMode,
                        onRememberMeChange = onRememberMeChange,
                        onThemeModeChange = onThemeModeChange,
                        onSignOut = handleSignOut
                    )
                }
            }
        }
    }
}
