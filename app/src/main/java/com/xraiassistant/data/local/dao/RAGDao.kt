package com.xraiassistant.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.xraiassistant.data.local.entities.RAGDocumentEntity
import com.xraiassistant.data.local.entities.RAGDocumentWithEmbedding
import com.xraiassistant.data.local.entities.RAGEmbeddingEntity

/**
 * RAG Data Access Object
 * Handles all database operations for RAG documents and embeddings
 */
@Dao
interface RAGDao {

    // MARK: - Insert Operations

    /**
     * Insert or replace RAG document
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: RAGDocumentEntity)

    /**
     * Insert or replace RAG embedding
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmbedding(embedding: RAGEmbeddingEntity)

    /**
     * Insert document and embedding together
     */
    @Transaction
    suspend fun insertDocumentWithEmbedding(
        document: RAGDocumentEntity,
        embedding: RAGEmbeddingEntity
    ) {
        insertDocument(document)
        insertEmbedding(embedding)
    }

    /**
     * Batch insert documents
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocuments(documents: List<RAGDocumentEntity>)

    /**
     * Batch insert embeddings
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmbeddings(embeddings: List<RAGEmbeddingEntity>)

    // MARK: - Full-Text Search (FTS4)

    /**
     * Full-text search on RAG documents using FTS4
     * Returns documents ranked by relevance
     */
    @Query("""
        SELECT d.* FROM rag_documents d
        JOIN rag_documents_fts fts ON d.id = fts.rowid
        WHERE rag_documents_fts MATCH :query
        LIMIT :limit
    """)
    suspend fun fullTextSearch(query: String, limit: Int = 50): List<RAGDocumentEntity>

    /**
     * Full-text search with source type filter
     */
    @Query("""
        SELECT d.* FROM rag_documents d
        JOIN rag_documents_fts fts ON d.id = fts.rowid
        WHERE rag_documents_fts MATCH :query
        AND d.source_type = :sourceType
        LIMIT :limit
    """)
    suspend fun fullTextSearchByType(
        query: String,
        sourceType: String,
        limit: Int = 50
    ): List<RAGDocumentEntity>

    // MARK: - Vector Search Queries

    /**
     * Load all documents with their embeddings
     * Used for in-memory vector similarity search
     */
    @Query("""
        SELECT d.*, e.embedding
        FROM rag_documents d
        JOIN rag_embeddings e ON d.id = e.document_id
    """)
    suspend fun loadAllEmbeddings(): List<RAGDocumentWithEmbedding>

    /**
     * Load embeddings filtered by source type
     */
    @Query("""
        SELECT d.*, e.embedding
        FROM rag_documents d
        JOIN rag_embeddings e ON d.id = e.document_id
        WHERE d.source_type = :sourceType
    """)
    suspend fun loadEmbeddingsByType(sourceType: String): List<RAGDocumentWithEmbedding>

    /**
     * Load single embedding for a document
     */
    @Query("SELECT embedding FROM rag_embeddings WHERE document_id = :documentId")
    suspend fun loadEmbedding(documentId: String): FloatArray?

    /**
     * Load embeddings for a specific source (e.g., conversation)
     */
    @Query("""
        SELECT e.embedding
        FROM rag_embeddings e
        JOIN rag_documents d ON e.document_id = d.id
        WHERE d.source_id = :sourceId AND d.source_type = :sourceType
    """)
    suspend fun loadEmbeddingsForSource(
        sourceId: String,
        sourceType: String = "conversation"
    ): List<FloatArray>

    // MARK: - Document Queries

    /**
     * Get document by ID
     */
    @Query("SELECT * FROM rag_documents WHERE id = :documentId")
    suspend fun getDocumentById(documentId: String): RAGDocumentEntity?

    /**
     * Get documents by source
     */
    @Query("""
        SELECT * FROM rag_documents
        WHERE source_type = :sourceType AND source_id = :sourceId
        ORDER BY chunk_index ASC
    """)
    suspend fun getDocumentsBySource(
        sourceType: String,
        sourceId: String
    ): List<RAGDocumentEntity>

    /**
     * Check if document exists
     */
    @Query("SELECT EXISTS(SELECT 1 FROM rag_documents WHERE id = :documentId)")
    suspend fun documentExists(documentId: String): Boolean

    /**
     * Check if source has been indexed
     */
    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM rag_documents
            WHERE source_type = :sourceType AND source_id = :sourceId
        )
    """)
    suspend fun isSourceIndexed(sourceType: String, sourceId: String): Boolean

    // MARK: - Stats & Maintenance

    /**
     * Get total number of indexed documents
     */
    @Query("SELECT COUNT(*) FROM rag_documents")
    suspend fun getDocumentCount(): Int

    /**
     * Get total number of embeddings
     */
    @Query("SELECT COUNT(*) FROM rag_embeddings")
    suspend fun getEmbeddingCount(): Int

    /**
     * Get count by source type
     */
    @Query("SELECT COUNT(*) FROM rag_documents WHERE source_type = :sourceType")
    suspend fun getDocumentCountByType(sourceType: String): Int

    /**
     * Get all indexed source IDs
     * Helper to check which conversations/messages have been indexed
     */
    @Query("""
        SELECT DISTINCT source_id FROM rag_documents
        WHERE source_type = :sourceType
    """)
    suspend fun getIndexedSourceIds(sourceType: String = "conversation"): List<String>

    // MARK: - Delete Operations

    /**
     * Delete document (cascade deletes embedding via foreign key)
     */
    @Query("DELETE FROM rag_documents WHERE id = :documentId")
    suspend fun deleteDocument(documentId: String)

    /**
     * Delete all documents for a source
     */
    @Query("""
        DELETE FROM rag_documents
        WHERE source_type = :sourceType AND source_id = :sourceId
    """)
    suspend fun deleteDocumentsBySource(sourceType: String, sourceId: String)

    /**
     * Delete all RAG data (for testing/reset)
     */
    @Transaction
    suspend fun deleteAllRAGData() {
        clearAllEmbeddings()
        clearAllDocuments()
    }

    @Query("DELETE FROM rag_embeddings")
    suspend fun clearAllEmbeddings()

    @Query("DELETE FROM rag_documents")
    suspend fun clearAllDocuments()

    // MARK: - Maintenance Queries

    /**
     * Get oldest documents (for cleanup/archival)
     */
    @Query("""
        SELECT * FROM rag_documents
        ORDER BY created_at ASC
        LIMIT :limit
    """)
    suspend fun getOldestDocuments(limit: Int): List<RAGDocumentEntity>

    /**
     * Delete documents older than timestamp
     */
    @Query("DELETE FROM rag_documents WHERE created_at < :timestamp")
    suspend fun deleteDocumentsOlderThan(timestamp: Long): Int
}
