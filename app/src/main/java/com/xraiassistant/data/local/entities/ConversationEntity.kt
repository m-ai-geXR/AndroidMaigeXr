package com.xraiassistant.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing conversation metadata
 *
 * Stores conversation-level information like title, library used, timestamps,
 * and optional screenshot thumbnail from 3D scene rendering.
 *
 * @property screenshotBase64 Base64-encoded JPEG screenshot from 3D scene canvas
 *                            Captured 5 seconds after code injection to WebView
 *                            Format: "data:image/jpeg;base64,..." or just base64 string
 *                            Average size: 70-110KB per screenshot
 */
@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val library3DID: String?,
    val modelUsed: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val screenshotBase64: String? = null  // NEW: Scene screenshot thumbnail
)
