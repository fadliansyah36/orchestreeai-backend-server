package ai.orchestree.backend.generativestudio

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object XlsxDocumentRenderer {

    fun renderSpreadsheet(sheetName: String, headers: List<String>, rows: List<List<String>>): ByteArray {
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { zos ->
            // 1. [Content_Types].xml
            zos.putNextEntry(ZipEntry("[Content_Types].xml"))
            zos.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>""".toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            // 2. _rels/.rels
            zos.putNextEntry(ZipEntry("_rels/.rels"))
            zos.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>""".toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            // 3. xl/_rels/workbook.xml.rels
            zos.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
            zos.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
</Relationships>""".toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            // 4. xl/workbook.xml
            zos.putNextEntry(ZipEntry("xl/workbook.xml"))
            zos.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="${escapeXml(sheetName)}" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>""".toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            // 5. xl/worksheets/sheet1.xml
            zos.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
            val sheetXml = StringBuilder()
            sheetXml.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetData>
""")
            // Header row (row 1)
            sheetXml.append("    <row r=\"1\">\n")
            headers.forEachIndexed { colIdx, h ->
                val colLetter = ('A' + colIdx).toString()
                sheetXml.append("      <c r=\"$colLetter 1\" t=\"inlineStr\"><is><t>${escapeXml(h)}</t></is></c>\n")
            }
            sheetXml.append("    </row>\n")

            // Data rows (row 2..)
            rows.forEachIndexed { rowIdx, row ->
                val rNum = rowIdx + 2
                sheetXml.append("    <row r=\"$rNum\">\n")
                row.forEachIndexed { colIdx, cellVal ->
                    val colLetter = ('A' + colIdx).toString()
                    sheetXml.append("      <c r=\"$colLetter$rNum\" t=\"inlineStr\"><is><t>${escapeXml(cellVal)}</t></is></c>\n")
                }
                sheetXml.append("    </row>\n")
            }

            sheetXml.append("""  </sheetData>
</worksheet>""")
            zos.write(sheetXml.toString().toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()
        }
        return bos.toByteArray()
    }

    private fun escapeXml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
