package ai.orchestree.backend

import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApplicationTest {

    @Test
    fun testRootEndpoint() = testApplication {
        application {
            module()
        }

        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("OrchestreeAI Enterprise Autonomous AI Workforce Server"))
    }

    @Test
    fun testHealthEndpoint() = testApplication {
        application {
            module()
        }

        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("healthy"))
    }

    @Test
    fun testAuthLoginWithoutTenantIdReturns400() = testApplication {
        application {
            module()
        }
        // Direct route: POST /auth/login without tenantId should return 400 Bad Request
        val response = client.post("/auth/login") {
            header(io.ktor.http.HttpHeaders.ContentType, io.ktor.http.ContentType.Application.Json.toString())
            setBody("""{"email":"admin@nusantara.co.id","password":"Admin123!"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("Tenant ID is required"))

        // Prefix route: POST /api/v1/auth/login without tenantId should also return 400 Bad Request
        val apiV1Response = client.post("/api/v1/auth/login") {
            header(io.ktor.http.HttpHeaders.ContentType, io.ktor.http.ContentType.Application.Json.toString())
            setBody("""{"email":"admin@nusantara.co.id","password":"Admin123!"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, apiV1Response.status)
    }

    @Test
    fun testAuthLoginWithTenantIdReturns200() = testApplication {
        application {
            module()
        }
        // Direct route: POST /auth/login with tenantId in body
        val directResponse = client.post("/auth/login") {
            header(io.ktor.http.HttpHeaders.ContentType, io.ktor.http.ContentType.Application.Json.toString())
            setBody("""{"email":"admin@nusantara.co.id","password":"Admin123!","tenantId":"tenant-default-001"}""")
        }
        assertEquals(HttpStatusCode.OK, directResponse.status)
        val directBody = directResponse.bodyAsText()
        assertTrue(directBody.contains("accessToken"))
        assertTrue(directBody.contains("tenantId"))

        // Prefix route: POST /api/v1/auth/login with X-Tenant-ID header
        val headerResponse = client.post("/api/v1/auth/login") {
            header(io.ktor.http.HttpHeaders.ContentType, io.ktor.http.ContentType.Application.Json.toString())
            header("X-Tenant-ID", "tenant-header-002")
            setBody("""{"email":"admin@nusantara.co.id","password":"Admin123!"}""")
        }
        assertEquals(HttpStatusCode.OK, headerResponse.status)
        val headerBody = headerResponse.bodyAsText()
        assertTrue(headerBody.contains("accessToken"))
    }

    @Test
    fun testAuthLoginAndRefreshEndpoints() = testApplication {
        application {
            module()
        }
        val loginResponse = client.post("/api/v1/auth/login") {
            header(io.ktor.http.HttpHeaders.ContentType, io.ktor.http.ContentType.Application.Json.toString())
            setBody("""{"email":"admin@nusantara.co.id","password":"Admin123!","tenantId":"tenant-default-001"}""")
        }
        assertEquals(HttpStatusCode.OK, loginResponse.status)
        val body = loginResponse.bodyAsText()
        assertTrue(body.contains("accessToken"))
        assertTrue(body.contains("refreshToken"))

        val refreshResponse = client.post("/api/v1/auth/refresh") {
            header(io.ktor.http.HttpHeaders.ContentType, io.ktor.http.ContentType.Application.Json.toString())
            header("X-Tenant-ID", "tenant-default-001")
            setBody("""{"refreshToken":"rt-test-12345"}""")
        }
        assertEquals(HttpStatusCode.OK, refreshResponse.status)
        val refreshBody = refreshResponse.bodyAsText()
        assertTrue(refreshBody.contains("accessToken"))
    }


    @Test
    fun testSessionsAndProfileEndpoints() = testApplication {
        application {
            module()
        }
        val loginRes = client.post("/api/v1/auth/login") {
            header(io.ktor.http.HttpHeaders.ContentType, io.ktor.http.ContentType.Application.Json.toString())
            setBody("""{"email":"admin@nusantara.co.id","password":"Admin123!","tenantId":"tenant-default-001"}""")
        }
        assertEquals(HttpStatusCode.OK, loginRes.status)
        val loginJson = kotlinx.serialization.json.Json.parseToJsonElement(loginRes.bodyAsText())
        val token = loginJson.let { (it as kotlinx.serialization.json.JsonObject)["accessToken"]?.toString()?.replace("\"", "") ?: "" }

        val sessRes = client.get("/api/v1/auth/sessions") {
            header("Authorization", "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, sessRes.status)
        assertTrue(sessRes.bodyAsText().contains("sess-"))

        val profRes = client.get("/api/v1/auth/profile") {
            header("Authorization", "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, profRes.status)
        assertTrue(profRes.bodyAsText().contains("admin@nusantara.co.id"))
    }

}
