package ai.orchestree.backend.generativestudio

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object DocxDocumentRenderer {

    fun renderDocument(title: String, paragraphs: List<String>): ByteArray {
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { zos ->
            // 1. [Content_Types].xml
            zos.putNextEntry(ZipEntry("[Content_Types].xml"))
            zos.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>""".toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            // 2. _rels/.rels
            zos.putNextEntry(ZipEntry("_rels/.rels"))
            zos.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>""".toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            // 3. word/document.xml
            zos.putNextEntry(ZipEntry("word/document.xml"))
            val docXml = StringBuilder()
            docXml.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:body>
    <w:p><w:r><w:rPr><w:b/><w:sz w:val="36"/></w:rPr><w:t>${escapeXml(title)}</w:t></w:r></w:p>
""")
            for (p in paragraphs) {
                docXml.append("    <w:p><w:r><w:t>${escapeXml(p)}</w:t></w:r></w:p>\n")
            }
            docXml.append("""    <w:sectPr/>
  </w:body>
</w:document>""")
            zos.write(docXml.toString().toByteArray(StandardCharsets.UTF_8))
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
