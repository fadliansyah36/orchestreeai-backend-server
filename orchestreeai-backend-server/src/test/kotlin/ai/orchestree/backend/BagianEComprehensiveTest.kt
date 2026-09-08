package ai.orchestree.backend

import ai.orchestree.backend.api.ChatApiRequest
import ai.orchestree.backend.api.ChatApiResponse
import ai.orchestree.backend.api.LoginApiRequest
import ai.orchestree.backend.api.LoginApiResponse
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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BagianEComprehensiveTest {

    private val jwtSecret = SecurityConfig.fromEnv().jwtSecretKey.ifBlank { "101ffa9b-10c9-4e15-9390-90c2d32ed6c8" }
    private val algorithm = Algorithm.HMAC256(jwtSecret)
    private val json = Json { ignoreUnknownKeys = true }

    private fun generateValidToken(
        userId: String = "usr-test-101",
        tenantId: String = "tenant-enterprise-001",
        role: String = "TENANT_ADMIN"
    ): String {
        return JWT.create()
            .withSubject(userId)
            .withClaim("sub", userId)
            .withClaim("tenant_id", tenantId)
            .withClaim("role", role)
            .withIssuedAt(Date())
            .withExpiresAt(Date(System.currentTimeMillis() + 3600 * 1000))
            .sign(algorithm)
    }

    @Test
    fun testPublicEndpointsAccessibleWithoutAuth() = testApplication {
        application {
            configureSerialization()
            configureHTTPS()
            configureAuthentication(AppConfig.load())
            configureRouting()
        }

        val client = createClient {}

        // Root
        val rootRes = client.get("/")
        assertEquals(HttpStatusCode.OK, rootRes.status)

        // Health check
        val healthRes = client.get("/health")
        assertEquals(HttpStatusCode.OK, healthRes.status)
        assertTrue(healthRes.bodyAsText().contains("healthy"))

        // Public payment webhook
        val paymentWebhookRes = client.post("/api/v1/payments/webhook/midtrans") {
            contentType(ContentType.Application.Json)
            setBody("""{"order_id": "ORD-1", "transaction_status": "settlement"}""")
        }
        assertEquals(HttpStatusCode.OK, paymentWebhookRes.status)

        // Public courier webhook
        val courierWebhookRes = client.post("/api/v1/shipments/webhook/jne") {
            contentType(ContentType.Application.Json)
            setBody("""{"tracking_number": "JNE123", "status": "DELIVERED"}""")
        }
        assertEquals(HttpStatusCode.OK, courierWebhookRes.status)
    }

    @Test
    fun testUnauthenticatedRequestToProtectedEndpointsReturns401() = testApplication {
        application {
            configureSerialization()
            configureHTTPS()
            configureAuthentication(AppConfig.load())
            configureRouting()
        }

        val client = createClient {}

        // 1. Overview dashboard
        val overviewRes = client.get("/api/v1/tenants/tenant-enterprise-001/dashboard/overview")
        assertEquals(HttpStatusCode.Unauthorized, overviewRes.status)

        // 2. Channel accounts
        val channelsRes = client.get("/api/v1/tenants/tenant-enterprise-001/channel-accounts")
        assertEquals(HttpStatusCode.Unauthorized, channelsRes.status)

        // 3. Enterprise connections
        val enterpriseRes = client.get("/api/v1/tenants/tenant-enterprise-001/enterprise-connections")
        assertEquals(HttpStatusCode.Unauthorized, enterpriseRes.status)

        // 4. Super Admin tenants
        val adminRes = client.get("/api/v1/admin/tenants")
        assertEquals(HttpStatusCode.Unauthorized, adminRes.status)

        // 5. Chat endpoint
        val chatRes = client.post("/api/v1/chat") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(ChatApiRequest(message = "Halo")))
        }
        assertEquals(HttpStatusCode.Unauthorized, chatRes.status)
    }

    @Test
    fun testAuthenticatedRequestWithValidJwtSucceeds() = testApplication {
        application {
            configureSerialization()
            configureHTTPS()
            configureAuthentication(AppConfig.load())
            configureRouting()
        }

        val client = createClient {}

        val validToken = generateValidToken(userId = "usr-director-1", tenantId = "tenant-enterprise-001")

        // 1. Login to get token
        val loginRes = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(LoginApiRequest(email = "admin@nusantara.energy", tenantId = "tenant-enterprise-001")))
        }
        assertEquals(HttpStatusCode.OK, loginRes.status)
        val loginData = json.decodeFromString<LoginApiResponse>(loginRes.bodyAsText())
        assertNotNull(loginData.accessToken)

        // 2. Access Overview with Token
        val overviewRes = client.get("/api/v1/tenants/tenant-enterprise-001/dashboard/overview") {
            header("Authorization", "Bearer ${loginData.accessToken}")
        }
        assertEquals(HttpStatusCode.OK, overviewRes.status)
        assertTrue(overviewRes.bodyAsText().contains("tenant-enterprise-001"))

        // 3. Access Enterprise Activity Stream (PRD Addendum 2 Bagian 78.1)
        val activityRes = client.get("/api/v1/tenants/tenant-enterprise-001/activity-stream") {
            header("Authorization", "Bearer $validToken")
        }
        assertEquals(HttpStatusCode.OK, activityRes.status)
        assertTrue(activityRes.bodyAsText().contains("SAP_ERP"))

        // 4. Access Credit Wallet (PRD Addendum 1 Bagian 51)
        val walletRes = client.get("/api/v1/tenants/tenant-enterprise-001/credit-wallet") {
            header("Authorization", "Bearer $validToken")
        }
        assertEquals(HttpStatusCode.OK, walletRes.status)
        assertTrue(walletRes.bodyAsText().contains("1500000"))

        // 5. Access Super Admin Tenants
        val adminRes = client.get("/api/v1/admin/tenants") {
            header("Authorization", "Bearer $validToken")
        }
        assertEquals(HttpStatusCode.OK, adminRes.status)
        assertTrue(adminRes.bodyAsText().contains("PT Nusantara Energy"))

        // 6. Access Chat messages
        val chatRes = client.post("/api/v1/chat") {
            header("Authorization", "Bearer $validToken")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(ChatApiRequest(message = "Tolong periksa status pipeline Q3")))
        }
        assertEquals(HttpStatusCode.OK, chatRes.status)
        val chatData = json.decodeFromString<ChatApiResponse>(chatRes.bodyAsText())
        assertNotNull(chatData.reply)
    }
}
