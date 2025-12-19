package com.xraiassistant.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xraiassistant.data.models.ChatMessage

/**
 * Reply Indicator - Shows which message is being replied to
 *
 * Equivalent to iOS replyIndicatorView (EnhancedChatView.swift lines 544-575)
 * Displays above the input field when user taps "Reply" on a message
 */
@Composable
fun ReplyIndicator(
    replyToMessageId: String,
    messages: List<ChatMessage>,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Find the message being replied to
    val replyToMessage = messages.firstOrNull { it.id == replyToMessageId }

    if (replyToMessage != null) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = modifier
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reply info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Replying to:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        text = replyToMessage.content.take(50) +
                               if (replyToMessage.content.length > 50) "..." else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Cancel button
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel reply",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Top border separator
        Divider(
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = 1.dp
        )
    }
}
