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
    fun testAuthLoginAndRefreshEndpoints() = testApplication {
        application {
            module()
        }
        val loginResponse = client.post("/api/v1/auth/login") {
            header(io.ktor.http.HttpHeaders.ContentType, io.ktor.http.ContentType.Application.Json.toString())
            setBody("""{"email":"admin@nusantara.co.id","password":"Admin123!"}""")
        }
        assertEquals(HttpStatusCode.OK, loginResponse.status)
        val body = loginResponse.bodyAsText()
        assertTrue(body.contains("accessToken"))
        assertTrue(body.contains("refreshToken"))

        val refreshResponse = client.post("/api/v1/auth/refresh") {
            header(io.ktor.http.HttpHeaders.ContentType, io.ktor.http.ContentType.Application.Json.toString())
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
        val sessRes = client.get("/api/v1/auth/sessions")
        assertEquals(HttpStatusCode.OK, sessRes.status)
        assertTrue(sessRes.bodyAsText().contains("sess-001"))

        val profRes = client.get("/api/v1/auth/profile")
        assertEquals(HttpStatusCode.OK, profRes.status)
        assertTrue(profRes.bodyAsText().contains("admin@nusantara.co.id"))
    }

}
