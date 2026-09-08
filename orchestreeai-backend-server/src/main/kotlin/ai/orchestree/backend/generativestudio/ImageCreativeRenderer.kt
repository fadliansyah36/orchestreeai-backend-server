package ai.orchestree.backend.generativestudio

import java.nio.charset.StandardCharsets

object ImageCreativeRenderer {

    /**
     * Menghasilkan placeholder SVG kreatif resolusi tinggi untuk kanvas desain iklan / banner.
     */
    fun renderSvgCreative(
        width: Int,
        height: Int,
        headline: String,
        subtext: String,
        primaryColorHex: String = "#1E40AF",
        accentColorHex: String = "#3B82F6"
    ): ByteArray {
        val svg = """<?xml version="1.0" encoding="UTF-8"?>
<svg width="$width" height="$height" viewBox="0 0 $width $height" fill="none" xmlns="http://www.w3.org/2000/svg">
  <defs>
    <linearGradient id="bgGrad" x1="0" y1="0" x2="$width" y2="$height" gradientUnits="userSpaceOnUse">
      <stop offset="0%" stop-color="$primaryColorHex"/>
      <stop offset="100%" stop-color="$accentColorHex"/>
    </linearGradient>
  </defs>
  <rect width="$width" height="$height" rx="16" fill="url(#bgGrad)"/>
  <circle cx="${width * 0.85}" cy="${height * 0.2}" r="${width * 0.15}" fill="white" fill-opacity="0.1"/>
  <text x="${width * 0.08}" y="${height * 0.45}" font-family="Arial, sans-serif" font-size="${(height * 0.08).toInt().coerceAtLeast(18)}" font-weight="bold" fill="#FFFFFF">${escapeXml(headline)}</text>
  <text x="${width * 0.08}" y="${height * 0.58}" font-family="Arial, sans-serif" font-size="${(height * 0.04).toInt().coerceAtLeast(12)}" fill="#E0E7FF">${escapeXml(subtext)}</text>
  <rect x="${width * 0.08}" y="${height * 0.72}" width="${width * 0.35}" height="${height * 0.12}" rx="8" fill="#F59E0B"/>
  <text x="${width * 0.12}" y="${height * 0.80}" font-family="Arial, sans-serif" font-size="${(height * 0.04).toInt().coerceAtLeast(12)}" font-weight="bold" fill="#FFFFFF">Beli Sekarang</text>
</svg>"""
        return svg.toByteArray(StandardCharsets.UTF_8)
    }

    private fun escapeXml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
