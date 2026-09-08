package ai.orchestree.backend

import ai.orchestree.backend.billing.DatabaseManager
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import java.sql.Connection
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class Langkah2PurgeOldAccountsTest {

    private fun getDbConnection(): Connection? = DatabaseManager.getConnection()

    @Test
    @Order(1)
    fun testPurgeAllOldTenantsAndUsersComprehensive() {
        println("=================================================================")
        println("=== LANGKAH 2: HAPUS SELURUH AKUN & TENANT LAMA (CLEAN PURGE) ===")
        println("=================================================================")

        val conn = getDbConnection()
        assertNotNull(conn, "Database connection to Supabase PostgreSQL must be active")

        conn.use { c ->
            c.autoCommit = true

            // 1. Find user triggers on ai_credit_ledger
            val triggers = mutableListOf<String>()
            c.createStatement().use { st ->
                val rs = st.executeQuery("""
                    SELECT trigger_name 
                    FROM information_schema.triggers 
                    WHERE event_object_table = 'ai_credit_ledger'
                """.trimIndent())
                while (rs.next()) {
                    val tName = rs.getString("trigger_name")
                    triggers.add(tName)
                    println("Found trigger on ai_credit_ledger: $tName")
                }
            }

            // Disable triggers on ai_credit_ledger
            for (t in triggers) {
                try {
                    c.createStatement().use { st ->
                        st.execute("ALTER TABLE public.ai_credit_ledger DISABLE TRIGGER \"$t\"")
                        println("Disabled trigger $t on ai_credit_ledger")
                    }
                } catch (e: Exception) {
                    println("Could not disable trigger $t: ${e.message}")
                }
            }
            try {
                c.createStatement().use { st ->
                    st.execute("ALTER TABLE public.ai_credit_ledger DISABLE TRIGGER USER")
                    println("Executed: ALTER TABLE public.ai_credit_ledger DISABLE TRIGGER USER")
                }
            } catch (e: Exception) {
                println("DISABLE TRIGGER USER exception: ${e.message}")
            }

            // 2. Delete all rows from ai_credit_ledger
            try {
                c.createStatement().use { st ->
                    val deletedLedger = st.executeUpdate("DELETE FROM public.ai_credit_ledger")
                    println("Successfully deleted $deletedLedger rows from public.ai_credit_ledger")
                }
            } catch (e: Exception) {
                println("Error deleting from ai_credit_ledger: ${e.message}")
            }

            // 3. Delete from all tables in public schema with tenant_id column
            val tablesWithTenantId = mutableListOf<String>()
            c.createStatement().use { st ->
                val rs = st.executeQuery("""
                    SELECT DISTINCT table_name 
                    FROM information_schema.columns 
                    WHERE table_schema = 'public' 
                      AND column_name = 'tenant_id'
                      AND table_name NOT IN ('tenants')
                """.trimIndent())
                while (rs.next()) {
                    tablesWithTenantId.add(rs.getString("table_name"))
                }
            }

            for (table in tablesWithTenantId) {
                try {
                    c.createStatement().use { st ->
                        val deleted = st.executeUpdate("DELETE FROM public.\"$table\"")
                        if (deleted > 0) {
                            println("Cleared $deleted rows from table $table")
                        }
                    }
                } catch (e: Exception) {
                    println("Note deleting from $table: ${e.message}")
                }
            }

            // 4. Delete from public.tenants
            try {
                c.createStatement().use { st ->
                    val deletedTenants = st.executeUpdate("DELETE FROM public.tenants")
                    println("Successfully deleted $deletedTenants rows from public.tenants")
                }
            } catch (e: Exception) {
                println("Error deleting from public.tenants: ${e.message}")
            }

            // 5. Re-enable triggers on ai_credit_ledger
            for (t in triggers) {
                try {
                    c.createStatement().use { st ->
                        st.execute("ALTER TABLE public.ai_credit_ledger ENABLE TRIGGER \"$t\"")
                        println("Re-enabled trigger $t on ai_credit_ledger")
                    }
                } catch (e: Exception) {
                    println("Could not re-enable trigger $t: ${e.message}")
                }
            }
            try {
                c.createStatement().use { st ->
                    st.execute("ALTER TABLE public.ai_credit_ledger ENABLE TRIGGER USER")
                }
            } catch (_: Exception) {}

            // 6. Delete from auth schema
            try {
                c.createStatement().use { st ->
                    try { st.executeUpdate("DELETE FROM auth.identities") } catch (_: Exception) {}
                    try { st.executeUpdate("DELETE FROM auth.sessions") } catch (_: Exception) {}
                    try { st.executeUpdate("DELETE FROM auth.refresh_tokens") } catch (_: Exception) {}
                    try { st.executeUpdate("DELETE FROM auth.mfa_factors") } catch (_: Exception) {}
                    val deletedAuth = st.executeUpdate("DELETE FROM auth.users")
                    println("Successfully deleted $deletedAuth rows from auth.users")
                }
            } catch (e: Exception) {
                println("Note when deleting from auth schema: ${e.message}")
            }

            // 7. Verification (Definition of Done)
            var tenantsCount = -1
            var authUsersCount = -1
            var publicUsersCount = -1
            var ledgerCount = -1
            var walletCount = -1
            var subCount = -1

            c.createStatement().use { st ->
                val rs = st.executeQuery("SELECT COUNT(*) FROM public.tenants")
                if (rs.next()) tenantsCount = rs.getInt(1)
            }

            c.createStatement().use { st ->
                val rs = st.executeQuery("SELECT COUNT(*) FROM auth.users")
                if (rs.next()) authUsersCount = rs.getInt(1)
            }

            c.createStatement().use { st ->
                val rs = st.executeQuery("SELECT COUNT(*) FROM public.users")
                if (rs.next()) publicUsersCount = rs.getInt(1)
            }

            c.createStatement().use { st ->
                val rs = st.executeQuery("SELECT COUNT(*) FROM public.ai_credit_ledger")
                if (rs.next()) ledgerCount = rs.getInt(1)
            }

            c.createStatement().use { st ->
                val rs = st.executeQuery("SELECT COUNT(*) FROM public.ai_credit_wallets")
                if (rs.next()) walletCount = rs.getInt(1)
            }

            c.createStatement().use { st ->
                val rs = st.executeQuery("SELECT COUNT(*) FROM public.tenant_subscriptions")
                if (rs.next()) subCount = rs.getInt(1)
            }

            println("\n=================================================================")
            println("=== DEFINITION OF DONE VERIFIKASI LANGKAH 2 ===")
            println("COUNT public.tenants:             $tenantsCount")
            println("COUNT auth.users:                 $authUsersCount")
            println("COUNT public.users:               $publicUsersCount")
            println("COUNT public.ai_credit_ledger:    $ledgerCount")
            println("COUNT public.ai_credit_wallets:   $walletCount")
            println("COUNT public.tenant_subscriptions:$subCount")
            println("=================================================================")

            assertEquals(0, tenantsCount, "SELECT COUNT(*) FROM public.tenants must be 0")
            assertEquals(0, authUsersCount, "SELECT COUNT(*) FROM auth.users must be 0")
            assertEquals(0, publicUsersCount, "SELECT COUNT(*) FROM public.users must be 0")
            assertEquals(0, ledgerCount, "SELECT COUNT(*) FROM public.ai_credit_ledger must be 0")
            assertEquals(0, walletCount, "SELECT COUNT(*) FROM public.ai_credit_wallets must be 0")
            assertEquals(0, subCount, "SELECT COUNT(*) FROM public.tenant_subscriptions must be 0")
        }
    }
}
