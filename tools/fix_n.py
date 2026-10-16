import pathlib
P = pathlib.Path('core/data/src/main/java/com/kuaida/core/data/provider/OpenAiCompatibleProviders.kt')
lines = P.read_text(encoding='utf-8').splitlines(keepends=True)
hunks = []
hunks.append((345, 345, 'class CustomAuxProvider @Inject constructor(\n    private val userPreferencesDataSource: UserPreferencesDataSource,\n    promptComposer: PromptComposer,\n    client: OkHttpClient,\n    json: Json,\n    ioDispatcher: CoroutineDispatcher,\n) : OpenAiCompatibleProviderAdapter(\n    type = ProviderType.CUSTOM_AUX,\n    promptComposer = promptComposer,\n    client = client,\n    json = json,\n    ioDispatcher = ioDispatcher,\n), EmbeddingProvider, SpeechToTextProvider {\n\n    override suspend fun resolveBaseUrl(): String {\n        val prefs = userPreferencesDataSource.preferences.first()\n        val baseUrl = prefs.auxBaseUrl.trim()\n        require(baseUrl.isNotBlank()) { "请先填写自定义向量接口的 Base URL。" }\n        return baseUrl\n    }\n\n    override suspend fun embed(apiKey: String, request: EmbeddingRequest): EmbeddingResult {\n        require(request.modelId.isNotBlank()) { "请先填写自定义向量接口的向量模型。" }\n        return embedInternal(apiKey, request)\n    }\n\n    override suspend fun transcribeAudio(apiKey: String, request: AudioTranscriptionRequest): TranscriptionResult {\n        require(request.modelId.isNotBlank()) { "请先填写自定义向量接口的语音模型。" }\n        return transcribeAudioInternal(apiKey, request)\n    }\n}\n\n'))
out = []
prev = 0
for i1, i2, new in hunks:
    out.extend(lines[prev:i1])
    out.append(new)
    prev = i2
out.extend(lines[prev:])
P.write_text(''.join(out), encoding='utf-8')
print('ok n')
