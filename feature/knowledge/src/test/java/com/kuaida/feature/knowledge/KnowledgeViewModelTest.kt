package com.kuaida.feature.knowledge

import android.net.Uri
import com.kuaida.core.data.repository.KnowledgeRepository
import com.kuaida.core.model.DocumentParseStatus
import com.kuaida.core.model.KnowledgeDocument
import com.kuaida.core.model.KnowledgeImportResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class KnowledgeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<KnowledgeRepository>()
    private val documentsFlow = MutableStateFlow(
        listOf(
            document(id = "doc_a", displayName = "产品手册"),
            document(id = "doc_b", displayName = "工程规范"),
        ),
    )

    @Test
    fun updateQuery_filtersDocumentsLocally() = runTest(mainDispatcherRule.testDispatcher.scheduler) {
        everyObserveDocuments()
        val viewModel = KnowledgeViewModel(repository)
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        runCurrent()
        viewModel.updateQuery("产品")
        runCurrent()

        assertEquals(1, viewModel.uiState.value.documents.size)
        assertEquals("产品手册", viewModel.uiState.value.documents.single().displayName)
        collectJob.cancel()
    }

    @Test
    fun importDocument_tracksLoadingAndShowsSuccessMessage() = runTest(mainDispatcherRule.testDispatcher.scheduler) {
        everyObserveDocuments()
        val uri = mockk<Uri>()
        val gate = CompletableDeferred<Unit>()
        coEvery { repository.importDocument(uri) } coAnswers {
            gate.await()
            KnowledgeImportResult(
                documentId = "doc_new",
                displayName = "面试题库.pdf",
                chunkCount = 3,
                embeddingCount = 3,
                warningMessage = null,
            )
        }

        val viewModel = KnowledgeViewModel(repository)
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        runCurrent()
        viewModel.importDocument(uri)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isImporting)

        gate.complete(Unit)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isImporting)
        assertEquals("《面试题库.pdf》已导入，共 3 个分片，已生成 3 条 embedding。", viewModel.uiState.value.message?.message)
        collectJob.cancel()
    }

    @Test
    fun toggleEnabled_failureClearsBusyStateAndShowsError() = runTest(mainDispatcherRule.testDispatcher.scheduler) {
        everyObserveDocuments()
        val target = documentsFlow.value.first()
        val gate = CompletableDeferred<Unit>()
        coEvery { repository.updateEnabled(target.id, false) } coAnswers {
            gate.await()
            throw IllegalStateException("切换失败")
        }

        val viewModel = KnowledgeViewModel(repository)
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        runCurrent()
        viewModel.toggleEnabled(target, false)
        advanceUntilIdle()
        assertTrue(target.id in viewModel.uiState.value.actionDocumentIds)

        gate.complete(Unit)
        advanceUntilIdle()

        assertTrue(target.id !in viewModel.uiState.value.actionDocumentIds)
        assertEquals("切换失败", viewModel.uiState.value.message?.message)
        collectJob.cancel()
    }

    private fun everyObserveDocuments() {
        every { repository.observeDocuments("") } returns documentsFlow
    }

    private fun document(id: String, displayName: String): KnowledgeDocument {
        return KnowledgeDocument(
            id = id,
            displayName = displayName,
            mimeType = "text/plain",
            sourceUri = "content://$id",
            sizeBytes = 128L,
            parseStatus = DocumentParseStatus.READY,
            chunkCount = 2,
            embeddingCount = 2,
            isEnabled = true,
            createdAt = 1L,
            updatedAt = 2L,
        )
    }
}
