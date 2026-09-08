package ai.orchestree.backend.database

import ai.orchestree.backend.config.RedisConfig
import org.slf4j.LoggerFactory
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class RedisService(
    private val config: RedisConfig = RedisConfig.fromEnv()
) {
    private val logger = LoggerFactory.getLogger(RedisService::class.java)
    private var jedisPool: JedisPool? = null

    // In-memory fallback stores for offline/test environments
    private val memoryRevokedTokens = ConcurrentHashMap<String, Long>()
    private val memorySemanticCache = ConcurrentHashMap<String, Pair<String, Long>>()
    private val memoryRateLimits = ConcurrentHashMap<String, Pair<AtomicInteger, Long>>()

    init {
        try {
            if (config.url.isNotBlank()) {
                val uri = URI(config.url)
                val poolConfig = JedisPoolConfig().apply {
                    maxTotal = 32
                    maxIdle = 8
                    minIdle = 2
                    testOnBorrow = true
                }
                jedisPool = JedisPool(poolConfig, uri)
                logger.info("Initialized Redis connection pool for ${uri.host}:${if (uri.port > 0) uri.port else 6379}")
            }
        } catch (e: Exception) {
            logger.warn("Could not initialize real Redis connection pool (${e.message}). Falling back to in-memory store for dev/testing.")
            jedisPool = null
        }
    }

    /**
     * LANGKAH 2: Refresh Token Revocation List (Fase 26.1)
     */
    fun revokeToken(jti: String, ttlSeconds: Long = 86400 * 7): Boolean {
        return try {
            jedisPool?.resource?.use { jedis ->
                jedis.setex("revoked_token:$jti", ttlSeconds, "revoked")
                true
            } ?: run {
                memoryRevokedTokens[jti] = System.currentTimeMillis() + (ttlSeconds * 1000)
                true
            }
        } catch (e: Exception) {
            logger.error("Error revoking token $jti in Redis: ${e.message}", e)
            memoryRevokedTokens[jti] = System.currentTimeMillis() + (ttlSeconds * 1000)
            true
        }
    }

    fun isTokenRevoked(jti: String): Boolean {
        return try {
            jedisPool?.resource?.use { jedis ->
                jedis.exists("revoked_token:$jti")
            } ?: run {
                val expiry = memoryRevokedTokens[jti] ?: return false
                if (System.currentTimeMillis() > expiry) {
                    memoryRevokedTokens.remove(jti)
                    false
                } else {
                    true
                }
            }
        } catch (e: Exception) {
            logger.error("Error checking token revocation for $jti: ${e.message}", e)
            val expiry = memoryRevokedTokens[jti] ?: return false
            System.currentTimeMillis() <= expiry
        }
    }

    /**
     * LANGKAH 2: Semantic Response Cache (Fase 66)
     */
    fun getSemanticCache(cacheKey: String): String? {
        return try {
            jedisPool?.resource?.use { jedis ->
                jedis.get("llm_cache:$cacheKey")
            } ?: run {
                val entry = memorySemanticCache[cacheKey] ?: return null
                if (System.currentTimeMillis() > entry.second) {
                    memorySemanticCache.remove(cacheKey)
                    null
                } else {
                    entry.first
                }
            }
        } catch (e: Exception) {
            logger.error("Error reading semantic cache from Redis: ${e.message}", e)
            memorySemanticCache[cacheKey]?.first
        }
    }

    fun setSemanticCache(cacheKey: String, response: String, ttlSeconds: Long = 3600 * 12): Boolean {
        return try {
            jedisPool?.resource?.use { jedis ->
                jedis.setex("llm_cache:$cacheKey", ttlSeconds, response)
                true
            } ?: run {
                memorySemanticCache[cacheKey] = response to (System.currentTimeMillis() + (ttlSeconds * 1000))
                true
            }
        } catch (e: Exception) {
            logger.error("Error writing semantic cache to Redis: ${e.message}", e)
            memorySemanticCache[cacheKey] = response to (System.currentTimeMillis() + (ttlSeconds * 1000))
            true
        }
    }

    /**
     * LANGKAH 2: Rate Limiting Counter (Fase 65)
     */
    fun checkAndIncrementRateLimit(key: String, maxRequests: Int = 120, windowSeconds: Long = 60): Boolean {
        val redisKey = "ratelimit:$key"
        return try {
            jedisPool?.resource?.use { jedis ->
                val current = jedis.incr(redisKey)
                if (current == 1L) {
                    jedis.expire(redisKey, windowSeconds)
                }
                current <= maxRequests
            } ?: run {
                val now = System.currentTimeMillis()
                val windowMs = windowSeconds * 1000
                val entry = memoryRateLimits.getOrPut(key) { AtomicInteger(0) to (now + windowMs) }

                if (now > entry.second) {
                    memoryRateLimits[key] = AtomicInteger(1) to (now + windowMs)
                    true
                } else {
                    val count = entry.first.incrementAndGet()
                    count <= maxRequests
                }
            }
        } catch (e: Exception) {
            logger.error("Error executing rate limit in Redis: ${e.message}", e)
            true
        }
    }

    fun close() {
        try {
            jedisPool?.close()
        } catch (e: Exception) {
            logger.warn("Error closing Redis pool: ${e.message}")
        }
    }
}
