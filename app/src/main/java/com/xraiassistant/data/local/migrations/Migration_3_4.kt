package com.xraiassistant.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Database Migration: Version 3 → Version 4
 *
 * Adds screenshotBase64 column to conversations table for storing
 * scene preview thumbnails captured from WebView canvas.
 *
 * Changes:
 * - ALTER TABLE conversations ADD COLUMN screenshotBase64 TEXT DEFAULT NULL
 *
 * Impact:
 * - Existing conversations will have NULL screenshot (backward compatible)
 * - New conversations can optionally store base64-encoded JPEG screenshots
 * - Screenshot capture happens 5 seconds after 3D scene code injection
 *
 * Storage Format:
 * - Base64-encoded JPEG at 70% quality
 * - Average size: ~70-110KB per screenshot
 * - 100 conversations ≈ 7-11MB database size
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("📦 [Database Migration] Starting v3 → v4")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        // Add screenshot column (nullable, default NULL for existing rows)
        database.execSQL(
            """
            ALTER TABLE conversations
            ADD COLUMN screenshotBase64 TEXT DEFAULT NULL
            """.trimIndent()
        )

        println("✅ Added screenshotBase64 column to conversations table")
        println("📊 Existing conversations: Screenshot will be NULL (backward compatible)")
        println("🆕 New conversations: Can store base64-encoded scene screenshots")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("✅ Migration 3→4 completed successfully!")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }
}
