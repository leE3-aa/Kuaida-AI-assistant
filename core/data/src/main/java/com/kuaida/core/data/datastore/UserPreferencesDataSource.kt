package com.kuaida.core.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kuaida.core.data.provider.ModelCatalog
import com.kuaida.core.model.AnswerStylePreset
import com.kuaida.core.model.ProviderType
import com.kuaida.core.model.SettingsDefaults
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userPrefsDataStore by preferencesDataStore(name = "quick_answer_preferences")

@Singleton
class UserPreferencesDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val selectedProvider = stringPreferencesKey("selected_provider")
        val selectedModelId = stringPreferencesKey("selected_model_id")
        val selectedEmbeddingProvider = stringPreferencesKey("selected_embedding_provider")
        val selectedEmbeddingModelId = stringPreferencesKey("selected_embedding_model_id")
        val selectedSpeechToTextProvider = stringPreferencesKey("selected_speech_to_text_provider")
        val selectedSpeechToTextModelId = stringPreferencesKey("selected_speech_to_text_model_id")
        val customBaseUrl = stringPreferencesKey("custom_base_url")
        val customChatModelId = stringPreferencesKey("custom_chat_model_id")
        val customEmbeddingModelId = stringPreferencesKey("custom_embedding_model_id")
        val selectedStyle = stringPreferencesKey("selected_style")
        val tokenLimit = intPreferencesKey("token_limit")
        val highCostAlert = booleanPreferencesKey("high_cost_alert")
    }

    val preferences: Flow<UserPreferences> = context.userPrefsDataStore.data.map(::mapPreferences)

    suspend fun setSelectedModel(provider: ProviderType, modelId: String) {
        context.userPrefsDataStore.edit {
            it[Keys.selectedProvider] = provider.name
            it[Keys.selectedModelId] = modelId
        }
    }

    suspend fun setSelectedStyle(stylePreset: AnswerStylePreset) {
        context.userPrefsDataStore.edit { it[Keys.selectedStyle] = stylePreset.name }
    }

    suspend fun setSelectedEmbeddingModel(provider: ProviderType, modelId: String) {
        context.userPrefsDataStore.edit {
            it[Keys.selectedEmbeddingProvider] = provider.name
            it[Keys.selectedEmbeddingModelId] = modelId
        }
    }

    suspend fun setSelectedSpeechToTextModel(provider: ProviderType, modelId: String) {
        context.userPrefsDataStore.edit {
            it[Keys.selectedSpeechToTextProvider] = provider.name
            it[Keys.selectedSpeechToTextModelId] = modelId
        }
    }

    suspend fun setCustomProviderConfig(baseUrl: String, chatModelId: String, embeddingModelId: String) {
        context.userPrefsDataStore.edit {
            it[Keys.customBaseUrl] = baseUrl.trim()
            it[Keys.customChatModelId] = chatModelId.trim()
            it[Keys.customEmbeddingModelId] = embeddingModelId.trim()
        }
    }

    suspend fun setTokenLimit(value: Int) {
        context.userPrefsDataStore.edit { it[Keys.tokenLimit] = value }
    }

    suspend fun setHighCostAlert(enabled: Boolean) {
        context.userPrefsDataStore.edit { it[Keys.highCostAlert] = enabled }
    }

    private fun mapPreferences(preferences: Preferences): UserPreferences {
        val defaultChatModel = ModelCatalog.defaultModel()
        val defaultEmbeddingModel = ModelCatalog.defaultEmbeddingModel()
        val defaultSpeechToTextModel = ModelCatalog.defaultSpeechToTextModel()
        return UserPreferences(
            selectedProvider = ProviderType.fromStoredValue(preferences[Keys.selectedProvider]),
            selectedModelId = preferences[Keys.selectedModelId] ?: defaultChatModel.modelId,
            selectedEmbeddingProvider = preferences[Keys.selectedEmbeddingProvider]
                ?.let(ProviderType::fromStoredValue)
                ?: defaultEmbeddingModel.provider,
            selectedEmbeddingModelId = preferences[Keys.selectedEmbeddingModelId] ?: defaultEmbeddingModel.modelId,
            selectedSpeechToTextProvider = preferences[Keys.selectedSpeechToTextProvider]
                ?.let(ProviderType::fromStoredValue)
                ?: defaultSpeechToTextModel.provider,
            selectedSpeechToTextModelId = preferences[Keys.selectedSpeechToTextModelId] ?: defaultSpeechToTextModel.modelId,
            customBaseUrl = preferences[Keys.customBaseUrl] ?: SettingsDefaults.DEFAULT_CUSTOM_BASE_URL,
            customChatModelId = preferences[Keys.customChatModelId] ?: SettingsDefaults.DEFAULT_CUSTOM_CHAT_MODEL,
            customEmbeddingModelId = preferences[Keys.customEmbeddingModelId] ?: SettingsDefaults.DEFAULT_CUSTOM_EMBEDDING_MODEL,
            selectedStyle = preferences[Keys.selectedStyle]?.let(AnswerStylePreset::valueOf) ?: AnswerStylePreset.DETAILED,
            tokenLimit = preferences[Keys.tokenLimit] ?: SettingsDefaults.DEFAULT_TOKEN_LIMIT,
            highCostAlert = preferences[Keys.highCostAlert] ?: true,
        )
    }
}

data class UserPreferences(
    val selectedProvider: ProviderType,
    val selectedModelId: String,
    val selectedEmbeddingProvider: ProviderType,
    val selectedEmbeddingModelId: String,
    val selectedSpeechToTextProvider: ProviderType,
    val selectedSpeechToTextModelId: String,
    val customBaseUrl: String = SettingsDefaults.DEFAULT_CUSTOM_BASE_URL,
    val customChatModelId: String = SettingsDefaults.DEFAULT_CUSTOM_CHAT_MODEL,
    val customEmbeddingModelId: String = SettingsDefaults.DEFAULT_CUSTOM_EMBEDDING_MODEL,
    val selectedStyle: AnswerStylePreset,
    val tokenLimit: Int,
    val highCostAlert: Boolean,
)
