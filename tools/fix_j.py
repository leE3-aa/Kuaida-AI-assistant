import pathlib
P = pathlib.Path('core/model/src/main/java/com/kuaida/core/model/AppModels.kt')
lines = P.read_text(encoding='utf-8').splitlines(keepends=True)
hunks = []
hunks.append((9, 9, '    CUSTOM_AUX,\n'))
hunks.append((268, 268, '    const val DEFAULT_AUX_BASE_URL = ""\n    const val DEFAULT_AUX_EMBEDDING_MODEL = "BAAI/bge-m3"\n    const val DEFAULT_AUX_SPEECH_MODEL = "FunAudioLLM/SenseVoiceSmall"\n'))
out = []
prev = 0
for i1, i2, new in hunks:
    out.extend(lines[prev:i1])
    out.append(new)
    prev = i2
out.extend(lines[prev:])
P.write_text(''.join(out), encoding='utf-8')
print('ok j')
