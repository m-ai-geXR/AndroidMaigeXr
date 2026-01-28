package com.xraiassistant.data.repositories

import com.xraiassistant.data.local.dao.FavoriteDao
import com.xraiassistant.data.local.entities.FavoriteEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FavoriteRepository - Repository for managing favorite code snippets
 *
 * Provides a clean API for favorites operations, abstracting the DAO layer.
 * Matches iOS ConversationStorageManager favorites functionality.
 */
@Singleton
class FavoriteRepository @Inject constructor(
    private val favoriteDao: FavoriteDao
) {

    /**
     * Get all favorites as a reactive Flow
     */
    fun getAllFavorites(): Flow<List<FavoriteEntity>> {
        return favoriteDao.getAllFavorites()
    }

    /**
     * Get all favorites as a one-shot list
     */
    suspend fun getAllFavoritesOnce(): List<FavoriteEntity> {
        return favoriteDao.getAllFavoritesOnce()
    }

    /**
     * Save a new favorite
     *
     * @param messageId The ID of the message containing the code
     * @param conversationId The ID of the conversation
     * @param title Title for the favorite (usually first line of code or user-provided)
     * @param codeContent The actual code content
     * @param libraryId Optional 3D library ID (threejs, babylonjs, etc.)
     * @param modelUsed Optional AI model that generated the code
     * @param screenshotBase64 Optional base64-encoded screenshot
     */
    suspend fun saveFavorite(
        messageId: String,
        conversationId: String,
        title: String,
        codeContent: String,
        libraryId: String? = null,
        modelUsed: String? = null,
        screenshotBase64: String? = null
    ): FavoriteEntity {
        val favorite = FavoriteEntity(
            id = UUID.randomUUID().toString(),
            messageId = messageId,
            conversationId = conversationId,
            title = title,
            codeContent = codeContent,
            libraryId = libraryId,
            modelUsed = modelUsed,
            screenshotBase64 = screenshotBase64,
            createdAt = System.currentTimeMillis(),
            favoriteOrder = null,
            tags = null
        )

        favoriteDao.insertFavorite(favorite)
        println("Favorite saved: ${favorite.title}")
        return favorite
    }

    /**
     * Check if a message is already favorited
     */
    suspend fun isFavorited(messageId: String): Boolean {
        return favoriteDao.isFavorited(messageId) > 0
    }

    /**
     * Get a favorite by message ID
     */
    suspend fun getFavoriteByMessageId(messageId: String): FavoriteEntity? {
        return favoriteDao.getFavoriteByMessageId(messageId)
    }

    /**
     * Toggle favorite status for a message
     *
     * @return true if now favorited, false if unfavorited
     */
    suspend fun toggleFavorite(
        messageId: String,
        conversationId: String,
        title: String,
        codeContent: String,
        libraryId: String? = null,
        modelUsed: String? = null,
        screenshotBase64: String? = null
    ): Boolean {
        val existing = favoriteDao.getFavoriteByMessageId(messageId)

        return if (existing != null) {
            // Already favorited - remove it
            favoriteDao.deleteFavorite(existing.id)
            println("Favorite removed: ${existing.title}")
            false
        } else {
            // Not favorited - add it
            saveFavorite(
                messageId = messageId,
                conversationId = conversationId,
                title = title,
                codeContent = codeContent,
                libraryId = libraryId,
                modelUsed = modelUsed,
                screenshotBase64 = screenshotBase64
            )
            true
        }
    }

    /**
     * Delete a favorite by ID
     */
    suspend fun deleteFavorite(id: String) {
        favoriteDao.deleteFavorite(id)
        println("Favorite deleted: $id")
    }

    /**
     * Delete a favorite by message ID
     */
    suspend fun deleteFavoriteByMessageId(messageId: String) {
        favoriteDao.deleteFavoriteByMessageId(messageId)
    }

    /**
     * Search favorites by title or code content
     */
    fun searchFavorites(query: String): Flow<List<FavoriteEntity>> {
        return favoriteDao.searchFavorites(query)
    }

    /**
     * Get favorites by library ID
     */
    fun getFavoritesByLibrary(libraryId: String): Flow<List<FavoriteEntity>> {
        return favoriteDao.getFavoritesByLibrary(libraryId)
    }

    /**
     * Clear all favorites
     */
    suspend fun clearAllFavorites() {
        favoriteDao.clearAllFavorites()
        println("All favorites cleared")
    }

    /**
     * Get total count of favorites
     */
    suspend fun getFavoritesCount(): Int {
        return favoriteDao.getFavoritesCount()
    }

    /**
     * Update favorite tags
     */
    suspend fun updateTags(id: String, tags: List<String>) {
        val tagsString = if (tags.isEmpty()) null else tags.joinToString(",")
        favoriteDao.updateFavoriteTags(id, tagsString)
    }
}
