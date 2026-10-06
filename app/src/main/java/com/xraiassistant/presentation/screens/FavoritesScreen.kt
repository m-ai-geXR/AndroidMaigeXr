package com.xraiassistant.presentation.screens

import android.text.format.DateUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.xraiassistant.data.local.entities.FavoriteEntity
import com.xraiassistant.data.repositories.FavoriteRepository
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
 * Favorites
 *
 * Opened from the star in the chat header, like the iOS favorites sheet: search,
 * one row per saved scene, tap to run it, swipe left to delete, Clear All in the
 * overflow menu.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FavoritesScreen(
    onFavoriteSelected: (FavoriteEntity) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    // Null until the first load lands, so the empty state never flashes on open.
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    var showClearDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            OverlayTopBar(title = "Favorites", onClose = onNavigateBack) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Clear all favorites") },
                            leadingIcon = { Icon(Icons.Filled.DeleteSweep, contentDescription = null) },
                            enabled = !favorites.isNullOrEmpty(),
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
                query = searchQuery,
                onQueryChange = viewModel::updateSearchQuery,
                placeholder = "Search favorites"
            )

            PullToRefreshLayout(
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                val loaded = favorites ?: return@PullToRefreshLayout

                // The empty state lives inside the list so the pull gesture still works.
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    if (loaded.isEmpty()) {
                        item(key = "empty") {
                            ListEmptyState(
                                icon = Icons.Filled.StarBorder,
                                title = if (searchQuery.isEmpty()) "No favorites yet" else "No matches",
                                body = if (searchQuery.isEmpty())
                                    "Tap the star on an AI reply with code to keep it here"
                                else
                                    "Try a different search term",
                                modifier = Modifier.fillParentMaxSize()
                            )
                        }
                    }

                    items(items = loaded, key = { it.id }) { favorite ->
                        Column(modifier = Modifier.animateItemPlacement()) {
                            SwipeToDeleteRow(
                                deleteLabel = "Delete favorite",
                                onDelete = { viewModel.deleteFavorite(favorite.id) }
                            ) {
                                FavoriteRow(
                                    favorite = favorite,
                                    onClick = { onFavoriteSelected(favorite) }
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
            title = { Text("Clear all favorites?") },
            text = { Text("This deletes every saved scene. This cannot be undone.") },
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
private fun FavoriteRow(
    favorite: FavoriteEntity,
    onClick: () -> Unit
) {
    val saved = remember(favorite.createdAt) {
        DateUtils.getRelativeTimeSpanString(
            favorite.createdAt,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()
    }
    val codePreview = remember(favorite.codeContent) {
        favorite.codeContent.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("//") }
            .take(3)
            .joinToString("  ")
    }

    ListRow(modifier = Modifier.clickable(onClick = onClick, onClickLabel = "Run scene")) {
        SceneThumbnail(
            screenshotBase64 = favorite.screenshotBase64,
            placeholder = Icons.Filled.Code
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = favorite.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = codePreview,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    text = listOfNotNull(saved, favorite.modelUsed).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                favorite.libraryId?.let { MetaPill(it) }
            }
        }
    }
}

/**
 * ViewModel for FavoritesScreen
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoriteRepository: FavoriteRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val refreshRequests = MutableStateFlow(0)

    /** Favorites matching the search box; null until the first query returns. */
    val favorites: StateFlow<List<FavoriteEntity>?> = combine(
        refreshRequests.flatMapLatest { favoriteRepository.getAllFavorites() },
        _searchQuery
    ) { all, query ->
        val q = query.trim()
        if (q.isEmpty()) {
            all
        } else {
            all.filter { favorite ->
                favorite.title.contains(q, ignoreCase = true) ||
                    favorite.codeContent.contains(q, ignoreCase = true) ||
                    favorite.libraryId?.contains(q, ignoreCase = true) == true
            }
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch { favoriteRepository.renameLegacyTitles() }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun refresh() {
        refreshRequests.update { it + 1 }
    }

    fun clearAll() {
        viewModelScope.launch { favoriteRepository.clearAllFavorites() }
    }

    fun deleteFavorite(id: String) {
        viewModelScope.launch {
            favoriteRepository.deleteFavorite(id)
        }
    }
}
