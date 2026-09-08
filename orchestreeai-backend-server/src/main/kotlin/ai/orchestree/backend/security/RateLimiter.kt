package ai.orchestree.backend.security

import ai.orchestree.backend.database.RedisService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.plugins.origin
import io.ktor.server.request.header
import io.ktor.server.request.path
import io.ktor.server.response.header
import io.ktor.server.response.respond
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Endpoint Categories with dedicated rate limits (PRD Fase 123 Bagian B)
 */
enum class RateLimitCategory(val maxRequestsPerMinute: Int, val windowSeconds: Int = 60) {
    AUTH(maxRequestsPerMinute = 5, windowSeconds = 60),
    CHAT_AI(maxRequestsPerMinute = 30, windowSeconds = 60),
    GENERATIVE_STUDIO(maxRequestsPerMinute = 10, windowSeconds = 60),
    WEBHOOK_PAYMENT(maxRequestsPerMinute = Int.MAX_VALUE, windowSeconds = 60),
    DEFAULT(maxRequestsPerMinute = 60, windowSeconds = 60)
}

data class RateLimitDecision(
    val isAllowed: Boolean,
    val currentCount: Long,
    val limit: Int,
    val retryAfterSeconds: Int
)

class RateLimiter(
    private val redisService: RedisService = RedisService(),
    private val defaultMaxRequestsPerMinute: Int = 120
) {
    private val logger = LoggerFactory.getLogger(RateLimiter::class.java)

    // In-memory fallback tracking when Redis is unavailable
    private val memoryCounters = ConcurrentHashMap<String, AtomicInteger>()
    private val memoryWindows = ConcurrentHashMap<String, Long>()

    /**
     * Sliding window rate limiting per user_id and per IP.
     * key: ratelimit:$userId:$endpoint or ratelimit:ip:$clientIp:$endpoint
     */
    fun checkRateLimit(
        identifier: String,
        endpoint: String,
        maxRequests: Int = defaultMaxRequestsPerMinute,
        windowSeconds: Int = 60
    ): RateLimitDecision {
        val key = "$identifier:$endpoint"
        val now = System.currentTimeMillis()

        // 1. Try Redis
        try {
            val allowed = redisService.checkAndIncrementRateLimit(key, maxRequests, windowSeconds.toLong())
            if (!allowed) {
                logger.warn("Rate limit exceeded for [$key]: limit $maxRequests exceeded (retry in ${windowSeconds}s)")
            }
            return RateLimitDecision(
                isAllowed = allowed,
                currentCount = if (allowed) 1L else (maxRequests + 1L),
                limit = maxRequests,
                retryAfterSeconds = if (allowed) 0 else windowSeconds
            )
        } catch (e: Exception) {
            logger.warn("Redis rate limit check failed, using local memory fallback: ${e.message}")
        }

        // 2. Local memory fallback
        val windowStart = memoryWindows.getOrPut(key) { now }
        if (now - windowStart > windowSeconds * 1000L) {
            memoryWindows[key] = now
            memoryCounters[key] = AtomicInteger(1)
            return RateLimitDecision(isAllowed = true, currentCount = 1L, limit = maxRequests, retryAfterSeconds = 0)
        }

        val count = memoryCounters.getOrPut(key) { AtomicInteger(0) }.incrementAndGet().toLong()
        val elapsedSec = ((now - windowStart) / 1000L).coerceAtLeast(0L).toInt()
        val retryAfter = (windowSeconds - elapsedSec).coerceAtLeast(1)
        val allowed = count <= maxRequests

        if (!allowed) {
            logger.warn("Memory fallback rate limit exceeded for [$key]: $count > $maxRequests (retry in ${retryAfter}s)")
        }

        return RateLimitDecision(
            isAllowed = allowed,
            currentCount = count,
            limit = maxRequests,
            retryAfterSeconds = if (allowed) 0 else retryAfter
        )
    }

    /**
     * Backward-compatible simple limit check
     */
    fun checkLimit(key: String, limit: Int = defaultMaxRequestsPerMinute): Boolean {
        return checkRateLimit(identifier = key, endpoint = "default", maxRequests = limit).isAllowed
    }
}

/**
 * Resolve endpoint category for rate limiting
 */
fun resolveRateLimitCategory(path: String): RateLimitCategory {
    return when {
        path.startsWith("/api/v1/auth") -> RateLimitCategory.AUTH
        path.startsWith("/api/v1/generativestudio") || path.startsWith("/api/v1/generative-studio") -> RateLimitCategory.GENERATIVE_STUDIO
        path.startsWith("/api/v1/chat") || path.startsWith("/api/v1/orchestration") -> RateLimitCategory.CHAT_AI
        path.startsWith("/api/v1/payments/webhook") || path.startsWith("/api/v1/webhooks") -> RateLimitCategory.WEBHOOK_PAYMENT
        else -> RateLimitCategory.DEFAULT
    }
}

val ServerRateLimitingPlugin = createApplicationPlugin(name = "ServerRateLimitingPlugin") {
    val rateLimiter = RateLimiter()
    val logger = LoggerFactory.getLogger("ai.orchestree.backend.security.ServerRateLimitingPlugin")

    onCall { call ->
        val path = call.request.path()
        // Skip health checks, root, and swagger
        if (path == "/health" || path == "/api/health" || path == "/") return@onCall

        val category = resolveRateLimitCategory(path)
        if (category == RateLimitCategory.WEBHOOK_PAYMENT) {
            // Webhooks are not rate limited, but signature verified
            return@onCall
        }

        // Identify client by user_id from token header/auth, or client IP
        val userId = call.request.header("X-User-Id") ?: call.request.header("X-Tenant-Id")
        val clientIp = call.request.header("X-Forwarded-For")?.split(",")?.firstOrNull()?.trim()
            ?: call.request.origin.remoteHost

        val identifier = if (!userId.isNullOrBlank()) "user:$userId" else "ip:$clientIp"

        val decision = rateLimiter.checkRateLimit(
            identifier = identifier,
            endpoint = category.name.lowercase(),
            maxRequests = category.maxRequestsPerMinute,
            windowSeconds = category.windowSeconds
        )

        if (!decision.isAllowed) {
            call.response.header("Retry-After", decision.retryAfterSeconds.toString())
            call.response.header("X-RateLimit-Limit", decision.limit.toString())
            call.response.header("X-RateLimit-Remaining", "0")
            call.respond(
                HttpStatusCode.TooManyRequests,
                mapOf(
                    "status" to "error",
                    "error" to "Rate limit exceeded for category ${category.name}",
                    "retryAfter" to decision.retryAfterSeconds,
                    "limit" to decision.limit
                )
            )
        } else {
            val remaining = (decision.limit - decision.currentCount).coerceAtLeast(0L)
            call.response.header("X-RateLimit-Limit", decision.limit.toString())
            call.response.header("X-RateLimit-Remaining", remaining.toString())
        }
    }
}

fun Application.configureServerRateLimiting() {
    install(ServerRateLimitingPlugin)
}

enum class ApprovalStatus { PENDING, APPROVED, REJECTED }

data class DualControlRequest(
    val requestId: String,
    val tenantId: String,
    val initiatorUserId: String,
    val actionType: String,
    val payloadJson: String,
    val approverUserId: String? = null,
    val status: ApprovalStatus = ApprovalStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis()
)

class DualControlEngine {
    private val logger = LoggerFactory.getLogger(DualControlEngine::class.java)
    private val pendingRequests = ConcurrentHashMap<String, DualControlRequest>()

    fun submitRequest(tenantId: String, initiatorUserId: String, actionType: String, payloadJson: String): DualControlRequest {
        val req = DualControlRequest(
            requestId = "dc-${java.util.UUID.randomUUID().toString().take(8)}",
            tenantId = tenantId,
            initiatorUserId = initiatorUserId,
            actionType = actionType,
            payloadJson = payloadJson
        )
        pendingRequests[req.requestId] = req
        logger.info("Submitted DualControl request ${req.requestId} for action $actionType by user $initiatorUserId")
        return req
    }

    fun approveRequest(requestId: String, approverUserId: String): Pair<Boolean, String?> {
        val req = pendingRequests[requestId] ?: return false to "Request $requestId not found"
        if (req.initiatorUserId == approverUserId) {
            val msg = "DualControl violation: Approver cannot be the same as Initiator ($approverUserId)"
            logger.warn(msg)
            return false to msg
        }
        val approved = req.copy(approverUserId = approverUserId, status = ApprovalStatus.APPROVED)
        pendingRequests[requestId] = approved
        logger.info("DualControl request $requestId APPROVED by $approverUserId")
        return true to null
    }
}
