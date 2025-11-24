package com.xraiassistant.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for storing individual chat messages with threading support
 *
 * Foreign key relationship with ConversationEntity ensures data integrity
 * Threading support allows for nested reply chains
 */
@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE // Delete messages when conversation is deleted
        )
    ],
    indices = [
        Index("conversationId"),
        Index("threadParentId") // Index for efficient thread lookups
    ]
)
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val conversationId: String,
    val content: String,
    val isUser: Boolean,
    val timestamp: Long,
    val model: String?,
    val libraryId: String?,
    val threadParentId: String? = null,  // Reference to parent message for threading
    val isWelcomeMessage: Boolean = false  // Mark welcome messages
)
