package com.xraiassistant.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * FavoriteEntity - Room Entity for storing favorite code snippets
 *
 * Stores individual code snippets from AI responses that users have bookmarked.
 * Matches iOS implementation in ConversationModels.swift
 *
 * Features:
 * - Independent storage (no foreign keys - orphaned favorites allowed)
 * - Stores code content with metadata
 * - Optional screenshot thumbnail for visual preview
 * - Support for tags and ordering
 */
@Entity(
    tableName = "favorites",
    indices = [
        Index(value = ["messageId"]),
        Index(value = ["createdAt"])
    ]
)
data class FavoriteEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "messageId")
    val messageId: String,

    @ColumnInfo(name = "conversationId")
    val conversationId: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "codeContent")
    val codeContent: String,

    @ColumnInfo(name = "libraryId")
    val libraryId: String? = null,

    @ColumnInfo(name = "modelUsed")
    val modelUsed: String? = null,

    @ColumnInfo(name = "screenshotBase64")
    val screenshotBase64: String? = null,

    @ColumnInfo(name = "createdAt")
    val createdAt: Long,

    @ColumnInfo(name = "favoriteOrder")
    val favoriteOrder: Int? = null,

    @ColumnInfo(name = "tags")
    val tags: String? = null  // Comma-separated tag list
)

/**
 * Extension to convert tags string to list
 */
fun FavoriteEntity.getTagsList(): List<String> {
    return tags?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
}

/**
 * Extension to create tags string from list
 */
fun List<String>.toTagsString(): String? {
    return if (isEmpty()) null else joinToString(",")
}
