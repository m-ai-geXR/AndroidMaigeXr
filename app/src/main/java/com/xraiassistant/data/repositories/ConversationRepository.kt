package com.xraiassistant.data.repositories

import com.xraiassistant.data.local.dao.ConversationDao
import com.xraiassistant.data.local.entities.ConversationEntity
import com.xraiassistant.data.local.entities.MessageEntity
import com.xraiassistant.data.models.ChatMessage
import kotlinx.coroutines.flow.Flow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing chat conversation persistence
 *
 * Handles saving/loading conversations and messages to/from Room database
 * Equivalent to iOS chat history functionality
 */
@Singleton
class ConversationRepository @Inject constructor(
    private val conversationDao: ConversationDao
) {

    // MARK: - Conversation Operations

    /**
     * Save a new conversation with messages
     *
     * @param messages List of chat messages
     * @param libraryId The 3D library used in conversation
     * @param modelUsed The AI model used
     * @param screenshotBase64 Optional base64-encoded screenshot from 3D scene
     * @return The conversation ID
     */
    suspend fun saveConversation(
        messages: List<ChatMessage>,
        libraryId: String?,
        modelUsed: String?,
        screenshotBase64: String? = null  // NEW: Optional screenshot
    ): String {
        val conversationId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        // Generate title from first user message (or use default)
        val title = messages.firstOrNull { it.isUser }?.content?.take(50)?.trim()
            ?: "Conversation ${Date(now).toString().substring(0, 16)}"

        // Create conversation entity
        val conversation = ConversationEntity(
            id = conversationId,
            title = title,
            library3DID = libraryId,
            modelUsed = modelUsed,
            createdAt = now,
            updatedAt = now,
            screenshotBase64 = screenshotBase64  // NEW: Store screenshot
        )

        // Create message entities
        val messageEntities = messages.map { message ->
            MessageEntity(
                id = message.id,
                conversationId = conversationId,
                content = message.content,
                isUser = message.isUser,
                timestamp = message.timestamp.time,
                model = message.model,
                libraryId = message.libraryId,
                threadParentId = message.threadParentId,
                isWelcomeMessage = message.isWelcomeMessage
            )
        }

        // Save to database
        conversationDao.insertConversation(conversation)
        conversationDao.insertMessages(messageEntities)

        return conversationId
    }

    /**
     * Update an existing conversation with new messages
     *
     * @param conversationId The conversation ID to update
     * @param messages Updated list of messages
     * @param libraryId Current library ID
     * @param modelUsed Current AI model
     */
    suspend fun updateConversation(
        conversationId: String,
        messages: List<ChatMessage>,
        libraryId: String?,
        modelUsed: String?
    ) {
        val conversation = conversationDao.getConversationById(conversationId) ?: return

        // Update conversation metadata
        val updatedConversation = conversation.copy(
            library3DID = libraryId,
            modelUsed = modelUsed,
            updatedAt = System.currentTimeMillis()
        )

        // Create message entities
        val messageEntities = messages.map { message ->
            MessageEntity(
                id = message.id,
                conversationId = conversationId,
                content = message.content,
                isUser = message.isUser,
                timestamp = message.timestamp.time,
                model = message.model,
                libraryId = message.libraryId,
                threadParentId = message.threadParentId,
                isWelcomeMessage = message.isWelcomeMessage
            )
        }

        // Save to database
        conversationDao.updateConversation(updatedConversation)
        conversationDao.deleteMessagesForConversation(conversationId)
        conversationDao.insertMessages(messageEntities)
    }

    /**
     * Get conversation with all its messages
     *
     * @param conversationId The conversation ID
     * @return Pair of conversation and messages
     */
    suspend fun getConversationWithMessages(
        conversationId: String
    ): Pair<ConversationEntity?, List<MessageEntity>> {
        return conversationDao.getConversationWithMessages(conversationId)
    }

    /**
     * Get all conversations ordered by most recent
     *
     * @return Flow of conversation list
     */
    fun getAllConversations(): Flow<List<ConversationEntity>> {
        return conversationDao.getAllConversations()
    }

    /**
     * Delete a specific conversation and all its messages
     *
     * @param conversationId The conversation ID to delete
     */
    suspend fun deleteConversation(conversationId: String) {
        conversationDao.deleteConversation(conversationId)
    }

    /**
     * Delete all conversations and messages
     * Used for "Clear All History" in Settings
     */
    suspend fun deleteAllConversations() {
        conversationDao.deleteAllConversations()
    }

    // MARK: - Screenshot Operations

    /**
     * Update conversation with screenshot thumbnail
     *
     * Called after 3D scene renders and screenshot is captured from WebView canvas.
     * Updates only the screenshot field without modifying messages or other metadata.
     *
     * @param conversationId The conversation ID to update
     * @param screenshotBase64 Base64-encoded JPEG screenshot from canvas.toDataURL()
     */
    suspend fun updateConversationScreenshot(
        conversationId: String,
        screenshotBase64: String
    ) {
        val conversation = conversationDao.getConversationById(conversationId) ?: run {
            println("⚠️ Cannot update screenshot: Conversation $conversationId not found")
            return
        }

        // Update only screenshot field + timestamp
        val updatedConversation = conversation.copy(
            screenshotBase64 = screenshotBase64,
            updatedAt = System.currentTimeMillis()
        )

        conversationDao.updateConversation(updatedConversation)
        println("✅ Screenshot saved for conversation: $conversationId")
        println("📊 Screenshot size: ${screenshotBase64.length} characters")
    }
}
