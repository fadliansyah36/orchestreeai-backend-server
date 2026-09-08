package ai.orchestree.backend

import ai.orchestree.backend.channels.ChannelAccountConfig
import ai.orchestree.backend.channels.ChannelGateway
import ai.orchestree.backend.channels.ChannelOperationMode
import ai.orchestree.backend.channels.ChannelType
import ai.orchestree.backend.database.repositories.workforce.ProactiveCollaborationScopeRepository
import ai.orchestree.backend.database.repositories.workforce.StaffProfile
import ai.orchestree.backend.database.repositories.workforce.StaffProfileRepository
import ai.orchestree.backend.intelligence.ManagementQueryEngine
import ai.orchestree.backend.models.ProactiveChannel
import ai.orchestree.backend.models.ProactiveNotifType
import ai.orchestree.backend.models.ProactiveSubscriptionDto
import ai.orchestree.backend.scheduler.jobs.ProactiveDailyReportJob
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProactiveCollaborationScopeTest {

    private val scopeRepo = ProactiveCollaborationScopeRepository.defaultInstance
    private val staffProfileRepo = StaffProfileRepository.defaultInstance
    private val proactiveJob = ProactiveDailyReportJob()
    private val managementQueryEngine = ManagementQueryEngine(scopeRepo = scopeRepo)
    private val channelGateway = ChannelGateway(staffProfileRepo = staffProfileRepo)

    @Test
    fun testDetermineProactiveScope_ForOwnerAndDireksi_ReturnsExecutiveFullSummary() = runBlocking {
        // Owner scope
        val ownerScope = scopeRepo.determineProactiveScope(
            staffId = "usr-owner-01",
            explicitJobLevel = "owner",
            explicitDepartment = "executive"
        )

        assertEquals("executive_full_summary", ownerScope.scopeType)
        assertNull(ownerScope.collaboratingAiJobTitleIds, "Owner should not be restricted to fixed list of AI titles")
        assertTrue(ownerScope.collaboratingAgentNames.first().contains("Chief of Staff"))

        // Direksi scope
        val direksiScope = scopeRepo.determineProactiveScope(
            staffId = "usr-direksi-02",
            explicitJobLevel = "direksi",
            explicitDepartment = "executive"
        )
        assertEquals("executive_full_summary", direksiScope.scopeType)
        assertNull(direksiScope.collaboratingAiJobTitleIds)
    }

    @Test
    fun testDetermineProactiveScope_ForDepartmentStaff_ReturnsDepartmentScoped() = runBlocking {
        // Sales Staff
        val salesScope = scopeRepo.determineProactiveScope(
            staffId = "usr-sales-01",
            explicitJobLevel = "staff",
            explicitDepartment = "sales"
        )
        assertEquals("department_scoped", salesScope.scopeType)
        assertTrue(salesScope.collaboratingAiJobTitleIds?.contains("sales") == true)
        assertTrue(salesScope.collaboratingAiJobTitleIds?.contains("marketing") == true)
        assertTrue(salesScope.collaboratingAgentNames.any { it.contains("Sales") })

        // HR Staff
        val hrScope = scopeRepo.determineProactiveScope(
            staffId = "usr-hr-01",
            explicitJobLevel = "staff",
            explicitDepartment = "hr"
        )
        assertEquals("department_scoped", hrScope.scopeType)
        assertTrue(hrScope.collaboratingAiJobTitleIds?.contains("hr_recruitment") == true)
        assertFalse(hrScope.collaboratingAiJobTitleIds?.contains("sales") == true)

        // Finance Staff
        val financeScope = scopeRepo.determineProactiveScope(
            staffId = "usr-fin-01",
            explicitJobLevel = "staff",
            explicitDepartment = "finance"
        )
        assertEquals("department_scoped", financeScope.scopeType)
        assertTrue(financeScope.collaboratingAiJobTitleIds?.contains("finance") == true)
    }

    @Test
    fun testRunProactiveJob_StaffSales_ContainsOnlySalesInsightsAndNoFinanceCrossDept() = runBlocking {
        // Setup subscription for Sales staff
        val salesStaffId = "usr-sales-test-01"
        scopeRepo.determineProactiveScope(
            staffId = salesStaffId,
            explicitJobLevel = "staff",
            explicitDepartment = "sales"
        )

        val sub = ProactiveSubscriptionDto(
            id = "sub-sales-01",
            tenantId = "tenant-001",
            staffId = salesStaffId,
            staffName = "Budi Sales",
            staffRole = "Staff Sales",
            channel = ProactiveChannel.WHATSAPP,
            destinationNumber = "+628120000001",
            enabledNotifTypes = listOf(ProactiveNotifType.DAILY_REPORT_PAGI),
            sendTimes = listOf("08:00")
        )

        val targetInternalChannel = ChannelAccountConfig(
            accountId = "acc-internal-01",
            tenantId = "tenant-001",
            channelType = ChannelType.WHATSAPP,
            operationMode = ChannelOperationMode.AI_AUTOPILOT
        )

        val result = proactiveJob.runProactiveJob(sub = sub, targetChannelAccount = targetInternalChannel, proactiveScopeRepo = scopeRepo)

        assertTrue(result.success)
        assertEquals("department_scoped", result.scopeType)
        assertTrue(result.messageContent.contains("DAILY BRIEFING - DEPARTEMEN SALES"))
        assertTrue(result.messageContent.contains("AI Sales Agent"))
        // Strict scope check: MUST NOT contain cross-department KPI or Chief of Staff Executive Briefing
        assertFalse(result.messageContent.contains("EXECUTIVE BRIEFING - AI CHIEF OF STAFF"))
        assertFalse(result.messageContent.contains("AI Finance & Accounting"))
        assertFalse(result.messageContent.contains("Cash flow harian"))
    }

    @Test
    fun testRunProactiveJob_Owner_ContainsChiefOfStaffExecutiveBriefing() = runBlocking {
        val ownerStaffId = "usr-owner-test-01"
        scopeRepo.determineProactiveScope(
            staffId = ownerStaffId,
            explicitJobLevel = "owner",
            explicitDepartment = "executive"
        )

        val sub = ProactiveSubscriptionDto(
            id = "sub-owner-01",
            tenantId = "tenant-001",
            staffId = ownerStaffId,
            staffName = "Bapak Hendra (Owner)",
            staffRole = "Owner",
            channel = ProactiveChannel.TELEGRAM,
            destinationNumber = "12345678",
            enabledNotifTypes = listOf(ProactiveNotifType.OWNER_WEEKLY_BRIEF),
            sendTimes = listOf("08:00")
        )

        val targetInternalChannel = ChannelAccountConfig(
            accountId = "acc-internal-02",
            tenantId = "tenant-001",
            channelType = ChannelType.TELEGRAM,
            operationMode = ChannelOperationMode.AI_AUTOPILOT
        )

        val result = proactiveJob.runProactiveJob(sub = sub, targetChannelAccount = targetInternalChannel, proactiveScopeRepo = scopeRepo)

        assertTrue(result.success)
        assertEquals("executive_full_summary", result.scopeType)
        assertTrue(result.messageContent.contains("EXECUTIVE BRIEFING - AI CHIEF OF STAFF"))
        assertTrue(result.messageContent.contains("Sintesis Eksekutif"))
        assertTrue(result.messageContent.contains("Sales & Commercial"))
        assertTrue(result.messageContent.contains("Finance & Kas"))
    }

    @Test
    fun testTwoWayWebhook_StaffSalesOutOfScopeQuery_PolitelyRejected() = runBlocking {
        val salesStaffId = "staff-sales-webhook"
        staffProfileRepo.save(
            StaffProfile(
                id = salesStaffId,
                userId = salesStaffId,
                tenantId = "tenant-001",
                departmentId = "sales",
                jobTitle = "Sales Representative",
                phone = "+62812333444",
                telegramChatId = "555666777"
            )
        )
        scopeRepo.determineProactiveScope(salesStaffId, explicitJobLevel = "staff", explicitDepartment = "sales")

        // Inbound out-of-scope query: Staff Sales asking about company financial profits
        val inboundResult = channelGateway.handleInboundProactiveChannelMessage(
            tenantId = "tenant-001",
            channelType = "whatsapp",
            senderId = "+62812333444",
            senderName = "Sales Rep",
            messageText = "Berapa total laba bersih dan laporan keuangan perusahaan kuartal ini?"
        )

        assertTrue(inboundResult.processed)
        assertEquals("MANAGEMENT_QUERY", inboundResult.intent)
        assertTrue(
            inboundResult.replyText.contains("Maaf, informasi ini di luar cakupan departemen Anda"),
            "Out of scope query for staff must be rejected with the mandated polite message"
        )
    }

    @Test
    fun testTwoWayWebhook_OwnerManagementQuery_AllowedFullCrossDepartmentAnswer() = runBlocking {
        val ownerStaffId = "staff-owner-webhook"
        staffProfileRepo.save(
            StaffProfile(
                id = ownerStaffId,
                userId = ownerStaffId,
                tenantId = "tenant-001",
                departmentId = "executive",
                jobTitle = "Owner / Direksi",
                phone = "+6281999888",
                telegramChatId = "999888777"
            )
        )
        scopeRepo.determineProactiveScope(ownerStaffId, explicitJobLevel = "owner", explicitDepartment = "executive")

        val inboundResult = channelGateway.handleInboundProactiveChannelMessage(
            tenantId = "tenant-001",
            channelType = "telegram",
            senderId = "999888777",
            senderName = "Direktur Utama",
            messageText = "Bagaimana overview laba dan revenue seluruh performa tim bulan ini?"
        )

        assertTrue(inboundResult.processed)
        assertEquals("MANAGEMENT_QUERY", inboundResult.intent)
        assertFalse(
            inboundResult.replyText.contains("Maaf, informasi ini di luar cakupan departemen Anda"),
            "Owner query must never be rejected for cross-department information"
        )
        assertTrue(inboundResult.replyText.length > 20)
    }
}
