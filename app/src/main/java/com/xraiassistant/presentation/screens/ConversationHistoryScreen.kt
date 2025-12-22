package com.xraiassistant.presentation.screens

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xraiassistant.data.local.entities.ConversationEntity
import com.xraiassistant.data.repositories.ConversationRepository
import com.xraiassistant.ui.theme.GlassCyberpunkDarkGray
import com.xraiassistant.ui.theme.NeonCyan
import com.xraiassistant.ui.theme.glassCard
import com.xraiassistant.ui.theme.neonGlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * Conversation History Screen
 *
 * Shows all saved conversations with ability to:
 * - Load previous conversations
 * - Delete individual conversations
 * - See conversation metadata (title, library, model, timestamp)
 *
 * Equivalent to iOS chat history functionality
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationHistoryScreen(
    onConversationSelected: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    conversationRepository: ConversationRepository = hiltViewModel<ConversationHistoryViewModel>().conversationRepository
) {
    val conversations by conversationRepository.getAllConversations().collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Conversation History") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        if (conversations.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "No Conversations Yet",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Start chatting to create conversation history",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Conversation list
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = conversations,
                    key = { it.id }
                ) { conversation ->
                    ConversationItem(
                        conversation = conversation,
                        onClick = { onConversationSelected(conversation.id) },
                        onDelete = {
                            scope.launch {
                                conversationRepository.deleteConversation(conversation.id)
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Individual conversation list item
 */
@Composable
private fun ConversationItem(
    conversation: ConversationEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .glassCard(
                backgroundColor = GlassCyberpunkDarkGray,
                blurRadius = 10.dp,
                borderGlow = NeonCyan,
                shape = RoundedCornerShape(14.dp)
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Screenshot thumbnail (left side)
            ConversationThumbnail(
                screenshotBase64 = conversation.screenshotBase64,
                modifier = Modifier
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Title
                Text(
                    text = conversation.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Metadata row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Library chip
                    conversation.library3DID?.let { libraryId ->
                        AssistChip(
                            onClick = { },
                            label = { Text(libraryId, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(24.dp)
                        )
                    }

                    // Model chip
                    conversation.modelUsed?.let { model ->
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    text = model.take(20),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }

                // Timestamp
                Text(
                    text = dateFormat.format(Date(conversation.updatedAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Delete button
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete conversation",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Conversation?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Conversation screenshot thumbnail
 * Decodes base64 image and displays it, or shows placeholder icon
 */
@Composable
private fun ConversationThumbnail(
    screenshotBase64: String?,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(screenshotBase64) {
        if (screenshotBase64 != null && screenshotBase64.isNotEmpty()) {
            try {
                // Remove data URL prefix if present
                val base64Data = screenshotBase64.removePrefix("data:image/jpeg;base64,")
                    .removePrefix("data:image/png;base64,")

                // Decode base64 to bitmap
                val imageBytes = Base64.decode(base64Data, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            } catch (e: Exception) {
                println("⚠️ Failed to decode screenshot: ${e.message}")
                null
            }
        } else {
            null
        }
    }

    Box(
        modifier = modifier
            .size(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.5.dp, NeonCyan, RoundedCornerShape(8.dp))
            .neonGlow(NeonCyan, 4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            // Display the screenshot
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Scene preview",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // Placeholder icon
            Icon(
                Icons.Default.Image,
                contentDescription = "No preview",
                tint = NeonCyan.copy(alpha = 0.5f),
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

/**
 * ViewModel for ConversationHistoryScreen
 *
 * Simple ViewModel that just provides access to ConversationRepository
 */
@HiltViewModel
class ConversationHistoryViewModel @Inject constructor(
    val conversationRepository: ConversationRepository
) : ViewModel()
