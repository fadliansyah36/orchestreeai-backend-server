package ai.orchestree.backend.util

import io.ktor.server.application.ApplicationCall
import kotlinx.serialization.Serializable

@Serializable
data class PagedResponse<T>(
    val items: List<T>,
    val total: Long,
    val limit: Int,
    val offset: Int
)

object PaginationDefaults {
    const val DEFAULT_LIMIT = 20
    const val MAX_LIMIT = 100

    fun parseLimit(call: ApplicationCall, default: Int = DEFAULT_LIMIT, max: Int = MAX_LIMIT): Int {
        val raw = call.request.queryParameters["limit"]?.toIntOrNull() ?: default
        return raw.coerceIn(1, max)
    }

    fun parseOffset(call: ApplicationCall): Int {
        val raw = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
        return raw.coerceAtLeast(0)
    }
}
