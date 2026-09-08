package ai.orchestree.backend

import ai.orchestree.backend.billing.DatabaseManager
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Test
import java.io.File
import java.sql.Connection
import java.sql.ResultSet
import kotlin.test.assertNotNull

class Langkah1BackupAuditTest {

    private fun resultSetToJson(rs: ResultSet): JsonArray {
        val md = rs.metaData
        val columnCount = md.columnCount
        val list = mutableListOf<JsonObject>()

        while (rs.next()) {
            val map = mutableMapOf<String, JsonElement>()
            for (i in 1..columnCount) {
                val colName = md.getColumnLabel(i) ?: md.getColumnName(i)
                val obj = rs.getObject(i)
                map[colName] = when (obj) {
                    null -> JsonNull
                    is Number -> JsonPrimitive(obj)
                    is Boolean -> JsonPrimitive(obj)
                    else -> JsonPrimitive(obj.toString())
                }
            }
            list.add(JsonObject(map))
        }
        return JsonArray(list)
    }

    @Test
    fun executeBackupAndAudit() {
        println("=================================================================")
        println("=== LANGKAH 1: BACKUP & AUDIT SELURUH USERS & TENANTS SAAT INI ===")
        println("=================================================================")

        val conn: Connection? = DatabaseManager.getConnection()
        assertNotNull(conn, "Database connection to Supabase PostgreSQL must be active!")

        val backupDirServer = File("backups").apply { mkdirs() }
        val backupDirRoot = File("../backups").apply { mkdirs() }

        conn.use { c ->
            // 1.1 Export public.users
            println("\n--- 1.1.1 Export public.users ---")
            val publicUsersArray = c.createStatement().use { st ->
                val rs = st.executeQuery("SELECT * FROM public.users ORDER BY created_at ASC")
                resultSetToJson(rs)
            }
            val jsonStrPublic = Json { prettyPrint = true }.encodeToString(JsonArray.serializer(), publicUsersArray)
            File(backupDirServer, "public_users_backup.json").writeText(jsonStrPublic)
            File(backupDirRoot, "public_users_backup.json").writeText(jsonStrPublic)
            println("Exported ${publicUsersArray.size} rows to public_users_backup.json")

            // 1.1 Export auth.users
            println("\n--- 1.1.2 Export auth.users ---")
            var authUsersCount = 0
            val authUsersArray = try {
                c.createStatement().use { st ->
                    val rs = st.executeQuery("SELECT id, instance_id, email, encrypted_password, email_confirmed_at, invited_at, confirmation_token, confirmation_sent_at, recovery_token, recovery_sent_at, email_change_token_new, email_change, email_change_sent_at, last_sign_in_at, raw_app_meta_data, raw_user_meta_data, is_super_admin, created_at, updated_at, phone, phone_confirmed_at, phone_change, phone_change_token, phone_change_sent_at, confirmed_at, email_change_token_current, email_change_confirm_status, banned_until, reauthentication_token, reauthentication_sent_at, is_sso_user, deleted_at, is_anonymous FROM auth.users ORDER BY created_at ASC")
                    resultSetToJson(rs)
                }
            } catch (e: Exception) {
                println("Note: querying auth.users encountered: ${e.message}")
                JsonArray(emptyList())
            }
            authUsersCount = authUsersArray.size
            val jsonStrAuth = Json { prettyPrint = true }.encodeToString(JsonArray.serializer(), authUsersArray)
            File(backupDirServer, "auth_users_backup.json").writeText(jsonStrAuth)
            File(backupDirRoot, "auth_users_backup.json").writeText(jsonStrAuth)
            println("Exported $authUsersCount rows to auth_users_backup.json")

            // 1.1 Export tenants
            println("\n--- 1.1.3 Export tenants ---")
            val tenantsArray = c.createStatement().use { st ->
                val rs = st.executeQuery("SELECT * FROM public.tenants ORDER BY created_at ASC")
                resultSetToJson(rs)
            }
            val jsonStrTenants = Json { prettyPrint = true }.encodeToString(JsonArray.serializer(), tenantsArray)
            File(backupDirServer, "tenants_backup.json").writeText(jsonStrTenants)
            File(backupDirRoot, "tenants_backup.json").writeText(jsonStrTenants)
            println("Exported ${tenantsArray.size} rows to tenants_backup.json")

            // 1.3 Detailed Query & Audit of ALL Tenants and Users
            println("\n=================================================================")
            println("=== 1.3 AUDIT LAPORAN LENGKAP TENANTS & USERS DI SUPABASE ===")
            println("=================================================================")

            val auditQuery = """
                SELECT 
                    t.id AS tenant_id,
                    t.name AS tenant_name,
                    t.domain AS tenant_domain,
                    t.status AS tenant_status,
                    t.tier AS tenant_tier,
                    t.created_at AS tenant_created_at,
                    u.id AS user_id,
                    u.name AS user_name,
                    u.email AS user_email,
                    u.auth_user_id AS auth_user_id,
                    u.created_at AS user_created_at
                FROM public.tenants t
                LEFT JOIN public.users u ON u.tenant_id = t.id
                ORDER BY t.created_at ASC, u.created_at ASC
            """.trimIndent()

            val auditLines = mutableListOf<String>()
            auditLines.add("TOTAL TENANTS DI DATABASE: ${tenantsArray.size}")
            auditLines.add("TOTAL PUBLIC USERS DI DATABASE: ${publicUsersArray.size}")
            auditLines.add("TOTAL AUTH USERS DI DATABASE: $authUsersCount")
            auditLines.add("-------------------------------------------------------------------------------------------------------------")
            auditLines.add(String.format("%-32s | %-28s | %-32s | %s", "TENANT NAME", "TENANT ID", "USER/OWNER EMAIL", "CREATED AT"))
            auditLines.add("-------------------------------------------------------------------------------------------------------------")

            c.createStatement().use { st ->
                val rs = st.executeQuery(auditQuery)
                while (rs.next()) {
                    val tenantName = rs.getString("tenant_name") ?: "(None)"
                    val tenantId = rs.getString("tenant_id") ?: "(None)"
                    val email = rs.getString("user_email") ?: "(Belum ada user)"
                    val createdAt = rs.getString("tenant_created_at") ?: "-"

                    val line = String.format("%-32s | %-28s | %-32s | %s", tenantName.take(32), tenantId.take(28), email.take(32), createdAt)
                    auditLines.add(line)
                    println(line)
                }
            }

            // Check orphan users
            val orphanUsersQuery = "SELECT id, email, name, created_at FROM public.users WHERE tenant_id IS NULL OR tenant_id NOT IN (SELECT id FROM public.tenants)"
            c.createStatement().use { st ->
                val rs = st.executeQuery(orphanUsersQuery)
                var hasOrphans = false
                while (rs.next()) {
                    if (!hasOrphans) {
                        auditLines.add("\n--- USERS TANPA TENANT VALID ---")
                        hasOrphans = true
                    }
                    val orphanLine = "Orphan User: ID=${rs.getString("id")}, Email=${rs.getString("email")}, Name=${rs.getString("name")}, Created=${rs.getString("created_at")}"
                    auditLines.add(orphanLine)
                    println(orphanLine)
                }
            }

            val auditReportContent = auditLines.joinToString("\n")
            File(backupDirServer, "audit_tenants_and_users_report.txt").writeText(auditReportContent)
            File(backupDirRoot, "audit_tenants_and_users_report.txt").writeText(auditReportContent)
            println("\nFull audit report saved to: ${File(backupDirRoot, "audit_tenants_and_users_report.txt").absolutePath}")
            println("=================================================================")
        }
    }
}
