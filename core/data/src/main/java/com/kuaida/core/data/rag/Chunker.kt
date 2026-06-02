package com.kuaida.core.data.rag

import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Chunker @Inject constructor() {
    fun chunk(text: String, maxLength: Int = 700, overlap: Int = 120): List<ChunkedText> {
        val paragraphs = text.split("\n").map(String::trim).filter(String::isNotBlank)
        if (paragraphs.isEmpty()) return emptyList()
        val results = mutableListOf<ChunkedText>()
        val builder = StringBuilder()
        var index = 0

        paragraphs.forEach { paragraph ->
            if (builder.length + paragraph.length + 1 > maxLength && builder.isNotBlank()) {
                val content = builder.toString().trim()
                results += ChunkedText(index++, content, content.length / 2, sha256(content))
                val tail = content.takeLast(overlap)
                builder.clear()
                builder.append(tail).append('\n')
            }
            builder.append(paragraph).append('\n')
        }

        if (builder.isNotBlank()) {
            val content = builder.toString().trim()
            results += ChunkedText(index, content, content.length / 2, sha256(content))
        }
        return results
    }

    private fun sha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}

data class ChunkedText(
    val index: Int,
    val content: String,
    val tokenCount: Int,
    val hash: String,
)
