import pathlib
P = pathlib.Path('feature/settings/src/main/java/com/kuaida/feature/settings/SettingsScreen.kt')
lines = P.read_text(encoding='utf-8').splitlines(keepends=True)
hunks = []
hunks.append((97, 97, '            onRefreshAuxModels = { viewModel.refreshAuxModels() },\n'))
hunks.append((123, 123, '    onRefreshAuxModels: () -> Unit = {},\n'))
hunks.append((133, 139, '    val modelOptions = state.remoteChatModels\n'))
hunks.append((250, 252, '                    OutlinedButton(\n                        onClick = onRefreshAuxModels,\n                        enabled = !state.loadingAuxModels,\n'))
hunks.append((253, 253, '                    ) {\n                        Text(if (state.loadingAuxModels) "正在获取模型列表…" else "获取接口可用模型")\n                    }\n\n                    HorizontalDivider()\n\n                    ModelDropdown(\n                        label = "向量模型",\n'))
hunks.append((254, 254, '                        options = state.auxModels,\n                        loading = state.loadingAuxModels,\n'))
hunks.append((255, 258, '                        onSelect = { onAuxEmbeddingModelChange(it.modelId) },\n                        onRefresh = {},\n                        showRefresh = false,\n'))
hunks.append((262, 265, '                    ModelDropdown(\n                        label = "语音模型",\n'))
hunks.append((266, 266, '                        options = state.auxModels,\n                        loading = state.loadingAuxModels,\n'))
hunks.append((267, 270, '                        onSelect = { onAuxSpeechModelChange(it.modelId) },\n                        onRefresh = {},\n                        showRefresh = false,\n'))
hunks.append((391, 391, '    showRefresh: Boolean = true,\n'))
hunks.append((427, 433, '        if (showRefresh) {\n            OutlinedButton(\n                onClick = onRefresh,\n                enabled = !loading,\n                modifier = Modifier.fillMaxWidth(),\n            ) {\n                Text(if (loading) "正在获取模型列表…" else "获取接口可用模型")\n            }\n'))
out = []
prev = 0
for i1, i2, new in hunks:
    out.extend(lines[prev:i1])
    out.append(new)
    prev = i2
out.extend(lines[prev:])
P.write_text(''.join(out), encoding='utf-8')
print('ok w')
