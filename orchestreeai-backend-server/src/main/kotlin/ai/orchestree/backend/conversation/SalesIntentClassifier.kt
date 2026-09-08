package ai.orchestree.backend.conversation

import org.json.JSONObject

enum class SalesIntentCode {
    SALAM,
    TANYA_PRODUK,
    TANYA_HARGA,
    TANYA_STOK,
    KONSULTASI,
    NEGO_DISKON,
    ORDER_CREATE,
    ORDER_STATUS,
    KOMPLAIN_RETUR,
    MINTA_HUMAN
}

data class IntentClassificationResult(
    val intent: SalesIntentCode,
    val confidence: Double,
    val extractedParameters: Map<String, Any> = emptyMap(),
    val rawReasoning: String = ""
)

object SalesIntentClassifier {

    fun classify(messageText: String): IntentClassificationResult {
        val textLower = messageText.trim().lowercase()
        val params = mutableMapOf<String, Any>()

        if (textLower.isBlank()) {
            return IntentClassificationResult(SalesIntentCode.SALAM, 0.5)
        }

        // 1. Minta Human Agent
        if (textLower.contains("manusia") || textLower.contains("operator") || textLower.contains("admin manusia") ||
            textLower.contains("staf") || textLower.contains("staf manusia") || textLower.contains("bicara orang") ||
            textLower.contains("hubungi cs") || textLower.contains("human") || textLower.contains("bukan bot")
        ) {
            params["reason"] = "Customer explicitly requested human intervention"
            return IntentClassificationResult(SalesIntentCode.MINTA_HUMAN, 0.95, params)
        }

        // 2. Komplain & Retur
        if (textLower.contains("rusak") || textLower.contains("cacat") || textLower.contains("salah kirim") ||
            textLower.contains("komplain") || textLower.contains("retur") || textLower.contains("refund") ||
            textLower.contains("pecah") || textLower.contains("klaim garansi") || textLower.contains("kecewa")
        ) {
            params["urgency"] = "HIGH"
            return IntentClassificationResult(SalesIntentCode.KOMPLAIN_RETUR, 0.90, params)
        }

        // 3. Order Status & Lacak Resi
        if (textLower.contains("resi") || textLower.contains("lacak") || textLower.contains("tracking") ||
            textLower.contains("sudah dikirim") || textLower.contains("sampai mana") || textLower.contains("status pesanan") ||
            textLower.contains("kapan dikirim") || textLower.contains("nomor resi")
        ) {
            val resiRegex = Regex("""([a-zA-Z0-9]{8,20})""")
            val matchedResi = resiRegex.findAll(messageText).map { it.value }.firstOrNull { it.any { c -> c.isDigit() } }
            if (matchedResi != null) params["extracted_resi"] = matchedResi
            return IntentClassificationResult(SalesIntentCode.ORDER_STATUS, 0.92, params)
        }

        // 4. Order Create / Checkout
        if (textLower.contains("mau pesan") || textLower.contains("mau beli") || textLower.contains("checkout") ||
            textLower.contains("order sekarang") || textLower.contains("no rekening") || textLower.contains("cara bayar") ||
            textLower.contains("transfer ke mana") || textLower.contains("beli yang ini") || textLower.contains("fix ambil")
        ) {
            params["buying_stage"] = "HOT_LEAD"
            return IntentClassificationResult(SalesIntentCode.ORDER_CREATE, 0.88, params)
        }

        // 5. Nego & Minta Diskon
        if (textLower.contains("diskon") || textLower.contains("nego") || textLower.contains("bisa kurang") ||
            textLower.contains("potongan") || textLower.contains("voucher") || textLower.contains("cashback") ||
            textLower.contains("harga grosir") || textLower.contains("ambil banyak")
        ) {
            params["discount_requested"] = true
            return IntentClassificationResult(SalesIntentCode.NEGO_DISKON, 0.86, params)
        }

        // 6. Cek Stok
        if (textLower.contains("stok") || textLower.contains("ready") || textLower.contains("ada barang") ||
            textLower.contains("masih ada") || textLower.contains("sisa berapa") || textLower.contains("po") ||
            textLower.contains("preorder") || textLower.contains("ready stock")
        ) {
            return IntentClassificationResult(SalesIntentCode.TANYA_STOK, 0.85, params)
        }

        // 7. Tanya Harga
        if (textLower.contains("harga") || textLower.contains("biaya") || textLower.contains("berapa") ||
            textLower.contains("pricelist") || textLower.contains("ongkir") || textLower.contains("tarif") ||
            textLower.contains("rp") || textLower.contains("price")
        ) {
            return IntentClassificationResult(SalesIntentCode.TANYA_HARGA, 0.84, params)
        }

        // 8. Konsultasi / Rekomendasi Kebutuhan
        if (textLower.contains("rekomendasi") || textLower.contains("saran") || textLower.contains("bagusan mana") ||
            textLower.contains("bingung") || textLower.contains("cocok") || textLower.contains("konsultasi") ||
            textLower.contains("bedanya") || textLower.contains("perbedaan")
        ) {
            return IntentClassificationResult(SalesIntentCode.KONSULTASI, 0.82, params)
        }

        // 9. Tanya Produk / Spesifikasi
        if (textLower.contains("spesifikasi") || textLower.contains("spek") || textLower.contains("ukuran") ||
            textLower.contains("size") || textLower.contains("warna") || textLower.contains("bahan") ||
            textLower.contains("fitur") || textLower.contains("varian") || textLower.contains("katalog")
        ) {
            return IntentClassificationResult(SalesIntentCode.TANYA_PRODUK, 0.80, params)
        }

        // 10. Salam / Sapaan Default
        if (textLower.contains("halo") || textLower.contains("hi") || textLower.contains("hai") ||
            textLower.contains("pagi") || textLower.contains("siang") || textLower.contains("sore") ||
            textLower.contains("malam") || textLower.contains("assalamualaikum") || textLower.contains("permisi")
        ) {
            return IntentClassificationResult(SalesIntentCode.SALAM, 0.75, params)
        }

        // General product fallback
        return IntentClassificationResult(SalesIntentCode.TANYA_PRODUK, 0.65, params)
    }
}
