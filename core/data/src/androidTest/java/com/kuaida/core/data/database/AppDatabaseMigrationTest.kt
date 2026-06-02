package com.kuaida.core.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val databaseName = "migration-test.db"

    @After
    fun tearDown() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun migrate1To3_preservesExistingDataAndUpgradesLegacyProviders() {
        createVersion1Database().use { database ->
            database.execSQL(
                """
                INSERT INTO chat_sessions (
                    id, question, transcript, answer_json, provider, model_id, style_preset,
                    source_type, status, latency_ms, error_message, created_at, updated_at
                ) VALUES (
                    'session_1', '如何推进项目', NULL, NULL, 'OPENAI', 'gpt-4o', 'DETAILED',
                    'TEXT', 'COMPLETED', 800, NULL, 1, 2
                )
                """.trimIndent(),
            )
        }

        val migrated = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
            .addMigrations(*APP_DATABASE_MIGRATIONS)
            .build()

        migrated.openHelper.writableDatabase.query("SELECT name FROM sqlite_master WHERE type='table' AND name='cost_records'")
            .use { cursor ->
                cursor.moveToFirst()
                assertEquals("cost_records", cursor.getString(0))
            }

        val session = runBlocking { migrated.chatSessionDao().getById("session_1") }
        assertNotNull(session)
        assertEquals("QWEN", session?.provider?.name)

        migrated.close()
    }

    private fun createVersion1Database(): SupportSQLiteDatabase {
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(databaseName)
            .callback(
                object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        createVersion1Schema(db)
                    }

                    override fun onUpgrade(
                        db: SupportSQLiteDatabase,
                        oldVersion: Int,
                        newVersion: Int,
                    ) = Unit
                },
            )
            .build()

        return FrameworkSQLiteOpenHelperFactory()
            .create(configuration)
            .writableDatabase
    }

    private fun createVersion1Schema(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `provider_credentials` (
                `provider` TEXT NOT NULL,
                `encrypted_key` BLOB NOT NULL,
                `iv` BLOB NOT NULL,
                `last4` TEXT NOT NULL,
                `is_configured` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                PRIMARY KEY(`provider`)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `chat_sessions` (
                `id` TEXT NOT NULL,
                `question` TEXT NOT NULL,
                `transcript` TEXT,
                `answer_json` TEXT,
                `provider` TEXT NOT NULL,
                `model_id` TEXT NOT NULL,
                `style_preset` TEXT NOT NULL,
                `source_type` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                `latency_ms` INTEGER NOT NULL,
                `error_message` TEXT,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_chat_sessions_created_at` ON `chat_sessions` (`created_at`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_chat_sessions_updated_at` ON `chat_sessions` (`updated_at`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_chat_sessions_provider` ON `chat_sessions` (`provider`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_chat_sessions_status` ON `chat_sessions` (`status`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_chat_sessions_question` ON `chat_sessions` (`question`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `knowledge_documents` (
                `id` TEXT NOT NULL,
                `display_name` TEXT NOT NULL,
                `mime_type` TEXT NOT NULL,
                `source_uri` TEXT NOT NULL,
                `size_bytes` INTEGER NOT NULL,
                `parse_status` TEXT NOT NULL,
                `chunk_count` INTEGER NOT NULL,
                `embedding_count` INTEGER NOT NULL,
                `failure_reason` TEXT,
                `is_enabled` INTEGER NOT NULL,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_knowledge_documents_display_name` ON `knowledge_documents` (`display_name`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_knowledge_documents_parse_status` ON `knowledge_documents` (`parse_status`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_knowledge_documents_is_enabled` ON `knowledge_documents` (`is_enabled`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_knowledge_documents_created_at` ON `knowledge_documents` (`created_at`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `knowledge_chunks` (
                `id` TEXT NOT NULL,
                `document_id` TEXT NOT NULL,
                `chunk_index` INTEGER NOT NULL,
                `content` TEXT NOT NULL,
                `token_count` INTEGER NOT NULL,
                `char_count` INTEGER NOT NULL,
                `content_hash` TEXT NOT NULL,
                `created_at` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`document_id`) REFERENCES `knowledge_documents`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_knowledge_chunks_document_id` ON `knowledge_chunks` (`document_id`)")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_knowledge_chunks_document_id_chunk_index` ON `knowledge_chunks` (`document_id`, `chunk_index`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_knowledge_chunks_document_id_content_hash` ON `knowledge_chunks` (`document_id`, `content_hash`)",
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `chunk_embeddings` (
                `chunk_id` TEXT NOT NULL,
                `document_id` TEXT NOT NULL,
                `provider` TEXT NOT NULL,
                `model_id` TEXT NOT NULL,
                `dimensions` INTEGER NOT NULL,
                `vector_json` TEXT NOT NULL,
                `created_at` INTEGER NOT NULL,
                PRIMARY KEY(`chunk_id`),
                FOREIGN KEY(`document_id`) REFERENCES `knowledge_documents`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`chunk_id`) REFERENCES `knowledge_chunks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_chunk_embeddings_document_id` ON `chunk_embeddings` (`document_id`)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_chunk_embeddings_provider_model_id` ON `chunk_embeddings` (`provider`, `model_id`)",
        )
    }
}
