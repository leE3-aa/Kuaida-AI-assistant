package com.kuaida.feature.workbench

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuaida.core.common.error.toUserMessage
import com.kuaida.core.common.ui.UiMessage
import com.kuaida.core.data.media.AudioRecorderManager
import com.kuaida.core.data.repository.ChatRepository
import com.kuaida.core.data.repository.SettingsRepository
import com.kuaida.core.model.AnswerBlock
import com.kuaida.core.model.AnswerStylePreset
import com.kuaida.core.model.CostRecord
import com.kuaida.core.model.FollowUpAction
import com.kuaida.core.model.InterviewSession
import com.kuaida.core.model.ProviderType
import com.kuaida.core.model.QuestionSourceType
import com.kuaida.core.model.UsageCost
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class WorkbenchViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val settingsRepository: SettingsRepository,
    private val audioRecorderManager: AudioRecorderManager,
) : ViewModel() {
    private val localState = MutableStateFlow(WorkbenchLocalState())
    private var timerJob: Job? = null

    val uiState: StateFlow<WorkbenchUiState> = combine(
        localState,
        settingsRepository.selectedModel,
        settingsRepository.selectedStyle,
    ) { local, model, style ->
        val session = local.session
        WorkbenchUiState(
            input = local.input,
            selectedStyle = local.selectedStyleOverride ?: style,
            modelLabel = model.displayName,
            isBusy = local.isBusy,
            hasAudioDraft = local.draftSourceType == QuestionSourceType.AUDIO,
            draftTranscript = local.transcript,
            recordingSheet = WorkbenchRecordingSheetUiState(
                isVisible = local.recordingSheetVisible,
                stage = local.recordingStage,
                durationSeconds = local.recordingSeconds,
                errorMessage = local.recordingErrorMessage,
                hasRecordingFile = local.recordingFilePath != null,
            ),
            answerCard = session?.answer?.let { answer ->
                WorkbenchAnswerCardState(
                    question = session.question,
                    transcript = session.transcript,
                    answer = answer,
                    sourceType = session.sourceType,
                    provider = session.provider,
                    modelId = session.modelId,
                    usage = session.usage,
                    costRecords = local.costRecords,
                    createdAt = session.createdAt,
                    latencyMs = session.latencyMs,
                )
            },
            message = local.message,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkbenchUiState())

    fun updateInput(value: String) {
        localState.update { state ->
            if (value.isBlank()) {
                state.copy(
                    input = value,
                    draftSourceType = QuestionSourceType.TEXT,
                    transcript = null,
                )
            } else {
                state.copy(input = value)
            }
        }
    }

    fun applyTemplatePrefill(value: String) {
        applyDraftPrefill(
            question = value,
            sourceType = QuestionSourceType.TEXT,
            transcript = null,
        )
    }

    fun applySessionPrefill(session: InterviewSession) {
        applyDraftPrefill(
            question = session.question,
            sourceType = session.sourceType,
            transcript = session.transcript,
            message = "已带入历史问题，可继续编辑后重新发送。",
        )
    }

    fun updateStyle(stylePreset: AnswerStylePreset) {
        viewModelScope.launch {
            settingsRepository.updateStyle(stylePreset)
            localState.update { it.copy(selectedStyleOverride = stylePreset) }
        }
    }

    fun submit() {
        val draft = localState.value
        val question = draft.input.trim()
        if (draft.isBusy) return
        if (draft.recordingSheetVisible) {
            showMessage("请先完成当前录音流程。")
            return
        }
        if (question.isBlank()) {
            showMessage("请先输入问题。")
            return
        }
        viewModelScope.launch {
            localState.update { it.copy(isBusy = true) }
            runCatching {
                val session = chatRepository.generateAnswer(
                    question = question,
                    sourceType = draft.draftSourceType,
                    transcript = draft.transcript?.takeIf { draft.draftSourceType == QuestionSourceType.AUDIO },
                )
                session to chatRepository.getCostRecords(session.id)
            }.onSuccess { (session, costs) ->
                localState.update {
                    it.copy(
                        isBusy = false,
                        session = session,
                        costRecords = costs,
                    )
                }
            }.onFailure { throwable ->
                localState.update { state ->
                    state.copy(
                        isBusy = false,
                        message = UiMessage(message = throwable.toUserMessage("生成失败，请稍后重试。")),
                    )
                }
            }
        }
    }

    fun runFollowUp(action: FollowUpAction) {
        val session = localState.value.session
        val answer = session?.answer
        if (localState.value.isBusy || session == null || answer == null) return
        viewModelScope.launch {
            localState.update { it.copy(isBusy = true) }
            runCatching {
                val nextSession = chatRepository.generateAnswer(
                    question = session.question,
                    sourceType = session.sourceType,
                    transcript = session.transcript,
                    followUpAction = action,
                    previousAnswer = answer,
                )
                nextSession to chatRepository.getCostRecords(nextSession.id)
            }.onSuccess { (nextSession, costs) ->
                localState.update {
                    it.copy(
                        isBusy = false,
                        session = nextSession,
                        costRecords = costs,
                    )
                }
            }.onFailure { throwable ->
                localState.update { state ->
                    state.copy(
                        isBusy = false,
                        message = UiMessage(message = throwable.toUserMessage("二次生成失败，请稍后重试。")),
                    )
                }
            }
        }
    }

    fun openRecordingSheet() {
        if (localState.value.isBusy || localState.value.recordingStage == RecordingStage.TRANSCRIBING) return
        localState.update {
            it.copy(
                recordingSheetVisible = true,
                recordingStage = RecordingStage.IDLE,
                recordingSeconds = 0,
                recordingFilePath = null,
                recordingErrorMessage = null,
            )
        }
    }

    fun dismissRecordingSheet() {
        val state = localState.value
        if (state.recordingStage == RecordingStage.TRANSCRIBING) return
        clearRecordingDraft(discardAudio = true)
        localState.update {
            it.copy(
                recordingSheetVisible = false,
                recordingStage = RecordingStage.IDLE,
                recordingSeconds = 0,
                recordingFilePath = null,
                recordingErrorMessage = null,
            )
        }
    }

    fun startRecording() {
        if (localState.value.isBusy) return
        clearRecordingDraft(discardAudio = true)
        runCatching { audioRecorderManager.startRecording() }
            .onSuccess { file ->
                localState.update {
                    it.copy(
                        recordingSheetVisible = true,
                        recordingStage = RecordingStage.RECORDING,
                        recordingSeconds = 0,
                        recordingFilePath = file.absolutePath,
                        recordingErrorMessage = null,
                    )
                }
                startTimer()
            }
            .onFailure { throwable ->
                localState.update {
                    it.copy(
                        recordingSheetVisible = true,
                        recordingStage = RecordingStage.IDLE,
                        recordingSeconds = 0,
                        recordingFilePath = null,
                        recordingErrorMessage = null,
                    )
                }
                showMessage(throwable.toUserMessage("录音启动失败，请稍后重试。"))
            }
    }

    fun pauseRecording() {
        if (localState.value.recordingStage != RecordingStage.RECORDING) return
        runCatching { audioRecorderManager.pauseRecording() }
            .onSuccess {
                stopTimer()
                localState.update { it.copy(recordingStage = RecordingStage.PAUSED) }
            }
            .onFailure { throwable ->
                showMessage(throwable.toUserMessage("录音暂停失败，请稍后重试。"))
            }
    }

    fun resumeRecording() {
        if (localState.value.recordingStage != RecordingStage.PAUSED) return
        runCatching { audioRecorderManager.resumeRecording() }
            .onSuccess {
                localState.update { it.copy(recordingStage = RecordingStage.RECORDING) }
                startTimer()
            }
            .onFailure { throwable ->
                showMessage(throwable.toUserMessage("录音继续失败，请稍后重试。"))
            }
    }

    fun finishRecording() {
        val draft = localState.value
        if (draft.recordingStage != RecordingStage.RECORDING && draft.recordingStage != RecordingStage.PAUSED) return
        stopTimer()
        val filePath = runCatching {
            audioRecorderManager.stopRecording()?.absolutePath ?: draft.recordingFilePath
        }.getOrElse { throwable ->
            localState.update {
                it.copy(
                    recordingStage = RecordingStage.FAILED,
                    recordingErrorMessage = throwable.toUserMessage("录音结束失败，请重试或重录。"),
                )
            }
            return
        }

        if (filePath.isNullOrBlank()) {
            localState.update {
                it.copy(
                    recordingStage = RecordingStage.FAILED,
                    recordingErrorMessage = "未找到录音文件，请重录。",
                )
            }
            return
        }

        localState.update {
            it.copy(
                recordingStage = RecordingStage.TRANSCRIBING,
                recordingFilePath = filePath,
                recordingErrorMessage = null,
            )
        }
        transcribeRecording(filePath)
    }

    fun retryRecordingTranscription() {
        val filePath = localState.value.recordingFilePath
        if (localState.value.isBusy || filePath.isNullOrBlank()) {
            showMessage("没有可重试的录音文件。")
            return
        }
        localState.update {
            it.copy(
                recordingStage = RecordingStage.TRANSCRIBING,
                recordingErrorMessage = null,
            )
        }
        transcribeRecording(filePath)
    }

    fun rerecord() {
        if (localState.value.recordingStage == RecordingStage.TRANSCRIBING) return
        clearRecordingDraft(discardAudio = true)
        localState.update {
            it.copy(
                recordingSheetVisible = true,
                recordingStage = RecordingStage.IDLE,
                recordingSeconds = 0,
                recordingFilePath = null,
                recordingErrorMessage = null,
            )
        }
        startRecording()
    }

    fun showMessage(message: String) {
        localState.update { it.copy(message = UiMessage(message = message)) }
    }

    fun consumeMessage() {
        localState.update { it.copy(message = null) }
    }

    override fun onCleared() {
        clearRecordingDraft(discardAudio = true)
        super.onCleared()
    }

    private fun transcribeRecording(filePath: String) {
        viewModelScope.launch {
            runCatching {
                chatRepository.transcribeAudio(filePath)
            }.onSuccess { result ->
                clearRecordingDraft(discardAudio = true)
                localState.update {
                    it.copy(
                        input = result.text,
                        draftSourceType = QuestionSourceType.AUDIO,
                        transcript = result.text,
                        recordingSheetVisible = false,
                        recordingStage = RecordingStage.IDLE,
                        recordingSeconds = 0,
                        recordingFilePath = null,
                        recordingErrorMessage = null,
                        message = UiMessage(message = "转写完成，可继续编辑后发送。"),
                    )
                }
            }.onFailure { throwable ->
                localState.update {
                    it.copy(
                        recordingStage = RecordingStage.FAILED,
                        recordingErrorMessage = throwable.toUserMessage("转写失败，请重试或重录。"),
                    )
                }
            }
        }
    }

    private fun startTimer() {
        stopTimer()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1_000)
                localState.update { state ->
                    if (state.recordingStage == RecordingStage.RECORDING) {
                        state.copy(recordingSeconds = state.recordingSeconds + 1)
                    } else {
                        state
                    }
                }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun clearRecordingDraft(discardAudio: Boolean) {
        stopTimer()
        if (!discardAudio) return
        val filePath = localState.value.recordingFilePath
        runCatching { audioRecorderManager.discardCurrent() }
        filePath?.let { path ->
            runCatching {
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
            }
        }
    }

    private fun applyDraftPrefill(
        question: String,
        sourceType: QuestionSourceType,
        transcript: String?,
        message: String? = null,
    ) {
        clearRecordingDraft(discardAudio = true)
        val effectiveSourceType = if (sourceType == QuestionSourceType.AUDIO && !transcript.isNullOrBlank()) {
            QuestionSourceType.AUDIO
        } else {
            QuestionSourceType.TEXT
        }
        localState.update {
            it.copy(
                input = question,
                draftSourceType = effectiveSourceType,
                transcript = transcript?.takeIf { effectiveSourceType == QuestionSourceType.AUDIO },
                recordingSheetVisible = false,
                recordingStage = RecordingStage.IDLE,
                recordingSeconds = 0,
                recordingFilePath = null,
                recordingErrorMessage = null,
                session = null,
                costRecords = emptyList(),
                message = message?.let { UiMessage(message = it) },
            )
        }
    }
}

private data class WorkbenchLocalState(
    val input: String = "",
    val selectedStyleOverride: AnswerStylePreset? = null,
    val isBusy: Boolean = false,
    val recordingSheetVisible: Boolean = false,
    val recordingStage: RecordingStage = RecordingStage.IDLE,
    val recordingSeconds: Int = 0,
    val recordingFilePath: String? = null,
    val recordingErrorMessage: String? = null,
    val draftSourceType: QuestionSourceType = QuestionSourceType.TEXT,
    val transcript: String? = null,
    val session: InterviewSession? = null,
    val costRecords: List<CostRecord> = emptyList(),
    val message: UiMessage? = null,
)

data class WorkbenchUiState(
    val input: String = "",
    val selectedStyle: AnswerStylePreset = AnswerStylePreset.DETAILED,
    val modelLabel: String = "阿里百炼 - Qwen Plus",
    val isBusy: Boolean = false,
    val hasAudioDraft: Boolean = false,
    val draftTranscript: String? = null,
    val recordingSheet: WorkbenchRecordingSheetUiState = WorkbenchRecordingSheetUiState(),
    val answerCard: WorkbenchAnswerCardState? = null,
    val message: UiMessage? = null,
)

data class WorkbenchRecordingSheetUiState(
    val isVisible: Boolean = false,
    val stage: RecordingStage = RecordingStage.IDLE,
    val durationSeconds: Int = 0,
    val errorMessage: String? = null,
    val hasRecordingFile: Boolean = false,
) {
    val isRecording: Boolean
        get() = stage == RecordingStage.RECORDING

    val isPaused: Boolean
        get() = stage == RecordingStage.PAUSED

    val isTranscribing: Boolean
        get() = stage == RecordingStage.TRANSCRIBING

    val hasFailed: Boolean
        get() = stage == RecordingStage.FAILED
}

enum class RecordingStage {
    IDLE,
    RECORDING,
    PAUSED,
    TRANSCRIBING,
    FAILED,
}

data class WorkbenchAnswerCardState(
    val question: String,
    val transcript: String?,
    val answer: AnswerBlock,
    val sourceType: QuestionSourceType,
    val provider: ProviderType,
    val modelId: String,
    val usage: UsageCost,
    val costRecords: List<CostRecord>,
    val createdAt: Long,
    val latencyMs: Long,
)
