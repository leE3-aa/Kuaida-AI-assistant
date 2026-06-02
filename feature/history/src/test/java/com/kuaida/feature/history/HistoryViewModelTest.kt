package com.kuaida.feature.history

import com.kuaida.core.data.repository.ChatRepository
import com.kuaida.core.model.AnswerBlock
import com.kuaida.core.model.AnswerStylePreset
import com.kuaida.core.model.InterviewSession
import com.kuaida.core.model.ProviderType
import com.kuaida.core.model.QuestionSourceType
import com.kuaida.core.model.SessionStatus
import com.kuaida.core.model.UsageCost
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val chatRepository = mockk<ChatRepository>()

    @Test
    fun updateQuery_switchesObservedSessions() = runTest(mainDispatcherRule.testDispatcher.scheduler) {
        val projectSession = session(id = "1", question = "产品题")
        val systemSession = session(id = "2", question = "系统设计")
        everyObserveSessions(projectSession, systemSession)

        val viewModel = HistoryViewModel(chatRepository)
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        runCurrent()
        assertEquals(2, viewModel.uiState.value.sessions.size)

        viewModel.updateQuery("产品")
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.sessions.size)
        assertEquals("产品题", viewModel.uiState.value.sessions.single().question)
        collectJob.cancel()
    }

    @Test
    fun delete_successTracksPendingStateAndShowsMessage() = runTest(mainDispatcherRule.testDispatcher.scheduler) {
        val session = session(id = "session_delete", question = "项目问题")
        val gate = CompletableDeferred<Unit>()
        every { chatRepository.observeSessions(any()) } returns flowOf(listOf(session))
        coEvery { chatRepository.deleteSession(session.id) } coAnswers { gate.await() }

        val viewModel = HistoryViewModel(chatRepository)
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        runCurrent()
        viewModel.delete(session)
        advanceUntilIdle()
        assertTrue(session.id in viewModel.uiState.value.actionSessionIds)

        gate.complete(Unit)
        advanceUntilIdle()

        assertTrue(session.id !in viewModel.uiState.value.actionSessionIds)
        assertEquals("已删除这条历史记录。", viewModel.uiState.value.message?.message)
        collectJob.cancel()
    }

    private fun everyObserveSessions(projectSession: InterviewSession, systemSession: InterviewSession) {
        every { chatRepository.observeSessions(any()) } answers {
            val query = firstArg<String>()
            val items = listOf(projectSession, systemSession).filter { query.isBlank() || it.question.contains(query) }
            flowOf(items)
        }
    }

    private fun session(id: String, question: String): InterviewSession {
        return InterviewSession(
            id = id,
            question = question,
            transcript = null,
            answer = AnswerBlock(
                conclusion = "回答结论",
                keyPoints = listOf("要点一"),
                expansion = "补充说明",
            ),
            provider = ProviderType.QWEN,
            modelId = "qwen-plus",
            stylePreset = AnswerStylePreset.DETAILED,
            sourceType = QuestionSourceType.TEXT,
            usage = UsageCost(totalTokens = 30, estimatedCostUsd = 0.1),
            status = SessionStatus.COMPLETED,
            latencyMs = 800L,
            createdAt = 1L,
        )
    }
}
