import pathlib
P = pathlib.Path('core/data/src/main/java/com/kuaida/core/data/datastore/UserPreferencesDataSource.kt')
lines = P.read_text(encoding='utf-8').splitlines(keepends=True)
hunks = []
hunks.append((35, 35, '        val auxBaseUrl = stringPreferencesKey("aux_base_url")\n        val auxEmbeddingModelId = stringPreferencesKey("aux_embedding_model_id")\n        val auxSpeechModelId = stringPreferencesKey("aux_speech_model_id")\n'))
hunks.append((83, 83, '    suspend fun setAuxConfig(baseUrl: String, embeddingModelId: String, speechModelId: String) {\n        context.userPrefsDataStore.edit {\n            it[Keys.auxBaseUrl] = baseUrl.trim()\n            it[Keys.auxEmbeddingModelId] = embeddingModelId.trim()\n            it[Keys.auxSpeechModelId] = speechModelId.trim()\n        }\n    }\n\n'))
hunks.append((101, 101, '            auxBaseUrl = preferences[Keys.auxBaseUrl] ?: SettingsDefaults.DEFAULT_AUX_BASE_URL,\n            auxEmbeddingModelId = preferences[Keys.auxEmbeddingModelId] ?: SettingsDefaults.DEFAULT_AUX_EMBEDDING_MODEL,\n            auxSpeechModelId = preferences[Keys.auxSpeechModelId] ?: SettingsDefaults.DEFAULT_AUX_SPEECH_MODEL,\n'))
hunks.append((118, 118, '    val auxBaseUrl: String = SettingsDefaults.DEFAULT_AUX_BASE_URL,\n    val auxEmbeddingModelId: String = SettingsDefaults.DEFAULT_AUX_EMBEDDING_MODEL,\n    val auxSpeechModelId: String = SettingsDefaults.DEFAULT_AUX_SPEECH_MODEL,\n'))
out = []
prev = 0
for i1, i2, new in hunks:
    out.extend(lines[prev:i1])
    out.append(new)
    prev = i2
out.extend(lines[prev:])
P.write_text(''.join(out), encoding='utf-8')
print('ok l')
