package com.kuaida.core.data.provider

import com.kuaida.core.model.AnswerBlock
import com.kuaida.core.model.AnswerGenerationRequest
import com.kuaida.core.model.AnswerStylePreset
import com.kuaida.core.model.FollowUpAction
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Singleton
class PromptComposer @Inject constructor() {
    fun systemPrompt(stylePreset: AnswerStylePreset): String = buildString {
        appendLine("你是一个面试口头回答助手。")
        appendLine("目标是帮助用户快速组织出可直接口述的结构化回答。")
        appendLine("如果提供了知识库片段，只吸收与当前问题直接相关的信息。")
        appendLine("不要捏造知识库中没有出现的具体事实、数据、经历或来源。")
        appendLine("请输出适合口述的结构化回答。")
        appendLine("必须严格返回 JSON，不要输出代码块或额外解释。")
        appendLine("""JSON 格式: {"conclusion":"", "keyPoints":["", ""], "expansion":""}""")
        when (stylePreset) {
            AnswerStylePreset.BRIEF -> appendLine("回答要简洁，控制在 45 秒内。")
            AnswerStylePreset.DETAILED -> appendLine("回答要完整，适合 90 秒左右表达。")
            AnswerStylePreset.SPOKEN -> appendLine("回答要自然口语化，避免书面腔。")
            AnswerStylePreset.ENGLISH -> appendLine("回答内容使用英语，但结构说明仍保持清晰。")
        }
    }

    fun userPrompt(request: AnswerGenerationRequest): String = buildString {
        appendLine("任务：请围绕下面的问题生成适合面试口头表达的回答。")
        appendLine("问题：${request.question}")
        if (request.ragContext.isNotEmpty()) {
            appendLine()
            appendLine("知识库参考（已按相关度排序，仅在相关时吸收）：")
            request.ragContext.forEachIndexed { index, chunk ->
                appendLine(
                    "${index + 1}. 来源=${chunk.documentName} | 分片=${chunk.chunkIndex + 1} | 相关度=${chunk.score.formatScore()}",
                )
                appendLine(chunk.content)
            }
            appendLine("使用要求：优先整合高相关片段，不要逐字照抄；如果片段不足以支撑结论，请给出稳妥、不过度编造的回答。")
        }
        request.previousAnswer?.let {
            appendLine()
            appendLine("当前已有回答：${Json.encodeToString(AnswerBlock.serializer(), it)}")
        }
        request.followUpAction?.let { action ->
            appendLine()
            appendLine(
                when (action) {
                    FollowUpAction.EXPAND_DETAIL -> "请在现有基础上展开细节。"
                    FollowUpAction.ADD_EXAMPLE -> "请补充一个贴近面试场景的例子。"
                    FollowUpAction.MORE_SPOKEN -> "请改得更像真实口头表达。"
                    FollowUpAction.REGENERATE -> "请重新生成一个更优版本。"
                },
            )
        }
    }

    fun parseAnswer(rawText: String): AnswerBlock {
        return runCatching {
            Json { ignoreUnknownKeys = true }.decodeFromString(AnswerBlock.serializer(), rawText.trim())
        }.getOrElse {
            val sections = rawText.lines().filter { line -> line.isNotBlank() }
            val conclusion = sections.firstOrNull().orEmpty()
            val keyPoints = sections.drop(1).take(3).ifEmpty { listOf("突出结论", "说明关键判断", "补充一条具体例子") }
            val expansion = sections.drop(4).joinToString("\n").ifBlank { rawText }
            AnswerBlock(conclusion = conclusion, keyPoints = keyPoints, expansion = expansion)
        }
    }

    private fun Double.formatScore(): String = String.format(Locale.US, "%.3f", this)
}
