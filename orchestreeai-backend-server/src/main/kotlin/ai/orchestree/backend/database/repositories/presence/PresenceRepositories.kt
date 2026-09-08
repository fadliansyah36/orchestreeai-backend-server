package ai.orchestree.backend.database.repositories.presence

import ai.orchestree.backend.config.AppConfig
import ai.orchestree.backend.models.PresenceCheckLog
import ai.orchestree.backend.models.PresenceMethodBreakdown
import ai.orchestree.backend.models.PresenceSecurityAuditStats
import ai.orchestree.backend.models.PresenceSecurityAuditSummary
import ai.orchestree.backend.models.UserPresenceEnrollment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class UserPresenceEnrollmentRepository(
    private val databaseUrl: String = AppConfig.load().supabase.databaseUrl
) {
    private val logger = LoggerFactory.getLogger(UserPresenceEnrollmentRepository::class.java)
    private val memoryEnrollments = ConcurrentHashMap<String, UserPresenceEnrollment>()

    init {
        // Seed default enrollment for standard test users
        val defaultEnrollment = UserPresenceEnrollment(
            id = "enr-01",
            userId = "usr-01",
            isEnabled = true,
            enrolledMethods = listOf("FACE", "FINGERPRINT"),
            faceEmbeddingRef = null, // Will be set on enroll
            fingerprintRegisteredDeviceIds = listOf("dev-known-01", "trusted-android-device"),
            enrolledAt = System.currentTimeMillis() - (7 * 86400000L)
        )
        memoryEnrollments["usr-01"] = defaultEnrollment
    }

    private fun getDbConnection(): Connection? {
        return try {
            if (databaseUrl.isNotBlank() && databaseUrl.startsWith("postgres")) {
                val jdbcUrl = if (databaseUrl.startsWith("postgresql://")) {
                    "jdbc:" + databaseUrl
                } else {
                    databaseUrl
                }
                DriverManager.getConnection(jdbcUrl)
            } else null
        } catch (e: Throwable) {
            logger.debug("Database direct connection not available: ${e.message}")
            null
        }
    }

    suspend fun get(userId: String): UserPresenceEnrollment? = withContext(Dispatchers.IO) {
        val conn = getDbConnection()
        if (conn != null) {
            try {
                conn.use { c ->
                    val stmt = c.prepareStatement(
                        "SELECT id, user_id, is_enabled, enrolled_methods, face_embedding_ref, fingerprint_registered_device_ids, enrolled_at FROM user_presence_enrollment WHERE user_id = ?"
                    )
                    stmt.setString(1, userId)
                    val rs = stmt.executeQuery()
                    if (rs.next()) {
                        val enrolledArray = rs.getArray("enrolled_methods")
                        val methods = if (enrolledArray != null) {
                            (enrolledArray.array as? Array<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                        } else emptyList()

                        val fpArray = rs.getArray("fingerprint_registered_device_ids")
                        val deviceIds = if (fpArray != null) {
                            (fpArray.array as? Array<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                        } else emptyList()

                        val enrolledAtTs = rs.getTimestamp("enrolled_at")

                        val enrollment = UserPresenceEnrollment(
                            id = rs.getString("id"),
                            userId = rs.getString("user_id"),
                            isEnabled = rs.getBoolean("is_enabled"),
                            enrolledMethods = methods,
                            faceEmbeddingRef = rs.getString("face_embedding_ref"),
                            fingerprintRegisteredDeviceIds = deviceIds,
                            enrolledAt = enrolledAtTs?.time
                        )
                        memoryEnrollments[userId] = enrollment
                        return@withContext enrollment
                    }
                }
            } catch (e: Exception) {
                logger.warn("Failed to fetch user_presence_enrollment from DB: ${e.message}")
            }
        }
        memoryEnrollments[userId]
    }

    suspend fun save(enrollment: UserPresenceEnrollment): UserPresenceEnrollment = withContext(Dispatchers.IO) {
        memoryEnrollments[enrollment.userId] = enrollment

        val conn = getDbConnection()
        if (conn != null) {
            try {
                conn.use { c ->
                    val methodsArray = c.createArrayOf("text", enrollment.enrolledMethods.toTypedArray())
                    val fpArray = c.createArrayOf("text", enrollment.fingerprintRegisteredDeviceIds.toTypedArray())
                    val enrolledTs = enrollment.enrolledAt?.let { Timestamp(it) } ?: Timestamp(System.currentTimeMillis())

                    val stmt = c.prepareStatement(
                        """
                        INSERT INTO user_presence_enrollment (id, user_id, is_enabled, enrolled_methods, face_embedding_ref, fingerprint_registered_device_ids, enrolled_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (user_id) DO UPDATE SET
                            is_enabled = EXCLUDED.is_enabled,
                            enrolled_methods = EXCLUDED.enrolled_methods,
                            face_embedding_ref = COALESCE(EXCLUDED.face_embedding_ref, user_presence_enrollment.face_embedding_ref),
                            fingerprint_registered_device_ids = EXCLUDED.fingerprint_registered_device_ids,
                            enrolled_at = EXCLUDED.enrolled_at
                        """.trimIndent()
                    )
                    stmt.setObject(1, UUID.fromString(if (enrollment.id.length == 36) enrollment.id else UUID.randomUUID().toString()))
                    stmt.setString(2, enrollment.userId)
                    stmt.setBoolean(3, enrollment.isEnabled)
                    stmt.setArray(4, methodsArray)
                    stmt.setString(5, enrollment.faceEmbeddingRef)
                    stmt.setArray(6, fpArray)
                    stmt.setTimestamp(7, enrolledTs)
                    stmt.executeUpdate()
                }
            } catch (e: Exception) {
                logger.warn("Failed to persist user_presence_enrollment to DB: ${e.message}")
            }
        }
        enrollment
    }

    suspend fun getEnrolledUsersCount(): Int = withContext(Dispatchers.IO) {
        val conn = getDbConnection()
        if (conn != null) {
            try {
                conn.use { c ->
                    val stmt = c.prepareStatement("SELECT COUNT(*) FROM user_presence_enrollment WHERE is_enabled = true")
                    val rs = stmt.executeQuery()
                    if (rs.next()) {
                        val dbCount = rs.getInt(1)
                        val memCount = memoryEnrollments.values.count { it.isEnabled }
                        return@withContext maxOf(dbCount, memCount)
                    }
                }
            } catch (e: Exception) {
                logger.warn("Failed to count enrolled users from DB: ${e.message}")
            }
        }
        memoryEnrollments.values.count { it.isEnabled }
    }

    companion object {
        val defaultInstance: UserPresenceEnrollmentRepository by lazy { UserPresenceEnrollmentRepository() }
    }
}

class PresenceCheckLogRepository(
    private val databaseUrl: String = AppConfig.load().supabase.databaseUrl
) {
    private val logger = LoggerFactory.getLogger(PresenceCheckLogRepository::class.java)
    private val memoryLogs = ConcurrentHashMap<String, PresenceCheckLog>()

    private fun getDbConnection(): Connection? {
        return try {
            if (databaseUrl.isNotBlank() && databaseUrl.startsWith("postgres")) {
                val jdbcUrl = if (databaseUrl.startsWith("postgresql://")) {
                    "jdbc:" + databaseUrl
                } else {
                    databaseUrl
                }
                DriverManager.getConnection(jdbcUrl)
            } else null
        } catch (e: Throwable) {
            logger.debug("Database direct connection not available: ${e.message}")
            null
        }
    }

    suspend fun insert(log: PresenceCheckLog): PresenceCheckLog = withContext(Dispatchers.IO) {
        memoryLogs[log.id] = log

        val conn = getDbConnection()
        if (conn != null) {
            try {
                conn.use { c ->
                    val stmt = c.prepareStatement(
                        """
                        INSERT INTO presence_check_log (id, user_id, check_type, method_used, verification_result, device_id, ip_address, location_approx, checked_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """.trimIndent()
                    )
                    stmt.setObject(1, UUID.fromString(if (log.id.length == 36) log.id else UUID.randomUUID().toString()))
                    stmt.setString(2, log.userId)
                    stmt.setString(3, log.checkType)
                    stmt.setString(4, log.methodUsed)
                    stmt.setString(5, log.verificationResult)
                    stmt.setString(6, log.deviceId)
                    stmt.setString(7, log.ipAddress)
                    stmt.setString(8, log.locationApprox)
                    stmt.setTimestamp(9, Timestamp(log.checkedAt))
                    stmt.executeUpdate()
                }
            } catch (e: Exception) {
                logger.warn("Failed to persist presence_check_log to DB: ${e.message}")
            }
        }
        log
    }

    suspend fun findTodayCheckIn(userId: String, zoneId: ZoneId = ZoneId.of("Asia/Jakarta")): PresenceCheckLog? = withContext(Dispatchers.IO) {
        val today = LocalDate.now(zoneId)
        val startOfDayMillis = today.atStartOfDay(zoneId).toInstant().toEpochMilli()

        // Check in memory first
        val memCheckIn = memoryLogs.values
            .filter { it.userId == userId && it.checkedAt >= startOfDayMillis }
            .filter { it.checkType in listOf("CHECK_IN", "LOGIN") && it.verificationResult == "SUCCESS" }
            .maxByOrNull { it.checkedAt }

        if (memCheckIn != null) return@withContext memCheckIn

        val conn = getDbConnection()
        if (conn != null) {
            try {
                conn.use { c ->
                    val stmt = c.prepareStatement(
                        """
                        SELECT id, user_id, check_type, method_used, verification_result, device_id, ip_address, location_approx, checked_at
                        FROM presence_check_log
                        WHERE user_id = ? 
                          AND check_type IN ('CHECK_IN', 'LOGIN')
                          AND verification_result = 'SUCCESS'
                          AND checked_at >= ?
                        ORDER BY checked_at DESC
                        LIMIT 1
                        """.trimIndent()
                    )
                    stmt.setString(1, userId)
                    stmt.setTimestamp(2, Timestamp(startOfDayMillis))
                    val rs = stmt.executeQuery()
                    if (rs.next()) {
                        val log = PresenceCheckLog(
                            id = rs.getString("id"),
                            userId = rs.getString("user_id"),
                            checkType = rs.getString("check_type"),
                            methodUsed = rs.getString("method_used"),
                            verificationResult = rs.getString("verification_result"),
                            deviceId = rs.getString("device_id"),
                            ipAddress = rs.getString("ip_address"),
                            locationApprox = rs.getString("location_approx"),
                            checkedAt = rs.getTimestamp("checked_at").time
                        )
                        memoryLogs[log.id] = log
                        return@withContext log
                    }
                }
            } catch (e: Exception) {
                logger.warn("Failed to query today check-in from DB: ${e.message}")
            }
        }
        null
    }

    suspend fun hasCheckedOutToday(userId: String, zoneId: ZoneId = ZoneId.of("Asia/Jakarta")): Boolean = withContext(Dispatchers.IO) {
        val today = LocalDate.now(zoneId)
        val startOfDayMillis = today.atStartOfDay(zoneId).toInstant().toEpochMilli()

        val memCheckOut = memoryLogs.values
            .any { it.userId == userId && it.checkedAt >= startOfDayMillis && it.checkType == "CHECK_OUT" && it.verificationResult == "SUCCESS" }

        if (memCheckOut) return@withContext true

        val conn = getDbConnection()
        if (conn != null) {
            try {
                conn.use { c ->
                    val stmt = c.prepareStatement(
                        """
                        SELECT 1 FROM presence_check_log
                        WHERE user_id = ? 
                          AND check_type = 'CHECK_OUT'
                          AND verification_result = 'SUCCESS'
                          AND checked_at >= ?
                        LIMIT 1
                        """.trimIndent()
                    )
                    stmt.setString(1, userId)
                    stmt.setTimestamp(2, Timestamp(startOfDayMillis))
                    val rs = stmt.executeQuery()
                    return@withContext rs.next()
                }
            } catch (e: Exception) {
                logger.warn("Failed to query today check-out from DB: ${e.message}")
            }
        }
        false
    }

    suspend fun getLogs(userId: String): List<PresenceCheckLog> = withContext(Dispatchers.IO) {
        memoryLogs.values.filter { it.userId == userId }.sortedByDescending { it.checkedAt }
    }

    suspend fun getSecurityAuditStats(enrolledUsersCount: Int): PresenceSecurityAuditSummary = withContext(Dispatchers.IO) {
        val allLogs = mutableListOf<PresenceCheckLog>()

        val conn = getDbConnection()
        if (conn != null) {
            try {
                conn.use { c ->
                    val stmt = c.prepareStatement(
                        """
                        SELECT id, user_id, check_type, method_used, verification_result, device_id, ip_address, location_approx, checked_at
                        FROM presence_check_log
                        ORDER BY checked_at DESC
                        """.trimIndent()
                    )
                    val rs = stmt.executeQuery()
                    while (rs.next()) {
                        val log = PresenceCheckLog(
                            id = rs.getString("id"),
                            userId = rs.getString("user_id"),
                            checkType = rs.getString("check_type"),
                            methodUsed = rs.getString("method_used"),
                            verificationResult = rs.getString("verification_result"),
                            deviceId = rs.getString("device_id"),
                            ipAddress = rs.getString("ip_address"),
                            locationApprox = rs.getString("location_approx"),
                            checkedAt = rs.getTimestamp("checked_at").time
                        )
                        allLogs.add(log)
                        memoryLogs[log.id] = log
                    }
                }
            } catch (e: Exception) {
                logger.warn("Failed to query all presence_check_log from DB: ${e.message}")
            }
        }

        for ((_, log) in memoryLogs) {
            if (allLogs.none { it.id == log.id }) {
                allLogs.add(log)
            }
        }

        allLogs.sortByDescending { it.checkedAt }

        val totalChecks = allLogs.size
        val successChecks = allLogs.filter { it.verificationResult == "SUCCESS" }
        val failedChecks = allLogs.filter { it.verificationResult != "SUCCESS" }

        var recentConsecutiveFailures = 0
        for (log in allLogs) {
            if (log.verificationResult != "SUCCESS") {
                recentConsecutiveFailures++
            } else {
                break
            }
        }

        val logsByUser = allLogs.groupBy { it.userId }
        var maxUserConsecutiveFailures = 0
        for ((_, userLogs) in logsByUser) {
            var userConsecutive = 0
            for (log in userLogs) {
                if (log.verificationResult != "SUCCESS") {
                    userConsecutive++
                } else {
                    break
                }
            }
            if (userConsecutive > maxUserConsecutiveFailures) {
                maxUserConsecutiveFailures = userConsecutive
            }
        }
        val effectiveConsecutiveFailures = maxOf(recentConsecutiveFailures, maxUserConsecutiveFailures)

        val unauthorizedReasons = setOf(
            "FAILED_SIMILARITY_BELOW_THRESHOLD",
            "FAILED_DEVICE_NOT_REGISTERED",
            "FAILED_BIOMETRIC_UNSUCCESSFUL",
            "FAILED_NO_ENROLLED_FACE"
        )
        val potentialUnauthorizedAttempts = allLogs.count { it.verificationResult in unauthorizedReasons }

        val faceCount = allLogs.count { it.methodUsed?.equals("FACE", ignoreCase = true) == true }
        val fpCount = allLogs.count {
            it.methodUsed?.equals("FINGERPRINT", ignoreCase = true) == true ||
            it.methodUsed?.equals("BIOMETRIC", ignoreCase = true) == true
        }
        val fallbackCount = allLogs.count { it.methodUsed?.equals("PASSWORD_FALLBACK", ignoreCase = true) == true }

        val lastIncident = failedChecks.maxByOrNull { it.checkedAt }?.checkedAt
        val lastSuccess = successChecks.maxByOrNull { it.checkedAt }?.checkedAt

        val riskLevel = when {
            effectiveConsecutiveFailures >= 3 || potentialUnauthorizedAttempts >= 3 -> "HIGH"
            effectiveConsecutiveFailures >= 1 || potentialUnauthorizedAttempts >= 1 -> "ELEVATED"
            else -> "NORMAL"
        }

        PresenceSecurityAuditSummary(
            totalEnrolledUsers = enrolledUsersCount,
            totalVerificationChecks = totalChecks,
            totalSuccessfulChecks = successChecks.size,
            totalFailedChecks = failedChecks.size,
            consecutiveFailures = effectiveConsecutiveFailures,
            potentialUnauthorizedAttempts = potentialUnauthorizedAttempts,
            methodBreakdown = PresenceMethodBreakdown(
                face = faceCount,
                fingerprint = fpCount,
                passwordFallback = fallbackCount
            ),
            lastIncidentTimestamp = lastIncident,
            lastSuccessfulCheckTimestamp = lastSuccess,
            securityRiskLevel = riskLevel
        )
    }

    companion object {
        val defaultInstance: PresenceCheckLogRepository by lazy { PresenceCheckLogRepository() }
    }
}

class StaffWorkScheduleRepository {
    private val schedules = ConcurrentHashMap<String, WorkSchedule>()

    data class WorkSchedule(
        val checkoutTime: LocalTime = LocalTime.of(17, 0), // 17:00:00 (5 PM)
        val timezone: ZoneId = ZoneId.of("Asia/Jakarta")
    )

    fun getCheckoutTime(userId: String): LocalTime {
        return schedules[userId]?.checkoutTime ?: LocalTime.of(17, 0)
    }

    fun getTimezone(userId: String): ZoneId {
        return schedules[userId]?.timezone ?: ZoneId.of("Asia/Jakarta")
    }

    fun setSchedule(userId: String, checkoutTime: LocalTime, timezone: ZoneId = ZoneId.of("Asia/Jakarta")) {
        schedules[userId] = WorkSchedule(checkoutTime, timezone)
    }

    companion object {
        val defaultInstance: StaffWorkScheduleRepository by lazy { StaffWorkScheduleRepository() }
    }
}
