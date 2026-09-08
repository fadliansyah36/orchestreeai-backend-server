package ai.orchestree.backend.config

data class SupabaseConfig(
    val url: String,
    val anonKey: String,
    val serviceRoleKey: String,
    val databaseUrl: String,
    val databaseDirectUrl: String
) {
    companion object {
        fun fromEnv(): SupabaseConfig = SupabaseConfig(
            url = EnvLoader.get("SUPABASE_URL"),
            anonKey = EnvLoader.get("SUPABASE_ANON_KEY"),
            serviceRoleKey = EnvLoader.get("SUPABASE_SERVICE_ROLE_KEY"),
            databaseUrl = EnvLoader.get("DATABASE_URL"),
            databaseDirectUrl = EnvLoader.get("DATABASE_DIRECT_URL")
        )
    }
}
