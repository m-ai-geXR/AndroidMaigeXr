package com.xraiassistant.presentation.screens

import android.text.format.DateUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.xraiassistant.data.local.entities.ConversationSummary
import com.xraiassistant.data.models.FavoriteTitle
import com.xraiassistant.data.models.MessagePreview
import com.xraiassistant.data.repositories.ConversationRepository
import com.xraiassistant.ui.components.ListEmptyState
import com.xraiassistant.ui.components.ListRow
import com.xraiassistant.ui.components.ListSearchField
import com.xraiassistant.ui.components.MetaPill
import com.xraiassistant.ui.components.OverlayTopBar
import com.xraiassistant.ui.components.PullToRefreshLayout
import com.xraiassistant.ui.components.RowDivider
import com.xraiassistant.ui.components.SceneThumbnail
import com.xraiassistant.ui.components.SwipeToDeleteRow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Conversation History
 *
 * Opened from the clock button in the chat header, like the iOS history sheet:
 * search, one row per conversation with its first reply, relative time and
 * message count, swipe left to delete, and Clear All in the overflow menu.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConversationHistoryScreen(
    onConversationSelected: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConversationHistoryViewModel = hiltViewModel()
) {
    // Null until the first load lands, so the empty state never flashes on open.
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    var showClearDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            OverlayTopBar(title = "History", onClose = onNavigateBack) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Clear all history") },
                            leadingIcon = { Icon(Icons.Filled.DeleteSweep, contentDescription = null) },
                            enabled = !conversations.isNullOrEmpty(),
                            onClick = {
                                showMenu = false
                                showClearDialog = true
                            }
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            ListSearchField(
                query = query,
                onQueryChange = viewModel::updateSearchQuery,
                placeholder = "Search conversations"
            )

            PullToRefreshLayout(
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                val loaded = conversations ?: return@PullToRefreshLayout

                // The empty state lives inside the list so the pull gesture still works.
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    if (loaded.isEmpty()) {
                        item(key = "empty") {
                            ListEmptyState(
                                icon = Icons.Filled.ChatBubbleOutline,
                                title = if (query.isEmpty()) "No conversations yet" else "No matches",
                                body = if (query.isEmpty())
                                    "Start a conversation and it will appear here"
                                else
                                    "No conversations match your search",
                                modifier = Modifier.fillParentMaxSize()
                            )
                        }
                    }

                    items(items = loaded, key = { it.conversation.id }) { summary ->
                        Column(modifier = Modifier.animateItemPlacement()) {
                            SwipeToDeleteRow(
                                deleteLabel = "Delete conversation",
                                onDelete = { viewModel.deleteConversation(summary.conversation.id) }
                            ) {
                                ConversationRow(
                                    summary = summary,
                                    onClick = { onConversationSelected(summary.conversation.id) }
                                )
                            }
                            RowDivider(startInset = 84.dp)
                        }
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear all history?") },
            text = { Text("This permanently deletes every saved conversation. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    viewModel.clearAll()
                }) {
                    Text("Clear all", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ConversationRow(
    summary: ConversationSummary,
    onClick: () -> Unit
) {
    val conversation = summary.conversation
    // Name the row after the scene when the reply named it; prompts make poor titles.
    val title = remember(summary.firstReply, conversation.title) {
        FavoriteTitle.fromProse(summary.firstReply) ?: conversation.title
    }
    val preview = remember(summary.firstReply) { MessagePreview.from(summary.firstReply) }
    val updated = remember(conversation.updatedAt) {
        DateUtils.getRelativeTimeSpanString(
            conversation.updatedAt,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()
    }

    ListRow(modifier = Modifier.clickable(onClick = onClick, onClickLabel = "Open conversation")) {
        SceneThumbnail(
            screenshotBase64 = conversation.screenshotBase64,
            placeholder = Icons.Filled.ViewInAr
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (preview != null) {
                Text(
                    text = preview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    text = "$updated · ${summary.messageCount} message${if (summary.messageCount == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                conversation.library3DID?.let { MetaPill(it) }
            }
        }
    }
}

/**
 * ViewModel for ConversationHistoryScreen
 *
 * Owns the query so it survives recomposition, filters by the search box, and
 * re-runs on pull-to-refresh.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ConversationHistoryViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository
) : ViewModel() {

    private val refreshRequests = MutableStateFlow(0)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /** Null until the first query returns. */
    val conversations: StateFlow<List<ConversationSummary>?> = combine(
        refreshRequests.flatMapLatest { conversationRepository.getConversationSummaries() },
        _searchQuery
    ) { all, query ->
        val q = query.trim()
        if (q.isEmpty()) all else all.filter {
            it.conversation.title.contains(q, ignoreCase = true) ||
                it.firstReply?.contains(q, ignoreCase = true) == true ||
                it.conversation.library3DID?.contains(q, ignoreCase = true) == true
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun refresh() {
        refreshRequests.update { it + 1 }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            conversationRepository.deleteConversation(id)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            conversationRepository.deleteAllConversations()
        }
    }
}
