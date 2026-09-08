package ai.orchestree.backend

import ai.orchestree.backend.api.AdminAppRegistryCreateRequest
import ai.orchestree.backend.api.AdminLlmProviderCreateRequest
import ai.orchestree.backend.api.AdminMasterDataCreateRequest
import ai.orchestree.backend.api.AdminMcpToolCreateRequest
import ai.orchestree.backend.api.AdminSkillPluginCreateRequest
import ai.orchestree.backend.api.AdminTenantCreateRequest
import ai.orchestree.backend.config.AppConfig
import ai.orchestree.backend.config.SecurityConfig
import ai.orchestree.backend.plugins.configureAuthentication
import ai.orchestree.backend.plugins.configureHTTPS
import ai.orchestree.backend.plugins.configureRouting
import ai.orchestree.backend.plugins.configureSerialization
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BagianGComprehensiveTest {

    private val jwtSecret = SecurityConfig.fromEnv().jwtSecretKey.ifBlank { "101ffa9b-10c9-4e15-9390-90c2d32ed6c8" }
    private val algorithm = Algorithm.HMAC256(jwtSecret)
    private val json = Json { ignoreUnknownKeys = true }

    private fun generateSuperAdminToken(): String {
        return JWT.create()
            .withSubject("usr-superadmin-01")
            .withClaim("sub", "usr-superadmin-01")
            .withClaim("tenant_id", "system-platform")
            .withClaim("role", "SUPER_ADMIN")
            .withIssuedAt(Date())
            .withExpiresAt(Date(System.currentTimeMillis() + 3600 * 1000))
            .sign(algorithm)
    }

    @Test
    fun testSuperAdminCrudEndpoints() = testApplication {
        application {
            configureSerialization()
            configureHTTPS()
            configureAuthentication(AppConfig.load())
            configureRouting()
        }

        val client = createClient {}
        val token = generateSuperAdminToken()

        // 1. Tenants (Fase 25.1)
        val tenantsGet = client.get("/api/v1/admin/tenants") {
            header("Authorization", "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, tenantsGet.status)
        assertTrue(tenantsGet.bodyAsText().contains("PT Nusantara Energy"))

        val tenantPost = client.post("/api/v1/admin/tenants") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(AdminTenantCreateRequest(name = "PT Solusi Digital", tier = "ENTERPRISE", ownerEmail = "owner@solusi.com")))
        }
        assertEquals(HttpStatusCode.Created, tenantPost.status)
        assertTrue(tenantPost.bodyAsText().contains("PT Solusi Digital"))

        // 2. LLM Providers (Fase 93.A)
        val llmGet = client.get("/api/v1/admin/llm-providers") {
            header("Authorization", "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, llmGet.status)
        assertTrue(llmGet.bodyAsText().contains("OpenRouter"))

        val llmPost = client.post("/api/v1/admin/llm-providers") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(AdminLlmProviderCreateRequest(name = "TogetherAI", providerType = "OPENAI_COMPATIBLE")))
        }
        assertEquals(HttpStatusCode.Created, llmPost.status)
        assertTrue(llmPost.bodyAsText().contains("TogetherAI"))

        // 3. MCP Tool Registry (Fase 93.B)
        val mcpGet = client.get("/api/v1/admin/mcp-tools") {
            header("Authorization", "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, mcpGet.status)

        val mcpPost = client.post("/api/v1/admin/mcp-tools") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(AdminMcpToolCreateRequest(name = "run_tax_calculation", description = "Calculate PPh 21 and PPN tax", riskLevel = "HIGH")))
        }
        assertEquals(HttpStatusCode.Created, mcpPost.status)
        assertTrue(mcpPost.bodyAsText().contains("run_tax_calculation"))

        // 4. Third-Party App Registry (Fase 93.C)
        val appGet = client.get("/api/v1/admin/app-registry") {
            header("Authorization", "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, appGet.status)
        assertTrue(appGet.bodyAsText().contains("SAP ERP Enterprise Connector"))

        val appPost = client.post("/api/v1/admin/app-registry") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(AdminAppRegistryCreateRequest(appName = "Accurate Online POS", appType = "ERP", clientId = "client_accurate_102")))
        }
        assertEquals(HttpStatusCode.Created, appPost.status)
        assertTrue(appPost.bodyAsText().contains("Accurate Online POS"))

        // 5. Master Data (Fase 91)
        val mdGet = client.get("/api/v1/admin/master-data") {
            header("Authorization", "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, mdGet.status)
        assertTrue(mdGet.bodyAsText().contains("ENERGY_MINING"))

        val mdPost = client.post("/api/v1/admin/master-data") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(AdminMasterDataCreateRequest(category = "INDUSTRY_TEMPLATES", key = "HEALTHCARE_HOSPITAL", value = "Hospital & Clinic Workforce Preset")))
        }
        assertEquals(HttpStatusCode.Created, mdPost.status)
        assertTrue(mdPost.bodyAsText().contains("HEALTHCARE_HOSPITAL"))

        // 6. Skill Plugins (Fase 92.C)
        val skillGet = client.get("/api/v1/admin/skill-plugins") {
            header("Authorization", "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, skillGet.status)
        assertTrue(skillGet.bodyAsText().contains("Enterprise Sentiment Analysis"))

        val skillPost = client.post("/api/v1/admin/skill-plugins") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(AdminSkillPluginCreateRequest(name = "Auto Document OCR", version = "1.0.0", author = "Vision AI Labs")))
        }
        assertEquals(HttpStatusCode.Created, skillPost.status)
        assertTrue(skillPost.bodyAsText().contains("Auto Document OCR"))

        // 7. Security & Audit Center (PRD Master)
        val auditGet = client.get("/api/v1/admin/audit-logs") {
            header("Authorization", "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, auditGet.status)
        assertTrue(auditGet.bodyAsText().contains("LLM_PROVIDER_UPDATED"))
    }
}
