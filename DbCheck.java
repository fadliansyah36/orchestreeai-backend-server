
import ai.orchestree.backend.billing.DatabaseManager;
import java.sql.Connection;
import java.sql.ResultSet;

public class DbCheck {
    public static void main(String[] args) {
        try {
            Connection conn = DatabaseManager.INSTANCE.getConnection();
            if (conn == null) {
                System.out.println("NO_DB_CONN");
                return;
            }
            String[] tables = {
                "subscription_plans", "feature_capabilities", "tenant_capability_overrides",
                "enterprise_system_connections", "enterprise_data_sync_jobs", "enterprise_ingested_records",
                "ai_data_permission_policies", "ai_data_access_requests"
            };
            for (String t : tables) {
                try {
                    ResultSet rs = conn.prepareStatement("SELECT count(*) FROM " + t).executeQuery();
                    if (rs.next()) {
                        System.out.println("TABLE " + t + ": EXISTS, rows=" + rs.getInt(1));
                    }
                } catch (Exception ex) {
                    System.out.println("TABLE " + t + ": DOES_NOT_EXIST (" + ex.getMessage() + ")");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
