package com.xraiassistant.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xraiassistant.domain.models.Library3D
import com.xraiassistant.ui.viewmodels.ChatViewModel

/**
 * Library Selector Modal Bottom Sheet
 *
 * Provides a scrollable, readable interface for selecting 3D libraries
 * Replaces the cramped ExposedDropdownMenuBox with better UX
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibrarySelectorModal(
    chatViewModel: ChatViewModel,
    currentLibrary: Library3D?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val library = currentLibrary ?: chatViewModel.getCurrentLibrary()
    val availableLibraries = chatViewModel.getAvailableLibraries()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            // Header
            Text(
                text = "Select 3D Library",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            Text(
                text = "Choose the 3D framework for scene rendering",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Scrollable library list
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(availableLibraries) { availableLibrary ->
                    LibraryCard(
                        library = availableLibrary,
                        isSelected = library.id == availableLibrary.id,
                        onClick = {
                            chatViewModel.selectLibrary(availableLibrary.id)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryCard(
    library: Library3D,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            CardDefaults.outlinedCardBorder().copy(
                width = 2.dp,
                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
            )
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Library name and version
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = library.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )

                    // Version badge
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = library.version,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                // Library description
                Text(
                    text = library.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                // Library type badge
                val libraryType = when {
                    library.id.contains("babylon") -> "WebGL Engine"
                    library.id.contains("three") -> "WebGL Library"
                    library.id.contains("aframe") -> "WebXR Framework"
                    library.id.contains("r3f") -> "React + Three.js"
                    library.id.contains("reactylon") -> "React + Babylon.js"
                    else -> "3D Library"
                }

                Surface(
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    },
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = libraryType,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            // Selection indicator
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(28.dp)
                        .padding(start = 8.dp)
                )
            }
        }
    }
}
