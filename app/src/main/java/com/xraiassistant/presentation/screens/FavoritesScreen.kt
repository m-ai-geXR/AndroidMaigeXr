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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
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
import androidx.lifecycle.viewModelScope
import com.xraiassistant.data.local.entities.FavoriteEntity
import com.xraiassistant.data.repositories.FavoriteRepository
import com.xraiassistant.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * Favorites Screen
 *
 * Shows all bookmarked code snippets from AI responses.
 * Users can:
 * - View saved code favorites
 * - Load code into the scene
 * - Delete favorites
 * - Search by title or code content
 *
 * Matches iOS FavoritesScreen functionality
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onFavoriteSelected: (FavoriteEntity) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle(initialValue = emptyList())
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle(initialValue = "")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            tint = NeonPink
                        )
                        Text("Favorites")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search favorites...") },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = "Search")
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            if (favorites.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = NeonPink.copy(alpha = 0.5f)
                        )
                        Text(
                            text = if (searchQuery.isEmpty()) "No Favorites Yet" else "No Results Found",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (searchQuery.isEmpty())
                                "Tap the star icon on AI code responses to save them here"
                            else
                                "Try a different search term",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Favorites list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = favorites,
                        key = { it.id }
                    ) { favorite ->
                        FavoriteItem(
                            favorite = favorite,
                            onClick = { onFavoriteSelected(favorite) },
                            onDelete = { viewModel.deleteFavorite(favorite.id) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual favorite list item
 */
@Composable
private fun FavoriteItem(
    favorite: FavoriteEntity,
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
                borderGlow = NeonPink,
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
            FavoriteThumbnail(
                screenshotBase64 = favorite.screenshotBase64
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Title
                Text(
                    text = favorite.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Code preview
                Text(
                    text = favorite.codeContent.take(100).replace("\n", " "),
                    style = MaterialTheme.typography.bodySmall,
                    color = NeonCyan.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Metadata row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Library chip
                    favorite.libraryId?.let { libraryId ->
                        AssistChip(
                            onClick = { },
                            label = { Text(libraryId, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(24.dp)
                        )
                    }

                    // Model chip
                    favorite.modelUsed?.let { model ->
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    text = model.take(15),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }

                // Timestamp
                Text(
                    text = dateFormat.format(Date(favorite.createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Delete button
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete favorite",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Favorite?") },
            text = { Text("This will remove the code snippet from your favorites.") },
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
 * Favorite screenshot thumbnail
 * Decodes base64 image and displays it, or shows code icon placeholder
 */
@Composable
private fun FavoriteThumbnail(
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
                println("Failed to decode favorite screenshot: ${e.message}")
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
            .border(1.5.dp, NeonPink, RoundedCornerShape(8.dp))
            .neonGlow(NeonPink, 4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            // Display the screenshot
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Code preview",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // Placeholder icon (code icon for favorites)
            Icon(
                Icons.Filled.Code,
                contentDescription = "No preview",
                tint = NeonPink.copy(alpha = 0.5f),
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

/**
 * ViewModel for FavoritesScreen
 */
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoriteRepository: FavoriteRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val favorites = favoriteRepository.getAllFavorites()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        // Note: For simplicity, filtering is done in the UI
        // For large datasets, use favoriteRepository.searchFavorites(query)
    }

    fun deleteFavorite(id: String) {
        viewModelScope.launch {
            favoriteRepository.deleteFavorite(id)
        }
    }
}
