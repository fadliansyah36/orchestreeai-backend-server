package ai.orchestree.backend

import ai.orchestree.backend.database.repositories.identity.TenantRepository
import ai.orchestree.backend.database.repositories.modelrouter.LlmProviderModelRepository
import ai.orchestree.backend.database.repositories.modelrouter.ProviderRegistryRepository
import ai.orchestree.backend.modelrouter.AllProvidersInChainFailedException
import ai.orchestree.backend.modelrouter.CostOptimization.ModelTieringEngine
import ai.orchestree.backend.modelrouter.LlmClient
import ai.orchestree.backend.modelrouter.LlmRequest
import ai.orchestree.backend.modelrouter.LlmResponse
import ai.orchestree.backend.modelrouter.ModelRouteRequest
import ai.orchestree.backend.modelrouter.ModelRouter
import ai.orchestree.backend.modelrouter.providers.OpenAiCompatibleLlmClient
import ai.orchestree.backend.scheduler.SchedulerEngine
import ai.orchestree.backend.scheduler.jobs.ProactiveDailyReportJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NvidiaNimDynamicCatalogAndTenantResolutionTest {

    @Test
    fun testNvidiaNimClientCostCalculation() {
        val client = OpenAiCompatibleLlmClient(
            providerId = "nvidia_nim",
            providerName = "NVIDIA NIM",
            baseUrl = "https://integrate.api.nvidia.com/v1",
            apiKeyProvider = { "nvapi-test-key-12345" },
            defaultModel = "meta/llama-3.3-70b-instruct"
        )
        // Check cost estimation
        val cost = client.calculateEstimatedCost("nvidia_nim", "meta/llama-3.3-70b-instruct", promptTokens = 1000, completionTokens = 500)
        assertTrue(cost > 0.0, "Cost should be calculated based on input/output tokens")
        // Prompt cost: 1000 * 0.00000030 = 0.0003, Completion cost: 500 * 0.00000060 = 0.0003 => 0.0006
        assertEquals(0.0006, cost, 0.00001)
    }

    @Test
    fun testDynamicModelTieringEngineSelection() {
        val modelRepo = LlmProviderModelRepository()
        // Register dynamically discovered models into catalog for tiering verification
        val sampleDiscovered = listOf("meta/llama-3.3-70b-instruct", "meta/llama-3.1-8b-instruct", "meta/llama-3.2-3b-instruct")
        for (m in sampleDiscovered) {
            val tier = modelRepo.classifyComplexityTier(m)
            modelRepo.registerModel(
                ai.orchestree.backend.database.repositories.modelrouter.LlmProviderModelEntity(
                    id = "nim-dynamic-${m.hashCode()}",
                    providerId = "llm-nvidia-nim",
                    providerCode = "NVIDIA_NIM",
                    modelIdentifier = m,
                    complexityTier = tier,
                    contextWindow = 131072,
                    supportsToolCalling = true,
                    isActive = true
                )
            )
        }
        val tieringEngine = ModelTieringEngine(modelRepo = modelRepo)

        // Frontier complexity
        val frontierTier = tieringEngine.mapTaskToComplexityTier("UNIVERSAL_PROMPT_COMPOSER", 100)
        assertEquals("frontier", frontierTier)

        // Select optimal model for frontier
        val (provider, model) = tieringEngine.selectOptimalModel("UNIVERSAL_PROMPT_COMPOSER", "CRITICAL", 200)
        assertEquals("nvidia_nim", provider)
        assertFalse(model.isBlank(), "Selected model must not be blank")

        // Moderate complexity
        val moderateTier = tieringEngine.mapTaskToComplexityTier("GENERAL_CHAT", 300)
        assertEquals("moderate", moderateTier)

        // Simple / fast complexity
        val simpleTier = tieringEngine.mapTaskToComplexityTier("FAST_TRIAGE", 50)
        assertEquals("simple", simpleTier)
    }

    @Test
    fun testFailClosedWhenAllProvidersFail() = runBlocking {
        val providerRepo = ProviderRegistryRepository()
        val failingClient = object : LlmClient {
            override val providerId = "nvidia_nim"
            override val providerName = "NVIDIA NIM"
            override suspend fun complete(request: LlmRequest): Result<LlmResponse> {
                return Result.failure(RuntimeException("NVIDIA NIM Error 500"))
            }
        }

        val router = ModelRouter(
            providerRepo = providerRepo,
            customProviders = mapOf(
                "nvidia_nim" to failingClient
            )
        )

        val result = router.route(
            ModelRouteRequest(
                taskCategory = "COMPLEX_REASONING",
                prompt = "Test prompt",
                tenantId = "tenant-fail-test"
            )
        )

        assertTrue(result.isFailure, "Router must fail when all providers fail")
        val ex = result.exceptionOrNull()
        assertTrue(ex is AllProvidersInChainFailedException, "Must throw AllProvidersInChainFailedException")
    }

    @Test
    fun testDynamicTenantResolutionInScheduler() = runBlocking {
        val executedTenants = mutableListOf<String>()

        val mockProactiveJob = object : ProactiveDailyReportJob() {
            override suspend fun execute(tenantId: String): String {
                executedTenants.add(tenantId)
                return "SUCCESS"
            }
        }

        val mockTenantRepo = object : TenantRepository() {
            override suspend fun getActiveTenantIds(): List<String> {
                return listOf("tenant-dynamic-alpha", "tenant-dynamic-beta")
            }
        }

        val scheduler = SchedulerEngine(
            proactiveReportJob = mockProactiveJob,
            tenantRepo = mockTenantRepo
        )

        scheduler.start(proactiveIntervalMs = 10_000)
        delay(200) // allow job to run first iteration
        scheduler.stop()

        assertTrue(executedTenants.contains("tenant-dynamic-alpha"), "Should execute for dynamic tenant alpha")
        assertTrue(executedTenants.contains("tenant-dynamic-beta"), "Should execute for dynamic tenant beta")
        assertFalse(executedTenants.contains("tenant-enterprise-001"), "Should NOT execute for hardcoded tenant-enterprise-001")
    }

    @Test
    fun testSchedulerSkipsWhenNoActiveTenantsFound() = runBlocking {
        val executedTenants = mutableListOf<String>()

        val mockProactiveJob = object : ProactiveDailyReportJob() {
            override suspend fun execute(tenantId: String): String {
                executedTenants.add(tenantId)
                return "SUCCESS"
            }
        }

        val mockTenantRepo = object : TenantRepository() {
            override suspend fun getActiveTenantIds(): List<String> {
                return emptyList()
            }
        }

        val scheduler = SchedulerEngine(
            proactiveReportJob = mockProactiveJob,
            tenantRepo = mockTenantRepo
        )

        scheduler.start(proactiveIntervalMs = 10_000)
        delay(200)
        scheduler.stop()

        assertTrue(executedTenants.isEmpty(), "Should skip execution when no active tenants are found")
    }
}
