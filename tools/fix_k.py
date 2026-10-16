import pathlib
P = pathlib.Path('core/common/src/main/java/com/kuaida/core/common/ui/UiLabels.kt')
lines = P.read_text(encoding='utf-8').splitlines(keepends=True)
hunks = []
hunks.append((14, 14, '        ProviderType.CUSTOM_AUX -> "自定义向量接口"\n'))
out = []
prev = 0
for i1, i2, new in hunks:
    out.extend(lines[prev:i1])
    out.append(new)
    prev = i2
out.extend(lines[prev:])
P.write_text(''.join(out), encoding='utf-8')
print('ok k')
