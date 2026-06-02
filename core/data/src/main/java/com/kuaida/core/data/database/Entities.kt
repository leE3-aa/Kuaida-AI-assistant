package com.kuaida.core.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.kuaida.core.model.AnswerBlock
import com.kuaida.core.model.AnswerStylePreset
import com.kuaida.core.model.CostRecordCategory
import com.kuaida.core.model.DocumentParseStatus
import com.kuaida.core.model.ProviderType
import com.kuaida.core.model.QuestionSourceType
import com.kuaida.core.model.SessionStatus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "provider_credentials")
data class ProviderCredentialEntity(
    @PrimaryKey val provider: ProviderType,
    @ColumnInfo(name = "encrypted_key") val encryptedKey: ByteArray,
    @ColumnInfo(name = "iv") val iv: ByteArray,
    @ColumnInfo(name = "last4") val last4: String,
    @ColumnInfo(name = "is_configured") val isConfigured: Boolean,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(
    tableName = "chat_sessions",
    indices = [
        Index("created_at"),
        Index("updated_at"),
        Index("provider"),
        Index("status"),
        Index("question"),
    ],
)
data class ChatSessionEntity(
    @PrimaryKey val id: String,
    val question: String,
    val transcript: String?,
    @ColumnInfo(name = "answer_json") val answerJson: String?,
    val provider: ProviderType,
    @ColumnInfo(name = "model_id") val modelId: String,
    @ColumnInfo(name = "style_preset") val stylePreset: AnswerStylePreset,
    @ColumnInfo(name = "source_type") val sourceType: QuestionSourceType,
    val status: SessionStatus,
    @ColumnInfo(name = "latency_ms") val latencyMs: Long,
    @ColumnInfo(name = "error_message") val errorMessage: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(
    tableName = "knowledge_documents",
    indices = [
        Index("display_name"),
        Index("parse_status"),
        Index("is_enabled"),
        Index("created_at"),
    ],
)
data class KnowledgeDocumentEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "mime_type") val mimeType: String,
    @ColumnInfo(name = "source_uri") val sourceUri: String,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long,
    @ColumnInfo(name = "parse_status") val parseStatus: DocumentParseStatus,
    @ColumnInfo(name = "chunk_count") val chunkCount: Int,
    @ColumnInfo(name = "embedding_count") val embeddingCount: Int,
    @ColumnInfo(name = "failure_reason") val failureReason: String?,
    @ColumnInfo(name = "is_enabled") val isEnabled: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(
    tableName = "knowledge_chunks",
    foreignKeys = [
        ForeignKey(
            entity = KnowledgeDocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["document_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("document_id"),
        Index(value = ["document_id", "chunk_index"], unique = true),
        Index(value = ["document_id", "content_hash"]),
    ],
)
data class KnowledgeChunkEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "document_id") val documentId: String,
    @ColumnInfo(name = "chunk_index") val chunkIndex: Int,
    val content: String,
    @ColumnInfo(name = "token_count") val tokenCount: Int,
    @ColumnInfo(name = "char_count") val charCount: Int,
    @ColumnInfo(name = "content_hash") val contentHash: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Entity(
    tableName = "chunk_embeddings",
    foreignKeys = [
        ForeignKey(
            entity = KnowledgeDocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["document_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = KnowledgeChunkEntity::class,
            parentColumns = ["id"],
            childColumns = ["chunk_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("document_id"),
        Index(value = ["provider", "model_id"]),
    ],
)
data class ChunkEmbeddingEntity(
    @PrimaryKey
    @ColumnInfo(name = "chunk_id")
    val chunkId: String,
    @ColumnInfo(name = "document_id") val documentId: String,
    val provider: ProviderType,
    @ColumnInfo(name = "model_id") val modelId: String,
    val dimensions: Int,
    @ColumnInfo(name = "vector_json") val vectorJson: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Entity(
    tableName = "cost_records",
    foreignKeys = [
        ForeignKey(
            entity = ChatSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = KnowledgeDocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["document_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = KnowledgeChunkEntity::class,
            parentColumns = ["id"],
            childColumns = ["chunk_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("session_id"),
        Index("document_id"),
        Index("chunk_id"),
        Index("category"),
        Index("created_at"),
        Index(value = ["provider", "model_id"]),
    ],
)
data class CostRecordEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "session_id") val sessionId: String?,
    @ColumnInfo(name = "document_id") val documentId: String?,
    @ColumnInfo(name = "chunk_id") val chunkId: String?,
    val category: CostRecordCategory,
    val provider: ProviderType,
    @ColumnInfo(name = "model_id") val modelId: String,
    @ColumnInfo(name = "prompt_tokens") val promptTokens: Int,
    @ColumnInfo(name = "completion_tokens") val completionTokens: Int,
    @ColumnInfo(name = "total_tokens") val totalTokens: Int,
    @ColumnInfo(name = "estimated_cost_usd") val estimatedCostUsd: Double,
    @ColumnInfo(name = "latency_ms") val latencyMs: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

class AppTypeConverters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromProviderType(value: ProviderType): String = value.name

    @TypeConverter
    fun toProviderType(value: String): ProviderType = ProviderType.fromStoredValue(value)

    @TypeConverter
    fun fromStylePreset(value: AnswerStylePreset): String = value.name

    @TypeConverter
    fun toStylePreset(value: String): AnswerStylePreset = AnswerStylePreset.valueOf(value)

    @TypeConverter
    fun fromSourceType(value: QuestionSourceType): String = value.name

    @TypeConverter
    fun toSourceType(value: String): QuestionSourceType = QuestionSourceType.valueOf(value)

    @TypeConverter
    fun fromSessionStatus(value: SessionStatus): String = value.name

    @TypeConverter
    fun toSessionStatus(value: String): SessionStatus = SessionStatus.valueOf(value)

    @TypeConverter
    fun fromDocumentStatus(value: DocumentParseStatus): String = value.name

    @TypeConverter
    fun toDocumentStatus(value: String): DocumentParseStatus = DocumentParseStatus.valueOf(value)

    @TypeConverter
    fun fromCostRecordCategory(value: CostRecordCategory): String = value.name

    @TypeConverter
    fun toCostRecordCategory(value: String): CostRecordCategory = CostRecordCategory.valueOf(value)

    @TypeConverter
    fun fromAnswerBlock(value: AnswerBlock?): String? = value?.let(json::encodeToString)

    @TypeConverter
    fun toAnswerBlock(value: String?): AnswerBlock? = value?.let { json.decodeFromString(AnswerBlock.serializer(), it) }
}
