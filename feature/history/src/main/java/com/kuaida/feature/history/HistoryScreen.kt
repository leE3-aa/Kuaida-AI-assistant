package com.kuaida.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kuaida.core.common.ui.components.QuickAnswerEmptyStateCard
import com.kuaida.core.common.ui.components.QuickAnswerMetricCard
import com.kuaida.core.common.ui.components.QuickAnswerPill
import com.kuaida.core.common.ui.components.QuickAnswerScaffold
import com.kuaida.core.common.ui.components.QuickAnswerSearchField
import com.kuaida.core.common.ui.components.QuickAnswerSectionCard
import com.kuaida.core.common.ui.components.QuickAnswerSectionHeader
import com.kuaida.core.common.ui.displayLabel
import com.kuaida.core.common.ui.theme.QuickAnswerTone
import com.kuaida.core.common.util.toReadableDateTime
import com.kuaida.core.common.util.toUsd
import com.kuaida.core.model.InterviewSession
import com.kuaida.core.model.SessionStatus

object HistoryRoute {
    const val route = "history"

    @Composable
    operator fun invoke(
        contentPadding: PaddingValues,
        onAskAgain: (InterviewSession) -> Unit,
        viewModel: HistoryViewModel = hiltViewModel(),
    ) {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(state.message?.id) {
            state.message?.let { snackbarHostState.showSnackbar(it.message) }
            viewModel.consumeMessage()
        }

        HistoryScreen(
            state = state,
            contentPadding = contentPadding,
            snackbarHostState = snackbarHostState,
            onSearchChange = viewModel::updateQuery,
            onDelete = viewModel::delete,
            onAskAgain = onAskAgain,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun HistoryScreen(
    state: HistoryUiState,
    contentPadding: PaddingValues,
    snackbarHostState: SnackbarHostState,
    onSearchChange: (String) -> Unit,
    onDelete: (InterviewSession) -> Unit,
    onAskAgain: (InterviewSession) -> Unit,
) {
    var selectedSession by remember { mutableStateOf<InterviewSession?>(null) }
    var pendingDelete by remember { mutableStateOf<InterviewSession?>(null) }

    LaunchedEffect(state.sessions, selectedSession?.id) {
        val selectedId = selectedSession?.id ?: return@LaunchedEffect
        if (state.sessions.none { it.id == selectedId }) {
            selectedSession = null
        }
    }

    LaunchedEffect(state.sessions, pendingDelete?.id) {
        val pendingId = pendingDelete?.id ?: return@LaunchedEffect
        if (state.sessions.none { it.id == pendingId }) {
            pendingDelete = null
        }
    }

    QuickAnswerScaffold(
        title = "会话历史",
        snackbarHostState = snackbarHostState,
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
                        title = "检索历史",
                        description = "按问题、转写或回答内容搜索，找到后可以查看详情、删除记录，或者把原问题重新带回工作台。",
                        titleColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        descriptionColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f),
                    )
                    QuickAnswerSearchField(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.query,
                        onValueChange = onSearchChange,
                        placeholder = "搜索问题、转写或回答内容",
                    )
                }
            }

            if (state.sessions.isEmpty()) {
                item {
                    EmptyHistoryCard(query = state.query)
                }
            } else {
                items(state.sessions, key = { it.id }) { session ->
                    HistorySessionCard(
                        session = session,
                        isDeleting = session.id in state.actionSessionIds,
                        onOpenDetail = { selectedSession = session },
                        onAskAgain = { onAskAgain(session) },
                        onDelete = { pendingDelete = session },
                    )
                }
            }
        }
    }

    selectedSession?.let { session ->
        HistoryDetailSheet(
            session = session,
            onDismiss = { selectedSession = null },
            onAskAgain = {
                selectedSession = null
                onAskAgain(session)
            },
            onDelete = { pendingDelete = session },
        )
    }

    pendingDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除历史记录") },
            text = {
                Text("删除后将移除这条会话及其关联成本记录，确定删除“${session.question}”吗？")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        selectedSession = selectedSession?.takeUnless { it.id == session.id }
                        onDelete(session)
                    },
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("取消")
                }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HistorySessionCard(
    session: InterviewSession,
    isDeleting: Boolean,
    onOpenDetail: () -> Unit,
    onAskAgain: () -> Unit,
    onDelete: () -> Unit,
) {
    QuickAnswerSectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = session.question,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                QuickAnswerPill(text = session.sourceType.displayLabel())
                QuickAnswerPill(text = session.provider.displayLabel())
                StatusPill(status = session.status)
            }

            Text(
                text = session.answer?.conclusion ?: session.errorMessage ?: "暂时没有可展示的回答内容。",
                style = MaterialTheme.typography.bodyLarge,
                color = if (session.answer != null) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            if (!session.transcript.isNullOrBlank() && session.transcript != session.question) {
                Text(
                    text = "转写：${session.transcript}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            HorizontalDivider()

            Text(
                text = "${session.createdAt.toReadableDateTime()} · ${session.usage.totalTokens} tokens · ${session.usage.estimatedCostUsd.toUsd()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(onClick = onOpenDetail) {
                    Text("详情")
                }
                TextButton(onClick = onAskAgain) {
                    Text("再次提问")
                }
                TextButton(
                    onClick = onDelete,
                    enabled = !isDeleting,
                ) {
                    Text(if (isDeleting) "删除中..." else "删除")
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryCard(query: String) {
    QuickAnswerEmptyStateCard(
        title = if (query.isBlank()) "还没有会话记录" else "没有匹配的历史记录",
        description = if (query.isBlank()) {
            "在工作台发送问题后，这里会自动保存问题、转写、回答和成本信息。"
        } else {
            "试试缩短关键词，或者搜索回答里的关键句子。"
        },
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun HistoryDetailSheet(
    session: InterviewSession,
    onDismiss: () -> Unit,
    onAskAgain: () -> Unit,
    onDelete: () -> Unit,
) {
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("会话详情", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        text = "生成于 ${session.createdAt.toReadableDateTime()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    QuickAnswerPill(text = session.sourceType.displayLabel())
                    QuickAnswerPill(text = session.provider.displayLabel())
                    QuickAnswerPill(text = session.modelId)
                    QuickAnswerPill(text = session.stylePreset.label)
                    StatusPill(status = session.status)
                }
            }

            item {
                DetailSectionCard(title = "原问题") {
                    Text(text = session.question, style = MaterialTheme.typography.bodyLarge)
                }
            }

            val transcript = session.transcript
            if (!transcript.isNullOrBlank()) {
                item {
                    DetailSectionCard(title = "原始转写") {
                        Text(
                            text = transcript,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                val answer = session.answer
                if (answer == null) {
                    DetailSectionCard(title = "回答详情") {
                        Text(
                            text = session.errorMessage ?: "这条记录暂时没有可展示的回答内容。",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    DetailSectionCard(title = "回答详情") {
                        DetailSubTitle("结论")
                        Text(
                            text = answer.conclusion,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )

                        if (answer.keyPoints.isNotEmpty()) {
                            DetailSubTitle("关键要点")
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                answer.keyPoints.forEachIndexed { index, point ->
                                    Text("${index + 1}. $point", style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }

                        DetailSubTitle("补充展开")
                        Text(
                            text = answer.expansion.ifBlank { "没有额外补充内容。" },
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }

            val errorMessage = session.errorMessage
            if (!errorMessage.isNullOrBlank()) {
                item {
                    DetailSectionCard(
                        title = "错误信息",
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ) {
                        Text(text = errorMessage, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            item {
                DetailSectionCard(title = "本轮统计") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        MetricCard(title = "Prompt", value = session.usage.promptTokens.toString())
                        MetricCard(title = "Completion", value = session.usage.completionTokens.toString())
                        MetricCard(title = "Total", value = session.usage.totalTokens.toString())
                        MetricCard(title = "费用估算", value = session.usage.estimatedCostUsd.toUsd())
                        MetricCard(title = "耗时", value = "${session.latencyMs}ms")
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = onAskAgain,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("再次提问")
                    }
                    TextButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("删除记录")
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailSectionCard(
    title: String,
    containerColor: Color? = null,
    contentColor: Color? = null,
    content: @Composable () -> Unit,
) {
    val resolvedContainerColor = containerColor ?: MaterialTheme.colorScheme.surfaceVariant
    val resolvedContentColor = contentColor ?: MaterialTheme.colorScheme.onSurface
    QuickAnswerSectionCard(
        containerColor = resolvedContainerColor,
        contentColor = resolvedContentColor,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides resolvedContentColor,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = resolvedContentColor,
                )
                content()
            }
        }
    }
}

@Composable
private fun DetailSubTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
) {
    QuickAnswerMetricCard(title = title, value = value)
}

@Composable
private fun StatusPill(status: SessionStatus) {
    QuickAnswerPill(
        text = status.displayLabel(),
        tone = when (status) {
            SessionStatus.COMPLETED -> QuickAnswerTone.Positive
            SessionStatus.FAILED -> QuickAnswerTone.Critical
            SessionStatus.GENERATING -> QuickAnswerTone.Warning
            SessionStatus.IDLE -> QuickAnswerTone.Tonal
        },
    )
}
