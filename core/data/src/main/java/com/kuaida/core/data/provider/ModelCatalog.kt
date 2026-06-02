package com.kuaida.core.data.provider

import com.kuaida.core.model.ModelDescriptor
import com.kuaida.core.model.ProviderType
import com.kuaida.core.model.SettingsDefaults
import java.net.URI

object ModelCatalog {
    val simplifiedChatModels = listOf(
        "deepseek-ai/DeepSeek-V3" to "默认推荐",
        "deepseek-ai/DeepSeek-R1" to "深度推理",
        "Qwen/Qwen2.5-72B-Instruct" to "稳定表达",
    )

    val chatModels = listOf(
        ModelDescriptor(
            provider = ProviderType.QWEN,
            modelId = "qwen-plus",
            displayName = "阿里百炼 - Qwen Plus",
            inputPricePer1M = 2.0,
            outputPricePer1M = 12.0,
        ),
        ModelDescriptor(
            provider = ProviderType.QWEN,
            modelId = "qwen-flash",
            displayName = "阿里百炼 - Qwen Flash",
            inputPricePer1M = 1.2,
            outputPricePer1M = 7.2,
        ),
        ModelDescriptor(
            provider = ProviderType.ZHIPU,
            modelId = "glm-4-plus",
            displayName = "智谱 AI - GLM-4-Plus",
            inputPricePer1M = 5.0,
            outputPricePer1M = 5.0,
        ),
        ModelDescriptor(
            provider = ProviderType.DEEPSEEK,
            modelId = "deepseek-chat",
            displayName = "DeepSeek - DeepSeek Chat",
            inputPricePer1M = 2.0,
            outputPricePer1M = 3.0,
        ),
        ModelDescriptor(
            provider = ProviderType.DEEPSEEK,
            modelId = "deepseek-reasoner",
            displayName = "DeepSeek - DeepSeek Reasoner",
            inputPricePer1M = 2.0,
            outputPricePer1M = 3.0,
        ),
    )

    val embeddingModels = listOf(
        ModelDescriptor(
            provider = ProviderType.QWEN,
            modelId = "text-embedding-v4",
            displayName = "阿里百炼 - text-embedding-v4",
            inputPricePer1M = 0.5,
            outputPricePer1M = 0.0,
            supportsEmbedding = true,
        ),
        ModelDescriptor(
            provider = ProviderType.QWEN,
            modelId = "text-embedding-v3",
            displayName = "阿里百炼 - text-embedding-v3",
            inputPricePer1M = 0.5,
            outputPricePer1M = 0.0,
            supportsEmbedding = true,
        ),
        ModelDescriptor(
            provider = ProviderType.ZHIPU,
            modelId = "embedding-3",
            displayName = "智谱 AI - Embedding-3",
            inputPricePer1M = 0.5,
            outputPricePer1M = 0.0,
            supportsEmbedding = true,
        ),
        ModelDescriptor(
            provider = ProviderType.ZHIPU,
            modelId = "embedding-2",
            displayName = "智谱 AI - Embedding-2",
            inputPricePer1M = 0.5,
            outputPricePer1M = 0.0,
            supportsEmbedding = true,
        ),
    )

    val speechToTextModels = listOf(
        ModelDescriptor(
            provider = ProviderType.QWEN,
            modelId = "qwen3-asr-flash",
            displayName = "阿里百炼 - Qwen3 ASR Flash",
            inputPricePer1M = 0.0,
            outputPricePer1M = 0.0,
            supportsTranscription = true,
        ),
        ModelDescriptor(
            provider = ProviderType.ZHIPU,
            modelId = "glm-asr-2512",
            displayName = "智谱 AI - GLM-ASR-2512",
            inputPricePer1M = 0.0,
            outputPricePer1M = 0.0,
            supportsTranscription = true,
        ),
    )

    val models: List<ModelDescriptor> = chatModels

    fun defaultModel(): ModelDescriptor = chatModels.first { it.provider == ProviderType.QWEN && it.modelId == "qwen-plus" }

    fun defaultModel(provider: ProviderType): ModelDescriptor {
        if (provider == ProviderType.CUSTOM) {
            return customChatModel()
        }
        return byProvider(provider).firstOrNull() ?: defaultModel()
    }

    fun defaultEmbeddingModel(): ModelDescriptor = embeddingModels.first { it.modelId == "text-embedding-v4" }

    fun defaultEmbeddingModel(provider: ProviderType): ModelDescriptor {
        if (provider == ProviderType.CUSTOM) {
            return customEmbeddingModel()
        }
        return embeddingByProvider(provider).firstOrNull() ?: defaultEmbeddingModel()
    }

    fun defaultSpeechToTextModel(): ModelDescriptor = speechToTextModels.first { it.modelId == "qwen3-asr-flash" }

    fun defaultSpeechToTextModel(provider: ProviderType): ModelDescriptor {
        if (provider == ProviderType.CUSTOM) {
            return customSpeechToTextModel()
        }
        return speechToTextByProvider(provider).firstOrNull() ?: defaultSpeechToTextModel()
    }

    fun findOrNull(provider: ProviderType, modelId: String): ModelDescriptor? {
        return chatModels.firstOrNull { it.provider == provider && it.modelId == modelId }
    }

    fun find(provider: ProviderType, modelId: String): ModelDescriptor {
        if (provider == ProviderType.CUSTOM) {
            return customChatModel(modelId = modelId)
        }
        return findOrNull(provider, modelId) ?: defaultModel()
    }

    fun byProvider(provider: ProviderType): List<ModelDescriptor> = chatModels.filter { it.provider == provider }

    fun findEmbeddingOrNull(provider: ProviderType, modelId: String): ModelDescriptor? {
        return embeddingModels.firstOrNull { it.provider == provider && it.modelId == modelId }
    }

    fun findEmbedding(provider: ProviderType, modelId: String): ModelDescriptor {
        if (provider == ProviderType.CUSTOM) {
            return customEmbeddingModel(modelId = modelId)
        }
        return findEmbeddingOrNull(provider, modelId) ?: defaultEmbeddingModel()
    }

    fun embeddingByProvider(provider: ProviderType): List<ModelDescriptor> {
        return embeddingModels.filter { it.provider == provider }
    }

    fun findSpeechToTextOrNull(provider: ProviderType, modelId: String): ModelDescriptor? {
        return speechToTextModels.firstOrNull { it.provider == provider && it.modelId == modelId }
    }

    fun findSpeechToText(provider: ProviderType, modelId: String): ModelDescriptor {
        if (provider == ProviderType.CUSTOM) {
            return customSpeechToTextModel(modelId = modelId)
        }
        return findSpeechToTextOrNull(provider, modelId) ?: defaultSpeechToTextModel()
    }

    fun speechToTextByProvider(provider: ProviderType): List<ModelDescriptor> {
        return speechToTextModels.filter { it.provider == provider }
    }

    fun customChatModel(
        baseUrl: String = "",
        modelId: String = "",
    ): ModelDescriptor {
        val normalizedModelId = modelId.trim()
        val providerLabel = customProviderLabel(baseUrl)
        val displayName = if (normalizedModelId.isBlank()) {
            "$providerLabel - 未设置模型"
        } else {
            "$providerLabel - $normalizedModelId"
        }
        return ModelDescriptor(
            provider = ProviderType.CUSTOM,
            modelId = normalizedModelId,
            displayName = displayName,
            inputPricePer1M = 0.0,
            outputPricePer1M = 0.0,
        )
    }

    fun customEmbeddingModel(
        baseUrl: String = "",
        modelId: String = "",
    ): ModelDescriptor {
        val normalizedModelId = modelId.trim()
        val providerLabel = customProviderLabel(baseUrl)
        val displayName = if (normalizedModelId.isBlank()) {
            "$providerLabel - 未设置向量模型"
        } else {
            "$providerLabel - $normalizedModelId"
        }
        return ModelDescriptor(
            provider = ProviderType.CUSTOM,
            modelId = normalizedModelId,
            displayName = displayName,
            inputPricePer1M = 0.0,
            outputPricePer1M = 0.0,
            supportsEmbedding = true,
        )
    }

    fun customSpeechToTextModel(
        baseUrl: String = "",
        modelId: String = SettingsDefaults.DEFAULT_CUSTOM_SPEECH_MODEL,
    ): ModelDescriptor {
        val normalizedModelId = modelId.trim()
        val providerLabel = customProviderLabel(baseUrl)
        val displayName = if (normalizedModelId.isBlank()) {
            "$providerLabel - 未设置语音模型"
        } else {
            "$providerLabel - $normalizedModelId"
        }
        return ModelDescriptor(
            provider = ProviderType.CUSTOM,
            modelId = normalizedModelId,
            displayName = displayName,
            inputPricePer1M = 0.0,
            outputPricePer1M = 0.0,
            supportsTranscription = true,
        )
    }

    fun simplifiedCustomChatModels(baseUrl: String = ""): List<ModelDescriptor> {
        return simplifiedChatModels.map { (modelId, label) ->
            customChatModel(baseUrl = baseUrl, modelId = modelId).copy(
                displayName = "$label · $modelId",
            )
        }
    }

    fun customProviderLabel(baseUrl: String): String {
        val normalized = baseUrl.trim()
        if (normalized.isBlank()) return "自定义兼容接口"
        val host = runCatching {
            val uri = if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
                URI(normalized)
            } else {
                URI("https://$normalized")
            }
            uri.host.orEmpty()
        }.getOrDefault("")
        return host
            .removePrefix("api.")
            .takeIf { it.isNotBlank() }
            ?: "自定义兼容接口"
    }
}
