import pathlib
P = pathlib.Path('core/data/src/main/java/com/kuaida/core/data/provider/OpenAiCompatibleProviders.kt')
lines = P.read_text(encoding='utf-8').splitlines(keepends=True)
hunks = []
hunks.append((375, 375, '\n    suspend fun listModels(apiKey: String): List<ModelDescriptor> = fetchModelsInternal(apiKey)\n'))
out = []
prev = 0
for i1, i2, new in hunks:
    out.extend(lines[prev:i1])
    out.append(new)
    prev = i2
out.extend(lines[prev:])
P.write_text(''.join(out), encoding='utf-8')
print('ok t')
