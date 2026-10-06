package com.xraiassistant.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Premium layout tokens, the Android twin of iOS Theme/Premium.swift: one corner
 * radius, compact pill and control heights, hairline separators. Compact controls
 * keep a 48 dp touch target through their surrounding padding or IconButton.
 */
object Metrics {
    val radius = 12.dp
    val pillHeight = 30.dp
    val control = 32.dp
    val hairline = 0.5.dp
}

/** A hairline separator in the divider colour. */
@Composable
fun Hairline(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.fillMaxWidth(),
        thickness = Metrics.hairline,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

/** Compact capsule for a menu: icon, value, chevron. */
@Composable
fun PillLabel(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(Metrics.pillHeight)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
            .clickable(onClick = onClick, role = Role.Button, onClickLabel = contentDescription)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 160.dp)
        )
        Icon(
            Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            modifier = Modifier.size(16.dp)
        )
    }
}

/**
 * The primary action on a message: a compact filled capsule in the accent, or a
 * quieter tint when there is no code to run. Matches iOS RunSceneLabel.
 */
@Composable
fun RunSceneButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Run scene",
    hasCode: Boolean = true
) {
    val fill = if (hasCode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    val content = if (hasCode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier
            .height(28.dp)
            .clip(CircleShape)
            .background(fill)
            .clickable(onClick = onClick, role = Role.Button)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = content, modifier = Modifier.size(14.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = content)
    }
}
