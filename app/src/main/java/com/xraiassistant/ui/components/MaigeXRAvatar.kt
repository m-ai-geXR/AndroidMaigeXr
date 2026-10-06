package com.xraiassistant.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.xraiassistant.R

/**
 * The m{ai}geXR avatar (the GitHub org profile image), used wherever the app shows
 * its own identity. Decorative by default: it always sits next to the name.
 */
@Composable
fun MaigeXRAvatar(
    size: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    Image(
        painter = painterResource(R.drawable.maigexr_avatar),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
    )
}

/**
 * The m{ai}geXR wordmark: only {ai} takes the cobalt, the rest the foreground
 * colour (brand/brand.json). [muted] dims it for an unselected tab label.
 */
@Composable
fun MaigeXRWordmark(
    style: TextStyle,
    modifier: Modifier = Modifier,
    muted: Boolean = false
) {
    val text = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground
    val accent = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = text)) { append("m") }
            withStyle(SpanStyle(color = accent)) { append("{ai}") }
            withStyle(SpanStyle(color = text)) { append("geXR") }
        },
        style = style,
        modifier = modifier
    )
}
