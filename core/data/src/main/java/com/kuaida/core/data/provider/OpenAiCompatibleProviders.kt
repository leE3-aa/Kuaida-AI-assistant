package com.kuaida.core.data.provider

import com.kuaida.core.common.error.toUserMessage
import com.kuaida.core.common.ui.displayLabel
import com.kuaida.core.data.datastore.UserPreferencesDataSource
import com.kuaida.core.model.AnswerGenerationRequest
import com.kuaida.core.model.AudioTranscriptionRequest
import com.kuaida.core.model.EmbeddingRequest
import com.kuaida.core.model.EmbeddingResult
import com.kuaida.core.model.ModelDescriptor
import com.kuaida.core.model.ProviderAnswerResult
import com.kuaida.core.model.ProviderType
import com.kuaida.core.model.ProviderValidationResult
import com.kuaida.core.model.SettingsDefaults
import com.kuaida.core.model.TranscriptionResult
import com.kuaida.core.model.UsageCost
import java.io.File
import java.util.Base64
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

abstract class OpenAiCompatibleProviderAdapter(
    final override val type: ProviderType,
    private val staticBaseUrl: String? = null,
    private val promptComposer: PromptComposer,
    private val client: OkHttpClient,
    protected val json: Json,
    protected val ioDispatcher: CoroutineDispatcher,
) : ProviderAdapter {

    protected fun availableModels(): List<ModelDescriptor> = ModelCatalog.byProvider(type)

    protected open suspend fun resolveBaseUrl(): String {
        return requireNotNull(staticBaseUrl) { "$type 未配置 Base URL。" }
    }

    protected suspend fun generateAnswerInternal(
        apiKey: String,
        request: AnswerGenerationRequest,
    ): ProviderAnswerResult = withContext(ioDispatcher) {
        val baseUrl = resolveBaseUrl()
        val payload = ChatCompletionRequest(
            model = request.modelId,
            messages = listOf(
                ChatMessage("system", promptComposer.systemPrompt(request.stylePreset)),
                ChatMessage("user", promptComposer.userPrompt(request)),
            ),
            temperature = 0.4,
            maxTokens = request.maxOutputTokens,
            thinking = ThinkingOption(type = "disabled"),
        )
        val response = executeJsonRequest(
            baseUrl = baseUrl,
            apiKey = apiKey,
            path = "chat/completions",
            bodyJson = json.encodeToString(ChatCompletionRequest.serializer(), payload),
        )
        val parsed = json.decodeFromString(ChatCompletionResponse.serializer(), response)
        val rawText = parsed.choices.firstOrNull()?.message?.content?.trim().orEmpty()
        val usage = parsed.usage?.toUsage(
            if (type == ProviderType.CUSTOM) {
                ModelCatalog.customChatModel(baseUrl = baseUrl, modelId = request.modelId)
            } else {
                ModelCatalog.find(type, request.modelId)
            },
        ) ?: UsageCost()
        ProviderAnswerResult(
            rawText = rawText,
            structuredAnswer = promptComposer.parseAnswer(rawText),
            usage = usage,
            modelId = request.modelId,
        )
    }

    override suspend fun validateKey(apiKey: String): ProviderValidationResult = withContext(ioDispatcher) {
        runCatching {
            val baseUrl = resolveBaseUrl()
            val request = Request.Builder()
                .url(baseUrl.ensureTrailingSlash() + "models")
                .header("Authorization", "Bearer $apiKey")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                ProviderValidationResult(
                    success = response.isSuccessful,
                    message = if (response.isSuccessful) {
                        "连接成功"
                    } else {
                        type.toProviderServiceException(response.code, body).message.orEmpty()
                    },
                )
            }
        }.getOrElse {
            ProviderValidationResult(
                success = false,
                message = it.toUserMessage("${type.displayLabel()} Key 校验失败，请稍后重试。"),
            )
        }
    }

    protected suspend fun transcribeAudioInternal(
        apiKey: String,
        request: AudioTranscriptionRequest,
    ): TranscriptionResult = withContext(ioDispatcher) {
        val baseUrl = resolveBaseUrl()
        val file = File(request.audioFilePath)
        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("model", request.modelId)
            .addFormDataPart(
                "file",
                file.name,
                file.asRequestBody(request.mimeType.toMediaType()),
            )
            .build()
        val httpRequest = Request.Builder()
            .url(baseUrl.ensureTrailingSlash() + "audio/transcriptions")
            .header("Authorization", "Bearer $apiKey")
            .post(multipartBody)
            .build()
        client.newCall(httpRequest).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw type.toProviderServiceException(response.code, body)
            val parsed = json.decodeFromString(AudioTranscriptionResponse.serializer(), body)
            TranscriptionResult(text = parsed.text)
        }
    }

    protected suspend fun embedInternal(
        apiKey: String,
        request: EmbeddingRequest,
    ): EmbeddingResult = withContext(ioDispatcher) {
        val baseUrl = resolveBaseUrl()
        val payload = EmbeddingPayload(model = request.modelId, input = request.input)
        val response = executeJsonRequest(
            baseUrl = baseUrl,
            apiKey = apiKey,
            path = "embeddings",
            bodyJson = json.encodeToString(EmbeddingPayload.serializer(), payload),
        )
        val parsed = json.decodeFromString(EmbeddingResponse.serializer(), response)
        val model = ModelCatalog.findEmbeddingOrNull(type, request.modelId)
            ?: ModelDescriptor(
                provider = type,
                modelId = request.modelId,
                displayName = request.modelId,
                inputPricePer1M = 0.0,
                outputPricePer1M = 0.0,
                supportsEmbedding = true,
            )
        EmbeddingResult(
            embedding = parsed.data.firstOrNull()?.embedding.orEmpty(),
            usage = parsed.usage?.toUsage(model) ?: UsageCost(),
        )
    }

    protected suspend fun fetchModelsInternal(apiKey: String): List<ModelDescriptor> = withContext(ioDispatcher) {
        val baseUrl = resolveBaseUrl()
        val request = Request.Builder()
            .url(baseUrl.ensureTrailingSlash() + "models")
            .header("Authorization", "Bearer $apiKey")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw type.toProviderServiceException(response.code, body)
            val parsed = json.decodeFromString(ModelListResponse.serializer(), body)
            parsed.data
                .map { it.id.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
                .map { modelId -> ModelCatalog.customChatModel(baseUrl = baseUrl, modelId = modelId) }
        }
    }

    protected fun executeJsonRequest(baseUrl: String, apiKey: String, path: String, bodyJson: String): String {
        val request = Request.Builder()
            .url(baseUrl.ensureTrailingSlash() + path)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(bodyJson.toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw type.toProviderServiceException(response.code, body)
            return body
        }
    }
}

class QwenProvider @Inject constructor(
    promptComposer: PromptComposer,
    client: OkHttpClient,
    json: Json,
    ioDispatcher: CoroutineDispatcher,
) : OpenAiCompatibleProviderAdapter(
    type = ProviderType.QWEN,
    staticBaseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1/",
    promptComposer = promptComposer,
    client = client,
    json = json,
    ioDispatcher = ioDispatcher,
), ChatProvider, EmbeddingProvider, SpeechToTextProvider {
    override suspend fun listModels(apiKey: String): List<ModelDescriptor> = availableModels()

    override suspend fun generateAnswer(apiKey: String, request: AnswerGenerationRequest): ProviderAnswerResult {
        return generateAnswerInternal(apiKey, request)
    }

    override suspend fun transcribeAudio(apiKey: String, request: AudioTranscriptionRequest): TranscriptionResult {
        return withContext(ioDispatcher) {
            val file = File(request.audioFilePath)
            require(file.exists()) { "录音文件不存在，请重录后再试。" }
            val base64Audio = Base64.getEncoder().encodeToString(file.readBytes())
            val dataUri = "data:${request.mimeType};base64,$base64Audio"
            val payload = QwenAsrRequest(
                model = request.modelId,
                messages = listOf(
                    QwenAsrMessage(
                        role = "user",
                        content = listOf(
                            QwenAsrInput(
                                type = "input_audio",
                                inputAudio = QwenInputAudio(data = dataUri),
                            ),
                        ),
                    ),
                ),
                asrOptions = QwenAsrOptions(enableItn = true),
            )
            val response = executeJsonRequest(
                baseUrl = resolveBaseUrl(),
                apiKey = apiKey,
                path = "chat/completions",
                bodyJson = json.encodeToString(QwenAsrRequest.serializer(), payload),
            )
            val parsed = json.decodeFromString(ChatCompletionResponse.serializer(), response)
            TranscriptionResult(text = parsed.choices.firstOrNull()?.message?.content?.trim().orEmpty())
        }
    }

    override suspend fun embed(apiKey: String, request: EmbeddingRequest): EmbeddingResult {
        return embedInternal(apiKey, request)
    }
}

class ZhipuProvider @Inject constructor(
    promptComposer: PromptComposer,
    client: OkHttpClient,
    json: Json,
    ioDispatcher: CoroutineDispatcher,
) : OpenAiCompatibleProviderAdapter(
    type = ProviderType.ZHIPU,
    staticBaseUrl = "https://open.bigmodel.cn/api/paas/v4/",
    promptComposer = promptComposer,
    client = client,
    json = json,
    ioDispatcher = ioDispatcher,
), ChatProvider, EmbeddingProvider, SpeechToTextProvider {
    override suspend fun listModels(apiKey: String): List<ModelDescriptor> = availableModels()

    override suspend fun generateAnswer(apiKey: String, request: AnswerGenerationRequest): ProviderAnswerResult {
        return generateAnswerInternal(apiKey, request)
    }

    override suspend fun transcribeAudio(apiKey: String, request: AudioTranscriptionRequest): TranscriptionResult {
        return transcribeAudioInternal(apiKey, request)
    }

    override suspend fun embed(apiKey: String, request: EmbeddingRequest): EmbeddingResult {
        return embedInternal(apiKey, request)
    }
}

class DeepSeekProvider @Inject constructor(
    promptComposer: PromptComposer,
    client: OkHttpClient,
    json: Json,
    ioDispatcher: CoroutineDispatcher,
) : OpenAiCompatibleProviderAdapter(
    type = ProviderType.DEEPSEEK,
    staticBaseUrl = "https://api.deepseek.com/v1/",
    promptComposer = promptComposer,
    client = client,
    json = json,
    ioDispatcher = ioDispatcher,
), ChatProvider {
    override suspend fun listModels(apiKey: String): List<ModelDescriptor> = availableModels()

    override suspend fun generateAnswer(apiKey: String, request: AnswerGenerationRequest): ProviderAnswerResult {
        return generateAnswerInternal(apiKey, request)
    }
}

class CustomOpenAiCompatibleProvider @Inject constructor(
    private val userPreferencesDataSource: UserPreferencesDataSource,
    promptComposer: PromptComposer,
    client: OkHttpClient,
    json: Json,
    ioDispatcher: CoroutineDispatcher,
) : OpenAiCompatibleProviderAdapter(
    type = ProviderType.CUSTOM,
    promptComposer = promptComposer,
    client = client,
    json = json,
    ioDispatcher = ioDispatcher,
), ChatProvider, EmbeddingProvider, SpeechToTextProvider {
    override suspend fun resolveBaseUrl(): String {
        val prefs = userPreferencesDataSource.preferences.first()
        val baseUrl = prefs.customBaseUrl.trim()
        require(baseUrl.isNotBlank()) { "请先填写自定义兼容接口的 Base URL。" }
        return baseUrl
    }

    override suspend fun listModels(apiKey: String): List<ModelDescriptor> = fetchModelsInternal(apiKey)

    override suspend fun generateAnswer(apiKey: String, request: AnswerGenerationRequest): ProviderAnswerResult {
        require(request.modelId.isNotBlank()) { "请先填写自定义兼容接口的聊天模型。" }
        return generateAnswerInternal(apiKey, request)
    }

    override suspend fun embed(apiKey: String, request: EmbeddingRequest): EmbeddingResult {
        require(request.modelId.isNotBlank()) { "请先填写自定义兼容接口的向量模型。" }
        return embedInternal(apiKey, request)
    }

    override suspend fun transcribeAudio(apiKey: String, request: AudioTranscriptionRequest): TranscriptionResult {
        require(request.modelId.isNotBlank()) { "请先填写自定义兼容接口的语音模型。" }
        return transcribeAudioInternal(apiKey, request)
    }
}

class CustomAuxProvider @Inject constructor(
    private val userPreferencesDataSource: UserPreferencesDataSource,
    promptComposer: PromptComposer,
    client: OkHttpClient,
    json: Json,
    ioDispatcher: CoroutineDispatcher,
) : OpenAiCompatibleProviderAdapter(
    type = ProviderType.CUSTOM_AUX,
    promptComposer = promptComposer,
    client = client,
    json = json,
    ioDispatcher = ioDispatcher,
), EmbeddingProvider, SpeechToTextProvider {

    override suspend fun resolveBaseUrl(): String {
        val prefs = userPreferencesDataSource.preferences.first()
        val baseUrl = prefs.auxBaseUrl.trim()
        require(baseUrl.isNotBlank()) { "请先填写自定义向量接口的 Base URL。" }
        return baseUrl
    }

    override suspend fun embed(apiKey: String, request: EmbeddingRequest): EmbeddingResult {
        require(request.modelId.isNotBlank()) { "请先填写自定义向量接口的向量模型。" }
        return embedInternal(apiKey, request)
    }

    override suspend fun transcribeAudio(apiKey: String, request: AudioTranscriptionRequest): TranscriptionResult {
        require(request.modelId.isNotBlank()) { "请先填写自定义向量接口的语音模型。" }
        return transcribeAudioInternal(apiKey, request)
    }
}

@Serializable
private data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double,
    @SerialName("max_tokens") val maxTokens: Int? = null,
    @SerialName("thinking") val thinking: ThinkingOption,
)

@Serializable
private data class ThinkingOption(
    val type: String,
)

@Serializable
private data class ChatMessage(
    val role: String,
    val content: String,
)

@Serializable
private data class ChatCompletionResponse(
    val choices: List<Choice>,
    val usage: UsagePayload? = null,
) {
    @Serializable
    data class Choice(
        val message: ChatMessage,
    )
}

@Serializable
private data class AudioTranscriptionResponse(
    val text: String,
)

@Serializable
private data class ModelListResponse(
    val data: List<ModelItem> = emptyList(),
) {
    @Serializable
    data class ModelItem(
        val id: String = "",
    )
}

@Serializable
private data class QwenAsrRequest(
    val model: String,
    val messages: List<QwenAsrMessage>,
    val stream: Boolean = false,
    @SerialName("asr_options") val asrOptions: QwenAsrOptions? = null,
)

@Serializable
private data class QwenAsrMessage(
    val role: String,
    val content: List<QwenAsrInput>,
)

@Serializable
private data class QwenAsrInput(
    val type: String,
    @SerialName("input_audio") val inputAudio: QwenInputAudio,
)

@Serializable
private data class QwenInputAudio(
    val data: String,
)

@Serializable
private data class QwenAsrOptions(
    @SerialName("enable_itn") val enableItn: Boolean = true,
)

@Serializable
private data class EmbeddingPayload(
    val model: String,
    val input: String,
)

@Serializable
private data class EmbeddingResponse(
    val data: List<EmbeddingItem>,
    val usage: UsagePayload? = null,
) {
    @Serializable
    data class EmbeddingItem(
        val embedding: List<Float>,
    )
}

@Serializable
private data class UsagePayload(
    @SerialName("prompt_tokens") val promptTokens: Int = 0,
    @SerialName("completion_tokens") val completionTokens: Int = 0,
    @SerialName("total_tokens") val totalTokens: Int = promptTokens + completionTokens,
) {
    fun toUsage(model: ModelDescriptor): UsageCost {
        val inputCost = promptTokens / 1_000_000.0 * model.inputPricePer1M
        val outputCost = completionTokens / 1_000_000.0 * model.outputPricePer1M
        return UsageCost(
            promptTokens = promptTokens,
            completionTokens = completionTokens,
            totalTokens = totalTokens,
            estimatedCostUsd = inputCost + outputCost,
        )
    }
}

private fun String.ensureTrailingSlash(): String = if (endsWith("/")) this else "$this/"
