import pathlib
P = pathlib.Path('core/data/src/main/java/com/kuaida/core/data/repository/SettingsRepository.kt')
lines = P.read_text(encoding='utf-8').splitlines(keepends=True)
hunks = []
hunks.append((32, 32, '    val userPreferences: Flow<com.kuaida.core.data.datastore.UserPreferences> = preferences.preferences\n\n'))
hunks.append((41, 42, '        resolveSpeechToTextModel(it)\n'))
hunks.append((182, 182, '    suspend fun updateAuxConfig(baseUrl: String, embeddingModelId: String, speechModelId: String) {\n        preferences.setAuxConfig(\n            baseUrl = baseUrl.trim(),\n            embeddingModelId = embeddingModelId.trim(),\n            speechModelId = speechModelId.trim(),\n        )\n    }\n\n'))
hunks.append((200, 202, '        return when (prefs.selectedEmbeddingProvider) {\n            ProviderType.CUSTOM -> ModelCatalog.customEmbeddingModel(\n'))
hunks.append((205, 207, '            ProviderType.CUSTOM_AUX -> ModelCatalog.customAuxEmbeddingModel(\n                baseUrl = prefs.auxBaseUrl,\n                modelId = prefs.auxEmbeddingModelId,\n            )\n            else -> ModelCatalog.findEmbedding(prefs.selectedEmbeddingProvider, prefs.selectedEmbeddingModelId)\n        }\n    }\n\n    private fun resolveSpeechToTextModel(prefs: com.kuaida.core.data.datastore.UserPreferences): ModelDescriptor {\n        return when (prefs.selectedSpeechToTextProvider) {\n            ProviderType.CUSTOM -> ModelCatalog.customSpeechToTextModel(\n                baseUrl = prefs.customBaseUrl,\n                modelId = SettingsDefaults.DEFAULT_CUSTOM_SPEECH_MODEL,\n            )\n            ProviderType.CUSTOM_AUX -> ModelCatalog.customAuxSpeechModel(\n                baseUrl = prefs.auxBaseUrl,\n                modelId = prefs.auxSpeechModelId,\n            )\n            else -> ModelCatalog.findSpeechToText(prefs.selectedSpeechToTextProvider, prefs.selectedSpeechToTextModelId)\n'))
out = []
prev = 0
for i1, i2, new in hunks:
    out.extend(lines[prev:i1])
    out.append(new)
    prev = i2
out.extend(lines[prev:])
P.write_text(''.join(out), encoding='utf-8')
print('ok p')
