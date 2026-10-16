import pathlib
P = pathlib.Path('feature/settings/src/main/java/com/kuaida/feature/settings/SettingsScreen.kt')
lines = P.read_text(encoding='utf-8').splitlines(keepends=True)
hunks = []
hunks.append((91, 99, '            onAuxBaseUrlChange = viewModel::updateAuxBaseUrlDraft,\n            onAuxEmbeddingModelChange = viewModel::updateAuxEmbeddingModelDraft,\n            onAuxSpeechModelChange = viewModel::updateAuxSpeechModelDraft,\n            onAuxKeyChange = { viewModel.updateDraftKey(ProviderType.CUSTOM_AUX, it) },\n            onClearAuxKey = { viewModel.clearApiKey(ProviderType.CUSTOM_AUX) },\n            onSaveAuxConfig = viewModel::saveAuxConfig,\n'))
hunks.append((119, 119, '    onAuxBaseUrlChange: (String) -> Unit = {},\n    onAuxEmbeddingModelChange: (String) -> Unit = {},\n    onAuxSpeechModelChange: (String) -> Unit = {},\n    onAuxKeyChange: (String) -> Unit = {},\n    onClearAuxKey: () -> Unit = {},\n    onSaveAuxConfig: () -> Unit = {},\n'))
hunks.append((136, 157, ''))
hunks.append((236, 238, '                    title = "知识库与录音接口",\n                    description = "可选。给向量和录音转写单独指定接口，不影响上面的聊天接口。",\n'))
hunks.append((240, 241, '                        "不配置时会退回关键词检索/语音转写不可用。当前向量模型：${state.embeddingModel.displayName}，当前转写模型：${state.speechToTextModel.displayName}",\n'))
hunks.append((245, 259, '                    SectionLabel("Base URL")\n                    OutlinedTextField(\n                        modifier = Modifier.fillMaxWidth(),\n                        value = state.auxBaseUrl,\n                        onValueChange = onAuxBaseUrlChange,\n                        label = { Text("Base URL") },\n                        placeholder = { Text("https://api.a6api.com/v1") },\n                        singleLine = true,\n                    )\n'))
hunks.append((263, 268, '                        value = state.providerKeys[ProviderType.CUSTOM_AUX].orEmpty(),\n                        configured = state.configuredProviders[ProviderType.CUSTOM_AUX] == true,\n                        onValueChange = onAuxKeyChange,\n                        onSave = onSaveAuxConfig,\n                        onClear = onClearAuxKey,\n'))
hunks.append((273, 290, '                    OutlinedTextField(\n'))
hunks.append((291, 304, '                        value = state.auxEmbeddingModelId,\n                        onValueChange = onAuxEmbeddingModelChange,\n                        label = { Text("向量模型 ID") },\n                        placeholder = { Text("BAAI/bge-m3") },\n                        singleLine = true,\n'))
hunks.append((308, 331, '                    SectionLabel("语音模型")\n                    OutlinedTextField(\n                        modifier = Modifier.fillMaxWidth(),\n                        value = state.auxSpeechModelId,\n                        onValueChange = onAuxSpeechModelChange,\n                        label = { Text("语音模型 ID") },\n                        placeholder = { Text("FunAudioLLM/SenseVoiceSmall") },\n                        singleLine = true,\n'))
hunks.append((335, 350, ''))
hunks.append((351, 353, '                        onClick = onSaveAuxConfig,\n                        enabled = state.auxBaseUrl.isNotBlank(),\n'))
hunks.append((355, 356, '                        Text("保存知识库与录音接口")\n'))
out = []
prev = 0
for i1, i2, new in hunks:
    out.extend(lines[prev:i1])
    out.append(new)
    prev = i2
out.extend(lines[prev:])
P.write_text(''.join(out), encoding='utf-8')
print('ok r')
