package ai.orchestree.backend.config

data class SecurityConfig(
    val jwtSecretKey: String,
    val masterKeyRef: String
) {
    companion object {
        fun fromEnv(): SecurityConfig = SecurityConfig(
            jwtSecretKey = EnvLoader.get("JWT_SECRET_KEY", "default_jwt_secret_key_for_dev"),
            masterKeyRef = EnvLoader.get("ENCRYPTION_MASTER_KEY_REF", "default_master_key_ref")
        )
    }
}
