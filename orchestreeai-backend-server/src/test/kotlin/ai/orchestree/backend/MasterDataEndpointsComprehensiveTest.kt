package ai.orchestree.backend

import ai.orchestree.backend.config.AppConfig
import ai.orchestree.backend.plugins.configureAuthentication
import ai.orchestree.backend.plugins.configureHTTPS
import ai.orchestree.backend.plugins.configureRouting
import ai.orchestree.backend.plugins.configureSerialization
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MasterDataEndpointsComprehensiveTest {

    @Test
    fun test01_verifyPublicDepartmentCategories() = testApplication {
        application {
            configureSerialization()
            configureHTTPS()
            configureAuthentication(AppConfig.load())
            configureRouting()
        }
        val client = createClient {}

        val response = client.get("/api/v1/public/department-categories")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        println("=== test01_verifyPublicDepartmentCategories raw output ===")
        println(body.take(300) + "...")
        assertTrue(body.contains("Executive & Leadership"))
        assertTrue(body.contains("Sales & Business Development"))
        assertTrue(body.contains("IT & Engineering"))
    }

    @Test
    fun test02_verifyPublicIndustryCatalog() = testApplication {
        application {
            configureSerialization()
            configureHTTPS()
            configureAuthentication(AppConfig.load())
            configureRouting()
        }
        val client = createClient {}

        val response = client.get("/api/v1/public/industry-catalog")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        println("=== test02_verifyPublicIndustryCatalog raw output ===")
        println(body.take(300) + "...")
        assertTrue(body.contains("Food & Beverage"))
        assertTrue(body.contains("Startup Teknologi & SaaS"))
        assertTrue(body.contains("Kesehatan & Rumah Sakit"))
    }

    @Test
    fun test03_verifyPublicJobLevelCatalog() = testApplication {
        application {
            configureSerialization()
            configureHTTPS()
            configureAuthentication(AppConfig.load())
            configureRouting()
        }
        val client = createClient {}

        val response = client.get("/api/v1/public/job-level-catalog")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        println("=== test03_verifyPublicJobLevelCatalog raw output ===")
        println(body.take(300) + "...")
        assertTrue(body.contains("Owner / Pendiri"))
        assertTrue(body.contains("Direksi / C-Level"))
        assertTrue(body.contains("hierarchy_order"))
    }

    @Test
    fun test04_verifyPublicJobSubTitleCatalogFiltered() = testApplication {
        application {
            configureSerialization()
            configureHTTPS()
            configureAuthentication(AppConfig.load())
            configureRouting()
        }
        val client = createClient {}

        val allResponse = client.get("/api/v1/public/job-sub-title-catalog")
        assertEquals(HttpStatusCode.OK, allResponse.status)
        val allBody = allResponse.bodyAsText()
        assertTrue(allBody.contains("Chief Executive Officer (CEO)"))

        val filteredResponse = client.get("/api/v1/public/job-sub-title-catalog?level_id=lvl-direksi&department_id=dept-exec")
        assertEquals(HttpStatusCode.OK, filteredResponse.status)
        val filteredBody = filteredResponse.bodyAsText()
        println("=== test04_verifyPublicJobSubTitleCatalogFiltered raw output ===")
        println(filteredBody)
        assertTrue(filteredBody.contains("Chief Executive Officer (CEO)"))
        assertTrue(filteredBody.contains("Chief Operating Officer (COO)"))
    }

    @Test
    fun test05_verifyTenantAiJobTitles() = testApplication {
        application {
            configureSerialization()
            configureHTTPS()
            configureAuthentication(AppConfig.load())
            configureRouting()
        }
        val client = createClient {}

        val response = client.get("/api/v1/tenants/tenant-default/ai-job-titles")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        println("=== test05_verifyTenantAiJobTitles raw output ===")
        println(body.take(300) + "...")
        assertTrue(body.contains("AI Chief of Staff"))
        assertTrue(body.contains("AI Sales"))
        assertTrue(body.contains("AI Operations"))
    }

    @Test
    fun test06_verifyTenantAiStructuralRolesFiltered() = testApplication {
        application {
            configureSerialization()
            configureHTTPS()
            configureAuthentication(AppConfig.load())
            configureRouting()
        }
        val client = createClient {}

        val response = client.get("/api/v1/tenants/tenant-default/ai-job-titles/jt-sales/structural-roles")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        println("=== test06_verifyTenantAiStructuralRolesFiltered raw output ===")
        println(body)
        assertTrue(body.contains("AI SDR"))
        assertTrue(body.contains("AI Closer"))
    }

    @Test
    fun test07_verifyTenantAiAvailableSkills() = testApplication {
        application {
            configureSerialization()
            configureHTTPS()
            configureAuthentication(AppConfig.load())
            configureRouting()
        }
        val client = createClient {}

        val response = client.get("/api/v1/tenants/tenant-default/ai-job-titles/jt-sales/available-skills")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        println("=== test07_verifyTenantAiAvailableSkills raw output ===")
        println(body)
        assertTrue(body.contains("lead_qualification"))
        assertTrue(body.contains("High-Conversion Objection Handling"))
    }

    @Test
    fun test08_verifyDynamicDepartmentCategoryAddition() = testApplication {
        application {
            configureSerialization()
            configureHTTPS()
            configureAuthentication(AppConfig.load())
            configureRouting()
        }
        val client = createClient {}

        val newCategoryPayload = """
            {
                "id": "dept-quantum",
                "category_code": "quantum_computing",
                "category_name": "Quantum & Deep Tech",
                "description": "Departemen R&D Komputasi Kuantum",
                "icon_key": "cpu",
                "is_active": true
            }
        """.trimIndent()

        val postResponse = client.post("/api/v1/public/department-categories") {
            contentType(ContentType.Application.Json)
            setBody(newCategoryPayload)
        }
        assertEquals(HttpStatusCode.Created, postResponse.status)

        // Verify it immediately appears in the GET endpoint
        val getResponse = client.get("/api/v1/public/department-categories")
        assertEquals(HttpStatusCode.OK, getResponse.status)
        val getBody = getResponse.bodyAsText()
        println("=== test08_verifyDynamicDepartmentCategoryAddition verified in GET ===")
        assertTrue(getBody.contains("Quantum & Deep Tech"))
    }

    @Test
    fun test09_verifyDynamicAiJobTitleAddition() = testApplication {
        application {
            configureSerialization()
            configureHTTPS()
            configureAuthentication(AppConfig.load())
            configureRouting()
        }
        val client = createClient {}

        val newAiJobPayload = """
            {
                "id": "jt-quantum-officer",
                "job_code": "quantum_officer",
                "job_name": "AI Quantum Strategist",
                "icon_key": "cpu",
                "star_rating": 5,
                "description": "Menganalisis strategi komputasi kuantum tenant",
                "maps_to_persona_type": "QUANTUM_STRATEGIST",
                "is_top_coordinator": false,
                "relevant_department_category_id": "dept-rnd"
            }
        """.trimIndent()

        val postResponse = client.post("/api/v1/tenants/tenant-default/ai-job-titles") {
            contentType(ContentType.Application.Json)
            setBody(newAiJobPayload)
        }
        assertEquals(HttpStatusCode.Created, postResponse.status)

        // Verify it immediately appears in the GET endpoint
        val getResponse = client.get("/api/v1/tenants/tenant-default/ai-job-titles")
        assertEquals(HttpStatusCode.OK, getResponse.status)
        val getBody = getResponse.bodyAsText()
        println("=== test09_verifyDynamicAiJobTitleAddition verified in GET ===")
        assertTrue(getBody.contains("AI Quantum Strategist"))
    }
}
