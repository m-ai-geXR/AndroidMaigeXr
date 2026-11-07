package com.xraiassistant.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.xraiassistant.data.models.ChatMessage
import com.xraiassistant.domain.models.Nova64Templates

/**
 * One chip per Nova64 game template, labelled with the words that pick it
 * ("dungeon · stone"). Shown under replies built on a template and under the
 * Nova64 welcome message. Tapping a chip runs that template's example.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Nova64TemplateChips(
    activeId: String?,
    onPreview: (code: String, libraryId: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(top = 8.dp)) {
        Text(
            text = "Game templates: say one of these words, or tap to preview",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Nova64Templates.all.forEach { t ->
                val active = t.id == activeId
                AssistChip(
                    onClick = { onPreview(t.config, "nova64") },
                    label = { Text(Nova64Templates.chipLabel(t)) },
                    colors = if (active) AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        labelColor = MaterialTheme.colorScheme.onPrimary
                    ) else AssistChipDefaults.assistChipColors(),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .semantics { contentDescription = "Preview the ${t.genre}, ${t.style} template" }
                )
            }
        }
    }
}

/** The template to highlight when [message] should show the chips, or "" for the welcome message; null hides them. */
fun nova64ChipsFor(message: ChatMessage): String? {
    if (message.isUser || message.libraryId != "nova64") return null
    return Nova64Templates.templateIdOf(message.content) ?: if (message.isWelcomeMessage) "" else null
}
