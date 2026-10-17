import pathlib
P = pathlib.Path('feature/settings/src/main/java/com/kuaida/feature/settings/SettingsViewModel.kt')
lines = P.read_text(encoding='utf-8').splitlines(keepends=True)
hunks = []
hunks.append((38, 38, '    private val auxModelsState = MutableStateFlow(AuxModelsState())\n'))
hunks.append((57, 58, '        auxModelsState,\n    ) { baseUrl, embeddingModelId, speechModelId, tokenDraft, auxModels ->\n'))
hunks.append((63, 63, '            auxModels = auxModels.models,\n            loadingAuxModels = auxModels.loading,\n'))
hunks.append((110, 110, '            auxModels = auxDrafts.auxModels,\n            loadingAuxModels = auxDrafts.loadingAuxModels,\n'))
hunks.append((159, 164, '                when {\n                    draft.isNotBlank() && !draft.startsWith("••••") -> {\n                        val validation = repository.validateApiKey(ProviderType.CUSTOM_AUX, draft)\n                        if (!validation.success) {\n                            message.value = UiMessage(message = validation.message)\n                            return@launch\n                        }\n                        repository.saveApiKey(ProviderType.CUSTOM_AUX, draft)\n                        drafts.update { it + (ProviderType.CUSTOM_AUX to "••••••••••••" + draft.takeLast(4)) }\n'))
hunks.append((165, 168, '                    draft.isBlank() -> {\n                        val configured = repository.configuredProviders.first()[ProviderType.CUSTOM_AUX] == true\n                        if (!configured) {\n                            message.value = UiMessage(message = "接口配置已保存，但还没填 API Key，知识库和录音暂时仍然不可用。")\n                            return@launch\n                        }\n                    }\n'))
hunks.append((171, 171, '                val modelHint = if (embeddingModel.isBlank()) "（未填写向量模型）" else "向量模型 $embeddingModel"\n'))
hunks.append((172, 173, '                    message = "知识库与录音接口已保存，$modelHint。",\n'))
hunks.append((189, 190, '                        message.value = UiMessage(message = "没取到模型列表,请确认 Base URL 和 API Key 是否正确。")\n'))
hunks.append((203, 203, '            }\n        }\n    }\n\n    fun refreshAuxModels() {\n        if (auxModelsState.value.loading) return\n        viewModelScope.launch {\n            auxModelsState.update { it.copy(loading = true) }\n            val result = runCatching { repository.fetchAuxModels() }\n            auxModelsState.update { it.copy(loading = false) }\n            result.onSuccess { models ->\n                if (models.isEmpty()) {\n                    message.value = UiMessage(message = "没取到模型列表，请确认 Base URL 和 API Key 是否正确。")\n                } else {\n                    auxModelsState.update { it.copy(models = models) }\n                    message.value = UiMessage(message = "已获取 ${models.size} 个可用模型。")\n                }\n            }.onFailure { throwable ->\n                message.value = UiMessage(message = throwable.toUserMessage("获取模型列表失败，请检查 Base URL 和 API Key。"))\n'))
hunks.append((437, 437, '    val auxModels: List<ModelDescriptor> = emptyList(),\n    val loadingAuxModels: Boolean = false,\n'))
hunks.append((446, 446, '    val models: List<ModelDescriptor> = emptyList(),\n    val loading: Boolean = false,\n)\n\nprivate data class AuxModelsState(\n'))
hunks.append((463, 463, '    val auxModels: List<ModelDescriptor> = emptyList(),\n    val loadingAuxModels: Boolean = false,\n'))
out = []
prev = 0
for i1, i2, new in hunks:
    out.extend(lines[prev:i1])
    out.append(new)
    prev = i2
out.extend(lines[prev:])
P.write_text(''.join(out), encoding='utf-8')
print('ok v')
