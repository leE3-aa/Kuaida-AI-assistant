package com.kuaida.core.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kuaida.core.data.database.AppDatabase
import com.kuaida.core.data.provider.EmbeddingProvider
import com.kuaida.core.data.provider.ModelCatalog
import com.kuaida.core.data.provider.ProviderRegistry
import com.kuaida.core.data.rag.ChunkedText
import com.kuaida.core.data.rag.Chunker
import com.kuaida.core.data.rag.DocumentTextExtractor
import com.kuaida.core.data.rag.ExtractedDocument
import com.kuaida.core.data.testing.MainDispatcherRule
import com.kuaida.core.model.CostRecordCategory
import com.kuaida.core.model.DocumentParseStatus
import com.kuaida.core.model.EmbeddingResult
import com.kuaida.core.model.ProviderType
import com.kuaida.core.model.UsageCost
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
class KnowledgeRepositoryTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var database: AppDatabase
    private lateinit var repository: KnowledgeRepository

    private val extractor = mockk<DocumentTextExtractor>()
    private val chunker = mockk<Chunker>()
    private val settingsRepository = mockk<SettingsRepository>()
    private val embeddingProvider = mockk<EmbeddingProvider>()
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        every { embeddingProvider.type } returns ProviderType.QWEN

        repository = KnowledgeRepository(
            context = context,
            database = database,
            documentDao = database.knowledgeDocumentDao(),
            chunkDao = database.knowledgeChunkDao(),
            embeddingDao = database.chunkEmbeddingDao(),
            costRecordDao = database.costRecordDao(),
            extractor = extractor,
            chunker = chunker,
            settingsRepository = settingsRepository,
            providerRegistry = ProviderRegistry(
                chatProviders = emptySet(),
                embeddingProviders = setOf(embeddingProvider),
                speechToTextProviders = emptySet(),
            ),
            json = json,
            ioDispatcher = mainDispatcherRule.testDispatcher,
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun importDocument_withoutConfiguredProvider_fallsBackToKeywordRetrieval() = runTest(mainDispatcherRule.testDispatcher.scheduler) {
        val uri = Uri.parse("content://kuaida/documents/guide")
        every { extractor.extract(uri, any()) } returns ExtractedDocument(
            name = "面试手册.pdf",
            mimeType = "application/pdf",
            sizeBytes = 2048L,
            text = "第一段\n第二段",
        )
        every { chunker.chunk("第一段\n第二段") } returns listOf(
            ChunkedText(index = 0, content = "第一段", tokenCount = 3, hash = "hash_0"),
            ChunkedText(index = 1, content = "第二段", tokenCount = 3, hash = "hash_1"),
        )
        coEvery { settingsRepository.currentEmbeddingModel() } returns ModelCatalog.defaultEmbeddingModel()
        coEvery { settingsRepository.getApiKeyOrNull(ProviderType.QWEN) } returns null

        val result = repository.importDocument(uri)
        val document = repository.observeDocuments("").first().single()
        val chunks = repository.getChunks(result.documentId)

        assertEquals("面试手册.pdf", result.displayName)
        assertEquals(2, result.chunkCount)
        assertEquals(0, result.embeddingCount)
        assertNotNull(result.warningMessage)
        assertEquals(DocumentParseStatus.READY, document.parseStatus)
        assertEquals(2, document.chunkCount)
        assertEquals(0, document.embeddingCount)
        assertEquals(2, chunks.size)
        assertTrue(chunks.all { it.embedding.isEmpty() })
        assertTrue(repository.getDocumentCosts(result.documentId).isEmpty())
    }

    @Test
    fun importDocument_withEmbeddingProvider_persistsEmbeddingsAndCosts() = runTest(mainDispatcherRule.testDispatcher.scheduler) {
        val uri = Uri.parse("content://kuaida/documents/playbook")
        every { extractor.extract(uri, any()) } returns ExtractedDocument(
            name = "项目推进.md",
            mimeType = "text/markdown",
            sizeBytes = 512L,
            text = "推进策略",
        )
        every { chunker.chunk("推进策略") } returns listOf(
            ChunkedText(index = 0, content = "推进策略", tokenCount = 4, hash = "hash_embed"),
        )
        coEvery { settingsRepository.currentEmbeddingModel() } returns ModelCatalog.defaultEmbeddingModel()
        coEvery { settingsRepository.getApiKeyOrNull(ProviderType.QWEN) } returns "sk-qwen"
        coEvery {
            embeddingProvider.embed(
                apiKey = "sk-qwen",
                request = match { it.input == "推进策略" && it.modelId == "text-embedding-v4" },
            )
        } returns EmbeddingResult(
            embedding = listOf(0.12f, 0.34f, 0.56f),
            usage = UsageCost(promptTokens = 4, completionTokens = 0, totalTokens = 0, estimatedCostUsd = 0.0002),
        )

        val result = repository.importDocument(uri)
        val chunks = repository.getChunks(result.documentId)
        val costs = repository.getDocumentCosts(result.documentId)

        assertEquals(1, result.embeddingCount)
        assertEquals(ProviderType.QWEN, result.embeddingProvider)
        assertEquals("text-embedding-v4", result.embeddingModelId)
        assertTrue(result.warningMessage.isNullOrBlank())
        assertEquals(listOf(0.12f, 0.34f, 0.56f), chunks.single().embedding)
        assertEquals(1, costs.size)
        assertEquals(CostRecordCategory.KNOWLEDGE_EMBEDDING, costs.single().category)
        assertEquals(4, costs.single().usage.totalTokens)
    }

    @Test
    fun importDocument_whenChunkingFails_marksDocumentAsFailed() = runTest(mainDispatcherRule.testDispatcher.scheduler) {
        val uri = Uri.parse("content://kuaida/documents/empty")
        every { extractor.extract(uri, any()) } returns ExtractedDocument(
            name = "空文件.txt",
            mimeType = "text/plain",
            sizeBytes = 1L,
            text = "没有可用 chunk",
        )
        every { chunker.chunk("没有可用 chunk") } returns emptyList()

        var failure: Throwable? = null
        try {
            repository.importDocument(uri)
        } catch (throwable: Throwable) {
            failure = throwable
        }

        val document = repository.observeDocuments("").first().single()

        assertNotNull(failure)
        assertEquals(DocumentParseStatus.FAILED, document.parseStatus)
        assertTrue(document.failureReason?.contains("未能从文件中切分出可用内容") == true)
    }
}
