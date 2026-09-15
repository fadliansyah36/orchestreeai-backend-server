import ai.orchestree.backend.billing.DatabaseManager;
import java.sql.Connection;
import java.sql.ResultSet;

public class DbCheck {
    public static void main(String[] args) {
        try {
            Connection conn = DatabaseManager.INSTANCE.getConnection();
            if (conn == null) return;
            ResultSet rs = conn.prepareStatement("SELECT id, name, status FROM tenants LIMIT 10").executeQuery();
            System.out.println("--- TENANTS IN DB ---");
            while (rs.next()) {
                System.out.println("  " + rs.getString("id") + " | " + rs.getString("name") + " | " + rs.getString("status"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
