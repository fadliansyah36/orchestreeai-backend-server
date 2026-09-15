import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import java.util.Date;

public class GenToken {
    public static void main(String[] args) {
        String secret = System.getenv("JWT_SECRET_KEY");
        if (secret == null || secret.isEmpty()) {
            secret = "orchestreesecretjwtkey2026developmententerprise";
        }
        Algorithm algorithm = Algorithm.HMAC256(secret);
        String token = JWT.create()
            .withSubject("user-admin-01")
            .withClaim("tenant_id", "tenant-default")
            .withClaim("role", "SUPER_ADMIN")
            .withClaim("is_super_admin", true)
            .withExpiresAt(new Date(System.currentTimeMillis() + 86400000L * 30))
            .sign(algorithm);
        System.out.println(token);
    }
}
