package ai.orchestree.backend.generativestudio

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

object PdfDocumentRenderer {

    /**
     * Menghasilkan file PDF biner murni yang valid secara deterministik.
     */
    fun renderDocument(title: String, author: String, contentParagraphs: List<String>): ByteArray {
        val out = ByteArrayOutputStream()
        val textBody = StringBuilder()
        textBody.append("BT\n/F1 18 Tf\n50 750 Td\n(${escapePdf(title)}) Tj\nET\n")
        textBody.append("BT\n/F1 10 Tf\n50 725 Td\n(Author: ${escapePdf(author)}) Tj\nET\n")

        var y = 690
        for (p in contentParagraphs) {
            textBody.append("BT\n/F1 12 Tf\n50 $y Td\n(${escapePdf(p)}) Tj\nET\n")
            y -= 25
            if (y < 50) y = 750 // New page if needed
        }

        val contentStream = textBody.toString().toByteArray(StandardCharsets.ISO_8859_1)
        val streamLength = contentStream.size

        val pdfContent = StringBuilder()
        pdfContent.append("%PDF-1.4\n")

        val offsets = mutableListOf<Int>()

        // Object 1: Catalog
        offsets.add(pdfContent.length)
        pdfContent.append("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")

        // Object 2: Pages
        offsets.add(pdfContent.length)
        pdfContent.append("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n")

        // Object 3: Page
        offsets.add(pdfContent.length)
        pdfContent.append("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>\nendobj\n")

        // Object 4: Content Stream
        offsets.add(pdfContent.length)
        pdfContent.append("4 0 obj\n<< /Length $streamLength >>\nstream\n")
        out.write(pdfContent.toString().toByteArray(StandardCharsets.ISO_8859_1))
        out.write(contentStream)

        val trailerPart = StringBuilder()
        trailerPart.append("\nendstream\nendobj\n")

        // Object 5: Font
        val obj5Offset = out.size() + trailerPart.length
        trailerPart.append("5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n")

        val xrefOffset = out.size() + trailerPart.length
        trailerPart.append("xref\n0 6\n0000000000 65535 f \n")
        offsets.forEach { off ->
            trailerPart.append(String.format("%010d 00000 n \n", off))
        }
        trailerPart.append(String.format("%010d 00000 n \n", obj5Offset))
        trailerPart.append("trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n$xrefOffset\n%%EOF\n")

        out.write(trailerPart.toString().toByteArray(StandardCharsets.ISO_8859_1))
        return out.toByteArray()
    }

    private fun escapePdf(text: String): String {
        return text.replace("\\", "\\\\")
            .replace("(", "\\(")
            .replace(")", "\\)")
    }
}
