package ai.orchestree.backend.generativestudio

import ai.orchestree.backend.database.SupabaseClientProvider
import ai.orchestree.backend.database.repositories.generativestudio.BrandAssetOverlayRepository
import kotlinx.serialization.Serializable
import java.security.MessageDigest
import java.util.UUID

@Serializable
data class BrandAssetResult(
    val publicUrl: String,
    val storagePath: String = "",
    val assetType: String = "logo",
    val isDeterministic: Boolean = true,
    val sha256Checksum: String = ""
)

class BrandAssetService(
    private val supabase: SupabaseClientProvider = SupabaseClientProvider.fromEnv(),
    val brandAssetOverlayRepo: BrandAssetOverlayRepository = BrandAssetOverlayRepository(supabase)
) {
    companion object {
        val defaultInstance = BrandAssetService()
    }

    /**
     * LANGKAH 2.1: Endpoint YANG BENAR (Fase 62 Bagian A)
     * FUNGSI INI 100% DETERMINISTIK: file yang di-upload user PERSIS SAMA dengan file yang tersimpan
     * dan ditampilkan kembali, TIDAK ADA transformasi/regenerasi AI dalam bentuk apapun.
     * ZERO pemanggilan LLM/Image Provider di fungsi ini SAMA SEKALI.
     */
    suspend fun uploadBrandLogo(tenantId: String, fileBytes: ByteArray, fileName: String): BrandAssetResult {
        // Compute SHA-256 to guarantee deterministic byte-by-byte integrity
        val digest = MessageDigest.getInstance("SHA-256").digest(fileBytes)
        val sha256 = digest.joinToString("") { "%02x".format(it) }

        val safeName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val storagePath = "brand-assets/$tenantId/logo/${UUID.randomUUID()}_$safeName"
        val contentType = when {
            fileName.endsWith(".svg", ignoreCase = true) -> "image/svg+xml"
            fileName.endsWith(".jpg", ignoreCase = true) || fileName.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
            fileName.endsWith(".webp", ignoreCase = true) -> "image/webp"
            else -> "image/png"
        }

        // Upload mentah, apa adanya ke Object Storage
        supabase.uploadStorageObject("brand-assets", storagePath, fileBytes, contentType)
        val publicUrl = supabase.getStoragePublicUrl("brand-assets", storagePath)

        // Upsert deterministik ke brand_asset_overlays
        brandAssetOverlayRepo.upsert(
            tenantId = tenantId,
            assetType = "logo",
            assetFileRef = publicUrl,
            assetName = fileName
        )

        return BrandAssetResult(
            publicUrl = publicUrl,
            storagePath = storagePath,
            assetType = "logo",
            isDeterministic = true,
            sha256Checksum = sha256
        )
    }

    /**
     * LANGKAH 5.1: Super Admin upload official platform-wide icon logo
     * Upload SEKALI, tersimpan sebagai konfigurasi PLATFORM-WIDE (bukan per-tenant).
     */
    suspend fun uploadPlatformIconLogo(fileBytes: ByteArray, fileName: String): BrandAssetResult {
        val digest = MessageDigest.getInstance("SHA-256").digest(fileBytes)
        val sha256 = digest.joinToString("") { "%02x".format(it) }

        val safeName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val storagePath = "platform-assets/icon-logo/official_icon_$safeName"
        val contentType = if (fileName.endsWith(".svg", ignoreCase = true)) "image/svg+xml" else "image/png"

        supabase.uploadStorageObject("platform-assets", storagePath, fileBytes, contentType)
        val publicUrl = supabase.getStoragePublicUrl("platform-assets", storagePath)

        brandAssetOverlayRepo.upsert(
            tenantId = "platform-global",
            assetType = "PLATFORM_ICON_LOGO",
            assetFileRef = publicUrl,
            assetName = "Official Platform Icon Logo"
        )
        brandAssetOverlayRepo.setPlatformIconLogoUrl(publicUrl)

        return BrandAssetResult(
            publicUrl = publicUrl,
            storagePath = storagePath,
            assetType = "PLATFORM_ICON_LOGO",
            isDeterministic = true,
            sha256Checksum = sha256
        )
    }

    fun getPlatformIconLogoUrl(): String? {
        return brandAssetOverlayRepo.getPlatformIconLogoUrl()
    }
}
