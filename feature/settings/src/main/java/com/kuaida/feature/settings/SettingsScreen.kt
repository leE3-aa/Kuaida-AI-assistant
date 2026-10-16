package com.kuaida.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kuaida.core.common.ui.components.QuickAnswerPill
import com.kuaida.core.common.ui.components.QuickAnswerScaffold
import com.kuaida.core.common.ui.components.QuickAnswerSectionCard
import com.kuaida.core.common.ui.components.QuickAnswerSectionHeader
import com.kuaida.core.common.ui.theme.QuickAnswerTone
import com.kuaida.core.data.provider.ModelCatalog
import com.kuaida.core.model.AnswerStylePreset
import com.kuaida.core.model.ModelDescriptor
import com.kuaida.core.model.ProviderType
import com.kuaida.core.model.SettingsDefaults

object SettingsRoute {
    const val route = "settings"

    @Composable
    operator fun invoke(
        contentPadding: PaddingValues,
        viewModel: SettingsViewModel = hiltViewModel(),
    ) {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(state.message?.id) {
            state.message?.let { snackbarHostState.showSnackbar(it.message) }
            viewModel.consumeMessage()
        }

        SettingsScreen(
            state = state,
            snackbarHostState = snackbarHostState,
            contentPadding = contentPadding,
            onKeyChange = { viewModel.updateDraftKey(ProviderType.CUSTOM, it) },
            onSaveKey = { viewModel.saveApiKey(ProviderType.CUSTOM) },
            onClearKey = { viewModel.clearApiKey(ProviderType.CUSTOM) },
            onBaseUrlChange = viewModel::updateCustomBaseUrlDraft,
            onModelChange = viewModel::updateCustomChatModelDraft,
            onRefreshModels = { viewModel.refreshRemoteModels() },
            onStyleChange = viewModel::updateStyle,
            onTokenLimitDraftChange = viewModel::updateTokenLimitDraft,
            onApplyTokenLimit = viewModel::applyTokenLimitDraft,
            onQuickTokenLimit = viewModel::applyTokenLimit,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SettingsScreen(
    state: SettingsUiState,
    snackbarHostState: SnackbarHostState,
    contentPadding: PaddingValues,
    onKeyChange: (String) -> Unit,
    onSaveKey: () -> Unit,
    onClearKey: () -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onRefreshModels: () -> Unit,
    onStyleChange: (AnswerStylePreset) -> Unit,
    onTokenLimitDraftChange: (String) -> Unit,
    onApplyTokenLimit: () -> Unit,
    onQuickTokenLimit: (Int) -> Unit,
) {
    val baseUrlLabel = ModelCatalog.customProviderLabel(state.customBaseUrl)
    val builtInModelOptions = ModelCatalog.simplifiedCustomChatModels(state.customBaseUrl)
    val modelOptions = if (state.remoteChatModels.isEmpty()) {
        builtInModelOptions
    } else {
        (state.remoteChatModels + builtInModelOptions).distinctBy { it.modelId }
    }
    val configured = state.configuredProviders[ProviderType.CUSTOM] == true

    QuickAnswerScaffold(
        title = "设置",
        snackbarHostState = snackbarHostState,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(contentPadding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                SettingsCard(
                    title = "模型调用",
                    description = "只保留最必要的连接项：Base URL、API Key 和模型选择。",
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "当前接口：$baseUrlLabel",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            HintLine("统一按 OpenAI 兼容接口调用，知识库和录音默认跟随这套配置。")
                        }
                        QuickAnswerPill(
                            text = if (configured) "已保存" else "待配置",
                            tone = if (configured) QuickAnswerTone.Positive else QuickAnswerTone.Tonal,
                        )
                    }

                    HorizontalDivider()

                    SectionLabel("Base URL")
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.customBaseUrl,
                        onValueChange = onBaseUrlChange,
                        label = { Text("Base URL") },
                        placeholder = { Text("例如 https://api.siliconflow.cn/v1") },
                        singleLine = true,
                    )

                    HorizontalDivider()

                    ProviderCredentialCard(
                        value = state.providerKeys[ProviderType.CUSTOM].orEmpty(),
                        configured = configured,
                        onValueChange = onKeyChange,
                        onSave = onSaveKey,
                        onClear = onClearKey,
                    )

                    HorizontalDivider()

                    ModelDropdown(
                        label = "模型",
                        value = state.customChatModelId,
                        options = modelOptions,
                        loading = state.loadingModels,
                        onValueChange = onModelChange,
                        onSelect = { onModelChange(it.modelId) },
                        onRefresh = onRefreshModels,
                    )
                    HintLine("可直接输入模型 ID，也可以拉取接口的全部可用模型。保存后下一次请求就会生效。")
                }
            }

            item {
                SettingsCard(
                    title = "回答偏好",
                    description = "回答风格和 token 上限只放在这里，不再在工作台重复展示。",
                ) {
                    SectionLabel("回答风格")
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AnswerStylePreset.entries.forEach { preset ->
                            FilterChip(
                                selected = state.selectedStyle == preset,
                                onClick = { onStyleChange(preset) },
                                label = { Text(preset.label) },
                            )
                        }
                    }

                    HorizontalDivider()

                    SectionLabel("输出 token 上限")
                    HintLine("当前值 ${state.tokenLimit}，会写入回答请求的 max_tokens。")
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(512, 1024, 2048, 4096).forEach { preset ->
                            FilterChip(
                                selected = state.tokenLimit == preset,
                                onClick = { onQuickTokenLimit(preset) },
                                label = { Text(preset.toString()) },
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            modifier = Modifier.weight(1f),
                            value = state.tokenLimitDraft,
                            onValueChange = onTokenLimitDraftChange,
                            label = { Text("自定义上限") },
                            placeholder = { Text("256 - 4096") },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done,
                            ),
                        )
                        Button(
                            onClick = onApplyTokenLimit,
                            enabled = state.tokenLimitDraft.isNotBlank(),
                            modifier = Modifier.widthIn(min = 92.dp),
                        ) {
                            Text("应用")
                        }
                    }
                    HintLine("建议范围 ${SettingsDefaults.MIN_TOKEN_LIMIT}-${SettingsDefaults.MAX_TOKEN_LIMIT}。超出范围会自动收敛。")
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    QuickAnswerSectionCard(tone = QuickAnswerTone.Tonal) {
        QuickAnswerSectionHeader(title = title, description = description)
        content()
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun HintLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelDropdown(
    label: String,
    value: String,
    options: List<ModelDescriptor>,
    loading: Boolean,
    onValueChange: (String) -> Unit,
    onSelect: (ModelDescriptor) -> Unit,
    onRefresh: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(label)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryEditable),
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text(SettingsDefaults.DEFAULT_CUSTOM_CHAT_MODEL) },
                singleLine = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                options.forEach { model ->
                    DropdownMenuItem(
                        text = { Text(model.displayName) },
                        onClick = {
                            onSelect(model)
                            expanded = false
                        },
                    )
                }
            }
        }
        OutlinedButton(
            onClick = onRefresh,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (loading) "正在获取模型列表…" else "获取接口可用模型")
        }
    }
}

@Composable
private fun ProviderCredentialCard(
    value: String,
    configured: Boolean,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    val canSave = configured || value.isNotBlank()
    val canClear = configured || value.isNotBlank()

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("API Key", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    text = if (configured) "状态：已验证" else "状态：未配置",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            QuickAnswerPill(
                text = if (configured) "已保存" else "待填写",
                tone = if (configured) QuickAnswerTone.Positive else QuickAnswerTone.Tonal,
            )
        }

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = value,
            onValueChange = onValueChange,
            label = { Text("KEY") },
            placeholder = { Text("粘贴 API Key") },
            singleLine = true,
            visualTransformation = if (visible || value.startsWith("••••")) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        imageVector = if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (visible) "隐藏 API Key" else "显示 API Key",
                        modifier = Modifier.size(20.dp),
                    )
                }
            },
        )

        HintLine(
            if (value.startsWith("••••")) {
                "当前显示的是已保存密钥的掩码。只改 Base URL 或模型时，直接点保存即可。"
            } else {
                "保存前会先联网校验，通过后再加密写入本地。"
            },
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onClear,
                enabled = canClear,
                modifier = Modifier.weight(1f),
            ) {
                Text("清空")
            }
            Button(
                onClick = onSave,
                enabled = canSave,
                modifier = Modifier.weight(1f),
            ) {
                Text("保存")
            }
        }
    }
}
