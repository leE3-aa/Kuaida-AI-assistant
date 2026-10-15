import pathlib

P = pathlib.Path('core/data/src/main/java/com/kuaida/core/data/repository/SettingsRepository.kt')
s = P.read_text(encoding='utf-8')

A = """        return secureSecretStorage.decrypt(entity.encryptedKey, entity.iv)
    }
"""
N = """        return secureSecretStorage.decrypt(entity.encryptedKey, entity.iv)
    }

    suspend fun fetchRemoteChatModels(): List<ModelDescriptor> {
        val provider = providerRegistry.findChatProvider(ProviderType.CUSTOM) ?: return emptyList()
        val apiKey = getApiKeyOrNull(ProviderType.CUSTOM) ?: return emptyList()
        return provider.listModels(apiKey)
    }
"""
assert s.count(A) == 1, 'A'
s = s.replace(A, N, 1)

P.write_text(s, encoding='utf-8')
print('ok b', len(s))
