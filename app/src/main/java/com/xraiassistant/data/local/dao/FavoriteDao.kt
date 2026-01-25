package com.xraiassistant.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.xraiassistant.data.local.entities.FavoriteEntity
import kotlinx.coroutines.flow.Flow

/**
 * FavoriteDao - Data Access Object for favorites table
 *
 * Provides CRUD operations for code snippet favorites.
 * Matches iOS ConversationStorageManager favorites methods.
 */
@Dao
interface FavoriteDao {

    /**
     * Insert a new favorite (or replace if exists)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    /**
     * Update an existing favorite
     */
    @Update
    suspend fun updateFavorite(favorite: FavoriteEntity)

    /**
     * Get all favorites ordered by creation date (newest first)
     * Returns Flow for reactive updates
     */
    @Query("SELECT * FROM favorites ORDER BY createdAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    /**
     * Get all favorites as a one-shot list (for non-reactive use cases)
     */
    @Query("SELECT * FROM favorites ORDER BY createdAt DESC")
    suspend fun getAllFavoritesOnce(): List<FavoriteEntity>

    /**
     * Get a favorite by its ID
     */
    @Query("SELECT * FROM favorites WHERE id = :id LIMIT 1")
    suspend fun getFavoriteById(id: String): FavoriteEntity?

    /**
     * Get a favorite by its message ID
     */
    @Query("SELECT * FROM favorites WHERE messageId = :messageId LIMIT 1")
    suspend fun getFavoriteByMessageId(messageId: String): FavoriteEntity?

    /**
     * Check if a message is favorited
     * Returns count (0 = not favorited, 1+ = favorited)
     */
    @Query("SELECT COUNT(*) FROM favorites WHERE messageId = :messageId")
    suspend fun isFavorited(messageId: String): Int

    /**
     * Get favorites by library ID
     */
    @Query("SELECT * FROM favorites WHERE libraryId = :libraryId ORDER BY createdAt DESC")
    fun getFavoritesByLibrary(libraryId: String): Flow<List<FavoriteEntity>>

    /**
     * Search favorites by title or code content
     */
    @Query("SELECT * FROM favorites WHERE title LIKE '%' || :query || '%' OR codeContent LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchFavorites(query: String): Flow<List<FavoriteEntity>>

    /**
     * Delete a favorite by ID
     */
    @Query("DELETE FROM favorites WHERE id = :id")
    suspend fun deleteFavorite(id: String)

    /**
     * Delete a favorite by message ID
     */
    @Query("DELETE FROM favorites WHERE messageId = :messageId")
    suspend fun deleteFavoriteByMessageId(messageId: String)

    /**
     * Clear all favorites
     */
    @Query("DELETE FROM favorites")
    suspend fun clearAllFavorites()

    /**
     * Get total favorites count
     */
    @Query("SELECT COUNT(*) FROM favorites")
    suspend fun getFavoritesCount(): Int

    /**
     * Update favorite order for reordering
     */
    @Query("UPDATE favorites SET favoriteOrder = :order WHERE id = :id")
    suspend fun updateFavoriteOrder(id: String, order: Int)

    /**
     * Update favorite tags
     */
    @Query("UPDATE favorites SET tags = :tags WHERE id = :id")
    suspend fun updateFavoriteTags(id: String, tags: String?)
}
