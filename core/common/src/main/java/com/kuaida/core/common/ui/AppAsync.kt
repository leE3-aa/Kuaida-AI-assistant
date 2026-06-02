package com.kuaida.core.common.ui

sealed interface AppAsync<out T> {
    data object Idle : AppAsync<Nothing>
    data object Loading : AppAsync<Nothing>
    data class Success<T>(val data: T) : AppAsync<T>
    data class Error(val message: String) : AppAsync<Nothing>
}
