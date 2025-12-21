package com.xraiassistant.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xraiassistant.data.models.ChatMessage
import com.xraiassistant.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Chat Message Card
 *
 * Displays individual chat messages with different styling for user vs AI
 * NOW WITH: Code extraction and "Run Scene" button for AI messages
 * Equivalent to ThreadedMessageView in iOS
 */
@Composable
fun ChatMessageCard(
    message: ChatMessage,
    onRunScene: ((code: String, libraryId: String?) -> Unit)? = null,
    onRunDemo: ((libraryId: String?) -> Unit)? = null,  // NEW: Run random demo callback
    modifier: Modifier = Modifier
) {
    // Extract code from message if it contains code blocks
    val extractedCode = extractCodeFromMessage(message.content)
    val hasCode = extractedCode != null && !message.isUser

    Row(
        modifier = modifier,
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
            val bubbleShape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 16.dp
            )

            Box(
                modifier = Modifier
                    .clip(bubbleShape)
                    .background(CyberpunkDarkGray)  // Dark background for both
                    .neonBorder(
                        color = if (message.isUser) NeonBlue else NeonCyan,
                        width = 1.5.dp,
                        glowRadius = 6.dp,
                        shape = bubbleShape
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
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
                            tint = if (message.isUser) NeonBlue else NeonCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (message.isUser) "You" else (message.model ?: "m{ai}geXR"),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (message.isUser) NeonBlue else NeonCyan,
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
                            color = CyberpunkWhite,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Row for timestamp and Run Demo/Scene buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 8.dp),  // Add padding to prevent cutoff
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
            ) {
                Text(
                    text = formatTime(message.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = CyberpunkGray,
                    fontWeight = FontWeight.Light
                )

                // Run Demo button for welcome messages OR Run Scene for AI messages with code
                if (message.isWelcomeMessage && onRunDemo != null) {
                    // Show "Run Demo" button for welcome messages
                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = { onRunDemo(message.libraryId) },
                        modifier = Modifier
                            .height(28.dp)
                            .neonButtonGlow(NeonPurpleGlow),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonPurple,
                            contentColor = CyberpunkBlack
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
                } else if (hasCode && extractedCode != null && onRunScene != null) {
                    // Show "Run Scene" button for AI messages with code
                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = { onRunScene(extractedCode, message.libraryId) },
                        modifier = Modifier
                            .height(28.dp)
                            .neonButtonGlow(NeonGreenGlow),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonGreen,
                            contentColor = CyberpunkBlack
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Run",
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
            }
        }

        if (!message.isUser) {
            Spacer(modifier = Modifier.weight(0.2f))
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