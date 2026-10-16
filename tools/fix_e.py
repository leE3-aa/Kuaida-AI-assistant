import pathlib

P = pathlib.Path('core/data/src/main/java/com/kuaida/core/data/provider/ProviderErrors.kt')
s = P.read_text(encoding='utf-8')

HELPERS = """private fun String.toServerDetailFragment(): String {
    val collapsed = replace(Regex("\\\\s+"), " ").trim()
    return if (collapsed.length > 300) collapsed.take(300) + "\u2026" else collapsed
}

private fun serverDetailSuffix(statusCode: Int, body: String): String {
    val fragment = body.toServerDetailFragment()
    return if (fragment.isEmpty()) {
        "\uff08HTTP $statusCode\uff1a\u670d\u52a1\u7aef\u672a\u8fd4\u56de\u5185\u5bb9\uff09"
    } else {
        "\uff08HTTP $statusCode\uff1a$fragment\uff09"
    }
}

"""

ANCHOR = "internal fun ProviderType.toProviderServiceException(statusCode: Int, body: String): ProviderServiceException {"
assert s.count(ANCHOR) == 1, 'anchor'
s = s.replace(ANCHOR, HELPERS + ANCHOR, 1)

SUFFIX = ' + serverDetailSuffix(statusCode, body),'
for old in [
    'message = "$label \u670d\u52a1\u6682\u65f6\u4e0d\u53ef\u7528\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5\u3002",',
    'message = "$label \u8bf7\u6c42\u53c2\u6570\u6709\u8bef\uff0c\u8bf7\u68c0\u67e5\u8bbe\u7f6e\u540e\u91cd\u8bd5\u3002",',
    'message = "$label \u8bf7\u6c42\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5\u3002",',
]:
    assert s.count(old) == 1, old
    s = s.replace(old, old[:-1] + SUFFIX, 1)

P.write_text(s, encoding='utf-8')
print('ok e', len(s))
