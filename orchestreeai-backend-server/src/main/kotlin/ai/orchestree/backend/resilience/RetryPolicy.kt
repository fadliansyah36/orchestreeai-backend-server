package ai.orchestree.backend.resilience

import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.concurrent.TimeoutException
import kotlin.reflect.KClass

private val logger = LoggerFactory.getLogger("RetryPolicy")

/**
 * Standard unified retry mechanism with exponential backoff for external calls.
 */
suspend fun <T> executeWithRetry(
    maxAttempts: Int = 3,
    initialDelayMs: Long = 1000L,
    backoffMultiplier: Double = 2.0,
    retryableExceptions: Set<KClass<out Throwable>> = setOf(
        IOException::class,
        TimeoutException::class,
        java.net.SocketTimeoutException::class,
        java.net.ConnectException::class
    ),
    block: suspend () -> T
): T {
    var currentDelay = initialDelayMs
    repeat(maxAttempts - 1) { attempt ->
        try {
            return block()
        } catch (e: Throwable) {
            val isRetryable = e::class in retryableExceptions || retryableExceptions.any { it.java.isAssignableFrom(e.javaClass) }
            if (!isRetryable) {
                // non-retryable, langsung gagal
                throw e
            }
            logger.warn("Percobaan ${attempt + 1} gagal: ${e.message}, retry dalam ${currentDelay}ms")
            delay(currentDelay)
            currentDelay = (currentDelay * backoffMultiplier).toLong()
        }
    }
    return block() // percobaan terakhir, biarkan exception dilempar jika gagal
}
