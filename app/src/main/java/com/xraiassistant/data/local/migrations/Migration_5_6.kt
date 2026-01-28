package com.xraiassistant.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Database Migration: Version 5 -> Version 6
 *
 * Fixes incorrect index names on the favorites table created in Migration 4->5.
 *
 * Problem: Migration 4->5 created indices with "idx_" prefix:
 *   - idx_favorites_messageId
 *   - idx_favorites_createdAt
 *
 * But Room expects indices with "index_" prefix (default naming convention):
 *   - index_favorites_messageId
 *   - index_favorites_createdAt
 *
 * This migration renames the indices to match Room's expected schema.
 *
 * Changes:
 * - DROP INDEX idx_favorites_messageId
 * - DROP INDEX idx_favorites_createdAt
 * - CREATE INDEX index_favorites_messageId
 * - CREATE INDEX index_favorites_createdAt
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        println("----------------------------------------")
        println("[Database Migration] Starting v5 -> v6")
        println("----------------------------------------")
        println("Fixing favorites table index names...")

        // Drop the incorrectly named indices (use IF EXISTS for safety)
        database.execSQL("DROP INDEX IF EXISTS idx_favorites_messageId")
        database.execSQL("DROP INDEX IF EXISTS idx_favorites_createdAt")
        println("Dropped old indices with incorrect naming")

        // Create indices with correct naming convention (Room expects index_ prefix)
        database.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_favorites_messageId
            ON favorites(messageId)
            """.trimIndent()
        )
        database.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_favorites_createdAt
            ON favorites(createdAt)
            """.trimIndent()
        )
        println("Created indices with correct naming convention")

        println("----------------------------------------")
        println("Migration 5->6 completed successfully!")
        println("Favorites table indices are now correctly named")
        println("----------------------------------------")
    }
}
