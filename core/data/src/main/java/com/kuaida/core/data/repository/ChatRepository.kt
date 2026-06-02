package com.kuaida.core.data.repository

import androidx.room.withTransaction
import com.kuaida.core.common.error.toUserMessage
import com.kuaida.core.common.ui.displayLabel
import com.kuaida.core.data.database.AppDatabase
import com.kuaida.core.data.database.ChatSessionDao
import com.kuaida.core.data.database.ChatSessionEntity
import com.kuaida.core.data.database.CostRecordDao
import com.kuaida.core.data.database.CostRecordEntity
import com.kuaida.core.data.database.toDomain
import com.kuaida.core.data.provider.findProviderServiceException
import com.kuaida.core.data.provider.findConfiguredSpeechToTextProvider
import com.kuaida.core.data.provider.ProviderRegistry
import com.kuaida.core.data.rag.RagContextAssembler
import com.kuaida.core.data.rag.KnowledgeRetriever
import com.kuaida.core.model.AnswerBlock
import com.kuaida.core.model.AnswerGenerationRequest
import com.kuaida.core.model.AudioTranscriptionRequest
import com.kuaida.core.model.CostRecord
import com.kuaida.core.model.CostRecordCategory
import com.kuaida.core.model.FollowUpAction
import com.kuaida.core.model.InterviewSession
import com.kuaida.core.model.QuestionSourceType
import com.kuaida.core.model.SessionStatus
import com.kuaida.core.model.TranscriptionResult
import com.kuaida.core.model.UsageCost
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

@Singleton
class ChatRepository @Inject constructor(
    private val database: AppDatabase,
    private val chatSessionDao: ChatSessionDao,
    private val costRecordDao: CostRecordDao,
    private val settingsRepository: SettingsRepository,
    private val providerRegistry: ProviderRegistry,
    private val knowledgeRetriever: KnowledgeRetriever,
    private val ragContextAssembler: RagContextAssembler,
    private val json: Json,
) {
    fun observeSessions(query: String): Flow<List<InterviewSession>> {
        return chatSessionDao.observeSessions(query).map { items ->
            items.map { it.toDomain(json) }
        }
    }

    fun observeCostRecords(sessionId: String): Flow<List<CostRecord>> {
        return costRecordDao.observeBySessionId(sessionId).map { items -> items.map { it.toDomain() } }
    }

    suspend fun getSession(id: String): InterviewSession? = chatSessionDao.getById(id)?.toDomain(json)

    suspend fun getCostRecords(sessionId: String): List<CostRecord> {
        return costRecordDao.findBySessionId(sessionId).map { it.toDomain() }
    }

    suspend fun deleteSession(id: String) {
        chatSessionDao.delete(id)
    }

    suspend fun generateAnswer(
        question: String,
        sourceType: QuestionSourceType,
        transcript: String? = null,
        followUpAction: FollowUpAction? = null,
        previousAnswer: AnswerBlock? = null,
    ): InterviewSession {
        val selectedModel = settingsRepository.currentModel()
        require(selectedModel.modelId.isNotBlank()) {
            if (selectedModel.provider == com.kuaida.core.model.ProviderType.CUSTOM) {
                "请先在设置页填写自定义兼容接口的聊天模型。"
            } else {
                "请先在设置页选择聊天模型。"
            }
        }
        val tokenLimit = settingsRepository.currentTokenLimit()
        val apiKey = requireNotNull(settingsRepository.getApiKeyOrNull(selectedModel.provider)) {
            "请先在设置页配置并验证 ${selectedModel.provider.displayLabel()} 的 API Key。"
        }
        val createdAt = System.currentTimeMillis()
        val sessionId = "session_$createdAt"
        val stylePreset = settingsRepository.selectedStyle.first()

        database.withTransaction {
            chatSessionDao.upsert(
                ChatSessionEntity(
                    id = sessionId,
                    question = question,
                    transcript = transcript,
                    answerJson = null,
                    provider = selectedModel.provider,
                    modelId = selectedModel.modelId,
                    stylePreset = stylePreset,
                    sourceType = sourceType,
                    status = SessionStatus.GENERATING,
                    latencyMs = 0L,
                    errorMessage = null,
                    createdAt = createdAt,
                    updatedAt = createdAt,
                ),
            )
        }

        return runCatching {
            val retrieval = knowledgeRetriever.retrieve(question)
            val ragContext = ragContextAssembler.assemble(
                question = question,
                retrievedChunks = retrieval.chunks,
                maxOutputTokens = tokenLimit,
                previousAnswer = previousAnswer,
            )
            if (retrieval.queryEmbeddingCosts.isNotEmpty()) {
                database.withTransaction {
                    costRecordDao.insertAll(
                        retrieval.queryEmbeddingCosts.mapIndexed { index, usageSnapshot ->
                            CostRecordEntity(
                                id = "cost_${sessionId}_query_embedding_$index",
                                sessionId = sessionId,
                                documentId = null,
                                chunkId = null,
                                category = CostRecordCategory.QUERY_EMBEDDING,
                                provider = usageSnapshot.provider,
                                modelId = usageSnapshot.modelId,
                                promptTokens = usageSnapshot.usage.promptTokens,
                                completionTokens = usageSnapshot.usage.completionTokens,
                                totalTokens = usageSnapshot.usage.totalTokens,
                                estimatedCostUsd = usageSnapshot.usage.estimatedCostUsd,
                                latencyMs = usageSnapshot.latencyMs,
                                createdAt = usageSnapshot.createdAt,
                            )
                        },
                    )
                }
            }
            val request = AnswerGenerationRequest(
                provider = selectedModel.provider,
                modelId = selectedModel.modelId,
                question = question,
                stylePreset = stylePreset,
                maxOutputTokens = tokenLimit,
                ragContext = ragContext.chunks,
                followUpAction = followUpAction,
                previousAnswer = previousAnswer,
            )
            val startedAt = System.currentTimeMillis()
            val result = providerRegistry.chatProvider(selectedModel.provider).generateAnswer(apiKey, request)
            val finishedAt = System.currentTimeMillis()
            val latencyMs = finishedAt - startedAt
            val answerJson = json.encodeToString(AnswerBlock.serializer(), result.structuredAnswer)
            val sessionEntity = ChatSessionEntity(
                id = sessionId,
                question = question,
                transcript = transcript,
                answerJson = answerJson,
                provider = selectedModel.provider,
                modelId = result.modelId,
                stylePreset = request.stylePreset,
                sourceType = sourceType,
                status = SessionStatus.COMPLETED,
                latencyMs = latencyMs,
                errorMessage = null,
                createdAt = createdAt,
                updatedAt = finishedAt,
            )
            val usage = result.usage.normalized()
            val costRecord = CostRecordEntity(
                id = "cost_${sessionId}_answer",
                sessionId = sessionId,
                documentId = null,
                chunkId = null,
                category = CostRecordCategory.ANSWER_GENERATION,
                provider = selectedModel.provider,
                modelId = result.modelId,
                promptTokens = usage.promptTokens,
                completionTokens = usage.completionTokens,
                totalTokens = usage.totalTokens,
                estimatedCostUsd = usage.estimatedCostUsd,
                latencyMs = latencyMs,
                createdAt = finishedAt,
            )

            database.withTransaction {
                chatSessionDao.upsert(sessionEntity)
                costRecordDao.insertAll(listOf(costRecord))
            }

            requireNotNull(chatSessionDao.getById(sessionId)?.toDomain(json)) {
                "会话保存失败，请稍后重试。"
            }
        }.getOrElse { throwable ->
            if (throwable.findProviderServiceException()?.isCredentialError == true) {
                settingsRepository.markApiKeyInvalid(selectedModel.provider)
            }
            val userMessage = throwable.toUserMessage("生成失败，请稍后重试。")
            val failedAt = System.currentTimeMillis()
            database.withTransaction {
                chatSessionDao.upsert(
                    ChatSessionEntity(
                        id = sessionId,
                        question = question,
                        transcript = transcript,
                        answerJson = null,
                        provider = selectedModel.provider,
                        modelId = selectedModel.modelId,
                        stylePreset = stylePreset,
                        sourceType = sourceType,
                        status = SessionStatus.FAILED,
                        latencyMs = failedAt - createdAt,
                        errorMessage = userMessage,
                        createdAt = createdAt,
                        updatedAt = failedAt,
                    ),
                )
            }
            throw IllegalStateException(userMessage, throwable)
        }
    }

    suspend fun transcribeAudio(filePath: String): TranscriptionResult {
        val selectedModel = settingsRepository.currentSpeechToTextModel()
        val configuredProvider = providerRegistry.findConfiguredSpeechToTextProvider(
            preferredType = selectedModel.provider,
            apiKeyLookup = settingsRepository::getApiKeyOrNull,
        ) ?: error("当前没有可用的转写 Provider，请先配置支持转写的 Provider Key。")
        val resolvedModel = if (configuredProvider.provider.type == selectedModel.provider) {
            selectedModel
        } else {
            com.kuaida.core.data.provider.ModelCatalog.defaultSpeechToTextModel(configuredProvider.provider.type)
        }
        return runCatching {
            configuredProvider.provider.transcribeAudio(
                apiKey = configuredProvider.apiKey,
                request = AudioTranscriptionRequest(
                    provider = configuredProvider.provider.type,
                    modelId = resolvedModel.modelId,
                    audioFilePath = filePath,
                    mimeType = "audio/mp4",
                ),
            )
        }.getOrElse { throwable ->
            if (throwable.findProviderServiceException()?.isCredentialError == true) {
                settingsRepository.markApiKeyInvalid(configuredProvider.provider.type)
            }
            throw IllegalStateException(
                throwable.toUserMessage("转写失败，请重试或重录。"),
                throwable,
            )
        }
    }

    private fun UsageCost.normalized(): UsageCost {
        return if (totalTokens > 0 || promptTokens + completionTokens == 0) {
            this
        } else {
            copy(totalTokens = promptTokens + completionTokens)
        }
    }
}
