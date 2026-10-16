import pathlib

P = pathlib.Path('feature/settings/src/main/java/com/kuaida/feature/settings/SettingsScreen.kt')
s = P.read_text(encoding='utf-8')

pairs = []

pairs.append((
    """import com.kuaida.core.common.ui.components.QuickAnswerSectionHeader
import com.kuaida.core.common.ui.theme.QuickAnswerTone""",
    """import com.kuaida.core.common.ui.components.QuickAnswerSectionHeader
import com.kuaida.core.common.ui.displayLabel
import com.kuaida.core.common.ui.theme.QuickAnswerTone""",
))

pairs.append((
    """            onQuickTokenLimit = viewModel::applyTokenLimit,
        )
    }
}""",
    """            onQuickTokenLimit = viewModel::applyTokenLimit,
            onEmbeddingVendorChange = viewModel::selectEmbeddingVendor,
            onEmbeddingKeyChange = { viewModel.updateDraftKey(state.embeddingVendor, it) },
            onClearEmbeddingKey = { viewModel.clearApiKey(state.embeddingVendor) },
            onSaveEmbedding = viewModel::saveEmbeddingConfig,
            onSpeechVendorChange = viewModel::selectSpeechVendor,
            onSpeechKeyChange = { viewModel.updateDraftKey(state.speechVendor, it) },
            onClearSpeechKey = { viewModel.clearApiKey(state.speechVendor) },
            onSaveSpeech = viewModel::saveSpeechConfig,
        )
    }
}""",
))

pairs.append((
    """    onQuickTokenLimit: (Int) -> Unit,
) {
    val baseUrlLabel = ModelCatalog.customProviderLabel(state.customBaseUrl)""",
    """    onQuickTokenLimit: (Int) -> Unit,
    onEmbeddingVendorChange: (ProviderType) -> Unit = {},
    onEmbeddingKeyChange: (String) -> Unit = {},
    onClearEmbeddingKey: () -> Unit = {},
    onSaveEmbedding: (ProviderType, String) -> Unit = { _, _ -> },
    onSpeechVendorChange: (ProviderType) -> Unit = {},
    onSpeechKeyChange: (String) -> Unit = {},
    onClearSpeechKey: () -> Unit = {},
    onSaveSpeech: (ProviderType, String) -> Unit = { _, _ -> },
) {
    val baseUrlLabel = ModelCatalog.customProviderLabel(state.customBaseUrl)""",
))

pairs.append((
    """    val configured = state.configuredProviders[ProviderType.CUSTOM] == true

    QuickAnswerScaffold(""",
    """    val configured = state.configuredProviders[ProviderType.CUSTOM] == true
    val vectorVendors = listOf(ProviderType.QWEN, ProviderType.ZHIPU)
    val embeddingOptions = ModelCatalog.embeddingByProvider(state.embeddingVendor)
    val speechOptions = ModelCatalog.speechToTextByProvider(state.speechVendor)
    var selectedEmbeddingModelId by remember(state.embeddingVendor) {
        mutableStateOf(
            state.embeddingModel
                .takeIf { it.provider == state.embeddingVendor }
                ?.modelId
                .orEmpty()
                .ifBlank { embeddingOptions.firstOrNull()?.modelId.orEmpty() },
        )
    }
    var selectedSpeechModelId by remember(state.speechVendor) {
        mutableStateOf(
            state.speechToTextModel
                .takeIf { it.provider == state.speechVendor }
                ?.modelId
                .orEmpty()
                .ifBlank { speechOptions.firstOrNull()?.modelId.orEmpty() },
        )
    }

    QuickAnswerScaffold(""",
))

for old, new in pairs:
    assert s.count(old) == 1, 'anchor missing: ' + old[:70]
    s = s.replace(old, new, 1)

P.write_text(s, encoding='utf-8')
print('ok g1', len(s))
