package com.kuaida.core.data.repository

import com.kuaida.core.data.database.CostRecordDao
import com.kuaida.core.data.database.ProviderCredentialDao
import com.kuaida.core.data.database.ProviderCredentialEntity
import com.kuaida.core.data.datastore.UserPreferences
import com.kuaida.core.data.datastore.UserPreferencesDataSource
import com.kuaida.core.data.provider.ProviderRegistry
import com.kuaida.core.data.security.EncryptedPayload
import com.kuaida.core.data.security.SecureSecretStorage
import com.kuaida.core.model.AnswerStylePreset
import com.kuaida.core.model.ProviderType
import com.kuaida.core.model.SettingsDefaults
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryTest {
    private val preferences = mockk<UserPreferencesDataSource>()
    private val providerCredentialDao = mockk<ProviderCredentialDao>()
    private val costRecordDao = mockk<CostRecordDao>()
    private val secureSecretStorage = mockk<SecureSecretStorage>()
    private val providerRegistry = mockk<ProviderRegistry>()

    @Test
    fun saveApiKey_encryptsAndUpsertsCredential() = runTest {
        val encrypted = EncryptedPayload(ciphertext = byteArrayOf(1, 2, 3), iv = byteArrayOf(4, 5))
        every { secureSecretStorage.encrypt("sk-qwen-1234") } returns encrypted
        coEvery { providerCredentialDao.upsert(any()) } just runs
        val repository = buildRepository()

        repository.saveApiKey(ProviderType.QWEN, "sk-qwen-1234")

        coVerify {
            providerCredentialDao.upsert(
                match {
                    it.provider == ProviderType.QWEN &&
                        it.last4 == "1234" &&
                        it.isConfigured &&
                        it.encryptedKey.contentEquals(encrypted.ciphertext) &&
                        it.iv.contentEquals(encrypted.iv)
                },
            )
        }
    }

    @Test
    fun saveApiKey_withBlankValueDeletesCredential() = runTest {
        coEvery { providerCredentialDao.delete(ProviderType.DEEPSEEK) } just runs
        val repository = buildRepository()

        repository.saveApiKey(ProviderType.DEEPSEEK, "   ")

        coVerify(exactly = 1) { providerCredentialDao.delete(ProviderType.DEEPSEEK) }
    }

    @Test
    fun settingsSummary_clampsTokenLimitAndReflectsConfiguredProviders() = runTest {
        val prefsFlow = MutableStateFlow(
            UserPreferences(
                selectedProvider = ProviderType.DEEPSEEK,
                selectedModelId = "deepseek-chat",
                selectedEmbeddingProvider = ProviderType.QWEN,
                selectedEmbeddingModelId = "text-embedding-v4",
                selectedSpeechToTextProvider = ProviderType.QWEN,
                selectedSpeechToTextModelId = "qwen3-asr-flash",
                selectedStyle = AnswerStylePreset.SPOKEN,
                tokenLimit = SettingsDefaults.MAX_TOKEN_LIMIT + 256,
                highCostAlert = false,
            ),
        )
        val repository = buildRepository(
            prefsFlow = prefsFlow,
            credentialsFlow = flowOf(
                listOf(
                    ProviderCredentialEntity(
                        provider = ProviderType.QWEN,
                        encryptedKey = byteArrayOf(9),
                        iv = byteArrayOf(8),
                        last4 = "9999",
                        isConfigured = true,
                        updatedAt = 1L,
                    ),
                ),
            ),
            totalTokensFlow = flowOf(321),
            totalCostFlow = flowOf(1.2345),
        )

        val summary = repository.settingsSummary.valueOrFirst()

        assertEquals("deepseek-chat", summary.selectedModel.modelId)
        assertEquals(ProviderType.DEEPSEEK, summary.selectedModel.provider)
        assertEquals(SettingsDefaults.MAX_TOKEN_LIMIT, summary.tokenLimit)
        assertEquals(321, summary.totalTokens)
        assertEquals(1.2345, summary.totalCostUsd, 0.0)
        assertTrue(summary.configuredProviders[ProviderType.QWEN] == true)
        assertFalse(summary.configuredProviders[ProviderType.ZHIPU] == true)
        assertFalse(summary.highCostAlertEnabled)
    }

    @Test
    fun updateTokenLimit_clampsBeforePersisting() = runTest {
        val captured = AtomicInteger()
        coEvery { preferences.setTokenLimit(any()) } coAnswers {
            captured.set(firstArg())
        }
        val repository = buildRepository()

        repository.updateTokenLimit(SettingsDefaults.MAX_TOKEN_LIMIT + 512)

        assertEquals(SettingsDefaults.MAX_TOKEN_LIMIT, captured.get())
        coVerify(exactly = 1) { preferences.setTokenLimit(SettingsDefaults.MAX_TOKEN_LIMIT) }
    }

    @Test
    fun maskedApiKeys_returnsDotsWithTailDigits() = runTest {
        val repository = buildRepository(
            credentialsFlow = flowOf(
                listOf(
                    ProviderCredentialEntity(
                        provider = ProviderType.ZHIPU,
                        encryptedKey = byteArrayOf(1),
                        iv = byteArrayOf(2),
                        last4 = "abcd",
                        isConfigured = true,
                        updatedAt = 2L,
                    ),
                    ProviderCredentialEntity(
                        provider = ProviderType.DEEPSEEK,
                        encryptedKey = byteArrayOf(3),
                        iv = byteArrayOf(4),
                        last4 = "0000",
                        isConfigured = false,
                        updatedAt = 3L,
                    ),
                ),
            ),
        )

        val masked = repository.maskedApiKeys()

        assertEquals("••••••••••••abcd", masked[ProviderType.ZHIPU])
        assertEquals("", masked[ProviderType.DEEPSEEK])
    }

    @Test
    fun getApiKeyOrNull_returnsNullWhenCredentialIsNotConfigured() = runTest {
        coEvery { providerCredentialDao.get(ProviderType.DEEPSEEK) } returns ProviderCredentialEntity(
            provider = ProviderType.DEEPSEEK,
            encryptedKey = byteArrayOf(7),
            iv = byteArrayOf(8),
            last4 = "9999",
            isConfigured = false,
            updatedAt = 4L,
        )
        val repository = buildRepository()

        val apiKey = repository.getApiKeyOrNull(ProviderType.DEEPSEEK)

        assertEquals(null, apiKey)
    }

    private fun buildRepository(
        prefsFlow: Flow<UserPreferences> = MutableStateFlow(defaultUserPreferences()),
        credentialsFlow: Flow<List<ProviderCredentialEntity>> = flowOf(emptyList()),
        totalTokensFlow: Flow<Int> = flowOf(0),
        totalCostFlow: Flow<Double> = flowOf(0.0),
    ): SettingsRepository {
        every { preferences.preferences } returns prefsFlow
        every { providerCredentialDao.observeAll() } returns credentialsFlow
        every { costRecordDao.observeTotalTokens() } returns totalTokensFlow
        every { costRecordDao.observeTotalCost() } returns totalCostFlow
        return SettingsRepository(
            preferences = preferences,
            providerCredentialDao = providerCredentialDao,
            costRecordDao = costRecordDao,
            secureSecretStorage = secureSecretStorage,
            providerRegistry = providerRegistry,
        )
    }

    private fun defaultUserPreferences() = UserPreferences(
        selectedProvider = ProviderType.QWEN,
        selectedModelId = "qwen-plus",
        selectedEmbeddingProvider = ProviderType.QWEN,
        selectedEmbeddingModelId = "text-embedding-v4",
        selectedSpeechToTextProvider = ProviderType.QWEN,
        selectedSpeechToTextModelId = "qwen3-asr-flash",
        selectedStyle = AnswerStylePreset.DETAILED,
        tokenLimit = SettingsDefaults.DEFAULT_TOKEN_LIMIT,
        highCostAlert = true,
    )

    private suspend fun kotlinx.coroutines.flow.Flow<com.kuaida.core.model.SettingsSummary>.valueOrFirst() = first()
}
