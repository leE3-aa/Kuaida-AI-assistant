package com.kuaida.core.data.database

import com.kuaida.core.model.AnswerBlock
import com.kuaida.core.model.CostRecord
import com.kuaida.core.model.InterviewSession
import com.kuaida.core.model.KnowledgeChunk
import com.kuaida.core.model.KnowledgeDocument
import com.kuaida.core.model.UsageCost
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

fun ChatSessionWithCosts.toDomain(json: Json): InterviewSession {
    return InterviewSession(
        id = session.id,
        question = session.question,
        transcript = session.transcript,
        answer = session.answerJson?.let { json.decodeFromString(AnswerBlock.serializer(), it) },
        provider = session.provider,
        modelId = session.modelId,
        stylePreset = session.stylePreset,
        sourceType = session.sourceType,
        usage = costs.toUsageCost(),
        status = session.status,
        latencyMs = session.latencyMs,
        createdAt = session.createdAt,
        errorMessage = session.errorMessage,
    )
}

fun KnowledgeDocumentEntity.toDomain(): KnowledgeDocument {
    return KnowledgeDocument(
        id = id,
        displayName = displayName,
        mimeType = mimeType,
        sourceUri = sourceUri,
        sizeBytes = sizeBytes,
        parseStatus = parseStatus,
        chunkCount = chunkCount,
        embeddingCount = embeddingCount,
        failureReason = failureReason,
        isEnabled = isEnabled,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

fun KnowledgeChunkWithEmbedding.toDomain(json: Json): KnowledgeChunk {
    val embedding = embedding?.vectorJson?.let {
        runCatching { json.decodeFromString(ListSerializer(Float.serializer()), it) }.getOrDefault(emptyList())
    }.orEmpty()
    return KnowledgeChunk(
        id = chunk.id,
        documentId = chunk.documentId,
        chunkIndex = chunk.chunkIndex,
        content = chunk.content,
        tokenCount = chunk.tokenCount,
        embedding = embedding,
        contentHash = chunk.contentHash,
    )
}

fun CostRecordEntity.toDomain(): CostRecord {
    return CostRecord(
        id = id,
        sessionId = sessionId,
        documentId = documentId,
        chunkId = chunkId,
        category = category,
        provider = provider,
        modelId = modelId,
        usage = toUsageCost(),
        latencyMs = latencyMs,
        createdAt = createdAt,
    )
}

fun List<CostRecordEntity>.toUsageCost(): UsageCost {
    return fold(UsageCost()) { acc, item ->
        UsageCost(
            promptTokens = acc.promptTokens + item.promptTokens,
            completionTokens = acc.completionTokens + item.completionTokens,
            totalTokens = acc.totalTokens + item.totalTokens,
            estimatedCostUsd = acc.estimatedCostUsd + item.estimatedCostUsd,
        )
    }
}

fun CostRecordEntity.toUsageCost(): UsageCost {
    return UsageCost(
        promptTokens = promptTokens,
        completionTokens = completionTokens,
        totalTokens = totalTokens,
        estimatedCostUsd = estimatedCostUsd,
    )
}
