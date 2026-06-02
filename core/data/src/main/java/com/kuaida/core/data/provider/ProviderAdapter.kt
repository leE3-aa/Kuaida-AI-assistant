package com.kuaida.core.data.provider

import com.kuaida.core.model.AnswerGenerationRequest
import com.kuaida.core.model.AudioTranscriptionRequest
import com.kuaida.core.model.EmbeddingRequest
import com.kuaida.core.model.EmbeddingResult
import com.kuaida.core.model.ModelDescriptor
import com.kuaida.core.model.ProviderAnswerResult
import com.kuaida.core.model.ProviderType
import com.kuaida.core.model.ProviderValidationResult
import com.kuaida.core.model.TranscriptionResult

interface ProviderAdapter {
    val type: ProviderType

    suspend fun validateKey(apiKey: String): ProviderValidationResult
}

interface ChatProvider : ProviderAdapter {
    suspend fun listModels(apiKey: String): List<ModelDescriptor>
    suspend fun generateAnswer(apiKey: String, request: AnswerGenerationRequest): ProviderAnswerResult
}

interface EmbeddingProvider : ProviderAdapter {
    suspend fun embed(apiKey: String, request: EmbeddingRequest): EmbeddingResult
}

interface SpeechToTextProvider : ProviderAdapter {
    suspend fun transcribeAudio(apiKey: String, request: AudioTranscriptionRequest): TranscriptionResult
}

class ProviderRegistry(
    chatProviders: Set<@JvmSuppressWildcards ChatProvider>,
    embeddingProviders: Set<@JvmSuppressWildcards EmbeddingProvider>,
    speechToTextProviders: Set<@JvmSuppressWildcards SpeechToTextProvider>,
) {
    private val chatProviderMap = chatProviders.associateBy { it.type }
    private val embeddingProviderMap = embeddingProviders.associateBy { it.type }
    private val speechToTextProviderMap = speechToTextProviders.associateBy { it.type }

    fun chatProvider(type: ProviderType): ChatProvider =
        requireNotNull(findChatProvider(type)) { "Chat provider $type has not been registered." }

    fun embeddingProvider(type: ProviderType): EmbeddingProvider =
        requireNotNull(findEmbeddingProvider(type)) { "Embedding provider $type has not been registered." }

    fun speechToTextProvider(type: ProviderType): SpeechToTextProvider =
        requireNotNull(findSpeechToTextProvider(type)) { "Speech-to-text provider $type has not been registered." }

    fun findChatProvider(type: ProviderType): ChatProvider? = chatProviderMap[type]

    fun findEmbeddingProvider(type: ProviderType): EmbeddingProvider? = embeddingProviderMap[type]

    fun findSpeechToTextProvider(type: ProviderType): SpeechToTextProvider? = speechToTextProviderMap[type]

    fun findProvider(type: ProviderType): ProviderAdapter? {
        return findChatProvider(type)
            ?: findEmbeddingProvider(type)
            ?: findSpeechToTextProvider(type)
    }
}

data class ConfiguredProvider<T : ProviderAdapter>(
    val provider: T,
    val apiKey: String,
)

suspend fun ProviderRegistry.findConfiguredEmbeddingProvider(
    preferredType: ProviderType,
    apiKeyLookup: suspend (ProviderType) -> String?,
    fallbackTypes: List<ProviderType> = listOf(ProviderType.QWEN, ProviderType.ZHIPU),
): ConfiguredProvider<EmbeddingProvider>? {
    return resolveConfiguredProvider(
        preferredType = preferredType,
        fallbackTypes = fallbackTypes,
        providerLookup = ::findEmbeddingProvider,
        apiKeyLookup = apiKeyLookup,
    )
}

suspend fun ProviderRegistry.findConfiguredSpeechToTextProvider(
    preferredType: ProviderType,
    apiKeyLookup: suspend (ProviderType) -> String?,
    fallbackTypes: List<ProviderType> = listOf(ProviderType.QWEN, ProviderType.ZHIPU),
): ConfiguredProvider<SpeechToTextProvider>? {
    return resolveConfiguredProvider(
        preferredType = preferredType,
        fallbackTypes = fallbackTypes,
        providerLookup = ::findSpeechToTextProvider,
        apiKeyLookup = apiKeyLookup,
    )
}

private suspend fun <T : ProviderAdapter> ProviderRegistry.resolveConfiguredProvider(
    preferredType: ProviderType,
    fallbackTypes: List<ProviderType>,
    providerLookup: (ProviderType) -> T?,
    apiKeyLookup: suspend (ProviderType) -> String?,
): ConfiguredProvider<T>? {
    return buildList {
        add(preferredType)
        addAll(fallbackTypes)
    }.distinct().firstNotNullOfOrNull { type ->
        val provider = providerLookup(type) ?: return@firstNotNullOfOrNull null
        val apiKey = apiKeyLookup(type) ?: return@firstNotNullOfOrNull null
        ConfiguredProvider(provider = provider, apiKey = apiKey)
    }
}
