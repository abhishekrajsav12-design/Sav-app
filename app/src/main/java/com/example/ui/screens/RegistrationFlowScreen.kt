package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.SavRole
import com.example.ui.components.SavErrorBanner
import com.example.ui.components.savRoleIcon
import com.example.ui.theme.SavAmber
import com.example.ui.theme.SavAmberContainer
import com.example.ui.theme.SavEmerald
import com.example.ui.theme.SavEmeraldContainer
import com.example.ui.theme.SavGoldPrimary
import com.example.ui.theme.SavGoldSoft
import com.example.ui.theme.SavNavyPrimary
import com.example.ui.theme.SavPureWhite
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun RegistrationFlowScreen(
    email: String,
    initialDisplayName: String,
    emailVerified: Boolean,
    isSubmitting: Boolean,
    actionError: String?,
    onRefreshEmailVerification: (Boolean) -> Unit,
    onSubmitRegistration: (
        fullName: String,
        phone: String,
        role: SavRole,
        verificationId: String,
        houseOrDepartment: String,
        classOrDesignation: String,
        wardOrSecondaryInfo: String,
        bio: String
    ) -> Unit,
    onSignOut: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) } // 0: Role, 1: Identity, 2: Email Verif, 3: Role Form
    var selectedRole by remember { mutableStateOf<SavRole?>(null) }
    var fullName by remember { mutableStateOf(initialDisplayName) }
    var phone by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var designation by remember { mutableStateOf("") }
    var secondaryInfo by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    var verificationNotice by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    BackHandler(enabled = step > 0) {
        localError = null
        step -= 1
    }

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
                .widthIn(max = 600.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        localError = null
                        if (step > 0) step -= 1 else onSignOut()
                    },
                    modifier = Modifier.testTag("reg_back_btn")
                ) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                }
                Column {
                    Text("SAV FAMILY Registration", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "Step ${step + 1} of 4",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            OutlinedButton(onClick = onSignOut) {
                Text("Switch Account")
            }
        }

        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { (step + 1) / 4f },
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = SavGoldPrimary
        )

        Spacer(Modifier.height(18.dp))

        Card(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                when (step) {
                    0 -> {
                        Text("Select Your SAV Role", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Choose your primary institutional role at Simultala Awasiya Vidyalaya. Permissions and dashboard tools are strictly tailored to your role.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        SavRole.entries.forEach { role ->
                            val isSelected = selectedRole == role
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp)
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable {
                                        selectedRole = role
                                        localError = null
                                    }
                                    .testTag("role_option_${role.name.lowercase()}"),
                                color = if (isSelected) SavGoldSoft else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (isSelected) BorderStroke(2.dp, SavGoldPrimary) else null,
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) SavNavyPrimary else MaterialTheme.colorScheme.surface
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = savRoleIcon(role),
                                            contentDescription = null,
                                            tint = if (isSelected) SavPureWhite else SavNavyPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = role.displayName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = role.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        val role = selectedRole ?: SavRole.STUDENT
                        Text("Identity Verification", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Provide your institutional credentials for ${role.displayName} verification by school administration.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Full Legal Name") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reg_full_name_input"),
                            singleLine = true
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Mobile Number") },
                            placeholder = { Text("e.g. 9876543210") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reg_phone_input"),
                            singleLine = true
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = verificationId,
                            onValueChange = { verificationId = it },
                            label = { Text(role.verificationHint) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reg_verification_id_input"),
                            singleLine = true
                        )
                    }

                    2 -> {
                        Text("Email Verification", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "SAV FAMILY separates Email Verification from School Approval. Confirm your verified email before completing your role application.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Surface(
                            color = if (emailVerified) SavEmeraldContainer else SavAmberContainer,
                            contentColor = if (emailVerified) SavEmerald else SavAmber,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (emailVerified) Icons.Outlined.CheckCircle else Icons.Outlined.WarningAmber,
                                    contentDescription = null
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (emailVerified) "Email Verified" else "Verification Required",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = email,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        try {
                                            Firebase.auth.currentUser?.reload()?.await()
                                            val verified = Firebase.auth.currentUser?.isEmailVerified == true
                                            onRefreshEmailVerification(verified)
                                            verificationNotice = if (verified) {
                                                "Email verification confirmed."
                                            } else {
                                                "Still unverified. Check your inbox and tap Refresh again."
                                            }
                                        } catch (e: Exception) {
                                            verificationNotice = e.localizedMessage ?: "Unable to refresh status."
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Refresh")
                            }
                            if (!emailVerified) {
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            try {
                                                Firebase.auth.currentUser?.sendEmailVerification()?.await()
                                                verificationNotice = "Verification email sent to $email."
                                            } catch (e: Exception) {
                                                verificationNotice = e.localizedMessage ?: "Could not send verification email."
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Outlined.MarkEmailRead, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Send Email")
                                }
                            }
                        }
                        if (verificationNotice != null) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = verificationNotice!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    3 -> {
                        val role = selectedRole ?: SavRole.STUDENT
                        Text("${role.displayName} Details", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Complete your role-specific profile before submitting your application to SAV administration.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = department,
                            onValueChange = { department = it },
                            label = { Text(role.departmentLabel) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reg_department_input"),
                            singleLine = true
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = designation,
                            onValueChange = { designation = it },
                            label = { Text(role.designationLabel) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reg_designation_input"),
                            singleLine = true
                        )
                        if (role.secondaryLabel != null) {
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = secondaryInfo,
                                onValueChange = { secondaryInfo = it },
                                label = { Text("${role.secondaryLabel} (Optional)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = bio,
                            onValueChange = { bio = it },
                            label = { Text("Introduction / Bio (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                    }
                }

                val err = localError ?: actionError
                if (err != null) {
                    Spacer(Modifier.height(14.dp))
                    SavErrorBanner(err)
                }

                Spacer(Modifier.height(22.dp))

                Button(
                    onClick = {
                        localError = null
                        when (step) {
                            0 -> {
                                if (selectedRole == null) {
                                    localError = "Please select your SAV role to continue."
                                } else {
                                    step = 1
                                }
                            }
                            1 -> {
                                when {
                                    fullName.trim().length < 2 -> localError = "Enter your full name (at least 2 characters)."
                                    phone.trim().length < 7 -> localError = "Enter a valid mobile number (at least 7 digits)."
                                    verificationId.trim().length < 2 -> localError = "Enter your institutional verification ID."
                                    else -> step = 2
                                }
                            }
                            2 -> {
                                if (!emailVerified) {
                                    localError = "Please verify your email address before continuing."
                                } else {
                                    step = 3
                                }
                            }
                            3 -> {
                                val role = selectedRole ?: return@Button
                                when {
                                    department.trim().isEmpty() -> localError = "Please enter ${role.departmentLabel}."
                                    designation.trim().isEmpty() -> localError = "Please enter ${role.designationLabel}."
                                    else -> {
                                        onSubmitRegistration(
                                            fullName,
                                            phone,
                                            role,
                                            verificationId,
                                            department,
                                            designation,
                                            secondaryInfo,
                                            bio
                                        )
                                    }
                                }
                            }
                        }
                    },
                    enabled = !isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("reg_next_submit_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SavNavyPrimary,
                        contentColor = SavPureWhite
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(Modifier.size(20.dp), color = SavPureWhite, strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = if (step < 3) "Continue" else "Submit Application for Approval",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
