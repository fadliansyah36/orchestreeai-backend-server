package ai.orchestree.backend.generativestudio

import ai.orchestree.backend.database.repositories.selection.SelectionCriterionRecord
import ai.orchestree.backend.database.repositories.selection.SelectionResultRecord
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DeterministicRenderer {

    fun renderLayout(templateId: String, elements: Map<String, Any>): String {
        return "rendered_layout_ref"
    }

    /**
     * LANGKAH 3: Deterministic CSV Renderer (RFC 4180)
     */
    fun renderSelectionCsv(
        requestId: String,
        results: List<SelectionResultRecord>,
        criteria: List<SelectionCriterionRecord>
    ): String {
        val sb = StringBuilder()
        // Header
        val baseHeaders = mutableListOf(
            "Rank",
            "Result ID",
            "Total Score",
            "Classification",
            "Priority",
            "Risk Score",
            "Confidence Score",
            "Human Review Status",
            "Reviewer ID",
            "AI Grounded Insight"
        )
        criteria.forEach { baseHeaders.add("${it.criterion_name} (${it.weight_percentage}%)") }
        sb.append(baseHeaders.joinToString(",") { escapeCsv(it) }).append("\r\n")

        // Rows
        for (r in results.sortedBy { it.rank_position }) {
            val rowValues = mutableListOf(
                r.rank_position.toString(),
                r.id,
                String.format(java.util.Locale.US, "%.2f", r.total_score),
                r.recommendation_classification,
                r.priority_level,
                String.format(java.util.Locale.US, "%.2f", r.risk_score),
                String.format(java.util.Locale.US, "%.2f", r.confidence_score),
                r.human_review_status,
                r.human_reviewer_id ?: "-",
                r.ai_insight_text ?: ""
            )

            val breakdownMap = (r.score_breakdown as? kotlinx.serialization.json.JsonObject)
            for (c in criteria) {
                val score = breakdownMap?.get(c.criterion_name)?.toString()?.replace("\"", "") ?: "-"
                rowValues.add(score)
            }

            sb.append(rowValues.joinToString(",") { escapeCsv(it) }).append("\r\n")
        }

        return sb.toString()
    }

    /**
     * LANGKAH 3: Deterministic XLSX Renderer (Standard OpenPackaging OpenXML Spreadsheet)
     */
    fun renderSelectionXlsx(
        requestId: String,
        results: List<SelectionResultRecord>,
        criteria: List<SelectionCriterionRecord>
    ): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zip ->
            // 1. [Content_Types].xml
            zip.putNextEntry(ZipEntry("[Content_Types].xml"))
            zip.write("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                    <Default Extension="xml" ContentType="application/xml"/>
                    <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                    <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                </Types>
            """.trimIndent().toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()

            // 2. _rels/.rels
            zip.putNextEntry(ZipEntry("_rels/.rels"))
            zip.write("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                </Relationships>
            """.trimIndent().toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()

            // 3. xl/_rels/workbook.xml.rels
            zip.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
            zip.write("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                </Relationships>
            """.trimIndent().toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()

            // 4. xl/workbook.xml
            zip.putNextEntry(ZipEntry("xl/workbook.xml"))
            zip.write("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                    <sheets>
                        <sheet name="Selection Results" sheetId="1" r:id="rId1"/>
                    </sheets>
                </workbook>
            """.trimIndent().toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()

            // 5. xl/worksheets/sheet1.xml
            val sheetData = StringBuilder()
            sheetData.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            sheetData.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")

            // Row 1: Headers
            sheetData.append("""<row r="1">""")
            val headers = listOf("Rank", "Result ID", "Total Score", "Classification", "Priority", "Risk Score", "Confidence Score", "Review Status", "AI Grounded Insight")
            headers.forEachIndexed { idx, h ->
                val colRef = "${('A'.code + idx).toChar()}1"
                sheetData.append("""<c r="$colRef" t="inlineStr"><is><t>${escapeXml(h)}</t></is></c>""")
            }
            sheetData.append("""</row>""")

            // Data Rows
            results.sortedBy { it.rank_position }.forEachIndexed { rIdx, r ->
                val rowNum = rIdx + 2
                sheetData.append("""<row r="$rowNum">""")
                val vals = listOf(
                    r.rank_position.toString(),
                    r.id,
                    String.format(java.util.Locale.US, "%.2f", r.total_score),
                    r.recommendation_classification,
                    r.priority_level,
                    String.format(java.util.Locale.US, "%.2f", r.risk_score),
                    String.format(java.util.Locale.US, "%.2f", r.confidence_score),
                    r.human_review_status,
                    r.ai_insight_text ?: ""
                )
                vals.forEachIndexed { cIdx, v ->
                    val colRef = "${('A'.code + cIdx).toChar()}$rowNum"
                    sheetData.append("""<c r="$colRef" t="inlineStr"><is><t>${escapeXml(v)}</t></is></c>""")
                }
                sheetData.append("""</row>""")
            }

            sheetData.append("""</sheetData></worksheet>""")
            zip.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
            zip.write(sheetData.toString().toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()
        }

        return baos.toByteArray()
    }

    /**
     * LANGKAH 3: Deterministic PDF 1.4 Document Generator
     */
    fun renderSelectionPdf(
        requestId: String,
        results: List<SelectionResultRecord>,
        criteria: List<SelectionCriterionRecord>,
        summary: String = ""
    ): ByteArray {
        val pdfText = StringBuilder()
        pdfText.append("BT /F1 16 Tf 50 780 Td (OrchestreeAI - Selection & Ranking Report) Tj ET\n")
        pdfText.append("BT /F1 10 Tf 50 760 Td (Request ID: ${escapePdf(requestId)}) Tj ET\n")
        pdfText.append("BT /F1 10 Tf 50 745 Td (Total Candidates: ${results.size} | Generated: ${java.time.Instant.now()}) Tj ET\n")
        pdfText.append("BT /F1 10 Tf 50 720 Td (-----------------------------------------------------------------------------------------------------------------) Tj ET\n")

        var y = 700
        for (r in results.sortedBy { it.rank_position }.take(25)) {
            if (y < 80) break
            val line1 = "#${r.rank_position} [Score: ${String.format(java.util.Locale.US, "%.1f", r.total_score)}] [Class: ${r.recommendation_classification}] [Review: ${r.human_review_status}]"
            pdfText.append("BT /F1 11 Tf 50 $y Td (${escapePdf(line1)}) Tj ET\n")
            y -= 14
            val insight = r.ai_insight_text ?: "Evaluasi objektif berdasarkan bobot kriteria."
            val trimmedInsight = if (insight.length > 95) insight.take(92) + "..." else insight
            pdfText.append("BT /F1 9 Tf 65 $y Td (${escapePdf(trimmedInsight)}) Tj ET\n")
            y -= 18
        }

        val contentBytes = pdfText.toString().toByteArray(StandardCharsets.ISO_8859_1)

        val baos = ByteArrayOutputStream()
        val offsets = mutableListOf<Int>()

        fun writeString(s: String) {
            baos.write(s.toByteArray(StandardCharsets.ISO_8859_1))
        }

        writeString("%PDF-1.4\n%\u00e2\u00e3\u00cf\u00d3\n")

        // 1: Catalog
        offsets.add(baos.size())
        writeString("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")

        // 2: Pages
        offsets.add(baos.size())
        writeString("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n")

        // 3: Page
        offsets.add(baos.size())
        writeString("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>\nendobj\n")

        // 4: Stream Content
        offsets.add(baos.size())
        writeString("4 0 obj\n<< /Length ${contentBytes.size} >>\nstream\n")
        baos.write(contentBytes)
        writeString("\nendstream\nendobj\n")

        // 5: Font
        offsets.add(baos.size())
        writeString("5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n")

        // XRef
        val xrefOffset = baos.size()
        writeString("xref\n0 6\n0000000000 65535 f \n")
        for (offset in offsets) {
            writeString(String.format(java.util.Locale.US, "%010d 00000 n \n", offset))
        }

        writeString("trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n$xrefOffset\n%%EOF\n")

        return baos.toByteArray()
    }

    private fun escapeCsv(value: String): String {
        val containsSpecial = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")
        return if (containsSpecial) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    private fun escapeXml(value: String): String {
        return value.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun escapePdf(value: String): String {
        return value.replace("\\", "\\\\")
            .replace("(", "\\(")
            .replace(")", "\\)")
    }
}

