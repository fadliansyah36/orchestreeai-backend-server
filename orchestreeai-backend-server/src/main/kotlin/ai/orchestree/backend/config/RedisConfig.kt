package ai.orchestree.backend.config

data class RedisConfig(
    val url: String
) {
    companion object {
        fun fromEnv(): RedisConfig = RedisConfig(
            url = System.getenv("REDIS_URL") ?: "redis://localhost:6379"
        )
    }
}
