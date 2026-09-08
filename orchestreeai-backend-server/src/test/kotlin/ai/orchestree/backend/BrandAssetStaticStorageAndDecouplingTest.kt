package ai.orchestree.backend

import ai.orchestree.backend.generativestudio.BrandAssetService
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BrandAssetStaticStorageAndDecouplingTest {

    @Test
    fun testDeterministicLogoUpload_NoAiPipelineInvolved() = runBlocking {
        val service = BrandAssetService()

        val samplePngBytes = byteArrayOf(
            0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte(),
            0x0D.toByte(), 0x0A.toByte(), 0x1A.toByte(), 0x0A.toByte(),
            0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x0D.toByte(),
            0x49.toByte(), 0x48.toByte(), 0x44.toByte(), 0x52.toByte()
        )

        // 1. Upload valid logo as static asset
        val asset = service.uploadBrandLogo(
            tenantId = "tenant-test-101",
            fileBytes = samplePngBytes,
            fileName = "company_logo.png"
        )

        // 2. Validate properties are static and deterministic
        assertEquals("logo", asset.assetType)
        assertTrue(asset.isDeterministic, "Must be flagged as deterministic non-generative asset")
        assertTrue(asset.storagePath.contains("brand-assets/tenant-test-101/logo/"), "Storage path must follow tenant isolation")
        assertTrue(asset.publicUrl.contains("brand-assets"), "Public URL must point to brand-assets bucket")
        assertFalse(asset.publicUrl.contains("generative"), "Must not call or reference generative AI pipelines")
        assertTrue(asset.sha256Checksum.isNotEmpty(), "Must have SHA-256 checksum calculated")

        println("✓ PASSED: Static logo uploaded without AI generation: ${asset.publicUrl} (SHA256=${asset.sha256Checksum.take(8)}...)")
    }

    @Test
    fun testPlatformIconUpload_SuperAdminBucketIsolation() = runBlocking {
        val service = BrandAssetService()

        val sampleIconBytes = "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 100 100\"><circle cx=\"50\" cy=\"50\" r=\"40\"/></svg>".toByteArray()

        val asset = service.uploadPlatformIconLogo(
            fileBytes = sampleIconBytes,
            fileName = "official_tree.svg"
        )

        assertEquals("PLATFORM_ICON_LOGO", asset.assetType)
        assertTrue(asset.isDeterministic)
        assertTrue(asset.storagePath.startsWith("platform-assets/icon-logo/"), "Must be isolated in platform-assets bucket")
        assertTrue(asset.publicUrl.contains("platform-assets"))
        assertEquals(asset.publicUrl, service.getPlatformIconLogoUrl())

        println("✓ PASSED: Super Admin platform icon isolated in platform-assets: ${asset.publicUrl}")
    }
}
