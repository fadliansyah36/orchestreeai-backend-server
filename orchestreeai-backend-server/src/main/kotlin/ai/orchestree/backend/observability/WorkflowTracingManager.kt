package ai.orchestree.backend.observability

import ai.orchestree.backend.database.SupabaseClientProvider
import ai.orchestree.backend.orchestration.toJson
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@Serializable
data class WorkflowSpanRecord(
    val spanId: String,
    val traceId: String,
    val executionId: String,
    val tenantId: String,
    val nodeId: String,
    val nodeType: String,
    val status: String,
    val durationMs: Long,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val attributes: Map<String, String> = emptyMap(),
    val errorMessage: String? = null
)

@Serializable
data class ExecutionTraceSummary(
    val executionId: String,
    val traceId: String,
    val tenantId: String,
    val nodeCount: Int,
    val totalDurationMs: Long,
    val status: String,
    val startedAt: Long
)

/**
 * Extension function providing .use { span -> ... } pattern for OpenTelemetry Span.
 * Ensures span is made current, executed within scope, and safely closed and ended.
 */
inline fun <T> Span.use(block: (Span) -> T): T {
    val scope = this.makeCurrent()
    return try {
        block(this)
    } finally {
        scope.close()
        this.end()
    }
}

/**
 * Centralized OpenTelemetry Tracing Manager for Workflow Engine (PRD Section 27 & LANGKAH 1).
 * Wraps workflow nodes, assigns trace_id per execution, captures span per node, and exports to Jaeger/Tempo OTLP.
 */
object WorkflowTracingManager {
    private val logger = LoggerFactory.getLogger(WorkflowTracingManager::class.java)
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val supabase = SupabaseClientProvider.fromEnv()

    // Queryable in-memory storage for UI inspection
    private val spansByExecution = ConcurrentHashMap<String, MutableList<WorkflowSpanRecord>>()
    private val spansByTrace = ConcurrentHashMap<String, MutableList<WorkflowSpanRecord>>()
    private val executionSummaries = ConcurrentHashMap<String, ExecutionTraceSummary>()

    val openTelemetry: OpenTelemetry
    val tracer: Tracer

    init {
        openTelemetry = initializeOpenTelemetry()
        tracer = openTelemetry.getTracer("orchestree-workflow-engine", "1.0.0")
    }

    private fun initializeOpenTelemetry(): OpenTelemetry {
        return try {
            val otlpEndpoint = System.getenv("OTEL_EXPORTER_OTLP_ENDPOINT")
                ?: System.getenv("OTEL_EXPORTER_OTLP_TRACES_ENDPOINT")
                ?: "http://localhost:4317"

            logger.info("[OPENTELEMETRY] Initializing Workflow Tracer with OTLP endpoint: $otlpEndpoint")

            val tracerProviderBuilder = SdkTracerProvider.builder()
                .setResource(Resource.getDefault().toBuilder().put("service.name", "orchestree-backend-server").build())

            // Attempt setting up OTLP exporter if endpoint is active
            try {
                val exporter = OtlpGrpcSpanExporter.builder()
                    .setEndpoint(otlpEndpoint)
                    .build()
                tracerProviderBuilder.addSpanProcessor(SimpleSpanProcessor.create(exporter))
                logger.info("[OPENTELEMETRY] OTLP Exporter configured for Jaeger/Grafana Tempo: $otlpEndpoint")
            } catch (e: Exception) {
                logger.warn("[OPENTELEMETRY] OtlpGrpcSpanExporter setup notice: ${e.message} (In-memory trace store active)")
            }

            OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProviderBuilder.build())
                .build()
        } catch (e: Exception) {
            logger.warn("[OPENTELEMETRY] Falling back to GlobalOpenTelemetry: ${e.message}")
            GlobalOpenTelemetry.get()
        }
    }

    fun recordSpan(
        spanId: String,
        traceId: String,
        executionId: String,
        tenantId: String,
        nodeId: String,
        nodeType: String,
        status: String,
        durationMs: Long,
        startTimeMs: Long,
        endTimeMs: Long,
        attributes: Map<String, String> = emptyMap(),
        errorMessage: String? = null
    ) {
        val record = WorkflowSpanRecord(
            spanId = spanId,
            traceId = traceId,
            executionId = executionId,
            tenantId = tenantId,
            nodeId = nodeId,
            nodeType = nodeType,
            status = status,
            durationMs = durationMs,
            startTimeMs = startTimeMs,
            endTimeMs = endTimeMs,
            attributes = attributes,
            errorMessage = errorMessage
        )

        spansByExecution.computeIfAbsent(executionId) { CopyOnWriteArrayList() }.add(record)
        spansByTrace.computeIfAbsent(traceId) { CopyOnWriteArrayList() }.add(record)

        // Update execution summary
        val existingSpans = spansByExecution[executionId] ?: listOf(record)
        val totalDuration = existingSpans.sumOf { it.durationMs }
        val overallStatus = if (existingSpans.any { it.status == "FAILED" }) "FAILED"
            else if (existingSpans.any { it.status == "NEEDS_HUMAN" }) "PAUSED_FOR_APPROVAL"
            else "COMPLETED"
        val firstStart = existingSpans.minOfOrNull { it.startTimeMs } ?: startTimeMs

        executionSummaries[executionId] = ExecutionTraceSummary(
            executionId = executionId,
            traceId = traceId,
            tenantId = tenantId,
            nodeCount = existingSpans.size,
            totalDurationMs = totalDuration,
            status = overallStatus,
            startedAt = firstStart
        )

        // Async persist to Supabase if configured
        if (supabase.isConfigured()) {
            backgroundScope.launch {
                try {
                    val payload = mapOf(
                        "span_id" to record.spanId,
                        "trace_id" to record.traceId,
                        "execution_id" to record.executionId,
                        "tenant_id" to record.tenantId,
                        "node_id" to record.nodeId,
                        "node_type" to record.nodeType,
                        "status" to record.status,
                        "start_time_ms" to record.startTimeMs,
                        "end_time_ms" to record.endTimeMs,
                        "duration_ms" to record.durationMs,
                        "error_message" to (record.errorMessage ?: ""),
                        "attributes_json" to record.attributes.toJson(),
                        "created_at" to System.currentTimeMillis()
                    )
                    supabase.insertRecord("workflow_node_spans", record.tenantId, payload.toJson())
                } catch (e: Exception) {
                    logger.debug("Supabase span record notice: ${e.message}")
                }
            }
        }
    }

    fun getSpansForExecution(executionId: String): List<WorkflowSpanRecord> {
        return spansByExecution[executionId]?.sortedBy { it.startTimeMs } ?: emptyList()
    }

    fun getTraceIdForExecution(executionId: String): String? {
        return spansByExecution[executionId]?.firstOrNull()?.traceId
            ?: executionSummaries[executionId]?.traceId
    }

    fun getSpansForTrace(traceId: String): List<WorkflowSpanRecord> {
        return spansByTrace[traceId]?.sortedBy { it.startTimeMs } ?: emptyList()
    }

    fun getAllExecutionSummaries(): List<ExecutionTraceSummary> {
        return executionSummaries.values.sortedByDescending { it.startedAt }
    }

    fun clear() {
        spansByExecution.clear()
        spansByTrace.clear()
        executionSummaries.clear()
    }
}
