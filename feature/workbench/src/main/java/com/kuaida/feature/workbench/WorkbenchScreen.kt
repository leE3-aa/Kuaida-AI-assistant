package com.kuaida.feature.workbench

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kuaida.core.common.ui.components.QuickAnswerEmptyStateCard
import com.kuaida.core.common.ui.components.QuickAnswerPill
import com.kuaida.core.common.ui.components.QuickAnswerScaffold
import com.kuaida.core.common.ui.components.QuickAnswerSectionCard
import com.kuaida.core.common.ui.components.QuickAnswerSectionHeader
import com.kuaida.core.common.ui.displayLabel
import com.kuaida.core.common.ui.theme.QaTheme
import com.kuaida.core.common.ui.theme.QuickAnswerTone
import com.kuaida.core.common.util.toReadableDateTime
import com.kuaida.core.model.AnswerStylePreset
import com.kuaida.core.model.FollowUpAction
import com.kuaida.core.model.InterviewSession
import com.kuaida.core.model.ProviderType
import com.kuaida.core.model.QuestionSourceType

object WorkbenchRoute {
    const val route = "workbench"

    @Composable
    operator fun invoke(
        contentPadding: PaddingValues,
        templatePrefill: String?,
        onTemplatePrefillConsumed: () -> Unit,
        historyPrefill: InterviewSession?,
        onHistoryPrefillConsumed: () -> Unit,
        onOpenTemplates: () -> Unit,
        viewModel: WorkbenchViewModel = hiltViewModel(),
    ) {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val snackbarHostState = remember { SnackbarHostState() }
        val recordPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { granted ->
                if (granted) {
                    viewModel.openRecordingSheet()
                } else {
                    viewModel.showMessage("麦克风权限被拒绝，请改用手动输入。")
                }
            },
        )

        LaunchedEffect(templatePrefill) {
            templatePrefill?.let {
                viewModel.applyTemplatePrefill(it)
                onTemplatePrefillConsumed()
            }
        }

        LaunchedEffect(historyPrefill?.id, historyPrefill?.createdAt) {
            historyPrefill?.let {
                viewModel.applySessionPrefill(it)
                onHistoryPrefillConsumed()
            }
        }

        LaunchedEffect(state.message?.id) {
            state.message?.let { snackbarHostState.showSnackbar(it.message) }
            viewModel.consumeMessage()
        }

        WorkbenchScreen(
            state = state,
            snackbarHostState = snackbarHostState,
            contentPadding = contentPadding,
            onInputChange = viewModel::updateInput,
            onStyleChange = viewModel::updateStyle,
            onSubmit = viewModel::submit,
            onOpenRecordingSheet = { recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
            onDismissRecordingSheet = viewModel::dismissRecordingSheet,
            onStartRecording = viewModel::startRecording,
            onPauseRecording = viewModel::pauseRecording,
            onResumeRecording = viewModel::resumeRecording,
            onFinishRecording = viewModel::finishRecording,
            onRerecord = viewModel::rerecord,
            onRetryRecordingTranscription = viewModel::retryRecordingTranscription,
            onFollowUp = viewModel::runFollowUp,
            onOpenTemplates = onOpenTemplates,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun WorkbenchScreen(
    state: WorkbenchUiState,
    snackbarHostState: SnackbarHostState,
    contentPadding: PaddingValues,
    onInputChange: (String) -> Unit,
    onStyleChange: (AnswerStylePreset) -> Unit,
    onSubmit: () -> Unit,
    onOpenRecordingSheet: () -> Unit,
    onDismissRecordingSheet: () -> Unit,
    onStartRecording: () -> Unit,
    onPauseRecording: () -> Unit,
    onResumeRecording: () -> Unit,
    onFinishRecording: () -> Unit,
    onRerecord: () -> Unit,
    onRetryRecordingTranscription: () -> Unit,
    onFollowUp: (FollowUpAction) -> Unit,
    onOpenTemplates: () -> Unit,
) {
    QuickAnswerScaffold(
        title = "工作台",
        subtitle = state.modelLabel,
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
                ComposerCard(
                    state = state,
                    onInputChange = onInputChange,
                    onStyleChange = onStyleChange,
                    onSubmit = onSubmit,
                    onOpenRecordingSheet = onOpenRecordingSheet,
                    onOpenTemplates = onOpenTemplates,
                )
            }

            item {
                if (state.answerCard == null) {
                    EmptyAnswerCard(hasAudioDraft = state.hasAudioDraft)
                } else {
                    var visible by remember(state.answerCard) { mutableStateOf(false) }
                    LaunchedEffect(state.answerCard) { visible = true }
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 }),
                    ) {
                        AnswerCard(
                            card = state.answerCard,
                            isBusy = state.isBusy,
                            onFollowUp = onFollowUp,
                        )
                    }
                }
            }
        }
    }

    if (state.recordingSheet.isVisible) {
        RecordingSheet(
            state = state.recordingSheet,
            onDismiss = onDismissRecordingSheet,
            onStart = onStartRecording,
            onPause = onPauseRecording,
            onResume = onResumeRecording,
            onFinish = onFinishRecording,
            onRerecord = onRerecord,
            onRetry = onRetryRecordingTranscription,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ComposerCard(
    state: WorkbenchUiState,
    onInputChange: (String) -> Unit,
    onStyleChange: (AnswerStylePreset) -> Unit,
    onSubmit: () -> Unit,
    onOpenRecordingSheet: () -> Unit,
    onOpenTemplates: () -> Unit,
) {
    QuickAnswerSectionCard(tone = QuickAnswerTone.Brand) {
        QuickAnswerSectionHeader(
            title = "输入问题",
            description = "支持手动输入、模板预填和语音转写。发送后会生成结构化回答。",
            titleColor = MaterialTheme.colorScheme.onPrimaryContainer,
            descriptionColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f),
        )

        if (state.hasAudioDraft) {
            QuickAnswerSectionCard(tone = QuickAnswerTone.Info) {
                Text("语音转写草稿", fontWeight = FontWeight.Bold)
                Text(
                    text = state.draftTranscript.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.input,
            onValueChange = onInputChange,
            minLines = 5,
            placeholder = { Text("例如：请你介绍一下做项目推进时，如何协调跨团队资源？") },
            supportingText = {
                Text(
                    if (state.recordingSheet.isVisible) {
                        "录音弹层已打开，可以在里面开始、暂停、结束或重录。"
                    } else {
                        "可以先口述一遍，再在这里修饰成更适合发送的问题。"
                    },
                )
            },
            singleLine = false,
            shape = MaterialTheme.shapes.small,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Send),
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AnswerStylePreset.entries.forEach { preset ->
                FilterChip(
                    selected = state.selectedStyle == preset,
                    onClick = { onStyleChange(preset) },
                    label = { Text(preset.label) },
                    enabled = !state.isBusy && !state.recordingSheet.isVisible,
                )
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(
                onClick = onOpenTemplates,
                enabled = !state.isBusy && !state.recordingSheet.isVisible,
            ) {
                Icon(Icons.Outlined.Style, contentDescription = null)
                Text("模板")
            }

            OutlinedButton(
                onClick = onOpenRecordingSheet,
                enabled = !state.isBusy && !state.recordingSheet.isVisible,
            ) {
                Icon(Icons.Outlined.Mic, contentDescription = null)
                Text("录音")
            }

            Button(
                onClick = onSubmit,
                enabled = state.input.isNotBlank() && !state.isBusy && !state.recordingSheet.isVisible,
            ) {
                Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null)
                Text(if (state.isBusy) "生成中..." else "发送")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordingSheet(
    state: WorkbenchRecordingSheetUiState,
    onDismiss: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    onRerecord: () -> Unit,
    onRetry: () -> Unit,
) {
    val micTransition = rememberInfiniteTransition(label = "micPulse")
    val micScale by micTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "micScale",
    )

    ModalBottomSheet(
        onDismissRequest = {
            if (!state.isTranscribing) {
                onDismiss()
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("语音录音", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        text = recordingStatusText(state),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!state.isTranscribing) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "关闭录音弹层")
                    }
                }
            }

            QuickAnswerSectionCard(
                tone = recordingTone(state),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Mic,
                        contentDescription = null,
                        modifier = if (state.isRecording) Modifier.scale(micScale) else Modifier,
                    )
                    Text(
                        text = formatRecordingDuration(state.durationSeconds),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = recordingHintText(state),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (state.isTranscribing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(strokeWidth = 2.5.dp)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("正在转写录音", fontWeight = FontWeight.Bold)
                        Text(
                            "这一步会把录音转换成可编辑的问题草稿，请稍等一下。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (state.hasFailed && !state.errorMessage.isNullOrBlank()) {
                QuickAnswerSectionCard(
                    tone = QuickAnswerTone.Critical,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Text(
                        text = state.errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            when {
                state.stage == RecordingStage.IDLE -> {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onStart,
                    ) {
                        Icon(Icons.Outlined.Mic, contentDescription = null)
                        Text("开始录音")
                    }
                    TextButton(
                        modifier = Modifier.align(Alignment.End),
                        onClick = onDismiss,
                    ) {
                        Text("取消")
                    }
                }

                state.isRecording -> {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onFinish,
                    ) {
                        Icon(Icons.Outlined.Mic, contentDescription = null)
                        Text("结束并转写")
                    }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onPause,
                    ) {
                        Icon(Icons.Outlined.Pause, contentDescription = null)
                        Text("暂停")
                    }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onRerecord,
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null)
                        Text("重录")
                    }
                    TextButton(
                        modifier = Modifier.align(Alignment.End),
                        onClick = onDismiss,
                    ) {
                        Text("取消本次录音")
                    }
                }

                state.isPaused -> {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onResume,
                    ) {
                        Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                        Text("继续录音")
                    }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onFinish,
                    ) {
                        Icon(Icons.Outlined.Mic, contentDescription = null)
                        Text("结束并转写")
                    }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onRerecord,
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null)
                        Text("重录")
                    }
                    TextButton(
                        modifier = Modifier.align(Alignment.End),
                        onClick = onDismiss,
                    ) {
                        Text("关闭")
                    }
                }

                state.hasFailed -> {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onRetry,
                        enabled = state.hasRecordingFile,
                    ) {
                        Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                        Text("重试转写")
                    }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onRerecord,
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null)
                        Text("重录")
                    }
                    TextButton(
                        modifier = Modifier.align(Alignment.End),
                        onClick = onDismiss,
                    ) {
                        Text("关闭")
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyAnswerCard(hasAudioDraft: Boolean) {
    QuickAnswerEmptyStateCard(
        title = "回答卡片",
        description = if (hasAudioDraft) {
            "语音已经转成文字了。现在可以继续微调问题，然后点击发送生成回答。"
        } else {
            "发送问题后，这里会展示结构化回答和追问增强按钮。"
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnswerCard(
    card: WorkbenchAnswerCardState,
    isBusy: Boolean,
    onFollowUp: (FollowUpAction) -> Unit,
) {
    var isExpansionExpanded by remember(card) { mutableStateOf(false) }

    QuickAnswerSectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("回答卡片", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                QuickAnswerPill(text = card.sourceType.displayLabel())
                QuickAnswerPill(text = card.provider.displayLabel())
                QuickAnswerPill(text = card.modelId)
                QuickAnswerPill(text = "耗时 ${card.latencyMs}ms", tone = QuickAnswerTone.Info)
            }

            SectionTitle("问题")
            Text(card.question, style = MaterialTheme.typography.bodyLarge)

            if (!card.transcript.isNullOrBlank() && card.transcript != card.question) {
                SectionTitle("原始转写")
                Text(
                    text = card.transcript,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            ConclusionBlock(conclusion = card.answer.conclusion)

            SectionTitle("关键要点")
            Column(verticalArrangement = Arrangement.spacedBy(QaTheme.spacing.medium)) {
                card.answer.keyPoints.forEachIndexed { index, point ->
                    KeyPointItem(number = index + 1, text = point)
                }
            }

            SectionTitle("补充展开")
            Column(modifier = Modifier.animateContentSize()) {
                Text(
                    text = card.answer.expansion,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = if (isExpansionExpanded) Int.MAX_VALUE else 2,
                    overflow = if (isExpansionExpanded) TextOverflow.Clip else TextOverflow.Ellipsis,
                )
                TextButton(
                    onClick = { isExpansionExpanded = !isExpansionExpanded },
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text(
                        text = if (isExpansionExpanded) "收起" else "展开查看完整补充",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }

            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("追问增强", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = if (isBusy) "正在生成新的增强版本..." else "对当前回答做补充、示例化或重生成。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FollowUpAction.entries.forEach { action ->
                        AssistChip(
                            onClick = { onFollowUp(action) },
                            label = { Text(action.label) },
                            enabled = !isBusy,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "生成时间 ${card.createdAt.toReadableDateTime()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.AccessTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${card.latencyMs}ms",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ConclusionBlock(conclusion: String) {
    QuickAnswerSectionCard(
        tone = QuickAnswerTone.Brand,
        contentPadding = PaddingValues(0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary),
            )
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "结论",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = conclusion,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun KeyPointItem(number: Int, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "$number",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
}

private fun formatRecordingDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

private fun recordingTone(state: WorkbenchRecordingSheetUiState): QuickAnswerTone = when {
    state.hasFailed -> QuickAnswerTone.Critical
    state.isRecording -> QuickAnswerTone.Brand
    state.isPaused -> QuickAnswerTone.Info
    state.isTranscribing -> QuickAnswerTone.Warning
    else -> QuickAnswerTone.Tonal
}

private fun recordingStatusText(state: WorkbenchRecordingSheetUiState): String {
    return when {
        state.isRecording -> "录音中，支持暂停后继续。"
        state.isPaused -> "已暂停，可以继续、结束或直接重录。"
        state.isTranscribing -> "录音已结束，正在转写中。"
        state.hasFailed -> "转写失败了，可以重试或者重新录一段。"
        else -> "先录下来，再转成可编辑的文本。"
    }
}

private fun recordingHintText(state: WorkbenchRecordingSheetUiState): String {
    return when {
        state.isRecording -> "正在采集麦克风声音"
        state.isPaused -> "录音已暂停，时长会保留"
        state.isTranscribing -> "音频处理中，请保持页面开启"
        state.hasFailed -> "保留了这段录音，你可以重试转写"
        else -> "点击开始后就会开始计时"
    }
}
