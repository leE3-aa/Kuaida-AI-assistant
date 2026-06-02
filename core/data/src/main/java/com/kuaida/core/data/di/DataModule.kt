package com.kuaida.core.data.di

import android.content.Context
import androidx.room.Room
import com.kuaida.core.common.di.IoDispatcher
import com.kuaida.core.data.database.AppDatabase
import com.kuaida.core.data.database.APP_DATABASE_MIGRATIONS
import com.kuaida.core.data.provider.ChatProvider
import com.kuaida.core.data.provider.CustomOpenAiCompatibleProvider
import com.kuaida.core.data.provider.DeepSeekProvider
import com.kuaida.core.data.provider.EmbeddingProvider
import com.kuaida.core.data.provider.ProviderRegistry
import com.kuaida.core.data.provider.QwenProvider
import com.kuaida.core.data.provider.SpeechToTextProvider
import com.kuaida.core.data.provider.ZhipuProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "quick_answer.db")
            .addMigrations(*APP_DATABASE_MIGRATIONS)
            .build()
    }

    @Provides
    fun provideCredentialDao(database: AppDatabase) = database.providerCredentialDao()

    @Provides
    fun provideChatSessionDao(database: AppDatabase) = database.chatSessionDao()

    @Provides
    fun provideKnowledgeDocumentDao(database: AppDatabase) = database.knowledgeDocumentDao()

    @Provides
    fun provideKnowledgeChunkDao(database: AppDatabase) = database.knowledgeChunkDao()

    @Provides
    fun provideChunkEmbeddingDao(database: AppDatabase) = database.chunkEmbeddingDao()

    @Provides
    fun provideCostRecordDao(database: AppDatabase) = database.costRecordDao()

    @Provides
    @Singleton
    fun provideProviderRegistry(
        chatProviders: Set<@JvmSuppressWildcards ChatProvider>,
        embeddingProviders: Set<@JvmSuppressWildcards EmbeddingProvider>,
        speechToTextProviders: Set<@JvmSuppressWildcards SpeechToTextProvider>,
    ): ProviderRegistry {
        return ProviderRegistry(
            chatProviders = chatProviders,
            embeddingProviders = embeddingProviders,
            speechToTextProviders = speechToTextProviders,
        )
    }

    @Provides
    @Singleton
    fun provideQwenProvider(
        promptComposer: com.kuaida.core.data.provider.PromptComposer,
        client: OkHttpClient,
        json: Json,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): QwenProvider = QwenProvider(promptComposer, client, json, ioDispatcher)

    @Provides
    @IntoSet
    fun provideQwenChatProvider(provider: QwenProvider): ChatProvider = provider

    @Provides
    @IntoSet
    fun provideQwenEmbeddingProvider(provider: QwenProvider): EmbeddingProvider = provider

    @Provides
    @IntoSet
    fun provideQwenSpeechToTextProvider(provider: QwenProvider): SpeechToTextProvider = provider

    @Provides
    @Singleton
    fun provideZhipuProvider(
        promptComposer: com.kuaida.core.data.provider.PromptComposer,
        client: OkHttpClient,
        json: Json,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): ZhipuProvider = ZhipuProvider(promptComposer, client, json, ioDispatcher)

    @Provides
    @IntoSet
    fun provideZhipuChatProvider(provider: ZhipuProvider): ChatProvider = provider

    @Provides
    @IntoSet
    fun provideZhipuEmbeddingProvider(provider: ZhipuProvider): EmbeddingProvider = provider

    @Provides
    @IntoSet
    fun provideZhipuSpeechToTextProvider(provider: ZhipuProvider): SpeechToTextProvider = provider

    @Provides
    @Singleton
    fun provideDeepSeekProvider(
        promptComposer: com.kuaida.core.data.provider.PromptComposer,
        client: OkHttpClient,
        json: Json,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): DeepSeekProvider = DeepSeekProvider(promptComposer, client, json, ioDispatcher)

    @Provides
    @IntoSet
    fun provideDeepSeekChatProvider(provider: DeepSeekProvider): ChatProvider = provider

    @Provides
    @Singleton
    fun provideCustomOpenAiCompatibleProvider(
        userPreferencesDataSource: com.kuaida.core.data.datastore.UserPreferencesDataSource,
        promptComposer: com.kuaida.core.data.provider.PromptComposer,
        client: OkHttpClient,
        json: Json,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): CustomOpenAiCompatibleProvider = CustomOpenAiCompatibleProvider(
        userPreferencesDataSource = userPreferencesDataSource,
        promptComposer = promptComposer,
        client = client,
        json = json,
        ioDispatcher = ioDispatcher,
    )

    @Provides
    @IntoSet
    fun provideCustomChatProvider(provider: CustomOpenAiCompatibleProvider): ChatProvider = provider

    @Provides
    @IntoSet
    fun provideCustomEmbeddingProvider(provider: CustomOpenAiCompatibleProvider): EmbeddingProvider = provider

    @Provides
    @IntoSet
    fun provideCustomSpeechToTextProvider(provider: CustomOpenAiCompatibleProvider): SpeechToTextProvider = provider
}
