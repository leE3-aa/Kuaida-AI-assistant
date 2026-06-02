package com.kuaida.core.common.error

import java.io.FileNotFoundException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

fun Throwable.toUserMessage(defaultMessage: String): String {
    firstChineseMessageOrNull()?.let { return it }

    val raw = message?.trim().orEmpty()
    val normalized = raw.lowercase()

    return when {
        this is FileNotFoundException ||
            (normalized.contains("not found") && normalized.contains("file")) -> {
            "文件不存在或已失效，请重新选择后重试。"
        }

        this is SocketTimeoutException ||
            normalized.contains("timeout") ||
            normalized.contains("timed out") -> {
            "请求超时，请稍后重试。"
        }

        this is UnknownHostException ||
            this is SSLException ||
            this is IOException ||
            normalized.contains("network") ||
            normalized.contains("connection") ||
            normalized.contains("failed to connect") ||
            normalized.contains("unable to resolve host") -> {
            "网络连接失败，请检查网络后重试。"
        }

        this is SecurityException ||
            normalized.contains("permission denied") ||
            normalized.contains("permission") -> {
            "当前缺少所需权限，请检查后重试。"
        }

        else -> defaultMessage
    }
}

private fun Throwable.firstChineseMessageOrNull(): String? {
    var current: Throwable? = this
    while (current != null) {
        val text = current.message?.trim().orEmpty()
        if (text.isNotBlank() && text.containsChinese()) return text
        current = current.cause
    }
    return null
}

private fun String.containsChinese(): Boolean = any { char ->
    Character.UnicodeScript.of(char.code) == Character.UnicodeScript.HAN
}
