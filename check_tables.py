import subprocess

kotlin_script = """
import ai.orchestree.backend.billing.DatabaseManager

val tables = listOf(
    "subscription_plans",
    "feature_capabilities",
    "tenant_capability_overrides",
    "enterprise_system_connections",
    "enterprise_data_sync_jobs",
    "enterprise_ingested_records",
    "ai_data_permission_policies",
    "ai_data_access_requests"
)

println("--- AUDIT DATABASE TABLES ---")
val conn = DatabaseManager.getConnection()
if (conn == null) {
    println("Database connection is null! (Fallback/offline mode or direct DB unreachable)")
} else {
    conn.use { c ->
        for (tbl in tables) {
            try {
                c.prepareStatement("SELECT count(*) FROM " + tbl).use { ps ->
                    ps.executeQuery().use { rs ->
                        if (rs.next()) {
                            println("TABLE: " + tbl + " -> EXISTS, ROW COUNT = " + rs.getInt(1))
                        }
                    }
                }
            } catch (e: Exception) {
                println("TABLE: " + tbl + " -> ERROR: " + e.message)
            }
        }
    }
}
"""
print("Script prepared")
