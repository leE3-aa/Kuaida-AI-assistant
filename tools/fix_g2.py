import pathlib

P = pathlib.Path('feature/settings/src/main/java/com/kuaida/feature/settings/SettingsScreen.kt')
s = P.read_text(encoding='utf-8')

OLD = """                    HintLine("可直接输入模型 ID，也可以拉取接口的全部可用模型。保存后下一次请求就会生效。")
                }
            }
"""

NEW = OLD + """
            item {
                SettingsCard(
                    title = "知识库向量接口",
                    description = "可选。给知识库单独指定向量接口，不影响上面的聊天接口。",
                ) {
                    HintLine(
                        "不配置时会退回关键词检索，召回效果会差一些。当前向量模型：${state.embeddingModel.displayName}",
                    )

                    HorizontalDivider()

                    SectionLabel("厂商")
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        vectorVendors.forEach { vendor ->
                            FilterChip(
                                selected = state.embeddingVendor == vendor,
                                onClick = { onEmbeddingVendorChange(vendor) },
                                label = { Text(vendor.displayLabel()) },
                            )
                        }
                    }
                    HintLine("DeepSeek 没有向量接口，所以这里只能选阿里百炼或智谱 AI。")

                    HorizontalDivider()

                    ProviderCredentialCard(
                        value = state.providerKeys[state.embeddingVendor].orEmpty(),
                        configured = state.configuredProviders[state.embeddingVendor] == true,
                        onValueChange = onEmbeddingKeyChange,
                        onSave = { onSaveEmbedding(state.embeddingVendor, selectedEmbeddingModelId) },
                        onClear = onClearEmbeddingKey,
                    )

                    HorizontalDivider()

                    SectionLabel("向量模型")
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        embeddingOptions.forEach { model ->
                            FilterChip(
                                selected = selectedEmbeddingModelId == model.modelId,
                                onClick = { selectedEmbeddingModelId = model.modelId },
                                label = { Text(model.displayName) },
                            )
                        }
                    }
                    HintLine("模型要跟着上面的厂商走。保存后，知识库入库和问题检索都会改用这个模型。")

                    Button(
                        onClick = { onSaveEmbedding(state.embeddingVendor, selectedEmbeddingModelId) },
                        enabled = selectedEmbeddingModelId.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("保存知识库向量接口")
                    }
                }
            }

            item {
                SettingsCard(
                    title = "录音转写接口",
                    description = "可选。给录音转写单独指定接口，不影响上面的聊天接口。",
                ) {
                    HintLine(
                        "不配置时语音转写不可用，只能手动输入问题。当前转写模型：${state.speechToTextModel.displayName}",
                    )

                    HorizontalDivider()

                    SectionLabel("厂商")
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        vectorVendors.forEach { vendor ->
                            FilterChip(
                                selected = state.speechVendor == vendor,
                                onClick = { onSpeechVendorChange(vendor) },
                                label = { Text(vendor.displayLabel()) },
                            )
                        }
                    }
                    HintLine("DeepSeek 没有语音识别接口，所以这里只能选阿里百炼或智谱 AI。")

                    HorizontalDivider()

                    ProviderCredentialCard(
                        value = state.providerKeys[state.speechVendor].orEmpty(),
                        configured = state.configuredProviders[state.speechVendor] == true,
                        onValueChange = onSpeechKeyChange,
                        onSave = { onSaveSpeech(state.speechVendor, selectedSpeechModelId) },
                        onClear = onClearSpeechKey,
                    )

                    HorizontalDivider()

                    SectionLabel("转写模型")
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        speechOptions.forEach { model ->
                            FilterChip(
                                selected = selectedSpeechModelId == model.modelId,
                                onClick = { selectedSpeechModelId = model.modelId },
                                label = { Text(model.displayName) },
                            )
                        }
                    }
                    HintLine("模型要跟着上面的厂商走。保存后，下一次录音转写就会改用这个模型。")

                    Button(
                        onClick = { onSaveSpeech(state.speechVendor, selectedSpeechModelId) },
                        enabled = selectedSpeechModelId.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("保存录音转写接口")
                    }
                }
            }
"""

assert s.count(OLD) == 1, 'anchor'
s = s.replace(OLD, NEW, 1)

P.write_text(s, encoding='utf-8')
print('ok g2', len(s))
