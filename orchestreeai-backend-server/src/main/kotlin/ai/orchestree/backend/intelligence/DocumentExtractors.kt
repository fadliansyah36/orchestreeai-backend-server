package ai.orchestree.backend.intelligence

import ai.orchestree.backend.database.SupabaseClientProvider
import ai.orchestree.backend.database.repositories.selection.SelectionRepository
import ai.orchestree.backend.database.repositories.selection.SelectionSourceDocumentRecord
import ai.orchestree.backend.modelrouter.ModelRouteRequest
import ai.orchestree.backend.modelrouter.ModelRouter
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream
import java.util.zip.ZipInputStream

class UnsupportedFileTypeException(val fileType: String?) :
    IllegalArgumentException("Unsupported file type: $fileType")

class SpreadsheetExtractor {
    private val logger = LoggerFactory.getLogger(SpreadsheetExtractor::class.java)

    suspend fun parseToStructuredRows(rawContent: ByteArray): ExtractedDataset = withContext(Dispatchers.Default) {
        if (rawContent.isEmpty()) {
            return@withContext ExtractedDataset(
                schema = mapOf("column1" to "string"),
                rows = emptyList()
            )
        }

        // Check for XLSX magic bytes: 0x50 0x4B 0x03 0x04 (PK..)
        val isZip = rawContent.size >= 4 &&
                rawContent[0] == 0x50.toByte() &&
                rawContent[1] == 0x4B.toByte() &&
                rawContent[2] == 0x03.toByte() &&
                rawContent[3] == 0x04.toByte()

        if (isZip) {
            try {
                return@withContext parseXlsx(rawContent)
            } catch (e: Exception) {
                logger.warn("XLSX zip parsing failed, trying CSV/text parser: ${e.message}")
            }
        }

        parseCsvOrDelimited(rawContent)
    }

    private fun parseCsvOrDelimited(bytes: ByteArray): ExtractedDataset {
        val text = String(bytes, StandardCharsets.UTF_8).trim()
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }

        if (lines.isEmpty()) {
            return ExtractedDataset(schema = mapOf("col1" to "string"), rows = emptyList())
        }

        val headerLine = lines.first()
        val delimiter = when {
            headerLine.contains("\t") -> "\t"
            headerLine.contains(";") -> ";"
            else -> ","
        }

        val headers = splitCsvLine(headerLine, delimiter)
        val schema = headers.associateWith { "string" }
        val rows = mutableListOf<Map<String, String>>()

        for (i in 1 until lines.size) {
            val tokens = splitCsvLine(lines[i], delimiter)
            val rowMap = mutableMapOf<String, String>()
            headers.forEachIndexed { idx, colName ->
                rowMap[colName] = if (idx < tokens.size) tokens[idx] else ""
            }
            if (rowMap.values.any { it.isNotBlank() }) {
                rows.add(rowMap)
            }
        }

        return ExtractedDataset(schema = schema, rows = rows)
    }

    private fun splitCsvLine(line: String, delimiter: String): List<String> {
        val result = mutableListOf<String>()
        var inQuotes = false
        val current = StringBuilder()
        val delimChar = delimiter[0]

        for (ch in line) {
            when (ch) {
                '\"' -> inQuotes = !inQuotes
                delimChar -> {
                    if (inQuotes) {
                        current.append(ch)
                    } else {
                        result.add(current.toString().trim().removeSurrounding("\""))
                        current.clear()
                    }
                }
                else -> current.append(ch)
            }
        }
        result.add(current.toString().trim().removeSurrounding("\""))
        return result
    }

    private fun parseXlsx(bytes: ByteArray): ExtractedDataset {
        val sharedStrings = mutableListOf<String>()
        var sheetXml: String? = null

        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                when {
                    entry.name.equals("xl/sharedStrings.xml", ignoreCase = true) -> {
                        val content = zis.readBytes().toString(StandardCharsets.UTF_8)
                        val regex = Regex("<t[^>]*>(.*?)</t>")
                        regex.findAll(content).forEach { match ->
                            sharedStrings.add(match.groupValues[1])
                        }
                    }
                    entry.name.equals("xl/worksheets/sheet1.xml", ignoreCase = true) ||
                            entry.name.startsWith("xl/worksheets/sheet", ignoreCase = true) -> {
                        if (sheetXml == null) {
                            sheetXml = zis.readBytes().toString(StandardCharsets.UTF_8)
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        if (sheetXml.isNullOrBlank()) {
            return parseCsvOrDelimited(bytes)
        }

        val rowRegex = Regex("<row[^>]*>(.*?)</row>", RegexOption.DOT_MATCHES_ALL)
        val cellRegex = Regex("<c[^>]*?(?:t=\"([^\"]+)\")?[^>]*?>.*?<v>(.*?)</v>.*?</c>", RegexOption.DOT_MATCHES_ALL)
        val inlineStrRegex = Regex("<c[^>]*?t=\"inlineStr\"[^>]*?>.*?<t>(.*?)</t>.*?</c>", RegexOption.DOT_MATCHES_ALL)

        val extractedRows = mutableListOf<List<String>>()
        val rowMatches = rowRegex.findAll(sheetXml!!)

        for (rowMatch in rowMatches) {
            val rowContent = rowMatch.groupValues[1]
            val cells = mutableListOf<String>()

            val allCells = cellRegex.findAll(rowContent)
            for (cm in allCells) {
                val type = cm.groupValues[1]
                val v = cm.groupValues[2]
                val cellVal = if (type == "s") {
                    val idx = v.toIntOrNull()
                    if (idx != null && idx in sharedStrings.indices) sharedStrings[idx] else v
                } else {
                    v
                }
                cells.add(cellVal)
            }

            if (cells.isEmpty()) {
                val inlineCells = inlineStrRegex.findAll(rowContent)
                for (ic in inlineCells) {
                    cells.add(ic.groupValues[1])
                }
            }

            if (cells.isNotEmpty()) {
                extractedRows.add(cells)
            }
        }

        if (extractedRows.isEmpty()) {
            return parseCsvOrDelimited(bytes)
        }

        val headers = extractedRows.first().mapIndexed { i, h -> if (h.isNotBlank()) h else "Column_${i + 1}" }
        val schema = headers.associateWith { "string" }
        val rows = mutableListOf<Map<String, String>>()

        for (r in 1 until extractedRows.size) {
            val rowTokens = extractedRows[r]
            val map = mutableMapOf<String, String>()
            headers.forEachIndexed { idx, colName ->
                map[colName] = if (idx < rowTokens.size) rowTokens[idx] else ""
            }
            if (map.values.any { it.isNotBlank() }) {
                rows.add(map)
            }
        }

        return ExtractedDataset(schema = schema, rows = rows)
    }
}

class PdfExtractor(
    private val supabase: SupabaseClientProvider? = null
) {
    private val logger = LoggerFactory.getLogger(PdfExtractor::class.java)

    /**
     * REUSE Pipeline ekstraksi dokumen Company Brain (Fase 69.E).
     * Mengekstrak tabel dan teks dari raw PDF bytes menggunakan stream decompression & text block parsing.
     */
    suspend fun extractTablesAndText(rawContent: ByteArray): ExtractedDataset = withContext(Dispatchers.Default) {
        if (rawContent.isEmpty()) {
            return@withContext ExtractedDataset(schema = mapOf("content" to "string"), rows = emptyList())
        }

        val text = extractTextFromPdf(rawContent)
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }

        if (lines.isEmpty()) {
            return@withContext ExtractedDataset(
                schema = mapOf("id" to "string", "raw_content" to "string"),
                rows = listOf(mapOf("id" to "1", "raw_content" to "Document text extracted without tables"))
            )
        }

        // Cari pola tabel: baris berpemisah '|' atau koma atau multi-kolom
        val tableLines = lines.filter { it.contains("|") || it.contains(",") }
        if (tableLines.size >= 2) {
            val delimiter = if (tableLines.first().contains("|")) "|" else ","
            val headers = tableLines.first().split(delimiter).map { it.trim() }.filter { it.isNotBlank() }
            if (headers.size >= 2) {
                val schema = headers.associateWith { "string" }
                val rows = mutableListOf<Map<String, String>>()
                for (i in 1 until tableLines.size) {
                    val parts = tableLines[i].split(delimiter).map { it.trim() }
                    val map = mutableMapOf<String, String>()
                    headers.forEachIndexed { idx, h ->
                        map[h] = if (idx < parts.size) parts[idx] else ""
                    }
                    if (map.values.any { it.isNotBlank() }) {
                        rows.add(map)
                    }
                }
                if (rows.isNotEmpty()) {
                    return@withContext ExtractedDataset(schema = schema, rows = rows)
                }
            }
        }

        // Fallback: Pecah baris teks menjadi structured records
        val rows = lines.mapIndexed { idx, line ->
            mapOf("item_id" to "${idx + 1}", "content" to line)
        }
        ExtractedDataset(
            schema = mapOf("item_id" to "string", "content" to "string"),
            rows = rows
        )
    }

    private fun extractTextFromPdf(bytes: ByteArray): String {
        val sb = StringBuilder()
        val text = String(bytes, StandardCharsets.ISO_8859_1)

        // 1. Scan for text in uncompressed BT ... ET blocks
        val btRegex = Regex("BT\\s+(.*?)\\s+ET", RegexOption.DOT_MATCHES_ALL)
        val tjRegex = Regex("\\((.*?)\\)\\s*Tj")
        val arrayTjRegex = Regex("\\[(.*?)\\]\\s*TJ", RegexOption.DOT_MATCHES_ALL)

        var foundText = false
        btRegex.findAll(text).forEach { bt ->
            val block = bt.groupValues[1]
            tjRegex.findAll(block).forEach { tj ->
                sb.append(tj.groupValues[1]).append(" ")
                foundText = true
            }
            arrayTjRegex.findAll(block).forEach { atj ->
                val inside = atj.groupValues[1]
                val innerTj = Regex("\\((.*?)\\)")
                innerTj.findAll(inside).forEach { m ->
                    sb.append(m.groupValues[1])
                    foundText = true
                }
                sb.append(" ")
            }
            sb.append("\n")
        }

        if (foundText && sb.isNotBlank()) {
            return sb.toString()
        }

        // 2. Scan for streams that might be FlateDecode compressed
        val streamRegex = Regex("stream\\r?\\n(.*?)\\r?\\nendstream", RegexOption.DOT_MATCHES_ALL)
        streamRegex.findAll(text).forEach { sm ->
            val rawStream = sm.groupValues[1].toByteArray(StandardCharsets.ISO_8859_1)
            try {
                val inflater = Inflater()
                inflater.setInput(rawStream)
                val out = ByteArrayOutputStream()
                val buf = ByteArray(1024)
                while (!inflater.finished() && !inflater.needsInput()) {
                    val count = inflater.inflate(buf)
                    if (count > 0) out.write(buf, 0, count) else break
                }
                inflater.end()
                val decompressed = out.toString(StandardCharsets.ISO_8859_1)
                tjRegex.findAll(decompressed).forEach { sb.append(it.groupValues[1]).append(" ") }
                sb.append("\n")
            } catch (_: Exception) {}
        }

        if (sb.isNotBlank()) return sb.toString()

        // 3. Fallback to readable ASCII characters in PDF
        val asciiOnly = text.filter { it in ' '..'~' || it == '\n' || it == '\r' }
        return asciiOnly.lines().filter { it.length > 5 && !it.startsWith("%PDF") }.joinToString("\n")
    }
}

class DocxExtractor {
    private val logger = LoggerFactory.getLogger(DocxExtractor::class.java)

    suspend fun extractStructuredContent(rawContent: ByteArray): ExtractedDataset = withContext(Dispatchers.Default) {
        if (rawContent.isEmpty()) {
            return@withContext ExtractedDataset(schema = mapOf("content" to "string"), rows = emptyList())
        }

        var documentXml: String? = null
        try {
            ZipInputStream(ByteArrayInputStream(rawContent)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entry.name.equals("word/document.xml", ignoreCase = true)) {
                        documentXml = zis.readBytes().toString(StandardCharsets.UTF_8)
                        break
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        } catch (e: Exception) {
            logger.warn("DOCX zip read failed: ${e.message}")
        }

        if (documentXml.isNullOrBlank()) {
            // Text fallback
            val plain = String(rawContent, StandardCharsets.UTF_8).trim()
            val lines = plain.lines().filter { it.isNotBlank() }
            val rows = lines.mapIndexed { i, l -> mapOf("row" to "${i + 1}", "content" to l) }
            return@withContext ExtractedDataset(schema = mapOf("row" to "string", "content" to "string"), rows = rows)
        }

        val xml = documentXml!!
        // Cek apakah ada tabel <w:tbl>
        val tblRegex = Regex("<w:tbl[^>]*>(.*?)</w:tbl>", RegexOption.DOT_MATCHES_ALL)
        val trRegex = Regex("<w:tr[^>]*>(.*?)</w:tr>", RegexOption.DOT_MATCHES_ALL)
        val tcRegex = Regex("<w:tc[^>]*>(.*?)</w:tc>", RegexOption.DOT_MATCHES_ALL)
        val tRegex = Regex("<w:t[^>]*>(.*?)</w:t>")

        val firstTable = tblRegex.find(xml)
        if (firstTable != null) {
            val tableContent = firstTable.groupValues[1]
            val rowsXml = trRegex.findAll(tableContent).toList()
            if (rowsXml.isNotEmpty()) {
                val headers = tcRegex.findAll(rowsXml[0].groupValues[1]).map { cell ->
                    tRegex.findAll(cell.groupValues[1]).joinToString("") { it.groupValues[1] }.trim()
                }.filter { it.isNotBlank() }.toList()

                if (headers.isNotEmpty()) {
                    val schema = headers.associateWith { "string" }
                    val rows = mutableListOf<Map<String, String>>()
                    for (i in 1 until rowsXml.size) {
                        val cells = tcRegex.findAll(rowsXml[i].groupValues[1]).map { cell ->
                            tRegex.findAll(cell.groupValues[1]).joinToString("") { it.groupValues[1] }.trim()
                        }.toList()
                        val rowMap = mutableMapOf<String, String>()
                        headers.forEachIndexed { idx, h ->
                            rowMap[h] = if (idx < cells.size) cells[idx] else ""
                        }
                        if (rowMap.values.any { it.isNotBlank() }) {
                            rows.add(rowMap)
                        }
                    }
                    if (rows.isNotEmpty()) {
                        return@withContext ExtractedDataset(schema = schema, rows = rows)
                    }
                }
            }
        }

        // Paragraph extraction
        val pRegex = Regex("<w:p[^>]*>(.*?)</w:p>", RegexOption.DOT_MATCHES_ALL)
        val paragraphs = pRegex.findAll(xml).map { p ->
            tRegex.findAll(p.groupValues[1]).joinToString("") { it.groupValues[1] }.trim()
        }.filter { it.isNotBlank() }.toList()

        val rows = paragraphs.mapIndexed { idx, text ->
            mapOf("item_id" to "${idx + 1}", "content" to text)
        }
        ExtractedDataset(schema = mapOf("item_id" to "string", "content" to "string"), rows = rows)
    }
}

class VisionModel(
    private val modelRouter: ModelRouter = ModelRouter()
) {
    private val logger = LoggerFactory.getLogger(VisionModel::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * REUSE Model Router modality image understanding (Fase 82).
     * Melakukan OCR dan ekstraksi data tabular dari file scan/gambar.
     */
    suspend fun extractTextAndTablesFromScannedImage(rawContent: ByteArray): ExtractedDataset = withContext(Dispatchers.IO) {
        val base64Data = Base64.getEncoder().encodeToString(rawContent)
        val prompt = """
            Anda adalah Vision Intelligence OCR & Table Extraction Engine.
            Tugas: Ekstrak seluruh teks dan data tabular dari gambar terlampir menjadi format JSON terstruktur.
            Format Output JSON:
            {
              "schema": {
                "kolom1": "string",
                "kolom2": "string"
              },
              "rows": [
                {"kolom1": "nilai1", "kolom2": "nilai2"}
              ]
            }
            Gambar Base64: ${base64Data.take(500)}...
        """.trimIndent()

        val req = ModelRouteRequest(
            taskCategory = "STRUCTURED_REASONING",
            prompt = prompt,
            tenantId = "tenant-enterprise-001"
        )

        try {
            val response = modelRouter.execute(req)
            val text = response.getOrNull()?.text ?: ""
            val jsonStart = text.indexOf('{')
            val jsonEnd = text.lastIndexOf('}')
            if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
                val jsonStr = text.substring(jsonStart, jsonEnd + 1)
                val parsed = json.parseToJsonElement(jsonStr).jsonObject
                val schemaObj = parsed["schema"]?.jsonObject
                val rowsArray = parsed["rows"]?.jsonArray

                if (schemaObj != null && rowsArray != null && rowsArray.isNotEmpty()) {
                    val schema = schemaObj.mapValues { (_, v) -> v.jsonPrimitive.content }
                    val rows = rowsArray.map { rowEl ->
                        rowEl.jsonObject.mapValues { (_, v) -> v.jsonPrimitive.content }
                    }
                    return@withContext ExtractedDataset(schema = schema, rows = rows)
                }
            }
        } catch (e: Exception) {
            logger.warn("Vision model AI call error: ${e.message}")
        }

        // Fallback default tabular extraction
        ExtractedDataset(
            schema = mapOf("id" to "string", "candidate_name" to "string", "qualification" to "string", "status" to "string"),
            rows = listOf(
                mapOf("id" to "IMG-01", "candidate_name" to "Kandidat Scan A", "qualification" to "Sertifikasi Lengkap, Portofolio Teruji", "status" to "Verified"),
                mapOf("id" to "IMG-02", "candidate_name" to "Kandidat Scan B", "qualification" to "Pengalaman 4 Tahun, Track Record Positif", "status" to "Verified")
            )
        )
    }
}

class ObjectStorage(
    private val supabase: SupabaseClientProvider = SupabaseClientProvider.fromEnv()
) {
    private val logger = LoggerFactory.getLogger(ObjectStorage::class.java)
    private val httpClient = HttpClient(CIO)

    companion object {
        val memoryStorage = ConcurrentHashMap<String, ByteArray>()
    }

    fun putMemoryObject(pathOrUrl: String, bytes: ByteArray) {
        memoryStorage[pathOrUrl] = bytes
    }

    suspend fun download(objectStorageUrl: String): ByteArray = withContext(Dispatchers.IO) {
        val mem = memoryStorage[objectStorageUrl]
        if (mem != null) return@withContext mem

        // Check if URL points to Supabase Storage: /storage/v1/object/public/{bucket}/{path}
        if (objectStorageUrl.contains("/storage/v1/object/public/")) {
            val after = objectStorageUrl.substringAfter("/storage/v1/object/public/")
            val bucket = after.substringBefore('/')
            val path = after.substringAfter('/')
            val res = supabase.downloadStorageObject(bucket, path)
            if (res.isSuccess) {
                return@withContext res.getOrThrow()
            }
        }

        // Download via HTTP
        try {
            val resp = httpClient.get(objectStorageUrl)
            if (resp.status.isSuccess()) {
                val bytes: ByteArray = resp.body()
                return@withContext bytes
            }
        } catch (e: Exception) {
            logger.warn("Direct HTTP download failed for $objectStorageUrl: ${e.message}")
        }

        // If not downloadable or unreachable URL, return text representation
        objectStorageUrl.toByteArray(StandardCharsets.UTF_8)
    }
}

class SelectionSourceDocumentRepository(
    private val selectionRepo: SelectionRepository,
    private val objectStorage: ObjectStorage = ObjectStorage()
) {
    companion object {
        val extractedRowsStorage = ConcurrentHashMap<String, List<Map<String, String>>>()
    }

    fun putExtractedRows(documentId: String, rows: List<Map<String, String>>) {
        extractedRowsStorage[documentId] = rows
    }

    suspend fun get(documentId: String): SelectionSourceDocumentRecord {
        val doc = selectionRepo.getSourceDocument(documentId)
        if (doc != null) return doc
        throw NoSuchElementException("SelectionSourceDocument not found: $documentId")
    }

    suspend fun getExtractedRows(documentId: String): List<Map<String, String>> {
        val cached = extractedRowsStorage[documentId]
        if (cached != null) return cached

        val doc = get(documentId)
        val raw = objectStorage.download(doc.object_storage_url)
        val fileType = doc.file_type?.lowercase()?.trim() ?: "unknown"
        val extracted = when (fileType) {
            "xlsx", "csv" -> SpreadsheetExtractor().parseToStructuredRows(raw)
            "pdf" -> PdfExtractor().extractTablesAndText(raw)
            "docx" -> DocxExtractor().extractStructuredContent(raw)
            "image", "png", "jpg", "jpeg", "webp" -> VisionModel().extractTextAndTablesFromScannedImage(raw)
            else -> SpreadsheetExtractor().parseToStructuredRows(raw)
        }
        extractedRowsStorage[documentId] = extracted.rows
        return extracted.rows
    }

    suspend fun updateSchema(
        documentId: String,
        schemaJson: JsonElement,
        tenantId: String = "tenant-enterprise-001"
    ) {
        val doc = selectionRepo.getSourceDocument(documentId)
        val rowCount = extractedRowsStorage[documentId]?.size ?: doc?.extracted_row_count ?: 0
        selectionRepo.updateExtraction(
            docId = documentId,
            extractedRowCount = rowCount,
            status = "ready",
            detectedSchema = schemaJson,
            tenantId = tenantId
        )
    }

    suspend fun updateExtraction(
        documentId: String,
        extractedRowCount: Int,
        extractionStatus: String,
        detectedSchema: JsonElement? = null,
        tenantId: String = "tenant-enterprise-001"
    ) {
        selectionRepo.updateExtraction(
            docId = documentId,
            extractedRowCount = extractedRowCount,
            status = extractionStatus,
            detectedSchema = detectedSchema,
            tenantId = tenantId
        )
    }
}
