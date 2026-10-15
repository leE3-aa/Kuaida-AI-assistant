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
        client.newCall(request).exeP��
