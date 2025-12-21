package com.xraiassistant.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xraiassistant.data.models.ChatMessage
import com.xraiassistant.data.models.getReplies
import java.text.SimpleDateFormat
import java.util.*

/**
 * Threaded Message View - Displays messages with reply threading support
 *
 * Equivalent to iOS ThreadedMessageView.swift
 *
 * Features:
 * - Nested reply chains with visual indicators
 * - Expand/collapse threads
 * - Reply button on AI messages
 * - Run Scene button for extracting code
 * - Markdown rendering for AI messages
 */
@Composable
fun ThreadedMessageView(
    message: ChatMessage,
    allMessages: List<ChatMessage>,
    isExpanded: Boolean,
    onReply: (String) -> Unit,
    onToggleThread: (String) -> Unit,
    onRunScene: ((code: String, libraryId: String?) -> Unit)? = null,
    onRunDemo: ((libraryId: String?) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val replies = allMessages.getReplies(message.id)
    val hasReplies = replies.isNotEmpty()

    // Extract code from message if it contains code blocks
    val extractedCode = extractCodeFromMessage(message.content)
    val hasCode = extractedCode != null && !message.isUser

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        // Main message bubble
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (message.isUser) {
                Arrangement.End
            } else {
                Arrangement.Start
            }
        ) {
            if (message.isUser) {
                Spacer(modifier = Modifier.weight(0.2f))
            }

            Column(
                horizontalAlignment = if (message.isUser) {
                    Alignment.End
                } else {
                    Alignment.Start
                }
            ) {
                // Message content card
                Box(
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (message.isUser) 16.dp else 4.dp,
                                bottomEnd = if (message.isUser) 4.dp else 16.dp
                            )
                        )
                        .background(
                            if (message.isUser) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .widthIn(max = 600.dp)
                ) {
                    Column {
                        // Header with icon and sender info
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (message.isUser) Icons.Default.Person else Icons.Default.SmartToy,
                                contentDescription = if (message.isUser) "User" else "AI",
                                tint = if (message.isUser) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    Color(0xFF2196F3)
                                },
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (message.isUser) "You" else (message.model ?: "m{ai}geXR"),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (message.isUser) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    Color(0xFF2196F3)
                                },
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Message content with markdown rendering
                        if (!message.isUser) {
                            // AI messages: Render with markdown support
                            MarkdownText(
                                markdown = message.content,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            // User messages: Plain text
                            Text(
                                text = message.content,
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Action buttons row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
                ) {
                    Text(
                        text = formatTime(message.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Light
                    )

                    if (!message.isUser) {
                        Spacer(modifier = Modifier.width(12.dp))

                        // Reply button
                        TextButton(
                            onClick = { onReply(message.id) },
                            modifier = Modifier.height(28.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Reply,
                                contentDescription = "Reply",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Reply",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    // Run Demo button for welcome messages OR Run Scene for AI messages with code
                    if (message.isWelcomeMessage && onRunDemo != null) {
                        Spacer(modifier = Modifier.width(12.dp))

                        Button(
                            onClick = { onRunDemo(message.libraryId) },
                            modifier = Modifier.height(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF9C27B0),  // Purple for demo
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Run Demo",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Run Demo",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else if (!message.isUser && onRunScene != null) {
                        // Always show Run Scene button for AI messages
                        Spacer(modifier = Modifier.width(12.dp))

                        Button(
                            onClick = {
                                // Use extracted code if available, otherwise use full content
                                val code = extractedCode ?: message.content
                                onRunScene(code, message.libraryId)
                            },
                            modifier = Modifier.height(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (hasCode) Color(0xFF4CAF50) else Color(0xFFFF9800),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Run Scene",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Run Scene",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Thread expansion toggle
                    if (hasReplies) {
                        Spacer(modifier = Modifier.width(12.dp))

                        TextButton(
                            onClick = { onToggleThread(message.id) },
                            modifier = Modifier.height(28.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = "${replies.size} ${if (replies.size == 1) "reply" else "replies"}",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            if (!message.isUser) {
                Spacer(modifier = Modifier.weight(0.2f))
            }
        }

        // Thread replies
        AnimatedVisibility(
            visible = hasReplies && isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = if (message.isUser) 0.dp else 40.dp,
                        end = if (message.isUser) 40.dp else 0.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                replies.forEach { reply ->
                    ThreadReplyView(
                        message = reply,
                        onReply = onReply,
                        onRunScene = onRunScene
                    )
                }
            }
        }
    }
}

/**
 * Thread Reply View - Displays a reply message in a thread
 *
 * Equivalent to iOS ThreadReplyView
 * Smaller, indented version of message with thread indicator line
 */
@Composable
fun ThreadReplyView(
    message: ChatMessage,
    onReply: (String) -> Unit,
    onRunScene: ((code: String, libraryId: String?) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val extractedCode = extractCodeFromMessage(message.content)
    val hasCode = extractedCode != null && !message.isUser

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Thread indicator line
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(64.dp)
                .background(Color(0xFF2196F3).copy(alpha = 0.3f))
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Reply bubble (smaller than main messages)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (message.isUser) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        }
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .widthIn(max = 500.dp)
            ) {
                // Message content with markdown rendering
                if (!message.isUser) {
                    MarkdownText(
                        markdown = message.content,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = message.content,
                        color = if (message.isUser) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTime(message.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Light
                )

                if (!message.isUser) {
                    TextButton(
                        onClick = { onReply(message.id) },
                        modifier = Modifier.height(24.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = "Reply",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    // Run Scene button for AI messages
                    if (onRunScene != null) {
                        TextButton(
                            onClick = {
                                val code = extractedCode ?: message.content
                                onRunScene(code, message.libraryId)
                            },
                            modifier = Modifier.height(24.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Run Scene",
                                modifier = Modifier.size(14.dp),
                                tint = if (hasCode) Color(0xFF4CAF50) else Color(0xFFFF9800)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Run Scene",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Extract code from message content
 * Looks for code between ```javascript (or similar) and ```
 * Returns null if no valid code block found
 */
private fun extractCodeFromMessage(content: String): String? {
    // Look for code between triple backticks
    val possibleStarts = listOf("```javascript", "```typescript", "```js", "```ts", "```jsx", "```html", "```")

    for (marker in possibleStarts) {
        val startIndex = content.indexOf(marker)
        if (startIndex != -1) {
            // Find the closing triple backticks
            val codeStart = startIndex + marker.length
            val endIndex = content.indexOf("```", codeStart)

            if (endIndex != -1) {
                // Extract code between markers
                var code = content.substring(codeStart, endIndex).trim()

                // Remove any trailing artifacts
                val artifacts = listOf("[/INSERT_CODE]", "[RUN_SCENE]", "```")
                for (artifact in artifacts) {
                    if (code.endsWith(artifact)) {
                        code = code.substring(0, code.length - artifact.length).trim()
                    }
                }

                // Sanity check: ignore if too short
                if (code.length >= 10) {
                    return code
                }
            }
        }
    }

    return null
}

private fun formatTime(date: Date): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(date)
}
