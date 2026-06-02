package com.kuaida.core.data.rag

import com.kuaida.core.model.AnswerBlock
import com.kuaida.core.model.RetrievedChunk
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RagContextAssembler @Inject constructor() {
    fun assemble(
        question: String,
        retrievedChunks: List<RetrievedChunk>,
        maxOutputTokens: Int,
        previousAnswer: AnswerBlock? = null,
    ): RagContextAssembly {
        if (retrievedChunks.isEmpty()) return RagContextAssembly()

        val requestedBudget = (maxOutputTokens * CONTEXT_BUDGET_MULTIPLIER)
            .coerceIn(MIN_CONTEXT_BUDGET, MAX_CONTEXT_BUDGET)
        val reservedBudget = FIXED_PROMPT_OVERHEAD +
            approximateTokens(question) +
            approximateTokens(previousAnswer.toPromptText())
        val contextBudget = (requestedBudget - reservedBudget).coerceAtLeast(MIN_TRUNCATED_CHUNK_TOKENS)

        val selectedChunks = mutableListOf<RetrievedChunk>()
        var consumedTokens = 0
        var truncatedChunkCount = 0

        for (chunk in retrievedChunks) {
            if (selectedChunks.size >= MAX_CONTEXT_CHUNKS) break
            val remainingTokens = contextBudget - consumedTokens
            if (remainingTokens <= 0) break

            val normalizedChunk = chunk.normalize()
            if (normalizedChunk.tokenCount <= remainingTokens) {
                selectedChunks += normalizedChunk
                consumedTokens += normalizedChunk.tokenCount
                continue
            }

            val truncatedChunk = normalizedChunk.truncateTo(remainingTokens) ?: break
            selectedChunks += truncatedChunk
            consumedTokens += truncatedChunk.tokenCount
            truncatedChunkCount += 1
            break
        }

        return RagContextAssembly(
            chunks = selectedChunks,
            usedTokens = consumedTokens,
            truncatedChunkCount = truncatedChunkCount,
            droppedChunkCount = (retrievedChunks.size - selectedChunks.size).coerceAtLeast(0),
        )
    }

    private fun RetrievedChunk.normalize(): RetrievedChunk {
        val normalizedContent = content.replace(Regex("\n{3,}"), "\n\n").trim()
        val normalizedTokens = tokenCount.takeIf { it > 0 } ?: approximateTokens(normalizedContent)
        return copy(content = normalizedContent, tokenCount = normalizedTokens)
    }

    private fun RetrievedChunk.truncateTo(remainingTokens: Int): RetrievedChunk? {
        if (remainingTokens < MIN_TRUNCATED_CHUNK_TOKENS) return null

        val maxChars = (remainingTokens * CHARS_PER_TOKEN).coerceAtLeast(MIN_TRUNCATED_CHARS)
        val rawSnippet = content.take(maxChars).trimEnd()
        if (rawSnippet.isBlank()) return null

        val cutIndex = preferredCutIndex(rawSnippet)
        val truncatedContent = buildString {
            append(rawSnippet.take(cutIndex).trimEnd())
            append("\n[内容已截断]")
        }
        return copy(
            content = truncatedContent,
            tokenCount = approximateTokens(truncatedContent),
        )
    }

    private fun preferredCutIndex(text: String): Int {
        val minBoundary = (text.length * 0.6).toInt()
        val boundaryChars = listOf('\n', '。', '！', '？', '；', '.', '!', '?', ';')
        return boundaryChars.maxOf { char ->
            text.lastIndexOf(char).takeIf { it >= minBoundary } ?: -1
        }.takeIf { it > 0 } ?: text.length
    }

    private fun approximateTokens(text: String): Int {
        if (text.isBlank()) return 0
        return (text.length / CHARS_PER_TOKEN).coerceAtLeast(1)
    }

    private fun AnswerBlock?.toPromptText(): String {
        val answer = this ?: return ""
        return buildString {
            append(answer.conclusion)
            if (answer.keyPoints.isNotEmpty()) {
                append('\n')
                append(answer.keyPoints.joinToString(separator = "\n"))
            }
            if (answer.expansion.isNotBlank()) {
                append('\n')
                append(answer.expansion)
            }
        }
    }

    private companion object {
        private const val CONTEXT_BUDGET_MULTIPLIER = 2
        private const val MIN_CONTEXT_BUDGET = 320
        private const val MAX_CONTEXT_BUDGET = 1400
        private const val FIXED_PROMPT_OVERHEAD = 180
        private const val MAX_CONTEXT_CHUNKS = 4
        private const val MIN_TRUNCATED_CHUNK_TOKENS = 72
        private const val CHARS_PER_TOKEN = 2
        private const val MIN_TRUNCATED_CHARS = 160
    }
}

data class RagContextAssembly(
    val chunks: List<RetrievedChunk> = emptyList(),
    val usedTokens: Int = 0,
    val truncatedChunkCount: Int = 0,
    val droppedChunkCount: Int = 0,
)
