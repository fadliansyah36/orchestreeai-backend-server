package ai.orchestree.backend.database.repositories.selection

import ai.orchestree.backend.database.SupabaseClientProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class AutoSelectionFolderConfig(
    val id: String = UUID.randomUUID().toString(),
    val tenantId: String,
    val folderPath: String, // e.g. "recruitment/", "tenders/", "candidates/", "auto-selection/"
    val domainCategory: String? = null,
    val defaultPrompt: String = "Lakukan evaluasi dan ranking otomatis untuk dokumen yang diunggah",
    val isEnabled: Boolean = true,
    val calibrationSettingsId: String? = null,
    val autoExecuteDownstream: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

class AutoSelectionConfigRepository(
    private val supabase: SupabaseClientProvider = SupabaseClientProvider.fromEnv()
) {
    private val logger = LoggerFactory.getLogger(AutoSelectionConfigRepository::class.java)
    private val configsCache = ConcurrentHashMap<String, MutableMap<String, AutoSelectionFolderConfig>>()

    companion object {
        val defaultInstance: AutoSelectionConfigRepository by lazy { AutoSelectionConfigRepository() }
    }

    suspend fun getConfigsForTenant(tenantId: String): List<AutoSelectionFolderConfig> = withContext(Dispatchers.IO) {
        val cached = configsCache[tenantId]?.values?.toList()
        if (!cached.isNullOrEmpty()) {
            return@withContext cached
        }

        try {
            val res = supabase.queryTable("selection_auto_configs", tenantId)
            if (res.isSuccess) {
                val jsonStr = res.getOrDefault("[]")
                val jsonArray = Json.parseToJsonElement(jsonStr).jsonArray
                val list = jsonArray.map { el ->
                    val obj = el.jsonObject
                    AutoSelectionFolderConfig(
                        id = obj["id"]?.jsonPrimitive?.content ?: UUID.randomUUID().toString(),
                        tenantId = obj["tenant_id"]?.jsonPrimitive?.content ?: tenantId,
                        folderPath = obj["folder_path"]?.jsonPrimitive?.content ?: "",
                        domainCategory = obj["domain_category"]?.jsonPrimitive?.content,
                        defaultPrompt = obj["default_prompt"]?.jsonPrimitive?.content
                            ?: "Lakukan evaluasi dan ranking otomatis untuk dokumen yang diunggah",
                        isEnabled = obj["is_enabled"]?.jsonPrimitive?.booleanOrNull ?: true,
                        calibrationSettingsId = obj["calibration_settings_id"]?.jsonPrimitive?.content,
                        autoExecuteDownstream = obj["auto_execute_downstream"]?.jsonPrimitive?.booleanOrNull ?: false
                    )
                }
                val tenantMap = configsCache.getOrPut(tenantId) { ConcurrentHashMap() }
                list.forEach { tenantMap[it.id] = it }
                return@withContext list
            }
        } catch (e: Exception) {
            logger.warn("Could not query selection_auto_configs from Supabase: ${e.message}")
        }

        configsCache[tenantId]?.values?.toList() ?: emptyList()
    }

    suspend fun getConfigForPath(tenantId: String, rawPath: String): AutoSelectionFolderConfig? = withContext(Dispatchers.IO) {
        val configs = getConfigsForTenant(tenantId)
        val normalized = rawPath.trim().trimStart('/')

        // Cari config yang path foldernya cocok dan enabled
        configs.firstOrNull { cfg ->
            if (!cfg.isEnabled) return@firstOrNull false
            val folder = cfg.folderPath.trim().trimStart('/')
            if (folder.isEmpty() || folder == "*") {
                true
            } else {
                normalized.contains(folder) || normalized.startsWith(folder)
            }
        }
    }

    suspend fun saveConfig(config: AutoSelectionFolderConfig): Result<AutoSelectionFolderConfig> = withContext(Dispatchers.IO) {
        try {
            val tenantMap = configsCache.getOrPut(config.tenantId) { ConcurrentHashMap() }
            tenantMap[config.id] = config

            val payload = buildJsonObject {
                put("id", config.id)
                put("tenant_id", config.tenantId)
                put("folder_path", config.folderPath)
                config.domainCategory?.let { put("domain_category", it) }
                put("default_prompt", config.defaultPrompt)
                put("is_enabled", config.isEnabled)
                config.calibrationSettingsId?.let { put("calibration_settings_id", it) }
                put("auto_execute_downstream", config.autoExecuteDownstream)
            }

            val res = supabase.insertRecord("selection_auto_configs", config.tenantId, payload.toString())
            if (res.isSuccess) {
                Result.success(config)
            } else {
                // Return success using cache if table schema is pending
                logger.info("Saved auto selection config to memory cache: ${config.id}")
                Result.success(config)
            }
        } catch (e: Exception) {
            logger.error("Error saving auto selection config: ${e.message}", e)
            Result.success(config) // Safe memory fallback
        }
    }

    suspend fun deleteConfig(tenantId: String, configId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            configsCache[tenantId]?.remove(configId)
            supabase.deleteRecord("selection_auto_configs", tenantId, "id=eq.$configId")
            true
        } catch (e: Exception) {
            logger.error("Error deleting auto selection config: ${e.message}", e)
            configsCache[tenantId]?.remove(configId) != null
        }
    }

    suspend fun deleteConfig(configId: String): Boolean = withContext(Dispatchers.IO) {
        var found = false
        for ((tId, map) in configsCache) {
            if (map.containsKey(configId)) {
                map.remove(configId)
                supabase.deleteRecord("selection_auto_configs", tId, "id=eq.$configId")
                found = true
            }
        }
        found
    }
}
