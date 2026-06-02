package com.kuaida.feature.workbench

import com.kuaida.core.data.media.AudioRecorderManager
import com.kuaida.core.data.provider.ModelCatalog
import com.kuaida.core.data.repository.ChatRepository
import com.kuaida.core.data.repository.SettingsRepository
import com.kuaida.core.model.AnswerBlock
import com.kuaida.core.model.AnswerStylePreset
import com.kuaida.core.model.CostRecord
import com.kuaida.core.model.CostRecordCategory
import com.kuaida.core.model.InterviewSession
import com.kuaida.core.model.ProviderType
import com.kuaida.core.model.QuestionSourceType
import com.kuaida.core.model.SessionStatus
import com.kuaida.core.model.TranscriptionResult
import com.kuaida.core.model.UsageCost
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkbenchViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val chatRepository = mockk<ChatRepository>()
    private val settingsRepository = mockk<SettingsRepository>()
    private val audioRecorderManager = mockk<AudioRecorderManager>(relaxed = true)

    @Test
    fun submit_withBlankInputShowsMessage() = runTest(mainDispatcherRule.testDispatcher.scheduler) {
        val viewModel = buildViewModel()
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        runCurrent()
        viewModel.submit()
        runCurrent()

        assertEquals("请先输入问题。", viewModel.uiState.value.message?.message)
        collectJob.cancel()
    }

    @Test
    fun submit_successPopulatesAnswerCardAndCosts() = runTest(mainDispatcherRule.testDispatcher.scheduler) {
        val session = completedSession(
            id = "session_submit",
            question = "如何推进项目？",
            transcript = null,
            sourceType = QuestionSourceType.TEXT,
        )
        val cost = CostRecord(
            id = "cost_submit",
            sessionId = session.id,
            category = CostRecordCategory.ANSWER_GENERATION,
            provider = ProviderType.QWEN,
            modelId = "qwen-plus",
            usage = UsageCost(promptTokens = 12, completionTokens = 20, totalTokens = 32, estimatedCostUsd = 0.12),
            createdAt = 2L,
        )
        coEvery {
            chatRepository.generateAnswer("如何推进项目？", QuestionSourceType.TEXT, null, null, null)
        } returns session
        coEvery { chatRepository.getCostRecords(session.id) } returns listOf(cost)

        val viewModel = buildViewModel()
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        runCurrent()
        viewModel.updateInput("如何推进项目？")
        runCurrent()
        viewModel.submit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isBusy)
        assertNotNull(state.answerCard)
        assertEquals("如何推进项目？", state.answerCard?.question)
        assertEquals(1, state.answerCard?.costRecords?.size)
        assertEquals("阿里百炼 - Qwen Plus", state.modelLabel)
        collectJob.cancel()
    }

    @Test
    fun applySessionPrefill_withAudioSessionRestoresAudioDraft() = runTest(mainDispatcherRule.testDispatcher.scheduler) {
        val session = completedSession(
            id = "session_audio",
            question = "请介绍一次跨团队推进经历",
            transcript = "请介绍一次跨团队推进经历",
            sourceType = QuestionSourceType.AUDIO,
        )
        val viewModel = buildViewModel()
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        runCurrent()
        viewModel.applySessionPrefill(session)
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals("请介绍一次跨团队推进经历", state.input)
        assertTrue(state.hasAudioDraft)
        assertEquals("请介绍一次跨团队推进经历", state.draftTranscript)
        assertEquals("已带入历史问题，可继续编辑后重新发送。", state.message?.message)
        collectJob.cancel()
    }

    @Test
    fun finishRecording_afterSuccessfulTranscriptionFillsDraftAndClosesSheet() = runTest(mainDispatcherRule.testDispatcher.scheduler) {
        val recordingFile = File.createTempFile("qa-recording", ".m4a")
        every { audioRecorderManager.startRecording() } returns recordingFile
        every { audioRecorderManager.stopRecording() } returns recordingFile
        every { audioRecorderManager.discardCurrent() } just runs
        coEvery { chatRepository.transcribeAudio(recordingFile.absolutePath) } returns TranscriptionResult("这是录音转写结果")

        val viewModel = buildViewModel()
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        runCurrent()
        viewModel.startRecording()
        runCurrent()
        assertTrue(viewModel.uiState.value.recordingSheet.isVisible)
        assertEquals(RecordingStage.RECORDING, viewModel.uiState.value.recordingSheet.stage)

        viewModel.finishRecording()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("这是录音转写结果", state.input)
        assertTrue(state.hasAudioDraft)
        assertFalse(state.recordingSheet.isVisible)
        assertEquals(RecordingStage.IDLE, state.recordingSheet.stage)
        assertEquals("转写完成，可继续编辑后发送。", state.message?.message)

        recordingFile.delete()
        collectJob.cancel()
    }

    private fun buildViewModel(): WorkbenchViewModel {
        every { settingsRepository.selectedModel } returns MutableStateFlow(ModelCatalog.defaultModel())
        every { settingsRepository.selectedStyle } returns MutableStateFlow(AnswerStylePreset.DETAILED)
        coEvery { settingsRepository.updateStyle(any()) } just runs
        return WorkbenchViewModel(
            chatRepository = chatRepository,
            settingsRepository = settingsRepository,
            audioRecorderManager = audioRecorderManager,
        )
    }

    private fun completedSession(
        id: String,
        question: String,
        transcript: String?,
        sourceType: QuestionSourceType,
    ): InterviewSession {
        return InterviewSession(
            id = id,
            question = question,
            transcript = transcript,
            answer = AnswerBlock(
                conclusion = "先对齐目标，再拆解计划。",
                keyPoints = listOf("目标一致", "节奏透明"),
                expansion = "我会先明确目标、依赖和节奏。",
            ),
            provider = ProviderType.QWEN,
            modelId = "qwen-plus",
            stylePreset = AnswerStylePreset.DETAILED,
            sourceType = sourceType,
            usage = UsageCost(promptTokens = 10, completionTokens = 20, totalTokens = 30, estimatedCostUsd = 0.1),
            status = SessionStatus.COMPLETED,
            latencyMs = 1_200L,
            createdAt = 1L,
        )
    }
}
