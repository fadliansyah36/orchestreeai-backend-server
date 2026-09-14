package ai.orchestree.backend.generativestudio

import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/**
 * MetadataStripper (PRD Fase 3 / Generative Studio & Privacy)
 * Strips EXIF tags, GPS telemetry, camera parameters, and personal metadata from images
 * before storing into object storage or publishing to public channels.
 */
object MetadataStripper {
    private val logger = LoggerFactory.getLogger(MetadataStripper::class.java)

    /**
     * Strips metadata from raw image bytes according to file type.
     * Preserves visual pixel fidelity while removing all EXIF, XMP, and metadata chunks.
     */
    fun stripMetadata(rawBytes: ByteArray, mimeType: String): ByteArray {
        if (rawBytes.isEmpty()) return rawBytes

        return try {
            when {
                mimeType.contains("jpeg", ignoreCase = true) || mimeType.contains("jpg", ignoreCase = true) -> {
                    stripJpegExif(rawBytes)
                }
                mimeType.contains("png", ignoreCase = true) -> {
                    stripPngMetadata(rawBytes)
                }
                mimeType.contains("svg", ignoreCase = true) -> {
                    stripSvgMetadata(rawBytes)
                }
                else -> {
                    // Fallback to ImageIO re-encoding to completely eliminate foreign metadata
                    sanitizeViaImageIo(rawBytes, "png") ?: rawBytes
                }
            }
        } catch (e: Exception) {
            logger.warn("Metadata stripping encountered an error, falling back to original bytes: ${e.message}")
            rawBytes
        }
    }

    /**
     * Sanitizes image bytes before public distribution or storage based on file name or MIME type.
     */
    fun sanitizeImageForPublishing(rawBytes: ByteArray, fileName: String): ByteArray {
        val lower = fileName.lowercase()
        val mime = when {
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
            lower.endsWith(".png") -> "image/png"
            lower.endsWith(".svg") -> "image/svg+xml"
            lower.endsWith(".webp") -> "image/webp"
            else -> "image/png"
        }
        return stripMetadata(rawBytes, mime)
    }

    /**
     * Checks if raw bytes contain EXIF markers (e.g. 0xFFE1 in JPEG or 'eXIf' in PNG).
     */
    fun containsExifMetadata(rawBytes: ByteArray): Boolean {
        if (rawBytes.size < 4) return false

        // JPEG APP1 check
        if ((rawBytes[0].toInt() and 0xFF) == 0xFF && (rawBytes[1].toInt() and 0xFF) == 0xD8) {
            var i = 2
            while (i < rawBytes.size - 4) {
                if ((rawBytes[i].toInt() and 0xFF) == 0xFF && (rawBytes[i + 1].toInt() and 0xFF) == 0xE1) {
                    return true
                }
                if ((rawBytes[i].toInt() and 0xFF) == 0xFF && (rawBytes[i + 1].toInt() and 0xFF) == 0xDA) {
                    break // SOS reached
                }
                i++
            }
        }

        // PNG check for eXIf or tEXt
        val asString = String(rawBytes.take(2048).toByteArray(), Charsets.ISO_8859_1)
        if (asString.contains("eXIf") || asString.contains("tEXt") || asString.contains("iTXt")) {
            return true
        }

        return false
    }

    /**
     * Fast binary stripping of JPEG APP markers (APP1 EXIF, APP2, APP13 IPTC)
     * keeping SOI (0xFFD8), quant tables, frame headers, Huffman tables, SOS, and scan data.
     */
    private fun stripJpegExif(bytes: ByteArray): ByteArray {
        if (bytes.size < 4 || (bytes[0].toInt() and 0xFF) != 0xFF || (bytes[1].toInt() and 0xFF) != 0xD8) {
            return sanitizeViaImageIo(bytes, "jpeg") ?: bytes
        }

        val out = ByteArrayOutputStream(bytes.size)
        out.write(bytes[0].toInt())
        out.write(bytes[1].toInt())

        var offset = 2
        var strippedAny = false

        while (offset < bytes.size - 1) {
            val markerPrefix = bytes[offset].toInt() and 0xFF
            if (markerPrefix != 0xFF) {
                // Not a valid marker prefix, copy rest or fallback
                out.write(bytes, offset, bytes.size - offset)
                break
            }

            val marker = bytes[offset + 1].toInt() and 0xFF

            // SOS (Start of Scan - 0xDA) means entropy-coded data follows until EOI
            if (marker == 0xDA) {
                out.write(bytes, offset, bytes.size - offset)
                break
            }

            // Standalone markers without length: RST0-RST7 (0xD0-0xD7), SOI (0xD8), EOI (0xD9)
            if (marker == 0xD8 || marker == 0xD9 || (marker in 0xD0..0xD7)) {
                out.write(bytes[offset].toInt())
                out.write(bytes[offset + 1].toInt())
                offset += 2
                continue
            }

            if (offset + 3 >= bytes.size) {
                out.write(bytes, offset, bytes.size - offset)
                break
            }

            // Length includes the two length bytes
            val length = ((bytes[offset + 2].toInt() and 0xFF) shl 8) or (bytes[offset + 3].toInt() and 0xFF)

            // Strip APP1 (0xE1: EXIF/XMP), APP2 (0xE2), APP13 (0xED: Photoshop), Comment (0xFE)
            val shouldStrip = marker == 0xE1 || marker == 0xE2 || marker == 0xED || marker == 0xFE

            if (shouldStrip) {
                strippedAny = true
                offset += 2 + length
            } else {
                val chunkSize = 2 + length
                if (offset + chunkSize <= bytes.size) {
                    out.write(bytes, offset, chunkSize)
                    offset += chunkSize
                } else {
                    out.write(bytes, offset, bytes.size - offset)
                    break
                }
            }
        }

        val result = out.toByteArray()
        return if (strippedAny && result.size > 100) result else (sanitizeViaImageIo(bytes, "jpeg") ?: bytes)
    }

    /**
     * Fast binary stripping of PNG ancillary metadata chunks (tEXt, zTXt, iTXt, eXIf).
     * Preserves critical chunks: IHDR, PLTE, IDAT, IEND.
     */
    private fun stripPngMetadata(bytes: ByteArray): ByteArray {
        val pngSignature = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        if (bytes.size < 8) return bytes
        for (i in 0..7) {
            if (bytes[i] != pngSignature[i]) return sanitizeViaImageIo(bytes, "png") ?: bytes
        }

        val out = ByteArrayOutputStream(bytes.size)
        out.write(pngSignature)

        var offset = 8
        var strippedAny = false

        while (offset + 12 <= bytes.size) {
            val length = ((bytes[offset].toInt() and 0xFF) shl 24) or
                    ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
                    ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
                    (bytes[offset + 3].toInt() and 0xFF)

            val chunkType = String(bytes, offset + 4, 4, Charsets.US_ASCII)
            val totalChunkSize = 12 + length // 4 length + 4 type + length data + 4 CRC

            if (offset + totalChunkSize > bytes.size) {
                out.write(bytes, offset, bytes.size - offset)
                break
            }

            // Ancillary chunks containing metadata to strip
            val isMetadataChunk = chunkType in listOf("tEXt", "zTXt", "iTXt", "eXIf", "tIME", "pHYs")

            if (isMetadataChunk) {
                strippedAny = true
                offset += totalChunkSize
            } else {
                out.write(bytes, offset, totalChunkSize)
                offset += totalChunkSize
            }

            if (chunkType == "IEND") break
        }

        val result = out.toByteArray()
        return if (strippedAny && result.size > 30) result else (sanitizeViaImageIo(bytes, "png") ?: bytes)
    }

    /**
     * Strips metadata / script tags from SVG documents.
     */
    private fun stripSvgMetadata(bytes: ByteArray): ByteArray {
        val svgContent = String(bytes, Charsets.UTF_8)
        val cleaned = svgContent
            .replace(Regex("""<metadata[\s\S]*?</metadata>""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""<script[\s\S]*?</script>""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""<!--[\s\S]*?-->"""), "")
        return cleaned.toByteArray(Charsets.UTF_8)
    }

    /**
     * Memory-safe re-encoding via ImageIO as clean raster pixels without any metadata header.
     */
    private fun sanitizeViaImageIo(bytes: ByteArray, format: String): ByteArray? {
        return try {
            val image = ImageIO.read(ByteArrayInputStream(bytes)) ?: return null
            val out = ByteArrayOutputStream()
            val cleanFormat = if (format.contains("png", ignoreCase = true)) "png" else "jpeg"
            ImageIO.write(image, cleanFormat, out)
            out.toByteArray()
        } catch (e: Exception) {
            null
        }
    }
}
