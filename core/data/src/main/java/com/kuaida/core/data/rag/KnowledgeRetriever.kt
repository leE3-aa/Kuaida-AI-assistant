package com.kuaida.core.data.rag

import com.kuaida.core.common.di.IoDispatcher
import com.kuaida.core.common.util.cosineSimilarity
import com.kuaida.core.data.database.KnowledgeChunkDao
import com.kuaida.core.data.database.KnowledgeChunkSearchRow
import com.kuaida.core.data.database.KnowledgeDocumentDao
import com.kuaida.core.data.provider.findProviderServiceException
import com.kuaida.core.data.provider.ProviderRegistry
import com.kuaida.core.data.repository.SettingsRepository
import com.kuaida.core.model.EmbeddingRequest
import com.kuaida.core.model.ProviderType
import com.kuaida.core.model.RetrievedChunk
import com.kuaida.core.model.UsageCost
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

@Singleton
class KnowledgeRetriever @Inject constructor(
    private val documentDao: KnowledgeDocumentDao,
    private val chunkDao: KnowledgeChunkDao,
    private val settingsRepository: SettingsRepository,
    private val providerRegistry: ProviderRegistry,
    private val json: Json,
    @IoDispatcher
    private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun retrieve(query: String, topK: Int = DEFAULT_TOP_K): RetrievalResult = withContext(ioDispatcher) {
        val documents = documentDao.getEnabledDocuments()
        if (documents.isEmpty()) return@withContext RetrievalResult()
        val documentIds = documents.map { it.id }
        if (documentIds.isEmpty()) return@withContext RetrievalResult()
        val chunks = chunkDao.findSearchRowsByDocumentIds(documentIds)
        if (chunks.isEmpty()) return@withContext RetrievalResult()

        val chunkEmbeddings = chunks.associate { it.chunkId to it.embedding() }
        val keywordScores = chunks.associate { it.chunkId to keywordScore(query, it.keywordSignal()) }
        val queryEmbeddings = buildQueryEmbeddings(query, chunks).associateBy { it.key }

        val retrieved = chunks.mapNotNull { chunk ->
            val keywordScore = keywordScores.getValue(chunk.chunkId)
            val embeddingScore = chunk.embeddingKey()?.let { key ->
                val queryVector = queryEmbeddings[key]?.vector ?: return@let null
                val chunkVector = chunkEmbeddings[chunk.chunkId].orEmpty()
                if (chunkVector.isEmpty()) null else cosineSimilarity(queryVector, chunkVector)
            }
            val finalScore = combineScores(embeddingScore = embeddingScore, keywordScore = keywordScore)
            if (!isRelevant(finalScore = finalScore, embeddingScore = embeddingScore, keywordScore = keywordScore)) {
                return@mapNotNull null
            }
            RetrievedChunk(
                chunkId = chunk.chunkId,
                documentId = chunk.documentId,
                documentName = chunk.documentName,
                chunkIndex = chunk.chunkIndex,
                content = chunk.content,
                tokenCount = chunk.tokenCount,
                score = finalScore,
            )
        }
            .sortedByDescending { it.score }
            .take(topK)
        RetrievalResult(
            chunks = retrieved,
            queryEmbeddingCosts = queryEmbeddings.values.map { it.toSnapshot() },
        )
    }

    private suspend fun buildQueryEmbeddings(
        query: String,
        chunks: List<KnowledgeChunkSearchRow>,
    ): List<QueryEmbeddingBuild> {
        val groups = chunks.groupBy { it.embeddingKey() }.filterKeys { it != null }
        if (groups.isEmpty()) return emptyList()

        val preferredModel = settingsRepository.currentEmbeddingModel()
        val preferredKey = EmbeddingModelKey(
            provider = preferredModel.provider,
            modelId = preferredModel.modelId,
        )
        val candidateKeys = groups.keys.filterNotNull()
            .sortedWith(
                compareByDescending<EmbeddingModelKey> { it == preferredKey }
                    .thenByDescending { key -> groups[key]?.size ?: 0 },
            )
            .take(MAX_QUERY_EMBEDDING_GROUPS)

        return candidateKeys.mapNotNull { key -> buildQueryEmbedding(query = query, key = key) }
    }

    private suspend fun buildQueryEmbedding(query: String, key: EmbeddingModelKey): QueryEmbeddingBuild? {
        val provider = providerRegistry.findEmbeddingProvider(key.provider) ?: return null
        val apiKey = settingsRepository.getApiKeyOrNull(key.provider) ?: return null
        val startedAt = System.currentTimeMillis()
        return runCatching {
            val result = provider.embed(
                apiKey = apiKey,
                request = EmbeddingRequest(
                    provider = key.provider,
                    modelId = key.modelId,
                    input = query,
                ),
            )
            val finishedAt = System.currentTimeMillis()
            QueryEmbeddingBuild(
                key = key,
                provider = key.provider,
                modelId = key.modelId,
                vector = result.embedding,
                usage = result.usage.normalized(),
                latencyMs = finishedAt - startedAt,
                createdAt = finishedAt,
            )
        }.onFailure { throwable ->
            if (throwable.findProviderServiceException()?.isCredentialError == true) {
                settingsRepository.markApiKeyInvalid(key.provider)
            }
        }.getOrNull()
    }

    private fun keywordScore(query: String, content: String): Double {
        val normalizedQuery = query.trim()
        val keywords = normalizedQuery
            .split(Regex("[^\\p{L}\\p{N}\\u4e00-\\u9fa5]+"))
            .map(String::trim)
            .filter { it.length >= 2 }
            .distinct()
        if (keywords.isEmpty()) return 0.0
        val hitRatio = keywords.count { content.contains(it, ignoreCase = true) } / keywords.size.toDouble()
        val phraseBoost = if (normalizedQuery.length >= 4 && content.contains(normalizedQuery, ignoreCase = true)) {
            DIRECT_HIT_BOOST
        } else {
            0.0
        }
        return (hitRatio + phraseBoost).coerceAtMost(1.0)
    }

    private fun combineScores(embeddingScore: Double?, keywordScore: Double): Double {
        return if (embeddingScore != null) {
            (embeddingScore * EMBEDDING_WEIGHT) + (keywordScore * KEYWORD_WEIGHT)
        } else {
            keywordScore
        }
    }

    private fun isRelevant(finalScore: Double, embeddingScore: Double?, keywordScore: Double): Boolean {
        return if (embeddingScore != null) {
            finalScore >= MIN_EMBEDDING_SCORE || keywordScore >= MIN_KEYWORD_SCORE
        } else {
            keywordScore >= MIN_KEYWORD_SCORE
        }
    }

    private fun KnowledgeChunkSearchRow.embedding(): List<Float> = embeddingJson?.let {
        runCatching { json.decodeFromString(ListSerializer(Float.serializer()), it) }.getOrDefault(emptyList())
    } ?: emptyList()

    private fun KnowledgeChunkSearchRow.embeddingKey(): EmbeddingModelKey? {
        val provider = embeddingProvider ?: return null
        val modelId = embeddingModelId ?: return null
        return EmbeddingModelKey(provider = provider, modelId = modelId)
    }

    private fun KnowledgeChunkSearchRow.keywordSignal(): String {
        return buildString(content.length + documentName.length + 8) {
            append(documentName)
            append('\n')
            append(content)
        }
    }

    private fun UsageCost.normalized(): UsageCost {
        return if (totalTokens > 0 || promptTokens + completionTokens == 0) {
            this
        } else {
            copy(totalTokens = promptTokens + completionTokens)
        }
    }

    private fun QueryEmbeddingBuild.toSnapshot(): QueryEmbeddingCostSnapshot {
        return QueryEmbeddingCostSnapshot(
            provider = provider,
            modelId = modelId,
            usage = usage,
            latencyMs = latencyMs,
            createdAt = createdAt,
        )
    }

    private data class QueryEmbeddingBuild(
        val key: EmbeddingModelKey,
        val provider: ProviderType,
        val modelId: String,
        val vector: List<Float>,
        val usage: UsageCost,
        val latencyMs: Long,
        val createdAt: Long,
    )

    private data class EmbeddingModelKey(
        val provider: ProviderType,
        val modelId: String,
    )

    private companion object {
        private const val DEFAULT_TOP_K = 6
        private const val MAX_QUERY_EMBEDDING_GROUPS = 2
        private const val EMBEDDING_WEIGHT = 0.85
        private const val KEYWORD_WEIGHT = 0.15
        private const val DIRECT_HIT_BOOST = 0.2
        private const val MIN_EMBEDDING_SCORE = 0.16
        private const val MIN_KEYWORD_SCORE = 0.18
    }
}

data class RetrievalResult(
    val chunks: List<RetrievedChunk> = emptyList(),
    val queryEmbeddingCosts: List<QueryEmbeddingCostSnapshot> = emptyList(),
)

data class QueryEmbeddingCostSnapshot(
    val provider: ProviderType,
    val modelId: String,
    val usage: UsageCost,
    val latencyMs: Long,
    val createdAt: Long,
)
