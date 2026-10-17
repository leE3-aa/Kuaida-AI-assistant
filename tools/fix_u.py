import pathlib
P = pathlib.Path('core/data/src/main/java/com/kuaida/core/data/repository/SettingsRepository.kt')
lines = P.read_text(encoding='utf-8').splitlines(keepends=True)
hunks = []
hunks.append((144, 144, '    suspend fun fetchAuxModels(): List<ModelDescriptor> {\n        val provider = providerRegistry.findEmbeddingProvider(ProviderType.CUSTOM_AUX) as? com.kuaida.core.data.provider.CustomAuxProvider ?: return emptyList()\n        val apiKey = getApiKeyOrNull(ProviderType.CUSTOM_AUX) ?: return emptyList()\n        return provider.listModels(apiKey)\n    }\n\n'))
out = []
prev = 0
for i1, i2, new in hunks:
    out.extend(lines[prev:i1])
    out.append(new)
    prev = i2
out.extend(lines[prev:])
P.write_text(''.join(out), encoding='utf-8')
print('ok u')
