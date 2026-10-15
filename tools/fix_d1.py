import pathlib

P = pathlib.Path('feature/settings/src/main/java/com/kuaida/feature/settings/SettingsScreen.kt')
s = P.read_text(encoding='utf-8')

pairs = []

pairs.append((
    """            onModelChange = viewModel::updateCustomChatModelDraft,
""",
    """            onModelChange = viewModel::updateCustomChatModelDraft,
            onRefreshModels = { viewModel.refreshRemoteModels() },
""",
))

pairs.append((
    """    onModelChange: (String) -> Unit,
""",
    """    onModelChange: (String) -> Unit,
    onRefreshModels: () -> Unit,
""",
))

pairs.append((
    """    val modelOptions = ModelCatalog.simplifiedCustomChatModels(state.customBaseUrl)
    val selectedModel = modelOptions.firstOrNull { it.modelId == state.customChatModelId } ?: modelOptions.first()
""",
    """    val builtInModelOptions = ModelCatalog.simplifiedCustomChatModels(state.customBaseUrl)
    val modelOptions = if (state.remoteChatModels.isEmpty()) {
        builtInModelOptions
    } else {
        (state.remoteChatModels + builtInModelOptions).distinctBy { it.modelId }
    }
""",
))

pairs.append((
    """                    ModelDropdown(
                        label = "模型",
                        selectedModel = selectedModel,
                        options = modelOptions,
                        onSelect = { onModelChange(it.modelId) },
                    )
                    HintLine("保留 3 个常用模型选项，减少切换成本；保存后下一次请求就会生效。")""",
    """                    ModelDropdown(
                        label = "模型",
                        value = state.customChatModelId,
                        options = modelOptions,
                        loading = state.loadingModels,
                        onValueChange = onModelChange,
                        onSelect = { onModelChange(it.modelId) },
                        onRefresh = onRefreshModels,
                    )
                    HintLine("可直接输入模型 ID，也可以拉取接口的全部可用模型。保存后下一次请求就会生效。")""",
))

pairs.append((
    """只保留最必要的连接项：Base URL、API Key 和 3 个模型选择。""",
    """只保留最必要的连接项：Base URL、API Key 和模型选择。""",
))

for old, new in pairs:
    assert s.count(old) == 1, old[:60]
    s = s.replace(old, new, 1)

P.write_text(s, encoding='utf-8')
print('ok d1', len(s))
