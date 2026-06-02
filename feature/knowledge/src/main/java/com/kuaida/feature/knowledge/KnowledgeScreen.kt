package com.kuaida.feature.knowledge

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.kuaida.core.common.ui.components.QuickAnswerInfoRow
import com.kuaida.core.common.ui.components.QuickAnswerPill
import com.kuaida.core.common.ui.components.QuickAnswerScaffold
import com.kuaida.core.common.ui.components.QuickAnswerSearchField
import com.kuaida.core.common.ui.components.QuickAnswerSectionCard
import com.kuaida.core.common.ui.components.QuickAnswerSectionHeader
import com.kuaida.core.common.ui.displayLabel
import com.kuaida.core.common.ui.theme.QuickAnswerTone
import com.kuaida.core.common.util.toReadableDateTime
import com.kuaida.core.common.util.toReadableFileSize
import com.kuaida.core.model.DocumentParseStatus
import com.kuaida.core.model.KnowledgeDocument

object KnowledgeRoute {
    const val route = "knowledge"

    @Composable
    operator fun invoke(
        contentPadding: PaddingValues,
        viewModel: KnowledgeViewModel = hiltViewModel(),
    ) {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val snackbarHostState = remember { SnackbarHostState() }
        val picker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
            onResult = { uri: Uri? -> uri?.let(viewModel::importDocument) },
        )

        LaunchedEffect(state.message?.id) {
            state.message?.let { snackbarHostState.showSnackbar(it.message) }
            viewModel.consumeMessage()
        }

        KnowledgeScreen(
            state = state,
            contentPadding = contentPadding,
            snackbarHostState = snackbarHostState,
            onSearchChange = viewModel::updateQuery,
            onImport = {
                picker.launch(
                    arrayOf(
                        "text/plain",
                        "text/markdown",
                        "application/pdf",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    ),
                )
            },
            onToggle = viewModel::toggleEnabled,
            onDelete = viewModel::delete,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun KnowledgeScreen(
    state: KnowledgeUiState,
    contentPadding: PaddingValues,
    snackbarHostState: SnackbarHostState,
    onSearchChange: (String) -> Unit,
    onImport: () -> Unit,
    onToggle: (KnowledgeDocument, Boolean) -> Unit,
    onDelete: (KnowledgeDocument) -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<KnowledgeDocument?>(null) }

    QuickAnswerScaffold(
        title = "知识库",
        snackbarHostState = snackbarHostState,
        actions = {
            if (state.isImporting) {
                Box(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.5.dp,
                    )
                }
            } else {
                IconButton(onClick = onImport) {
                    Icon(Icons.Outlined.UploadFile, contentDescription = "导入文件")
                }
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
                        title = "导入资料",
                        description = "通过系统文件选择器导入 txt、md、pdf、docx，导入后会自动抽取文本、切分 chunk 并尝试生成 embedding。",
                        titleColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        descriptionColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f),
                    )
                    QuickAnswerSearchField(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.query,
                        onValueChange = onSearchChange,
                        placeholder = "搜索文件名",
                    )
                }
            }

            if (state.isImporting) {
                item {
                    QuickAnswerSectionCard(tone = QuickAnswerTone.Info) {
                        Text("正在导入文件", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "我们正在抽取文本、切分内容并写入向量数据，完成后会在下方列表里更新状态。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            if (state.documents.isEmpty()) {
                item {
                    QuickAnswerEmptyStateCard(
                        title = "还没有知识文件",
                        description = if (state.query.isBlank()) {
                            "点击右上角导入资料，后续问答就能优先参考这些内容。"
                        } else {
                            "没有匹配当前搜索词的文件。"
                        },
                    )
                }
            } else {
                items(state.documents, key = { it.id }) { document ->
                    val isBusy = document.id in state.actionDocumentIds
                    KnowledgeDocumentCard(
                        document = document,
                        isBusy = isBusy,
                        onToggle = { enabled -> onToggle(document, enabled) },
                        onDelete = { pendingDelete = document },
                    )
                }
            }
        }
    }

    pendingDelete?.let { document ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除知识文件") },
            text = {
                Text("删除后会同时移除分片、embedding 和相关成本记录，确定删除《${document.displayName}》吗？")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        onDelete(document)
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

@Composable
private fun KnowledgeDocumentCard(
    document: KnowledgeDocument,
    isBusy: Boolean,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    QuickAnswerSectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = document.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${document.mimeType} · ${document.sizeBytes.toReadableFileSize()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = onDelete,
                    enabled = !isBusy,
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = "删除文件")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                QuickAnswerPill(
                    text = document.parseStatus.displayLabel(),
                    tone = document.parseStatus.toTone(),
                )
                if (!document.isEnabled) {
                    QuickAnswerPill(text = "已停用", tone = QuickAnswerTone.Tonal)
                }
            }

            QuickAnswerInfoRow(
                label = "召回",
                value = document.retrievalModeLabel(),
                modifier = Modifier.fillMaxWidth(),
            )
            QuickAnswerInfoRow(
                label = "分片",
                value = "${document.chunkCount} 个",
                modifier = Modifier.fillMaxWidth(),
            )
            QuickAnswerInfoRow(
                label = "Embedding",
                value = "${document.embeddingCount} 条",
                modifier = Modifier.fillMaxWidth(),
            )
            QuickAnswerInfoRow(
                label = "更新时间",
                value = document.updatedAt.toReadableDateTime(),
                modifier = Modifier.fillMaxWidth(),
            )

            if (!document.failureReason.isNullOrBlank()) {
                HorizontalDivider()
                Text(
                    text = "失败原因：${document.failureReason}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (document.parseStatus == DocumentParseStatus.PROCESSING) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("启用检索", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (document.isEnabled) {
                            "当前文档会参与问答召回。"
                        } else {
                            "当前文档已停用，不会参与问答召回。"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .size(20.dp),
                        strokeWidth = 2.5.dp,
                    )
                } else {
                    Switch(
                        checked = document.isEnabled,
                        onCheckedChange = onToggle,
                        enabled = document.parseStatus == DocumentParseStatus.READY,
                    )
                }
            }
        }
    }
}

private fun KnowledgeDocument.retrievalModeLabel(): String {
    return when {
        parseStatus != DocumentParseStatus.READY -> "等待解析完成"
        embeddingCount > 0 && isEnabled -> "向量召回 + 关键词兜底"
        embeddingCount > 0 -> "已生成 embedding，当前停用"
        isEnabled -> "仅关键词召回"
        else -> "已停用"
    }
}

private fun DocumentParseStatus.toTone(): QuickAnswerTone {
    return when (this) {
        DocumentParseStatus.READY -> QuickAnswerTone.Positive
        DocumentParseStatus.FAILED -> QuickAnswerTone.Critical
        DocumentParseStatus.PROCESSING,
        DocumentParseStatus.PENDING -> QuickAnswerTone.Warning
    }
}
