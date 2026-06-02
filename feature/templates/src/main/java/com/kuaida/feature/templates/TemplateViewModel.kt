package com.kuaida.feature.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuaida.core.data.repository.TemplateRepository
import com.kuaida.core.model.TemplateItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class TemplateViewModel @Inject constructor(
    private val repository: TemplateRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")

    val uiState: StateFlow<TemplateUiState> = query.flatMapLatest { keyword ->
        repository.observeTemplates(keyword).map { TemplateUiState(query = keyword, items = it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TemplateUiState())

    fun updateQuery(value: String) {
        query.value = value
    }

    fun toggleFavorite(id: String) {
        viewModelScope.launch { repository.toggleFavorite(id) }
    }
}

data class TemplateUiState(
    val query: String = "",
    val items: List<TemplateItem> = emptyList(),
)
