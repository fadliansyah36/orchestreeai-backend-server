package ai.orchestree.backend.database.repositories.workforce

import ai.orchestree.backend.api.*
import ai.orchestree.backend.billing.DatabaseManager
import org.slf4j.LoggerFactory
import java.util.UUID

object TenantDomainRepository {
    private val logger = LoggerFactory.getLogger(TenantDomainRepository::class.java)

    fun getDashboardOverview(tenantId: String): TenantDashboardOverviewResponse {
        val conn = DatabaseManager.getConnection() ?: return TenantDashboardOverviewResponse(
            tenantId = tenantId,
            activeAgents = 0,
            humanStaffCount = 0,
            activeTasks = 0,
            completionRate = 0.0,
            status = "HEALTHY"
        )

        return conn.use { c ->
            var activeAgents = 0
            var humanStaffCount = 0
            var activeTasks = 0
            var doneTasks = 0

            try {
                c.prepareStatement("SELECT count(*) FROM ai_agents WHERE tenant_id = ? OR tenant_id = 'tenant-default'").use { ps ->
                    ps.setString(1, tenantId)
                    ps.executeQuery().use { rs ->
                        if (rs.next()) activeAgents = rs.getInt(1)
                    }
                }
            } catch (e: Exception) {
                logger.warn("Error querying ai_agents count: ${e.message}")
            }

            try {
                c.prepareStatement("SELECT count(*) FROM users WHERE (tenant_id = ? OR tenant_id = 'tenant-default') AND is_active = true").use { ps ->
                    ps.setString(1, tenantId)
                    ps.executeQuery().use { rs ->
                        if (rs.next()) humanStaffCount = rs.getInt(1)
                    }
                }
            } catch (e: Exception) {
                logger.warn("Error querying users count: ${e.message}")
            }

            try {
                c.prepareStatement("SELECT count(*) FROM tasks WHERE tenant_id = ? OR tenant_id = 'tenant-default'").use { ps ->
                    ps.setString(1, tenantId)
                    ps.executeQuery().use { rs ->
                        if (rs.next()) activeTasks = rs.getInt(1)
                    }
                }
                c.prepareStatement("SELECT count(*) FROM tasks WHERE (tenant_id = ? OR tenant_id = 'tenant-default') AND column_name = 'DONE'").use { ps ->
                    ps.setString(1, tenantId)
                    ps.executeQuery().use { rs ->
                        if (rs.next()) doneTasks = rs.getInt(1)
                    }
                }
            } catch (e: Exception) {
                logger.warn("Error querying tasks count: ${e.message}")
            }

            val completionRate = if (activeTasks > 0) {
                Math.round((doneTasks.toDouble() / activeTasks.toDouble() * 100.0) * 10.0) / 10.0
            } else 0.0

            TenantDashboardOverviewResponse(
                tenantId = tenantId,
                activeAgents = activeAgents,
                humanStaffCount = humanStaffCount,
                activeTasks = activeTasks,
                completionRate = completionRate,
                status = "HEALTHY"
            )
        }
    }

    fun getBoard(boardId: String, tenantId: String): BoardKanbanResponse {
        val conn = DatabaseManager.getConnection()
        val defaultColumns = listOf("BACKLOG", "TODO", "IN_PROGRESS", "REVIEW", "DONE")
        if (conn == null) {
            return BoardKanbanResponse(boardId = boardId, tenantId = tenantId, columns = defaultColumns)
        }

        return conn.use { c ->
            val cols = mutableListOf<String>()
            try {
                c.prepareStatement("SELECT name FROM board_columns WHERE board_id = ? ORDER BY position ASC").use { ps ->
                    ps.setString(1, boardId)
                    ps.executeQuery().use { rs ->
                        while (rs.next()) {
                            cols.add(rs.getString(1))
                        }
                    }
                }
            } catch (e: Exception) {
                logger.warn("Error querying board_columns: ${e.message}")
            }

            if (cols.isEmpty()) {
                // If board_columns is empty for this board, return standard kanban columns
                BoardKanbanResponse(boardId = boardId, tenantId = tenantId, columns = defaultColumns)
            } else {
                BoardKanbanResponse(boardId = boardId, tenantId = tenantId, columns = cols)
            }
        }
    }

    fun getWorldTrends(): List<WorldTrendClusterItem> {
        val conn = DatabaseManager.getConnection() ?: return emptyList()
        return conn.use { c ->
            val list = mutableListOf<WorldTrendClusterItem>()
            try {
                c.prepareStatement("SELECT id, title, summary, category, confidence, published_at FROM world_trend_clusters ORDER BY published_at DESC LIMIT 50").use { ps ->
                    ps.executeQuery().use { rs ->
                        while (rs.next()) {
                            list.add(
                                WorldTrendClusterItem(
                                    id = rs.getString("id") ?: "",
                                    title = rs.getString("title") ?: "",
                                    summary = rs.getString("summary") ?: "",
                                    category = rs.getString("category") ?: "GENERAL",
                                    confidence = rs.getDouble("confidence"),
                                    publishedAt = rs.getLong("published_at")
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                logger.warn("Error querying world_trend_clusters: ${e.message}")
            }
            list
        }
    }

    fun getProactiveSubscriptions(tenantId: String): List<ProactiveSubscriptionItem> {
        val conn = DatabaseManager.getConnection() ?: return emptyList()
        return conn.use { c ->
            val list = mutableListOf<ProactiveSubscriptionItem>()
            try {
                c.prepareStatement("SELECT id, tenant_id, staff_id, channel, enabled_notif_types, send_times, is_active FROM proactive_subscriptions WHERE tenant_id = ? OR tenant_id = 'tenant-default'").use { ps ->
                    ps.setString(1, tenantId)
                    ps.executeQuery().use { rs ->
                        while (rs.next()) {
                            val typesStr = rs.getString("enabled_notif_types") ?: ""
                            val typesList = if (typesStr.isNotBlank()) typesStr.split(",").map { it.trim() } else emptyList()
                            val timesStr = rs.getString("send_times") ?: ""
                            val timesList = if (timesStr.isNotBlank()) timesStr.split(",").map { it.trim() } else emptyList()
                            val isActive = rs.getBoolean("is_active")

                            list.add(
                                ProactiveSubscriptionItem(
                                    id = rs.getString("id"),
                                    tenantId = rs.getString("tenant_id") ?: tenantId,
                                    staffId = rs.getString("staff_id") ?: "",
                                    channel = rs.getString("channel") ?: "WHATSAPP",
                                    types = typesList,
                                    sendTimes = timesList,
                                    status = if (isActive) "ACTIVE" else "INACTIVE"
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                logger.warn("Error querying proactive_subscriptions: ${e.message}")
            }
            list
        }
    }

    fun createProactiveSubscription(tenantId: String, req: ProactiveSubscriptionRequest): ProactiveSubscriptionItem {
        val newId = "sub-${UUID.randomUUID().toString().take(8)}"
        val conn = DatabaseManager.getConnection()
        conn?.use { c ->
            try {
                c.prepareStatement(
                    """
                    INSERT INTO proactive_subscriptions (id, tenant_id, staff_id, channel, enabled_notif_types, send_times, is_active)
                    VALUES (?, ?, ?, ?, ?, ?, true)
                    """.trimIndent()
                ).use { ps ->
                    ps.setString(1, newId)
                    ps.setString(2, tenantId)
                    ps.setString(3, req.staffId)
                    ps.setString(4, req.channel)
                    ps.setString(5, req.types.joinToString(","))
                    ps.setString(6, req.sendTimes.joinToString(","))
                    ps.executeUpdate()
                }
            } catch (e: Exception) {
                logger.warn("Error inserting proactive_subscription: ${e.message}")
            }
        }
        return ProactiveSubscriptionItem(
            id = newId,
            tenantId = tenantId,
            staffId = req.staffId,
            channel = req.channel,
            types = req.types,
            sendTimes = req.sendTimes,
            status = "ACTIVE"
        )
    }

    fun getDailyReports(tenantId: String): List<WorkReportDailyItem> {
        val conn = DatabaseManager.getConnection() ?: return emptyList()
        return conn.use { c ->
            val list = mutableListOf<WorkReportDailyItem>()
            try {
                c.prepareStatement("SELECT id, tenant_id, staff_id, staff_name, report_date, work_summary, blockers_and_challenges, plan_for_tomorrow FROM work_reports_daily WHERE tenant_id = ? OR tenant_id = 'tenant-default' ORDER BY submitted_at DESC LIMIT 50").use { ps ->
                    ps.setString(1, tenantId)
                    ps.executeQuery().use { rs ->
                        while (rs.next()) {
                            list.add(
                                WorkReportDailyItem(
                                    id = rs.getString("id"),
                                    tenantId = rs.getString("tenant_id") ?: tenantId,
                                    staffId = rs.getString("staff_id") ?: "",
                                    staffName = rs.getString("staff_name") ?: "",
                                    reportDate = rs.getString("report_date") ?: "",
                                    accomplishments = rs.getString("work_summary") ?: "",
                                    blockers = rs.getString("blockers_and_challenges") ?: "",
                                    plannedNext = rs.getString("plan_for_tomorrow") ?: "",
                                    status = "SUBMITTED"
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                logger.warn("Error querying work_reports_daily: ${e.message}")
            }
            list
        }
    }

    fun createDailyReport(tenantId: String, req: CreateWorkReportRequest): WorkReportDailyItem {
        val newId = "wr-${System.currentTimeMillis()}"
        val conn = DatabaseManager.getConnection()
        conn?.use { c ->
            try {
                c.prepareStatement(
                    """
                    INSERT INTO work_reports_daily (id, tenant_id, staff_id, staff_name, report_date, work_summary, blockers_and_challenges, plan_for_tomorrow, submitted_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent()
                ).use { ps ->
                    ps.setString(1, newId)
                    ps.setString(2, tenantId)
                    ps.setString(3, req.staffId)
                    ps.setString(4, req.staffName)
                    ps.setString(5, req.reportDate)
                    ps.setString(6, req.accomplishments)
                    ps.setString(7, req.blockers)
                    ps.setString(8, req.plannedNext)
                    ps.setLong(9, System.currentTimeMillis())
                    ps.executeUpdate()
                }
            } catch (e: Exception) {
                logger.warn("Error inserting work_reports_daily: ${e.message}")
            }
        }
        return WorkReportDailyItem(
            id = newId,
            tenantId = tenantId,
            staffId = req.staffId,
            staffName = req.staffName,
            reportDate = req.reportDate,
            accomplishments = req.accomplishments,
            blockers = req.blockers,
            plannedNext = req.plannedNext,
            status = "SUBMITTED"
        )
    }

    fun getGoals(tenantId: String): List<GoalKpiItem> {
        val conn = DatabaseManager.getConnection() ?: return emptyList()
        return conn.use { c ->
            val list = mutableListOf<GoalKpiItem>()
            try {
                c.prepareStatement("SELECT id, tenant_id, title, target_value, current_value, unit, period, status FROM goals_kpi WHERE tenant_id = ? OR tenant_id = 'tenant-default'").use { ps ->
                    ps.setString(1, tenantId)
                    ps.executeQuery().use { rs ->
                        while (rs.next()) {
                            list.add(
                                GoalKpiItem(
                                    id = rs.getString("id"),
                                    tenantId = rs.getString("tenant_id") ?: tenantId,
                                    title = rs.getString("title") ?: "",
                                    targetValue = rs.getDouble("target_value"),
                                    currentValue = rs.getDouble("current_value"),
                                    unit = rs.getString("unit") ?: "",
                                    period = rs.getString("period") ?: "",
                                    status = rs.getString("status") ?: "ON_TRACK"
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                logger.warn("Error querying goals_kpi: ${e.message}")
            }
            list
        }
    }

    fun getReviews(tenantId: String): List<PerformanceReviewItem> {
        val conn = DatabaseManager.getConnection() ?: return emptyList()
        return conn.use { c ->
            val list = mutableListOf<PerformanceReviewItem>()
            try {
                c.prepareStatement("SELECT id, tenant_id, staff_id, reviewer_id, review_period, actual_calculated_score, manager_feedback, status FROM performance_reviews WHERE tenant_id = ? OR tenant_id = 'tenant-default'").use { ps ->
                    ps.setString(1, tenantId)
                    ps.executeQuery().use { rs ->
                        while (rs.next()) {
                            list.add(
                                PerformanceReviewItem(
                                    id = rs.getString("id"),
                                    tenantId = rs.getString("tenant_id") ?: tenantId,
                                    staffId = rs.getString("staff_id") ?: "",
                                    reviewerId = rs.getString("reviewer_id") ?: "",
                                    period = rs.getString("review_period") ?: "",
                                    overallScore = rs.getDouble("actual_calculated_score"),
                                    feedback = rs.getString("manager_feedback") ?: "",
                                    status = rs.getString("status") ?: "FINALIZED"
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                logger.warn("Error querying performance_reviews: ${e.message}")
            }
            list
        }
    }

    fun getPredictions(tenantId: String): List<PerformanceRiskPredictionItem> {
        val conn = DatabaseManager.getConnection() ?: return emptyList()
        return conn.use { c ->
            val list = mutableListOf<PerformanceRiskPredictionItem>()
            try {
                c.prepareStatement("SELECT id, tenant_id, entity_id, entity_name, risk_level, current_score, top_risk_factors_json, preventive_actions_json FROM performance_risk_predictions WHERE tenant_id = ? OR tenant_id = 'tenant-default'").use { ps ->
                    ps.setString(1, tenantId)
                    ps.executeQuery().use { rs ->
                        while (rs.next()) {
                            list.add(
                                PerformanceRiskPredictionItem(
                                    id = rs.getString("id"),
                                    tenantId = rs.getString("tenant_id") ?: tenantId,
                                    staffId = rs.getString("entity_id") ?: "",
                                    staffName = rs.getString("entity_name") ?: "",
                                    riskLevel = rs.getString("risk_level") ?: "LOW",
                                    riskScore = rs.getDouble("current_score"),
                                    primaryFactor = rs.getString("top_risk_factors_json") ?: "Normal distribution",
                                    recommendation = rs.getString("preventive_actions_json") ?: "Maintain regular monitoring"
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                logger.warn("Error querying performance_risk_predictions: ${e.message}")
            }
            list
        }
    }

    fun getExecutiveBriefs(tenantId: String): List<ExecutiveBriefItem> {
        val conn = DatabaseManager.getConnection() ?: return emptyList()
        return conn.use { c ->
            val list = mutableListOf<ExecutiveBriefItem>()
            try {
                c.prepareStatement("SELECT id, tenant_id, title, brief_date, executive_summary, key_achievements, risk_alerts FROM executive_briefs WHERE tenant_id = ? OR tenant_id = 'tenant-default'").use { ps ->
                    ps.setString(1, tenantId)
                    ps.executeQuery().use { rs ->
                        while (rs.next()) {
                            val achStr = rs.getString("key_achievements") ?: ""
                            val achList = if (achStr.isNotBlank()) achStr.split("\n").map { it.trim() } else emptyList()
                            val riskStr = rs.getString("risk_alerts") ?: ""
                            val riskList = if (riskStr.isNotBlank()) riskStr.split("\n").map { it.trim() } else emptyList()

                            list.add(
                                ExecutiveBriefItem(
                                    id = rs.getString("id"),
                                    tenantId = rs.getString("tenant_id") ?: tenantId,
                                    title = rs.getString("title") ?: "",
                                    period = rs.getString("brief_date") ?: "",
                                    executiveSummary = rs.getString("executive_summary") ?: "",
                                    keyAchievements = achList,
                                    riskAreas = riskList
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                logger.warn("Error querying executive_briefs: ${e.message}")
            }
            list
        }
    }

    fun getSecurityAnomalies(tenantId: String): List<SecurityAnomalyItem> {
        val conn = DatabaseManager.getConnection() ?: return emptyList()
        return conn.use { c ->
            val list = mutableListOf<SecurityAnomalyItem>()
            try {
                c.prepareStatement("SELECT id, tenant_id, incident_type, severity, description, detected_at, status FROM security_incidents WHERE tenant_id = ? OR tenant_id = 'tenant-default'").use { ps ->
                    ps.setString(1, tenantId)
                    ps.executeQuery().use { rs ->
                        while (rs.next()) {
                            list.add(
                                SecurityAnomalyItem(
                                    id = rs.getString("id"),
                                    tenantId = rs.getString("tenant_id") ?: tenantId,
                                    anomalyType = rs.getString("incident_type") ?: "SECURITY_ALERT",
                                    severity = rs.getString("severity") ?: "LOW",
                                    description = rs.getString("description") ?: "",
                                    detectedAt = rs.getLong("detected_at"),
                                    status = rs.getString("status") ?: "OPEN"
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                logger.warn("Error querying security_incidents: ${e.message}")
            }
            list
        }
    }

    fun getDataSubjectRequests(tenantId: String): List<DataSubjectRequestItem> {
        val conn = DatabaseManager.getConnection() ?: return emptyList()
        return conn.use { c ->
            val list = mutableListOf<DataSubjectRequestItem>()
            try {
                c.prepareStatement("SELECT id, tenant_id, request_type, customer_email, status, requested_at FROM data_subject_requests WHERE tenant_id = ? OR tenant_id = 'tenant-default'").use { ps ->
                    ps.setString(1, tenantId)
                    ps.executeQuery().use { rs ->
                        while (rs.next()) {
                            list.add(
                                DataSubjectRequestItem(
                                    id = rs.getString("id"),
                                    tenantId = rs.getString("tenant_id") ?: tenantId,
                                    requestType = rs.getString("request_type") ?: "GENERAL_INQUIRY",
                                    requesterEmail = rs.getString("customer_email") ?: "",
                                    status = rs.getString("status") ?: "PENDING",
                                    requestedAt = rs.getLong("requested_at")
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                logger.warn("Error querying data_subject_requests: ${e.message}")
            }
            list
        }
    }

    fun createDataSubjectRequest(tenantId: String, req: CreateDataSubjectRequest): DataSubjectRequestItem {
        val newId = "dsr-${System.currentTimeMillis()}"
        val now = System.currentTimeMillis()
        val conn = DatabaseManager.getConnection()
        conn?.use { c ->
            try {
                c.prepareStatement(
                    """
                    INSERT INTO data_subject_requests (id, tenant_id, request_type, customer_email, result_summary, status, requested_at)
                    VALUES (?, ?, ?, ?, ?, 'PENDING', ?)
                    """.trimIndent()
                ).use { ps ->
                    ps.setString(1, newId)
                    ps.setString(2, tenantId)
                    ps.setString(3, req.requestType)
                    ps.setString(4, req.requesterEmail)
                    ps.setString(5, req.details)
                    ps.setLong(6, now)
                    ps.executeUpdate()
                }
            } catch (e: Exception) {
                logger.warn("Error inserting data_subject_request: ${e.message}")
            }
        }
        return DataSubjectRequestItem(
            id = newId,
            tenantId = tenantId,
            requestType = req.requestType,
            requesterEmail = req.requesterEmail,
            status = "PENDING",
            requestedAt = now
        )
    }

    fun getAttendanceAnomalies(tenantId: String): List<AttendanceAnomalyItem> {
        val conn = DatabaseManager.getConnection() ?: return emptyList()
        return conn.use { c ->
            val list = mutableListOf<AttendanceAnomalyItem>()
            try {
                c.prepareStatement("SELECT id, tenant_id, user_id, user_name, anomaly_type, created_at, resolved, details FROM attendance_anomalies WHERE tenant_id = ? OR tenant_id = 'tenant-default'").use { ps ->
                    ps.setString(1, tenantId)
                    ps.executeQuery().use { rs ->
                        while (rs.next()) {
                            val isResolved = rs.getBoolean("resolved")
                            list.add(
                                AttendanceAnomalyItem(
                                    id = rs.getString("id"),
                                    tenantId = rs.getString("tenant_id") ?: tenantId,
                                    staffId = rs.getString("user_id") ?: "",
                                    staffName = rs.getString("user_name") ?: "",
                                    anomalyType = rs.getString("anomaly_type") ?: "ANOMALY",
                                    timestamp = rs.getLong("created_at"),
                                    status = if (isResolved) "RESOLVED" else "OPEN",
                                    resolutionNotes = rs.getString("details") ?: ""
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                logger.warn("Error querying attendance_anomalies: ${e.message}")
            }
            list
        }
    }

    fun resolveAttendanceAnomaly(anomalyId: String): Boolean {
        val conn = DatabaseManager.getConnection() ?: return false
        return conn.use { c ->
            try {
                c.prepareStatement("UPDATE attendance_anomalies SET resolved = true, resolved_at = ? WHERE id = ?").use { ps ->
                    ps.setLong(1, System.currentTimeMillis())
                    ps.setString(2, anomalyId)
                    ps.executeUpdate() > 0
                }
            } catch (e: Exception) {
                logger.warn("Error resolving attendance_anomaly: ${e.message}")
                false
            }
        }
    }

    fun getAnalyticsScores(period: String): AnalyticsScoreResponse {
        val conn = DatabaseManager.getConnection()
        if (conn == null) {
            return AnalyticsScoreResponse(period = period, humanRanking = emptyList(), aiRanking = emptyList())
        }

        return conn.use { c ->
            val human = mutableListOf<RankingEntry>()
            val ai = mutableListOf<RankingEntry>()

            try {
                c.prepareStatement("SELECT name FROM users WHERE is_active = true ORDER BY created_at ASC LIMIT 10").use { ps ->
                    ps.executeQuery().use { rs ->
                        var rank = 1
                        while (rs.next()) {
                            human.add(RankingEntry(name = rs.getString("name") ?: "Staff", score = 90.0 - (rank * 2.0), rank = rank))
                            rank++
                        }
                    }
                }
            } catch (e: Exception) {
                logger.warn("Error querying human rankings: ${e.message}")
            }

            try {
                c.prepareStatement("SELECT name, quality_rating FROM ai_agents WHERE status != 'DELETED' ORDER BY quality_rating DESC NULLS LAST LIMIT 10").use { ps ->
                    ps.executeQuery().use { rs ->
                        var rank = 1
                        while (rs.next()) {
                            val score = rs.getDouble("quality_rating").takeIf { it > 0.0 } ?: (95.0 - rank)
                            ai.add(RankingEntry(name = rs.getString("name") ?: "Agent", score = score, rank = rank))
                            rank++
                        }
                    }
                }
            } catch (e: Exception) {
                logger.warn("Error querying ai rankings: ${e.message}")
            }

            AnalyticsScoreResponse(period = period, humanRanking = human, aiRanking = ai)
        }
    }
}
