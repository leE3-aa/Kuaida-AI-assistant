package com.kuaida.core.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.room.withTransaction
import com.kuaida.core.common.error.toUserMessage
import com.kuaida.core.common.di.IoDispatcher
import com.kuaida.core.data.database.AppDatabase
import com.kuaida.core.data.database.ChunkEmbeddingDao
import com.kuaida.core.data.database.ChunkEmbeddingEntity
import com.kuaida.core.data.database.CostRecordDao
import com.kuaida.core.data.database.CostRecordEntity
import com.kuaida.core.data.database.KnowledgeChunkDao
import com.kuaida.core.data.database.KnowledgeChunkEntity
import com.kuaida.core.data.database.KnowledgeDocumentDao
import com.kuaida.core.data.database.KnowledgeDocumentEntity
import com.kuaida.core.data.database.toDomain
import com.kuaida.core.data.provider.findProviderServiceException
import com.kuaida.core.data.provider.findConfiguredEmbeddingProvider
import com.kuaida.core.data.provider.ProviderRegistry
import com.kuaida.core.data.rag.Chunker
import com.kuaida.core.data.rag.DocumentTextExtractor
import com.kuaida.core.model.CostRecord
import com.kuaida.core.model.CostRecordCategory
import com.kuaida.core.model.DocumentParseStatus
import com.kuaida.core.model.EmbeddingRequest
import com.kuaida.core.model.KnowledgeChunk
import com.kuaida.core.model.KnowledgeDocument
import com.kuaida.core.model.KnowledgeImportResult
import com.kuaida.core.model.ProviderType
import com.kuaida.core.model.UsageCost
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

@Singleton
class KnowledgeRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val documentDao: KnowledgeDocumentDao,
    private val chunkDao: KnowledgeChunkDao,
    private val embeddingDao: ChunkEmbeddingDao,
    private val costRecordDao: CostRecordDao,
    private val extractor: DocumentTextExtractor,
    private val chunker: Chunker,
    private val settingsRepository: SettingsRepository,
    private val providerRegistry: ProviderRegistry,
    private val json: Json,
    @IoDispatcher
    private val ioDispatcher: CoroutineDispatcher,
) {
    fun observeDocuments(query: String): Flow<List<KnowledgeDocument>> {
        return documentDao.observeDocuments(query).map { items -> items.map { it.toDomain() } }
    }

    fun observeDocumentCosts(documentId: String): Flow<List<CostRecord>> {
        return costRecordDao.observeByDocumentId(documentId).map { items -> items.map { it.toDomain() } }
    }

    suspend fun getDocumentCosts(documentId: String): List<CostRecord> {
        return costRecordDao.findByDocumentId(documentId).map { it.toDomain() }
    }

    suspend fun getChunks(documentId: String): List<KnowledgeChunk> {
        return chunkDao.findWithEmbeddingsByDocumentId(documentId).map { it.toDomain(json) }
    }

    suspend fun importDocument(uri: Uri): KnowledgeImportResult = withContext(ioDispatcher) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }

        val extracted = extractor.extract(uri, context.contentResolver.getType(uri).orEmpty())
        val now = System.currentTimeMillis()
        val documentId = "doc_$now"
        val baseDocument = KnowledgeDocumentEntity(
            id = documentId,
            displayName = extracted.name,
            mimeType = extracted.mimeType,
            sourceUri = uri.toString(),
            sizeBytes = extracted.sizeBytes,
            parseStatus = DocumentParseStatus.PROCESSING,
            chunkCount = 0,
            embeddingCount = 0,
            failureReason = null,
            isEnabled = true,
            createdAt = now,
            updatedAt = now,
        )

        documentDao.upsert(baseDocument)

        return@withContext runCatching {
            val chunked = chunker.chunk(extracted.text)
            require(chunked.isNotEmpty()) { "未能从文件中切分出可用内容。" }
            val chunkEntities = chunked.map { chunk ->
                KnowledgeChunkEntity(
                    id = "${documentId}_${chunk.index}",
                    documentId = documentId,
                    chunkIndex = chunk.index,
                    content = chunk.content,
                    tokenCount = chunk.tokenCount,
                    charCount = chunk.content.length,
                    contentHash = chunk.hash,
                    createdAt = now,
                )
            }
            val embeddingBatch = buildEmbeddings(chunkEntities)
            val embeddingEntities = embeddingBatch.builds.map { build ->
                ChunkEmbeddingEntity(
                    chunkId = build.chunkId,
                    documentId = documentId,
                    provider = build.provider,
                    modelId = build.modelId,
                    dimensions = build.vector.size,
                    vectorJson = json.encodeToString(ListSerializer(Float.serializer()), build.vector),
                    createdAt = build.createdAt,
                )
            }
            val costEntities = embeddingBatch.builds.map { build ->
                CostRecordEntity(
                    id = "cost_${build.chunkId}_embedding",
                    sessionId = null,
                    documentId = documentId,
                    chunkId = build.chunkId,
                    category = CostRecordCategory.KNOWLEDGE_EMBEDDING,
                    provider = build.provider,
                    modelId = build.modelId,
                    promptTokens = build.usage.promptTokens,
                    completionTokens = build.usage.completionTokens,
                    totalTokens = build.usage.totalTokens,
                    estimatedCostUsd = build.usage.estimatedCostUsd,
                    latencyMs = build.latencyMs,
                    createdAt = build.createdAt,
                )
            }
            val finishedAt = System.currentTimeMillis()

            database.withTransaction {
                chunkDao.insertAll(chunkEntities)
                if (embeddingEntities.isNotEmpty()) {
                    embeddingDao.insertAll(embeddingEntities)
                }
                if (costEntities.isNotEmpty()) {
                    costRecordDao.insertAll(costEntities)
                }
                documentDao.updateProcessingResult(
                    id = documentId,
                    status = DocumentParseStatus.READY,
                    chunkCount = chunkEntities.size,
                    embeddingCount = embeddingEntities.size,
                    failureReason = null,
                    updatedAt = finishedAt,
                )
            }
            KnowledgeImportResult(
                documentId = documentId,
                displayName = extracted.name,
                chunkCount = chunkEntities.size,
                embeddingCount = embeddingEntities.size,
                embeddingProvider = embeddingBatch.provider,
                embeddingModelId = embeddingBatch.modelId,
                warningMessage = embeddingBatch.warningMessage,
            )
        }.onFailure { throwable ->
            documentDao.updateProcessingResult(
                id = documentId,
                status = DocumentParseStatus.FAILED,
                chunkCount = 0,
                embeddingCount = 0,
                failureReason = throwable.toUserMessage("文档导入失败，请稍后重试。"),
                updatedAt = System.currentTimeMillis(),
            )
            throw throwable
        }.getOrThrow()
    }

    suspend fun updateEnabled(id: String, enabled: Boolean) = withContext(ioDispatcher) {
        documentDao.updateEnabled(id, enabled, System.currentTimeMillis())
    }

    suspend fun delete(id: String) = withContext(ioDispatcher) {
        val document = documentDao.get(id)
        documentDao.delete(id)
        document?.sourceUri?.let { uriString ->
            runCatching {
                context.contentResolver.releasePersistableUriPermission(
                    Uri.parse(uriString),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
        }
    }

    private suspend fun buildEmbeddings(
        chunks: List<KnowledgeChunkEntity>,
    ): EmbeddingBatch {
        if (chunks.isEmpty()) return EmbeddingBatch()

        val selectedModel = settingsRepository.currentEmbeddingModel()
        val configuredProvider = providerRegistry.findConfiguredEmbeddingProvider(
            preferredType = selectedModel.provider,
            apiKeyLookup = settingsRepository::getApiKeyOrNull,
        ) ?: return EmbeddingBatch(
            warningMessage = "当前没有可用的 Embedding Provider，已导入文本并回退为关键词检索。",
        )

        val resolvedModel = if (configuredProvider.provider.type == selectedModel.provider) {
            selectedModel
        } else {
            com.kuaida.core.data.provider.ModelCatalog.defaultEmbeddingModel(configuredProvider.provider.type)
        }
        val modelId = resolvedModel.modelId
        if (modelId.isBlank()) {
            return EmbeddingBatch(
                warningMessage = "当前未填写知识库向量模型，已导入文本并回退为关键词检索。",
            )
        }
        val builds = chunks.map { chunk ->
            val startedAt = System.currentTimeMillis()
            runCatching {
                val result = configuredProvider.provider.embed(
                    apiKey = configuredProvider.apiKey,
                    request = EmbeddingRequest(
                        provider = configuredProvider.provider.type,
                        modelId = modelId,
                        input = chunk.content,
                    ),
                )
                require(result.embedding.isNotEmpty()) { "Embedding 结果为空。" }
                val usage = result.usage.normalized()
                EmbeddingBuild(
                    chunkId = chunk.id,
                    provider = configuredProvider.provider.type,
                    modelId = modelId,
                    vector = result.embedding,
                    usage = usage,
                    latencyMs = System.currentTimeMillis() - startedAt,
                    createdAt = System.currentTimeMillis(),
                )
            }.getOrElse { throwable ->
                if (throwable.findProviderServiceException()?.isCredentialError == true) {
                    settingsRepository.markApiKeyInvalid(configuredProvider.provider.type)
                }
                val userMessage = throwable.toUserMessage("知识库向量化失败，请稍后重试。")
                throw IllegalStateException(
                    "为文件分片 ${chunk.chunkIndex + 1} 生成向量失败：$userMessage",
                    throwable,
                )
            }
        }

        return EmbeddingBatch(
            builds = builds,
            provider = configuredProvider.provider.type,
            modelId = modelId,
        )
    }

    private fun UsageCost.normalized(): UsageCost {
        return if (totalTokens > 0 || promptTokens + completionTokens == 0) {
            this
        } else {
            copy(totalTokens = promptTokens + completionTokens)
        }
    }

    private data class EmbeddingBuild(
        val chunkId: String,
        val provider: ProviderType,
        val modelId: String,
        val vector: List<Float>,
        val usage: UsageCost,
        val latencyMs: Long,
        val createdAt: Long,
    )

    private data class EmbeddingBatch(
        val builds: List<EmbeddingBuild> = emptyList(),
        val provider: ProviderType? = null,
        val modelId: String? = null,
        val warningMessage: String? = null,
    )
}
