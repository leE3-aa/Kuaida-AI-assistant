package com.kuaida.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuaida.core.common.error.toUserMessage
import com.kuaida.core.common.ui.UiMessage
import com.kuaida.core.data.repository.ChatRepository
import com.kuaida.core.model.InterviewSession
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val actionSessionIds = MutableStateFlow<Set<String>>(emptySet())
    private val message = MutableStateFlow<UiMessage?>(null)

    private val sessions = query.flatMapLatest(chatRepository::observeSessions)

    val uiState: StateFlow<HistoryUiState> = combine(
        query,
        sessions,
        actionSessionIds,
        message,
    ) { keyword, sessions, actionIds, message ->
        HistoryUiState(
            query = keyword,
            sessions = sessions,
            actionSessionIds = actionIds,
            message = message,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun updateQuery(value: String) {
        query.value = value
    }

    fun delete(session: InterviewSession) {
        if (session.id in actionSessionIds.value) return
        viewModelScope.launch {
            actionSessionIds.update { it + session.id }
            runCatching {
                chatRepository.deleteSession(session.id)
            }.onSuccess {
                message.value = UiMessage(message = "已删除这条历史记录。")
            }.onFailure { throwable ->
                message.value = UiMessage(message = throwable.toUserMessage("删除失败，请稍后重试。"))
            }
            actionSessionIds.update { it - session.id }
        }
    }

    fun consumeMessage() {
        message.value = null
    }
}

data class HistoryUiState(
    val query: String = "",
    val sessions: List<InterviewSession> = emptyList(),
    val actionSessionIds: Set<String> = emptySet(),
    val message: UiMessage? = null,
)
