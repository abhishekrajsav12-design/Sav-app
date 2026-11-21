package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.data.model.CommunityChannel
import com.example.data.model.DirectoryEntry
import com.example.data.model.SchoolMessage
import com.example.data.model.UserProfile
import com.example.ui.components.SavEmptyState
import com.example.ui.components.SavRoleBadge
import com.example.ui.components.formatSavTimestamp
import com.example.ui.theme.SavGoldPrimary
import com.example.ui.theme.SavNavyPrimary
import com.example.ui.theme.SavPureWhite

@Composable
fun MessagingScreen(
    profile: UserProfile,
    selectedChannel: CommunityChannel,
    channelMessages: List<SchoolMessage>,
    directMessages: List<SchoolMessage>,
    directory: List<DirectoryEntry>,
    preselectedDirectPeer: DirectoryEntry?,
    onSelectChannel: (CommunityChannel) -> Unit,
    onSendChannelMessage: (String, () -> Unit) -> Unit,
    onSendDirectMessage: (String, String, () -> Unit) -> Unit
) {
    val role = profile.parsedRole()
    val accessibleChannels = remember(role) { CommunityChannel.accessibleFor(role) }
    var mode by remember(preselectedDirectPeer) {
        mutableIntStateOf(if (preselectedDirectPeer != null) 1 else 0)
    }
    var selectedPeer by remember(preselectedDirectPeer) {
        mutableStateOf(preselectedDirectPeer)
    }
    var draft by remember { mutableStateOf("") }

    val peers = remember(directory, profile.userId) {
        directory.filter { it.userId != profile.userId }
    }

    val activeDirectThreadMessages = remember(directMessages, selectedPeer, profile.userId) {
        val peerId = selectedPeer?.userId ?: return@remember emptyList()
        directMessages.filter { peerId in it.participantIds && profile.userId in it.participantIds }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            SegmentedButton(
                selected = mode == 0,
                onClick = { mode = 0 },
                shape = SegmentedButtonDefaults.itemShape(0, 2)
            ) {
                Text("Role Channels")
            }
            SegmentedButton(
                selected = mode == 1,
                onClick = { mode = 1 },
                shape = SegmentedButtonDefaults.itemShape(1, 2)
            ) {
                Text("Direct Messages")
            }
        }

        if (mode == 0) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(accessibleChannels) { channel ->
                    FilterChip(
                        selected = selectedChannel == channel,
                        onClick = { onSelectChannel(channel) },
                        label = { Text(channel.title) }
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = SavGoldPrimary,
                        modifier = Modifier.width(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${selectedChannel.title}: ${selectedChannel.subtitle}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (channelMessages.isEmpty()) {
                    item {
                        SavEmptyState(
                            title = "Start the Conversation",
                            description = "No messages yet in ${selectedChannel.title}. Send the first message below."
                        )
                    }
                } else {
                    items(channelMessages, key = { it.id }) { msg ->
                        MessageBubble(msg = msg, isMine = msg.senderId == profile.userId)
                    }
                }
            }

            MessageComposerBar(
                value = draft,
                onValueChange = { draft = it },
                placeholder = "Message ${selectedChannel.title}...",
                onSend = {
                    if (draft.trim().isNotEmpty()) {
                        onSendChannelMessage(draft) { draft = "" }
                    }
                }
            )
        } else {
            if (peers.isEmpty()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SavEmptyState(
                        title = "No Other Members Yet",
                        description = "Once other SAV FAMILY members are approved by school administration, you can message them directly here."
                    )
                }
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(peers, key = { it.userId }) { peer ->
                        val active = selectedPeer?.userId == peer.userId
                        Card(
                            modifier = Modifier
                                .clickable { selectedPeer = peer },
                            colors = CardDefaults.cardColors(
                                containerColor = if (active) SavNavyPrimary else MaterialTheme.colorScheme.surface
                            ),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(
                                    text = peer.fullName,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (active) SavPureWhite else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = peer.parsedRole().shortLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (active) SavGoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                if (selectedPeer == null) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(16.dp)
                    ) {
                        SavEmptyState(
                            title = "Select a Member",
                            description = "Tap any approved SAV FAMILY member above to open a private direct conversation."
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (activeDirectThreadMessages.isEmpty()) {
                            item {
                                SavEmptyState(
                                    title = "Direct Chat with ${selectedPeer!!.fullName}",
                                    description = "Send a private message to ${selectedPeer!!.fullName} (${selectedPeer!!.parsedRole().displayName})."
                                )
                            }
                        } else {
                            items(activeDirectThreadMessages, key = { it.id }) { msg ->
                                MessageBubble(msg = msg, isMine = msg.senderId == profile.userId)
                            }
                        }
                    }

                    MessageComposerBar(
                        value = draft,
                        onValueChange = { draft = it },
                        placeholder = "Message ${selectedPeer!!.fullName}...",
                        onSend = {
                            val peer = selectedPeer
                            if (peer != null && draft.trim().isNotEmpty()) {
                                onSendDirectMessage(peer.userId, draft) { draft = "" }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(msg: SchoolMessage, isMine: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 310.dp),
            color = if (isMine) SavNavyPrimary else MaterialTheme.colorScheme.surface,
            contentColor = if (isMine) SavPureWhite else MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMine) 16.dp else 4.dp,
                bottomEnd = if (isMine) 4.dp else 16.dp
            ),
            tonalElevation = if (isMine) 0.dp else 2.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (!isMine) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = msg.senderName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        SavRoleBadge(msg.parsedSenderRole())
                    }
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    text = msg.body,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = formatSavTimestamp(msg.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isMine) SavPureWhite.copy(alpha = 0.72f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MessageComposerBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    onSend: () -> Unit
) {
    Surface(
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text(placeholder) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("message_input"),
                maxLines = 3
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = onSend,
                enabled = value.trim().isNotEmpty(),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = SavNavyPrimary,
                    contentColor = SavPureWhite
                ),
                modifier = Modifier.testTag("message_send_btn")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send message")
            }
        }
    }
}
