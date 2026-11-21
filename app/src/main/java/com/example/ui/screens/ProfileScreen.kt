package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.UserProfile
import com.example.ui.components.ApprovalStatusPill
import com.example.ui.components.SavRoleBadge
import com.example.ui.theme.SavCrimson
import com.example.ui.theme.SavGoldBright
import com.example.ui.theme.SavNavyPrimary
import com.example.ui.theme.SavPureWhite

@Composable
fun ProfileScreen(
    profile: UserProfile,
    rememberMe: Boolean,
    themeMode: String,
    onRememberMeChange: (Boolean) -> Unit,
    onThemeModeChange: (String) -> Unit,
    onUpdateProfile: (String, String, String, String, String, String, () -> Unit) -> Unit,
    onSignOut: () -> Unit
) {
    val role = profile.parsedRole()
    var showEditDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SavNavyPrimary),
                shape = MaterialTheme.shapes.large
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_sav_emblem),
                            contentDescription = "SAV Identity Card",
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SAV FAMILY IDENTITY CARD",
                                style = MaterialTheme.typography.labelSmall,
                                color = SavGoldBright,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = profile.fullName,
                                style = MaterialTheme.typography.headlineSmall,
                                color = SavPureWhite
                            )
                            Text(
                                text = profile.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SavRoleBadge(role)
                        ApprovalStatusPill(profile.parsedStatus())
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Role Profile Structure", style = MaterialTheme.typography.titleMedium)
                        TextButton(
                            onClick = { showEditDialog = true },
                            modifier = Modifier.testTag("edit_profile_btn")
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Edit")
                        }
                    }
                    ProfileInfoRow("Institutional ID", profile.verificationId)
                    ProfileInfoRow("Mobile", profile.phone)
                    ProfileInfoRow(role.departmentLabel, profile.houseOrDepartment)
                    ProfileInfoRow(role.designationLabel, profile.classOrDesignation)
                    if (role.secondaryLabel != null && !profile.wardOrSecondaryInfo.isNullOrBlank()) {
                        ProfileInfoRow(role.secondaryLabel, profile.wardOrSecondaryInfo)
                    }
                    if (!profile.bio.isNullOrBlank()) {
                        ProfileInfoRow("Bio", profile.bio)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Verified,
                            contentDescription = null,
                            tint = SavGoldBright,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (profile.emailVerified) "Email Verified" else "Email Pending Verification",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Preferences & Security", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Remember Me", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Auto-restore Google session on launch",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = rememberMe, onCheckedChange = onRememberMeChange)
                    }
                    HorizontalDivider()
                    Text("Appearance Theme", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("SYSTEM" to "System", "LIGHT" to "Light", "DARK" to "Dark").forEach { (key, label) ->
                            FilterChip(
                                selected = themeMode == key,
                                onClick = { onThemeModeChange(key) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("profile_signout_btn"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SavCrimson)
            ) {
                Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Sign Out of SAV FAMILY")
            }
        }
    }

    if (showEditDialog) {
        var fullName by remember(profile) { mutableStateOf(profile.fullName) }
        var phone by remember(profile) { mutableStateOf(profile.phone) }
        var dept by remember(profile) { mutableStateOf(profile.houseOrDepartment) }
        var desig by remember(profile) { mutableStateOf(profile.classOrDesignation) }
        var secondary by remember(profile) { mutableStateOf(profile.wardOrSecondaryInfo.orEmpty()) }
        var bio by remember(profile) { mutableStateOf(profile.bio.orEmpty()) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Update Profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Mobile") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = dept,
                        onValueChange = { dept = it },
                        label = { Text(role.departmentLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = desig,
                        onValueChange = { desig = it },
                        label = { Text(role.designationLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (role.secondaryLabel != null) {
                        OutlinedTextField(
                            value = secondary,
                            onValueChange = { secondary = it },
                            label = { Text(role.secondaryLabel) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        label = { Text("Bio") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateProfile(fullName, phone, dept, desig, secondary, bio) {
                            showEditDialog = false
                        }
                    },
                    enabled = fullName.trim().length >= 2 && phone.trim().length >= 7,
                    colors = ButtonDefaults.buttonColors(containerColor = SavNavyPrimary)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
