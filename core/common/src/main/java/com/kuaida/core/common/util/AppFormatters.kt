package com.kuaida.core.common.util

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Long.toReadableDateTime(): String {
    val format = SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.CHINA)
    return format.format(Date(this))
}

fun Long.toReadableDate(): String {
    val format = SimpleDateFormat("yyyy年M月d日", Locale.CHINA)
    return format.format(Date(this))
}

fun Double.toUsd(): String {
    val format = DecimalFormat("¥#,##0.0000")
    return format.format(this)
}

fun Long.toReadableFileSize(): String {
    if (this <= 0L) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var size = toDouble()
    var unitIndex = 0
    while (size >= 1024 && unitIndex < units.lastIndex) {
        size /= 1024
        unitIndex++
    }
    val pattern = if (size >= 100 || unitIndex == 0) "#,##0" else "#,##0.0"
    return "${DecimalFormat(pattern).format(size)} ${units[unitIndex]}"
}
