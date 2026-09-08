package ai.orchestree.backend.config

data class AppConfig(
    val environment: String = System.getenv("APPLICATION_ENV") ?: "development",
    val port: Int = System.getenv("BACKEND_PORT")?.toIntOrNull()
        ?: System.getenv("PORT")?.toIntOrNull()?.takeIf { it != 8080 }
        ?: 8082,
    val supabase: SupabaseConfig = SupabaseConfig.fromEnv(),
    val redis: RedisConfig = RedisConfig.fromEnv(),
    val security: SecurityConfig = SecurityConfig.fromEnv()
) {
    companion object {
        fun hasGeminiApiKeyConfigured(): Boolean {
            val key = ai.orchestree.backend.config.EnvLoader.get("GEMINI_API_KEY")
            return key.isNotBlank() && key != "placeholder"
        }

        fun load(): AppConfig {
            val config = AppConfig()
            config.validate()
            return config
        }
    }

    fun validate() {
        if (environment == "production") {
            check(supabase.url.isNotBlank()) { "SUPABASE_URL must not be empty in production" }
            check(supabase.serviceRoleKey.isNotBlank()) { "SUPABASE_SERVICE_ROLE_KEY must not be empty in production" }
            check(security.jwtSecretKey.isNotBlank()) { "JWT_SECRET_KEY must not be empty in production" }
        }
    }
}
