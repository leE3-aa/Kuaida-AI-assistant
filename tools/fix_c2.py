import pathlib

P = pathlib.Path('feature/settings/src/main/java/com/kuaida/feature/settings/SettingsViewModel.kt')
s = P.read_text(encoding='utf-8')

O1 = """    fun updateCustomEmbeddingModelDraft(value: String) {
        customEmbeddingModelDraft.value = value
    }
"""
N1 = O1 + """
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
"""
assert s.count(O1) == 1, 'O1'
s = s.replace(O1, N1, 1)

O2 = """                repository.saveApiKey(provider, draft)
                val masked = repository.maskedApiKeys()[provider].orEmpty()
                drafts.update { it + (provider to masked) }"""
N2 = O2 + """
                if (provider == ProviderType.CUSTOM && draft.isNotBlank()) {
                    refreshRemoteModels(silent = true)
                }"""
assert s.count(O2) == 1, 'O2'
s = s.replace(O2, N2, 1)

O3 = """    val customEmbeddingModelId: String = SettingsDefaults.DEFAULT_CUSTOM_EMBEDDING_MODEL,"""
N3 = O3 + """
    val remoteChatModels: List<ModelDescriptor> = emptyList(),
    val loadingModels: Boolean = false,"""
assert s.count(O3) == 1, 'O3'
s = s.replace(O3, N3, 1)

O4 = """    val message: UiMessage? = null,
)"""
N4 = O4 + """

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
)"""
assert s.count(O4) == 1, 'O4'
s = s.replace(O4, N4, 1)

P.write_text(s, encoding='utf-8')
print('ok c2', len(s))
