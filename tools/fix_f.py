import pathlib

P = pathlib.Path('feature/settings/src/main/java/com/kuaida/feature/settings/SettingsViewModel.kt')
s = P.read_text(encoding='utf-8')

pairs = []

pairs.append((
    """    private val tokenLimitDraft = MutableStateFlow(SettingsDefaults.DEFAULT_TOKEN_LIMIT.toString())
    private val message = MutableStateFlow<UiMessage?>(null)""",
    """    private val tokenLimitDraft = MutableStateFlow(SettingsDefaults.DEFAULT_TOKEN_LIMIT.toString())
    private val embeddingVendor = MutableStateFlow(ProviderType.QWEN)
    private val speechVendor = MutableStateFlow(ProviderType.QWEN)
    private val message = MutableStateFlow<UiMessage?>(null)""",
))

pairs.append((
    """            remoteModels = remote.models,
            loadingModels = remote.loading,
        )
    }

    init {""",
    """            remoteModels = remote.models,
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

    init {""",
))

pairs.append((
    """        customProviderDrafts,
        tokenLimitDraft,
        message,
    ) { summary, keyDrafts, customDrafts, tokenDraft, uiMessage ->""",
    """        customProviderDrafts,
        vendorSelection,
        message,
    ) { summary, keyDrafts, customDrafts, vendorState, uiMessage ->""",
))

pairs.append((
    """            tokenLimitDraft = tokenDraft,
            embeddingModel = summary.embeddingModel,
            embeddingModels = ModelCatalog.embeddingModels,
            speechToTextModel = summary.speechToTextModel,
            speechToTextModels = ModelCatalog.speechToTextModels,""",
    """            tokenLimitDraft = vendorState.tokenLimitDraft,
            embeddingModel = summary.embeddingModel,
            embeddingModels = ModelCatalog.embeddingModels,
            embeddingVendor = vendorState.embeddingVendor,
            speechToTextModel = summary.speechToTextModel,
            speechToTextModels = ModelCatalog.speechToTextModels,
            speechVendor = vendorState.speechVendor,""",
))

NEW_FUNCS = """    fun selectEmbeddingVendor(provider: ProviderType) {
        embeddingVendor.value = provider
    }

    fun selectSpeechVendor(provider: ProviderType) {
        speechVendor.value = provider
    }

    fun saveEmbeddingConfig(provider: ProviderType, modelId: String) {
        viewModelScope.launch {
            runCatching {
                if (modelId.isBlank()) {
                    message.value = UiMessage(message = "\u8bf7\u5148\u9009\u62e9\u8981\u4f7f\u7528\u7684\u5411\u91cf\u6a21\u578b\u3002")
                    return@launch
                }
                val draft = drafts.value[provider].orEmpty().trim()
                if (draft.isNotBlank() && !draft.startsWith("\u2022\u2022\u2022\u2022")) {
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
                    message = "\u77e5\u8bc6\u5e93\u5411\u91cf\u63a5\u53e3\u5df2\u8bbe\u7f6e\u4e3a ${provider.displayLabel()} \u00b7 $modelId\u3002",
                )
            }.onFailure { throwable ->
                message.value = UiMessage(message = throwable.toUserMessage("\u4fdd\u5b58\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5\u3002"))
            }
        }
    }

    fun saveSpeechConfig(provider: ProviderType, modelId: String) {
        viewModelScope.launch {
            runCatching {
                if (modelId.isBlank()) {
                    message.value = UiMessage(message = "\u8bf7\u5148\u9009\u62e9\u8981\u4f7f\u7528\u7684\u8bed\u97f3\u8bc6\u522b\u6a21\u578b\u3002")
                    return@launch
                }
                val draft = drafts.value[provider].orEmpty().trim()
                if (draft.isNotBlank() && !draft.startsWith("\u2022\u2022\u2022\u2022")) {
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
                    message = "\u5f55\u97f3\u8f6c\u5199\u63a5\u53e3\u5df2\u8bbe\u7f6e\u4e3a ${provider.displayLabel()} \u00b7 $modelId\u3002",
                )
            }.onFailure { throwable ->
                message.value = UiMessage(message = throwable.toUserMessage("\u4fdd\u5b58\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5\u3002"))
            }
        }
    }

"""
pairs.append(("    fun selectProvider(provider: ProviderType) {", NEW_FUNCS + "    fun selectProvider(provider: ProviderType) {"))

pairs.append((
    """    val embeddingModels: List<ModelDescriptor> = ModelCatalog.embeddingModels,
    val speechToTextModel: ModelDescriptor = ModelCatalog.defaultSpeechToTextModel(),
    val speechToTextModels: List<ModelDescriptor> = ModelCatalog.speechToTextModels,""",
    """    val embeddingModels: List<ModelDescriptor> = ModelCatalog.embeddingModels,
    val embeddingVendor: ProviderType = ProviderType.QWEN,
    val speechToTextModel: ModelDescriptor = ModelCatalog.defaultSpeechToTextModel(),
    val speechToTextModels: List<ModelDescriptor> = ModelCatalog.speechToTextModels,
    val speechVendor: ProviderType = ProviderType.QWEN,""",
))

pairs.append((
    """private data class CustomDrafts(
    val baseUrl: String,
    val chatModelId: String,
    val embeddingModelId: String,
    val remoteModels: List<ModelDescriptor> = emptyList(),
    val loadingModels: Boolean = false,
)""",
    """private data class CustomDrafts(
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
)""",
))

for old, new in pairs:
    assert s.count(old) == 1, 'anchor missing: ' + old[:70]
    s = s.replace(old, new, 1)

P.write_text(s, encoding='utf-8')
print('ok f', len(s))
