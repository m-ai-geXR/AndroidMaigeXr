package com.xraiassistant.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.xraiassistant.data.local.dao.ConversationDao
import com.xraiassistant.data.local.dao.FavoriteDao
import com.xraiassistant.data.local.dao.RAGDao
import com.xraiassistant.data.local.entities.ConversationEntity
import com.xraiassistant.data.local.entities.FavoriteEntity
import com.xraiassistant.data.local.entities.MessageEntity
import com.xraiassistant.data.local.entities.RAGDocumentEntity
import com.xraiassistant.data.local.entities.RAGDocumentFts
import com.xraiassistant.data.local.entities.RAGEmbeddingEntity
import com.xraiassistant.data.local.entities.RAGTypeConverters

/**
 * Room Database for XRAiAssistant
 *
 * Stores chat conversations and messages with full history support
 *
 * @version 1 - Initial database schema
 * @version 2 - Added threading support (threadParentId, isWelcomeMessage)
 * @version 3 - Added RAG tables (rag_documents, rag_embeddings, rag_documents_fts)
 * @version 4 - Added screenshotBase64 column to conversations for scene thumbnails
 * @version 5 - Added favorites table for bookmarked code snippets
 * @version 6 - Fixed favorites table index names (idx_ -> index_ prefix)
 */
@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        RAGDocumentEntity::class,
        RAGEmbeddingEntity::class,
        RAGDocumentFts::class,
        FavoriteEntity::class
    ],
    version = 6,  // UPDATED: v5 -> v6 for favorites index name fix
    exportSchema = false // Set to false for development - enable with schemaLocation in production
)
@TypeConverters(RAGTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {

    /**
     * Provides access to conversation and message operations
     */
    abstract fun conversationDao(): ConversationDao

    /**
     * Provides access to RAG document and embedding operations
     */
    abstract fun ragDao(): RAGDao

    /**
     * Provides access to favorites operations
     */
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        const val DATABASE_NAME = "xraiassistant_db"
    }
}
