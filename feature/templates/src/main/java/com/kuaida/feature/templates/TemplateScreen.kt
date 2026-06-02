package com.kuaida.feature.templates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kuaida.core.common.ui.components.QuickAnswerEmptyStateCard
import com.kuaida.core.common.ui.components.QuickAnswerPill
import com.kuaida.core.common.ui.components.QuickAnswerScaffold
import com.kuaida.core.common.ui.components.QuickAnswerSearchField
import com.kuaida.core.common.ui.components.QuickAnswerSectionCard
import com.kuaida.core.common.ui.components.QuickAnswerSectionHeader
import com.kuaida.core.common.ui.theme.QuickAnswerTone

object TemplateRoute {
    const val route = "templates"

    @Composable
    operator fun invoke(
        contentPadding: PaddingValues,
        onBack: () -> Unit,
        onUseTemplate: (String) -> Unit,
        viewModel: TemplateViewModel = hiltViewModel(),
    ) {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        TemplateScreen(
            state = state,
            contentPadding = contentPadding,
            onBack = onBack,
            onQueryChange = viewModel::updateQuery,
            onFavoriteToggle = viewModel::toggleFavorite,
            onUseTemplate = onUseTemplate,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplateScreen(
    state: TemplateUiState,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onFavoriteToggle: (String) -> Unit,
    onUseTemplate: (String) -> Unit,
) {
    QuickAnswerScaffold(
        title = "面试模板",
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = null)
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(contentPadding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                QuickAnswerSectionCard(tone = QuickAnswerTone.Brand) {
                    QuickAnswerSectionHeader(
                        title = "选择模板",
                        description = "把常见面试题、英文表达和结构化提问方式沉淀成快捷入口。",
                        titleColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        descriptionColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f),
                    )
                    QuickAnswerSearchField(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.query,
                        onValueChange = onQueryChange,
                        placeholder = "搜索模板...",
                    )
                }
            }
            if (state.items.isEmpty()) {
                item {
                    QuickAnswerEmptyStateCard(
                        title = "没有匹配的模板",
                        description = if (state.query.isBlank()) {
                            "收藏常用模板后，这里会成为你最快的提问入口。"
                        } else {
                            "试试换一个关键词，或者从分类词开始搜索。"
                        },
                    )
                }
            }
            items(state.items, key = { it.id }) { item ->
                QuickAnswerSectionCard(
                    tone = if (item.favorite) QuickAnswerTone.Brand else QuickAnswerTone.Surface,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            QuickAnswerPill(
                                text = item.category,
                                tone = if (item.favorite) QuickAnswerTone.Positive else QuickAnswerTone.Tonal,
                            )
                            IconButton(onClick = { onFavoriteToggle(item.id) }) {
                                Icon(
                                    imageVector = if (item.favorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = null,
                                )
                            }
                        }
                        Text(item.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(item.description, style = MaterialTheme.typography.bodyLarge)
                        Text("${item.readingMinutes} 分钟准备")
                        Button(onClick = { onUseTemplate(item.prompt) }, modifier = Modifier.fillMaxWidth()) {
                            Text("使用模板")
                        }
                    }
                }
            }
        }
    }
}
