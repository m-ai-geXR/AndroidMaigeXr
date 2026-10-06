package com.xraiassistant.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.*
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.delay
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xraiassistant.R
import com.xraiassistant.domain.models.Library3D
import com.xraiassistant.ui.theme.*
import com.xraiassistant.ui.viewmodels.ChatViewModel
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle

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
    onOpenHistory: () -> Unit = {},
    onOpenFavorites: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val messages by chatViewModel.messages.collectAsStateWithLifecycle()
    val isLoading by chatViewModel.isLoading.collectAsStateWithLifecycle()
    val lastGeneratedCode by chatViewModel.lastGeneratedCode.collectAsStateWithLifecycle()
    val codeReadyNotice by chatViewModel.codeReadyNotice.collectAsStateWithLifecycle()
    val currentLibrary by chatViewModel.currentLibrary.collectAsStateWithLifecycle()
    // Collected, not read once: a plain read never recomposes, so the header kept
    // showing the old model after a new one was picked.
    val selectedModel by chatViewModel.selectedModelState.collectAsStateWithLifecycle()
    val favoritedMessages by chatViewModel.favoritedMessages.collectAsStateWithLifecycle()

    var chatInput by remember { mutableStateOf("") }
    var providerNeedingKey by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current

    // "Thinking…" only until the first streamed words arrive; after that the
    // growing reply is its own progress indicator.
    val lastMessage = messages.lastOrNull()
    val awaitingFirstChunk = isLoading &&
        !(lastMessage != null && !lastMessage.isUser && lastMessage.content.isNotEmpty())

    // Follow the conversation: jump to a newly added message, and keep a streaming
    // reply in view, unless the user has scrolled up to read something.
    val isAtBottom by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull() ?: return@derivedStateOf true
            lastVisible.index >= info.totalItemsCount - 1
        }
    }
    LaunchedEffect(messages.size) {
        val count = listState.layoutInfo.totalItemsCount
        if (count > 0) listState.animateScrollToItem(count - 1)
    }
    LaunchedEffect(lastMessage?.content?.length) {
        val count = listState.layoutInfo.totalItemsCount
        if (lastMessage?.isStreaming == true && isAtBottom && count > 0) {
            // An offset past the item's end is clamped to the end of the list.
            listState.scrollToItem(count - 1, Int.MAX_VALUE)
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
            isLoading = isLoading,
            onOpenHistory = onOpenHistory,
            onOpenFavorites = onOpenFavorites
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
            items(
                items = topLevelMessages,
                key = { it.id },
                contentType = { if (it.isUser) "user" else "ai" }
            ) { message ->
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
                    onToggleFavorite = { messageId, title, code, libraryId ->
                        chatViewModel.toggleFavorite(messageId, title, code, libraryId)
                    },
                    isFavorited = favoritedMessages.contains(message.id),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (awaitingFirstChunk) {
                item(key = "thinking", contentType = "thinking") {
                    LoadingIndicator()
                }
            }
        }

        // AI Code Ready notice: shows once per new piece of code, then gets out of the way.
        AnimatedVisibility(
            visible = codeReadyNotice && lastGeneratedCode.isNotEmpty(),
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
        ) {
            AICodeReadyBanner(
                modelName = chatViewModel.getModelDisplayName(selectedModel),
                onRun = {
                    chatViewModel.dismissCodeReadyNotice()
                    onNavigateToScene()
                },
                onDismiss = { chatViewModel.dismissCodeReadyNotice() }
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
                val provider = chatViewModel.getModelProvider(selectedModel)
                if (chatInput.isNotBlank() && provider != null && !chatViewModel.isProviderConfigured(provider)) {
                    // Answer the tap up front and keep the draft, instead of sending
                    // and replying with an error bubble.
                    providerNeedingKey = provider
                } else if (chatInput.isNotBlank()) {
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

    providerNeedingKey?.let { provider ->
        ApiKeyRequiredDialog(
            providerName = provider,
            onOpenSettings = {
                providerNeedingKey = null
                chatViewModel.showSettings()
            },
            onDismiss = { providerNeedingKey = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatHeader(
    chatViewModel: ChatViewModel,
    selectedModel: String,
    currentLibrary: Library3D?,
    isLoading: Boolean,
    onOpenHistory: () -> Unit,
    onOpenFavorites: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 2.dp, bottom = 8.dp)
            ) {
            // Top bar, laid out like the iOS chat toolbar: History on the left,
            // the brand centred, Favorites and more options on the right.
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onOpenHistory, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(Icons.Default.History, contentDescription = "History")
                }
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MaigeXRAvatar(size = 26.dp)
                    MaigeXRWordmark(style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Row(modifier = Modifier.align(Alignment.CenterEnd)) {
                    IconButton(onClick = onOpenFavorites) {
                        Icon(Icons.Default.StarBorder, contentDescription = "Favorites")
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("New conversation") },
                                leadingIcon = { Icon(Icons.Default.AddComment, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    chatViewModel.newConversation()
                                }
                            )
                        }
                    }
                }
            }

            // Model and library pills, and the library docs, like the iOS header.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ModelSelector(
                    chatViewModel = chatViewModel,
                    selectedModel = selectedModel
                )
                LibrarySelector(
                    chatViewModel = chatViewModel,
                    currentLibrary = currentLibrary
                )
                Spacer(modifier = Modifier.weight(1f))
                val library = currentLibrary ?: chatViewModel.getCurrentLibrary()
                IconButton(onClick = { uriHandler.openUri(library.documentationURL) }) {
                    Icon(
                        Icons.AutoMirrored.Outlined.MenuBook,
                        contentDescription = "${library.displayName} documentation",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            }

            Hairline()
        }
    }
}

@Composable
private fun LoadingIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
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

/** How long the code-ready notice stays before tidying itself away. */
private const val CODE_READY_NOTICE_MILLIS = 6_000L

@Composable
private fun AICodeReadyBanner(
    modelName: String,
    onRun: () -> Unit,
    onDismiss: () -> Unit
) {
    // Same timing rule as a Material snackbar: accessibility services can ask for
    // longer (or no) timeouts on content that carries actions.
    val accessibilityManager = LocalAccessibilityManager.current
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    LaunchedEffect(Unit) {
        val timeout = accessibilityManager?.calculateRecommendedTimeoutMillis(
            CODE_READY_NOTICE_MILLIS,
            containsIcons = true,
            containsText = true,
            containsControls = true
        ) ?: CODE_READY_NOTICE_MILLIS
        if (timeout != Long.MAX_VALUE) {
            delay(timeout)
            currentOnDismiss()
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .semantics(mergeDescendants = true) {}
            ) {
                Text(
                    text = stringResource(R.string.ai_code_ready),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Generated by $modelName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TextButton(onClick = onRun) {
                Text("Run")
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
        color = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Hairline()

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

            // Input row: attach on the left, then one rounded field with the send
            // button inside it, matching the iOS input.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ImagePickerButton(
                    selectedImages = selectedImages,
                    onImagesSelected = { images ->
                        chatViewModel.addImages(images)
                    },
                    maxImages = 5
                )

                val canSend = value.isNotBlank() && enabled
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(start = 4.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.chat_input_hint)) },
                        enabled = enabled,
                        singleLine = true,  // Enter sends
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { if (canSend) onSend() }),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    IconButton(onClick = onSend, enabled = canSend) {
                        Box(
                            modifier = Modifier
                                .size(Metrics.control)
                                .clip(CircleShape)
                                .background(
                                    if (canSend) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.ArrowUpward,
                                contentDescription = "Send",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
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

    val modelName = chatViewModel.getModelDisplayName(selectedModel)
    PillLabel(
        icon = Icons.Outlined.Memory,
        text = modelName,
        onClick = { showModal = true },
        contentDescription = "Change model, currently $modelName"
    )

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

    PillLabel(
        icon = Icons.Outlined.ViewInAr,
        text = library.displayName,
        onClick = { showModal = true },
        contentDescription = "Change 3D library, currently ${library.displayName}"
    )

    // Show modal when button is clicked
    if (showModal) {
        LibrarySelectorModal(
            chatViewModel = chatViewModel,
            currentLibrary = currentLibrary,
            onDismiss = { showModal = false }
        )
    }
}