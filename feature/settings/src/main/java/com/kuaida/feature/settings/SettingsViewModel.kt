package com.kuaida.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuaida.core.common.error.toUserMessage
import com.kuaida.core.common.ui.UiMessage
import com.kuaida.core.common.ui.displayLabel
import com.kuaida.core.data.provider.ModelCatalog
import com.kuaida.core.data.repository.SettingsRepository
import com.kuaida.core.model.AnswerStylePreset
import com.kuaida.core.model.ModelDescriptor
import com.kuaida.core.model.ProviderType
import com.kuaida.core.model.SettingsDefaults
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
) : ViewModel() {
    private val drafts = MutableStateFlow(ProviderType.entries.associateWith { "" })
    private val customBaseUrlDraft = MutableStateFlow(SettingsDefaults.DEFAULT_CUSTOM_BASE_URL)
    private val customChatModelDraft = MutableStateFlow(SettingsDefaults.DEFAULT_CUSTOM_CHAT_MODEL)
    private val customEmbeddingModelDraft = MutableStateFlow(SettingsDefaults.DEFAULT_CUSTOM_EMBEDDING_MODEL)
    private val tokenLimitDraft = MutableStateFlow(SettingsDefaults.DEFAULT_TOKEN_LIMIT.toString())
    private val embeddingVendor = MutableStateFlow(ProviderType.QWEN)
    private val speechVendor = MutableStateFlow(ProviderType.QWEN)
    private val message = MutableStateFlow<UiMessage?>(null)
    private val remoteModelsState = MutableStateFlow(RemoteModelsState())
    private val customProviderDrafts = combine(
        customBaseUrlDraft,
        customChatModelDraft,
        customEmbeddingModelDraft,
        remoteModelsState,
    ) { baseUrl, chatModelId, embeddingModelId, remote ->
        CustomDrafts(
            baseUrl = baseUrl,
            chatModelId = chatModelId,
            embeddingModelId = embeddingModelId,
            remoteModels = remote.models,
            loadingModels = remote.loading,
        )
    }
    private val vendorSelection = combine(
        embeddingVendor,
        speechVendor,
        tokenLimitDraft,
    ) { embedding, speech, tokenDraft ->
        VendorSelection(
            embeddingVendor = embedding,
            speechVendor = speech,
            tokenLimitDraft = tokenDraft,
        )
    }

    init {
        viewModelScope.launch {
            val masked = repository.maskedApiKeys()
            drafts.value = drafts.value.mapValues { (provider, _) -> masked[provider].orEmpty() }
        }
        viewModelScope.launch {
            val summary = repository.settingsSummary.first()
            tokenLimitDraft.value = summary.tokenLimit.toString()
            customBaseUrlDraft.value = summary.customProviderConfig.baseUrl
            customChatModelDraft.value = summary.customProviderConfig.chatModelId
            customEmbeddingModelDraft.value = summary.customProviderConfig.embeddingModelId
        }
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        repository.settingsSummary,
        drafts,
        customProviderDrafts,
        vendorSelection,
        message,
    ) { summary, keyDrafts, customDrafts, vendorState, uiMessage ->
        SettingsUiState(
            availableProviders = availableProviders(),
            selectedModel = summary.selectedModel,
            availableModels = ModelCatalog.chatModels,
            selectedStyle = summary.selectedStyle,
            tokenLimit = summary.tokenLimit,
            tokenLimitDraft = vendorState.tokenLimitDraft,
            embeddingModel = summary.embeddingModel,
            embeddingModels = ModelCatalog.embeddingModels,
            embeddingVendor = vendorState.embeddingVendor,
            speechToTextModel = summary.speechToTextModel,
            speechToTextModels = ModelCatalog.speechToTextModels,
            speechVendor = vendorState.speechVendor,
            providerKeys = keyDrafts,
            configuredProviders = summary.configuredProviders,
            customBaseUrl = customDrafts.baseUrl,
            customChatModelId = customDrafts.chatModelId,
            customEmbeddingModelId = customDrafts.embeddingModelId,
            remoteChatModels = customDrafts.remoteModels,
            loadingModels = customDrafts.loadingModels,
            totalTokens = summary.totalTokens,
            totalCostUsd = summary.totalCostUsd,
            highCostAlertEnabled = summary.highCostAlertEnabled,
            message = uiMessage,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun updateDraftKey(provider: ProviderType, value: String) {
        drafts.update { it + (provider to value) }
    }

    fun updateCustomBaseUrlDraft(value: String) {
        customBaseUrlDraft.value = value
    }

    fun updateCustomChatModelDraft(value: String) {
        customChatModelDraft.value = value
    }

    fun updateCustomEmbeddingModelDraft(value: String) {
        customEmbeddingModelDraft.value = value
    }

    fun refreshRemoteModels(silent: Boolean = false) {
        if (remoteModelsState.value.loading) return
        viewModelScope.launch {
            remoteModelsState.update { it.copy(loading = true) }
            val result = runCatching { repository.fetchRemoteChatModels() }
            remoteModelsState.update { it.copy(loading = false) }
            result.onSuccess { models ->
                if (models.isEmpty()) {
                    if (!silent) {
                        message.value = UiMessage(message = "没取到模型列表，请确认 Base URL 和 API Key 是否正确。")
                    }
                } else {
                    remoteModelsState.update { it.copy(models = models) }
                    if (!silent) {
                        message.value = UiMessage(message = "已获取 ${models.size} 个可用模型。")
                    }
                }
            }.onFailure { throwable ->
                if (!silent) {
                    message.value = UiMessage(
                        message = throwable.toUserMessage("获取模型列表失败，请检查 Base URL 和 API Key。"),
                    )
                }
            }
        }
    }

    fun saveApiKey(provider: ProviderType) {
        viewModelScope.launch {
            runCatching {
                val draft = drafts.value[provider].orEmpty().trim()
                if (provider == ProviderType.CUSTOM) {
                    val baseUrl = customBaseUrlDraft.value.trim()
                    val chatModelId = customChatModelDraft.value.trim()
                    val embeddingModelId = customEmbeddingModelDraft.value.trim()
                    if (baseUrl.isBlank()) {
                        message.value = UiMessage(message = "请先填写自定义兼容接口的 Base URL。")
                        return@launch
                    }
                    if (chatModelId.isBlank()) {
                        message.value = UiMessage(message = "请先填写自定义兼容接口的聊天模型。")
                        return@launch
                    }
                    repository.updateCustomProviderConfig(baseUrl, chatModelId, embeddingModelId)
                    repository.updateSelectedModel(ProviderType.CUSTOM, chatModelId)
                    repository.updateSelectedEmbeddingModel(ProviderType.CUSTOM, embeddingModelId)
                    repository.updateSelectedSpeechToTextModel(
                        ProviderType.CUSTOM,
                        SettingsDefaults.DEFAULT_CUSTOM_SPEECH_MODEL,
                    )
                }
                if (draft.startsWith("••••")) {
                    message.value = UiMessage(
                        message = if (provider == ProviderType.CUSTOM) {
                            "模型调用配置已更新，已保留现有 API Key。"
                        } else {
                            "${provider.displayLabel()} 的 API Key 未发生变化。"
                        },
                    )
                    return@launch
                }
                if (draft.isNotBlank()) {
                    val validation = repository.validateApiKey(provider, draft)
                    if (!validation.success) {
                        message.value = UiMessage(message = validation.message)
                        return@launch
                    }
                }
                repository.saveApiKey(provider, draft)
                val masked = repository.maskedApiKeys()[provider].orEmpty()
                drafts.update { it + (provider to masked) }
                if (provider == ProviderType.CUSTOM && draft.isNotBlank()) {
                    refreshRemoteModels(silent = true)
                }
                message.value = UiMessage(
                    message = if (draft.isBlank()) {
                        "已清空 ${provider.displayLabel()} 的 API Key。"
                    } else {
                        "${provider.displayLabel()} 的 API Key 已验证并加密保存。"
                    },
                )
            }.onFailure { throwable ->
                message.value = UiMessage(message = throwable.toUserMessage("保存 API Key 失败，请稍后重试。"))
            }
        }
    }

    fun clearApiKey(provider: ProviderType) {
        viewModelScope.launch {
            runCatching {
                repository.saveApiKey(provider, "")
                drafts.update { it + (provider to "") }
                message.value = UiMessage(message = "已清空 ${provider.displayLabel()} 的 API Key。")
            }.onFailure { throwable ->
                message.value = UiMessage(message = throwable.toUserMessage("清空 API Key 失败，请稍后重试。"))
            }
        }
    }

    fun updateSelectedModel(provider: ProviderType, modelId: String) {
        viewModelScope.launch {
            if (provider == ProviderType.CUSTOM) {
                customChatModelDraft.value = modelId
                repository.updateCustomProviderConfig(
                    customBaseUrlDraft.value,
                    modelId,
                    customEmbeddingModelDraft.value,
                )
            }
            repository.updateSelectedModel(provider, modelId)
        }
    }

    fun updateStyle(stylePreset: AnswerStylePreset) {
        viewModelScope.launch { repository.updateStyle(stylePreset) }
    }

    fun updateSelectedEmbeddingModel(provider: ProviderType, modelId: String) {
        viewModelScope.launch { repository.updateSelectedEmbeddingModel(provider, modelId) }
    }

    fun updateSelectedSpeechToTextModel(provider: ProviderType, modelId: String) {
        viewModelScope.launch { repository.updateSelectedSpeechToTextModel(provider, modelId) }
    }

    fun selectEmbeddingVendor(provider: ProviderType) {
        embeddingVendor.value = provider
    }

    fun selectSpeechVendor(provider: ProviderType) {
        speechVendor.value = provider
    }

    fun saveEmbeddingConfig(provider: ProviderType, modelId: String) {
        viewModelScope.launch {
            runCatching {
                if (modelId.isBlank()) {
                    message.value = UiMessage(message = "请先选择要使用的向量模型。")
                    return@launch
                }
                val draft = drafts.value[provider].orEmpty().trim()
                if (draft.isNotBlank() && !draft.startsWith("••••")) {
                    val validation = repository.validateApiKey(provider, draft)
                    if (!validation.success) {
                        message.value = UiMessage(message = validation.message)
                        return@launch
                    }
                    repository.saveApiKey(provider, draft)
                    val masked = repository.maskedApiKeys()[provider].orEmpty()
                    drafts.update { it + (provider to masked) }
                }
                repository.updateSelectedEmbeddingModel(provider, modelId)
                message.value = UiMessage(
                    message = "知识库向量接口已设置为 ${provider.displayLabel()} · $modelId。",
                )
            }.onFailure { throwable ->
                message.value = UiMessage(message = throwable.toUserMessage("保存失败，请稍后重试。"))
            }
        }
    }

    fun saveSpeechConfig(provider: ProviderType, modelId: String) {
        viewModelScope.launch {
            runCatching {
                if (modelId.isBlank()) {
                    message.value = UiMessage(message = "请先选择要使用的语音识别模型。")
                    return@launch
                }
                val draft = drafts.value[provider].orEmpty().trim()
                if (draft.isNotBlank() && !draft.startsWith("••••")) {
                    val validation = repository.validateApiKey(provider, draft)
                    if (!validation.success) {
                        message.value = UiMessage(message = validation.message)
                        return@launch
                    }
                    repository.saveApiKey(provider, draft)
                    val masked = repository.maskedApiKeys()[provider].orEmpty()
                    drafts.update { it + (provider to masked) }
                }
                repository.updateSelectedSpeechToTextModel(provider, modelId)
                message.value = UiMessage(
                    message = "录音转写接口已设置为 ${provider.displayLabel()} · $modelId。",
                )
            }.onFailure { throwable ->
                message.value = UiMessage(message = throwable.toUserMessage("保存失败，请稍后重试。"))
            }
        }
    }

    fun selectProvider(provider: ProviderType) {
        viewModelScope.launch {
            if (provider == ProviderType.CUSTOM) {
                val hadCustomConfig = customBaseUrlDraft.value.trim().isNotBlank() &&
                    customChatModelDraft.value.trim().isNotBlank()
                val baseUrl = customBaseUrlDraft.value.trim().ifBlank { SettingsDefaults.DEFAULT_CUSTOM_BASE_URL }
                val supportedChatModels = ModelCatalog.simplifiedCustomChatModels(baseUrl)
                val chatModelId = customChatModelDraft.value.trim()
                    .takeIf { draft -> supportedChatModels.any { it.modelId == draft } }
                    ?: SettingsDefaults.DEFAULT_CUSTOM_CHAT_MODEL
                val embeddingModelId = customEmbeddingModelDraft.value.trim()
                    .ifBlank { SettingsDefaults.DEFAULT_CUSTOM_EMBEDDING_MODEL }
                customBaseUrlDraft.value = baseUrl
                customChatModelDraft.value = chatModelId
                customEmbeddingModelDraft.value = embeddingModelId
                repository.updateCustomProviderConfig(baseUrl, chatModelId, embeddingModelId)
                repository.updateSelectedModel(ProviderType.CUSTOM, chatModelId)
                repository.updateSelectedEmbeddingModel(ProviderType.CUSTOM, embeddingModelId)
                repository.updateSelectedSpeechToTextModel(
                    ProviderType.CUSTOM,
                    SettingsDefaults.DEFAULT_CUSTOM_SPEECH_MODEL,
                )
                message.value = UiMessage(
                    message = if (hadCustomConfig) {
                        "已切到统一兼容接口，确认 API Key 后就可以继续使用。"
                    } else {
                        "已切到统一兼容接口，默认预填了常用地址和模型，输入 API Key 就能开始使用。"
                    },
                )
                return@launch
            }

            val chatModel = ModelCatalog.byProvider(provider).firstOrNull()
            val embeddingModel = ModelCatalog.embeddingByProvider(provider).firstOrNull()
            val speechModel = ModelCatalog.speechToTextByProvider(provider).firstOrNull()

            require(chatModel != null) { "${provider.displayLabel()} 当前没有可用聊天模型。" }

            repository.updateSelectedModel(chatModel.provider, chatModel.modelId)
            embeddingModel?.let { repository.updateSelectedEmbeddingModel(it.provider, it.modelId) }
            speechModel?.let { repository.updateSelectedSpeechToTextModel(it.provider, it.modelId) }

            message.value = UiMessage(
                message = when {
                    embeddingModel != null && speechModel != null -> {
                        "已切换到 ${provider.displayLabel()}，填入这一个 API Key 就能跑通聊天、知识库和语音转写。"
                    }

                    embeddingModel != null -> {
                        "已切换到 ${provider.displayLabel()}，填入这一个 API Key 就能使用聊天和知识库。语音转写需切换到支持 ASR 的 Provider。"
                    }

                    speechModel != null -> {
                        "已切换到 ${provider.displayLabel()}，填入这一个 API Key 就能使用聊天和语音转写。知识库会优先使用已配置的向量 Provider。"
                    }

                    else -> {
                        "已切换到 ${provider.displayLabel()}，填入这一个 API Key 就能开始聊天。若未额外配置其它能力，知识库会回退关键词检索，语音转写暂不可用。"
                    }
                },
            )
        }
    }

    fun updateTokenLimitDraft(value: String) {
        if (value.isEmpty() || value.all(Char::isDigit)) {
            tokenLimitDraft.value = value
        }
    }

    fun applyTokenLimit(limit: Int) {
        viewModelScope.launch {
            val normalized = limit.coerceIn(SettingsDefaults.MIN_TOKEN_LIMIT, SettingsDefaults.MAX_TOKEN_LIMIT)
            repository.updateTokenLimit(normalized)
            tokenLimitDraft.value = normalized.toString()
            message.value = UiMessage(message = "输出 token 上限已更新为 $normalized。")
        }
    }

    fun applyTokenLimitDraft() {
        val parsed = tokenLimitDraft.value.toIntOrNull()
        if (parsed == null) {
            message.value = UiMessage(
                message = "请输入 ${SettingsDefaults.MIN_TOKEN_LIMIT}-${SettingsDefaults.MAX_TOKEN_LIMIT} 之间的整数。",
            )
            return
        }
        applyTokenLimit(parsed)
    }

    fun updateHighCostAlert(enabled: Boolean) {
        viewModelScope.launch { repository.updateHighCostAlert(enabled) }
    }

    fun consumeMessage() {
        message.value = null
    }

    private fun availableProviders(): List<ProviderType> {
        return listOf(
            ProviderType.QWEN,
            ProviderType.ZHIPU,
            ProviderType.DEEPSEEK,
            ProviderType.CUSTOM,
        )
    }
}

data class SettingsUiState(
    val availableProviders: List<ProviderType> = listOf(
        ProviderType.QWEN,
        ProviderType.ZHIPU,
        ProviderType.DEEPSEEK,
        ProviderType.CUSTOM,
    ),
    val selectedModel: ModelDescriptor = ModelCatalog.defaultModel(),
    val availableModels: List<ModelDescriptor> = ModelCatalog.chatModels,
    val selectedStyle: AnswerStylePreset = AnswerStylePreset.DETAILED,
    val tokenLimit: Int = SettingsDefaults.DEFAULT_TOKEN_LIMIT,
    val tokenLimitDraft: String = SettingsDefaults.DEFAULT_TOKEN_LIMIT.toString(),
    val embeddingModel: ModelDescriptor = ModelCatalog.defaultEmbeddingModel(),
    val embeddingModels: List<ModelDescriptor> = ModelCatalog.embeddingModels,
    val embeddingVendor: ProviderType = ProviderType.QWEN,
    val speechToTextModel: ModelDescriptor = ModelCatalog.defaultSpeechToTextModel(),
    val speechToTextModels: List<ModelDescriptor> = ModelCatalog.speechToTextModels,
    val speechVendor: ProviderType = ProviderType.QWEN,
    val providerKeys: Map<ProviderType, String> = ProviderType.entries.associateWith { "" },
    val configuredProviders: Map<ProviderType, Boolean> = ProviderType.entries.associateWith { false },
    val customBaseUrl: String = SettingsDefaults.DEFAULT_CUSTOM_BASE_URL,
    val customChatModelId: String = SettingsDefaults.DEFAULT_CUSTOM_CHAT_MODEL,
    val customEmbeddingModelId: String = SettingsDefaults.DEFAULT_CUSTOM_EMBEDDING_MODEL,
    val remoteChatModels: List<ModelDescriptor> = emptyList(),
    val loadingModels: Boolean = false,
    val totalTokens: Int = 0,
    val totalCostUsd: Double = 0.0,
    val highCostAlertEnabled: Boolean = true,
    val message: UiMessage? = null,
)

private data class RemoteModelsState(
    val models: List<ModelDescriptor> = emptyList(),
    val loading: Boolean = false,
)

private data class CustomDrafts(
    val baseUrl: String,
    val chatModelId: String,
    val embeddingModelId: String,
    val remoteModels: List<ModelDescriptor> = emptyList(),
    val loadingModels: Boolean = false,
)

private data class VendorSelection(
    val embeddingVendor: ProviderType = ProviderType.QWEN,
    val speechVendor: ProviderType = ProviderType.QWEN,
    val tokenLimitDraft: String = SettingsDefaults.DEFAULT_TOKEN_LIMIT.toString(),
)
