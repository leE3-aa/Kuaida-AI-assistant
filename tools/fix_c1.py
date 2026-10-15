import pathlib

P = pathlib.Path('feature/settings/src/main/java/com/kuaida/feature/settings/SettingsViewModel.kt')
s = P.read_text(encoding='utf-8')

O1 = """    private val customProviderDrafts = combine(
        customBaseUrlDraft,
        customChatModelDraft,
        customEmbeddingModelDraft,
    ) { baseUrl, chatModelId, embeddingModelId ->
        Triple(baseUrl, chatModelId, embeddingModelId)
    }"""
N1 = """    private val remoteModelsState = MutableStateFlow(RemoteModelsState())
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
    }"""
assert s.count(O1) == 1, 'O1'
s = s.replace(O1, N1, 1)

O2 = """    ) { summary, keyDrafts, customDrafts, tokenDraft, uiMessage ->
        val (customBaseUrl, customChatModelId, customEmbeddingModelId) = customDrafts
        SettingsUiState("""
N2 = """    ) { summary, keyDrafts, customDrafts, tokenDraft, uiMessage ->
        SettingsUiState("""
assert s.count(O2) == 1, 'O2'
s = s.replace(O2, N2, 1)

O3 = """            customBaseUrl = customBaseUrl,
            customChatModelId = customChatModelId,
            customEmbeddingModelId = customEmbeddingModelId,"""
N3 = """            customBaseUrl = customDrafts.baseUrl,
            customChatModelId = customDrafts.chatModelId,
            customEmbeddingModelId = customDrafts.embeddingModelId,
            remoteChatModels = customDrafts.remoteModels,
            loadingModels = customDrafts.loadingModels,"""
assert s.count(O3) == 1, 'O3'
s = s.replace(O3, N3, 1)

P.write_text(s, encoding='utf-8')
print('ok c1', len(s))
