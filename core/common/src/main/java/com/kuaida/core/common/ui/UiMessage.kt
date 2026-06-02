package com.kuaida.core.common.ui

data class UiMessage(
    val id: Long = System.currentTimeMillis(),
    val message: String,
)
