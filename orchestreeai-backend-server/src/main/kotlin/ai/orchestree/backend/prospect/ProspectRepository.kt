package ai.orchestree.backend.prospect

import ai.orchestree.backend.billing.DatabaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class ProspectRepository(
    private val dbManager: DatabaseManager = DatabaseManager
) {
    private val logger = LoggerFactory.getLogger(ProspectRepository::class.java)

    init {
        ensureSchemaReady()
    }

    private fun ensureSchemaReady() {
        try {
            val conn = dbManager.getConnection() ?: return
            conn.use { c ->
                c.createStatement().use { stmt ->
                    stmt.execute("""
                        CREATE TABLE IF NOT EXISTS prospect_registrations (
                            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            full_name TEXT NOT NULL,
                            email TEXT NOT NULL,
                            phone_number TEXT NOT NULL,
                            whatsapp_number TEXT,
                            address TEXT,
                            company_name TEXT NOT NULL,
                            job_title TEXT NOT NULL,
                            industry_category_id TEXT REFERENCES industry_catalog(id) ON DELETE SET NULL,
                            company_size_range TEXT,
                            interest_option TEXT NOT NULL CHECK (interest_option IN
                                ('schedule_meeting_presentation', 'direct_trial_or_subscription')),
                            interested_plan_id UUID REFERENCES commercial_plans(id) ON DELETE SET NULL,
                            trial_selection_status TEXT DEFAULT 'not_selected'
                                CHECK (trial_selection_status IN ('not_selected','selected_for_trial','trial_activated','rejected')),
                            meeting_status TEXT DEFAULT 'not_scheduled'
                                CHECK (meeting_status IN ('not_scheduled','scheduled','completed','cancelled')),
                            meeting_scheduled_at TIMESTAMPTZ,
                            admin_notes TEXT,
                            contacted_by_admin_id UUID,
                            contacted_at TIMESTAMPTZ,
                            activated_tenant_id UUID REFERENCES tenants(id) ON DELETE SET NULL,
                            ip_address TEXT,
                            submitted_at TIMESTAMPTZ DEFAULT now(),
                            updated_at TIMESTAMPTZ DEFAULT now()
                        );
                        CREATE INDEX IF NOT EXISTS idx_prospect_email ON prospect_registrations(email);
                        CREATE INDEX IF NOT EXISTS idx_prospect_submitted_at ON prospect_registrations(submitted_at DESC);
                        CREATE INDEX IF NOT EXISTS idx_prospect_trial_status ON prospect_registrations(trial_selection_status);
                        CREATE INDEX IF NOT EXISTS idx_prospect_meeting_status ON prospect_registrations(meeting_status);
                    """.trimIndent())
                }
            }
        } catch (e: Exception) {
            logger.warn("Schema initialization notice: ${e.message}")
        }
    }

    suspend fun insertProspect(req: ProspectRegistrationRequest, ipAddress: String?): ProspectRegistrationDto = withContext(Dispatchers.IO) {
        val conn = dbManager.getConnection() ?: error("Database connection unavailable")
        conn.use { c ->
            val sql = """
                INSERT INTO prospect_registrations (
                    full_name, email, phone_number, whatsapp_number, address,
                    company_name, job_title, industry_category_id, company_size_range,
                    interest_option, interested_plan_id, ip_address, submitted_at, updated_at
                ) VALUES (
                    ?, ?, ?, ?, ?,
                    ?, ?, ?, ?,
                    ?, ?::uuid, ?, now(), now()
                ) RETURNING id, submitted_at
            """.trimIndent()

            c.prepareStatement(sql).use { ps ->
                ps.setString(1, req.fullName.trim())
                ps.setString(2, req.email.trim().lowercase())
                ps.setString(3, req.phoneNumber.trim())
                ps.setString(4, req.whatsappNumber?.trim() ?: req.phoneNumber.trim())
                ps.setString(5, req.address?.trim())
                ps.setString(6, req.companyName.trim())
                ps.setString(7, req.jobTitle.trim())
                ps.setString(8, req.industryCategoryId)
                ps.setString(9, req.companySizeRange)
                ps.setString(10, req.interestOption)
                ps.setString(11, req.interestedPlanId)
                ps.setString(12, ipAddress)

                val rs = ps.executeQuery()
                if (rs.next()) {
                    val id = rs.getString("id")
                    val submittedAt = rs.getTimestamp("submitted_at")?.toInstant()?.toString() ?: Instant.now().toString()
                    getProspectById(id) ?: ProspectRegistrationDto(
                        id = id,
                        fullName = req.fullName,
                        email = req.email,
                        phoneNumber = req.phoneNumber,
                        whatsappNumber = req.whatsappNumber ?: req.phoneNumber,
                        address = req.address,
                        companyName = req.companyName,
                        jobTitle = req.jobTitle,
                        industryCategoryId = req.industryCategoryId,
                        companySizeRange = req.companySizeRange,
                        interestOption = req.interestOption,
                        interestedPlanId = req.interestedPlanId,
                        trialSelectionStatus = "not_selected",
                        meetingStatus = "not_scheduled",
                        submittedAt = submittedAt
                    )
                } else {
                    error("Failed to insert prospect registration")
                }
            }
        }
    }

    suspend fun getProspectById(id: String): ProspectRegistrationDto? = withContext(Dispatchers.IO) {
        val conn = dbManager.getConnection() ?: return@withContext null
        conn.use { c ->
            val sql = """
                SELECT p.*,
                       ic.industry_name AS industry_name,
                       cp.plan_name AS plan_name
                FROM prospect_registrations p
                LEFT JOIN industry_catalog ic ON ic.id = p.industry_category_id
                LEFT JOIN commercial_plans cp ON cp.id = p.interested_plan_id
                WHERE p.id = ?::uuid
            """.trimIndent()

            c.prepareStatement(sql).use { ps ->
                ps.setString(1, id)
                val rs = ps.executeQuery()
                if (rs.next()) {
                    mapResultSetToDto(rs)
                } else null
            }
        }
    }

    suspend fun getProspects(
        search: String? = null,
        interestOption: String? = null,
        trialStatus: String? = null,
        meetingStatus: String? = null
    ): List<ProspectRegistrationDto> = withContext(Dispatchers.IO) {
        val conn = dbManager.getConnection() ?: return@withContext emptyList()
        conn.use { c ->
            val conditions = mutableListOf<String>()
            val params = mutableListOf<String>()

            if (!search.isNullOrBlank()) {
                val q = "%${search.trim().lowercase()}%"
                conditions.add("(LOWER(p.full_name) LIKE ? OR LOWER(p.email) LIKE ? OR LOWER(p.company_name) LIKE ? OR p.phone_number LIKE ?)")
                params.add(q); params.add(q); params.add(q); params.add(q)
            }
            if (!interestOption.isNullOrBlank()) {
                conditions.add("p.interest_option = ?")
                params.add(interestOption.trim())
            }
            if (!trialStatus.isNullOrBlank()) {
                conditions.add("p.trial_selection_status = ?")
                params.add(trialStatus.trim())
            }
            if (!meetingStatus.isNullOrBlank()) {
                conditions.add("p.meeting_status = ?")
                params.add(meetingStatus.trim())
            }

            val whereClause = if (conditions.isNotEmpty()) "WHERE " + conditions.joinToString(" AND ") else ""
            val sql = """
                SELECT p.*,
                       ic.industry_name AS industry_name,
                       cp.plan_name AS plan_name
                FROM prospect_registrations p
                LEFT JOIN industry_catalog ic ON ic.id = p.industry_category_id
                LEFT JOIN commercial_plans cp ON cp.id = p.interested_plan_id
                $whereClause
                ORDER BY p.submitted_at DESC
            """.trimIndent()

            c.prepareStatement(sql).use { ps ->
                for (i in params.indices) {
                    ps.setString(i + 1, params[i])
                }
                val rs = ps.executeQuery()
                val list = mutableListOf<ProspectRegistrationDto>()
                while (rs.next()) {
                    list.add(mapResultSetToDto(rs))
                }
                list
            }
        }
    }

    suspend fun selectTrial(id: String, status: String, adminId: String?, notes: String?): ProspectRegistrationDto = withContext(Dispatchers.IO) {
        val conn = dbManager.getConnection() ?: error("Database connection unavailable")
        conn.use { c ->
            val sql = """
                UPDATE prospect_registrations
                SET trial_selection_status = ?,
                    admin_notes = COALESCE(?, admin_notes),
                    contacted_by_admin_id = ?::uuid,
                    contacted_at = now(),
                    updated_at = now()
                WHERE id = ?::uuid
            """.trimIndent()

            c.prepareStatement(sql).use { ps ->
                ps.setString(1, status)
                ps.setString(2, notes)
                ps.setString(3, adminId)
                ps.setString(4, id)
                ps.executeUpdate()
            }
        }
        getProspectById(id) ?: error("Prospect with ID $id not found")
    }

    suspend fun scheduleMeeting(id: String, scheduledAt: String, status: String, notes: String?, adminId: String?): ProspectRegistrationDto = withContext(Dispatchers.IO) {
        val conn = dbManager.getConnection() ?: error("Database connection unavailable")
        conn.use { c ->
            val sql = """
                UPDATE prospect_registrations
                SET meeting_status = ?,
                    meeting_scheduled_at = ?::timestamptz,
                    admin_notes = COALESCE(?, admin_notes),
                    contacted_by_admin_id = ?::uuid,
                    contacted_at = now(),
                    updated_at = now()
                WHERE id = ?::uuid
            """.trimIndent()

            c.prepareStatement(sql).use { ps ->
                ps.setString(1, status)
                ps.setString(2, scheduledAt)
                ps.setString(3, notes)
                ps.setString(4, adminId)
                ps.setString(5, id)
                ps.executeUpdate()
            }
        }
        getProspectById(id) ?: error("Prospect with ID $id not found")
    }

    suspend fun activateTrialTenant(id: String, adminId: String?): ActivateTrialResponse = withContext(Dispatchers.IO) {
        val prospect = getProspectById(id) ?: error("Prospect registration not found: $id")
        val conn = dbManager.getConnection() ?: error("Database connection unavailable")

        val generatedTenantId = "tenant-trial-${UUID.randomUUID().toString().substring(0, 8)}"
        val trialExpires = Instant.now().plus(7, ChronoUnit.DAYS)

        conn.use { c ->
            c.autoCommit = false
            try {
                // 1. Create tenant
                c.prepareStatement("""
                    INSERT INTO tenants (id, name, domain, tier, status)
                    VALUES (?, ?, ?, 'TRIAL', 'ACTIVE')
                    ON CONFLICT (id) DO NOTHING
                """.trimIndent()).use { ps ->
                    ps.setString(1, generatedTenantId)
                    ps.setString(2, prospect.companyName)
                    val domainPrefix = prospect.companyName.lowercase().replace(Regex("[^a-z0-9]"), "").take(24)
                    val uniqueDomain = "$domainPrefix-${generatedTenantId.takeLast(8)}.trial.orchestree.ai"
                    ps.setString(3, uniqueDomain)
                    ps.executeUpdate()
                }

                // 2. Ensure a trial commercial plan exists or use default
                var trialPlanId: String? = null
                c.prepareStatement("SELECT id FROM commercial_plans WHERE LOWER(plan_code) = 'trial' LIMIT 1").use { ps ->
                    val rs = ps.executeQuery()
                    if (rs.next()) {
                        trialPlanId = rs.getString("id")
                    }
                }
                if (trialPlanId == null) {
                    val newPlanId = UUID.randomUUID().toString()
                    c.prepareStatement("""
                        INSERT INTO commercial_plans (
                            id, plan_code, plan_name, billing_interval, price, currency,
                            credit_allocation, human_seat_limit, ai_agent_limit, is_price_visible, is_active, sort_order
                        ) VALUES (?::uuid, 'trial', '7-Day Autonomous Trial', 'monthly', 0, 'IDR', 1000, 5, 3, false, true, 0)
                        ON CONFLICT DO NOTHING
                    """.trimIndent()).use { ps ->
                        ps.setString(1, newPlanId)
                        ps.executeUpdate()
                    }
                    trialPlanId = newPlanId
                }

                // 3. Create tenant subscription
                c.prepareStatement("""
                    INSERT INTO tenant_subscriptions (
                        id, tenant_id, plan_id, status, current_period_start, current_period_end, is_founder_exclusive, updated_at
                    ) VALUES (
                        gen_random_uuid(), ?, ?::uuid, 'active', now(), ?::timestamptz, false, now()
                    )
                """.trimIndent()).use { ps ->
                    ps.setString(1, generatedTenantId)
                    ps.setString(2, trialPlanId)
                    ps.setTimestamp(3, Timestamp.from(trialExpires))
                    ps.executeUpdate()
                }

                // 4. Initialize AI Credit Wallet with 1,000 trial credits
                c.prepareStatement("""
                    INSERT INTO ai_credit_wallets (
                        id, tenant_id, subscription_balance, topup_balance, bonus_balance,
                        reserved_balance, used_balance, expired_balance, is_unlimited, updated_at
                    ) VALUES (
                        gen_random_uuid(), ?, 1000, 0, 0, 0, 0, 0, false, now()
                    )
                    ON CONFLICT (tenant_id) DO UPDATE
                    SET subscription_balance = 1000, is_unlimited = false, updated_at = now()
                """.trimIndent()).use { ps ->
                    ps.setString(1, generatedTenantId)
                    ps.executeUpdate()
                }

                // 5. Update prospect_registrations
                c.prepareStatement("""
                    UPDATE prospect_registrations
                    SET trial_selection_status = 'trial_activated',
                        activated_tenant_id = ?,
                        contacted_by_admin_id = ?::uuid,
                        contacted_at = now(),
                        updated_at = now()
                    WHERE id = ?::uuid
                """.trimIndent()).use { ps ->
                    ps.setString(1, generatedTenantId)
                    ps.setString(2, adminId)
                    ps.setString(3, id)
                    ps.executeUpdate()
                }

                c.commit()
            } catch (e: Exception) {
                c.rollback()
                throw e
            }
        }

        ActivateTrialResponse(
            success = true,
            prospectId = id,
            tenantId = generatedTenantId,
            companyName = prospect.companyName,
            adminEmail = prospect.email,
            planCode = "trial",
            trialExpiresAt = trialExpires.toString(),
            initialCredits = 1000,
            message = "Tenant trial 7 hari berhasil diaktivasi untuk ${prospect.companyName}. Email onboarding dikirim ke ${prospect.email}."
        )
    }

    suspend fun getAnalytics(): ProspectAnalyticsResponse = withContext(Dispatchers.IO) {
        val conn = dbManager.getConnection() ?: return@withContext ProspectAnalyticsResponse(
            totalRegistered = 0,
            selectedForTrialCount = 0,
            activatedTrialCount = 0,
            scheduledMeetingCount = 0,
            breakdownByInterestOption = emptyMap(),
            breakdownByPlan = emptyMap(),
            breakdownByIndustry = emptyMap(),
            breakdownByCompanySize = emptyMap()
        )

        var total = 0
        var selectedTrial = 0
        var activatedTrial = 0
        var scheduledMeeting = 0
        val byOption = mutableMapOf<String, Int>()
        val byPlan = mutableMapOf<String, Int>()
        val byIndustry = mutableMapOf<String, Int>()
        val bySize = mutableMapOf<String, Int>()

        conn.use { c ->
            // Total & status counts
            c.createStatement().use { stmt ->
                val rs = stmt.executeQuery("""
                    SELECT 
                        COUNT(*) AS total,
                        COUNT(*) FILTER (WHERE trial_selection_status = 'selected_for_trial') AS selected_trial,
                        COUNT(*) FILTER (WHERE trial_selection_status = 'trial_activated') AS activated_trial,
                        COUNT(*) FILTER (WHERE meeting_status = 'scheduled') AS scheduled_meeting
                    FROM prospect_registrations
                """.trimIndent())
                if (rs.next()) {
                    total = rs.getInt("total")
                    selectedTrial = rs.getInt("selected_trial")
                    activatedTrial = rs.getInt("activated_trial")
                    scheduledMeeting = rs.getInt("scheduled_meeting")
                }
            }

            // By Interest Option
            c.createStatement().use { stmt ->
                val rs = stmt.executeQuery("""
                    SELECT interest_option, COUNT(*) AS cnt
                    FROM prospect_registrations
                    GROUP BY interest_option
                """.trimIndent())
                while (rs.next()) {
                    val k = rs.getString("interest_option") ?: "unknown"
                    byOption[k] = rs.getInt("cnt")
                }
            }

            // By Plan
            c.createStatement().use { stmt ->
                val rs = stmt.executeQuery("""
                    SELECT COALESCE(cp.plan_name, 'No Plan Specified') AS plan_name, COUNT(*) AS cnt
                    FROM prospect_registrations p
                    LEFT JOIN commercial_plans cp ON cp.id = p.interested_plan_id
                    GROUP BY cp.plan_name
                """.trimIndent())
                while (rs.next()) {
                    val k = rs.getString("plan_name") ?: "No Plan Specified"
                    byPlan[k] = rs.getInt("cnt")
                }
            }

            // By Industry
            c.createStatement().use { stmt ->
                val rs = stmt.executeQuery("""
                    SELECT COALESCE(ic.industry_name, 'Lainnya / Belum Dikategorikan') AS industry_name, COUNT(*) AS cnt
                    FROM prospect_registrations p
                    LEFT JOIN industry_catalog ic ON ic.id = p.industry_category_id
                    GROUP BY ic.industry_name
                """.trimIndent())
                while (rs.next()) {
                    val k = rs.getString("industry_name") ?: "Lainnya"
                    byIndustry[k] = rs.getInt("cnt")
                }
            }

            // By Company Size
            c.createStatement().use { stmt ->
                val rs = stmt.executeQuery("""
                    SELECT COALESCE(company_size_range, 'Tidak Disebutkan') AS size_range, COUNT(*) AS cnt
                    FROM prospect_registrations
                    GROUP BY company_size_range
                """.trimIndent())
                while (rs.next()) {
                    val k = rs.getString("size_range") ?: "Tidak Disebutkan"
                    bySize[k] = rs.getInt("cnt")
                }
            }
        }

        ProspectAnalyticsResponse(
            totalRegistered = total,
            selectedForTrialCount = selectedTrial,
            maxTrialQuota = 36,
            activatedTrialCount = activatedTrial,
            scheduledMeetingCount = scheduledMeeting,
            breakdownByInterestOption = byOption,
            breakdownByPlan = byPlan,
            breakdownByIndustry = byIndustry,
            breakdownByCompanySize = bySize
        )
    }

    private fun mapResultSetToDto(rs: ResultSet): ProspectRegistrationDto {
        return ProspectRegistrationDto(
            id = rs.getString("id"),
            fullName = rs.getString("full_name"),
            email = rs.getString("email"),
            phoneNumber = rs.getString("phone_number"),
            whatsappNumber = rs.getString("whatsapp_number"),
            address = rs.getString("address"),
            companyName = rs.getString("company_name"),
            jobTitle = rs.getString("job_title"),
            industryCategoryId = rs.getString("industry_category_id"),
            industryName = try { rs.getString("industry_name") } catch (_: Exception) { null },
            companySizeRange = rs.getString("company_size_range"),
            interestOption = rs.getString("interest_option"),
            interestedPlanId = rs.getString("interested_plan_id"),
            interestedPlanName = try { rs.getString("plan_name") } catch (_: Exception) { null },
            trialSelectionStatus = rs.getString("trial_selection_status") ?: "not_selected",
            meetingStatus = rs.getString("meeting_status") ?: "not_scheduled",
            meetingScheduledAt = rs.getTimestamp("meeting_scheduled_at")?.toInstant()?.toString(),
            adminNotes = rs.getString("admin_notes"),
            contactedByAdminId = rs.getString("contacted_by_admin_id"),
            contactedAt = rs.getTimestamp("contacted_at")?.toInstant()?.toString(),
            activatedTenantId = rs.getString("activated_tenant_id"),
            ipAddress = rs.getString("ip_address"),
            submittedAt = rs.getTimestamp("submitted_at")?.toInstant()?.toString() ?: Instant.now().toString(),
            updatedAt = rs.getTimestamp("updated_at")?.toInstant()?.toString()
        )
    }
}
