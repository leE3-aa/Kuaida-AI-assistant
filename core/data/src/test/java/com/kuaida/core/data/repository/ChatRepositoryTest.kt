package com.kuaida.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kuaida.core.data.database.AppDatabase
import com.kuaida.core.data.provider.ChatProvider
import com.kuaida.core.data.provider.ModelCatalog
import com.kuaida.core.data.provider.ProviderRegistry
import com.kuaida.core.data.provider.SpeechToTextProvider
import com.kuaida.core.data.rag.KnowledgeRetriever
import com.kuaida.core.data.rag.QueryEmbeddingCostSnapshot
import com.kuaida.core.data.rag.RagContextAssembler
import com.kuaida.core.data.rag.RagContextAssembly
import com.kuaida.core.data.rag.RetrievalResult
import com.kuaida.core.data.testing.MainDispatcherRule
import com.kuaida.core.model.AnswerBlock
import com.kuaida.core.model.AnswerStylePreset
import com.kuaida.core.model.ProviderAnswerResult
import com.kuaida.core.model.ProviderType
import com.kuaida.core.model.QuestionSourceType
import com.kuaida.core.model.RetrievedChunk
import com.kuaida.core.model.SessionStatus
import com.kuaida.core.model.TranscriptionResult
import com.kuaida.core.model.UsageCost
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ChatRepositoryTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var database: AppDatabase
    private lateinit var repository: ChatRepository

    private val settingsRepository = mockk<SettingsRepository>()
    private val knowledgeRetriever = mockk<KnowledgeRetriever>()
    private val ragContextAssembler = mockk<RagContextAssembler>()
    private val chatProvider = mockk<ChatProvider>()
    private val speechToTextProvider = mockk<SpeechToTextProvider>()

    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        every { chatProvider.type } returns ProviderType.QWEN
        every { speechToTextProvider.type } returns ProviderType.QWEN

        repository = ChatRepository(
            database = database,
            chatSessionDao = database.chatSessionDao(),
            costRecordDao = database.costRecordDao(),
            settingsRepository = settingsRepository,
            providerRegistry = ProviderRegistry(
                chatProviders = setOf(chatProvider),
                embeddingProviders = emptySet(),
                speechToTextProviders = setOf(speechToTextProvider),
            ),
            knowledgeRetriever = knowledgeRetriever,
            ragContextAssembler = ragContextAssembler,
            json = json,
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun generateAnswer_persistsCompletedSessionAndCostRecords() = runTest(mainDispatcherRule.testDispatcher.scheduler) {
        val structuredAnswer = AnswerBlock(
            conclusion = "先对齐目标，再拆解资源与风险。",
            keyPoints = listOf("同步目标", "明确 owner"),
            expansion = "我会先拉齐目标，再分解里程碑和依赖。",
        )
        val retrievedChunks = listOf(
            RetrievedChunk(
                chunkId = "chunk_1",
                documentId = "doc_1",
                documentName = "项目推进手册",
                chunkIndex = 0,
                content = "项目推进时先明确目标和里程碑。",
                tokenCount = 40,
                score = 0.91,
            ),
        )

        coEvery { settingsRepository.currentModel() } returns ModelCatalog.defaultModel()
        coEvery { settingsRepository.currentTokenLimit() } returns 512
        coEvery { settingsRepository.getApiKeyOrNull(ProviderType.QWEN) } returns "sk-qwen"
        every { settingsRepository.selectedStyle } returns flowOf(AnswerStylePreset.SPOKEN)
        coEvery { knowledgeRetriever.retrieve("如何推进跨团队项目？") } returns RetrievalResult(
            chunks = retrievedChunks,
            queryEmbeddingCosts = listOf(
                QueryEmbeddingCostSnapshot(
                    provider = ProviderType.QWEN,
                    modelId = "text-embedding-v4",
                    usage = UsageCost(promptTokens = 3, completionTokens = 0, totalTokens = 0, estimatedCostUsd = 0.0001),
                    latencyMs = 80L,
                    createdAt = 10L,
                ),
            ),
        )
        every {
            ragContextAssembler.assemble(
                question = "如何推进跨团队项目？",
                retrievedChunks = retrievedChunks,
                maxOutputTokens = 512,
                previousAnswer = null,
            )
        } returns RagContextAssembly(chunks = retrievedChunks, usedTokens = 40)
        coEvery {
            chatProvider.generateAnswer(
                apiKey = "sk-qwen",
                request = match {
                    it.question == "如何推进跨团队项目？" &&
                        it.stylePreset == AnswerStylePreset.SPOKEN &&
                        it.ragContext == retrievedChunks
                },
            )
        } returns ProviderAnswerResult(
            rawText = "结构化回答",
            structuredAnswer = structuredAnswer,
            usage = UsageCost(promptTokens = 12, completionTokens = 18, totalTokens = 0, estimatedCostUsd = 0.1234),
            modelId = "qwen-plus",
        )

        val session = repository.generateAnswer(
            question = "如何推进跨团队项目？",
            sourceType = QuestionSourceType.TEXT,
        )

        assertEquals(SessionStatus.COMPLETED, session.status)
        assertEquals(structuredAnswer, session.answer)
        assertEquals(15, session.usage.promptTokens)
        assertEquals(18, session.usage.completionTokens)
        assertEquals(30, session.usage.totalTokens)

        val savedSession = repository.getSession(session.id)
        val costRecords = repository.getCostRecords(session.id)

        assertNotNull(savedSession)
        assertEquals(2, costRecords.size)
        assertTrue(costRecords.any { it.modelId == "qwen-plus" })
        assertTrue(costRecords.any { it.modelId == "text-embedding-v4" })
    }

    @Test
    fun generateAnswer_whenProviderFails_marksSessionAsFailed() = runTest(mainDispatcherRule.testDispatcher.scheduler) {
        coEvery { settingsRepository.currentModel() } returns ModelCatalog.defaultModel()
        coEvery { settingsRepository.currentTokenLimit() } returns 1024
        coEvery { settingsRepository.getApiKeyOrNull(ProviderType.QWEN) } returns "sk-qwen"
        every { settingsRepository.selectedStyle } returns flowOf(AnswerStylePreset.DETAILED)
        coEvery { knowledgeRetriever.retrieve("失败问题") } returns RetrievalResult()
        every {
            ragContextAssembler.assemble(
                question = "失败问题",
                retrievedChunks = emptyList(),
                maxOutputTokens = 1024,
                previousAnswer = null,
            )
        } returns RagContextAssembly()
        coEvery { chatProvider.generateAnswer("sk-qwen", any()) } throws IllegalStateException("provider down")

        var failure: Throwable? = null
        try {
            repository.generateAnswer("失败问题", QuestionSourceType.TEXT)
        } catch (throwable: Throwable) {
            failure = throwable
        }

        val sessions = repository.observeSessions("").first()

        assertNotNull(failure)
        assertEquals("生成失败，请稍后重试。", failure?.message)
        assertEquals(1, sessions.size)
        assertEquals(SessionStatus.FAILED, sessions.single().status)
        assertEquals("生成失败，请稍后重试。", sessions.single().errorMessage)
        assertTrue(repository.getCostRecords(sessions.single().id).isEmpty())
    }

    @Test
    fun transcribeAudio_usesConfiguredSpeechProvider() = runTest(mainDispatcherRule.testDispatcher.scheduler) {
        coEvery { settingsRepository.currentSpeechToTextModel() } returns ModelCatalog.defaultSpeechToTextModel()
        coEvery { settingsRepository.getApiKeyOrNull(ProviderType.QWEN) } returns "sk-qwen"
        coEvery {
            speechToTextProvider.transcribeAudio(
                apiKey = "sk-qwen",
                request = match { it.audioFilePath == "/tmp/interview.m4a" && it.modelId == "qwen3-asr-flash" },
            )
        } returns TranscriptionResult(text = "这是转写文本")

        val result = repository.transcribeAudio("/tmp/interview.m4a")

        assertEquals("这是转写文本", result.text)
        coVerify(exactly = 1) { speechToTextProvider.transcribeAudio("sk-qwen", any()) }
    }
}
