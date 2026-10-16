import pathlib
P = pathlib.Path('core/data/src/main/java/com/kuaida/core/data/provider/ModelCatalog.kt')
lines = P.read_text(encoding='utf-8').splitlines(keepends=True)
hunks = []
hunks.append((240, 240, '    fun customAuxEmbeddingModel(\n        baseUrl: String = "",\n        modelId: String = "",\n    ): ModelDescriptor {\n        val normalizedModelId = modelId.trim()\n        val providerLabel = customProviderLabel(baseUrl)\n        val displayName = if (normalizedModelId.isBlank()) {\n            "$providerLabel - 未设置向量模型"\n        } else {\n            "$providerLabel - $normalizedModelId"\n        }\n        return ModelDescriptor(\n            provider = ProviderType.CUSTOM_AUX,\n            modelId = normalizedModelId,\n            displayName = displayName,\n            inputPricePer1M = 0.0,\n            outputPricePer1M = 0.0,\n            supportsEmbedding = true,\n        )\n    }\n\n    fun customAuxSpeechModel(\n        baseUrl: String = "",\n        modelId: String = "",\n    ): ModelDescriptor {\n        val normalizedModelId = modelId.trim()\n        val providerLabel = customProviderLabel(baseUrl)\n        val displayName = if (normalizedModelId.isBlank()) {\n            "$providerLabel - 未设置语音模型"\n        } else {\n            "$providerLabel - $normalizedModelId"\n        }\n        return ModelDescriptor(\n            provider = ProviderType.CUSTOM_AUX,\n            modelId = normalizedModelId,\n            displayName = displayName,\n            inputPricePer1M = 0.0,\n            outputPricePer1M = 0.0,\n            supportsTranscription = true,\n        )\n    }\n\n'))
out = []
prev = 0
for i1, i2, new in hunks:
    out.extend(lines[prev:i1])
    out.append(new)
    prev = i2
out.extend(lines[prev:])
P.write_text(''.join(out), encoding='utf-8')
print('ok m')
