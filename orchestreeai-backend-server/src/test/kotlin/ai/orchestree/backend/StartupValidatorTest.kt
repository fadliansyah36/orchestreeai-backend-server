package ai.orchestree.backend

import ai.orchestree.backend.config.AppConfig
import ai.orchestree.backend.config.RedisConfig
import ai.orchestree.backend.config.SecurityConfig
import ai.orchestree.backend.config.SupabaseConfig
import ai.orchestree.backend.startup.StartupValidationException
import ai.orchestree.backend.startup.StartupValidator
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertTrue

class StartupValidatorTest {

    @Test
    fun testStartupWithMissingRequiredEnvVarsFailsClosed() {
        val validator = StartupValidator(shouldExitProcessOnFailure = false)
        val badConfig = AppConfig(
            environment = "production",
            supabase = SupabaseConfig(
                url = "",
                anonKey = "",
                serviceRoleKey = "",
                databaseUrl = "",
                databaseDirectUrl = ""
            ),
            security = SecurityConfig(jwtSecretKey = "", masterKeyRef = "")
        )

        val exception = assertThrows<StartupValidationException> {
            validator.verifyRequiredEnvVars(badConfig)
        }

        assertTrue(exception.message!!.contains("RequiredEnvVars"))
        assertTrue(exception.message!!.contains("SUPABASE_URL"))
    }

    @Test
    fun testStartupWithInvalidDatabaseEndpointFailsClosed() = runBlocking {
        val validator = StartupValidator(shouldExitProcessOnFailure = false)
        val badDbConfig = AppConfig(
            environment = "development",
            supabase = SupabaseConfig(
                url = "https://invalid-non-existent-supabase-host-99999.co",
                anonKey = "anon-key",
                serviceRoleKey = "service-role-key",
                databaseUrl = "",
                databaseDirectUrl = ""
            ),
            security = SecurityConfig(
                jwtSecretKey = "test-jwt-secret-key-at-least-32-chars-long",
                masterKeyRef = "master-key-ref"
            )
        )

        val exception = assertThrows<StartupValidationException> {
            validator.verifyDatabaseConnection(badDbConfig)
        }

        assertTrue(exception.message!!.contains("DatabaseConnection"))
    }

    @Test
    fun testStartupWithEmptyDatabaseKeysFailsClosed() = runBlocking {
        val validator = StartupValidator(shouldExitProcessOnFailure = false)
        val badDbConfig = AppConfig(
            supabase = SupabaseConfig(
                url = "",
                anonKey = "",
                serviceRoleKey = "",
                databaseUrl = "",
                databaseDirectUrl = ""
            )
        )

        val exception = assertThrows<StartupValidationException> {
            validator.verifyDatabaseConnection(badDbConfig)
        }

        assertTrue(exception.message!!.contains("DatabaseConnection"))
    }

    @Test
    fun testStartupWithValidEnvPassesRequiredVarsCheck() {
        val validator = StartupValidator(shouldExitProcessOnFailure = false)
        val validConfig = AppConfig(
            environment = "development",
            supabase = SupabaseConfig(
                url = "https://example.supabase.co",
                anonKey = "valid-anon-key",
                serviceRoleKey = "valid-service-role-key",
                databaseUrl = "postgresql://postgres:postgres@localhost:5432/postgres",
                databaseDirectUrl = "postgresql://postgres:postgres@localhost:5432/postgres"
            ),
            redis = RedisConfig(url = "redis://localhost:6379"),
            security = SecurityConfig(
                jwtSecretKey = "super-secret-production-jwt-key-minimum-32-chars",
                masterKeyRef = "master-key-ref"
            )
        )

        // Should not throw exception
        validator.verifyRequiredEnvVars(validConfig)
    }

    @Test
    fun testVerifyNoHardcodedProviderFallbackThrowsWhenTableEmpty() = runBlocking {
        val validator = StartupValidator(shouldExitProcessOnFailure = false)
        val stubRepo = object : ai.orchestree.backend.database.repositories.modelrouter.ProviderRegistryRepository() {
            override fun getAllActive(): List<ai.orchestree.backend.database.repositories.modelrouter.LlmProviderEntity> = emptyList()
        }

        val ex = org.junit.jupiter.api.assertThrows<ai.orchestree.backend.startup.FatalConfigurationException> {
            validator.verifyNoHardcodedProviderFallback(stubRepo)
        }
        assertTrue(ex.message!!.contains("Tabel llm_providers kosong"))
    }

    @Test
    fun testVerifyNoHardcodedProviderFallbackThrowsWhenPrimaryCredentialsMissing() = runBlocking {
        val validator = StartupValidator(shouldExitProcessOnFailure = false)
        val stubRepo = object : ai.orchestree.backend.database.repositories.modelrouter.ProviderRegistryRepository() {
            override fun getAllActive(): List<ai.orchestree.backend.database.repositories.modelrouter.LlmProviderEntity> = listOf(
                ai.orchestree.backend.database.repositories.modelrouter.LlmProviderEntity(
                    id = "llm-openrouter",
                    name = "OpenRouter",
                    providerCode = "OPENROUTER",
                    apiBaseUrl = "https://openrouter.ai/api/v1",
                    apiKeyEnv = "COMPLETELY_MISSING_API_KEY_ENV",
                    priority = 1,
                    fallbackPriority = 1,
                    taskSpecialization = "general"
                )
            )
        }

        val ex = org.junit.jupiter.api.assertThrows<ai.orchestree.backend.startup.FatalConfigurationException> {
            validator.verifyNoHardcodedProviderFallback(stubRepo)
        }
        assertTrue(ex.message!!.contains("tidak memiliki kredensial yang valid"))
    }

    @Test
    fun testVerifyNoHardcodedProviderFallbackThrowsWhenGeminiIsPrimaryWhileOthersExist() = runBlocking {
        val validator = StartupValidator(shouldExitProcessOnFailure = false)
        val stubRepo = object : ai.orchestree.backend.database.repositories.modelrouter.ProviderRegistryRepository() {
            override fun getAllActive(): List<ai.orchestree.backend.database.repositories.modelrouter.LlmProviderEntity> = listOf(
                ai.orchestree.backend.database.repositories.modelrouter.LlmProviderEntity(
                    id = "llm-gemini",
                    name = "Google Gemini",
                    providerCode = "GEMINI",
                    apiBaseUrl = "https://generativelanguage.googleapis.com",
                    apiKeyEnv = "PATH",
                    priority = 1,
                    fallbackPriority = 1,
                    taskSpecialization = "general"
                ),
                ai.orchestree.backend.database.repositories.modelrouter.LlmProviderEntity(
                    id = "llm-groq",
                    name = "Groq",
                    providerCode = "GROQ",
                    apiBaseUrl = "https://api.groq.com/openai/v1",
                    apiKeyEnv = "PATH",
                    priority = 2,
                    fallbackPriority = 2,
                    taskSpecialization = "fast"
                )
            )
        }

        val ex = org.junit.jupiter.api.assertThrows<ai.orchestree.backend.startup.FatalConfigurationException> {
            validator.verifyNoHardcodedProviderFallback(stubRepo)
        }
        assertTrue(ex.message!!.contains("Google Gemini terkonfigurasi sebagai prioritas 1"))
    }

    @Test
    fun testVerifyNoHardcodedProviderFallbackPassesForStandardProviders() = runBlocking {
        val validator = StartupValidator(shouldExitProcessOnFailure = false)
        val stubRepo = object : ai.orchestree.backend.database.repositories.modelrouter.ProviderRegistryRepository() {
            override fun getAllActive(): List<ai.orchestree.backend.database.repositories.modelrouter.LlmProviderEntity> = listOf(
                ai.orchestree.backend.database.repositories.modelrouter.LlmProviderEntity(
                    id = "llm-openrouter",
                    name = "OpenRouter",
                    providerCode = "OPENROUTER",
                    apiBaseUrl = "https://openrouter.ai/api/v1",
                    apiKeyEnv = "PATH",
                    priority = 1,
                    fallbackPriority = 1,
                    taskSpecialization = "general"
                ),
                ai.orchestree.backend.database.repositories.modelrouter.LlmProviderEntity(
                    id = "llm-groq",
                    name = "Groq",
                    providerCode = "GROQ",
                    apiBaseUrl = "https://api.groq.com/openai/v1",
                    apiKeyEnv = "PATH",
                    priority = 2,
                    fallbackPriority = 2,
                    taskSpecialization = "fast"
                )
            )
        }

        // Should not throw
        validator.verifyNoHardcodedProviderFallback(stubRepo)
    }
}
