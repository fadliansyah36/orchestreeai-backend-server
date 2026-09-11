package ai.orchestree.backend.database

import ai.orchestree.backend.config.SupabaseConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

class SupabaseClientProvider(
    private val config: SupabaseConfig,
    private val httpClient: HttpClient = HttpClient(CIO)
) {
    private val logger = LoggerFactory.getLogger(SupabaseClientProvider::class.java)

    init {
        logger.info("Initialized SupabaseClientProvider for URL: ${config.url.ifBlank { "Not Configured" }}")
    }

    fun isConfigured(): Boolean = config.url.isNotBlank() && config.serviceRoleKey.isNotBlank()

    /**
     * Query table with strict tenant context injection for RLS compliance.
     */
    suspend fun queryTable(
        tableName: String,
        tenantId: String,
        select: String,
        extraParams: Map<String, String>
    ): Result<String> {
        if (!isConfigured()) {
            return Result.failure(IllegalStateException("Supabase configuration missing (SUPABASE_URL or SUPABASE_SERVICE_ROLE_KEY)"))
        }

        return try {
            val body = withContext(Dispatchers.IO) {
                var endpoint = "${config.url.trimEnd('/')}/rest/v1/$tableName?select=$select&tenant_id=eq.$tenantId"
                for ((key, value) in extraParams) {
                    endpoint += "&$key=$value"
                }

                val response: HttpResponse = httpClient.get(endpoint) {
                    header("apikey", config.serviceRoleKey)
                    header("Authorization", "Bearer ${config.serviceRoleKey}")
                    header("X-Tenant-Id", tenantId)
                    header("Range", "0-99")
                }

                val resText = response.bodyAsText()
                if (response.status.isSuccess()) {
                    resText
                } else {
                    logger.error("Supabase query error on $tableName: HTTP ${response.status.value} - $resText")
                    throw RuntimeException("Supabase error: $resText")
                }
            }
            Result.success(body)
        } catch (e: Exception) {
            logger.error("Exception querying Supabase table $tableName: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun queryTable(tableName: String, tenantId: String): Result<String> =
        queryTable(tableName, tenantId, "*", emptyMap())

    suspend fun queryTable(tableName: String, tenantId: String, select: String): Result<String> =
        queryTable(tableName, tenantId, select, emptyMap())

    suspend fun queryTable(tableName: String, tenantId: String, extraParams: Map<String, String>): Result<String> =
        queryTable(tableName, tenantId, "*", extraParams)

    /**
     * Query table globally using service role key (for server recovery jobs & platform administrative tasks).
     */
    suspend fun queryTableGlobal(
        tableName: String,
        select: String,
        extraParams: Map<String, String>
    ): Result<String> {
        if (!isConfigured()) {
            return Result.failure(IllegalStateException("Supabase configuration missing (SUPABASE_URL or SUPABASE_SERVICE_ROLE_KEY)"))
        }

        return try {
            val body = withContext(Dispatchers.IO) {
                var endpoint = "${config.url.trimEnd('/')}/rest/v1/$tableName?select=$select"
                for ((key, value) in extraParams) {
                    endpoint += "&$key=$value"
                }

                val response: HttpResponse = httpClient.get(endpoint) {
                    header("apikey", config.serviceRoleKey)
                    header("Authorization", "Bearer ${config.serviceRoleKey}")
                    header("Range", "0-99")
                }

                val resText = response.bodyAsText()
                if (response.status.isSuccess()) {
                    resText
                } else {
                    logger.error("Supabase global query error on $tableName: HTTP ${response.status.value} - $resText")
                    throw RuntimeException("Supabase error: $resText")
                }
            }
            Result.success(body)
        } catch (e: Exception) {
            logger.error("Exception in global query on Supabase table $tableName: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun queryTableGlobal(tableName: String): Result<String> =
        queryTableGlobal(tableName, "*", emptyMap())

    suspend fun queryTableGlobal(tableName: String, select: String): Result<String> =
        queryTableGlobal(tableName, select, emptyMap())

    suspend fun queryTableGlobal(tableName: String, extraParams: Map<String, String>): Result<String> =
        queryTableGlobal(tableName, "*", extraParams)

    /**
     * Insert record with tenant isolation and trigger Postgres Realtime publication.
     */
    suspend fun insertRecord(
        tableName: String,
        tenantId: String,
        jsonPayload: String
    ): Result<String> {
        if (!isConfigured()) {
            return Result.failure(IllegalStateException("Supabase configuration missing"))
        }

        return try {
            val body = withContext(Dispatchers.IO) {
                val endpoint = "${config.url.trimEnd('/')}/rest/v1/$tableName"
                val response: HttpResponse = httpClient.post(endpoint) {
                    contentType(ContentType.Application.Json)
                    header("apikey", config.serviceRoleKey)
                    header("Authorization", "Bearer ${config.serviceRoleKey}")
                    header("X-Tenant-Id", tenantId)
                    header("Prefer", "return=representation")
                    setBody(jsonPayload)
                }

                val resText = response.bodyAsText()
                if (response.status.isSuccess()) {
                    logger.info("Successfully inserted record into $tableName for tenant $tenantId (Realtime broadcast enabled via Postgres replication)")
                    resText
                } else {
                    logger.error("Supabase insert error on $tableName: HTTP ${response.status.value} - $resText")
                    throw RuntimeException("Supabase insert error: $resText")
                }
            }
            Result.success(body)
        } catch (e: Exception) {
            logger.error("Exception inserting to Supabase table $tableName: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Update record with tenant isolation and trigger Postgres Realtime publication.
     */
    suspend fun updateRecord(
        tableName: String,
        tenantId: String,
        filter: String,
        jsonPayload: String
    ): Result<String> {
        if (!isConfigured()) {
            return Result.failure(IllegalStateException("Supabase configuration missing"))
        }

        return try {
            val body = withContext(Dispatchers.IO) {
                val endpoint = "${config.url.trimEnd('/')}/rest/v1/$tableName?tenant_id=eq.$tenantId&$filter"
                val response: HttpResponse = httpClient.patch(endpoint) {
                    contentType(ContentType.Application.Json)
                    header("apikey", config.serviceRoleKey)
                    header("Authorization", "Bearer ${config.serviceRoleKey}")
                    header("X-Tenant-Id", tenantId)
                    header("Prefer", "return=representation")
                    setBody(jsonPayload)
                }

                val resText = response.bodyAsText()
                if (response.status.isSuccess()) {
                    logger.info("Successfully updated record in $tableName for tenant $tenantId (Realtime broadcast enabled)")
                    resText
                } else {
                    logger.error("Supabase update error on $tableName: HTTP ${response.status.value} - $resText")
                    throw RuntimeException("Supabase update error: $resText")
                }
            }
            Result.success(body)
        } catch (e: Exception) {
            logger.error("Exception updating Supabase table $tableName: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Delete record with tenant isolation.
     */
    suspend fun deleteRecord(
        tableName: String,
        tenantId: String,
        filter: String
    ): Result<String> {
        if (!isConfigured()) {
            return Result.failure(IllegalStateException("Supabase configuration missing"))
        }

        return try {
            val body = withContext(Dispatchers.IO) {
                val endpoint = "${config.url.trimEnd('/')}/rest/v1/$tableName?tenant_id=eq.$tenantId&$filter"
                val response: HttpResponse = httpClient.delete(endpoint) {
                    header("apikey", config.serviceRoleKey)
                    header("Authorization", "Bearer ${config.serviceRoleKey}")
                    header("X-Tenant-Id", tenantId)
                }

                val resText = response.bodyAsText()
                if (response.status.isSuccess()) {
                    logger.info("Successfully deleted record from $tableName for tenant $tenantId")
                    resText
                } else {
                    logger.error("Supabase delete error on $tableName: HTTP ${response.status.value} - $resText")
                    throw RuntimeException("Supabase delete error: $resText")
                }
            }
            Result.success(body)
        } catch (e: Exception) {
            logger.error("Exception deleting from Supabase table $tableName: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Execute RPC (Stored Procedure), e.g. for pgvector kNN search or custom business logic.
     */
    suspend fun callRpc(
        procedureName: String,
        paramsJson: String,
        tenantId: String?
    ): Result<String> {
        if (!isConfigured()) {
            return Result.failure(IllegalStateException("Supabase configuration missing"))
        }

        return try {
            val body = withContext(Dispatchers.IO) {
                val endpoint = "${config.url.trimEnd('/')}/rest/v1/rpc/$procedureName"
                val response: HttpResponse = httpClient.post(endpoint) {
                    contentType(ContentType.Application.Json)
                    header("apikey", config.serviceRoleKey)
                    header("Authorization", "Bearer ${config.serviceRoleKey}")
                    if (!tenantId.isNullOrBlank()) {
                        header("X-Tenant-Id", tenantId)
                    }
                    setBody(paramsJson)
                }

                val resText = response.bodyAsText()
                if (response.status.isSuccess()) {
                    resText
                } else {
                    logger.error("Supabase RPC error on $procedureName: HTTP ${response.status.value} - $resText")
                    throw RuntimeException("Supabase RPC error: $resText")
                }
            }
            Result.success(body)
        } catch (e: Exception) {
            logger.error("Exception calling Supabase RPC $procedureName: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun callRpc(procedureName: String, paramsJson: String): Result<String> =
        callRpc(procedureName, paramsJson, null)

    /**
     * Upload raw document to Supabase Storage bucket for Company Brain processing.
     */
    suspend fun uploadStorageObject(
        bucketName: String,
        filePath: String,
        fileBytes: ByteArray,
        contentType: String
    ): Result<String> {
        if (!isConfigured()) {
            return Result.failure(IllegalStateException("Supabase configuration missing"))
        }

        return try {
            val body = withContext(Dispatchers.IO) {
                val endpoint = "${config.url.trimEnd('/')}/storage/v1/object/$bucketName/$filePath"
                val response: HttpResponse = httpClient.post(endpoint) {
                    header("apikey", config.serviceRoleKey)
                    header("Authorization", "Bearer ${config.serviceRoleKey}")
                    header("Content-Type", contentType)
                    header("x-upsert", "true")
                    setBody(fileBytes)
                }

                val resText = response.bodyAsText()
                if (response.status.isSuccess()) {
                    resText
                } else {
                    throw RuntimeException("Supabase Storage error: $resText")
                }
            }
            Result.success(body)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadStorageObject(
        bucketName: String,
        filePath: String,
        fileBytes: ByteArray
    ): Result<String> = uploadStorageObject(bucketName, filePath, fileBytes, "application/octet-stream")

    /**
     * Download raw document from Supabase Storage bucket.
     */
    suspend fun downloadStorageObject(
        bucketName: String,
        filePath: String
    ): Result<ByteArray> {
        if (!isConfigured()) {
            return Result.failure(IllegalStateException("Supabase configuration missing"))
        }

        return try {
            val bytes = withContext(Dispatchers.IO) {
                val endpoint = "${config.url.trimEnd('/')}/storage/v1/object/$bucketName/$filePath"
                val response: HttpResponse = httpClient.get(endpoint) {
                    header("apikey", config.serviceRoleKey)
                    header("Authorization", "Bearer ${config.serviceRoleKey}")
                }

                if (response.status.isSuccess()) {
                    val responseBytes: ByteArray = response.body()
                    responseBytes
                } else {
                    val resText = response.bodyAsText()
                    throw RuntimeException("Supabase Storage download error: $resText")
                }
            }
            Result.success(bytes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get public URL for a stored object in Supabase Storage.
     */
    fun getStoragePublicUrl(bucketName: String, filePath: String): String {
        return "${config.url.trimEnd('/')}/storage/v1/object/public/$bucketName/$filePath"
    }

    companion object {
        fun fromEnv(): SupabaseClientProvider = SupabaseClientProvider(SupabaseConfig.fromEnv())
    }
}
