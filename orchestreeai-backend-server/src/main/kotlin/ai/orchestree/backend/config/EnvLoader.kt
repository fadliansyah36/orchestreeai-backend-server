package ai.orchestree.backend.config

import java.io.File

object EnvLoader {
    private val properties: MutableMap<String, String> = mutableMapOf()

    init {
        loadAll()
    }

    fun loadAll() {
        val candidateFiles = listOf(
            File("/app/applet/.env"),
            File("/root/.orchestreeai/secrets.properties"),
            File(".env"),
            File("../.env"),
            File("../../.env"),
            File(System.getProperty("user.home"), ".orchestreeai/secrets.properties")
        )
        for (file in candidateFiles) {
            if (file.exists() && file.isFile) {
                try {
                    file.forEachLine { rawLine ->
                        val line = rawLine.trim()
                        if (line.isNotBlank() && !line.startsWith("#") && line.contains("=")) {
                            val key = line.substringBefore("=").trim()
                            val value = line.substringAfter("=").trim().trim('"').trim('\'')
                            if (value.isNotBlank()) {
                                properties[key] = value
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun get(key: String, default: String = ""): String {
        val envVal = System.getenv(key)
        if (!envVal.isNullOrBlank() && !envVal.contains("placeholder", ignoreCase = true)) return envVal
        if (properties.isEmpty()) {
            loadAll()
        }
        val propVal = properties[key]
        if (!propVal.isNullOrBlank() && !propVal.contains("placeholder", ignoreCase = true)) return propVal
        return default
    }
}
