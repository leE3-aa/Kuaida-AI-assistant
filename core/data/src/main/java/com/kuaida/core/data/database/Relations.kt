package com.kuaida.core.data.database

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Relation
import com.kuaida.core.model.ProviderType

data class ChatSessionWithCosts(
    @Embedded val session: ChatSessionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "session_id",
    )
    val costs: List<CostRecordEntity>,
)

data class KnowledgeChunkWithEmbedding(
    @Embedded val chunk: KnowledgeChunkEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "chunk_id",
    )
    val embedding: ChunkEmbeddingEntity?,
)

data class KnowledgeDocumentWithChunks(
    @Embedded val document: KnowledgeDocumentEntity,
    @Relation(
        entity = KnowledgeChunkEntity::class,
        parentColumn = "id",
        entityColumn = "document_id",
    )
    val chunks: List<KnowledgeChunkWithEmbedding>,
)

data class KnowledgeChunkSearchRow(
    @ColumnInfo(name = "chunk_id") val chunkId: String,
    @ColumnInfo(name = "document_id") val documentId: String,
    @ColumnInfo(name = "document_name") val documentName: String,
    @ColumnInfo(name = "chunk_index") val chunkIndex: Int,
    val content: String,
    @ColumnInfo(name = "token_count") val tokenCount: Int,
    @ColumnInfo(name = "content_hash") val contentHash: String,
    @ColumnInfo(name = "embedding_provider") val embeddingProvider: ProviderType?,
    @ColumnInfo(name = "embedding_model_id") val embeddingModelId: String?,
    @ColumnInfo(name = "embedding_json") val embeddingJson: String?,
)
