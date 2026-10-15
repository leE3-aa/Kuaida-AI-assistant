import pathlib

P = pathlib.Path('core/data/src/main/java/com/kuaida/core/data/provider/OpenAiCompatibleProviders.kt')
s = P.read_text(encoding='utf-8')

A1 = "    protected fun executeJsonRequest("
N1 = """    protected suspend fun fetchModelsInternal(apiKey: String): List<ModelDescriptor> = withContext(ioDispatcher) {
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

""" + A1
assert s.count(A1) == 1, 'A1'
s = s.replace(A1, N1, 1)

O2 = """    override suspend fun listModels(apiKey: String): List<ModelDescriptor> {
        val prefs = userPreferencesDataSource.preferences.first()
        return listOf(
            ModelCatalog.customChatModel(
                baseUrl = prefs.customBaseUrl,
                modelId = prefs.customChatModelId.ifBlank { SettingsDefaults.DEFAULT_CUSTOM_CHAT_MODEL },
            ),
        )
    }"""
N2 = "    override suspend fun listModels(apiKey: String): List<ModelDescriptor> = fetchModelsInternal(apiKey)"
assert s.count(O2) == 1, 'O2'
s = s.replace(O2, N2, 1)

O3 = """@Serializable
private data class AudioTranscriptionResponse(
    val text: String,
)"""
N3 = O3 + """

@Serializable
private data class ModelListResponse(
    val data: List<ModelItem> = emptyList(),
) {
    @Serializable
    data class ModelItem(
        val id: String = "",
    )
}"""
assert s.count(O3) == 1, 'O3'
s = s.replace(O3, N3, 1)

P.write_text(s, encoding='utf-8')
print('ok a', len(s))
