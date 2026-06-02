package com.kuaida.feature.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.kuaida.core.data.provider.ModelCatalog
import com.kuaida.core.model.AnswerStylePreset
import com.kuaida.core.model.ProviderType
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun settingsScreen_showsMinimalConnectionFlow() {
        composeRule.setContent {
            MaterialTheme {
                SettingsScreen(
                    state = SettingsUiState(
                        selectedModel = ModelCatalog.defaultModel(),
                        selectedStyle = AnswerStylePreset.DETAILED,
                        tokenLimit = 1024,
                        embeddingModel = ModelCatalog.defaultEmbeddingModel(),
                        speechToTextModel = ModelCatalog.defaultSpeechToTextModel(),
                        configuredProviders = ProviderType.entries.associateWith { it == ProviderType.QWEN },
                    ),
                    snackbarHostState = SnackbarHostState(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    onKeyChange = { _ -> },
                    onSaveKey = {},
                    onClearKey = {},
                    onBaseUrlChange = { _ -> },
                    onModelChange = { _ -> },
                    onStyleChange = { _ -> },
                    onTokenLimitDraftChange = { _ -> },
                    onApplyTokenLimit = {},
                    onQuickTokenLimit = {},
                )
            }
        }

        composeRule.onNodeWithText("模型调用").assertIsDisplayed()
        composeRule.onNodeWithText("Base URL").assertIsDisplayed()
        composeRule.onNodeWithText("KEY").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_hidesAdvancedOptionsByDefault() {
        composeRule.setContent {
            MaterialTheme {
                SettingsScreen(
                    state = SettingsUiState(
                        selectedModel = ModelCatalog.defaultModel(),
                        selectedStyle = AnswerStylePreset.DETAILED,
                        tokenLimit = 1024,
                        embeddingModel = ModelCatalog.defaultEmbeddingModel(),
                        speechToTextModel = ModelCatalog.defaultSpeechToTextModel(),
                        configuredProviders = ProviderType.entries.associateWith { it == ProviderType.QWEN },
                    ),
                    snackbarHostState = SnackbarHostState(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    onKeyChange = { _ -> },
                    onSaveKey = {},
                    onClearKey = {},
                    onBaseUrlChange = { _ -> },
                    onModelChange = { _ -> },
                    onStyleChange = { _ -> },
                    onTokenLimitDraftChange = { _ -> },
                    onApplyTokenLimit = {},
                    onQuickTokenLimit = {},
                )
            }
        }

        composeRule.onAllNodesWithText("高级设置").assertCountEquals(0)
        composeRule.onAllNodesWithText("全部 Provider 凭证").assertCountEquals(0)
    }
}
