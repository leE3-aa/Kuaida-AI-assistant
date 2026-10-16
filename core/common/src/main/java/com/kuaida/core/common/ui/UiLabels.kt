package com.kuaida.core.common.ui

import com.kuaida.core.model.CostRecordCategory
import com.kuaida.core.model.DocumentParseStatus
import com.kuaida.core.model.ProviderType
import com.kuaida.core.model.QuestionSourceType
import com.kuaida.core.model.SessionStatus

fun ProviderType.displayLabel(): String {
    return when (this) {
        ProviderType.QWEN -> "阿里百炼"
        ProviderType.ZHIPU -> "智谱 AI"
        ProviderType.DEEPSEEK -> "DeepSeek"
        ProviderType.CUSTOM -> "自定义兼容接口"
        ProviderType.CUSTOM_AUX -> "自定义向量接口"
    }
}

fun QuestionSourceType.displayLabel(): String {
    return when (this) {
        QuestionSourceType.TEXT -> "手动输入"
        QuestionSourceType.AUDIO -> "语音转写"
    }
}

fun SessionStatus.displayLabel(): String {
    return when (this) {
        SessionStatus.IDLE -> "待处理"
        SessionStatus.GENERATING -> "生成中"
        SessionStatus.COMPLETED -> "已完成"
        SessionStatus.FAILED -> "失败"
    }
}

fun DocumentParseStatus.displayLabel(): String {
    return when (this) {
        DocumentParseStatus.PENDING -> "等待处理"
        DocumentParseStatus.PROCESSING -> "处理中"
        DocumentParseStatus.READY -> "可用"
        DocumentParseStatus.FAILED -> "处理失败"
    }
}

fun CostRecordCategory.displayLabel(): String {
    return when (this) {
        CostRecordCategory.ANSWER_GENERATION -> "回答生成"
        CostRecordCategory.AUDIO_TRANSCRIPTION -> "语音转写"
        CostRecordCategory.KNOWLEDGE_EMBEDDING -> "知识库向量化"
        CostRecordCategory.QUERY_EMBEDDING -> "问题检索向量"
    }
}
