package ai.orchestree.backend.competitor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

interface IngestionAdapter {
    val adapterName: String
    fun canHandle(url: String, category: String): Boolean
    suspend fun ingest(url: String): IngestionResult
}

object IngestionResilienceManager {
    private val domainFailureCount = ConcurrentHashMap<String, AtomicInteger>()
    private val domainLastRequestTime = ConcurrentHashMap<String, Long>()
    private const val MAX_CONSECUTIVE_FAILURES = 3
    private const val DOMAIN_RATE_LIMIT_MS = 2000L

    fun extractDomain(url: String): String {
        return try {
            val uri = java.net.URI(url)
            uri.host ?: url
        } catch (e: Exception) {
            url
        }
    }

    fun isDomainUnreachable(domain: String): Boolean {
        return (domainFailureCount[domain]?.get() ?: 0) >= MAX_CONSECUTIVE_FAILURES
    }

    fun recordSuccess(domain: String) {
        domainFailureCount[domain]?.set(0)
    }

    fun recordFailure(domain: String) {
        domainFailureCount.computeIfAbsent(domain) { AtomicInteger(0) }.incrementAndGet()
    }

    suspend fun enforceRateLimit(domain: String) {
        val last = domainLastRequestTime[domain] ?: 0L
        val now = System.currentTimeMillis()
        val elapsed = now - last
        if (elapsed < DOMAIN_RATE_LIMIT_MS) {
            kotlinx.coroutines.delay(DOMAIN_RATE_LIMIT_MS - elapsed)
        }
        domainLastRequestTime[domain] = System.currentTimeMillis()
    }
}

class WebIngestionAdapter(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) : IngestionAdapter {
    override val adapterName: String = "WebIngestionAdapter"
    private val logger = LoggerFactory.getLogger(WebIngestionAdapter::class.java)

    override fun canHandle(url: String, category: String): Boolean = true

    override suspend fun ingest(url: String): IngestionResult = withContext(Dispatchers.IO) {
        val domain = IngestionResilienceManager.extractDomain(url)
        if (IngestionResilienceManager.isDomainUnreachable(domain)) {
            return@withContext IngestionResult.Failure(
                reason = "Circuit breaker aktif: Domain $domain mengalami kegagalan berturut-turut.",
                requiresManualReview = true
            )
        }

        IngestionResilienceManager.enforceRateLimit(domain)

        val startTime = System.currentTimeMillis()
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (compatible; OrchestreeAIBot/1.0; +https://orchestree.ai/bot)")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .build()

            val response = client.newCall(request).execute()
            val elapsed = System.currentTimeMillis() - startTime
            val statusCode = response.code
            val bodyString = response.body?.string() ?: ""

            if (statusCode in 401..403) {
                IngestionResilienceManager.recordFailure(domain)
                return@withContext IngestionResult.Failure(
                    reason = "Akses diblokir oleh target (HTTP $statusCode WAF/Forbidden).",
                    statusCode = statusCode,
                    isBlockedOrCompliance = true,
                    requiresManualReview = true
                )
            }

            if (!response.isSuccessful) {
                IngestionResilienceManager.recordFailure(domain)
                return@withContext IngestionResult.Failure(
                    reason = "HTTP Request gagal dengan status: $statusCode",
                    statusCode = statusCode
                )
            }

            IngestionResilienceManager.recordSuccess(domain)

            val rawHtmlHash = MessageDigest.getInstance("SHA-256")
                .digest(bodyString.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }

            val titleMatch = Regex("<title>(.*?)</title>", RegexOption.IGNORE_CASE).find(bodyString)
            val title = titleMatch?.groupValues?.get(1)?.trim() ?: ""

            val descMatch = Regex("<meta\\s+name=[\"']description[\"']\\s+content=[\"'](.*?)[\"']", RegexOption.IGNORE_CASE).find(bodyString)
            val metaDesc = descMatch?.groupValues?.get(1)?.trim() ?: ""

            // Simple text stripper
            val cleanText = bodyString
                .replace(Regex("<script[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), " ")
                .replace(Regex("<style[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), " ")
                .replace(Regex("<[^>]+>"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
                .take(4000)

            IngestionResult.Success(
                rawHtmlHash = rawHtmlHash,
                parsedContent = cleanText,
                httpStatusCode = statusCode,
                responseTimeMs = elapsed,
                title = title,
                metaDescription = metaDesc,
                adapterUsed = adapterName
            )
        } catch (e: Exception) {
            IngestionResilienceManager.recordFailure(domain)
            logger.warn("Web ingestion failed for $url: ${e.message}")
            IngestionResult.Failure(
                reason = "Koneksi terputus: ${e.message}",
                statusCode = 0
            )
        }
    }
}
