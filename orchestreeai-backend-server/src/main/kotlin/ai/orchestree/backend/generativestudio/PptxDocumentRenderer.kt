package ai.orchestree.backend.generativestudio

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object PptxDocumentRenderer {

    fun renderPresentation(title: String, slides: List<Pair<String, List<String>>>): ByteArray {
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { zos ->
            // 1. [Content_Types].xml
            zos.putNextEntry(ZipEntry("[Content_Types].xml"))
            val ctXml = StringBuilder()
            ctXml.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/ppt/presentation.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml"/>
""")
            slides.forEachIndexed { idx, _ ->
                ctXml.append("  <Override PartName=\"/ppt/slides/slide${idx + 1}.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.presentationml.slide+xml\"/>\n")
            }
            ctXml.append("</Types>")
            zos.write(ctXml.toString().toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            // 2. _rels/.rels
            zos.putNextEntry(ZipEntry("_rels/.rels"))
            zos.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="ppt/presentation.xml"/>
</Relationships>""".toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            // 3. ppt/_rels/presentation.xml.rels
            zos.putNextEntry(ZipEntry("ppt/_rels/presentation.xml.rels"))
            val prXml = StringBuilder()
            prXml.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
""")
            slides.forEachIndexed { idx, _ ->
                prXml.append("  <Relationship Id=\"rId${idx + 1}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide\" Target=\"slides/slide${idx + 1}.xml\"/>\n")
            }
            prXml.append("</Relationships>")
            zos.write(prXml.toString().toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            // 4. ppt/presentation.xml
            zos.putNextEntry(ZipEntry("ppt/presentation.xml"))
            val presXml = StringBuilder()
            presXml.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:presentation xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <p:sldIdLst>
""")
            slides.forEachIndexed { idx, _ ->
                presXml.append("    <p:sldId id=\"${256 + idx}\" r:id=\"rId${idx + 1}\"/>\n")
            }
            presXml.append("""  </p:sldIdLst>
</p:presentation>""")
            zos.write(presXml.toString().toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            // 5. ppt/slides/slideN.xml
            slides.forEachIndexed { idx, (slideTitle, bullets) ->
                zos.putNextEntry(ZipEntry("ppt/slides/slide${idx + 1}.xml"))
                val sldXml = StringBuilder()
                sldXml.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sld xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">
  <p:cSld>
    <p:spTree>
      <p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr>
      <p:grpSpPr/>
      <p:sp>
        <p:nvSpPr><p:cNvPr id="2" name="Title"/><p:cNvSpPr/><p:nvPr/></p:nvSpPr>
        <p:spPr/>
        <p:txBody>
          <a:bodyPr/>
          <a:p><a:r><a:t>${escapeXml(slideTitle)}</a:t></a:r></a:p>
        </p:txBody>
      </p:sp>
""")
                bullets.forEach { b ->
                    sldXml.append("      <p:sp><p:nvSpPr><p:cNvPr id=\"3\" name=\"Body\"/><p:cNvSpPr/><p:nvPr/></p:nvSpPr><p:spPr/><p:txBody><a:bodyPr/><a:p><a:r><a:t>• ${escapeXml(b)}</a:t></a:r></a:p></p:txBody></p:sp>\n")
                }
                sldXml.append("""    </p:spTree>
  </p:cSld>
</p:sld>""")
                zos.write(sldXml.toString().toByteArray(StandardCharsets.UTF_8))
                zos.closeEntry()
            }
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
