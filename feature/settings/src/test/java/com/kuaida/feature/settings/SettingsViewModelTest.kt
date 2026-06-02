package com.kuaida.feature.settings

import com.kuaida.core.data.provider.ModelCatalog
import com.kuaida.core.data.repository.SettingsRepository
import com.kuaida.core.model.AnswerStylePreset
import com.kuaida.core.model.ProviderValidationResult
import com.kuaida.core.model.ProviderType
import com.kuaida.core.model.SettingsSummary
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<SettingsRepository>()
    private val summaryFlow = MutableStateFlow(
        SettingsSummary(
            selectedModel = ModelCatalog.defaultModel(),
            selectedStyle = AnswerStylePreset.DETAILED,
            tokenLimit = 1024,
            embeddingModel = ModelCatalog.defaultEmbeddingModel(),
            speechToTextModel = ModelCatalog.defaultSpeechToTextModel(),
            configuredProviders = ProviderType.entries.associateWith { it == ProviderType.QWEN },
            totalTokens = 123,
            totalCostUsd = 0.4567,
            highCostAlertEnabled = true,
        ),
    )

    @Test
    fun initialState_loadsMaskedKeysAndTokenDraft() = runTest(mainDispatcherRule.testDispatcher.scheduler) {
        everyBaseState()
        coEvery { repository.maskedApiKeys() } returns mapOf(ProviderType.QWEN to "••••••••••••1234")

        val viewModel = SettingsViewModel(repository)
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        advanceUntilIdle()

        assertEquals("••••••••••••1234", viewModel.uiState.value.providerKeys[ProviderType.QWEN])
        assertEquals("1024", viewModel.uiState.value.tokenLimitDraft)
        assertEquals(123, viewModel.uiState.value.totalTokens)
        collectJob.cancel()
    }

    @Test
    fun saveApiKey_whenDraftStillMaskedShowsUnchangedMessage() = runTest(mainDispatcherRule.testDispatcher.scheduler) {
        everyBaseState()
        coEvery { repository.maskedApiKeys() } returns mapOf(ProviderType.QWEN to "••••••••••••1234")

        val viewModel = SettingsViewModel(repository)
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        advanceUntilIdle()
        viewModel.saveApiKey(ProviderType.QWEN)
        advanceUntilIdle()

        assertEquals("阿里百炼 的 API Key 未发生变化。", viewModel.uiState.value.message?.message)
        coVerify(exactly = 0) { repository.saveApiKey(any(), any()) }
        collectJob.cancel()
    }

    @Test
    fun saveApiKey_whenValidationFailsShowsMessageAndDoesNotPersist() = runTest(mainDispatcherRule.testDispatcher.scheduler) {
        everyBaseState()
        coEvery { repository.maskedApiKeys() } returns emptyMap()
        coEvery {
            repository.validateApiKey(ProviderType.QWEN, "bad-key")
        } returns ProviderValidationResult(success = false, message = "阿里百炼 API Key 无效，请到设置页重新填写并保存。")

        val viewModel = SettingsViewModel(repository)
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        advanceUntilIdle()
        viewModel.updateDraftKey(ProviderType.QWEN, "bad-key")
        runCurrent()
        viewModel.saveApiKey(ProviderType.QWEN)
        advanceUntilIdle()

        assertEquals("阿里百炼 API Key 无效，请到设置页重新填写并保存。", viewModel.uiState.value.message?.message)
        coVerify(exactly = 0) { repository.saveApiKey(any(), any()) }
        collectJob.cancel()
    }

    @Test
    fun applyTokenLimitDraft_withBlankValueShowsValidationMessage() = runTest(mainDispatcherRule.testDispatcher.scheduler) {
        everyBaseState()
        coEvery { repository.maskedApiKeys() } returns emptyMap()

        val viewModel = SettingsViewModel(repository)
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        advanceUntilIdle()
        viewModel.updateTokenLimitDraft("")
        runCurrent()
        viewModel.applyTokenLimitDraft()
        runCurrent()

        assertEquals("请输入 256-4096 之间的整数。", viewModel.uiState.value.message?.message)
        collectJob.cancel()
    }

    @Test
    fun applyTokenLimit_clampsValueAndUpdatesDraft() = runTest(mainDispatcherRule.testDispatcher.scheduler) {
        everyBaseState()
        coEvery { repository.maskedApiKeys() } returns emptyMap()
        coEvery { repository.updateTokenLimit(4096) } just runs

        val viewModel = SettingsViewModel(repository)
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        advanceUntilIdle()
        viewModel.applyTokenLimit(10_000)
        advanceUntilIdle()

        assertEquals("4096", viewModel.uiState.value.tokenLimitDraft)
        assertEquals("输出 token 上限已更新为 4096。", viewModel.uiState.value.message?.message)
        coVerify(exactly = 1) { repository.updateTokenLimit(4096) }
        collectJob.cancel()
    }

    @Test
    fun selectProvider_updatesChatEmbeddingAndSpeechModelsTogether() = runTest(mainDispatcherRule.testDispatcher.scheduler) {
        everyBaseState()
        coEvery { repository.maskedApiKeys() } returns emptyMap()

        val viewModel = SettingsViewModel(repository)
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        advanceUntilIdle()
        viewModel.selectProvider(ProviderType.ZHIPU)
        advanceUntilIdle()

        val chatModel = ModelCatalog.byProvider(ProviderType.ZHIPU).first()
        val embeddingModel = ModelCatalog.embeddingByProvider(ProviderType.ZHIPU).first()
        val speechModel = ModelCatalog.speechToTextByProvider(ProviderType.ZHIPU).first()

        coVerify(exactly = 1) { repository.updateSelectedModel(chatModel.provider, chatModel.modelId) }
        coVerify(exactly = 1) { repository.updateSelectedEmbeddingModel(embeddingModel.provider, embeddingModel.modelId) }
        coVerify(exactly = 1) { repository.updateSelectedSpeechToTextModel(speechModel.provider, speechModel.modelId) }
        assertEquals(
            "已切换到 智谱 AI，填入这一个 API Key 就能跑通聊天、知识库和语音转写。",
            viewModel.uiState.value.message?.message,
        )
        collectJob.cancel()
    }

    @Test
    fun saveApiKey_forCustomProvider_persistsConfigBeforeValidation() = runTest(mainDispatcherRule.testDispatcher.scheduler) {
        everyBaseState()
        coEvery { repository.maskedApiKeys() } returns emptyMap()
        coEvery {
            repository.validateApiKey(ProviderType.CUSTOM, "sk-custom")
        } returns ProviderValidationResult(success = true, message = "连接成功")

        val viewModel = SettingsViewModel(repository)
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        advanceUntilIdle()
        viewModel.updateCustomBaseUrlDraft("https://api.siliconflow.cn/v1")
        viewModel.updateCustomChatModelDraft("deepseek-ai/DeepSeek-V3")
        viewModel.updateDraftKey(ProviderType.CUSTOM, "sk-custom")
        runCurrent()
        viewModel.saveApiKey(ProviderType.CUSTOM)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            repository.updateCustomProviderConfig(
                "https://api.siliconflow.cn/v1",
                "deepseek-ai/DeepSeek-V3",
                "BAAI/bge-m3",
            )
        }
        coVerify(exactly = 1) {
            repository.updateSelectedModel(ProviderType.CUSTOM, "deepseek-ai/DeepSeek-V3")
        }
        coVerify(exactly = 1) {
            repository.updateSelectedEmbeddingModel(ProviderType.CUSTOM, "BAAI/bge-m3")
        }
        coVerify(exactly = 1) {
            repository.updateSelectedSpeechToTextModel(
                ProviderType.CUSTOM,
                "FunAudioLLM/SenseVoiceSmall",
            )
        }
        coVerify(exactly = 1) { repository.saveApiKey(ProviderType.CUSTOM, "sk-custom") }
        collectJob.cancel()
    }

    private fun everyBaseState() {
        every { repository.settingsSummary } returns summaryFlow
        coEvery { repository.validateApiKey(any(), any()) } returns ProviderValidationResult(success = true, message = "连接成功")
        coEvery { repository.updateCustomProviderConfig(any(), any(), any()) } just runs
        coEvery { repository.updateSelectedModel(any(), any()) } just runs
        coEvery { repository.updateStyle(any()) } just runs
        coEvery { repository.updateSelectedEmbeddingModel(any(), any()) } just runs
        coEvery { repository.updateSelectedSpeechToTextModel(any(), any()) } just runs
        coEvery { repository.updateHighCostAlert(any()) } just runs
    }
}
