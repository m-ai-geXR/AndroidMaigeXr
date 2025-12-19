package com.xraiassistant.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xraiassistant.data.models.AIModel
import com.xraiassistant.ui.viewmodels.ChatViewModel

/**
 * Model Selector Modal Bottom Sheet
 *
 * Provides a scrollable, readable interface for selecting AI models
 * Replaces the cramped ExposedDropdownMenuBox with better UX
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelectorModal(
    chatViewModel: ChatViewModel,
    selectedModel: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                text = "Select AI Model",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            Text(
                text = "Choose the AI model for code generation",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Scrollable model list
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Models organized by provider
                chatViewModel.modelsByProvider.forEach { (provider, models) ->
                    // Provider header
                    item {
                        ProviderHeader(
                            provider = provider,
                            isConfigured = chatViewModel.isProviderConfigured(provider)
                        )
                    }

                    // Models for this provider
                    items(models) { model ->
                        ModelCard(
                            model = model,
                            isSelected = selectedModel == model.id,
                            isProviderConfigured = chatViewModel.isProviderConfigured(provider),
                            onClick = {
                                chatViewModel.selectedModel = model.id
                                onDismiss()
                            }
                        )
                    }
                }

                // Legacy models if available
                if (chatViewModel.availableModels.isNotEmpty()) {
                    item {
                        ProviderHeader(
                            provider = "Legacy",
                            isConfigured = true
                        )
                    }

                    chatViewModel.availableModels.forEach { modelId ->
                        item {
                            LegacyModelCard(
                                modelId = modelId,
                                isSelected = selectedModel == modelId,
                                chatViewModel = chatViewModel,
                                onClick = {
                                    chatViewModel.selectedModel = modelId
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderHeader(
    provider: String,
    isConfigured: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = provider,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        if (!isConfigured) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Not configured",
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Configure in Settings",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFF9800)
                )
            }
        }
    }
}

@Composable
private fun ModelCard(
    model: AIModel,
    isSelected: Boolean,
    isProviderConfigured: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        onClick = onClick,
        enabled = isProviderConfigured,
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
                // Model name
                Text(
                    text = model.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                // Model description
                Text(
                    text = model.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                // Pricing badge
                if (model.pricing.isNotEmpty()) {
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
                            text = model.pricing,
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

@Composable
private fun LegacyModelCard(
    modelId: String,
    isSelected: Boolean,
    chatViewModel: ChatViewModel,
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
                Text(
                    text = chatViewModel.getModelDisplayName(modelId),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                Text(
                    text = chatViewModel.getModelDescription(modelId),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

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
