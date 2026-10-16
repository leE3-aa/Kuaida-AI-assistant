import pathlib
P = pathlib.Path('core/data/src/main/java/com/kuaida/core/data/di/DataModule.kt')
lines = P.read_text(encoding='utf-8').splitlines(keepends=True)
hunks = []
hunks.append((172, 172, '\n    @Provides\n    @Singleton\n    fun provideCustomAuxProvider(\n        userPreferencesDataSource: com.kuaida.core.data.datastore.UserPreferencesDataSource,\n        promptComposer: com.kuaida.core.data.provider.PromptComposer,\n        client: OkHttpClient,\n        json: Json,\n        @IoDispatcher ioDispatcher: CoroutineDispatcher,\n    ): com.kuaida.core.data.provider.CustomAuxProvider = com.kuaida.core.data.provider.CustomAuxProvider(\n        userPreferencesDataSource = userPreferencesDataSource,\n        promptComposer = promptComposer,\n        client = client,\n        json = json,\n        ioDispatcher = ioDispatcher,\n    )\n\n    @Provides\n    @IntoSet\n    fun provideCustomAuxEmbeddingProvider(provider: com.kuaida.core.data.provider.CustomAuxProvider): EmbeddingProvider = provider\n\n    @Provides\n    @IntoSet\n    fun provideCustomAuxSpeechToTextProvider(provider: com.kuaida.core.data.provider.CustomAuxProvider): SpeechToTextProvider = provider\n'))
out = []
prev = 0
for i1, i2, new in hunks:
    out.extend(lines[prev:i1])
    out.append(new)
    prev = i2
out.extend(lines[prev:])
P.write_text(''.join(out), encoding='utf-8')
print('ok o')
