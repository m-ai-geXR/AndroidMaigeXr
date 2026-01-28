package com.xraiassistant.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Database Migration: Version 4 -> Version 5
 *
 * Adds the favorites table for storing bookmarked code snippets from AI responses.
 * Matches iOS implementation in DatabaseManager.swift (migrations v4_favorites_table
 * and v5_favorites_no_fk).
 *
 * Changes:
 * - CREATE TABLE favorites with columns: id, messageId, conversationId, title,
 *   codeContent, libraryId, modelUsed, screenshotBase64, createdAt, favoriteOrder, tags
 * - CREATE INDEX on messageId for quick lookups
 * - CREATE INDEX on createdAt for sorted queries
 *
 * Design Notes:
 * - No foreign key constraints (orphaned favorites are allowed - matching iOS behavior)
 * - Screenshots stored as base64-encoded JPEG for visual previews
 * - Tags stored as comma-separated string for simplicity
 *
 * Impact:
 * - New table created (no impact on existing data)
 * - Users can now favorite individual AI code responses
 * - Favorites persist across conversation deletions
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        println("----------------------------------------")
        println("[Database Migration] Starting v4 -> v5")
        println("----------------------------------------")

        // Create the favorites table
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS favorites (
                id TEXT PRIMARY KEY NOT NULL,
                messageId TEXT NOT NULL,
                conversationId TEXT NOT NULL,
                title TEXT NOT NULL,
                codeContent TEXT NOT NULL,
                libraryId TEXT,
                modelUsed TEXT,
                screenshotBase64 TEXT,
                createdAt INTEGER NOT NULL,
                favoriteOrder INTEGER,
                tags TEXT
            )
            """.trimIndent()
        )
        println("Created favorites table")

        // Create index on messageId for quick favorite lookups
        // NOTE: Index name must match Room's default naming convention: index_{tableName}_{columnName}
        database.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_favorites_messageId
            ON favorites(messageId)
            """.trimIndent()
        )
        println("Created index on messageId")

        // Create index on createdAt for sorted queries
        // NOTE: Index name must match Room's default naming convention: index_{tableName}_{columnName}
        database.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_favorites_createdAt
            ON favorites(createdAt)
            """.trimIndent()
        )
        println("Created index on createdAt")

        println("----------------------------------------")
        println("Migration 4->5 completed successfully!")
        println("Favorites table is now available")
        println("----------------------------------------")
    }
}
