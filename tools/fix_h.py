import pathlib

P = pathlib.Path('core/data/src/main/java/com/kuaida/core/data/provider/OpenAiCompatibleProviders.kt')
s = P.read_text(encoding='utf-8')

O1 = """            temperature = 0.4,
            maxTokens = request.maxOutputTokens,
        )"""
N1 = """            temperature = 0.4,
            maxTokens = request.maxOutputTokens,
            thinking = ThinkingOption(type = "disabled"),
        )"""
assert s.count(O1) == 1, 'O1'
s = s.replace(O1, N1, 1)

O2 = """    val temperature: Double,
    @SerialName("max_tokens") val maxTokens: Int? = null,
)"""
N2 = """    val temperature: Double,
    @SerialName("max_tokens") val maxTokens: Int? = null,
    @SerialName("thinking") val thinking: ThinkingOption,
)

@Serializable
private data class ThinkingOption(
    val type: String,
)"""
assert s.count(O2) == 1, 'O2'
s = s.replace(O2, N2, 1)

P.write_text(s, encoding='utf-8')
print('ok h', len(s))
