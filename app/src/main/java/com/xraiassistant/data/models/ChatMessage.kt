package com.xraiassistant.data.models

import java.util.Date
import java.util.UUID

/**
 * Chat message data model with threading support
 * Equivalent to EnhancedChatMessage in iOS ConversationModels.swift
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val isUser: Boolean,
    val timestamp: Date = Date(),
    val model: String? = null,
    val libraryId: String? = null,  // Track which 3D library this message is for
    val isWelcomeMessage: Boolean = false,  // Mark as welcome message to show "Run Demo" button
    val threadParentId: String? = null,  // Reference to parent message for threading
    val replies: List<String> = emptyList()  // Child message IDs
) {
    // Helper property: Check if this is a top-level message (not a reply)
    val isTopLevel: Boolean
        get() = threadParentId == null

    companion object {
        fun userMessage(
            content: String,
            threadParentId: String? = null
        ): ChatMessage {
            return ChatMessage(
                content = content,
                isUser = true,
                threadParentId = threadParentId
            )
        }

        fun aiMessage(
            content: String,
            model: String? = null,
            libraryId: String? = null,
            threadParentId: String? = null
        ): ChatMessage {
            return ChatMessage(
                content = content,
                isUser = false,
                model = model,
                libraryId = libraryId,
                threadParentId = threadParentId
            )
        }
    }
}

/**
 * Extension functions for working with message threads
 */
fun List<ChatMessage>.getTopLevelMessages(): List<ChatMessage> {
    return this.filter { it.isTopLevel }
}

fun List<ChatMessage>.getReplies(parentId: String): List<ChatMessage> {
    return this.filter { it.threadParentId == parentId }
        .sortedBy { it.timestamp }
}

fun List<ChatMessage>.hasReplies(messageId: String): Boolean {
    return this.any { it.threadParentId == messageId }
}