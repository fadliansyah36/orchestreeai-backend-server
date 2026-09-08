package ai.orchestree.backend.database.repositories.generativestudio

import ai.orchestree.backend.database.SupabaseClientProvider
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class BrandAssetOverlayRecord(
    val id: String,
    val tenantId: String,
    val assetName: String,
    val assetType: String,
    val assetFileUrl: String,
    val defaultPosition: String = "TOP_RIGHT",
    val customPosXPercent: Float = 0.85f,
    val customPosYPercent: Float = 0.05f,
    val targetScalePercent: Float = 0.15f,
    val opacity: Float = 1.0f,
    val blendMode: String = "NORMAL",
    val minMarginPx: Int = 32,
    val isDefaultActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

class BrandAssetOverlayRepository(
    private val supabase: SupabaseClientProvider = SupabaseClientProvider.fromEnv()
) {
    // In-memory deterministic registry for fast lookups & testability
    private val localOverlayStore = ConcurrentHashMap<String, MutableList<BrandAssetOverlayRecord>>()
    @Volatile
    private var platformIconLogoUrl: String? = null

    suspend fun upsert(
        tenantId: String,
        assetType: String,
        assetFileRef: String,
        assetName: String = "Official Brand $assetType",
        defaultPosition: String = "TOP_RIGHT",
        targetScalePercent: Float = 0.15f,
        opacity: Float = 1.0f
    ): BrandAssetOverlayRecord {
        val recordId = "overlay-${java.util.UUID.randomUUID().toString().take(8)}"
        val now = System.currentTimeMillis()
        val record = BrandAssetOverlayRecord(
            id = recordId,
            tenantId = tenantId,
            assetName = assetName,
            assetType = assetType,
            assetFileUrl = assetFileRef,
            defaultPosition = defaultPosition,
            targetScalePercent = targetScalePercent,
            opacity = opacity,
            createdAt = now,
            updatedAt = now
        )

        // Store locally
        val list = localOverlayStore.computeIfAbsent(tenantId) { mutableListOf() }
        list.removeIf { it.assetType.equals(assetType, ignoreCase = true) }
        list.add(record)

        if (tenantId == "platform-global" && assetType.equals("PLATFORM_ICON_LOGO", ignoreCase = true)) {
            platformIconLogoUrl = assetFileRef
        }

        // Persist to Supabase if configured
        if (supabase.isConfigured()) {
            val payload = buildJsonObject {
                put("id", record.id)
                put("tenant_id", record.tenantId)
                put("asset_name", record.assetName)
                put("asset_type", record.assetType)
                put("asset_file_url", record.assetFileUrl)
                put("default_position", record.defaultPosition)
                put("custom_pos_x_percent", record.customPosXPercent)
                put("custom_pos_y_percent", record.customPosYPercent)
                put("target_scale_percent", record.targetScalePercent)
                put("opacity", record.opacity)
                put("blend_mode", record.blendMode)
                put("min_margin_px", record.minMarginPx)
                put("is_default_active", record.isDefaultActive)
                put("created_at", record.createdAt)
                put("updated_at", record.updatedAt)
            }.toString()

            supabase.insertRecord("brand_asset_overlays", tenantId, payload)
        }

        return record
    }

    suspend fun getOverlays(tenantId: String): List<BrandAssetOverlayRecord> {
        return localOverlayStore[tenantId]?.toList() ?: emptyList()
    }

    fun getPlatformIconLogoUrl(): String? {
        return platformIconLogoUrl
    }

    fun setPlatformIconLogoUrl(url: String) {
        platformIconLogoUrl = url
    }
}
