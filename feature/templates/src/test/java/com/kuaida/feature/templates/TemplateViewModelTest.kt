package com.kuaida.feature.templates

import com.kuaida.core.data.repository.TemplateRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class TemplateViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun updateQuery_filtersTemplateItems() = runTest(mainDispatcherRule.testDispatcher.scheduler) {
        val viewModel = TemplateViewModel(TemplateRepository())
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        runCurrent()
        viewModel.updateQuery("产品")
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.items.size)
        assertEquals("metric", viewModel.uiState.value.items.single().id)
        collectJob.cancel()
    }

    @Test
    fun toggleFavorite_updatesTemplateState() = runTest(mainDispatcherRule.testDispatcher.scheduler) {
        val viewModel = TemplateViewModel(TemplateRepository())
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        runCurrent()
        viewModel.toggleFavorite("intro")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.items.first { it.id == "intro" }.favorite)
        collectJob.cancel()
    }
}
