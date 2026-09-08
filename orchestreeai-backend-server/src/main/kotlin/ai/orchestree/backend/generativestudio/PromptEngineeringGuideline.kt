package ai.orchestree.backend.generativestudio

/**
 * Prompt Engineering Knowledge Base & Structural Pattern Library (PRD Generative Studio & Addendum 2)
 * 
 * INTERNAL SYSTEM KNOWLEDGE ONLY:
 * Used by ContentPlanningService and PromptComposer to formulate highly structured, professional,
 * and detailed briefs to image generation / content planning models.
 * 
 * NOTE: This is NEVER exposed as a user-selectable template or interactive UI component.
 * User inputs via free-form text in UniversalPromptComposer, and the engine automatically
 * leverages these structural patterns during inference.
 */
object PromptEngineeringGuideline {

    val PATTERNS = mapOf(
        "social_banner" to """
            Saat menyusun brief visual untuk kategori ini, WAJIB pastikan Content Plan mencakup elemen berikut secara eksplisit (diekstrak/diinferensi dari deskripsi bebas user, JANGAN ditanyakan satu-satu ke user):
            - Komposisi: posisi elemen utama (rule of thirds, focal point, ruang untuk headline jika ada teks)
            - Pencahayaan & mood: derajat kontras, warna dominan, suasana (cerah/dramatis/minimalis) sesuai konteks kalimat user
            - Detail subjek: jika user menyebut objek/orang/produk, deskripsikan tekstur, material, sudut pandang kamera secara spesifik (bukan generik)
            - Elemen yang harus DIHINDARI: teks buram/typo pada gambar (kecuali headline yang di-overlay terpisah, Bagian 2), watermark asing, logo kompetitor
        """.trimIndent(),

        "company_profile" to """
            Untuk kategori ini, brief visual per slot gambar WAJIB:
            - Konsisten satu gaya visual di seluruh halaman (warna, pencahayaan, sudut) - JANGAN campur gaya fotorealistik dengan ilustrasi datar dalam satu dokumen
            - Fokus pada representasi profesional/korporat: jika slot adalah "foto tim" atau "kantor", arahkan ke gaya fotografi editorial bersih, bukan gaya artistik berlebihan
            - Prioritaskan RUANG NEGATIF (negative space) yang cukup di sekitar elemen visual agar layout teks (Fase 73.C Deterministic Renderer) tidak bertabrakan dengan detail visual penting
        """.trimIndent(),

        "business_plan" to """
            Kategori ini MINIM kebutuhan gambar dekoratif - HANYA generate visual jika Content Plan eksplisit memerlukan diagram konsep/ilustrasi pendukung (BUKAN chart data, chart WAJIB dari Deterministic Renderer dengan data asli, Fase 73.C poin 4). Jika perlu ilustrasi konsep, gaya WAJIB minimalis/skematik, bukan artistik dekoratif.
        """.trimIndent(),

        "presentation" to """
            Setiap slide gambar WAJIB mempertimbangkan area aman untuk teks presenter (jangan menaruh elemen visual penting di area yang biasanya tertutup judul/bullet point). Gaya visual antar-slide WAJIB konsisten (palet warna dan pencahayaan yang sama).
        """.trimIndent(),

        "report" to """
            Untuk dokumen laporan dan SOP operasional:
            - Utamakan visual teknis fungsional jika diperlukan (flowchart skematik, diagram alir SOP, layout teknis).
            - Minimalkan elemen dekoratif murni agar fokus pada kejelasan prosedur dan keterbacaan data.
            - Visual harus mendukung kejelasan instruksi langkah demi langkah secara tegas dan terukur.
        """.trimIndent(),

        "spreadsheet_data" to """
            Kategori data finansial dan spreadsheet:
            - Tidak memerlukan gambar dekoratif atau ilustratif generatif.
            - Fokus pada ketepatan struktural tabel, format angka (IDR/USD), dan pembagian kolom metrik.
            - Semua visualisasi data ditangani secara deterministik melalui grafik/tabel angka riil.
        """.trimIndent(),

        "general" to """
            Ekstrak makna implisit dari deskripsi bebas user semaksimal mungkin sebelum menganggap informasi kurang.
            Jika user hanya menulis "buatkan gambar gunung untuk postingan", WAJIB infer: konteks postingan (sosial media -> rasio persegi/vertikal umum), mood (adventure/tenang berdasar konteks kalimat sekitar), dan komposisi standar (foreground-midground-background untuk lanskap) - TANPA bertanya balik ke user kecuali benar-benar ambigu total.
        """.trimIndent()
    )

    fun getGuideline(useCaseCategory: String): String {
        return PATTERNS[useCaseCategory] ?: PATTERNS["general"] ?: ""
    }

    /**
     * Synthesizes an enriched structural image prompt from brief, use case, company context, and brand palette.
     * Transforms short user briefs into high-fidelity, production-grade image synthesis instructions.
     */
    fun enrichImagePrompt(
        userPrompt: String,
        useCaseCategory: String,
        companyName: String = "",
        brandPalette: String = ""
    ): String {
        val lower = userPrompt.lowercase()
        val paletteDesc = if (brandPalette.isNotBlank()) "berbasis palet brand ($brandPalette)" else "palet korporat harmonis"

        return when (useCaseCategory) {
            "social_banner" -> {
                when {
                    lower.contains("semeru") || lower.contains("gunung") || lower.contains("mountain") || lower.contains("adventure") ->
                        "Fotografi lanskap gunung bergaya adventure editorial, komposisi rule-of-thirds dengan puncak gunung sebagai focal point di sepertiga atas frame, pencahayaan golden hour dramatis dengan kontras tinggi, kabut tipis di lembah menambah kedalaman, warna dominan earth-tone (hijau tua, coklat, oranye langit), ruang negatif di area bawah-kiri untuk overlay headline, tanpa teks buram atau elemen artifisial, gaya fotorealistik tajam."
                    lower.contains("kopi") || lower.contains("coffee") || lower.contains("cafe") || lower.contains("kuliner") ->
                        "Fotografi produk komersial gaya editorial artisanal, sudut pandang 45 derajat close-up, pencahayaan alami jendela lembut (soft diffused light), uap hangat tipis, tekstur material meja kayu natural tajam, kedalaman ruang shallow depth of field, area aman ruang negatif di sisi kanan untuk teks promo, palet warna warm amber & deep brown, bebas teks buram."
                    lower.contains("diskon") || lower.contains("promo") || lower.contains("sale") ->
                        "Fotografi produk komersial premium studio modern dengan pencahayaan rim light kontemporer, penataan subjek dinamis dengan ruang negatif 40% untuk penempatan tipografi penawaran, $paletteDesc, refleksi permukaan halus, tanpa artefak visual atau watermark."
                    else ->
                        "Desain visual promosi digital komersial berkualitas tinggi untuk $companyName, komposisi seimbang dengan focal point tegas pada subjek utama, pencahayaan modern kontras terkontrol, ruang negatif bersih di sepertiga frame untuk penempatan teks, $paletteDesc, rendering fotorealistik tajam tanpa artefak."
                }
            }
            "company_profile" -> {
                "Fotografi arsitektur kantor dan tim profesional gaya editorial korporat modern untuk $companyName, pencahayaan interior terang merata (diffused natural lighting), komposisi simetris bersih dengan ruang negatif luas di area samping untuk layout dokumen resmi, $paletteDesc, atmosfer kolaboratif berwibawa tanpa distorsi artistik."
            }
            "presentation" -> {
                "Ilustrasi konsep grafis minimalis dan skematik untuk slide presentasi eksekutif $companyName, komposisi tata letak bersih dengan area aman lebar di 60% frame untuk penempatan bullet points dan judul, warna latar netral elegan dengan aksen $paletteDesc, tanpa elemen visual yang menutupi area teks utama."
            }
            "business_plan" -> {
                "Diagram skematik arsitektur konsep bisnis minimalis, garis bersih, palet monokromatik dengan highlight $paletteDesc, representasi alur kerja terstruktur tanpa elemen dekoratif berlebih."
            }
            "report" -> {
                "Ilustrasi proses alur operasional teknis standar, diagram skematik bersih, aksen warna terkalibrasi $paletteDesc, fokus tinggi pada keterbacaan alur kerja dan kejelasan instruksi operasional."
            }
            "spreadsheet_data" -> {
                "Struktur data matriks analitik dan visualisasi metrik finansial terukur, penataan tabular presisi, aksen garis bersih untuk pelaporan eksekutif $companyName."
            }
            else -> {
                if (lower.contains("gunung") || lower.contains("lanskap") || lower.contains("landscape")) {
                    "Fotografi lanskap megah berkualitas tinggi, komposisi seimbang foreground-midground-background, pencahayaan alami atmosferik, warna alami tajam dengan kontras dinamis, ruang pandang luas dan bersih."
                } else {
                    "Visualisasi komersial resolusi tinggi dengan komposisi terencana, subjek terdefinisi jelas, pencahayaan profesional, dan ruang penataan seimbang untuk kebutuhan enterprise $companyName."
                }
            }
        }
    }
}
