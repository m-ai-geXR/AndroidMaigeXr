package com.xraiassistant.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xraiassistant.R
import com.xraiassistant.domain.models.Library3D
import com.xraiassistant.ui.theme.*
import com.xraiassistant.ui.viewmodels.ChatViewModel

/**
 * Chat Screen
 *
 * Displays AI conversation interface
 * Equivalent to chat section in iOS ContentView.swift
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatViewModel: ChatViewModel,
    onNavigateToScene: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val messages by chatViewModel.messages.collectAsStateWithLifecycle()
    val isLoading by chatViewModel.isLoading.collectAsStateWithLifecycle()
    val lastGeneratedCode by chatViewModel.lastGeneratedCode.collectAsStateWithLifecycle()
    val currentLibrary by chatViewModel.currentLibrary.collectAsStateWithLifecycle()
    val selectedModel = chatViewModel.selectedModel
    
    var chatInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Chat header with model and library info
        ChatHeader(
            chatViewModel = chatViewModel,
            selectedModel = selectedModel,
            currentLibrary = currentLibrary,
            isLoading = isLoading
        )
        
        // Messages list
        val expandedThreads by chatViewModel.expandedThreads.collectAsStateWithLifecycle()
        val topLevelMessages = remember(messages) {
            messages.filter { it.isTopLevel }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(topLevelMessages) { message ->
                ThreadedMessageView(
                    message = message,
                    allMessages = messages,
                    isExpanded = expandedThreads.contains(message.id),
                    onReply = { messageId ->
                        chatViewModel.setReplyTo(messageId)
                    },
                    onToggleThread = { messageId ->
                        chatViewModel.toggleThread(messageId)
                    },
                    onRunScene = { code, libraryId ->
                        // Run the code from this message
                        chatViewModel.runCodeFromMessage(code, libraryId)
                        // Navigate to Scene tab to show the result
                        onNavigateToScene()
                    },
                    onRunDemo = { libraryId ->
                        // Run a random demo from the specified library
                        chatViewModel.loadRandomDemoExample(libraryId)
                        // Navigate to Scene tab to show the demo
                        onNavigateToScene()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (isLoading) {
                item {
                    LoadingIndicator()
                }
            }
        }
        
        // AI Code Ready Banner
        if (lastGeneratedCode.isNotEmpty()) {
            AICodeReadyBanner(
                chatViewModel = chatViewModel,
                codeLength = lastGeneratedCode.length,
                selectedModel = selectedModel
            )
        }

        // Reply Indicator
        val replyToMessageId by chatViewModel.replyToMessageId.collectAsStateWithLifecycle()
        replyToMessageId?.let { replyId ->
            ReplyIndicator(
                replyToMessageId = replyId,
                messages = messages,
                onCancel = { chatViewModel.clearReplyTo() }
            )
        }

        // Chat input
        ChatInputField(
            value = chatInput,
            onValueChange = { chatInput = it },
            onSend = {
                if (chatInput.isNotBlank()) {
                    chatViewModel.sendMessage(chatInput.trim())
                    chatInput = ""
                    chatViewModel.clearImages()  // Clear images after sending
                    keyboardController?.hide()
                }
            },
            enabled = !isLoading,
            chatViewModel = chatViewModel,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatHeader(
    chatViewModel: ChatViewModel,
    selectedModel: String,
    currentLibrary: Library3D?,
    isLoading: Boolean
) {
    Surface(
        color = CyberpunkDarkGray,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
            // Top row with title and loading indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology, // Brain icon
                    contentDescription = null,
                    tint = NeonCyan
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "m{ai}geXR",
                    style = MaterialTheme.typography.headlineSmall,
                    color = NeonCyan
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = NeonCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Model and Library selector row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Model selector
                Icon(
                    imageVector = Icons.Default.Computer,
                    contentDescription = null,
                    tint = CyberpunkGray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Model:",
                    style = MaterialTheme.typography.bodySmall,
                    color = CyberpunkGray
                )
                Spacer(modifier = Modifier.width(4.dp))

                ModelSelector(
                    chatViewModel = chatViewModel,
                    selectedModel = selectedModel
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Library selector
                Icon(
                    imageVector = Icons.Default.ViewInAr,
                    contentDescription = null,
                    tint = CyberpunkGray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Library:",
                    style = MaterialTheme.typography.bodySmall,
                    color = CyberpunkGray
                )
                Spacer(modifier = Modifier.width(4.dp))
                
                LibrarySelector(
                    chatViewModel = chatViewModel,
                    currentLibrary = currentLibrary
                )

                Spacer(modifier = Modifier.weight(1f))
            }
            }

            // Gradient divider line (cyan fade to transparent)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .gradientBackground(
                        colors = CyanFadeGradient,
                        angle = 0f,  // Horizontal fade
                        shape = RoundedCornerShape(0.dp)
                    )
            )
        }
    }
}

@Composable
private fun LoadingIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.chat_thinking),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AICodeReadyBanner(
    chatViewModel: ChatViewModel,
    codeLength: Int,
    selectedModel: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .neonCardGlow(NeonGreenGlow),
        colors = CardDefaults.cardColors(
            containerColor = CyberpunkDarkGray
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = NeonGreen
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.ai_code_ready),
                    style = MaterialTheme.typography.bodyLarge,
                    color = CyberpunkWhite
                )
                Text(
                    text = "Generated by ${chatViewModel.getModelDisplayName(selectedModel)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = CyberpunkGray
                )
            }
            Text(
                text = "($codeLength chars)",
                style = MaterialTheme.typography.bodySmall,
                color = CyberpunkGray
            )
        }
    }
}

@Composable
private fun ChatInputField(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean,
    chatViewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val selectedImages by chatViewModel.selectedImages.collectAsState()

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Image preview row
            if (selectedImages.isNotEmpty()) {
                ImagePreviewRow(
                    images = selectedImages,
                    onRemoveImage = { index ->
                        chatViewModel.removeImageAt(index)
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Input row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Image picker button
                ImagePickerButton(
                    selectedImages = selectedImages,
                    onImagesSelected = { images ->
                        chatViewModel.addImages(images)
                    },
                    maxImages = 5
                )

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .weight(1f)
                        .neonInputGlow(NeonCyan),  // Keep the neon glow
                    placeholder = {
                        Text(stringResource(R.string.chat_input_hint))
                    },
                    enabled = enabled,
                    singleLine = true,  // CRITICAL: Prevents newline, enables Enter to send
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = { if (enabled) onSend() }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CyberpunkGray,
                        cursorColor = NeonCyan,
                        focusedLabelColor = NeonCyan
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                FloatingActionButton(
                    onClick = onSend,
                    modifier = Modifier
                        .size(48.dp)
                        .then(
                            // Apply strong neon glow when button is active
                            if (value.isNotBlank() && enabled) {
                                Modifier.neonButtonGlow(NeonPink)
                            } else {
                                Modifier
                            }
                        ),
                    containerColor = if (value.isBlank() || !enabled) {
                        MaterialTheme.colorScheme.surfaceVariant
                    } else {
                        NeonPink  // Use neon pink for active send button
                    }
                ) {
                    Icon(
                        Icons.Filled.Send,
                        contentDescription = "Send",
                        tint = if (value.isBlank() || !enabled) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            CyberpunkBlack  // Dark icon on bright button
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ModelSelector(
    chatViewModel: ChatViewModel,
    selectedModel: String
) {
    var showModal by remember { mutableStateOf(false) }

    // Model selector button
    Card(
        onClick = { showModal = true },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2196F3).copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = chatViewModel.getModelDisplayName(selectedModel),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF2196F3),
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = null,
                tint = Color(0xFF2196F3),
                modifier = Modifier.size(14.dp)
            )
        }
    }

    // Show modal when button is clicked
    if (showModal) {
        ModelSelectorModal(
            chatViewModel = chatViewModel,
            selectedModel = selectedModel,
            onDismiss = { showModal = false }
        )
    }
}

@Composable
private fun LibrarySelector(
    chatViewModel: ChatViewModel,
    currentLibrary: Library3D?
) {
    var showModal by remember { mutableStateOf(false) }
    val library = currentLibrary ?: chatViewModel.getCurrentLibrary()

    // Library selector button
    Card(
        onClick = { showModal = true },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = library.displayName,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF4CAF50),
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(14.dp)
            )
        }
    }

    // Show modal when button is clicked
    if (showModal) {
        LibrarySelectorModal(
            chatViewModel = chatViewModel,
            currentLibrary = currentLibrary,
            onDismiss = { showModal = false }
        )
    }
}