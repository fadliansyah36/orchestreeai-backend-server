package ai.orchestree.backend.generativestudio

import java.nio.charset.StandardCharsets

object CsvDocumentRenderer {

    /**
     * Menghasilkan teks CSV yang sepenuhnya mematuhi standar RFC 4180.
     */
    fun renderCsv(headers: List<String>, rows: List<List<String>>): ByteArray {
        val sb = StringBuilder()
        sb.append(headers.joinToString(",") { escapeCsv(it) }).append("\r\n")

        for (row in rows) {
            sb.append(row.joinToString(",") { escapeCsv(it) }).append("\r\n")
        }

        return sb.toString().toByteArray(StandardCharsets.UTF_8)
    }

    private fun escapeCsv(value: String): String {
        val containsSpecial = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")
        return if (containsSpecial) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }
}
