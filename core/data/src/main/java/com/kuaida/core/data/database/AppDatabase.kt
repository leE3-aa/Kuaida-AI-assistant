package com.kuaida.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        ProviderCredentialEntity::class,
        ChatSessionEntity::class,
        KnowledgeDocumentEntity::class,
        KnowledgeChunkEntity::class,
        ChunkEmbeddingEntity::class,
        CostRecordEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
@TypeConverters(AppTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun providerCredentialDao(): ProviderCredentialDao
    abstract fun chatSessionDao(): ChatSessionDao
    abstract fun knowledgeDocumentDao(): KnowledgeDocumentDao
    abstract fun knowledgeChunkDao(): KnowledgeChunkDao
    abstract fun chunkEmbeddingDao(): ChunkEmbeddingDao
    abstract fun costRecordDao(): CostRecordDao
}
