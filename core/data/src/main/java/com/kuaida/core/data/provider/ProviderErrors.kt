package com.kuaida.core.data.provider

import com.kuaida.core.common.ui.displayLabel
import com.kuaida.core.model.ProviderType

internal class ProviderServiceException(
    val provider: ProviderType,
    val isCredentialError: Boolean,
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)

internal fun Throwable.findProviderServiceException(): ProviderServiceException? {
    var current: Throwable? = this
    while (current != null) {
        if (current is ProviderServiceException) return current
        current = current.cause
    }
    return null
}

internal fun ProviderType.toProviderServiceException(statusCode: Int, body: String): ProviderServiceException {
    val normalized = body.lowercase()
    val label = displayLabel()

    return when {
        statusCode == 401 ||
            statusCode == 403 ||
            normalized.contains("invalid_api_key") ||
            normalized.contains("apikey-error") ||
            normalized.contains("incorrect api key") ||
            normalized.contains("api key provided") -> {
            ProviderServiceException(
                provider = this,
                isCredentialError = true,
                message = "$label API Key 无效，请到设置页重新填写并保存。",
            )
        }

        statusCode == 429 ||
            normalized.contains("rate limit") ||
            normalized.contains("too many requests") -> {
            ProviderServiceException(
                provider = this,
                isCredentialError = false,
                message = "$label 请求过于频繁，请稍后再试。",
            )
        }

        normalized.contains("insufficient_quota") ||
            normalized.contains("quota") ||
            normalized.contains("余额") ||
            normalized.contains("arrears") ||
            normalized.contains("billing") -> {
            ProviderServiceException(
                provider = this,
                isCredentialError = false,
                message = "$label 额度不足，请检查账号余额或配额。",
            )
        }

        normalized.contains("model_not_found") ||
            normalized.contains("model not found") ||
            normalized.contains("no such model") ||
            normalized.contains("unsupported model") -> {
            ProviderServiceException(
                provider = this,
                isCredentialError = false,
                message = "$label 当前模型不可用，请到设置页更换模型后重试。",
            )
        }

        normalized.contains("unsupported file") ||
            normalized.contains("unsupported audio") ||
            normalized.contains("invalid file format") ||
            normalized.contains("invalid audio") ||
            normalized.contains("audio format") ||
            statusCode == 415 -> {
            ProviderServiceException(
                provider = this,
                isCredentialError = false,
                message = "录音格式暂不支持，请重录后再试。",
            )
        }

        normalized.contains("too large") ||
            normalized.contains("file size") ||
            normalized.contains("content too large") ||
            statusCode == 413 -> {
            ProviderServiceException(
                provider = this,
                isCredentialError = false,
                message = "提交的内容过大，请精简后重试。",
            )
        }

        normalized.contains("context length") ||
            normalized.contains("max_tokens") ||
            normalized.contains("maximum context") -> {
            ProviderServiceException(
                provider = this,
                isCredentialError = false,
                message = "输入内容过长，请精简后重试。",
            )
        }

        statusCode in 500..599 -> {
            ProviderServiceException(
                provider = this,
                isCredentialError = false,
                message = "$label 服务暂时不可用，请稍后重试。",
            )
        }

        statusCode == 400 -> {
            ProviderServiceException(
                provider = this,
                isCredentialError = false,
                message = "$label 请求参数有误，请检查设置后重试。",
            )
        }

        else -> {
            ProviderServiceException(
                provider = this,
                isCredentialError = false,
                message = "$label 请求失败，请稍后重试。",
            )
        }
    }
}
