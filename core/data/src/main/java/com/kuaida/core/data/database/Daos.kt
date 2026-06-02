package com.kuaida.core.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.kuaida.core.model.DocumentParseStatus
import com.kuaida.core.model.ProviderType
import kotlinx.coroutines.flow.Flow

@Dao
interface ProviderCredentialDao {
    @Query("SELECT * FROM provider_credentials")
    fun observeAll(): Flow<List<ProviderCredentialEntity>>

    @Query("SELECT * FROM provider_credentials WHERE provider = :provider LIMIT 1")
    suspend fun get(provider: ProviderType): ProviderCredentialEntity?

    @Upsert
    suspend fun upsert(entity: ProviderCredentialEntity)

    @Query(
        """
        UPDATE provider_credentials
        SET is_configured = :isConfigured,
            updated_at = :updatedAt
        WHERE provider = :provider
        """
    )
    suspend fun updateConfiguredStatus(provider: ProviderType, isConfigured: Boolean, updatedAt: Long)

    @Query("DELETE FROM provider_credentials WHERE provider = :provider")
    suspend fun delete(provider: ProviderType)
}

@Dao
interface ChatSessionDao {
    @Transaction
    @Query(
        """
        SELECT * FROM chat_sessions
        WHERE (
            :query = ''
            OR question LIKE '%' || :query || '%'
            OR IFNULL(transcript, '') LIKE '%' || :query || '%'
            OR IFNULL(answer_json, '') LIKE '%' || :query || '%'
        )
        ORDER BY created_at DESC
        """
    )
    fun observeSessions(query: String): Flow<List<ChatSessionWithCosts>>

    @Transaction
    @Query("SELECT * FROM chat_sessions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ChatSessionWithCosts?

    @Upsert
    suspend fun upsert(entity: ChatSessionEntity)

    @Query("DELETE FROM chat_sessions WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface KnowledgeDocumentDao {
    @Query(
        """
        SELECT * FROM knowledge_documents
        WHERE (:query = '' OR display_name LIKE '%' || :query || '%')
        ORDER BY created_at DESC
        """
    )
    fun observeDocuments(query: String): Flow<List<KnowledgeDocumentEntity>>

    @Query("SELECT * FROM knowledge_documents WHERE id = :id LIMIT 1")
    suspend fun get(id: String): KnowledgeDocumentEntity?

    @Transaction
    @Query("SELECT * FROM knowledge_documents WHERE id = :id LIMIT 1")
    suspend fun getWithChunks(id: String): KnowledgeDocumentWithChunks?

    @Query(
        """
        SELECT * FROM knowledge_documents
        WHERE is_enabled = 1 AND parse_status = 'READY'
        ORDER BY updated_at DESC
        """
    )
    suspend fun getEnabledDocuments(): List<KnowledgeDocumentEntity>

    @Upsert
    suspend fun upsert(entity: KnowledgeDocumentEntity)

    @Query("UPDATE knowledge_documents SET is_enabled = :enabled, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateEnabled(id: String, enabled: Boolean, updatedAt: Long)

    @Query(
        """
        UPDATE knowledge_documents
        SET parse_status = :status,
            chunk_count = :chunkCount,
            embedding_count = :embeddingCount,
            failure_reason = :failureReason,
            updated_at = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun updateProcessingResult(
        id: String,
        status: DocumentParseStatus,
        chunkCount: Int,
        embeddingCount: Int,
        failureReason: String?,
        updatedAt: Long,
    )

    @Query("DELETE FROM knowledge_documents WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface KnowledgeChunkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<KnowledgeChunkEntity>)

    @Query("SELECT * FROM knowledge_chunks WHERE document_id IN (:documentIds) ORDER BY document_id, chunk_index")
    suspend fun findByDocumentIds(documentIds: List<String>): List<KnowledgeChunkEntity>

    @Transaction
    @Query("SELECT * FROM knowledge_chunks WHERE document_id = :documentId ORDER BY chunk_index")
    suspend fun findWithEmbeddingsByDocumentId(documentId: String): List<KnowledgeChunkWithEmbedding>

    @Query(
        """
        SELECT
            kc.id AS chunk_id,
            kc.document_id AS document_id,
            kd.display_name AS document_name,
            kc.chunk_index AS chunk_index,
            kc.content AS content,
            kc.token_count AS token_count,
            kc.content_hash AS content_hash,
            ce.provider AS embedding_provider,
            ce.model_id AS embedding_model_id,
            ce.vector_json AS embedding_json
        FROM knowledge_chunks kc
        INNER JOIN knowledge_documents kd ON kd.id = kc.document_id
        LEFT JOIN chunk_embeddings ce ON ce.chunk_id = kc.id
        WHERE kc.document_id IN (:documentIds)
        ORDER BY kc.document_id, kc.chunk_index
        """
    )
    suspend fun findSearchRowsByDocumentIds(documentIds: List<String>): List<KnowledgeChunkSearchRow>

    @Query("DELETE FROM knowledge_chunks WHERE document_id = :documentId")
    suspend fun deleteByDocumentId(documentId: String)
}

@Dao
interface ChunkEmbeddingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ChunkEmbeddingEntity>)

    @Query("SELECT * FROM chunk_embeddings WHERE document_id = :documentId ORDER BY chunk_id")
    suspend fun findByDocumentId(documentId: String): List<ChunkEmbeddingEntity>

    @Query("SELECT * FROM chunk_embeddings WHERE chunk_id IN (:chunkIds)")
    suspend fun findByChunkIds(chunkIds: List<String>): List<ChunkEmbeddingEntity>

    @Query("DELETE FROM chunk_embeddings WHERE document_id = :documentId")
    suspend fun deleteByDocumentId(documentId: String)
}

@Dao
interface CostRecordDao {
    @Query("SELECT * FROM cost_records WHERE session_id = :sessionId ORDER BY created_at")
    fun observeBySessionId(sessionId: String): Flow<List<CostRecordEntity>>

    @Query("SELECT * FROM cost_records WHERE session_id = :sessionId ORDER BY created_at")
    suspend fun findBySessionId(sessionId: String): List<CostRecordEntity>

    @Query("SELECT * FROM cost_records WHERE document_id = :documentId ORDER BY created_at")
    fun observeByDocumentId(documentId: String): Flow<List<CostRecordEntity>>

    @Query("SELECT * FROM cost_records WHERE document_id = :documentId ORDER BY created_at")
    suspend fun findByDocumentId(documentId: String): List<CostRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CostRecordEntity>)

    @Query("SELECT COALESCE(SUM(total_tokens), 0) FROM cost_records")
    fun observeTotalTokens(): Flow<Int>

    @Query("SELECT COALESCE(SUM(estimated_cost_usd), 0.0) FROM cost_records")
    fun observeTotalCost(): Flow<Double>

    @Query("DELETE FROM cost_records WHERE session_id = :sessionId")
    suspend fun deleteBySessionId(sessionId: String)

    @Query("DELETE FROM cost_records WHERE document_id = :documentId")
    suspend fun deleteByDocumentId(documentId: String)
}
