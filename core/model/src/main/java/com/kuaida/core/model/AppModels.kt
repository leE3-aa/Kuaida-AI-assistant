package com.kuaida.core.model

import kotlinx.serialization.Serializable

enum class ProviderType {
    QWEN,
    ZHIPU,
    DEEPSEEK,
    CUSTOM,
    ;

    companion object {
        fun fromStoredValue(value: String?): ProviderType {
            return when (value) {
                QWEN.name,
                "OPENAI",
                null,
                "",
                -> QWEN

                ZHIPU.name,
                "ANTHROPIC",
                -> ZHIPU

                DEEPSEEK.name -> DEEPSEEK
                CUSTOM.name,
                "CUSTOM_OPENAI",
                "OPENAI_COMPATIBLE",
                -> CUSTOM
                else -> QWEN
            }
        }
    }
}

enum class QuestionSourceType {
    TEXT,
    AUDIO,
}

enum class AnswerStylePreset(val label: String) {
    BRIEF("简洁"),
    DETAILED("详细"),
    SPOKEN("口语"),
    ENGLISH("英文"),
}

enum class FollowUpAction(val label: String) {
    EXPAND_DETAIL("展开细节"),
    ADD_EXAMPLE("添加示例"),
    MORE_SPOKEN("更口语化"),
    REGENERATE("重新生成"),
}

enum class SessionStatus {
    IDLE,
    GENERATING,
    COMPLETED,
    FAILED,
}

enum class DocumentParseStatus {
    PENDING,
    PROCESSING,
    READY,
    FAILED,
}

enum class CostRecordCategory {
    ANSWER_GENERATION,
    AUDIO_TRANSCRIPTION,
    KNOWLEDGE_EMBEDDING,
    QUERY_EMBEDDING,
}

enum class RecorderState {
    IDLE,
    RECORDING,
    STOPPED,
    TRANSCRIBING,
    ERROR,
}

@Serializable
data class AnswerBlock(
    val conclusion: String,
    val keyPoints: List<String>,
    val expansion: String,
)

data class UsageCost(
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val totalTokens: Int = 0,
    val estimatedCostUsd: Double = 0.0,
)

data class ModelDescriptor(
    val provider: ProviderType,
    val modelId: String,
    val displayName: String,
    val inputPricePer1M: Double,
    val outputPricePer1M: Double,
    val supportsTranscription: Boolean = false,
    val supportsEmbedding: Boolean = false,
)

data class ProviderCredential(
    val provider: ProviderType,
    val apiKey: String,
)

data class AnswerGenerationRequest(
    val provider: ProviderType,
    val modelId: String,
    val question: String,
    val stylePreset: AnswerStylePreset,
    val maxOutputTokens: Int,
    val ragContext: List<RetrievedChunk> = emptyList(),
    val followUpAction: FollowUpAction? = null,
    val previousAnswer: AnswerBlock? = null,
)

data class ProviderAnswerResult(
    val rawText: String,
    val structuredAnswer: AnswerBlock,
    val usage: UsageCost,
    val modelId: String,
)

data class AudioTranscriptionRequest(
    val provider: ProviderType,
    val modelId: String,
    val audioFilePath: String,
    val mimeType: String,
)

data class TranscriptionResult(
    val text: String,
    val usage: UsageCost = UsageCost(),
)

data class EmbeddingRequest(
    val provider: ProviderType,
    val modelId: String,
    val input: String,
)

data class EmbeddingResult(
    val embedding: List<Float>,
    val usage: UsageCost = UsageCost(),
)

data class ProviderValidationResult(
    val success: Boolean,
    val message: String,
)

data class CustomProviderConfig(
    val baseUrl: String = SettingsDefaults.DEFAULT_CUSTOM_BASE_URL,
    val chatModelId: String = SettingsDefaults.DEFAULT_CUSTOM_CHAT_MODEL,
    val embeddingModelId: String = SettingsDefaults.DEFAULT_CUSTOM_EMBEDDING_MODEL,
)

data class ProviderConfig(
    val provider: ProviderType,
    val encryptedKey: ByteArray,
    val iv: ByteArray,
    val last4: String,
    val isConfigured: Boolean,
    val updatedAt: Long,
)

data class InterviewSession(
    val id: String,
    val question: String,
    val transcript: String?,
    val answer: AnswerBlock?,
    val provider: ProviderType,
    val modelId: String,
    val stylePreset: AnswerStylePreset,
    val sourceType: QuestionSourceType,
    val usage: UsageCost,
    val status: SessionStatus,
    val latencyMs: Long,
    val createdAt: Long,
    val errorMessage: String? = null,
)

data class KnowledgeDocument(
    val id: String,
    val displayName: String,
    val mimeType: String,
    val sourceUri: String,
    val sizeBytes: Long,
    val parseStatus: DocumentParseStatus,
    val chunkCount: Int,
    val embeddingCount: Int = 0,
    val failureReason: String? = null,
    val isEnabled: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

data class KnowledgeImportResult(
    val documentId: String,
    val displayName: String,
    val chunkCount: Int,
    val embeddingCount: Int,
    val embeddingProvider: ProviderType? = null,
    val embeddingModelId: String? = null,
    val warningMessage: String? = null,
)

data class KnowledgeChunk(
    val id: String,
    val documentId: String,
    val chunkIndex: Int,
    val content: String,
    val tokenCount: Int,
    val embedding: List<Float>,
    val contentHash: String,
)

data class CostRecord(
    val id: String,
    val sessionId: String? = null,
    val documentId: String? = null,
    val chunkId: String? = null,
    val category: CostRecordCategory,
    val provider: ProviderType,
    val modelId: String,
    val usage: UsageCost,
    val latencyMs: Long = 0,
    val createdAt: Long,
)

data class RetrievedChunk(
    val chunkId: String = "",
    val documentId: String = "",
    val documentName: String,
    val chunkIndex: Int = 0,
    val content: String,
    val tokenCount: Int = 0,
    val score: Double,
)

data class SettingsSummary(
    val selectedModel: ModelDescriptor,
    val selectedStyle: AnswerStylePreset,
    val tokenLimit: Int,
    val embeddingModel: ModelDescriptor,
    val speechToTextModel: ModelDescriptor,
    val configuredProviders: Map<ProviderType, Boolean>,
    val customProviderConfig: CustomProviderConfig = CustomProviderConfig(),
    val totalTokens: Int,
    val totalCostUsd: Double,
    val highCostAlertEnabled: Boolean,
)

object SettingsDefaults {
    const val MIN_TOKEN_LIMIT = 256
    const val MAX_TOKEN_LIMIT = 4096
    const val DEFAULT_TOKEN_LIMIT = 1024
    const val DEFAULT_CUSTOM_BASE_URL = "https://api.siliconflow.cn/v1"
    const val DEFAULT_CUSTOM_CHAT_MODEL = "deepseek-ai/DeepSeek-V3"
    const val DEFAULT_CUSTOM_EMBEDDING_MODEL = "BAAI/bge-m3"
    const val DEFAULT_CUSTOM_SPEECH_MODEL = "FunAudioLLM/SenseVoiceSmall"
}

data class TemplateItem(
    val id: String,
    val category: String,
    val title: String,
    val description: String,
    val prompt: String,
    val readingMinutes: Int,
    val favorite: Boolean,
)
