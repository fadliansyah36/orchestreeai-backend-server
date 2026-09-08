package ai.orchestree.backend.api

import ai.orchestree.backend.database.repositories.masterdata.*
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.masterDataPublicRoutes(repo: MasterDataRepository = MasterDataRepository()) {
    route("/public") {
        // GET /api/v1/public/department-categories
        get("/department-categories") {
            val items = repo.getDepartmentCategories()
            call.respond(HttpStatusCode.OK, items)
        }

        // POST /api/v1/public/department-categories (Super Admin / Dynamic update)
        post("/department-categories") {
            val record = call.receive<DepartmentCategoryRecord>()
            repo.addDepartmentCategory(record)
            call.respond(HttpStatusCode.Created, mapOf("status" to "CREATED", "category_code" to record.category_code))
        }

        // GET /api/v1/public/industry-catalog
        get("/industry-catalog") {
            val items = repo.getIndustryCatalog()
            call.respond(HttpStatusCode.OK, items)
        }

        // GET /api/v1/public/job-level-catalog
        get("/job-level-catalog") {
            val items = repo.getJobLevelCatalog()
            call.respond(HttpStatusCode.OK, items)
        }

        // GET /api/v1/public/job-sub-title-catalog?level_id=X&department_id=Y
        get("/job-sub-title-catalog") {
            val levelId = call.request.queryParameters["level_id"]
            val deptId = call.request.queryParameters["department_id"]
            val items = repo.getJobSubTitleCatalog(levelId, deptId)
            call.respond(HttpStatusCode.OK, items)
        }
    }
}

fun Route.masterDataTenantRoutes(repo: MasterDataRepository = MasterDataRepository()) {
    route("/tenants/{id}") {
        // GET /api/v1/tenants/{id}/ai-job-titles
        get("/ai-job-titles") {
            val tenantId = call.parameters["id"] ?: "tenant-default"
            val items = repo.getAiJobTitles(tenantId)
            call.respond(HttpStatusCode.OK, items)
        }

        // POST /api/v1/tenants/{id}/ai-job-titles (Super Admin / Dynamic AI taxonomy extension)
        post("/ai-job-titles") {
            val record = call.receive<AiJobTitleRecord>()
            repo.addAiJobTitle(record)
            call.respond(HttpStatusCode.Created, mapOf("status" to "CREATED", "job_code" to record.job_code))
        }

        // GET /api/v1/tenants/{id}/ai-job-titles/{jobTitleId}/structural-roles
        get("/ai-job-titles/{jobTitleId}/structural-roles") {
            val tenantId = call.parameters["id"] ?: "tenant-default"
            val jobTitleId = call.parameters["jobTitleId"] ?: ""
            val items = repo.getAiStructuralRoles(tenantId, jobTitleId)
            call.respond(HttpStatusCode.OK, items)
        }

        // GET /api/v1/tenants/{id}/ai-job-titles/{jobTitleId}/available-skills
        get("/ai-job-titles/{jobTitleId}/available-skills") {
            val tenantId = call.parameters["id"] ?: "tenant-default"
            val jobTitleId = call.parameters["jobTitleId"] ?: ""
            val items = repo.getAiAvailableSkills(tenantId, jobTitleId)
            call.respond(HttpStatusCode.OK, items)
        }
    }
}
