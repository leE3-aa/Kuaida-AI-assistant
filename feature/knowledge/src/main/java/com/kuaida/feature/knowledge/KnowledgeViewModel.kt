package com.kuaida.feature.knowledge

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuaida.core.common.error.toUserMessage
import com.kuaida.core.common.ui.UiMessage
import com.kuaida.core.common.ui.displayLabel
import com.kuaida.core.data.repository.KnowledgeRepository
import com.kuaida.core.model.KnowledgeDocument
import com.kuaida.core.model.KnowledgeImportResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class KnowledgeViewModel @Inject constructor(
    private val repository: KnowledgeRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val message = MutableStateFlow<UiMessage?>(null)
    private val isImporting = MutableStateFlow(false)
    private val actionDocumentIds = MutableStateFlow<Set<String>>(emptySet())

    val uiState: StateFlow<KnowledgeUiState> = combine(
        query,
        repository.observeDocuments(""),
        isImporting,
        actionDocumentIds,
        message,
    ) { keyword, docs, importing, actionIds, msg ->
        KnowledgeUiState(
            query = keyword,
            documents = if (keyword.isBlank()) docs else docs.filter { it.displayName.contains(keyword, true) },
            isImporting = importing,
            actionDocumentIds = actionIds,
            message = msg,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), KnowledgeUiState())

    fun updateQuery(value: String) {
        query.value = value
    }

    fun importDocument(uri: Uri) {
        if (isImporting.value) return
        viewModelScope.launch {
            isImporting.value = true
            runCatching { repository.importDocument(uri) }
                .onSuccess { result ->
                    message.value = UiMessage(message = result.toSuccessMessage())
                }
                .onFailure { message.value = UiMessage(message = it.toUserMessage("导入失败，请稍后重试。")) }
            isImporting.value = false
        }
    }

    fun toggleEnabled(document: KnowledgeDocument, enabled: Boolean) {
        updateDocumentAction(document.id) {
            repository.updateEnabled(document.id, enabled)
            message.value = UiMessage(
                message = if (enabled) {
                    "已启用《${document.displayName}》参与检索。"
                } else {
                    "已停用《${document.displayName}》的检索能力。"
                },
            )
        }
    }

    fun delete(document: KnowledgeDocument) {
        updateDocumentAction(document.id) {
            repository.delete(document.id)
            message.value = UiMessage(message = "已删除《${document.displayName}》。")
        }
    }

    fun consumeMessage() {
        message.value = null
    }

    private fun updateDocumentAction(
        documentId: String,
        action: suspend () -> Unit,
    ) {
        viewModelScope.launch {
            actionDocumentIds.update { it + documentId }
            runCatching { action() }
                .onFailure { message.value = UiMessage(message = it.toUserMessage("操作失败，请稍后重试。")) }
            actionDocumentIds.update { it - documentId }
        }
    }

    private fun KnowledgeImportResult.toSuccessMessage(): String {
        val summary = buildString {
            append("《")
            append(displayName)
            append("》已导入，共 ")
            append(chunkCount)
            append(" 个分片")
            if (embeddingCount > 0) {
                append("，已生成 ")
                append(embeddingCount)
                append(" 条 embedding")
                embeddingProvider?.let { provider ->
                    append("（")
                    append(provider.displayLabel())
                    embeddingModelId?.let { modelId ->
                        append(" / ")
                        append(modelId)
                    }
                    append("）")
                }
            }
        }
        return warningMessage?.let { "$summary。$it" } ?: "$summary。"
    }
}

data class KnowledgeUiState(
    val query: String = "",
    val documents: List<KnowledgeDocument> = emptyList(),
    val isImporting: Boolean = false,
    val actionDocumentIds: Set<String> = emptySet(),
    val message: UiMessage? = null,
)
