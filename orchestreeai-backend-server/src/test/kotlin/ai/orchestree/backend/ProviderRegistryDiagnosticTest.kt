package ai.orchestree.backend

import ai.orchestree.backend.config.EnvLoader
import org.junit.jupiter.api.Test
import java.sql.DriverManager

class ProviderRegistryDiagnosticTest {

    @Test
    fun testQueryProviderRegistries() {
        println("Connecting via DatabaseManager.getConnection()...")

        try {
            val conn = ai.orchestree.backend.billing.DatabaseManager.getConnection()
            if (conn == null) {
                println("DatabaseManager returned null connection!")
            } else {
                conn.use { c ->
                    val stmt = c.createStatement()

                    // Fix fallback_priority in Supabase according to Fase 82
                    println("=== UPDATING llm_providers fallback_priority (Fase 82 restoration) ===")
                    try {
                        stmt.executeUpdate("UPDATE llm_providers SET fallback_priority = 1, priority = 1 WHERE provider_code ILIKE '%openrouter%'")
                        stmt.executeUpdate("UPDATE llm_providers SET fallback_priority = 2, priority = 2 WHERE provider_code ILIKE '%groq%'")
                        stmt.executeUpdate("UPDATE llm_providers SET fallback_priority = 3, priority = 3 WHERE provider_code ILIKE '%deepseek%'")
                        stmt.executeUpdate("UPDATE llm_providers SET fallback_priority = 4, priority = 4 WHERE provider_code ILIKE '%anthropic%'")
                        stmt.executeUpdate("UPDATE llm_providers SET fallback_priority = 5, priority = 5 WHERE provider_code ILIKE '%openai%'")
                        stmt.executeUpdate("UPDATE llm_providers SET fallback_priority = 6, priority = 6 WHERE provider_code ILIKE '%kimi%'")
                        stmt.executeUpdate("UPDATE llm_providers SET fallback_priority = 99, priority = 99 WHERE provider_code ILIKE '%gemini%'")
                        stmt.executeUpdate("UPDATE llm_providers SET fallback_priority = 100, priority = 100 WHERE provider_code ILIKE '%ollama%'")
                        println("Successfully updated llm_providers priorities in Supabase!")
                    } catch (e: Exception) {
                        println("Error updating llm_providers: ${e.message}")
                    }

                    println("=== QUERY: SELECT * FROM llm_providers ORDER BY fallback_priority ===")
                    try {
                        val rs = stmt.executeQuery("SELECT id, name, provider_code, api_base_url, api_key_env, priority, fallback_priority, is_enabled, health_status FROM llm_providers ORDER BY fallback_priority ASC")
                        while (rs.next()) {
                            println("LLM: id=${rs.getString("id")}, name=${rs.getString("name")}, code=${rs.getString("provider_code")}, env=${rs.getString("api_key_env")}, priority=${rs.getInt("priority")}, fallback=${rs.getInt("fallback_priority")}, enabled=${rs.getBoolean("is_enabled")}, health=${rs.getString("health_status")}")
                        }
                    } catch (e: Exception) {
                        println("Error querying llm_providers: ${e.message}")
                    }

                    println("=== QUERY: SELECT * FROM image_provider_registry ===")
                    try {
                        val rs2 = stmt.executeQuery("SELECT * FROM image_provider_registry")
                        val meta = rs2.metaData
                        val cols = (1..meta.columnCount).map { meta.getColumnName(it) }
                        println("image_provider_registry columns: $cols")
                        while (rs2.next()) {
                            val row = cols.associateWith { rs2.getString(it) }
                            println("IMG: $row")
                        }
                    } catch (e: Exception) {
                        println("Error querying image_provider_registry: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            println("Connection to DB failed: ${e.message}")
        }
    }
}
