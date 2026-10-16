import pathlib
P = pathlib.Path('feature/settings/src/main/java/com/kuaida/feature/settings/SettingsViewModel.kt')
lines = P.read_text(encoding='utf-8').splitlines(keepends=True)
hunks = []
hunks.append((32, 32, '    private val auxBaseUrlDraft = MutableStateFlow(SettingsDefaults.DEFAULT_AUX_BASE_URL)\n    private val auxEmbeddingModelDraft = MutableStateFlow(SettingsDefaults.DEFAULT_AUX_EMBEDDING_MODEL)\n    private val auxSpeechModelDraft = MutableStateFlow(SettingsDefaults.DEFAULT_AUX_SPEECH_MODEL)\n'))
hunks.append((33, 35, ''))
hunks.append((51, 54, '    private val auxDrafts = combine(\n        auxBaseUrlDraft,\n        auxEmbeddingModelDraft,\n        auxSpeechModelDraft,\n'))
hunks.append((55, 59, '    ) { baseUrl, embeddingModelId, speechModelId, tokenDraft ->\n        AuxDrafts(\n            baseUrl = baseUrl,\n            embeddingModelId = embeddingModelId,\n            speechModelId = speechModelId,\n'))
hunks.append((74, 74, '            val prefs = repository.userPreferences.first()\n            auxBaseUrlDraft.value = prefs.auxBaseUrl\n            auxEmbeddingModelDraft.value = prefs.auxEmbeddingModelId\n            auxSpeechModelDraft.value = prefs.auxSpeechModelId\n'))
hunks.append((81, 82, '        auxDrafts,\n'))
hunks.append((83, 84, '    ) { summary, keyDrafts, customDrafts, auxDrafts, uiMessage ->\n'))
hunks.append((90, 91, '            tokenLimitDraft = auxDrafts.tokenLimitDraft,\n'))
hunks.append((93, 94, ''))
hunks.append((96, 97, ''))
hunks.append((102, 102, '            auxBaseUrl = auxDrafts.baseUrl,\n            auxEmbeddingModelId = auxDrafts.embeddingModelId,\n            auxSpeechModelId = auxDrafts.speechModelId,\n'))
hunks.append((125, 125, '    }\n\n    fun updateAuxBaseUrlDraft(value: String) {\n        auxBaseUrlDraft.value = value\n    }\n\n    fun updateAuxEmbeddingModelDraft(value: String) {\n        auxEmbeddingModelDraft.value = value\n    }\n\n    fun updateAuxSpeechModelDraft(value: String) {\n        auxSpeechModelDraft.value = value\n    }\n\n    fun saveAuxConfig() {\n        viewModelScope.launch {\n            runCatching {\n                val baseUrl = auxBaseUrlDraft.value.trim()\n                if (baseUrl.isBlank()) {\n                    message.value = UiMessage(message = "请先填写向量接口的 Base URL。")\n                    return@launch\n                }\n                val embeddingModel = auxEmbeddingModelDraft.value.trim()\n                val speechModel = auxSpeechModelDraft.value.trim()\n                val draft = drafts.value[ProviderType.CUSTOM_AUX].orEmpty().trim()\n                if (draft.isNotBlank() && !draft.startsWith("••••")) {\n                    val validation = repository.validateApiKey(ProviderType.CUSTOM_AUX, draft)\n                    if (!validation.success) {\n                        message.value = UiMessage(message = validation.message)\n                        return@launch\n                    }\n                    repository.saveApiKey(ProviderType.CUSTOM_AUX, draft)\n                    val masked = repository.maskedApiKeys()[ProviderType.CUSTOM_AUX].orEmpty()\n                    drafts.update { it + (ProviderType.CUSTOM_AUX to masked) }\n                }\n                repository.updateAuxConfig(baseUrl, embeddingModel, speechModel)\n                repository.updateSelectedEmbeddingModel(ProviderType.CUSTOM_AUX, embeddingModel)\n                repository.updateSelectedSpeechToTextModel(ProviderType.CUSTOM_AUX, speechModel)\n                message.value = UiMessage(\n                    message = "知识库与录音接口已保存，向量模型 $embeddingModel。",\n                )\n            }.onFailure { throwable ->\n                message.value = UiMessage(message = throwable.toUserMessage("保存失败，请稍后重试。"))\n            }\n        }\n'))
hunks.append((252, 316, ''))
hunks.append((438, 439, ''))
hunks.append((441, 442, ''))
hunks.append((447, 447, '    val auxBaseUrl: String = SettingsDefaults.DEFAULT_AUX_BASE_URL,\n    val auxEmbeddingModelId: String = SettingsDefaults.DEFAULT_AUX_EMBEDDING_MODEL,\n    val auxSpeechModelId: String = SettingsDefaults.DEFAULT_AUX_SPEECH_MODEL,\n'))
hunks.append((468, 471, 'private data class AuxDrafts(\n    val baseUrl: String,\n    val embeddingModelId: String,\n    val speechModelId: String,\n'))
out = []
prev = 0
for i1, i2, new in hunks:
    out.extend(lines[prev:i1])
    out.append(new)
    prev = i2
out.extend(lines[prev:])
P.write_text(''.join(out), encoding='utf-8')
print('ok q')
