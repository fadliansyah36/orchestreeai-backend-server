package ai.orchestree.backend.generativestudio

import kotlinx.serialization.Serializable

@Serializable
data class ProcessedAssetMetadata(
    val assetId: String,
    val mimeType: String,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val tags: List<String>,
    val copyrightNotice: String
)

object MetadataProcessingService {

    fun extractAssetMetadata(assetId: String, rawBytes: ByteArray, detectedMime: String): ProcessedAssetMetadata {
        val size = rawBytes.size.toLong()
        var w = 1080
        var h = 1080

        // Basic dimension detection from PNG/JPEG headers
        if (detectedMime == "image/png" && rawBytes.size >= 24) {
            // PNG width/height at bytes 16-23
            w = ((rawBytes[16].toInt() and 0xFF) shl 24) or
                ((rawBytes[17].toInt() and 0xFF) shl 16) or
                ((rawBytes[18].toInt() and 0xFF) shl 8) or
                (rawBytes[19].toInt() and 0xFF)
            h = ((rawBytes[20].toInt() and 0xFF) shl 24) or
                ((rawBytes[21].toInt() and 0xFF) shl 16) or
                ((rawBytes[22].toInt() and 0xFF) shl 8) or
                (rawBytes[23].toInt() and 0xFF)
        }

        return ProcessedAssetMetadata(
            assetId = assetId,
            mimeType = detectedMime,
            width = if (w > 0) w else 1080,
            height = if (h > 0) h else 1080,
            sizeBytes = size,
            tags = listOf("orchestree", "generative_studio", "auto_processed"),
            copyrightNotice = "© Orchestree AI Enterprise Studio. All rights reserved."
        )
    }
}
