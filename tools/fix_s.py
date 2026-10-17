import pathlib

P = pathlib.Path('feature/settings/src/main/java/com/kuaida/feature/settings/SettingsViewModel.kt')
s = P.read_text(encoding='utf-8')

OLD = """                val embeddingModel = auxEmbeddingModelDraft.value.trim()
                val speechModel = auxSpeechModelDraft.value.trim()
                val draft = drafts.value[ProviderType.CUSTOM_AUX].orEmpty().trim()
                if (draft.isNotBlank() && !draft.startsWith("\u2022\u2022\u2022\u2022")) {
                    val validation = repository.validateApiKey(ProviderType.CUSTOM_AUX, draft)
                    if (!validation.success) {
                        message.value = UiMessage(message = validation.message)
                        return@launch
                    }
                    repository.saveApiKey(ProviderType.CUSTOM_AUX, draft)
                    val masked = repository.maskedApiKeys()[ProviderType.CUSTOM_AUX].orEmpty()
                    drafts.update { it + (ProviderType.CUSTOM_AUX to masked) }
                }
                repository.updateAuxConfig(baseUrl, embeddingModel, speechModel)
"""

NEW = """                val embeddingModel = auxEmbeddingModelDraft.value.trim()
                val speechModel = auxSpeechModelDraft.value.trim()
                repository.updateAuxConfig(baseUrl, embeddingModel, speechModel)
                val draft = drafts.value[ProviderType.CUSTOM_AUX].orEmpty().trim()
                if (draft.isNotBlank() && !draft.startsWith("\u2022\u2022\u2022\u2022")) {
                    val validation = repository.validateApiKey(ProviderType.CUSTOM_AUX, draft)
                    if (!validation.success) {
                        message.value = UiMessage(message = validation.message)
                        return@launch
                    }
                    repository.saveApiKey(ProviderType.CUSTOM_AUX, draft)
                    val masked = repository.maskedApiKeys()[ProviderType.CUSTOM_AUX].orEmpty()
                    drafts.update { it + (ProviderType.CUSTOM_AUX to masked) }
                }
"""

assert s.count(OLD) == 1, 'anchor'
s = s.replace(OLD, NEW, 1)
P.write_text(s, encoding='utf-8')
print('ok s')
