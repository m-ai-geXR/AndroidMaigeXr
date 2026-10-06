package com.xraiassistant.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * MarkdownText - Renders markdown content with code block highlighting
 *
 * Simplified implementation using Compose Text with styled annotations
 * Matches iOS MarkdownMessageView.swift functionality
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val codeBlocks = remember(markdown) { extractCodeBlocks(markdown) }

    Column(modifier = modifier) {
        if (codeBlocks.isEmpty()) {
            // No code blocks, render as styled text
            Text(
                text = remember(markdown) { buildStyledText(markdown) },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            // Has code blocks, render them separately
            var lastIndex = 0
            codeBlocks.forEach { codeBlock ->
                // Render text before code block
                if (codeBlock.range.first > lastIndex) {
                    val textBefore = markdown.substring(lastIndex, codeBlock.range.first)
                    if (textBefore.isNotBlank()) {
                        Text(
                            text = remember(textBefore) { buildStyledText(textBefore) },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // Render code block
                CodeBlock(
                    code = codeBlock.code,
                    language = codeBlock.language
                )
                Spacer(modifier = Modifier.height(8.dp))

                lastIndex = codeBlock.range.last + 1
            }

            // Render remaining text after last code block
            if (lastIndex < markdown.length) {
                val textAfter = markdown.substring(lastIndex)
                if (textAfter.isNotBlank()) {
                    Text(
                        text = remember(textAfter) { buildStyledText(textAfter) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Build styled text with basic markdown formatting
 * Supports: **bold**, *italic*, `inline code`
 */
private fun buildStyledText(text: String) = buildAnnotatedString {
    var index = 0
    val cleanText = text.trim()

    while (index < cleanText.length) {
        when {
            // Bold: **text**
            cleanText.startsWith("**", index) -> {
                val endIndex = cleanText.indexOf("**", index + 2)
                if (endIndex != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(cleanText.substring(index + 2, endIndex))
                    }
                    index = endIndex + 2
                } else {
                    append(cleanText[index])
                    index++
                }
            }
            // Italic: *text*
            cleanText.startsWith("*", index) && !cleanText.startsWith("**", index) -> {
                val endIndex = cleanText.indexOf("*", index + 1)
                if (endIndex != -1) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(cleanText.substring(index + 1, endIndex))
                    }
                    index = endIndex + 1
                } else {
                    append(cleanText[index])
                    index++
                }
            }
            // Inline code: `code`
            cleanText.startsWith("`", index) && !cleanText.startsWith("```", index) -> {
                val endIndex = cleanText.indexOf("`", index + 1)
                if (endIndex != -1) {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color(0xFF2D2D2D),
                            color = Color(0xFFE0E0E0)
                        )
                    ) {
                        append(cleanText.substring(index + 1, endIndex))
                    }
                    index = endIndex + 1
                } else {
                    append(cleanText[index])
                    index++
                }
            }
            else -> {
                append(cleanText[index])
                index++
            }
        }
    }
}

/**
 * CodeBlock - Renders code with syntax highlighting and copy button
 *
 * Matches iOS implementation:
 * - Dark theme background
 * - Language label in blue uppercase
 * - Copy button with feedback
 * - Horizontal scrolling
 */
/** Code longer than this collapses to a preview, so a demo does not fill the screen. */
private const val COLLAPSED_LINES = 12

@Composable
fun CodeBlock(
    code: String,
    language: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showCopied by remember { mutableStateOf(false) }
    var expanded by remember(code) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val lines = remember(code) { code.lines() }
    val isLong = lines.size > COLLAPSED_LINES
    val shown = if (isLong && !expanded) lines.take(COLLAPSED_LINES).joinToString("\n") else code
    val codeBackground = Color(0xFF14191F)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(codeBackground)
    ) {
        // Header: language on the left, a quiet copy button on the right.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1C222B))
                .padding(start = 12.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = language?.uppercase().orEmpty(),
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp
            )

            Row(
                modifier = Modifier
                    .height(26.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Color.White.copy(alpha = if (showCopied) 0.18f else 0.10f))
                    .clickable(onClickLabel = "Copy code") {
                        copyToClipboard(context, code)
                        showCopied = true
                        scope.launch {
                            delay(2000)
                            showCopied = false
                        }
                    }
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (showCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = if (showCopied) "Copied" else "Copy",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Code, with long blocks collapsed behind a fade until expanded.
        Box {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                Text(
                    text = remember(shown, language) { highlightSyntax(shown, language) },
                    color = Color(0xFFE0E0E0),
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 20.sp
                )
            }
            if (isLong && !expanded) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(Brush.verticalGradient(listOf(codeBackground.copy(alpha = 0f), codeBackground)))
                )
            }
        }

        if (isLong) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClickLabel = if (expanded) "Collapse code" else "Expand code") { expanded = !expanded }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (expanded) "Show less" else "Show all ${lines.size} lines",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Syntax highlighting function
 * Matches iOS MarkdownMessageView keyword highlighting
 */
private fun highlightSyntax(code: String, language: String?) = buildAnnotatedString {
    // Keywords based on language (matching iOS implementation)
    val keywords = when (language?.lowercase()) {
        "javascript", "js", "typescript", "ts" -> listOf(
            "const", "let", "var", "function", "return", "if", "else", "for", "while",
            "class", "extends", "import", "export", "async", "await", "try", "catch",
            "new", "this", "super", "static", "get", "set", "typeof", "instanceof"
        )
        "python", "py" -> listOf(
            "def", "class", "if", "else", "elif", "for", "while", "return", "import",
            "from", "try", "except", "finally", "with", "as", "lambda", "yield", "async", "await"
        )
        "kotlin", "kt" -> listOf(
            "fun", "val", "var", "class", "object", "interface", "if", "else", "when",
            "for", "while", "return", "import", "package", "data", "sealed", "enum",
            "companion", "override", "suspend", "private", "public", "protected"
        )
        else -> emptyList()
    }

    // Simple keyword highlighting
    val words = code.split(Regex("\\b"))

    for (word in words) {
        if (keywords.contains(word)) {
            // Keyword style - cyan color matching iOS
            withStyle(SpanStyle(color = Color(0xFF66D9EF))) {
                append(word)
            }
        } else {
            append(word)
        }
    }
}

/**
 * Copy text to clipboard
 */
private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("code", text)
    clipboard.setPrimaryClip(clip)
}

/**
 * Extract code blocks from markdown text
 * Used for detecting and rendering code separately
 */
fun extractCodeBlocks(markdown: String): List<CodeBlockInfo> {
    val codeBlocks = mutableListOf<CodeBlockInfo>()
    val codeBlockRegex = "```(\\w+)?\\n([\\s\\S]*?)```".toRegex()

    codeBlockRegex.findAll(markdown).forEach { match ->
        val language = match.groupValues[1].takeIf { it.isNotEmpty() }
        val code = match.groupValues[2].trim()
        codeBlocks.add(CodeBlockInfo(code, language, match.range))
    }

    return codeBlocks
}

data class CodeBlockInfo(
    val code: String,
    val language: String?,
    val range: IntRange
)
