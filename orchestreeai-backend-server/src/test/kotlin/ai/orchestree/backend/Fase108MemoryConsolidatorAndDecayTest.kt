package ai.orchestree.backend

import ai.orchestree.backend.database.repositories.memory.MemoryDocumentRecord
import ai.orchestree.backend.database.repositories.memory.MemoryDocumentRepository
import ai.orchestree.backend.memory.CandidateInteraction
import ai.orchestree.backend.memory.HybridMemorySearchEngine
import ai.orchestree.backend.memory.MemoryConsolidator
import ai.orchestree.backend.memory.MemoryDecayEngine
import ai.orchestree.backend.scheduler.jobs.MemoryDecayJob
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class Fase108MemoryConsolidatorAndDecayTest {

    private lateinit var memoryRepo: MemoryDocumentRepository
    private lateinit var consolidator: MemoryConsolidator
    private lateinit var decayEngine: MemoryDecayEngine
    private lateinit var searchEngine: HybridMemorySearchEngine

    @BeforeEach
    fun setup() {
        memoryRepo = MemoryDocumentRepository()
        memoryRepo.clear()
        consolidator = MemoryConsolidator(memoryRepo, threshold = 0.60)
        decayEngine = MemoryDecayEngine(memoryRepo)
        searchEngine = HybridMemorySearchEngine(memoryRepo)
    }

    /**
     * LANGKAH 1 — AUDIT KUALITAS MEMORY CONSOLIDATOR (PRD Master Bagian 17.2)
     * Masukkan 20 interaksi remeh berturut-turut, VERIFIKASI TIDAK ADA yang masuk
     * sebagai memory permanen (Consolidator benar-benar menyaring).
     * Masukkan interaksi penting berbobot bisnis, VERIFIKASI berhasil masuk memori permanen.
     */
    @Test
    fun testMemoryConsolidatorFiltersTrivialInteractionsAndRetainsImportantOnes() = runBlocking {
        val tenantId = "tenant-audit-consolidator"

        val trivialMessages = listOf(
            "halo",
            "selamat pagi",
            "terima kasih banyak ya",
            "oke siap",
            "ok",
            "sip mantap",
            "wkwk lucu banget",
            "apa kabar mas?",
            "siap bos",
            "noted ya",
            "baik terima kasih",
            "testing 123",
            "halo halo",
            "thanks bro",
            "iya nih",
            "lol",
            "p",
            "keren sekali",
            "yup betul",
            "selamat sore semuanya"
        )
        assertEquals(20, trivialMessages.size)

        val trivialInteractions = trivialMessages.map { text ->
            CandidateInteraction(
                tenantId = tenantId,
                sender = "staff_user",
                content = text
            )
        }

        // Jalankan evaluasi batch untuk 20 interaksi remeh
        val decisions = consolidator.consolidateBatch(trivialInteractions)

        val acceptedCount = decisions.count { it.isConsolidated }
        val rejectedCount = decisions.count { !it.isConsolidated }

        println("[TEST_LOG] Interaksi remeh dievaluasi: ${decisions.size}")
        println("[TEST_LOG] Ditolak (tersaring): $rejectedCount | Diterima: $acceptedCount")

        // VERIFIKASI: TIDAK SEMUA MASUK (bahkan 0 dari 20 yang masuk karena murni obrolan remeh)
        assertTrue(acceptedCount < 20, "Consolidator harus menyaring obrolan remeh dan tidak memasukkan semuanya!")
        assertEquals(0, acceptedCount, "Seluruh 20 interaksi remeh harus berhasil disaring dan ditolak.")
        assertEquals(20, rejectedCount)

        // Verifikasi database memori tetap bersih dari obrolan remeh
        val storedDocs = memoryRepo.getAllActive(tenantId)
        assertEquals(0, storedDocs.size, "Database memori permanen tidak boleh menyimpan interaksi remeh.")

        // Uji interaksi substantif & penting berbobot tinggi
        val substantiveInteraction = CandidateInteraction(
            tenantId = tenantId,
            sender = "finance_director",
            content = "Keputusan rapat: Klien PT Maju Bersama disetujui diskon volume 15% untuk perpanjangan kontrak enterprise senilai Rp 150.000.000 dengan klausul SLA 99.9%."
        )

        val substantiveDecision = consolidator.evaluateInteraction(substantiveInteraction)
        println("[TEST_LOG] Interaksi substantif skor gabungan: ${substantiveDecision.combinedScore} (Importance: ${substantiveDecision.importanceScore}, Novelty: ${substantiveDecision.noveltyScore})")

        assertTrue(substantiveDecision.isConsolidated, "Interaksi substantif bernilai bisnis tinggi harus diterima masuk memori permanen.")
        assertNotNull(substantiveDecision.savedDocumentId)

        val storedAfterSubstantive = memoryRepo.getAllActive(tenantId)
        assertEquals(1, storedAfterSubstantive.size)
        assertEquals(substantiveDecision.savedDocumentId, storedAfterSubstantive[0].id)
    }

    /**
     * LANGKAH 2 — IMPLEMENTASI DECAY OTOMATIS
     * - Episodic: 90 hari linear decay (jika > 90 hari bobot = 0.0 dan diarsipkan).
     * - Competitive: 12 bulan (365 hari) rolling decay.
     * - Semantic / Company Context: Permanen (tidak pernah decay, bobot tetap 1.0).
     */
    @Test
    fun testAutomatedMemoryDecayJobLowersRelevanceAndArchivesOldMemories() = runBlocking {
        val tenantId = "tenant-decay-test"
        val now = Instant.now()
        val dayMs = 86_400_000L

        // 1. Dokumen Episodic Baru (10 hari) -> bobot ~0.888 (aktif)
        val freshEpisodic = MemoryDocumentRecord(
            id = "mem-fresh-10d",
            tenantId = tenantId,
            sourceType = "episodic",
            title = "Diskusi Produk Baru",
            content = "Catatan ide fitur chatbot WhatsApp minggu lalu.",
            relevanceWeight = 1.0,
            createdAt = now.toEpochMilli() - (10 * dayMs)
        )

        // 2. Dokumen Episodic Usang (95 hari) -> bobot 0.0 -> diarsipkan
        val oldEpisodic = MemoryDocumentRecord(
            id = "mem-old-95d",
            tenantId = tenantId,
            sourceType = "episodic",
            title = "Pengumuman Jadwal Libur Lebaran",
            content = "Informasi libur operasional kantor bulan lalu.",
            relevanceWeight = 1.0,
            createdAt = now.toEpochMilli() - (95 * dayMs)
        )

        // 3. Dokumen Competitive Pertengahan (180 hari) -> bobot ~0.506 (aktif)
        val midCompetitive = MemoryDocumentRecord(
            id = "mem-comp-180d",
            tenantId = tenantId,
            sourceType = "competitive",
            title = "Analisis Harga Kompetitor B",
            content = "Daftar harga paket langganan kompetitor B.",
            relevanceWeight = 1.0,
            createdAt = now.toEpochMilli() - (180 * dayMs)
        )

        // 4. Dokumen Competitive Usang (380 hari) -> bobot 0.0 -> diarsipkan
        val oldCompetitive = MemoryDocumentRecord(
            id = "mem-comp-380d",
            tenantId = tenantId,
            sourceType = "competitive",
            title = "Promo Diskon Akhir Tahun Lalu Kompetitor C",
            content = "Brosur promo akhir tahun lalu yang sudah kedaluwarsa.",
            relevanceWeight = 1.0,
            createdAt = now.toEpochMilli() - (380 * dayMs)
        )

        // 5. Dokumen Company Context / Semantic (400 hari) -> bobot tetap 1.0 (permanen, tidak decay)
        val permanentPolicy = MemoryDocumentRecord(
            id = "mem-policy-400d",
            tenantId = tenantId,
            sourceType = "company_context",
            title = "Kebijakan Keamanan dan Standar ISO 27001",
            content = "Seluruh kredensial API dan database dilarang disimpan di kode sumber.",
            relevanceWeight = 1.0,
            createdAt = now.toEpochMilli() - (400 * dayMs)
        )

        listOf(freshEpisodic, oldEpisodic, midCompetitive, oldCompetitive, permanentPolicy).forEach {
            memoryRepo.upsert(it)
        }

        // Jalankan Decay Engine
        val decaySummary = decayEngine.applyMemoryDecay(tenantId, referenceTime = now)
        println("[TEST_LOG] Hasil Decay: Diproses: ${decaySummary.totalProcessed}, Didecay: ${decaySummary.totalDecayed}, Diarsipkan: ${decaySummary.totalArchived}")

        assertEquals(5, decaySummary.totalProcessed)
        assertEquals(2, decaySummary.totalArchived) // oldEpisodic dan oldCompetitive harus diarsipkan

        // Verifikasi dokumen episodic baru (10 hari)
        val freshUpdated = memoryRepo.getById("mem-fresh-10d")
        assertNotNull(freshUpdated)
        assertFalse(freshUpdated!!.isArchived)
        assertEquals(1.0 - (10.0 / 90.0), freshUpdated.relevanceWeight, 0.01)

        // Verifikasi dokumen episodic usang (95 hari)
        val oldEpisodicUpdated = memoryRepo.getById("mem-old-95d")
        assertNotNull(oldEpisodicUpdated)
        assertTrue(oldEpisodicUpdated!!.isArchived, "Episodic > 90 hari harus diarsipkan")
        assertEquals(0.0, oldEpisodicUpdated.relevanceWeight, 0.001)

        // Verifikasi dokumen competitive pertengahan (180 hari)
        val compMidUpdated = memoryRepo.getById("mem-comp-180d")
        assertNotNull(compMidUpdated)
        assertFalse(compMidUpdated!!.isArchived)
        assertEquals(1.0 - (180.0 / 365.0), compMidUpdated.relevanceWeight, 0.01)

        // Verifikasi dokumen competitive usang (380 hari)
        val compOldUpdated = memoryRepo.getById("mem-comp-380d")
        assertNotNull(compOldUpdated)
        assertTrue(compOldUpdated!!.isArchived, "Competitive > 365 hari harus diarsipkan")
        assertEquals(0.0, compOldUpdated.relevanceWeight, 0.001)

        // Verifikasi dokumen kebijakan semantic permanen (400 hari)
        val policyUpdated = memoryRepo.getById("mem-policy-400d")
        assertNotNull(policyUpdated)
        assertFalse(policyUpdated!!.isArchived, "Company Context / Semantic dilarang didecay!")
        assertEquals(1.0, policyUpdated.relevanceWeight, 0.001)

        // Verifikasi job berkala scheduler
        val job = MemoryDecayJob(decayEngine)
        val jobResult = job.execute(tenantId)
        assertTrue(jobResult.contains("Memori diproses"))
    }

    /**
     * LANGKAH 3 — TUNING HYBRID SEARCH (KUALITAS RETRIEVAL DENGAN RE-RANKING)
     * - Mengambil Top-20 kandidat similarity awal
     * - Re-ranking skor gabungan: (similarity * 0.50) + (freshness * relevance_weight * 0.30) + (specificity * 0.20)
     * - Mengembalikan HANYA Top-5 ke LLM
     * - Membuktikan bahwa dokumen lama yang sudah decay tidak lagi mencemari Top-5
     */
    @Test
    fun testHybridSearchReRankingPrioritizesFreshRelevantMemoriesOverOutdatedOnes() = runBlocking {
        val tenantId = "tenant-hybrid-search"
        val now = Instant.now()
        val dayMs = 86_400_000L

        // Buat 10 dokumen memori yang semuanya mengandung kata kunci "diskon klien enterprise"
        // Dokumen A: Dokumen usang (80 hari, relevance_weight rendah: 0.11), kemiripan leksikal tinggi
        val outdatedDoc = MemoryDocumentRecord(
            id = "mem-outdated-high-similarity",
            tenantId = tenantId,
            sourceType = "episodic",
            title = "Kebijakan diskon klien enterprise tahun lalu",
            content = "diskon klien enterprise pada kuartal lalu sebesar 50% untuk semua produk legacy.",
            relevanceWeight = 0.11, // Hampir decayed
            importanceScore = 0.50,
            noveltyScore = 0.40,
            specificityScore = 0.40,
            createdAt = now.toEpochMilli() - (80 * dayMs)
        )

        // Dokumen B: Dokumen segar (3 hari, relevance_weight: 1.0, specificity: 0.95), kemiripan leksikal baik
        val freshDoc = MemoryDocumentRecord(
            id = "mem-fresh-high-relevance",
            tenantId = tenantId,
            sourceType = "episodic",
            title = "Kesepakatan diskon klien enterprise PT Maju",
            content = "diskon klien enterprise disetujui sebesar 12% untuk kontrak 2 tahun dengan minimum komitmen 500 kursi.",
            relevanceWeight = 1.0,
            importanceScore = 0.95,
            noveltyScore = 0.90,
            specificityScore = 0.95,
            createdAt = now.toEpochMilli() - (3 * dayMs)
        )

        // Dokumen C: Kebijakan permanen
        val permanentPolicy = MemoryDocumentRecord(
            id = "mem-permanent-pricing-rules",
            tenantId = tenantId,
            sourceType = "company_context",
            title = "SOP Batas Maksimum diskon klien enterprise",
            content = "diskon klien enterprise dilarang melebihi 20% tanpa persetujuan tertulis dari Chief Commercial Officer.",
            relevanceWeight = 1.0,
            importanceScore = 1.0,
            noveltyScore = 0.95,
            specificityScore = 0.90,
            createdAt = now.toEpochMilli() - (200 * dayMs)
        )

        // Buat beberapa dokumen tambahan agar mencapai > 5 kandidat
        val additionalDocs = (1..10).map { i ->
            MemoryDocumentRecord(
                id = "mem-extra-$i",
                tenantId = tenantId,
                sourceType = "episodic",
                title = "Catatan umum meeting $i mengenai diskon klien enterprise",
                content = "Diskusi umum $i tentang diskon klien enterprise dan penyesuaian target penjualan tahunan.",
                relevanceWeight = (1.0 - (i * 0.08)).coerceAtLeast(0.2),
                importanceScore = 0.60,
                noveltyScore = 0.50,
                specificityScore = 0.50,
                createdAt = now.toEpochMilli() - (i * 7 * dayMs)
            )
        }

        listOf(outdatedDoc, freshDoc, permanentPolicy).plus(additionalDocs).forEach {
            memoryRepo.upsert(it)
        }

        val query = "diskon klien enterprise kontrak terbaru"

        // 1. Eksekusi Pure Vector Search (enableReranking = false)
        val pureResult = searchEngine.search(tenantId, query, enableReranking = false, referenceTime = now)
        assertEquals(5, pureResult.topResults.size, "Retrieval harus mengembalikan maksimal Top-5")

        // 2. Eksekusi Re-Ranked Hybrid Search (enableReranking = true)
        val rerankedResult = searchEngine.search(tenantId, query, enableReranking = true, referenceTime = now)
        assertEquals(5, rerankedResult.topResults.size, "Retrieval hasil re-ranking harus mengembalikan HANYA Top-5")

        // 3. Uji A/B Komparasi
        val abComparison = searchEngine.runABComparison(tenantId, query, referenceTime = now)
        println("[TEST_LOG] A/B Query: '$query'")
        println("[TEST_LOG] Pure Vector Top-1: ${pureResult.topResults[0].document.id} (${pureResult.topResults[0].document.title})")
        println("[TEST_LOG] Re-Ranked Top-1: ${rerankedResult.topResults[0].document.id} (${rerankedResult.topResults[0].document.title})")
        println("[TEST_LOG] Summary: ${abComparison.relevanceShiftSummary}")

        // VERIFIKASI DEFINITION OF DONE:
        // Dokumen segar (freshDoc) harus berada di posisi teratas (#1) pada hasil Re-ranking
        assertEquals("mem-fresh-high-relevance", rerankedResult.topResults[0].document.id,
            "Dokumen segar dengan bobot relevansi dan spesifisitas tinggi wajib menduduki peringkat #1 pada Re-ranking")

        // Dokumen usang (outdatedDoc) dengan bobot relevansi 0.11 harus memiliki compositeScore lebih rendah daripada dokumen segar
        val outdatedInReranked = rerankedResult.topResults.find { it.document.id == "mem-outdated-high-similarity" }
        val freshInReranked = rerankedResult.topResults.find { it.document.id == "mem-fresh-high-relevance" }
        assertNotNull(freshInReranked)

        if (outdatedInReranked != null) {
            assertTrue(freshInReranked!!.compositeScore > outdatedInReranked.compositeScore,
                "Skor dokumen segar (${freshInReranked.compositeScore}) harus lebih tinggi dari dokumen usang (${outdatedInReranked.compositeScore})")
        }

        // Dokumen yang sudah benar-benar decayed (>90 hari) dan berstatus archived TIDAK boleh muncul di retrieval
        val deadDoc = MemoryDocumentRecord(
            id = "mem-dead-120d",
            tenantId = tenantId,
            sourceType = "episodic",
            title = "diskon klien enterprise 120 hari lalu",
            content = "diskon klien enterprise yang sudah kadaluwarsa",
            relevanceWeight = 0.0,
            isArchived = true,
            createdAt = now.toEpochMilli() - (120 * dayMs)
        )
        memoryRepo.upsert(deadDoc)

        val searchAfterArchived = searchEngine.search(tenantId, query, enableReranking = true, referenceTime = now)
        val foundDeadDoc = searchAfterArchived.topResults.any { it.document.id == "mem-dead-120d" }
        assertFalse(foundDeadDoc, "Dokumen yang diarsipkan tidak boleh muncul sama sekali dalam hasil pencarian!")
    }
}
