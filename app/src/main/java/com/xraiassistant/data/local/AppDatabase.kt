package com.xraiassistant.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.xraiassistant.data.local.dao.ConversationDao
import com.xraiassistant.data.local.dao.RAGDao
import com.xraiassistant.data.local.entities.ConversationEntity
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
 */
@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        RAGDocumentEntity::class,
        RAGEmbeddingEntity::class,
        RAGDocumentFts::class
    ],
    version = 4,  // UPDATED: v3 → v4 for screenshot support
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

    companion object {
        const val DATABASE_NAME = "xraiassistant_db"
    }
}
