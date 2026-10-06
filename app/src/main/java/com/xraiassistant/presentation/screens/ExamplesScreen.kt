package com.xraiassistant.presentation.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xraiassistant.domain.models.CodeExample
import com.xraiassistant.domain.models.ExampleCategory
import com.xraiassistant.domain.models.ExampleDifficulty
import com.xraiassistant.domain.models.Library3D
import com.xraiassistant.ui.components.ListEmptyState
import com.xraiassistant.ui.components.ListSearchField
import com.xraiassistant.ui.components.OverlayTopBar
import com.xraiassistant.ui.theme.StatusColors

/**
 * Examples
 *
 * Browses the current library's built-in scenes, matching the iOS Examples tab:
 * search across title, description and keywords, filter by category and
 * difficulty, and tap a card to run it in the scene.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExamplesScreen(
    library: Library3D,
    onExampleSelected: (CodeExample) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<ExampleCategory?>(null) }
    var difficulty by rememberSaveable { mutableStateOf<ExampleDifficulty?>(null) }

    val filtered = remember(library.id, query, category, difficulty) {
        val q = query.trim()
        library.examples.filter { example ->
            (q.isEmpty() ||
                example.title.contains(q, ignoreCase = true) ||
                example.description.contains(q, ignoreCase = true) ||
                example.keywords.orEmpty().any { it.contains(q, ignoreCase = true) }) &&
                (category == null || example.category == category) &&
                (difficulty == null || example.difficulty == difficulty)
        }
    }
    // Only offer categories this library actually has.
    val categories = remember(library.id) {
        ExampleCategory.entries.filter { c -> library.examples.any { it.category == c } }
    }

    Scaffold(
        topBar = { OverlayTopBar(title = "Examples", onClose = null) },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            ListSearchField(
                query = query,
                onQueryChange = { query = it },
                placeholder = "Search examples or keywords"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterMenu(
                    label = category?.displayName ?: "Category",
                    icon = { Icon(Icons.Filled.Category, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    selected = category != null,
                    allLabel = "All categories",
                    options = categories.map { it.displayName },
                    onSelect = { index -> category = index?.let { categories[it] } }
                )
                FilterMenu(
                    label = difficulty?.displayName ?: "Difficulty",
                    icon = { Icon(Icons.Filled.BarChart, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    selected = difficulty != null,
                    allLabel = "All levels",
                    options = ExampleDifficulty.entries.map { it.displayName },
                    onSelect = { index -> difficulty = index?.let { ExampleDifficulty.entries[it] } }
                )
                if (category != null || difficulty != null) {
                    TextButton(onClick = { category = null; difficulty = null }) { Text("Clear") }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(
                    "${filtered.size} example${if (filtered.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    library.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (filtered.isEmpty()) {
                    item(key = "empty") {
                        ListEmptyState(
                            icon = Icons.Filled.SearchOff,
                            title = "No examples found",
                            body = "Try another search or clear the filters",
                            modifier = Modifier.fillParentMaxSize()
                        )
                    }
                }
                items(filtered, key = { it.id ?: it.title }) { example ->
                    ExampleCard(
                        example = example,
                        onClick = { onExampleSelected(example) },
                        modifier = Modifier.animateItemPlacement()
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterMenu(
    label: String,
    icon: @Composable () -> Unit,
    selected: Boolean,
    allLabel: String,
    options: List<String>,
    onSelect: (Int?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = selected,
            onClick = { expanded = true },
            label = { Text(label, maxLines = 1) },
            leadingIcon = icon,
            trailingIcon = {
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(allLabel) }, onClick = { expanded = false; onSelect(null) })
            HorizontalDivider()
            options.forEachIndexed { index, option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { expanded = false; onSelect(index) })
            }
        }
    }
}

@Composable
private fun ExampleCard(
    example: CodeExample,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick, onClickLabel = "Run example"),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    example.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                DifficultyBadge(example.difficulty)
            }

            Text(
                example.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            val keywords = example.keywords.orEmpty()
            if (keywords.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    keywords.take(4).forEach { keyword ->
                        Text(
                            keyword,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (keywords.size > 4) {
                        Text(
                            "+${keywords.size - 4}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    example.category.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (example.aiPromptHints != null) {
                    Icon(
                        Icons.Filled.AutoAwesome,
                        contentDescription = "Has AI prompt hints",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun DifficultyBadge(difficulty: ExampleDifficulty) {
    val color: Color = when (difficulty) {
        ExampleDifficulty.BEGINNER -> StatusColors.success
        ExampleDifficulty.INTERMEDIATE -> StatusColors.warning
        ExampleDifficulty.ADVANCED -> MaterialTheme.colorScheme.error
    }
    Text(
        difficulty.displayName,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .semantics { stateDescription = "Difficulty: ${difficulty.displayName}" }
    )
}
