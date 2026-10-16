package com.kuaida.core.data.repository

import com.kuaida.core.data.database.CostRecordDao
import com.kuaida.core.data.database.ProviderCredentialDao
import com.kuaida.core.data.database.ProviderCredentialEntity
import com.kuaida.core.data.datastore.UserPreferencesDataSource
import com.kuaida.core.data.provider.ProviderRegistry
import com.kuaida.core.data.provider.ModelCatalog
import com.kuaida.core.data.security.SecureSecretStorage
import com.kuaida.core.common.ui.displayLabel
import com.kuaida.core.model.CustomProviderConfig
import com.kuaida.core.model.AnswerStylePreset
import com.kuaida.core.model.ModelDescriptor
import com.kuaida.core.model.ProviderType
import com.kuaida.core.model.ProviderValidationResult
import com.kuaida.core.model.SettingsDefaults
import com.kuaida.core.model.SettingsSummary
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class SettingsRepository @Inject constructor(
    private val preferences: UserPreferencesDataSource,
    private val providerCredentialDao: ProviderCredentialDao,
    private val costRecordDao: CostRecordDao,
    private val secureSecretStorage: SecureSecretStorage,
    private val providerRegistry: ProviderRegistry,
) {
    val selectedModel: Flow<ModelDescriptor> = preferences.preferences.map {
        resolveChatModel(it)
    }

    val selectedEmbeddingModel: Flow<ModelDescriptor> = preferences.preferences.map {
        resolveEmbeddingModel(it)
    }

    val selectedSpeechToTextModel: Flow<ModelDescriptor> = preferences.preferences.map {
        ModelCatalog.findSpeechToText(it.selectedSpeechToTextProvider, it.selectedSpeechToTextModelId)
    }

    val selectedStyle: Flow<AnswerStylePreset> = preferences.preferences.map { it.selectedStyle }

    val selectedTokenLimit: Flow<Int> = preferences.preferences.map { prefs ->
        prefs.tokenLimit.coerceIn(SettingsDefaults.MIN_TOKEN_LIMIT, SettingsDefaults.MAX_TOKEN_LIMIT)
    }

    val configuredProviders: Flow<Map<ProviderType, Boolean>> = providerCredentialDao.observeAll().map { entries ->
        ProviderType.entries.associateWith { provider -> entries.any { it.provider == provider && it.isConfigured } }
    }

    private val configuredModels: Flow<Triple<ModelDescriptor, ModelDescriptor, ModelDescriptor>> = combine(
        selectedModel,
        selectedEmbeddingModel,
        selectedSpeechToTextModel,
    ) { model, embeddingModel, speechToTextModel ->
        Triple(model, embeddingModel, speechToTextModel)
    }

    val settingsSummary: Flow<SettingsSummary> = combine(
        configuredModels,
        configuredProviders,
        costRecordDao.observeTotalTokens(),
        costRecordDao.observeTotalCost(),
        preferences.preferences,
    ) { models, providers, totalTokens, totalCost, prefs ->
        val (model, embeddingModel, speechToTextModel) = models
        SettingsSummary(
            selectedModel = model,
            selectedStyle = prefs.selectedStyle,
            tokenLimit = prefs.tokenLimit.coerceIn(SettingsDefaults.MIN_TOKEN_LIMIT, SettingsDefaults.MAX_TOKEN_LIMIT),
            embeddingModel = embeddingModel,
            speechToTextModel = speechToTextModel,
            configuredProviders = providers,
            customProviderConfig = CustomProviderConfig(
                baseUrl = prefs.customBaseUrl,
                chatModelId = prefs.customChatModelId,
                embeddingModelId = prefs.customEmbeddingModelId,
            ),
            totalTokens = totalTokens,
            totalCostUsd = totalCost,
            highCostAlertEnabled = prefs.highCostAlert,
        )
    }

    fun modelsByProvider(provider: ProviderType): List<ModelDescriptor> = ModelCatalog.byProvider(provider)

    fun embeddingModelsByProvider(provider: ProviderType): List<ModelDescriptor> = ModelCatalog.embeddingByProvider(provider)

    fun speechToTextModelsByProvider(provider: ProviderType): List<ModelDescriptor> =
        ModelCatalog.speechToTextByProvider(provider)

    suspend fun updateCustomProviderConfig(baseUrl: String, chatModelId: String, embeddingModelId: String) {
        preferences.setCustomProviderConfig(
            baseUrl = baseUrl.trim(),
            chatModelId = chatModelId.trim(),
            embeddingModelId = embeddingModelId.trim(),
        )
    }

    suspend fun saveApiKey(provider: ProviderType, apiKey: String) {
        if (apiKey.isBlank()) {
            providerCredentialDao.delete(provider)
            return
        }
        val encrypted = secureSecretStorage.encrypt(apiKey)
        providerCredentialDao.upsert(
            ProviderCredentialEntity(
                provider = provider,
                encryptedKey = encrypted.ciphertext,
                iv = encrypted.iv,
                last4 = apiKey.takeLast(4),
                isConfigured = true,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun validateApiKey(provider: ProviderType, apiKey: String): ProviderValidationResult {
        val adapter = providerRegistry.findProvider(provider)
            ?: return ProviderValidationResult(
                success = false,
                message = "${provider.displayLabel()} 暂不支持 Key 校验。",
            )
        return adapter.validateKey(apiKey)
    }

    suspend fun getApiKeyOrNull(provider: ProviderType): String? {
        val entity = providerCredentialDao.get(provider) ?: return null
        if (!entity.isConfigured) return null
        return secureSecretStorage.decrypt(entity.encryptedKey, entity.iv)
    }

    suspend fun fetchRemoteChatModels(): List<ModelDescriptor> {
        val provider = providerRegistry.findChatProvider(ProviderType.CUSTOM) ?: return emptyList()
        val apiKey = getApiKeyOrNull(ProviderType.CUSTOM) ?: return emptyList()
        return provider.listModels(apiKey)
    }

    suspend fun markApiKeyInvalid(provider: ProviderType) {
        providerCredentialDao.updateConfiguredStatus(
            provider = provider,
            isConfigured = false,
            updatedAt = System.currentTimeMillis(),
        )
    }

    suspend fun currentModel(): ModelDescriptor = selectedModel.first()

    suspend fun currentEmbeddingModel(): ModelDescriptor = selectedEmbeddingModel.first()

    suspend fun currentSpeechToTextModel(): ModelDescriptor = selectedSpeechToTextModel.first()

    suspend fun currentTokenLimit(): Int = selectedTokenLimit.first()

    suspend fun updateSelectedModel(provider: ProviderType, modelId: String) {
        preferences.setSelectedModel(provider, modelId)
    }

    suspend fun updateSelectedEmbeddingModel(provider: ProviderType, modelId: String) {
        preferences.setSelectedEmbeddingModel(provider, modelId)
    }

    suspend fun updateSelectedSpeechToTextModel(provider: ProviderType, modelId: String) {
        preferences.setSelectedSpeechToTextModel(provider, modelId)
    }

    suspend fun updateStyle(stylePreset: AnswerStylePreset) {
        preferences.setSelectedStyle(stylePreset)
    }

    suspend fun updateTokenLimit(limit: Int) {
        preferences.setTokenLimit(limit.coerceIn(SettingsDefaults.MIN_TOKEN_LIMIT, SettingsDefaults.MAX_TOKEN_LIMIT))
    }

    suspend fun updateHighCostAlert(enabled: Boolean) {
        preferences.setHighCostAlert(enabled)
    }

    suspend fun maskedApiKeys(): Map<ProviderType, String> {
        return providerCredentialDao.observeAll().first().associate { entity ->
            entity.provider to if (entity.isConfigured) "••••••••••••${entity.last4}" else ""
        }
    }

    private fun resolveChatModel(prefs: com.kuaida.core.data.datastore.UserPreferences): ModelDescriptor {
        return if (prefs.selectedProvider == ProviderType.CUSTOM) {
            ModelCatalog.customChatModel(
                baseUrl = prefs.customBaseUrl,
                modelId = prefs.customChatModelId,
            )
        } else {
            ModelCatalog.find(prefs.selectedProvider, prefs.selectedModelId)
        }
    }

    private fun resolveEmbeddingModel(prefs: com.kuaida.core.data.datastore.UserPreferences): ModelDescriptor {
        return if (prefs.selectedEmbeddingProvider == ProviderType.CUSTOM) {
            ModelCatalog.customEmbeddingModel(
                baseUrl = prefs.customBaseUrl,
                modelId = prefs.customEmbeddingModelId,
            )
        } else {
            ModelCatalog.findEmbedding(prefs.selectedEmbeddingProvider, prefs.selectedEmbeddingModelId)
        }
    }
}
