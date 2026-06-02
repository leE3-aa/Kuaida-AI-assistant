package com.kuaida.core.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `cost_records` (
                `id` TEXT NOT NULL,
                `session_id` TEXT,
                `document_id` TEXT,
                `chunk_id` TEXT,
                `category` TEXT NOT NULL,
                `provider` TEXT NOT NULL,
                `model_id` TEXT NOT NULL,
                `prompt_tokens` INTEGER NOT NULL,
                `completion_tokens` INTEGER NOT NULL,
                `total_tokens` INTEGER NOT NULL,
                `estimated_cost_usd` REAL NOT NULL,
                `latency_ms` INTEGER NOT NULL,
                `created_at` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`session_id`) REFERENCES `chat_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`document_id`) REFERENCES `knowledge_documents`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`chunk_id`) REFERENCES `knowledge_chunks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_cost_records_session_id` ON `cost_records` (`session_id`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_cost_records_document_id` ON `cost_records` (`document_id`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_cost_records_chunk_id` ON `cost_records` (`chunk_id`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_cost_records_category` ON `cost_records` (`category`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_cost_records_created_at` ON `cost_records` (`created_at`)")
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_cost_records_provider_model_id` ON `cost_records` (`provider`, `model_id`)",
        )
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        listOf(
            "provider_credentials",
            "chat_sessions",
            "chunk_embeddings",
            "cost_records",
        ).forEach { table ->
            database.execSQL("UPDATE `$table` SET `provider` = 'QWEN' WHERE `provider` = 'OPENAI'")
            database.execSQL("UPDATE `$table` SET `provider` = 'ZHIPU' WHERE `provider` = 'ANTHROPIC'")
        }
    }
}

val APP_DATABASE_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
