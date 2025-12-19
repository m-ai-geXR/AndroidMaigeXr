package com.xraiassistant.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

/**
 * RAG Document Entity
 * Stores text chunks for vector search and retrieval
 */
@Entity(
    tableName = "rag_documents",
    indices = [
        Index(value = ["source_type", "source_id"]),
        Index(value = ["created_at"])
    ]
)
@TypeConverters(RAGTypeConverters::class)
data class RAGDocumentEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "source_type")
    val sourceType: String,  // "conversation", "message", "code", "documentation"

    @ColumnInfo(name = "source_id")
    val sourceId: String,    // conversation ID or message ID

    @ColumnInfo(name = "chunk_text")
    val chunkText: String,

    @ColumnInfo(name = "chunk_index")
    val chunkIndex: Int,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "metadata")
    val metadata: String? = null  // JSON string
)

/**
 * RAG Embedding Entity
 * Stores 768-dimensional embeddings as binary data
 */
@Entity(
    tableName = "rag_embeddings",
    foreignKeys = [
        ForeignKey(
            entity = RAGDocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["document_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["document_id"])
    ]
)
@TypeConverters(RAGTypeConverters::class)
data class RAGEmbeddingEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "document_id")
    val documentId: String,

    @ColumnInfo(name = "embedding", typeAffinity = ColumnInfo.BLOB)
    val embedding: FloatArray,  // 768 dimensions

    @ColumnInfo(name = "embedding_model")
    val embeddingModel: String = "togethercomputer/m2-bert-80M-8k-retrieval",

    @ColumnInfo(name = "dimension")
    val dimension: Int = 768,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RAGEmbeddingEntity

        if (id != other.id) return false
        if (documentId != other.documentId) return false
        if (!embedding.contentEquals(other.embedding)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + documentId.hashCode()
        result = 31 * result + embedding.contentHashCode()
        return result
    }
}

/**
 * FTS4 Full-Text Search Entity for RAG Documents
 * Enables fast keyword-based search on chunk text
 */
@Fts4(contentEntity = RAGDocumentEntity::class)
@Entity(tableName = "rag_documents_fts")
data class RAGDocumentFts(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowId: Int,

    @ColumnInfo(name = "chunk_text")
    val chunkText: String
)

/**
 * Combined entity for queries that need both document and embedding
 */
data class RAGDocumentWithEmbedding(
    @androidx.room.Embedded
    val document: RAGDocumentEntity,

    @ColumnInfo(name = "embedding")
    val embedding: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RAGDocumentWithEmbedding

        if (document != other.document) return false
        if (!embedding.contentEquals(other.embedding)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = document.hashCode()
        result = 31 * result + embedding.contentHashCode()
        return result
    }
}

/**
 * Type converters for Room database
 */
class RAGTypeConverters {
    /**
     * Convert FloatArray to ByteArray for BLOB storage
     */
    @TypeConverter
    fun fromFloatArray(value: FloatArray): ByteArray {
        val byteArray = ByteArray(value.size * 4)
        for (i in value.indices) {
            val bits = java.lang.Float.floatToIntBits(value[i])
            byteArray[i * 4] = (bits shr 24).toByte()
            byteArray[i * 4 + 1] = (bits shr 16).toByte()
            byteArray[i * 4 + 2] = (bits shr 8).toByte()
            byteArray[i * 4 + 3] = bits.toByte()
        }
        return byteArray
    }

    /**
     * Convert ByteArray from BLOB storage to FloatArray
     */
    @TypeConverter
    fun toFloatArray(value: ByteArray): FloatArray {
        val floatArray = FloatArray(value.size / 4)
        for (i in floatArray.indices) {
            val bits = ((value[i * 4].toInt() and 0xFF) shl 24) or
                       ((value[i * 4 + 1].toInt() and 0xFF) shl 16) or
                       ((value[i * 4 + 2].toInt() and 0xFF) shl 8) or
                       (value[i * 4 + 3].toInt() and 0xFF)
            floatArray[i] = java.lang.Float.intBitsToFloat(bits)
        }
        return floatArray
    }
}
