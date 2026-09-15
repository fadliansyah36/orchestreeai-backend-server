# DOKUMEN METHOD & ENDPOINT FINAL (GREP-CONFIRMED)
> **Definitive Reference Document**: Dihasilkan secara langsung dari verifikasi grep kode sumber aktual Ktor backend server (`ai.orchestree.backend`). Dokumen ini merefleksikan seluruh perbaikan Fase 1-5 dan menjadi rujukan tunggal absolut bagi tim Aplikasi Client, Android, dan Admin Dashboard.

## Ringkasan Eksekutif & Statistik Endpoint
- **Total Endpoint Terverifikasi (Grep-Confirmed)**: **335 Endpoint**
- **Total Domain Operasional**: **16 Domain**

| Domain | File Sumber | Jumlah Endpoint |
|---|---|:---:|
| 1. Core Infrastructure & Webhook Gateways | `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/plugins/Routing.kt` | **10** |
| 2. Core Authentication & Profile Lifecycle | `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AuthRoutes.kt` | **6** |
| 3. Core Master Data & Structural Role Catalog | `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/MasterDataRoutes.kt` | **9** |
| 4. Core Biometric Presence & Liveness | `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/PresenceRoutes.kt` | **6** |
| 5. Core Attendance & Geofencing Intelligence | `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AttendanceRoutes.kt` | **5** |
| 6. Core Prospect Registration & Enterprise Trials | `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/prospect/ProspectRoutes.kt` | **6** |
| 7. Tenant & Hybrid Workforce Management | `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt` | **46** |
| 8. Orchestration & Autonomous Workflow DAG | `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OrchestrationRoutes.kt` | **14** |
| 9. AI Chat & Brain Knowledge RAG | `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/ChatRoutes.kt` | **6** |
| 10. Universal Selection & Autonomous Ranking | `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/SelectionRoutes.kt` | **22** |
| 11. Generative Studio & Brand Asset Management | `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/GenerativeStudioRoutes.kt` | **8** |
| 12. Enterprise Governance, Context Fabric & Chief of Staff | `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt` | **41** |
| 13. Autonomous Memory Consolidation & Decay | `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/MemoryRoutes.kt` | **6** |
| 14. Omnichannel Sales, CRM & Channel Gateway | `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt` | **28** |
| 15. Commercial Billing, Quota & Dunning | `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt` | **30** |
| 16. Super Admin, Operations & Security Command | `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt` | **92** |
| **TOTAL** | | **335** |

---

## 1. Core Infrastructure & Webhook Gateways
- **File Sumber**: `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/plugins/Routing.kt`
- **Jumlah Endpoint**: **10**

### 1. `GET` /api/v1/
- **Grep Confirmation**: Line 61 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/plugins/Routing.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `"OrchestreeAI Enterprise Autonomous AI Workforce Server - Running"`
- **Status Engine Terhubung**: Stateless / Infrastructure Gateway
- **Tabel Supabase Terpengaruh**: Stateless / System Info

### 2. `GET` /api/v1/health
- **Grep Confirmation**: Line 65 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/plugins/Routing.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HealthStatusResponse`: `(val status: String, val service: String, val timestamp: Long)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke HealthEngine (Component Health Probes)
- **Tabel Supabase Terpengaruh**: Stateless / Live Health Diagnostics

### 3. `GET` /api/v1/api/health
- **Grep Confirmation**: Line 75 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/plugins/Routing.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HealthStatusResponse`: `(val status: String, val service: String, val timestamp: Long)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke HealthEngine (Component Health Probes)
- **Tabel Supabase Terpengaruh**: Stateless / Live Health Diagnostics

### 4. `GET` /api/v1/api/v1/platform-assets/icon-logo
- **Grep Confirmation**: Line 96 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/plugins/Routing.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, mapOf("platformIconLogoUrl" to (url ?: ""`
- **Status Engine Terhubung**: Stateless / Infrastructure Gateway
- **Tabel Supabase Terpengaruh**: Stateless / System Info

### 5. `POST` /api/v1/api/v1/payments/webhook/{gateway}
- **Grep Confirmation**: Line 110 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/plugins/Routing.kt`
- **Header Wajib**: `X-Signature: <signature-hash>`
- **Request Body Schema**: `String (raw text / webhook payload)`
- **Response Body Schema**: `HttpStatusCode.OK, mapOf("status" to "processed", "order_id" to orderId, "gateway" to gateway`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ChannelGateway & WebhookEventProcessor
- **Tabel Supabase Terpengaruh**: `channel_webhook_events` (INSERT), `orders` (SELECT/UPDATE)

### 6. `POST` /api/v1/api/v1/shipments/webhook/{courier}
- **Grep Confirmation**: Line 163 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/plugins/Routing.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: `String (raw text / webhook payload)`
- **Response Body Schema**: `HttpStatusCode.OK, mapOf("status" to "processed", "courier" to courier`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ChannelGateway & WebhookEventProcessor
- **Tabel Supabase Terpengaruh**: `channel_webhook_events` (INSERT), `orders` (SELECT/UPDATE)

### 7. `POST` /api/v1/api/v1/webhooks/whatsapp
- **Grep Confirmation**: Line 207 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/plugins/Routing.kt`
- **Header Wajib**: `X-Hub-Signature-256: <signature-hash>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `String (raw text / webhook payload)`
- **Response Body Schema**: `HttpStatusCode.Unauthorized, mapOf("error" to (result.errorMessage ?: "Invalid WhatsApp/Meta webh...`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ChannelGateway & WebhookEventProcessor
- **Tabel Supabase Terpengaruh**: `channel_webhook_events` (INSERT), `orders` (SELECT/UPDATE)

### 8. `POST` /api/v1/api/v1/webhooks/telegram
- **Grep Confirmation**: Line 208 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/plugins/Routing.kt`
- **Header Wajib**: `X-Telegram-Bot-Api-Secret-Token: <secret-token>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `String (raw text / webhook payload)`
- **Response Body Schema**: `HttpStatusCode.Unauthorized, mapOf("error" to (result.errorMessage ?: "Invalid Telegram webhook s...`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ChannelGateway & WebhookEventProcessor
- **Tabel Supabase Terpengaruh**: `channel_webhook_events` (INSERT), `orders` (SELECT/UPDATE)

### 9. `POST` /api/v1/api/v1/webhook/whatsapp
- **Grep Confirmation**: Line 212 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/plugins/Routing.kt`
- **Header Wajib**: `X-Hub-Signature-256: <signature-hash>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `String (raw text / webhook payload)`
- **Response Body Schema**: `HttpStatusCode.Unauthorized, mapOf("error" to (result.errorMessage ?: "Invalid WhatsApp/Meta webh...`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ChannelGateway & WebhookEventProcessor
- **Tabel Supabase Terpengaruh**: `channel_webhook_events` (INSERT), `orders` (SELECT/UPDATE)

### 10. `POST` /api/v1/api/v1/webhook/telegram
- **Grep Confirmation**: Line 213 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/plugins/Routing.kt`
- **Header Wajib**: `X-Telegram-Bot-Api-Secret-Token: <secret-token>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `String (raw text / webhook payload)`
- **Response Body Schema**: `HttpStatusCode.Unauthorized, mapOf("error" to (result.errorMessage ?: "Invalid Telegram webhook s...`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ChannelGateway & WebhookEventProcessor
- **Tabel Supabase Terpengaruh**: `channel_webhook_events` (INSERT), `orders` (SELECT/UPDATE)

## 2. Core Authentication & Profile Lifecycle
- **File Sumber**: `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AuthRoutes.kt`
- **Jumlah Endpoint**: **6**

### 1. `POST` /auth/login
- **Grep Confirmation**: Line 74 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AuthRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`, `User-Agent: <user-id>`, `X-Forwarded-For: <client-ip>`, `X-Forwarded-For: <ip-address>`, `X-Tenant-ID: <tenant-uuid>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `LoginApiRequest`: `(val email: String, val password: String? = null, val tenantId: String? = null)`
- **Response Body Schema**: `LoginApiResponse`: `(val accessToken: String, val refreshToken: String, val tokenType: String = "Bearer", val expiresInSeconds: Long = 3600, val userId: String, val tenantId: String)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke AuthService & SupabaseAuthGateway
- **Tabel Supabase Terpengaruh**: `users` (SELECT), `user_sessions` (INSERT/UPDATE)

### 2. `POST` /auth/refresh
- **Grep Confirmation**: Line 173 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AuthRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`, `X-Tenant-ID: <tenant-uuid>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `RefreshApiRequest`: `(val refreshToken: String)`
- **Response Body Schema**: `LoginApiResponse`: `(val accessToken: String, val refreshToken: String, val tokenType: String = "Bearer", val expiresInSeconds: Long = 3600, val userId: String, val tenantId: String)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke AuthService & SupabaseAuthGateway
- **Tabel Supabase Terpengaruh**: `users` (SELECT/UPDATE)

### 3. `GET` /auth/sessions
- **Grep Confirmation**: Line 212 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AuthRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `User-Agent: <user-id>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, sessions`
- **Status Engine Terhubung**: Ya — Terkoneksi ke AuthService & SupabaseAuthGateway
- **Tabel Supabase Terpengaruh**: `users` (SELECT/UPDATE)

### 4. `POST` /auth/sessions/revoke/{id}
- **Grep Confirmation**: Line 277 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AuthRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, mapOf("status" to "revoked", "id" to sessionId, "success" to revoked`
- **Status Engine Terhubung**: Ya — Terkoneksi ke AuthService & SupabaseAuthGateway
- **Tabel Supabase Terpengaruh**: `users` (SELECT/UPDATE)

### 5. `GET` /auth/profile
- **Grep Confirmation**: Line 293 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AuthRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-ID: <tenant-uuid>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, userProfile!!`
- **Status Engine Terhubung**: Ya — Terkoneksi ke AuthService & SupabaseAuthGateway
- **Tabel Supabase Terpengaruh**: `users` (SELECT/UPDATE), `tenants` (SELECT)

### 6. `POST` /auth/profile
- **Grep Confirmation**: Line 362 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AuthRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `UserProfileDto`: `(val id: String, val tenantId: String, val email: String, val name: String, val role: UserRole, val capabilities: List<String>, val departmentName: String = "")`
- **Response Body Schema**: `HttpStatusCode.OK, req`
- **Status Engine Terhubung**: Ya — Terkoneksi ke AuthService & SupabaseAuthGateway
- **Tabel Supabase Terpengaruh**: `users` (SELECT/UPDATE), `tenants` (SELECT)

## 3. Core Master Data & Structural Role Catalog
- **File Sumber**: `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/MasterDataRoutes.kt`
- **Jumlah Endpoint**: **9**

### 1. `GET` /api/v1/public/department-categories
- **Grep Confirmation**: Line 16 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/MasterDataRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, items`
- **Status Engine Terhubung**: Ya — Terkoneksi ke MasterDataCatalogRepository
- **Tabel Supabase Terpengaruh**: `master_data_catalog` (SELECT)

### 2. `POST` /api/v1/public/department-categories
- **Grep Confirmation**: Line 22 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/MasterDataRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `DepartmentCategoryRecord`: `(val id: String, val category_code: String, val category_name: String, val description: String? = null, val icon_key: String? = null, val is_active: Boolean = true)`
- **Response Body Schema**: `HttpStatusCode.Created, mapOf("status" to "CREATED", "category_code" to record.category_code`
- **Status Engine Terhubung**: Ya — Terkoneksi ke MasterDataCatalogRepository
- **Tabel Supabase Terpengaruh**: `master_data_catalog` (SELECT)

### 3. `GET` /api/v1/public/industry-catalog
- **Grep Confirmation**: Line 29 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/MasterDataRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, items`
- **Status Engine Terhubung**: Ya — Terkoneksi ke MasterDataCatalogRepository
- **Tabel Supabase Terpengaruh**: `master_data_catalog` (SELECT)

### 4. `GET` /api/v1/public/job-level-catalog
- **Grep Confirmation**: Line 35 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/MasterDataRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, items`
- **Status Engine Terhubung**: Ya — Terkoneksi ke MasterDataCatalogRepository
- **Tabel Supabase Terpengaruh**: `master_data_catalog` (SELECT)

### 5. `GET` /api/v1/public/job-sub-title-catalog
- **Grep Confirmation**: Line 41 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/MasterDataRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, items`
- **Status Engine Terhubung**: Ya — Terkoneksi ke MasterDataCatalogRepository
- **Tabel Supabase Terpengaruh**: `master_data_catalog` (SELECT)

### 6. `GET` /api/v1/tenants/{id}/ai-job-titles
- **Grep Confirmation**: Line 53 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/MasterDataRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, items`
- **Status Engine Terhubung**: Ya — Terkoneksi ke MasterDataCatalogRepository
- **Tabel Supabase Terpengaruh**: `master_data_catalog` (SELECT)

### 7. `POST` /api/v1/tenants/{id}/ai-job-titles
- **Grep Confirmation**: Line 60 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/MasterDataRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `AiJobTitleRecord`: `(val id: String, val job_code: String, val job_name: String, val icon_key: String? = null, val star_rating: Int = 4, val description: String, val maps_to_persona_type: String, val is_top_coordinator: Boolean = false, val relevant_department_category_id: String? = null)`
- **Response Body Schema**: `HttpStatusCode.Created, mapOf("status" to "CREATED", "job_code" to record.job_code`
- **Status Engine Terhubung**: Ya — Terkoneksi ke MasterDataCatalogRepository
- **Tabel Supabase Terpengaruh**: `master_data_catalog` (SELECT)

### 8. `GET` /api/v1/tenants/{id}/ai-job-titles/{jobTitleId}/structural-roles
- **Grep Confirmation**: Line 67 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/MasterDataRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, items`
- **Status Engine Terhubung**: Ya — Terkoneksi ke MasterDataCatalogRepository
- **Tabel Supabase Terpengaruh**: `ai_structural_roles` (SELECT), `master_data` (SELECT)

### 9. `GET` /api/v1/tenants/{id}/ai-job-titles/{jobTitleId}/available-skills
- **Grep Confirmation**: Line 75 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/MasterDataRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, items`
- **Status Engine Terhubung**: Ya — Terkoneksi ke MasterDataCatalogRepository
- **Tabel Supabase Terpengaruh**: `master_data_catalog` (SELECT)

## 4. Core Biometric Presence & Liveness
- **File Sumber**: `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/PresenceRoutes.kt`
- **Jumlah Endpoint**: **6**

### 1. `POST` /api/v1/presence/enroll
- **Grep Confirmation**: Line 31 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/PresenceRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `PresenceEnrollRequest`: `(val userId: String, val method: String? = null, // "FACE", "FINGERPRINT" val faceEmbedding: List<Float>? = null, val deviceId: String? = null, val isEnabled: Boolean = true)`
- **Response Body Schema**: `HttpStatusCode.OK, response`
- **Status Engine Terhubung**: Ya — Terkoneksi ke BiometricLivenessEngine & AntiSpoofingValidator
- **Tabel Supabase Terpengaruh**: `biometric_verification_logs` (SELECT)

### 2. `GET` /api/v1/presence/requirement-check
- **Grep Confirmation**: Line 58 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/PresenceRoutes.kt`
- **Header Wajib**: `X-Device-Id: <required-value>`, `X-Forwarded-For: <client-ip>`, `X-Forwarded-For: <ip-address>`, `X-User-Id: <user-id>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, result`
- **Status Engine Terhubung**: Ya — Terkoneksi ke BiometricLivenessEngine & AntiSpoofingValidator
- **Tabel Supabase Terpengaruh**: `biometric_verification_logs` (SELECT)

### 3. `POST` /api/v1/presence/verify
- **Grep Confirmation**: Line 98 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/PresenceRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`, `X-Device-Id: <required-value>`
- **Request Body Schema**: `PresenceVerifyRequest`: `(val userId: String, val checkType: String = "CHECK_IN", // "CHECK_IN", "CHECK_OUT", "LOGIN" val methodUsed: String, // "FACE", "FINGERPRINT" val biometricSuccess: Boolean? = null, // for fingerprint from BiometricPrompt val faceEmbedding: List<Float>? = null, // on-device generated face embedding val deviceId: String? = null, val ipAddress: String? = null, val locationApprox: String? = null)`
- **Response Body Schema**: `HttpStatusCode.OK, response`
- **Status Engine Terhubung**: Ya — Terkoneksi ke BiometricLivenessEngine & AntiSpoofingValidator
- **Tabel Supabase Terpengaruh**: `biometric_verification_logs` (INSERT), `users` (SELECT)

### 4. `GET` /api/v1/presence/enrollment
- **Grep Confirmation**: Line 131 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/PresenceRoutes.kt`
- **Header Wajib**: `X-User-Id: <user-id>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, enrollment`
- **Status Engine Terhubung**: Ya — Terkoneksi ke BiometricLivenessEngine & AntiSpoofingValidator
- **Tabel Supabase Terpengaruh**: `biometric_verification_logs` (SELECT)

### 5. `GET` /api/v1/presence/logs
- **Grep Confirmation**: Line 153 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/PresenceRoutes.kt`
- **Header Wajib**: `X-User-Id: <user-id>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, logs`
- **Status Engine Terhubung**: Ya — Terkoneksi ke BiometricLivenessEngine & AntiSpoofingValidator
- **Tabel Supabase Terpengaruh**: `biometric_verification_logs` (SELECT)

### 6. `GET` /api/v1/presence/security-audit-stats
- **Grep Confirmation**: Line 172 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/PresenceRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, stats`
- **Status Engine Terhubung**: Ya — Terkoneksi ke BiometricLivenessEngine & AntiSpoofingValidator
- **Tabel Supabase Terpengaruh**: `biometric_verification_logs` (SELECT)

## 5. Core Attendance & Geofencing Intelligence
- **File Sumber**: `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AttendanceRoutes.kt`
- **Jumlah Endpoint**: **5**

### 1. `POST` /api/v1/attendance/check-in
- **Grep Confirmation**: Line 68 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AttendanceRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `AttendanceCheckInRequest`: `(val userId: String, val tenantId: String = "tenant-default", val latitude: Double, val longitude: Double, val locationName: String = "Headquarters Jakarta", val type: String = "CHECK_IN")`
- **Response Body Schema**: `AttendanceCheckInResponse`: `(val id: String, val status: String = "SUCCESS", val message: String = "Presensi berhasil dicatat di server", val timestamp: Long = System.currentTimeMillis()`
- **Status Engine Terhubung**: Ya — Terkoneksi ke GeofenceEngine & AttendanceAnomalyEngine
- **Tabel Supabase Terpengaruh**: `attendance_records` (INSERT), `work_locations` (SELECT)

### 2. `GET` /api/v1/attendance/anomalies
- **Grep Confirmation**: Line 149 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AttendanceRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `getOrDefault`
- **Status Engine Terhubung**: Ya — Terkoneksi ke GeofenceEngine & AttendanceAnomalyEngine
- **Tabel Supabase Terpengaruh**: `attendance_records` (SELECT/INSERT)

### 3. `GET` /api/v1/attendance/history
- **Grep Confirmation**: Line 155 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AttendanceRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, list`
- **Status Engine Terhubung**: Ya — Terkoneksi ke GeofenceEngine & AttendanceAnomalyEngine
- **Tabel Supabase Terpengaruh**: `attendance_records` (SELECT)

### 4. `GET` /api/v1/tenants/{id}/geofences
- **Grep Confirmation**: Line 198 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AttendanceRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, list`
- **Status Engine Terhubung**: Ya — Terkoneksi ke GeofenceEngine & AttendanceAnomalyEngine
- **Tabel Supabase Terpengaruh**: `attendance_records` (SELECT/INSERT)

### 5. `POST` /api/v1/tenants/{id}/geofences
- **Grep Confirmation**: Line 231 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AttendanceRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `GeofenceCreateRequest`: `(val name: String, val latitude: Double, val longitude: Double, val radiusMeters: Double = 100.0)`
- **Response Body Schema**: `GeofenceItem`: `(val id: String, val tenantId: String, val name: String, val latitude: Double, val longitude: Double, val radiusMeters: Double = 100.0, val isActive: Boolean = true)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke GeofenceEngine & AttendanceAnomalyEngine
- **Tabel Supabase Terpengaruh**: `attendance_records` (SELECT/INSERT)

## 6. Core Prospect Registration & Enterprise Trials
- **File Sumber**: `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/prospect/ProspectRoutes.kt`
- **Jumlah Endpoint**: **6**

### 1. `POST` /api/v1/public/prospect-registration
- **Grep Confirmation**: Line 44 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/prospect/ProspectRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `ProspectRegistrationRequest`: `(val fullName: String, val email: String, val phoneNumber: String, val whatsappNumber: String? = null, val address: String? = null, val companyName: String, val jobTitle: String, val industryCategoryId: String? = null, val companySizeRange: String? = null, val interestOption: String, // 'schedule_meeting_presentation' or 'direct_trial_or_subscription' val interestedPlanId: String? = null, val captchaToken: String? = null)`
- **Response Body Schema**: `ProspectRegistrationResponse`: `(val success: Boolean, val data: ProspectRegistrationDto, val confirmationMessage: String)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ProspectRepository & TrialProvisioningEngine
- **Tabel Supabase Terpengaruh**: `prospects` (SELECT)

### 2. `GET` /api/v1/admin/prospect-registrations
- **Grep Confirmation**: Line 119 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/prospect/ProspectRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, list`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ProspectRepository & TrialProvisioningEngine
- **Tabel Supabase Terpengaruh**: `prospects` (SELECT)

### 3. `GET` /api/v1/admin/prospect-registrations/analytics
- **Grep Confirmation**: Line 138 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/prospect/ProspectRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, analytics`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ProspectRepository & TrialProvisioningEngine
- **Tabel Supabase Terpengaruh**: `prospects` (SELECT)

### 4. `PATCH` /api/v1/admin/prospect-registrations/{id}/select-trial
- **Grep Confirmation**: Line 152 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/prospect/ProspectRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `SelectTrialRequest`: `(val status: String = "selected_for_trial", // 'selected_for_trial', 'not_selected', 'rejected' val adminNotes: String? = null)`
- **Response Body Schema**: `HttpStatusCode.OK, updated`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ProspectRepository & TrialProvisioningEngine
- **Tabel Supabase Terpengaruh**: `prospects` (SELECT)

### 5. `PATCH` /api/v1/admin/prospect-registrations/{id}/schedule-meeting
- **Grep Confirmation**: Line 169 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/prospect/ProspectRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `ScheduleMeetingRequest`: `(val meetingScheduledAt: String, // ISO timestamp or formatted string val meetingStatus: String = "scheduled", val adminNotes: String? = null)`
- **Response Body Schema**: `HttpStatusCode.OK, updated`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ProspectRepository & TrialProvisioningEngine
- **Tabel Supabase Terpengaruh**: `prospects` (SELECT)

### 6. `POST` /api/v1/admin/prospect-registrations/{id}/activate-trial
- **Grep Confirmation**: Line 186 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/prospect/ProspectRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.Created, response`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ProspectRepository & TrialProvisioningEngine
- **Tabel Supabase Terpengaruh**: `prospects` (SELECT)

## 7. Tenant & Hybrid Workforce Management
- **File Sumber**: `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Jumlah Endpoint**: **46**

### 1. `GET` /api/v1/tenants/{id}/dashboard/overview
- **Grep Confirmation**: Line 312 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, overview`
- **Status Engine Terhubung**: Ya — Terkoneksi ke TaskRepository, AgentRepository & TenantDomainRepository
- **Tabel Supabase Terpengaruh**: `tasks` (SELECT/INSERT/UPDATE), `board_columns` (SELECT)

### 2. `GET` /api/v1/tenants/{id}/departments
- **Grep Confirmation**: Line 319 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `getOrDefault`
- **Status Engine Terhubung**: Ya — Terkoneksi ke TaskRepository, AgentRepository & TenantDomainRepository
- **Tabel Supabase Terpengaruh**: `departments` (SELECT/INSERT/UPDATE)

### 3. `POST` /api/v1/tenants/{id}/departments
- **Grep Confirmation**: Line 325 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `DepartmentCreateRequest`: `(val name: String, val description: String = "", val managerUserId: String? = null, val colorTag: String = "#1E6FE0")`
- **Response Body Schema**: `GenericStatusResponse`: `(val status: String, val message: String? = null, val id: String? = null)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke TaskRepository, AgentRepository & TenantDomainRepository
- **Tabel Supabase Terpengaruh**: `departments` (SELECT/INSERT/UPDATE)

### 4. `DELETE` /api/v1/tenants/{id}/departments/{deptId}
- **Grep Confirmation**: Line 343 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `GenericStatusResponse`: `(val status: String, val message: String? = null, val id: String? = null)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke TaskRepository, AgentRepository & TenantDomainRepository
- **Tabel Supabase Terpengaruh**: `departments` (SELECT/INSERT/UPDATE)

### 5. `GET` /api/v1/tenants/{id}/staff
- **Grep Confirmation**: Line 351 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `getOrDefault`
- **Status Engine Terhubung**: Ya — Terkoneksi ke TaskRepository, AgentRepository & TenantDomainRepository
- **Tabel Supabase Terpengaruh**: `users` (SELECT/INSERT/UPDATE), `departments` (SELECT)

### 6. `DELETE` /api/v1/tenants/{id}/staff/{staffId}
- **Grep Confirmation**: Line 357 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `GenericStatusResponse`: `(val status: String, val message: String? = null, val id: String? = null)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke TaskRepository, AgentRepository & TenantDomainRepository
- **Tabel Supabase Terpengaruh**: `users` (SELECT/INSERT/UPDATE), `departments` (SELECT)

### 7. `POST` /api/v1/tenants/{id}/staff
- **Grep Confirmation**: Line 364 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `StaffCreateRequest`: `(val name: String, val email: String, val role: String, val departmentId: String)`
- **Response Body Schema**: `GenericStatusResponse`: `(val status: String, val message: String? = null, val id: String? = null)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke TaskRepository, AgentRepository & TenantDomainRepository
- **Tabel Supabase Terpengaruh**: `users` (SELECT/INSERT/UPDATE), `departments` (SELECT)

### 8. `GET` /api/v1/tenants/{id}/agents
- **Grep Confirmation**: Line 393 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `getOrDefault`
- **Status Engine Terhubung**: Ya — Terkoneksi ke TaskRepository, AgentRepository & TenantDomainRepository
- **Tabel Supabase Terpengaruh**: `ai_agents` (SELECT/INSERT/UPDATE), `agent_skill_confidence` (SELECT)

### 9. `POST` /api/v1/tenants/{id}/agents
- **Grep Confirmation**: Line 399 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `AgentCreateRequest`: `(val name: String, val jobTitleId: String, val departmentId: String, val structuralRoleId: String? = null)`
- **Response Body Schema**: `GenericStatusResponse`: `(val status: String, val message: String? = null, val id: String? = null)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke TaskRepository, AgentRepository & TenantDomainRepository
- **Tabel Supabase Terpengaruh**: `ai_agents` (SELECT/INSERT/UPDATE), `agent_skill_confidence` (SELECT)

### 10. `GET` /api/v1/tenants/{id}/tasks
- **Grep Confirmation**: Line 424 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`, `X-User-Id: <user-id>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, tasks`
- **Status Engine Terhubung**: Ya — Terkoneksi ke TaskRepository, AgentRepository & TenantDomainRepository
- **Tabel Supabase Terpengaruh**: `tasks` (SELECT/INSERT/UPDATE), `board_columns` (SELECT)

### 11. `POST` /api/v1/tenants/{id}/tasks
- **Grep Confirmation**: Line 440 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `Map<String, String`
- **Response Body Schema**: `GenericStatusResponse`: `(val status: String, val message: String? = null, val id: String? = null)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke TaskRepository, AgentRepository & TenantDomainRepository
- **Tabel Supabase Terpengaruh**: `tasks` (SELECT/INSERT/UPDATE), `board_columns` (SELECT)

### 12. `GET` /api/v1/tenants/{id}/boards/{boardId}
- **Grep Confirmation**: Line 463 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, board`
- **Status Engine Terhubung**: Ya — Terkoneksi ke TaskRepository, AgentRepository & TenantDomainRepository
- **Tabel Supabase Terpengaruh**: `tasks` (SELECT/INSERT/UPDATE), `board_columns` (SELECT)

### 13. `GET` /api/v1/tasks
- **Grep Confirmation**: Line 473 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`, `X-User-Id: <user-id>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, tasks`
- **Status Engine Terhubung**: Ya — Terkoneksi ke TaskRepository, AgentRepository & TenantDomainRepository
- **Tabel Supabase Terpengaruh**: `tasks` (SELECT/INSERT/UPDATE), `board_columns` (SELECT)

### 14. `POST` /api/v1/tasks/inbound-channel-message
- **Grep Confirmation**: Line 489 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `InboundChannelMessageApiRequest`: `(val channel: String, // 'telegram', 'whatsapp', 'dashboard' val senderId: String, val senderName: String? = null, val message: String, val tenantId: String = "tenant-default")`
- **Response Body Schema**: `HttpStatusCode.OK, result`
- **Status Engine Terhubung**: Ya — Terkoneksi ke TaskRepository, AgentRepository & TenantDomainRepository
- **Tabel Supabase Terpengaruh**: `tasks` (SELECT/INSERT/UPDATE), `board_columns` (SELECT)

### 15. `GET` /api/v1/tasks/proactive/subscriptions
- **Grep Confirmation**: Line 507 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, subs`
- **Status Engine Terhubung**: Ya — Terkoneksi ke TaskRepository, AgentRepository & TenantDomainRepository
- **Tabel Supabase Terpengaruh**: `tasks` (SELECT/INSERT/UPDATE), `board_columns` (SELECT)

### 16. `GET` /api/v1/tasks/proactive/scope/{staffId}
- **Grep Confirmation**: Line 513 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, scope`
- **Status Engine Terhubung**: Ya — Terkoneksi ke TaskRepository, AgentRepository & TenantDomainRepository
- **Tabel Supabase Terpengaruh**: `tasks` (SELECT/INSERT/UPDATE), `board_columns` (SELECT)

### 17. `POST` /api/v1/tasks/proactive/subscriptions
- **Grep Confirmation**: Line 522 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `ProactiveSubscriptionRequest`: `(val staffId: String, val channel: String, val types: List<String>, val sendTimes: List<String>)`
- **Response Body Schema**: `GenericStatusResponse`: `(val status: String, val message: String? = null, val id: String? = null)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke TaskRepository, AgentRepository & TenantDomainRepository
- **Tabel Supabase Terpengaruh**: `tasks` (SELECT/INSERT/UPDATE), `board_columns` (SELECT)

### 18. `POST` /api/v1/tenants/tasks/inbound-channel-message
- **Grep Confirmation**: Line 537 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `InboundChannelMessageApiRequest`: `(val channel: String, // 'telegram', 'whatsapp', 'dashboard' val senderId: String, val senderName: String? = null, val message: String, val tenantId: String = "tenant-default")`
- **Response Body Schema**: `HttpStatusCode.OK, result`
- **Status Engine Terhubung**: Ya — Terkoneksi ke TaskRepository, AgentRepository & TenantDomainRepository
- **Tabel Supabase Terpengaruh**: `tasks` (SELECT/INSERT/UPDATE), `board_columns` (SELECT)

### 19. `PATCH` /api/v1/tasks/{taskId}/move
- **Grep Confirmation**: Line 556 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `TaskMoveRequest`: `(val toColumn: String = "", val targetColumn: String = "", val version: Int = 1, val expectedVersion: Int = 1, val targetIndex: Int = 0)`
- **Response Body Schema**: `GenericStatusResponse`: `(val status: String, val message: String? = null, val id: String? = null)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke TaskRepository, AgentRepository & TenantDomainRepository
- **Tabel Supabase Terpengaruh**: `tasks` (UPDATE), `task_activity_logs` (INSERT)

### 20. `GET` /api/v1/tasks/{taskId}/checklists
- **Grep Confirmation**: Line 572 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, items`
- **Status Engine Terhubung**: Ya — Terkoneksi ke TaskRepository, AgentRepository & TenantDomainRepository
- **Tabel Supabase Terpengaruh**: `task_checklists` (INSERT/UPDATE)

### 21. `POST` /api/v1/tasks/{taskId}/checklists
- **Grep Confirmation**: Line 578 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `ChecklistItemCreateRequest`: `(val itemText: String, val orderIndex: Int = 0)`
- **Response Body Schema**: `HttpStatusCode.Created, created`
- **Status Engine Terhubung**: Ya — Terkoneksi ke TaskRepository, AgentRepository & TenantDomainRepository
- **Tabel Supabase Terpengaruh**: `task_checklists` (INSERT/UPDATE)

### 22. `PATCH` /api/v1/tasks/{taskId}/checklists/{checklistId}/toggle
- **Grep Confirmation**: Line 592 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, mapOf("success" to success, "checklistId" to checklistId, "isCompleted" to isC...`
- **Status Engine Terhubung**: Ya — Terkoneksi ke TaskRepository, AgentRepository & TenantDomainRepository
- **Tabel Supabase Terpengaruh**: `task_checklists` (INSERT/UPDATE)

### 23. `GET` /api/v1/tasks/{taskId}/activity-log
- **Grep Confirmation**: Line 600 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, logs`
- **Status Engine Terhubung**: Ya — Terkoneksi ke TaskRepository, AgentRepository & TenantDomainRepository
- **Tabel Supabase Terpengaruh**: `tasks` (SELECT/INSERT/UPDATE), `board_columns` (SELECT)

### 24. `PATCH` /api/v1/tasks/{taskId}/description
- **Grep Confirmation**: Line 606 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `TaskDescriptionUpdateRequest`: `(val descriptionRichText: String)`
- **Response Body Schema**: `getOrThrow`
- **Status Engine Terhubung**: Ya — Terkoneksi ke TaskRepository, AgentRepository & TenantDomainRepository
- **Tabel Supabase Terpengaruh**: `tasks` (SELECT/INSERT/UPDATE), `board_columns` (SELECT)

### 25. `GET` /api/v1/tasks/{taskId}/attachments
- **Grep Confirmation**: Line 617 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, items`
- **Status Engine Terhubung**: Ya — Terkoneksi ke TaskRepository, AgentRepository & TenantDomainRepository
- **Tabel Supabase Terpengaruh**: `task_attachments` (INSERT/SELECT)

### 26. `POST` /api/v1/tasks/{taskId}/attachments
- **Grep Confirmation**: Line 623 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `AttachmentCreateRequest`: `(val fileName: String, val fileUrl: String, val fileSizeBytes: Long = 0L, val uploadedBy: String = "staff")`
- **Response Body Schema**: `HttpStatusCode.Created, created`
- **Status Engine Terhubung**: Ya — Terkoneksi ke TaskRepository, AgentRepository & TenantDomainRepository
- **Tabel Supabase Terpengaruh**: `task_attachments` (INSERT/SELECT)

### 27. `GET` /api/v1/intel/competitors
- **Grep Confirmation**: Line 642 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, list`
- **Status Engine Terhubung**: Ya — Terkoneksi ke TaskRepository, AgentRepository & TenantDomainRepository
- **Tabel Supabase Terpengaruh**: `tenants` (SELECT/UPDATE), `work_locations` (SELECT)

### 28. `POST` /api/v1/intel/competitors
- **Grep Confirmation**: Line 682 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `CompetitorTargetRequest`: `(val name: String, val category: String, val urls: List<String>, val frequency: String = "daily", val assignedAgentId: String? = null)`
- **Response Body Schema**: `GenericStatusResponse`: `(val status: String, val message: String? = null, val id: String? = null)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke TaskRepository, AgentRepository & TenantDomainRepository
- **Tabel Supabase Terpengaruh**: `tenants` (SELECT/UPDATE), `work_locations` (SELECT)

### 29. `GET` /api/v1/intel/competitors/{id}/insights
- **Grep Confirmation**: Line 773 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, finalInsights`
- **Status Engine Terhubung**: Ya — Terkoneksi ke TaskRepository, AgentRepository & TenantDomainRepository
- **Tabel Supabase Terpengaruh**: `tenants` (SELECT/UPDATE), `work_locations` (SELECT)

### 30. `GET` /api/v1/intel/world-trends
- **Grep Confirmation**: Line 864 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, trends`
- **Status Engine Terhubung**: Ya — Terkoneksi ke TaskRepository, AgentRepository & TenantDomainRepository
- **Tabel Supabase Terpengaruh**: `tenants` (SELECT/UPDATE), `work_locations` (SELECT)

### 31. `POST` /api/v1/integrations/{platform}/connect
- **Grep Confirmation**: Line 872 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `IntegrationConnectRequest`: `(val authCode: String? = null, val redirectUri: String? = null, val scopes: List<String> = emptyList()`
- **Response Body Schema**: `GenericStatusResponse`: `(val status: String, val message: String? = null, val id: String? = null)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke TaskRepository, AgentRepository & TenantDomainRepository
- **Tabel Supabase Terpengaruh**: `tenants` (SELECT/UPDATE), `work_locations` (SELECT)

### 32. `GET` /api/v1/proactive/subscriptions
- **Grep Confirmation**: Line 884 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, subs`
- **Status Engine Terhubung**: Ya — Terkoneksi ke TaskRepository, AgentRepository & TenantDomainRepository
- **Tabel Supabase Terpengaruh**: `tenants` (SELECT/UPDATE), `work_locations` (SELECT)

### 33. `GET` /api/v1/proactive/scope/{staffId}
- **Grep Confirmation**: Line 890 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, scope`
- **Status Engine Terhubung**: Ya — Terkoneksi ke TaskRepository, AgentRepository & TenantDomainRepository
- **Tabel Supabase Terpengaruh**: `users` (SELECT/INSERT/UPDATE), `departments` (SELECT)

### 34. `POST` /api/v1/proactive/subscriptions
- **Grep Confirmation**: Line 899 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `ProactiveSubscriptionRequest`: `(val staffId: String, val channel: String, val types: List<String>, val sendTimes: List<String>)`
- **Response Body Schema**: `GenericStatusResponse`: `(val status: String, val message: String? = null, val id: String? = null)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke TaskRepository, AgentRepository & TenantDomainRepository
- **Tabel Supabase Terpengaruh**: `tenants` (SELECT/UPDATE), `work_locations` (SELECT)

### 35. `GET` /api/v1/analytics/scores
- **Grep Confirmation**: Line 914 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, response`
- **Status Engine Terhubung**: Ya — Terkoneksi ke TaskRepository, AgentRepository & TenantDomainRepository
- **Tabel Supabase Terpengaruh**: `tenants` (SELECT/UPDATE), `work_locations` (SELECT)

### 36. `GET` /api/v1/performance/reports
- **Grep Confirmation**: Line 923 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, reports`
- **Status Engine Terhubung**: Ya — Terkoneksi ke TaskRepository, AgentRepository & TenantDomainRepository
- **Tabel Supabase Terpengaruh**: `tenants` (SELECT/UPDATE), `work_locations` (SELECT)

### 37. `POST` /api/v1/performance/reports
- **Grep Confirmation**: Line 929 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `CreateWorkReportRequest`: `(val staffId: String, val staffName: String, val reportDate: String, val accomplishments: String, val blockers: String = "", val plannedNext: String = "")`
- **Response Body Schema**: `HttpStatusCode.Created, report`
- **Status Engine Terhubung**: Ya — Terkoneksi ke TaskRepository, AgentRepository & TenantDomainRepository
- **Tabel Supabase Terpengaruh**: `tenants` (SELECT/UPDATE), `work_locations` (SELECT)

### 38. `GET` /api/v1/performance/goals
- **Grep Confirmation**: Line 936 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, goals`
- **Status Engine Terhubung**: Ya — Terkoneksi ke TaskRepository, AgentRepository & TenantDomainRepository
- **Tabel Supabase Terpengaruh**: `tenants` (SELECT/UPDATE), `work_locations` (SELECT)

### 39. `GET` /api/v1/performance/reviews
- **Grep Confirmation**: Line 942 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, reviews`
- **Status Engine Terhubung**: Ya — Terkoneksi ke TaskRepository, AgentRepository & TenantDomainRepository
- **Tabel Supabase Terpengaruh**: `tenants` (SELECT/UPDATE), `work_locations` (SELECT)

### 40. `GET` /api/v1/performance/predictions
- **Grep Confirmation**: Line 948 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, predictions`
- **Status Engine Terhubung**: Ya — Terkoneksi ke TaskRepository, AgentRepository & TenantDomainRepository
- **Tabel Supabase Terpengaruh**: `tenants` (SELECT/UPDATE), `work_locations` (SELECT)

### 41. `GET` /api/v1/performance/executive-briefs
- **Grep Confirmation**: Line 954 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, briefs`
- **Status Engine Terhubung**: Ya — Terkoneksi ke TaskRepository, AgentRepository & TenantDomainRepository
- **Tabel Supabase Terpengaruh**: `tenants` (SELECT/UPDATE), `work_locations` (SELECT)

### 42. `GET` /api/v1/security/anomalies
- **Grep Confirmation**: Line 963 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, anomalies`
- **Status Engine Terhubung**: Ya — Terkoneksi ke TaskRepository, AgentRepository & TenantDomainRepository
- **Tabel Supabase Terpengaruh**: `tenants` (SELECT/UPDATE), `work_locations` (SELECT)

### 43. `GET` /api/v1/security/dsr
- **Grep Confirmation**: Line 969 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, requests`
- **Status Engine Terhubung**: Ya — Terkoneksi ke TaskRepository, AgentRepository & TenantDomainRepository
- **Tabel Supabase Terpengaruh**: `tenants` (SELECT/UPDATE), `work_locations` (SELECT)

### 44. `POST` /api/v1/security/dsr
- **Grep Confirmation**: Line 975 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `CreateDataSubjectRequest`: `(val requestType: String, val requesterEmail: String, val details: String = "")`
- **Response Body Schema**: `HttpStatusCode.Created, created`
- **Status Engine Terhubung**: Ya — Terkoneksi ke TaskRepository, AgentRepository & TenantDomainRepository
- **Tabel Supabase Terpengaruh**: `tenants` (SELECT/UPDATE), `work_locations` (SELECT)

### 45. `GET` /api/v1/attendance/anomalies
- **Grep Confirmation**: Line 985 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, anomalies`
- **Status Engine Terhubung**: Ya — Terkoneksi ke TaskRepository, AgentRepository & TenantDomainRepository
- **Tabel Supabase Terpengaruh**: `tenants` (SELECT/UPDATE), `work_locations` (SELECT)

### 46. `POST` /api/v1/attendance/anomalies/{anomalyId}/resolve
- **Grep Confirmation**: Line 991 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `GenericStatusResponse`: `(val status: String, val message: String? = null, val id: String? = null)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke TaskRepository, AgentRepository & TenantDomainRepository
- **Tabel Supabase Terpengaruh**: `tenants` (SELECT/UPDATE), `work_locations` (SELECT)

## 8. Orchestration & Autonomous Workflow DAG
- **File Sumber**: `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OrchestrationRoutes.kt`
- **Jumlah Endpoint**: **14**

### 1. `POST` /api/v1/orchestration/dispatch
- **Grep Confirmation**: Line 59 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OrchestrationRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `WorkflowDispatchRequest`: `(val workflowDefId: String, val prompt: String, val tenantId: String = "tenant-default", val contextParams: Map<String, String> = emptyMap()`
- **Response Body Schema**: `HttpStatusCode.OK, result`
- **Status Engine Terhubung**: Ya — Terkoneksi ke OrchestrationEngine (Workflow DAG & Autonomous Execution)
- **Tabel Supabase Terpengaruh**: `workflow_executions` (INSERT/UPDATE), `workflow_definitions` (SELECT)

### 2. `POST` /api/v1/orchestration/workflows/run
- **Grep Confirmation**: Line 71 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OrchestrationRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `WorkflowDispatchRequest`: `(val workflowDefId: String, val prompt: String, val tenantId: String = "tenant-default", val contextParams: Map<String, String> = emptyMap()`
- **Response Body Schema**: `HttpStatusCode.OK, result`
- **Status Engine Terhubung**: Ya — Terkoneksi ke OrchestrationEngine (Workflow DAG & Autonomous Execution)
- **Tabel Supabase Terpengaruh**: `workflow_executions` (INSERT/UPDATE), `workflow_definitions` (SELECT)

### 3. `GET` /api/v1/orchestration/status/{executionId}
- **Grep Confirmation**: Line 83 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OrchestrationRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, result`
- **Status Engine Terhubung**: Ya — Terkoneksi ke OrchestrationEngine (Workflow DAG & Autonomous Execution)
- **Tabel Supabase Terpengaruh**: `workflow_executions` (SELECT)

### 4. `GET` /api/v1/orchestration/executions/{executionId}
- **Grep Confirmation**: Line 96 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OrchestrationRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, result`
- **Status Engine Terhubung**: Ya — Terkoneksi ke OrchestrationEngine (Workflow DAG & Autonomous Execution)
- **Tabel Supabase Terpengaruh**: `workflow_executions` (SELECT)

### 5. `GET` /api/v1/orchestration/approvals
- **Grep Confirmation**: Line 109 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OrchestrationRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, pendingList`
- **Status Engine Terhubung**: Ya — Terkoneksi ke OrchestrationEngine (Workflow DAG & Autonomous Execution)
- **Tabel Supabase Terpengaruh**: `pending_approvals` (SELECT/UPDATE), `workflow_executions` (UPDATE)

### 6. `POST` /api/v1/orchestration/approvals/{id}/decision
- **Grep Confirmation**: Line 115 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OrchestrationRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `ApprovalDecisionApiRequest`: `(val decision: String, // APPROVED or REJECTED val approvedBy: String? = null)`
- **Response Body Schema**: `HttpStatusCode.OK, result`
- **Status Engine Terhubung**: Ya — Terkoneksi ke OrchestrationEngine (Workflow DAG & Autonomous Execution)
- **Tabel Supabase Terpengaruh**: `pending_approvals` (SELECT/UPDATE), `workflow_executions` (UPDATE)

### 7. `GET` /api/v1/orchestration/traces
- **Grep Confirmation**: Line 143 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OrchestrationRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, traces`
- **Status Engine Terhubung**: Ya — Terkoneksi ke OrchestrationEngine (Workflow DAG & Autonomous Execution)
- **Tabel Supabase Terpengaruh**: `workflow_definitions` (SELECT), `workflow_executions` (SELECT)

### 8. `GET` /api/v1/orchestration/traces/{executionId}
- **Grep Confirmation**: Line 148 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OrchestrationRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, mapOf( "executionId" to executionId, "traceId" to (spans.firstOrNull(`
- **Status Engine Terhubung**: Ya — Terkoneksi ke OrchestrationEngine (Workflow DAG & Autonomous Execution)
- **Tabel Supabase Terpengaruh**: `workflow_definitions` (SELECT), `workflow_executions` (SELECT)

### 9. `GET` /api/v1/orchestration/traces/by-trace/{traceId}
- **Grep Confirmation**: Line 164 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OrchestrationRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, spans`
- **Status Engine Terhubung**: Ya — Terkoneksi ke OrchestrationEngine (Workflow DAG & Autonomous Execution)
- **Tabel Supabase Terpengaruh**: `workflow_definitions` (SELECT), `workflow_executions` (SELECT)

### 10. `GET` /api/v1/intelligence/confidence/calibration
- **Grep Confirmation**: Line 177 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OrchestrationRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, calibrations`
- **Status Engine Terhubung**: Ya — Terkoneksi ke OrchestrationEngine (Workflow DAG & Autonomous Execution)
- **Tabel Supabase Terpengaruh**: `workflow_definitions` (SELECT), `workflow_executions` (SELECT)

### 11. `POST` /api/v1/intelligence/confidence/calibrate
- **Grep Confirmation**: Line 186 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OrchestrationRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, mapOf( "status" to "CALIBRATED", "tenantId" to tenantId, "bucketsCalibrated" t...`
- **Status Engine Terhubung**: Ya — Terkoneksi ke OrchestrationEngine (Workflow DAG & Autonomous Execution)
- **Tabel Supabase Terpengaruh**: `workflow_definitions` (SELECT), `workflow_executions` (SELECT)

### 12. `GET` /api/v1/intelligence/confidence/audit
- **Grep Confirmation**: Line 197 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OrchestrationRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, report`
- **Status Engine Terhubung**: Ya — Terkoneksi ke OrchestrationEngine (Workflow DAG & Autonomous Execution)
- **Tabel Supabase Terpengaruh**: `workflow_definitions` (SELECT), `workflow_executions` (SELECT)

### 13. `GET` /api/v1/mcp/tools
- **Grep Confirmation**: Line 206 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OrchestrationRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, tools`
- **Status Engine Terhubung**: Ya — Terkoneksi ke OrchestrationEngine (Workflow DAG & Autonomous Execution)
- **Tabel Supabase Terpengaruh**: `workflow_definitions` (SELECT), `workflow_executions` (SELECT)

### 14. `POST` /api/v1/mcp/execute
- **Grep Confirmation**: Line 211 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OrchestrationRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `McpExecuteApiRequest`: `(val toolName: String, val params: Map<String, String>, val tenantId: String = "tenant-default", val callerRole: String = "STAFF_HUMAN")`
- **Response Body Schema**: `HttpStatusCode.OK, result`
- **Status Engine Terhubung**: Ya — Terkoneksi ke OrchestrationEngine (Workflow DAG & Autonomous Execution)
- **Tabel Supabase Terpengaruh**: `workflow_definitions` (SELECT), `workflow_executions` (SELECT)

## 9. AI Chat & Brain Knowledge RAG
- **File Sumber**: `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/ChatRoutes.kt`
- **Jumlah Endpoint**: **6**

### 1. `POST` /api/v1/chat
- **Grep Confirmation**: Line 107 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/ChatRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `ChatApiRequest`: `(val message: String = "", val prompt: String = "", val tenantId: String = "tenant-default", val conversationId: String? = null, val agentId: String? = null, val systemPrompt: String? = null)`
- **Response Body Schema**: `ChatApiResponse`: `(val reply: String, val modelUsed: String, val latencyMs: Long, val conversationId: String, val references: List<String> = emptyList()`
- **Status Engine Terhubung**: Ya — Terkoneksi ke OrchestrationEngine, ModelRouter & CompanyBrainRAG
- **Tabel Supabase Terpengaruh**: `chat_messages` (INSERT/SELECT), `chat_sessions` (INSERT/SELECT), `memory_documents` (SELECT)

### 2. `POST` /api/v1/chat/messages
- **Grep Confirmation**: Line 112 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/ChatRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `ChatApiRequest`: `(val message: String = "", val prompt: String = "", val tenantId: String = "tenant-default", val conversationId: String? = null, val agentId: String? = null, val systemPrompt: String? = null)`
- **Response Body Schema**: `ChatApiResponse`: `(val reply: String, val modelUsed: String, val latencyMs: Long, val conversationId: String, val references: List<String> = emptyList()`
- **Status Engine Terhubung**: Ya — Terkoneksi ke OrchestrationEngine, ModelRouter & CompanyBrainRAG
- **Tabel Supabase Terpengaruh**: `chat_messages` (INSERT/SELECT), `chat_sessions` (INSERT/SELECT), `memory_documents` (SELECT)

### 3. `GET` /api/v1/chat/history/{conversationId}
- **Grep Confirmation**: Line 118 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/ChatRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, history.map { mapOf("role" to it.first, "content" to it.second`
- **Status Engine Terhubung**: Ya — Terkoneksi ke OrchestrationEngine, ModelRouter & CompanyBrainRAG
- **Tabel Supabase Terpengaruh**: `chat_messages` (INSERT/SELECT), `chat_sessions` (INSERT/SELECT), `memory_documents` (SELECT)

### 4. `POST` /api/v1/agents/{agentId}/chat
- **Grep Confirmation**: Line 160 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/ChatRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `ChatApiRequest`: `(val message: String = "", val prompt: String = "", val tenantId: String = "tenant-default", val conversationId: String? = null, val agentId: String? = null, val systemPrompt: String? = null)`
- **Response Body Schema**: `ChatApiResponse`: `(val reply: String, val modelUsed: String, val latencyMs: Long, val conversationId: String, val references: List<String> = emptyList()`
- **Status Engine Terhubung**: Ya — Terkoneksi ke OrchestrationEngine, ModelRouter & CompanyBrainRAG
- **Tabel Supabase Terpengaruh**: `chat_messages` (INSERT/SELECT), `chat_sessions` (INSERT/SELECT), `memory_documents` (SELECT)

### 5. `POST` /api/v1/company-brain/documents
- **Grep Confirmation**: Line 245 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/ChatRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `DocumentUploadRequest`: `(val title: String, val content: String, val tenantId: String)`
- **Response Body Schema**: `HttpStatusCode.Created, mapOf( "documentId" to docId, "title" to uploadReq.title, "chunksIndexed"...`
- **Status Engine Terhubung**: Ya — Terkoneksi ke OrchestrationEngine, ModelRouter & CompanyBrainRAG
- **Tabel Supabase Terpengaruh**: `chat_messages` (INSERT/SELECT), `chat_sessions` (INSERT/SELECT), `memory_documents` (SELECT)

### 6. `GET` /api/v1/company-brain/search
- **Grep Confirmation**: Line 266 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/ChatRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, results`
- **Status Engine Terhubung**: Ya — Terkoneksi ke OrchestrationEngine, ModelRouter & CompanyBrainRAG
- **Tabel Supabase Terpengaruh**: `chat_messages` (INSERT/SELECT), `chat_sessions` (INSERT/SELECT), `memory_documents` (SELECT)

## 10. Universal Selection & Autonomous Ranking
- **File Sumber**: `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/SelectionRoutes.kt`
- **Jumlah Endpoint**: **22**

### 1. `POST` /api/v1/selection/upload
- **Grep Confirmation**: Line 212 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/SelectionRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`, `X-User-Id: <user-id>`
- **Request Body Schema**: `MultipartFormDataContent (file upload)`
- **Response Body Schema**: `SelectionUploadResponse`: `(val requestId: String, val documentId: String, val fileName: String, val objectStorageUrl: String, val status: String)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke SelectionEngine (Universal Data Selection & Autonomous Ranking)
- **Tabel Supabase Terpengaruh**: `selection_requests` (INSERT), `selection_criteria` (INSERT), `selection_results` (INSERT)

### 2. `POST` /api/v1/selection/prompt-only
- **Grep Confirmation**: Line 353 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/SelectionRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`, `X-User-Id: <user-id>`
- **Request Body Schema**: `PromptOnlySelectionRequest`: `(val prompt: String, val calibrationSettingsId: String? = null)`
- **Response Body Schema**: `HttpStatusCode.Accepted, mapOf( "requestId" to reqId, "status" to "processing", "sourceType" to "...`
- **Status Engine Terhubung**: Ya — Terkoneksi ke SelectionEngine (Universal Data Selection & Autonomous Ranking)
- **Tabel Supabase Terpengaruh**: `selection_requests` (INSERT), `selection_criteria` (INSERT), `selection_results` (INSERT)

### 3. `POST` /api/v1/selection/api-database
- **Grep Confirmation**: Line 401 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/SelectionRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`, `X-User-Id: <user-id>`
- **Request Body Schema**: `ApiDatabaseSelectionRequest`: `(val prompt: String, val tableName: String? = null, val items: List<Map<String, String>>? = null)`
- **Response Body Schema**: `HttpStatusCode.Accepted, mapOf( "requestId" to reqId, "status" to "processing", "sourceType" to "...`
- **Status Engine Terhubung**: Ya — Terkoneksi ke SelectionEngine (Universal Data Selection & Autonomous Ranking)
- **Tabel Supabase Terpengaruh**: `selection_requests` (SELECT), `selection_results` (SELECT)

### 4. `GET` /api/v1/selection/requests
- **Grep Confirmation**: Line 443 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/SelectionRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, list`
- **Status Engine Terhubung**: Ya — Terkoneksi ke SelectionEngine (Universal Data Selection & Autonomous Ranking)
- **Tabel Supabase Terpengaruh**: `selection_requests` (SELECT), `selection_results` (SELECT)

### 5. `GET` /api/v1/selection/{id}
- **Grep Confirmation**: Line 453 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/SelectionRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `SelectionDetailsResponse`: `(val request: SelectionRequestRecord, val criteria: List<SelectionCriterionRecord>, val results: List<SelectionResultRecord>)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke SelectionEngine (Universal Data Selection & Autonomous Ranking)
- **Tabel Supabase Terpengaruh**: `selection_requests` (SELECT), `selection_results` (SELECT)

### 6. `GET` /api/v1/selection/{id}/results
- **Grep Confirmation**: Line 479 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/SelectionRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, results`
- **Status Engine Terhubung**: Ya — Terkoneksi ke SelectionEngine (Universal Data Selection & Autonomous Ranking)
- **Tabel Supabase Terpengaruh**: `selection_requests` (SELECT), `selection_results` (SELECT)

### 7. `GET` /api/v1/selection/{id}/analytics
- **Grep Confirmation**: Line 490 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/SelectionRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, analytics`
- **Status Engine Terhubung**: Ya — Terkoneksi ke SelectionEngine (Universal Data Selection & Autonomous Ranking)
- **Tabel Supabase Terpengaruh**: `selection_results` (SELECT), `selection_requests` (SELECT)

### 8. `POST` /api/v1/selection/documents/{documentId}/understand
- **Grep Confirmation**: Line 501 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/SelectionRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, understanding`
- **Status Engine Terhubung**: Ya — Terkoneksi ke SelectionEngine (Universal Data Selection & Autonomous Ranking)
- **Tabel Supabase Terpengaruh**: `selection_requests` (SELECT), `selection_results` (SELECT)

### 9. `GET` /api/v1/selection/documents/{documentId}/understanding
- **Grep Confirmation**: Line 521 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/SelectionRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, understanding`
- **Status Engine Terhubung**: Ya — Terkoneksi ke SelectionEngine (Universal Data Selection & Autonomous Ranking)
- **Tabel Supabase Terpengaruh**: `selection_requests` (SELECT), `selection_results` (SELECT)

### 10. `POST` /api/v1/selection/calibration
- **Grep Confirmation**: Line 541 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/SelectionRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`, `X-User-Id: <user-id>`
- **Request Body Schema**: `CalibrationRequest`: `(val calibration_name: String, val items: List<CalibrationItemRequest>, val is_saved_as_preset: Boolean = false)`
- **Response Body Schema**: `HttpStatusCode.Created, result`
- **Status Engine Terhubung**: Ya — Terkoneksi ke SelectionEngine (Universal Data Selection & Autonomous Ranking)
- **Tabel Supabase Terpengaruh**: `selection_requests` (SELECT), `selection_results` (SELECT)

### 11. `GET` /api/v1/selection/calibration
- **Grep Confirmation**: Line 576 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/SelectionRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, list`
- **Status Engine Terhubung**: Ya — Terkoneksi ke SelectionEngine (Universal Data Selection & Autonomous Ranking)
- **Tabel Supabase Terpengaruh**: `selection_requests` (SELECT), `selection_results` (SELECT)

### 12. `GET` /api/v1/selection/calibration/{id}
- **Grep Confirmation**: Line 586 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/SelectionRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `CalibrationDetailResponse`: `(val setting: SelectionCalibrationSettingRecord, val items: List<SelectionCalibrationItemRecord>)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke SelectionEngine (Universal Data Selection & Autonomous Ranking)
- **Tabel Supabase Terpengaruh**: `selection_requests` (SELECT), `selection_results` (SELECT)

### 13. `POST` /api/v1/selection/calibration/{id}/validate-dataset/{documentId}
- **Grep Confirmation**: Line 605 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/SelectionRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, validation`
- **Status Engine Terhubung**: Ya — Terkoneksi ke SelectionEngine (Universal Data Selection & Autonomous Ranking)
- **Tabel Supabase Terpengaruh**: `selection_requests` (SELECT), `selection_results` (SELECT)

### 14. `POST` /api/v1/selection/results/{id}/review
- **Grep Confirmation**: Line 642 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/SelectionRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`, `X-User-Id: <user-id>`
- **Request Body Schema**: `ReviewSelectionResultRequest`: `(val action: String, // "approve", "reject", "override_rank", "add_notes" val notes: String? = null, val overrideRankPosition: Int? = null, val overrideClassification: String? = null)`
- **Response Body Schema**: `getOrThrow`
- **Status Engine Terhubung**: Ya — Terkoneksi ke SelectionEngine (Universal Data Selection & Autonomous Ranking)
- **Tabel Supabase Terpengaruh**: `selection_requests` (SELECT), `selection_results` (SELECT)

### 15. `POST` /api/v1/selection/results/{id}/execute-downstream
- **Grep Confirmation**: Line 674 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/SelectionRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, mapOf( "status" to "executed", "result_id" to resultId, "action" to "downstrea...`
- **Status Engine Terhubung**: Ya — Terkoneksi ke SelectionEngine (Universal Data Selection & Autonomous Ranking)
- **Tabel Supabase Terpengaruh**: `selection_requests` (SELECT), `selection_results` (SELECT)

### 16. `GET` /api/v1/selection/{id}/export
- **Grep Confirmation**: Line 740 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/SelectionRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `parse`
- **Status Engine Terhubung**: Ya — Terkoneksi ke SelectionEngine (Universal Data Selection & Autonomous Ranking)
- **Tabel Supabase Terpengaruh**: `selection_requests` (SELECT), `selection_results` (SELECT)

### 17. `POST` /api/v1/selection/{id}/export
- **Grep Confirmation**: Line 741 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/SelectionRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `parse`
- **Status Engine Terhubung**: Ya — Terkoneksi ke SelectionEngine (Universal Data Selection & Autonomous Ranking)
- **Tabel Supabase Terpengaruh**: `selection_requests` (SELECT), `selection_results` (SELECT)

### 18. `GET` /api/v1/selection/auto-selection/configs
- **Grep Confirmation**: Line 751 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/SelectionRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, configs`
- **Status Engine Terhubung**: Ya — Terkoneksi ke SelectionEngine (Universal Data Selection & Autonomous Ranking)
- **Tabel Supabase Terpengaruh**: `selection_requests` (SELECT), `selection_results` (SELECT)

### 19. `POST` /api/v1/selection/auto-selection/configs
- **Grep Confirmation**: Line 758 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/SelectionRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `AutoSelectionConfigCreateRequest`: `(val folder_path: String, val domain_category: String? = null, val default_prompt: String = "Lakukan evaluasi dan ranking otomatis untuk dokumen yang diunggah", val is_enabled: Boolean = true, val calibration_settings_id: String? = null, val auto_execute_downstream: Boolean = false)`
- **Response Body Schema**: `HttpStatusCode.Created, saved`
- **Status Engine Terhubung**: Ya — Terkoneksi ke SelectionEngine (Universal Data Selection & Autonomous Ranking)
- **Tabel Supabase Terpengaruh**: `selection_requests` (SELECT), `selection_results` (SELECT)

### 20. `DELETE` /api/v1/selection/auto-selection/configs/{id}
- **Grep Confirmation**: Line 776 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/SelectionRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, mapOf("status" to "deleted", "id" to id`
- **Status Engine Terhubung**: Ya — Terkoneksi ke SelectionEngine (Universal Data Selection & Autonomous Ranking)
- **Tabel Supabase Terpengaruh**: `selection_requests` (SELECT), `selection_results` (SELECT)

### 21. `POST` /api/v1/selection/webhook/storage
- **Grep Confirmation**: Line 794 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/SelectionRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `StorageUploadWebhookPayload`: `(val type: String? = null, val table: String? = null, val schema: String? = null, val record: StorageObjectRecord? = null, val tenant_id: String? = null, val bucket: String? = null, val path: String? = null, val file_name: String? = null, val file_type: String? = null, val storage_url: String? = null, val file_content_base64: String? = null)`
- **Response Body Schema**: `StorageWebhookResponse`: `(val status: String, val auto_selection_triggered: Boolean, val selection_request_id: String? = null, val document_id: String? = null, val folder_path: String? = null, val domain_category: String? = null, val assigned_ai_job_title: String? = null, val assigned_ai_job_code: String? = null, val assigned_ai_job_id: String? = null, val message: String? = null, val tenant_id: String? = null)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke SelectionEngine (Universal Data Selection & Autonomous Ranking)
- **Tabel Supabase Terpengaruh**: `selection_requests` (SELECT), `selection_results` (SELECT)

### 22. `POST` /api/v1/selection/webhook/integration-fabric
- **Grep Confirmation**: Line 958 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/SelectionRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-ID: <tenant-uuid>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `IntegrationFabricWebhookPayload`: `(val tenant_id: String? = null, val source_system: String = "ERP", val domain: String? = null, val prompt: String = "Evaluasi dan ranking otomatis dari Integration Fabric", val table_name: String? = null, val items: List<Map<String, String>> = emptyList()`
- **Response Body Schema**: `IntegrationFabricWebhookResponse`: `(val status: String, val selection_request_id: String, val source_type: String, val source_system: String, val domain_category: String, val assigned_ai_job_title: String, val assigned_ai_job_code: String, val assigned_ai_job_id: String, val items_received: Int)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke SelectionEngine (Universal Data Selection & Autonomous Ranking)
- **Tabel Supabase Terpengaruh**: `selection_requests` (SELECT), `selection_results` (SELECT)

## 11. Generative Studio & Brand Asset Management
- **File Sumber**: `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/GenerativeStudioRoutes.kt`
- **Jumlah Endpoint**: **8**

### 1. `POST` /api/v1/studio/generate-image
- **Grep Confirmation**: Line 74 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/GenerativeStudioRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `GenerateImageRequest`: `(val prompt: String? = null, val productName: String? = null, val targetAudience: String? = null, val visualTheme: String? = null, val aspectRatio: String? = "1:1", val tenantId: String? = null)`
- **Response Body Schema**: `HttpStatusCode.OK, mapOf( "status" to "completed", "imageUrl" to imageUrl, "prompt" to finalPromp...`
- **Status Engine Terhubung**: Ya — Terkoneksi ke BrandAssetService & MultiModalCreativeStudio
- **Tabel Supabase Terpengaruh**: `creative_layout_templates` (SELECT), `brand_asset_overlays` (SELECT)

### 2. `POST` /api/v1/studio/compose-prompt
- **Grep Confirmation**: Line 151 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/GenerativeStudioRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `ComposePromptRequest`: `(val productName: String, val targetAudience: String, val visualTheme: String, val aspectRatio: String = "1:1")`
- **Response Body Schema**: `HttpStatusCode.OK, composed`
- **Status Engine Terhubung**: Ya — Terkoneksi ke BrandAssetService & MultiModalCreativeStudio
- **Tabel Supabase Terpengaruh**: `creative_layout_templates` (SELECT), `brand_asset_overlays` (SELECT)

### 3. `POST` /api/v1/studio/campaign-creative
- **Grep Confirmation**: Line 163 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/GenerativeStudioRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `CampaignCreativeRequest`: `(val topic: String, val platform: String = "INSTAGRAM", val tenantId: String? = null)`
- **Response Body Schema**: `HttpStatusCode.OK, plan`
- **Status Engine Terhubung**: Ya — Terkoneksi ke BrandAssetService & MultiModalCreativeStudio
- **Tabel Supabase Terpengaruh**: `creative_layout_templates` (SELECT), `brand_asset_overlays` (SELECT)

### 4. `GET` /api/v1/studio/templates
- **Grep Confirmation**: Line 179 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/GenerativeStudioRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, emptyList<String>(`
- **Status Engine Terhubung**: Ya — Terkoneksi ke BrandAssetService & MultiModalCreativeStudio
- **Tabel Supabase Terpengaruh**: `creative_layout_templates` (SELECT)

### 5. `POST` /api/v1/studio/brand-assets/upload-logo
- **Grep Confirmation**: Line 184 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/GenerativeStudioRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `BrandLogoUploadRequest`: `(val fileName: String, val fileBase64: String, val tenantId: String? = null)`
- **Response Body Schema**: `HttpStatusCode.Created, result`
- **Status Engine Terhubung**: Ya — Terkoneksi ke BrandAssetService & MultiModalCreativeStudio
- **Tabel Supabase Terpengaruh**: `brand_asset_overlays` (INSERT/SELECT)

### 6. `POST` /api/v1/studio/brand-assets/logo
- **Grep Confirmation**: Line 200 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/GenerativeStudioRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `BrandLogoUploadRequest`: `(val fileName: String, val fileBase64: String, val tenantId: String? = null)`
- **Response Body Schema**: `HttpStatusCode.Created, result`
- **Status Engine Terhubung**: Ya — Terkoneksi ke BrandAssetService & MultiModalCreativeStudio
- **Tabel Supabase Terpengaruh**: `brand_asset_overlays` (INSERT/SELECT)

### 7. `GET` /api/v1/studio/assets
- **Grep Confirmation**: Line 216 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/GenerativeStudioRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, items`
- **Status Engine Terhubung**: Ya — Terkoneksi ke BrandAssetService & MultiModalCreativeStudio
- **Tabel Supabase Terpengaruh**: `brand_asset_overlays` (INSERT/SELECT)

### 8. `POST` /api/v1/studio/assets
- **Grep Confirmation**: Line 233 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/GenerativeStudioRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `BrandAssetCreateRequest`: `(val title: String, val assetType: String, val fileUrl: String, val isLocked: Boolean = false)`
- **Response Body Schema**: `BrandAssetItem`: `(val id: String, val tenantId: String, val title: String, val assetType: String, val fileUrl: String, val isLocked: Boolean = false, val createdAt: Long = System.currentTimeMillis()`
- **Status Engine Terhubung**: Ya — Terkoneksi ke BrandAssetService & MultiModalCreativeStudio
- **Tabel Supabase Terpengaruh**: `brand_asset_overlays` (INSERT/SELECT)

## 12. Enterprise Governance, Context Fabric & Chief of Staff
- **File Sumber**: `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Jumlah Endpoint**: **41**

### 1. `GET` /api/v1/tenants/{id}/enterprise-connections
- **Grep Confirmation**: Line 210 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, connections`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ChiefOfStaffService, MonitoringLoopEngine & CrossSystemCorrelator
- **Tabel Supabase Terpengaruh**: `enterprise_connections` (SELECT/INSERT)

### 2. `POST` /api/v1/tenants/{id}/enterprise-connections
- **Grep Confirmation**: Line 216 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `EnterpriseConnectionCreateRequest`: `(val systemType: String, val connectionEndpoint: String, val authType: String = "BEARER_TOKEN", val syncScheduleCron: String = "0 * * * *")`
- **Response Body Schema**: `EnterpriseConnectionCreateResponse`: `(val connectionId: String, val systemType: String, val status: String, val health: String)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ChiefOfStaffService, MonitoringLoopEngine & CrossSystemCorrelator
- **Tabel Supabase Terpengaruh**: `enterprise_connections` (SELECT/INSERT)

### 3. `GET` /api/v1/tenants/{id}/ai-data-permissions
- **Grep Confirmation**: Line 244 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, policies`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ChiefOfStaffService, MonitoringLoopEngine & CrossSystemCorrelator
- **Tabel Supabase Terpengaruh**: `ai_data_permission_policies` (SELECT/INSERT)

### 4. `POST` /api/v1/tenants/{id}/ai-data-permissions
- **Grep Confirmation**: Line 250 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `AiDataPermissionPolicyRequest`: `(val agentPersonaType: String, val domainScope: String, val accessLevel: String = "READ_ONLY", val conditionsJson: String = "{}")`
- **Response Body Schema**: `AiDataPermissionPolicyResponse`: `(val policyId: String, val agentPersonaType: String, val accessLevel: String, val status: String)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ChiefOfStaffService, MonitoringLoopEngine & CrossSystemCorrelator
- **Tabel Supabase Terpengaruh**: `ai_data_permission_policies` (SELECT/INSERT)

### 5. `POST` /api/v1/tenants/{id}/ai-data-permissions/check
- **Grep Confirmation**: Line 277 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `Map<String, String`
- **Response Body Schema**: `HttpStatusCode.OK, mapOf( "status" to "success", "decision" to decision.decision, "reason" to dec...`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ChiefOfStaffService, MonitoringLoopEngine & CrossSystemCorrelator
- **Tabel Supabase Terpengaruh**: `ai_data_permission_policies` (SELECT/INSERT)

### 6. `POST` /api/v1/tenants/{id}/tier
- **Grep Confirmation**: Line 317 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `Map<String, String`
- **Response Body Schema**: `HttpStatusCode.OK, mapOf( "status" to "success", "tenantId" to tenantId, "tier" to tier.name, "ti...`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ChiefOfStaffService, MonitoringLoopEngine & CrossSystemCorrelator
- **Tabel Supabase Terpengaruh**: `context_fabric_snapshots` (SELECT/INSERT)

### 7. `POST` /api/v1/tenants/{id}/downgrade
- **Grep Confirmation**: Line 334 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `Map<String, String`
- **Response Body Schema**: `HttpStatusCode.OK, mapOf( "status" to "success", "tenantId" to report.tenantId, "previousTier" to...`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ChiefOfStaffService, MonitoringLoopEngine & CrossSystemCorrelator
- **Tabel Supabase Terpengaruh**: `context_fabric_snapshots` (SELECT/INSERT)

### 8. `GET` /api/v1/tenants/{id}/activity-stream
- **Grep Confirmation**: Line 356 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, streamItems`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ChiefOfStaffService, MonitoringLoopEngine & CrossSystemCorrelator
- **Tabel Supabase Terpengaruh**: `context_fabric_snapshots` (SELECT/INSERT)

### 9. `GET` /api/v1/tenants/{id}/context-fabric/{entityId}
- **Grep Confirmation**: Line 366 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, fabric`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ChiefOfStaffService, MonitoringLoopEngine & CrossSystemCorrelator
- **Tabel Supabase Terpengaruh**: `context_fabric_snapshots` (SELECT/INSERT)

### 10. `POST` /api/v1/tenants/{id}/management-query
- **Grep Confirmation**: Line 374 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `ManagementQueryRequest`: `(val question: String, val entityFocus: String? = null, val sessionId: String? = null, val role: String? = "EXECUTIVE", val agentId: String? = "agent-chief-of-staff")`
- **Response Body Schema**: `ManagementQueryResponse`: `(val question: String, val answer: String, val confidence: Double, val dataAvailability: String, val sourcesUsed: List<String>, val sessionId: String? = null, val turnCount: Int = 1, val accessRestricted: Boolean = false, val rolePersonalization: String? = null)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ChiefOfStaffService, MonitoringLoopEngine & CrossSystemCorrelator
- **Tabel Supabase Terpengaruh**: `context_fabric_snapshots` (SELECT/INSERT)

### 11. `POST` /api/v1/tenants/{id}/correlate-signals
- **Grep Confirmation**: Line 511 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `Map<String, String`
- **Response Body Schema**: `HttpStatusCode.OK, mapOf( "isCorrelated" to result.isCorrelated, "entityReference" to result.enti...`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ChiefOfStaffService, MonitoringLoopEngine & CrossSystemCorrelator
- **Tabel Supabase Terpengaruh**: `context_fabric_snapshots` (SELECT/INSERT)

### 12. `GET` /api/v1/tenants/{id}/reports/daily
- **Grep Confirmation**: Line 534 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, report`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ChiefOfStaffService, MonitoringLoopEngine & CrossSystemCorrelator
- **Tabel Supabase Terpengaruh**: `context_fabric_snapshots` (SELECT/INSERT)

### 13. `GET` /api/v1/tenants/{id}/knowledge-rules
- **Grep Confirmation**: Line 542 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, rules`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ChiefOfStaffService, MonitoringLoopEngine & CrossSystemCorrelator
- **Tabel Supabase Terpengaruh**: `context_fabric_snapshots` (SELECT/INSERT)

### 14. `POST` /api/v1/tenants/{id}/knowledge-rules
- **Grep Confirmation**: Line 549 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `KnowledgeRuleCreateRequest`: `(val entityType: String, val sopReference: String, val structuredRuleJson: String = "{}", val naturalLanguageRule: String = "", val condition: String = "operating_temperature", val comparisonOperator: String = ">=", val thresholdValue: Double = 0.0, val ruleDescription: String = "")`
- **Response Body Schema**: `HttpStatusCode.Created, created`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ChiefOfStaffService, MonitoringLoopEngine & CrossSystemCorrelator
- **Tabel Supabase Terpengaruh**: `context_fabric_snapshots` (SELECT/INSERT)

### 15. `POST` /api/v1/tenants/{id}/knowledge-rules/{ruleId}/approve
- **Grep Confirmation**: Line 566 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, approved`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ChiefOfStaffService, MonitoringLoopEngine & CrossSystemCorrelator
- **Tabel Supabase Terpengaruh**: `context_fabric_snapshots` (SELECT/INSERT)

### 16. `POST` /api/v1/tenants/{id}/knowledge-rules/fuse
- **Grep Confirmation**: Line 578 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `Map<String, String`
- **Response Body Schema**: `HttpStatusCode.OK, fusionResult`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ChiefOfStaffService, MonitoringLoopEngine & CrossSystemCorrelator
- **Tabel Supabase Terpengaruh**: `context_fabric_snapshots` (SELECT/INSERT)

### 17. `GET` /api/v1/tenants/{id}/events
- **Grep Confirmation**: Line 590 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, events`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ChiefOfStaffService, MonitoringLoopEngine & CrossSystemCorrelator
- **Tabel Supabase Terpengaruh**: `ai_events` (SELECT/INSERT)

### 18. `POST` /api/v1/tenants/{id}/events
- **Grep Confirmation**: Line 596 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `AiEventPublishRequest`: `(val eventCode: String, val entityReference: String, val sourceSystem: String = "INTERNAL", val severity: String = "HIGH", val isMultiAgentCollaborative: Boolean = false, val payloadJson: String = "{}")`
- **Response Body Schema**: `HttpStatusCode.Created, dispatchResult`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ChiefOfStaffService, MonitoringLoopEngine & CrossSystemCorrelator
- **Tabel Supabase Terpengaruh**: `ai_events` (SELECT/INSERT)

### 19. `GET` /api/v1/tenants/{id}/finance/cashflow-pressure
- **Grep Confirmation**: Line 613 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, report`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ChiefOfStaffService, MonitoringLoopEngine & CrossSystemCorrelator
- **Tabel Supabase Terpengaruh**: `context_fabric_snapshots` (SELECT/INSERT)

### 20. `GET` /api/v1/tenants/{id}/actions
- **Grep Confirmation**: Line 620 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, actions`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ChiefOfStaffService, MonitoringLoopEngine & CrossSystemCorrelator
- **Tabel Supabase Terpengaruh**: `ai_proposed_actions` (SELECT/INSERT)

### 21. `POST` /api/v1/tenants/{id}/actions/propose
- **Grep Confirmation**: Line 626 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `ActionProposeRequest`: `(val agentId: String, val actionType: String, val targetSystem: String, val payload: Map<String, String> = emptyMap()`
- **Response Body Schema**: `HttpStatusCode.OK, result`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ChiefOfStaffService, MonitoringLoopEngine & CrossSystemCorrelator
- **Tabel Supabase Terpengaruh**: `ai_proposed_actions` (SELECT/INSERT)

### 22. `POST` /api/v1/tenants/{id}/actions/{actionId}/execute
- **Grep Confirmation**: Line 640 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, execResult`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ChiefOfStaffService, MonitoringLoopEngine & CrossSystemCorrelator
- **Tabel Supabase Terpengaruh**: `ai_proposed_actions` (SELECT/INSERT)

### 23. `GET` /api/v1/tenants/{id}/monitoring-loops
- **Grep Confirmation**: Line 649 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, loops`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ChiefOfStaffService, MonitoringLoopEngine & CrossSystemCorrelator
- **Tabel Supabase Terpengaruh**: `monitoring_loops` (SELECT/INSERT)

### 24. `POST` /api/v1/tenants/{id}/monitoring-loops
- **Grep Confirmation**: Line 655 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `MonitoringLoopRegisterRequest`: `(val anomalyOrMetricType: String, val entityReference: String, val sourceSystem: String = "INTERNAL_INVENTORY", val baselineValue: Double = 0.0, val detectedValue: Double = 0.0, val targetResolvedValue: Double = 0.0, val assignedAgentOrHumanId: String = "agent-sentinel-ops")`
- **Response Body Schema**: `HttpStatusCode.Created, loop`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ChiefOfStaffService, MonitoringLoopEngine & CrossSystemCorrelator
- **Tabel Supabase Terpengaruh**: `monitoring_loops` (SELECT/INSERT)

### 25. `POST` /api/v1/tenants/{id}/monitoring-loops/tick
- **Grep Confirmation**: Line 671 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, updated`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ChiefOfStaffService, MonitoringLoopEngine & CrossSystemCorrelator
- **Tabel Supabase Terpengaruh**: `monitoring_loops` (SELECT/INSERT)

### 26. `GET` /api/v1/tenants/{id}/swarm/status
- **Grep Confirmation**: Line 678 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, status`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ChiefOfStaffService, MonitoringLoopEngine & CrossSystemCorrelator
- **Tabel Supabase Terpengaruh**: `context_fabric_snapshots` (SELECT/INSERT)

### 27. `POST` /api/v1/tenants/{id}/swarm/freeze
- **Grep Confirmation**: Line 684 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `Map<String, String`
- **Response Body Schema**: `HttpStatusCode.OK, mapOf("status" to "FROZEN", "detail" to detail`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ChiefOfStaffService, MonitoringLoopEngine & CrossSystemCorrelator
- **Tabel Supabase Terpengaruh**: `context_fabric_snapshots` (SELECT/INSERT)

### 28. `POST` /api/v1/tenants/{id}/swarm/resume
- **Grep Confirmation**: Line 693 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `Map<String, String`
- **Response Body Schema**: `HttpStatusCode.OK, mapOf("status" to "RESUMED", "success" to resumed`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ChiefOfStaffService, MonitoringLoopEngine & CrossSystemCorrelator
- **Tabel Supabase Terpengaruh**: `context_fabric_snapshots` (SELECT/INSERT)

### 29. `GET` /api/v1/tenants/{id}/chief-of-staff/briefings
- **Grep Confirmation**: Line 702 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, list`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ChiefOfStaffService, MonitoringLoopEngine & CrossSystemCorrelator
- **Tabel Supabase Terpengaruh**: `chief_of_staff_briefings` (SELECT/INSERT)

### 30. `POST` /api/v1/tenants/{id}/chief-of-staff/synthesize
- **Grep Confirmation**: Line 746 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, briefing`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ChiefOfStaffService, MonitoringLoopEngine & CrossSystemCorrelator
- **Tabel Supabase Terpengaruh**: `context_fabric_snapshots` (SELECT/INSERT)

### 31. `POST` /api/v1/tenants/{id}/chief-of-staff/research-directives
- **Grep Confirmation**: Line 759 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `ResearchDirectiveRequest`: `(val tenantId: String, val topic: String, val requestedBy: String, val targetPersonas: List<String> = emptyList()`
- **Response Body Schema**: `GenericStatusResponse`: `(val status: String, val message: String? = null, val id: String? = null)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ChiefOfStaffService, MonitoringLoopEngine & CrossSystemCorrelator
- **Tabel Supabase Terpengaruh**: `context_fabric_snapshots` (SELECT/INSERT)

### 32. `GET` /api/v1/tenants/{id}/agents/{agentId}/skill-confidence
- **Grep Confirmation**: Line 800 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `AgentSkillConfidenceResponse`: `(val agentId: String, val skillConfidenceScore: Double, val reinforceCount: Int, val correctCount: Int, val growthTrend: String)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ChiefOfStaffService, MonitoringLoopEngine & CrossSystemCorrelator
- **Tabel Supabase Terpengaruh**: `agent_skill_confidence` (SELECT/UPDATE)

### 33. `GET` /api/v1/tenants/{id}/data-quality-issues
- **Grep Confirmation**: Line 817 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, issues`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ChiefOfStaffService, MonitoringLoopEngine & CrossSystemCorrelator
- **Tabel Supabase Terpengaruh**: `data_quality_issues` (SELECT/UPDATE)

### 34. `POST` /api/v1/tenants/{id}/chief-of-staff/briefings/generate
- **Grep Confirmation**: Line 868 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, briefing`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ChiefOfStaffService, MonitoringLoopEngine & CrossSystemCorrelator
- **Tabel Supabase Terpengaruh**: `chief_of_staff_briefings` (SELECT/INSERT)

### 35. `GET` /api/v1/tenants/{id}/chief-of-staff/research-directives
- **Grep Confirmation**: Line 875 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, directives`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ChiefOfStaffService, MonitoringLoopEngine & CrossSystemCorrelator
- **Tabel Supabase Terpengaruh**: `context_fabric_snapshots` (SELECT/INSERT)

### 36. `POST` /api/v1/tenants/{id}/data-quality-issues/resolve
- **Grep Confirmation**: Line 882 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `ResolveDataQualityIssueRequest`: `(val issueId: String, val resolvedBy: String = "OPERATOR")`
- **Response Body Schema**: `HttpStatusCode.OK, mapOf("resolved" to resolved, "issueId" to req.issueId`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ChiefOfStaffService, MonitoringLoopEngine & CrossSystemCorrelator
- **Tabel Supabase Terpengaruh**: `data_quality_issues` (SELECT/UPDATE)

### 37. `GET` /api/v1/tenants/{id}/project-health/{projectId}
- **Grep Confirmation**: Line 890 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, report`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ChiefOfStaffService, MonitoringLoopEngine & CrossSystemCorrelator
- **Tabel Supabase Terpengaruh**: `context_fabric_snapshots` (SELECT/INSERT)

### 38. `POST` /api/v1/tenants/{id}/project-health/evaluate
- **Grep Confirmation**: Line 898 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `EvaluateProjectHealthRequest`: `(val projectId: String, val projectName: String, val scheduleScore: Double = 88.0, val budgetScore: Double = 92.0, val riskScore: Double = 85.0, val workforceScore: Double = 90.0, val blockersCount: Int = 0)`
- **Response Body Schema**: `HttpStatusCode.OK, report`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ChiefOfStaffService, MonitoringLoopEngine & CrossSystemCorrelator
- **Tabel Supabase Terpengaruh**: `context_fabric_snapshots` (SELECT/INSERT)

### 39. `POST` /api/v1/tenants/{id}/multi-agent-collaborations/initiate
- **Grep Confirmation**: Line 915 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `ai.orchestree.backend.collaboration.MultiAgentCollaborationRequest`
- **Response Body Schema**: `HttpStatusCode.Created, session`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ChiefOfStaffService, MonitoringLoopEngine & CrossSystemCorrelator
- **Tabel Supabase Terpengaruh**: `context_fabric_snapshots` (SELECT/INSERT)

### 40. `GET` /api/v1/tenants/{id}/multi-agent-collaborations
- **Grep Confirmation**: Line 922 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, collabs`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ChiefOfStaffService, MonitoringLoopEngine & CrossSystemCorrelator
- **Tabel Supabase Terpengaruh**: `context_fabric_snapshots` (SELECT/INSERT)

### 41. `GET` /api/v1/tenants/{id}/explainability/{executionId}
- **Grep Confirmation**: Line 929 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, trace`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ChiefOfStaffService, MonitoringLoopEngine & CrossSystemCorrelator
- **Tabel Supabase Terpengaruh**: `context_fabric_snapshots` (SELECT/INSERT)

## 13. Autonomous Memory Consolidation & Decay
- **File Sumber**: `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/MemoryRoutes.kt`
- **Jumlah Endpoint**: **6**

### 1. `POST` /api/v1/memory/consolidate/evaluate
- **Grep Confirmation**: Line 38 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/MemoryRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `CandidateInteraction`: `(val id: String = UUID.randomUUID()`
- **Response Body Schema**: `HttpStatusCode.OK, decision`
- **Status Engine Terhubung**: Ya — Terkoneksi ke MemoryConsolidator, MemoryDecayEngine & HybridMemorySearchEngine
- **Tabel Supabase Terpengaruh**: `memory_documents` (INSERT), `memory_consolidation_logs` (INSERT)

### 2. `POST` /api/v1/memory/consolidate/batch
- **Grep Confirmation**: Line 44 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/MemoryRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `List<CandidateInteraction`: `(val id: String = UUID.randomUUID()`
- **Response Body Schema**: `HttpStatusCode.OK, decisions`
- **Status Engine Terhubung**: Ya — Terkoneksi ke MemoryConsolidator, MemoryDecayEngine & HybridMemorySearchEngine
- **Tabel Supabase Terpengaruh**: `memory_documents` (INSERT), `memory_consolidation_logs` (INSERT)

### 3. `POST` /api/v1/memory/decay
- **Grep Confirmation**: Line 51 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/MemoryRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `DecayMemoryRequest`: `(val tenantId: String? = null)`
- **Response Body Schema**: `HttpStatusCode.OK, summary`
- **Status Engine Terhubung**: Ya — Terkoneksi ke MemoryConsolidator, MemoryDecayEngine & HybridMemorySearchEngine
- **Tabel Supabase Terpengaruh**: `memory_documents` (UPDATE)

### 4. `POST` /api/v1/memory/search
- **Grep Confirmation**: Line 58 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/MemoryRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `SearchMemoryRequest`: `(val tenantId: String, val query: String, val enableReranking: Boolean = true)`
- **Response Body Schema**: `HttpStatusCode.OK, result`
- **Status Engine Terhubung**: Ya — Terkoneksi ke MemoryConsolidator, MemoryDecayEngine & HybridMemorySearchEngine
- **Tabel Supabase Terpengaruh**: `memory_documents` (SELECT)

### 5. `POST` /api/v1/memory/search/ab-compare
- **Grep Confirmation**: Line 64 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/MemoryRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `SearchMemoryRequest`: `(val tenantId: String, val query: String, val enableReranking: Boolean = true)`
- **Response Body Schema**: `HttpStatusCode.OK, comparison`
- **Status Engine Terhubung**: Ya — Terkoneksi ke MemoryConsolidator, MemoryDecayEngine & HybridMemorySearchEngine
- **Tabel Supabase Terpengaruh**: `memory_documents` (SELECT)

### 6. `GET` /api/v1/memory/documents
- **Grep Confirmation**: Line 71 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/MemoryRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, docs`
- **Status Engine Terhubung**: Ya — Terkoneksi ke MemoryConsolidator, MemoryDecayEngine & HybridMemorySearchEngine
- **Tabel Supabase Terpengaruh**: `memory_documents` (SELECT/UPDATE)

## 14. Omnichannel Sales, CRM & Channel Gateway
- **File Sumber**: `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Jumlah Endpoint**: **28**

### 1. `GET` /api/v1/tenants/{id}/channel-accounts
- **Grep Confirmation**: Line 261 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `withContext`
- **Status Engine Terhubung**: Ya — Terkoneksi ke SalesPersonaEngine, ChannelGateway & PaymentReconciliationEngine
- **Tabel Supabase Terpengaruh**: `channel_accounts` (SELECT), `leads` (SELECT)

### 2. `POST` /api/v1/tenants/{id}/channel-accounts
- **Grep Confirmation**: Line 361 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `ChannelAccountCreateRequest`: `(val channelType: String, val accountLabel: String, val externalIdentifier: String, val departmentId: String? = null)`
- **Response Body Schema**: `GenericStatusResponse`: `(val status: String, val message: String? = null, val id: String? = null)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke SalesPersonaEngine, ChannelGateway & PaymentReconciliationEngine
- **Tabel Supabase Terpengaruh**: `channel_accounts` (SELECT), `leads` (SELECT)

### 3. `POST` /api/v1/tenants/{id}/channel-accounts/{caId}/verify
- **Grep Confirmation**: Line 402 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `GenericStatusResponse`: `(val status: String, val message: String? = null, val id: String? = null)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke SalesPersonaEngine, ChannelGateway & PaymentReconciliationEngine
- **Tabel Supabase Terpengaruh**: `channel_accounts` (SELECT), `leads` (SELECT)

### 4. `PATCH` /api/v1/tenants/{id}/channel-accounts/{caId}/approve
- **Grep Confirmation**: Line 420 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `GenericStatusResponse`: `(val status: String, val message: String? = null, val id: String? = null)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke SalesPersonaEngine, ChannelGateway & PaymentReconciliationEngine
- **Tabel Supabase Terpengaruh**: `channel_accounts` (SELECT), `leads` (SELECT)

### 5. `GET` /api/v1/tenants/{id}/channel-accounts/{caId}/health
- **Grep Confirmation**: Line 438 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `ChannelAccountHealthResponse`: `(val accountId: String, val isHealthy: Boolean, val details: String, val timestamp: Long = System.currentTimeMillis()`
- **Status Engine Terhubung**: Ya — Terkoneksi ke SalesPersonaEngine, ChannelGateway & PaymentReconciliationEngine
- **Tabel Supabase Terpengaruh**: `channel_accounts` (SELECT), `leads` (SELECT)

### 6. `GET` /api/v1/tenants/{id}/credit-wallet
- **Grep Confirmation**: Line 473 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `CreditWalletResponse`: `(val tenantId: String, val balance: Double, val currency: String, val lowBalanceThreshold: Double, val status: String)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke SalesPersonaEngine, ChannelGateway & PaymentReconciliationEngine
- **Tabel Supabase Terpengaruh**: `channel_accounts` (SELECT), `leads` (SELECT)

### 7. `GET` /api/v1/tenants/{id}/customers/search
- **Grep Confirmation**: Line 525 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `withContext`
- **Status Engine Terhubung**: Ya — Terkoneksi ke SalesPersonaEngine, ChannelGateway & PaymentReconciliationEngine
- **Tabel Supabase Terpengaruh**: `channel_accounts` (SELECT), `leads` (SELECT)

### 8. `POST` /api/v1/tenants/{id}/customers/resolve
- **Grep Confirmation**: Line 578 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `Map<String, String`
- **Response Body Schema**: `HttpStatusCode.OK, profile`
- **Status Engine Terhubung**: Ya — Terkoneksi ke SalesPersonaEngine, ChannelGateway & PaymentReconciliationEngine
- **Tabel Supabase Terpengaruh**: `channel_accounts` (SELECT), `leads` (SELECT)

### 9. `POST` /api/v1/tenants/{id}/customers/{cust_id}/merge
- **Grep Confirmation**: Line 592 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `CustomerMergeRequest`: `(val candidateCustomerIds: List<String>, val reason: String = "Admin approved merge")`
- **Response Body Schema**: `GenericStatusResponse`: `(val status: String, val message: String? = null, val id: String? = null)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke SalesPersonaEngine, ChannelGateway & PaymentReconciliationEngine
- **Tabel Supabase Terpengaruh**: `channel_accounts` (SELECT), `leads` (SELECT)

### 10. `GET` /api/v1/tenants/{id}/inbox/conversations
- **Grep Confirmation**: Line 608 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `withContext`
- **Status Engine Terhubung**: Ya — Terkoneksi ke SalesPersonaEngine, ChannelGateway & PaymentReconciliationEngine
- **Tabel Supabase Terpengaruh**: `channel_accounts` (SELECT), `leads` (SELECT)

### 11. `GET` /api/v1/tenants/{id}/leads
- **Grep Confirmation**: Line 671 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `withContext`
- **Status Engine Terhubung**: Ya — Terkoneksi ke SalesPersonaEngine, ChannelGateway & PaymentReconciliationEngine
- **Tabel Supabase Terpengaruh**: `leads` (SELECT/INSERT/UPDATE)

### 12. `POST` /api/v1/tenants/{id}/leads
- **Grep Confirmation**: Line 710 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `LeadCreateRequest`: `(val customerName: String, val contactIdentifier: String? = null, val sourceChannel: String = "WHATSAPP", val status: String = "NEW", val budget: Double = 0.0, val notes: String = "")`
- **Response Body Schema**: `GenericStatusResponse`: `(val status: String, val message: String? = null, val id: String? = null)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke SalesPersonaEngine, ChannelGateway & PaymentReconciliationEngine
- **Tabel Supabase Terpengaruh**: `leads` (SELECT/INSERT/UPDATE)

### 13. `GET` /api/v1/tenants/{id}/products
- **Grep Confirmation**: Line 749 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `withContext`
- **Status Engine Terhubung**: Ya — Terkoneksi ke SalesPersonaEngine, ChannelGateway & PaymentReconciliationEngine
- **Tabel Supabase Terpengaruh**: `channel_accounts` (SELECT), `leads` (SELECT)

### 14. `POST` /api/v1/tenants/{id}/products
- **Grep Confirmation**: Line 786 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `ProductCreateRequest`: `(val sku: String, val name: String, val description: String = "", val category: String, val basePrice: Double, val currency: String = "IDR")`
- **Response Body Schema**: `GenericStatusResponse`: `(val status: String, val message: String? = null, val id: String? = null)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke SalesPersonaEngine, ChannelGateway & PaymentReconciliationEngine
- **Tabel Supabase Terpengaruh**: `channel_accounts` (SELECT), `leads` (SELECT)

### 15. `GET` /api/v1/tenants/{id}/inventory
- **Grep Confirmation**: Line 817 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `withContext`
- **Status Engine Terhubung**: Ya — Terkoneksi ke SalesPersonaEngine, ChannelGateway & PaymentReconciliationEngine
- **Tabel Supabase Terpengaruh**: `channel_accounts` (SELECT), `leads` (SELECT)

### 16. `GET` /api/v1/tenants/{id}/inventory/{variantId}
- **Grep Confirmation**: Line 851 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, resp`
- **Status Engine Terhubung**: Ya — Terkoneksi ke SalesPersonaEngine, ChannelGateway & PaymentReconciliationEngine
- **Tabel Supabase Terpengaruh**: `channel_accounts` (SELECT), `leads` (SELECT)

### 17. `GET` /api/v1/tenants/{id}/orders
- **Grep Confirmation**: Line 888 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `withContext`
- **Status Engine Terhubung**: Ya — Terkoneksi ke SalesPersonaEngine, ChannelGateway & PaymentReconciliationEngine
- **Tabel Supabase Terpengaruh**: `orders` (SELECT/INSERT/UPDATE), `order_items` (SELECT/INSERT)

### 18. `GET` /api/v1/tenants/{id}/campaigns
- **Grep Confirmation**: Line 930 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `withContext`
- **Status Engine Terhubung**: Ya — Terkoneksi ke SalesPersonaEngine, ChannelGateway & PaymentReconciliationEngine
- **Tabel Supabase Terpengaruh**: `channel_accounts` (SELECT), `leads` (SELECT)

### 19. `POST` /api/v1/tenants/{id}/campaigns
- **Grep Confirmation**: Line 971 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `CampaignCreateRequest`: `(val name: String, val instruction: String, val targetChannels: List<String> = listOf("WHATSAPP", "INSTAGRAM")`
- **Response Body Schema**: `GenericStatusResponse`: `(val status: String, val message: String? = null, val id: String? = null)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke SalesPersonaEngine, ChannelGateway & PaymentReconciliationEngine
- **Tabel Supabase Terpengaruh**: `channel_accounts` (SELECT), `leads` (SELECT)

### 20. `GET` /api/v1/tenants/{id}/analytics/revenue-intelligence
- **Grep Confirmation**: Line 1000 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `RevenueIntelligenceResponse`: `(val totalRevenue: Double, val topChannel: String, val conversionRate: Double, val topSellingProduct: String)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke SalesPersonaEngine, ChannelGateway & PaymentReconciliationEngine
- **Tabel Supabase Terpengaruh**: `orders` (SELECT), `leads` (SELECT), `order_items` (SELECT)

### 21. `GET` /api/v1/tenants/{id}/analytics/sales-coach
- **Grep Confirmation**: Line 1054 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `SalesCoachResponse`: `(val overallTeamConversion: Double, val topInsight: String, val recommendations: List<String>)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke SalesPersonaEngine, ChannelGateway & PaymentReconciliationEngine
- **Tabel Supabase Terpengaruh**: `channel_accounts` (SELECT), `leads` (SELECT)

### 22. `POST` /api/v1/tenants/{id}/experiments
- **Grep Confirmation**: Line 1098 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `ExperimentCreateRequest`: `(val experimentName: String, val variantAContent: String, val variantBContent: String)`
- **Response Body Schema**: `GenericStatusResponse`: `(val status: String, val message: String? = null, val id: String? = null)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke SalesPersonaEngine, ChannelGateway & PaymentReconciliationEngine
- **Tabel Supabase Terpengaruh**: `channel_accounts` (SELECT), `leads` (SELECT)

### 23. `GET` /api/v1/tenants/{id}/service-requests
- **Grep Confirmation**: Line 1153 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `withContext`
- **Status Engine Terhubung**: Ya — Terkoneksi ke SalesPersonaEngine, ChannelGateway & PaymentReconciliationEngine
- **Tabel Supabase Terpengaruh**: `channel_accounts` (SELECT), `leads` (SELECT)

### 24. `POST` /api/v1/tenants/{id}/service-requests
- **Grep Confirmation**: Line 1189 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `ServiceRequestCreateRequest`: `(val customerId: String? = null, val customerName: String, val requestType: String = "INQUIRY", val priority: String = "MEDIUM", val subject: String, val description: String)`
- **Response Body Schema**: `GenericStatusResponse`: `(val status: String, val message: String? = null, val id: String? = null)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke SalesPersonaEngine, ChannelGateway & PaymentReconciliationEngine
- **Tabel Supabase Terpengaruh**: `channel_accounts` (SELECT), `leads` (SELECT)

### 25. `PATCH` /api/v1/tenants/{id}/ai-agents/{agentId}/persona
- **Grep Confirmation**: Line 1223 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `AgentPersonaUpdateRequest`: `(val personaType: String, val personaConfig: Map<String, String> = emptyMap()`
- **Response Body Schema**: `GenericStatusResponse`: `(val status: String, val message: String? = null, val id: String? = null)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke SalesPersonaEngine, ChannelGateway & PaymentReconciliationEngine
- **Tabel Supabase Terpengaruh**: `channel_accounts` (SELECT), `leads` (SELECT)

### 26. `POST` /api/v1/tenants/{id}/conversations/{convId}/persona-reply
- **Grep Confirmation**: Line 1255 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `PersonaReplyRequest`: `(val customerMessage: String, val customerName: String = "Pelanggan")`
- **Response Body Schema**: `put`
- **Status Engine Terhubung**: Ya — Terkoneksi ke SalesPersonaEngine, ChannelGateway & PaymentReconciliationEngine
- **Tabel Supabase Terpengaruh**: `channel_accounts` (SELECT), `leads` (SELECT)

### 27. `POST` /api/v1/conversations/{id}/takeover
- **Grep Confirmation**: Line 1402 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `GenericStatusResponse`: `(val status: String, val message: String? = null, val id: String? = null)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke SalesPersonaEngine, ChannelGateway & PaymentReconciliationEngine
- **Tabel Supabase Terpengaruh**: `channel_accounts` (SELECT), `leads` (SELECT)

### 28. `POST` /api/v1/carts/{id}/checkout
- **Grep Confirmation**: Line 1444 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `CheckoutRequest`: `(val paymentMethod: String, val shippingAddress: String, val courier: String, val customerName: String? = null, val customerPhone: String? = null)`
- **Response Body Schema**: `CartCheckoutResponse`: `(val cartId: String, val orderId: String, val status: String, val paymentUrl: String)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke SalesPersonaEngine, ChannelGateway & PaymentReconciliationEngine
- **Tabel Supabase Terpengaruh**: `channel_accounts` (SELECT), `leads` (SELECT)

## 15. Commercial Billing, Quota & Dunning
- **File Sumber**: `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Jumlah Endpoint**: **30**

### 1. `GET` /api/v1/plans
- **Grep Confirmation**: Line 73 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, plans`
- **Status Engine Terhubung**: Ya — Terkoneksi ke EntitlementEngine, CentralCreditLedgerService & DunningEngine
- **Tabel Supabase Terpengaruh**: `commercial_plans` (SELECT)

### 2. `GET` /api/v1/plans/{id}
- **Grep Confirmation**: Line 83 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, plan`
- **Status Engine Terhubung**: Ya — Terkoneksi ke EntitlementEngine, CentralCreditLedgerService & DunningEngine
- **Tabel Supabase Terpengaruh**: `commercial_plans` (SELECT)

### 3. `GET` /api/v1/plans/entitlements-matrix
- **Grep Confirmation**: Line 94 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, matrix`
- **Status Engine Terhubung**: Ya — Terkoneksi ke EntitlementEngine, CentralCreditLedgerService & DunningEngine
- **Tabel Supabase Terpengaruh**: `commercial_plans` (SELECT)

### 4. `GET` /api/v1/tenant/entitlements
- **Grep Confirmation**: Line 109 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, entitlements`
- **Status Engine Terhubung**: Ya — Terkoneksi ke EntitlementEngine, CentralCreditLedgerService & DunningEngine
- **Tabel Supabase Terpengaruh**: `tenant_subscriptions` (SELECT), `commercial_plans` (SELECT)

### 5. `GET` /api/v1/billing/plans
- **Grep Confirmation**: Line 126 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, plans`
- **Status Engine Terhubung**: Ya — Terkoneksi ke EntitlementEngine, CentralCreditLedgerService & DunningEngine
- **Tabel Supabase Terpengaruh**: `commercial_plans` (SELECT)

### 6. `GET` /api/v1/billing/plans/{id}
- **Grep Confirmation**: Line 135 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, plan`
- **Status Engine Terhubung**: Ya — Terkoneksi ke EntitlementEngine, CentralCreditLedgerService & DunningEngine
- **Tabel Supabase Terpengaruh**: `commercial_plans` (SELECT)

### 7. `POST` /api/v1/billing/calculate-cost
- **Grep Confirmation**: Line 146 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `CreditCostContext`: `(val activityType: String, val complexityLevel: String = "simple", // 'simple'/'medium'/'complex' val modelUsed: String = "standard", // 'openrouter'/'groq'/'deepseek'/'claude'/'kimi'/'standard' val toolsInvoked: Int = 0, val executionType: String = "single_step" // 'single_step'/'multi_step'/'autonomous')`
- **Response Body Schema**: `HttpStatusCode.OK, result`
- **Status Engine Terhubung**: Ya — Terkoneksi ke EntitlementEngine, CentralCreditLedgerService & DunningEngine
- **Tabel Supabase Terpengaruh**: `tenant_subscriptions` (SELECT/UPDATE)

### 8. `GET` /api/v1/billing/subscription
- **Grep Confirmation**: Line 162 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `SubscriptionPlanResponse`: `(val status: String = "ok", val tenantId: String? = null, val subscription: TenantSubscription? = null, val plan: CommercialPlan? = null)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke EntitlementEngine, CentralCreditLedgerService & DunningEngine
- **Tabel Supabase Terpengaruh**: `tenant_subscriptions` (SELECT/UPDATE)

### 9. `POST` /api/v1/billing/subscription
- **Grep Confirmation**: Line 187 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `SubscriptionStartRequest`: `(val planId: String, val billingInterval: String = "monthly")`
- **Response Body Schema**: `SubscriptionPlanResponse`: `(val status: String = "ok", val tenantId: String? = null, val subscription: TenantSubscription? = null, val plan: CommercialPlan? = null)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke EntitlementEngine, CentralCreditLedgerService & DunningEngine
- **Tabel Supabase Terpengaruh**: `tenant_subscriptions` (SELECT/UPDATE)

### 10. `POST` /api/v1/billing/subscription/upgrade
- **Grep Confirmation**: Line 209 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `SubscriptionUpgradeRequest`: `(val targetPlanId: String)`
- **Response Body Schema**: `HttpStatusCode.OK, result`
- **Status Engine Terhubung**: Ya — Terkoneksi ke EntitlementEngine, CentralCreditLedgerService & DunningEngine
- **Tabel Supabase Terpengaruh**: `tenant_subscriptions` (SELECT/UPDATE)

### 11. `POST` /api/v1/billing/subscription/downgrade
- **Grep Confirmation**: Line 222 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `SubscriptionDowngradeRequest`: `(val targetPlanId: String)`
- **Response Body Schema**: `DowngradeBlockedResponse`: `(val status: String = "blocked", val message: String, val overages: List<String>)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke EntitlementEngine, CentralCreditLedgerService & DunningEngine
- **Tabel Supabase Terpengaruh**: `tenant_subscriptions` (SELECT/UPDATE)

### 12. `POST` /api/v1/billing/subscription/cancel
- **Grep Confirmation**: Line 257 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `CancelSubscriptionResponse`: `(val status: String = "cancelled", val subscription: TenantSubscription)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke EntitlementEngine, CentralCreditLedgerService & DunningEngine
- **Tabel Supabase Terpengaruh**: `tenant_subscriptions` (SELECT/UPDATE)

### 13. `GET` /api/v1/billing/credits
- **Grep Confirmation**: Line 273 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `CreditsDisplayResponse`: `(val available: Double, val reserved: Double, val used: Double, val total: Double)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke EntitlementEngine, CentralCreditLedgerService & DunningEngine
- **Tabel Supabase Terpengaruh**: `central_credit_ledger` (SELECT/INSERT), `tenant_subscriptions` (SELECT)

### 14. `GET` /api/v1/billing/credits/summary
- **Grep Confirmation**: Line 298 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, summary`
- **Status Engine Terhubung**: Ya — Terkoneksi ke EntitlementEngine, CentralCreditLedgerService & DunningEngine
- **Tabel Supabase Terpengaruh**: `central_credit_ledger` (SELECT/INSERT), `tenant_subscriptions` (SELECT)

### 15. `GET` /api/v1/billing/credits/ledger
- **Grep Confirmation**: Line 335 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK / Standard JSON DTO`
- **Status Engine Terhubung**: Ya — Terkoneksi ke EntitlementEngine, CentralCreditLedgerService & DunningEngine
- **Tabel Supabase Terpengaruh**: `central_credit_ledger` (SELECT/INSERT), `tenant_subscriptions` (SELECT)

### 16. `GET` /api/v1/billing/ledger
- **Grep Confirmation**: Line 336 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK / Standard JSON DTO`
- **Status Engine Terhubung**: Ya — Terkoneksi ke EntitlementEngine, CentralCreditLedgerService & DunningEngine
- **Tabel Supabase Terpengaruh**: `central_credit_ledger` (SELECT/INSERT), `tenant_subscriptions` (SELECT)

### 17. `POST` /api/v1/billing/credits/topup
- **Grep Confirmation**: Line 339 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `CreditTopupRequest`: `(val amount: Double, val amountPaid: Double = 0.0, val currency: String = "IDR", val reference: String? = null)`
- **Response Body Schema**: `TopupSuccessResponse`: `(val status: String = "success", val tenantId: String, val creditsAdded: Double, val newBalance: Double, val reference: String = "")`
- **Status Engine Terhubung**: Ya — Terkoneksi ke EntitlementEngine, CentralCreditLedgerService & DunningEngine
- **Tabel Supabase Terpengaruh**: `central_credit_ledger` (SELECT/INSERT), `tenant_subscriptions` (SELECT)

### 18. `GET` /api/v1/billing/seats
- **Grep Confirmation**: Line 371 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `SeatsListResponse`: `(val seats: List<ai.orchestree.backend.database.repositories.workforce.User>, val totalActive: Int, val seatLimit: Int)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke EntitlementEngine, CentralCreditLedgerService & DunningEngine
- **Tabel Supabase Terpengaruh**: `tenant_subscriptions` (SELECT/UPDATE)

### 19. `POST` /api/v1/billing/seats
- **Grep Confirmation**: Line 393 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `SeatCreateRequest`: `(val name: String, val email: String, val role: String = "STAFF_HUMAN", val departmentId: String = "general")`
- **Response Body Schema**: `HttpStatusCode.Created, mapOf("status" to "created", "seat_id" to newUser.id`
- **Status Engine Terhubung**: Ya — Terkoneksi ke EntitlementEngine, CentralCreditLedgerService & DunningEngine
- **Tabel Supabase Terpengaruh**: `tenant_subscriptions` (SELECT/UPDATE)

### 20. `DELETE` /api/v1/billing/seats/{id}
- **Grep Confirmation**: Line 431 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, mapOf("status" to "deleted", "seat_id" to seatId`
- **Status Engine Terhubung**: Ya — Terkoneksi ke EntitlementEngine, CentralCreditLedgerService & DunningEngine
- **Tabel Supabase Terpengaruh**: `tenant_subscriptions` (SELECT/UPDATE)

### 21. `GET` /api/v1/billing/agents
- **Grep Confirmation**: Line 453 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `AgentsListResponse`: `(val agents: List<ai.orchestree.backend.database.repositories.workforce.AgentModel>, val totalActive: Int, val agentLimit: Int)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke EntitlementEngine, CentralCreditLedgerService & DunningEngine
- **Tabel Supabase Terpengaruh**: `tenant_subscriptions` (SELECT/UPDATE)

### 22. `POST` /api/v1/billing/agents
- **Grep Confirmation**: Line 475 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `BillingAgentCreateRequest`: `(val name: String, val role: String = "Autonomous AI Specialist", val personaCode: String = "AGENT", val departmentId: String? = null)`
- **Response Body Schema**: `HttpStatusCode.Created, mapOf("status" to "created", "agent_id" to newAgent.id`
- **Status Engine Terhubung**: Ya — Terkoneksi ke EntitlementEngine, CentralCreditLedgerService & DunningEngine
- **Tabel Supabase Terpengaruh**: `tenant_subscriptions` (SELECT/UPDATE)

### 23. `DELETE` /api/v1/billing/agents/{id}
- **Grep Confirmation**: Line 513 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, mapOf("status" to "deleted", "agent_id" to agentId`
- **Status Engine Terhubung**: Ya — Terkoneksi ke EntitlementEngine, CentralCreditLedgerService & DunningEngine
- **Tabel Supabase Terpengaruh**: `tenant_subscriptions` (SELECT/UPDATE)

### 24. `GET` /api/v1/billing/invoices
- **Grep Confirmation**: Line 534 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, invoices`
- **Status Engine Terhubung**: Ya — Terkoneksi ke EntitlementEngine, CentralCreditLedgerService & DunningEngine
- **Tabel Supabase Terpengaruh**: `invoices` (SELECT/INSERT), `dunning_logs` (SELECT/INSERT)

### 25. `GET` /api/v1/billing/invoices/{id}
- **Grep Confirmation**: Line 545 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, invoice`
- **Status Engine Terhubung**: Ya — Terkoneksi ke EntitlementEngine, CentralCreditLedgerService & DunningEngine
- **Tabel Supabase Terpengaruh**: `invoices` (SELECT/INSERT), `dunning_logs` (SELECT/INSERT)

### 26. `POST` /api/v1/billing/invoices/{id}/fail-dunning
- **Grep Confirmation**: Line 561 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `Map<String, String`
- **Response Body Schema**: `HttpStatusCode.OK, result`
- **Status Engine Terhubung**: Ya — Terkoneksi ke EntitlementEngine, CentralCreditLedgerService & DunningEngine
- **Tabel Supabase Terpengaruh**: `invoices` (SELECT/INSERT), `dunning_logs` (SELECT/INSERT)

### 27. `POST` /api/v1/billing/payment
- **Grep Confirmation**: Line 580 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `PaymentCreateRequest`: `(val planId: String? = null, val invoiceId: String? = null, val amount: Double? = null, val currency: String = "IDR")`
- **Response Body Schema**: `PaymentInitiateResponse`: `(val status: String = "pending", @kotlinx.serialization.SerialName("invoice_id")`
- **Status Engine Terhubung**: Ya — Terkoneksi ke EntitlementEngine, CentralCreditLedgerService & DunningEngine
- **Tabel Supabase Terpengaruh**: `tenant_subscriptions` (SELECT/UPDATE)

### 28. `POST` /api/v1/billing/payment/webhook
- **Grep Confirmation**: Line 623 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK / Standard JSON DTO`
- **Status Engine Terhubung**: Ya — Terkoneksi ke EntitlementEngine, CentralCreditLedgerService & DunningEngine
- **Tabel Supabase Terpengaruh**: `tenant_subscriptions` (SELECT/UPDATE)

### 29. `POST` /api/v1/billing/webhook/midtrans
- **Grep Confirmation**: Line 624 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK / Standard JSON DTO`
- **Status Engine Terhubung**: Ya — Terkoneksi ke EntitlementEngine, CentralCreditLedgerService & DunningEngine
- **Tabel Supabase Terpengaruh**: `tenant_subscriptions` (SELECT/UPDATE)

### 30. `GET` /api/v1/billing/upgrade-recommendation
- **Grep Confirmation**: Line 630 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `UpgradeRecommendationResponse`: `(val level: String, // NO_UPGRADE, SOFT_RECOMMENDATION, STRONG_RECOMMENDATION, LIMIT_REACHED val title: String, val message: String, val suggestedPlanCode: String, val suggestedPlanName: String, val utilizationMetrics: Map<String, Double> = emptyMap()`
- **Status Engine Terhubung**: Ya — Terkoneksi ke EntitlementEngine, CentralCreditLedgerService & DunningEngine
- **Tabel Supabase Terpengaruh**: `tenant_subscriptions` (SELECT/UPDATE)

## 16. Super Admin, Operations & Security Command
- **File Sumber**: `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Jumlah Endpoint**: **92**

### 1. `GET` /api/v1/admin/presence/security-stats
- **Grep Confirmation**: Line 442 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, stats`
- **Status Engine Terhubung**: Ya — Terkoneksi ke AdminDomainStores & Repository Layer
- **Tabel Supabase Terpengaruh**: `audit_logs` (SELECT), `tenants` (SELECT)

### 2. `GET` /api/v1/admin/tenants
- **Grep Confirmation**: Line 449 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, tenantItems`
- **Status Engine Terhubung**: Ya — Terkoneksi ke AdminDomainStores & Repository Layer
- **Tabel Supabase Terpengaruh**: `audit_logs` (SELECT), `tenants` (SELECT)

### 3. `POST` /api/v1/admin/tenants
- **Grep Confirmation**: Line 490 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `AdminTenantCreateRequest`: `(val name: String, val tier: String = "GROWTH", val ownerEmail: String)`
- **Response Body Schema**: `AdminTenantCreateResponse`: `(val tenantId: String, val name: String, val tier: String, val status: String)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke AdminDomainStores & Repository Layer
- **Tabel Supabase Terpengaruh**: `audit_logs` (SELECT), `tenants` (SELECT)

### 4. `GET` /api/v1/admin/llm-providers
- **Grep Confirmation**: Line 507 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `toList`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ModelRouter & LlmProviderModelRepository
- **Tabel Supabase Terpengaruh**: `llm_providers` (SELECT/INSERT/UPDATE), `llm_provider_models` (SELECT/INSERT)

### 5. `POST` /api/v1/admin/llm-providers
- **Grep Confirmation**: Line 511 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `AdminLlmProviderCreateRequest`: `(val name: String, val providerType: String = "OPENROUTER", val baseUrl: String? = null, val enabled: Boolean = true, val taskSpecialization: String = "general", val fallbackPriority: Int = 1, val apiKey: String? = null, val models: List<String> = emptyList()`
- **Response Body Schema**: `AdminLlmProviderCreateResponse`: `(val provider: String, val type: String, val status: String)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ModelRouter & LlmProviderModelRepository
- **Tabel Supabase Terpengaruh**: `llm_providers` (SELECT/INSERT/UPDATE), `llm_provider_models` (SELECT/INSERT)

### 6. `PUT` /api/v1/admin/llm-providers/{id}
- **Grep Confirmation**: Line 533 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `AdminLlmProviderCreateRequest`: `(val name: String, val providerType: String = "OPENROUTER", val baseUrl: String? = null, val enabled: Boolean = true, val taskSpecialization: String = "general", val fallbackPriority: Int = 1, val apiKey: String? = null, val models: List<String> = emptyList()`
- **Response Body Schema**: `HttpStatusCode.OK, updated`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ModelRouter & LlmProviderModelRepository
- **Tabel Supabase Terpengaruh**: `llm_providers` (SELECT/INSERT/UPDATE), `llm_provider_models` (SELECT/INSERT)

### 7. `DELETE` /api/v1/admin/llm-providers/{id}
- **Grep Confirmation**: Line 554 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, mapOf("status" to "DELETED", "id" to id`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ModelRouter & LlmProviderModelRepository
- **Tabel Supabase Terpengaruh**: `llm_providers` (SELECT/INSERT/UPDATE), `llm_provider_models` (SELECT/INSERT)

### 8. `POST` /api/v1/admin/llm-providers/{id}/toggle-status
- **Grep Confirmation**: Line 564 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, updated`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ModelRouter & LlmProviderModelRepository
- **Tabel Supabase Terpengaruh**: `llm_providers` (SELECT/INSERT/UPDATE), `llm_provider_models` (SELECT/INSERT)

### 9. `GET` /api/v1/admin/image-providers
- **Grep Confirmation**: Line 579 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `toList`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ModelRouter & LlmProviderModelRepository
- **Tabel Supabase Terpengaruh**: `llm_providers` (SELECT/INSERT/UPDATE), `llm_provider_models` (SELECT/INSERT)

### 10. `POST` /api/v1/admin/image-providers
- **Grep Confirmation**: Line 583 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `AdminImageProviderCreateRequest`: `(val name: String, val providerType: String, val models: List<String> = listOf("image-gen-v1")`
- **Response Body Schema**: `AdminImageProviderItem`: `(val id: String, val name: String, val providerType: String, val models: List<String> = listOf("image-gen-v1")`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ModelRouter & LlmProviderModelRepository
- **Tabel Supabase Terpengaruh**: `llm_providers` (SELECT/INSERT/UPDATE), `llm_provider_models` (SELECT/INSERT)

### 11. `DELETE` /api/v1/admin/image-providers/{id}
- **Grep Confirmation**: Line 599 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, mapOf("status" to "DELETED", "id" to id`
- **Status Engine Terhubung**: Ya — Terkoneksi ke ModelRouter & LlmProviderModelRepository
- **Tabel Supabase Terpengaruh**: `llm_providers` (SELECT/INSERT/UPDATE), `llm_provider_models` (SELECT/INSERT)

### 12. `GET` /api/v1/admin/master-data/categories
- **Grep Confirmation**: Line 612 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, categories`
- **Status Engine Terhubung**: Ya — Terkoneksi ke MasterDataRepository & AdminDomainStores
- **Tabel Supabase Terpengaruh**: `master_data_catalog` (SELECT/INSERT/UPDATE)

### 13. `GET` /api/v1/admin/master-data
- **Grep Confirmation**: Line 628 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, allItems`
- **Status Engine Terhubung**: Ya — Terkoneksi ke MasterDataRepository & AdminDomainStores
- **Tabel Supabase Terpengaruh**: `master_data_catalog` (SELECT/INSERT/UPDATE)

### 14. `POST` /api/v1/admin/master-data
- **Grep Confirmation**: Line 639 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `AdminMasterDataCreateRequest`: `(val category: String, val key: String, val value: String, val description: String? = null)`
- **Response Body Schema**: `AdminMasterDataCreateResponse`: `(val id: String, val category: String, val key: String, val status: String)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke MasterDataRepository & AdminDomainStores
- **Tabel Supabase Terpengaruh**: `master_data_catalog` (SELECT/INSERT/UPDATE)

### 15. `DELETE` /api/v1/admin/master-data/{id}
- **Grep Confirmation**: Line 661 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, mapOf("status" to "DELETED", "id" to id`
- **Status Engine Terhubung**: Ya — Terkoneksi ke MasterDataRepository & AdminDomainStores
- **Tabel Supabase Terpengaruh**: `master_data_catalog` (SELECT/INSERT/UPDATE)

### 16. `DELETE` /api/v1/admin/master-data/{category}/{id}
- **Grep Confirmation**: Line 677 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, mapOf("status" to "DELETED", "id" to id, "category" to category`
- **Status Engine Terhubung**: Ya — Terkoneksi ke MasterDataRepository & AdminDomainStores
- **Tabel Supabase Terpengaruh**: `master_data_catalog` (SELECT/INSERT/UPDATE)

### 17. `GET` /api/v1/admin/skill-plugins
- **Grep Confirmation**: Line 692 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, all`
- **Status Engine Terhubung**: Ya — Terkoneksi ke McpToolRegistry & SkillPluginUploadEngine
- **Tabel Supabase Terpengaruh**: `mcp_tools` (SELECT/INSERT/UPDATE), `skill_plugins` (SELECT/INSERT)

### 18. `POST` /api/v1/admin/skill-plugins
- **Grep Confirmation**: Line 740 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `AdminSkillPluginCreateRequest`: `(val name: String, val version: String, val author: String, val executionRuntime: String = "WASM", val status: String = "PENDING_APPROVAL")`
- **Response Body Schema**: `AdminSkillPluginCreateResponse`: `(val id: String, val name: String, val status: String)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke McpToolRegistry & SkillPluginUploadEngine
- **Tabel Supabase Terpengaruh**: `mcp_tools` (SELECT/INSERT/UPDATE), `skill_plugins` (SELECT/INSERT)

### 19. `PUT` /api/v1/admin/skill-plugins/{id}
- **Grep Confirmation**: Line 789 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `AdminSkillPluginCreateRequest`: `(val name: String, val version: String, val author: String, val executionRuntime: String = "WASM", val status: String = "PENDING_APPROVAL")`
- **Response Body Schema**: `AdminSkillPluginItem`: `(val id: String, val name: String, val version: String, val author: String, val runtime: String = "WASM", val status: String = "APPROVED", val downloads: Int = 0, val declaredTools: List<String> = emptyList()`
- **Status Engine Terhubung**: Ya — Terkoneksi ke McpToolRegistry & SkillPluginUploadEngine
- **Tabel Supabase Terpengaruh**: `mcp_tools` (SELECT/INSERT/UPDATE), `skill_plugins` (SELECT/INSERT)

### 20. `DELETE` /api/v1/admin/skill-plugins/{id}
- **Grep Confirmation**: Line 833 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, mapOf("status" to "DELETED", "id" to id`
- **Status Engine Terhubung**: Ya — Terkoneksi ke McpToolRegistry & SkillPluginUploadEngine
- **Tabel Supabase Terpengaruh**: `mcp_tools` (SELECT/INSERT/UPDATE), `skill_plugins` (SELECT/INSERT)

### 21. `PATCH` /api/v1/admin/skill-plugins/{id}/status
- **Grep Confirmation**: Line 849 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `Map<String, String`
- **Response Body Schema**: `HttpStatusCode.OK, updated`
- **Status Engine Terhubung**: Ya — Terkoneksi ke McpToolRegistry & SkillPluginUploadEngine
- **Tabel Supabase Terpengaruh**: `mcp_tools` (SELECT/INSERT/UPDATE), `skill_plugins` (SELECT/INSERT)

### 22. `POST` /api/v1/admin/skill-plugins/upload
- **Grep Confirmation**: Line 874 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `AdminSkillPluginUploadRequest`: `(val pluginName: String, val version: String = "1.0.0", val author: String = "Super Admin", val manifestJson: String, val skillDefinitionMd: String = "", val zipBase64: String? = null)`
- **Response Body Schema**: `AdminSkillPluginUploadResponse`: `(val success: Boolean, val pluginId: String, val pluginName: String, val version: String, val securityScanPassed: Boolean, val declaredTools: List<String>, val validationErrors: List<String> = emptyList()`
- **Status Engine Terhubung**: Ya — Terkoneksi ke McpToolRegistry & SkillPluginUploadEngine
- **Tabel Supabase Terpengaruh**: `mcp_tools` (SELECT/INSERT/UPDATE), `skill_plugins` (SELECT/INSERT)

### 23. `GET` /api/v1/admin/mcp-tools
- **Grep Confirmation**: Line 955 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, all`
- **Status Engine Terhubung**: Ya — Terkoneksi ke McpToolRegistry & SkillPluginUploadEngine
- **Tabel Supabase Terpengaruh**: `mcp_tools` (SELECT/INSERT/UPDATE), `skill_plugins` (SELECT/INSERT)

### 24. `POST` /api/v1/admin/mcp-tools
- **Grep Confirmation**: Line 988 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `AdminMcpToolCreateRequest`: `(val name: String, val description: String, val riskLevel: String = "LOW", val requiredRole: String = "STAFF_HUMAN", val restrictedToOperationMode: String = "UNRESTRICTED", val inputSchema: String = "{}")`
- **Response Body Schema**: `AdminMcpToolCreateResponse`: `(val toolName: String, val riskLevel: String, val status: String)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke McpToolRegistry & SkillPluginUploadEngine
- **Tabel Supabase Terpengaruh**: `mcp_tools` (SELECT/INSERT/UPDATE), `skill_plugins` (SELECT/INSERT)

### 25. `PUT` /api/v1/admin/mcp-tools/{id}
- **Grep Confirmation**: Line 1047 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `AdminMcpToolCreateRequest`: `(val name: String, val description: String, val riskLevel: String = "LOW", val requiredRole: String = "STAFF_HUMAN", val restrictedToOperationMode: String = "UNRESTRICTED", val inputSchema: String = "{}")`
- **Response Body Schema**: `HttpStatusCode.OK, updated`
- **Status Engine Terhubung**: Ya — Terkoneksi ke McpToolRegistry & SkillPluginUploadEngine
- **Tabel Supabase Terpengaruh**: `mcp_tools` (SELECT/INSERT/UPDATE), `skill_plugins` (SELECT/INSERT)

### 26. `DELETE` /api/v1/admin/mcp-tools/{id}
- **Grep Confirmation**: Line 1090 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, mapOf("status" to "DELETED", "id" to id`
- **Status Engine Terhubung**: Ya — Terkoneksi ke McpToolRegistry & SkillPluginUploadEngine
- **Tabel Supabase Terpengaruh**: `mcp_tools` (SELECT/INSERT/UPDATE), `skill_plugins` (SELECT/INSERT)

### 27. `PATCH` /api/v1/admin/mcp-tools/{id}/kill-switch
- **Grep Confirmation**: Line 1107 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, updated`
- **Status Engine Terhubung**: Ya — Terkoneksi ke McpToolRegistry & SkillPluginUploadEngine
- **Tabel Supabase Terpengaruh**: `mcp_tools` (SELECT/INSERT/UPDATE), `skill_plugins` (SELECT/INSERT)

### 28. `GET` /api/v1/admin/app-registry
- **Grep Confirmation**: Line 1146 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `toList`
- **Status Engine Terhubung**: Ya — Terkoneksi ke AdminDomainStores & Repository Layer
- **Tabel Supabase Terpengaruh**: `audit_logs` (SELECT), `tenants` (SELECT)

### 29. `POST` /api/v1/admin/app-registry
- **Grep Confirmation**: Line 1150 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `AdminAppRegistryCreateRequest`: `(val appName: String, val appType: String, val clientId: String, val scopes: List<String> = emptyList()`
- **Response Body Schema**: `AdminAppRegistryCreateResponse`: `(val id: String, val appName: String, val status: String)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke AdminDomainStores & Repository Layer
- **Tabel Supabase Terpengaruh**: `audit_logs` (SELECT), `tenants` (SELECT)

### 30. `PUT` /api/v1/admin/app-registry/{id}
- **Grep Confirmation**: Line 1170 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `AdminAppRegistryCreateRequest`: `(val appName: String, val appType: String, val clientId: String, val scopes: List<String> = emptyList()`
- **Response Body Schema**: `HttpStatusCode.OK, updated`
- **Status Engine Terhubung**: Ya — Terkoneksi ke AdminDomainStores & Repository Layer
- **Tabel Supabase Terpengaruh**: `audit_logs` (SELECT), `tenants` (SELECT)

### 31. `DELETE` /api/v1/admin/app-registry/{id}
- **Grep Confirmation**: Line 1191 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, mapOf("status" to "DELETED", "id" to id`
- **Status Engine Terhubung**: Ya — Terkoneksi ke AdminDomainStores & Repository Layer
- **Tabel Supabase Terpengaruh**: `audit_logs` (SELECT), `tenants` (SELECT)

### 32. `PATCH` /api/v1/admin/app-registry/{id}/mark-migration
- **Grep Confirmation**: Line 1201 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `Map<String, String`
- **Response Body Schema**: `HttpStatusCode.OK, updated`
- **Status Engine Terhubung**: Ya — Terkoneksi ke AdminDomainStores & Repository Layer
- **Tabel Supabase Terpengaruh**: `audit_logs` (SELECT), `tenants` (SELECT)

### 33. `GET` /api/v1/admin/analytics/tenant-workforce-summary
- **Grep Confirmation**: Line 1222 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `AdminWorkforceMonitoringSummary`: `(val totalActiveDepartments: Int, val totalAiAgents: Int, val humanToAiRatio: String, val departmentDistribution: List<DepartmentCountItem>, val aiJobTitleDistribution: List<AiJobTitleCountItem>)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke PlatformAnalyticsRepository
- **Tabel Supabase Terpengaruh**: `llm_usage_logs` (SELECT), `central_credit_ledger` (SELECT), `tasks` (SELECT)

### 34. `GET` /api/v1/admin/monitoring/system-overview
- **Grep Confirmation**: Line 1292 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `SystemMonitoringOverview`: `(val status: String, val uptimeSeconds: Long, val timestamp: Long, val providerHealth: List<AdminHealthReportItem>, val serverHealth: ServerHealthMetrics, val jobQueueStatus: JobQueueStatusMetrics, val securityIncidents: SecurityIncidentsMetrics, val rateLimitViolations: RateLimitViolationsMetrics)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke HealthEngine & SystemMonitoringMetrics
- **Tabel Supabase Terpengaruh**: Stateless / Live JVM & OS System Metrics, `dead_letter_queue` (SELECT)

### 35. `POST` /api/v1/admin/swarm/freeze
- **Grep Confirmation**: Line 1353 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `Map<String, String`
- **Response Body Schema**: `HttpStatusCode.OK, mapOf("status" to "FROZEN", "detail" to freezeDetail`
- **Status Engine Terhubung**: Ya — Terkoneksi ke AdminDomainStores & Repository Layer
- **Tabel Supabase Terpengaruh**: `audit_logs` (SELECT), `tenants` (SELECT)

### 36. `POST` /api/v1/admin/swarm/resume
- **Grep Confirmation**: Line 1367 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `Map<String, String`
- **Response Body Schema**: `HttpStatusCode.OK, mapOf("status" to "RESUMED", "success" to resumed`
- **Status Engine Terhubung**: Ya — Terkoneksi ke AdminDomainStores & Repository Layer
- **Tabel Supabase Terpengaruh**: `audit_logs` (SELECT), `tenants` (SELECT)

### 37. `GET` /api/v1/admin/swarm/status
- **Grep Confirmation**: Line 1380 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, status`
- **Status Engine Terhubung**: Ya — Terkoneksi ke AdminDomainStores & Repository Layer
- **Tabel Supabase Terpengaruh**: `audit_logs` (SELECT), `tenants` (SELECT)

### 38. `GET` /api/v1/admin/audit-logs
- **Grep Confirmation**: Line 1388 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, dbLogs`
- **Status Engine Terhubung**: Ya — Terkoneksi ke AuditLogger & SecurityAuditService
- **Tabel Supabase Terpengaruh**: `audit_logs` (SELECT)

### 39. `GET` /api/v1/admin/usage
- **Grep Confirmation**: Line 1449 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `AdminUsageAnalyticsResponse`: `(val groupBy: String, val totalTokens: Long, val totalCostUsd: Double, val breakdown: List<AdminTenantUsageBreakdown>)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke PlatformAnalyticsRepository
- **Tabel Supabase Terpengaruh**: `llm_usage_logs` (SELECT), `central_credit_ledger` (SELECT), `tasks` (SELECT)

### 40. `GET` /api/v1/admin/llm-usage
- **Grep Confirmation**: Line 1472 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `LlmUsageSummaryResponse`: `(val totalTokens: Int, val totalCostUsd: Double, val activeProviders: List<String>)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke PlatformAnalyticsRepository
- **Tabel Supabase Terpengaruh**: `llm_usage_logs` (SELECT), `central_credit_ledger` (SELECT), `tasks` (SELECT)

### 41. `GET` /api/v1/admin/health-check
- **Grep Confirmation**: Line 1485 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `AdminHealthResponse`: `(val status: String, val timestamp: Long, val reports: List<AdminHealthReportItem>)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke AdminDomainStores & Repository Layer
- **Tabel Supabase Terpengaruh**: `audit_logs` (SELECT), `tenants` (SELECT)

### 42. `POST` /api/v1/admin/jobs/trigger
- **Grep Confirmation**: Line 1506 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `TriggerJobApiRequest`: `(val jobName: String, val tenantId: String = "tenant-admin", val forceTestFailure: Boolean = false)`
- **Response Body Schema**: `AdminJobTriggerResponse`: `(val jobName: String, val tenantId: String, val status: String, val triggeredAt: Long)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke AdminDomainStores & Repository Layer
- **Tabel Supabase Terpengaruh**: `audit_logs` (SELECT), `tenants` (SELECT)

### 43. `GET` /api/v1/admin/billing/subscriptions
- **Grep Confirmation**: Line 1525 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, items`
- **Status Engine Terhubung**: Ya — Terkoneksi ke AdminDomainStores & Repository Layer
- **Tabel Supabase Terpengaruh**: `audit_logs` (SELECT), `tenants` (SELECT)

### 44. `GET` /api/v1/admin/billing/invoices
- **Grep Confirmation**: Line 1578 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, invoices`
- **Status Engine Terhubung**: Ya — Terkoneksi ke AdminDomainStores & Repository Layer
- **Tabel Supabase Terpengaruh**: `audit_logs` (SELECT), `tenants` (SELECT)

### 45. `GET` /api/v1/admin/specialist-agents
- **Grep Confirmation**: Line 1617 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, agents`
- **Status Engine Terhubung**: Ya — Terkoneksi ke AdminDomainStores & Repository Layer
- **Tabel Supabase Terpengaruh**: `audit_logs` (SELECT), `tenants` (SELECT)

### 46. `GET` /api/v1/admin/studio/templates
- **Grep Confirmation**: Line 1647 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, templates`
- **Status Engine Terhubung**: Ya — Terkoneksi ke AdminDomainStores & Repository Layer
- **Tabel Supabase Terpengaruh**: `audit_logs` (SELECT), `tenants` (SELECT)

### 47. `GET` /api/v1/admin/dead-letter-queue
- **Grep Confirmation**: Line 1661 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, items`
- **Status Engine Terhubung**: Ya — Terkoneksi ke SchedulerEngine & DeadLetterQueueRepository
- **Tabel Supabase Terpengaruh**: `dead_letter_queue` (SELECT/UPDATE)

### 48. `POST` /api/v1/admin/dead-letter-queue/{id}/reprocess
- **Grep Confirmation**: Line 1668 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `AdminDlqReprocessResponse`: `(val status: String, val id: String, val summary: String, val timestamp: Long)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke SchedulerEngine & DeadLetterQueueRepository
- **Tabel Supabase Terpengaruh**: `dead_letter_queue` (SELECT/UPDATE)

### 49. `POST` /api/v1/admin/workflow-executions/{id}/replay
- **Grep Confirmation**: Line 1699 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, replayResult`
- **Status Engine Terhubung**: Ya — Terkoneksi ke OrchestrationEngine (Deterministic Replay Sandbox)
- **Tabel Supabase Terpengaruh**: `workflow_executions` (SELECT/UPDATE)

### 50. `GET` /api/v1/admin/workflow-executions
- **Grep Confirmation**: Line 1722 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, executions`
- **Status Engine Terhubung**: Ya — Terkoneksi ke OrchestrationEngine (Deterministic Replay Sandbox)
- **Tabel Supabase Terpengaruh**: `workflow_executions` (SELECT/UPDATE)

### 51. `GET` /api/v1/admin/analytics/overview
- **Grep Confirmation**: Line 1736 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, overview`
- **Status Engine Terhubung**: Ya — Terkoneksi ke PlatformAnalyticsRepository
- **Tabel Supabase Terpengaruh**: `llm_usage_logs` (SELECT), `central_credit_ledger` (SELECT), `tasks` (SELECT)

### 52. `GET` /api/v1/admin/analytics/usage-credit
- **Grep Confirmation**: Line 1744 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, usageList`
- **Status Engine Terhubung**: Ya — Terkoneksi ke PlatformAnalyticsRepository
- **Tabel Supabase Terpengaruh**: `llm_usage_logs` (SELECT), `central_credit_ledger` (SELECT), `tasks` (SELECT)

### 53. `GET` /api/v1/admin/analytics/llm-usage-platform-wide
- **Grep Confirmation**: Line 1752 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, llmUsage`
- **Status Engine Terhubung**: Ya — Terkoneksi ke PlatformAnalyticsRepository
- **Tabel Supabase Terpengaruh**: `llm_usage_logs` (SELECT), `central_credit_ledger` (SELECT), `tasks` (SELECT)

### 54. `GET` /api/v1/admin/analytics/kpi-summary
- **Grep Confirmation**: Line 1760 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, kpi`
- **Status Engine Terhubung**: Ya — Terkoneksi ke PlatformAnalyticsRepository
- **Tabel Supabase Terpengaruh**: `llm_usage_logs` (SELECT), `central_credit_ledger` (SELECT), `tasks` (SELECT)

### 55. `GET` /api/v1/admin/analytics/daily-task-performance
- **Grep Confirmation**: Line 1768 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, metrics`
- **Status Engine Terhubung**: Ya — Terkoneksi ke PlatformAnalyticsRepository
- **Tabel Supabase Terpengaruh**: `llm_usage_logs` (SELECT), `central_credit_ledger` (SELECT), `tasks` (SELECT)

### 56. `GET` /api/v1/admin/analytics/task-activity-summary
- **Grep Confirmation**: Line 1779 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, summary`
- **Status Engine Terhubung**: Ya — Terkoneksi ke PlatformAnalyticsRepository
- **Tabel Supabase Terpengaruh**: `llm_usage_logs` (SELECT), `central_credit_ledger` (SELECT), `tasks` (SELECT)

### 57. `GET` /api/v1/admin/analytics/universal-selection-usage
- **Grep Confirmation**: Line 1790 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, usage`
- **Status Engine Terhubung**: Ya — Terkoneksi ke PlatformAnalyticsRepository
- **Tabel Supabase Terpengaruh**: `llm_usage_logs` (SELECT), `central_credit_ledger` (SELECT), `tasks` (SELECT)

### 58. `POST` /api/v1/admin/analytics/transactions
- **Grep Confirmation**: Line 1797 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `AdminRecordTransactionRequest`: `(val tenantId: String, val customerId: String = "cust-new-001", val amount: Double, val orderNumber: String? = null)`
- **Response Body Schema**: `HttpStatusCode.Created, mapOf( "status" to "RECORDED", "orderId" to order.id, "orderNumber" to or...`
- **Status Engine Terhubung**: Ya — Terkoneksi ke PlatformAnalyticsRepository
- **Tabel Supabase Terpengaruh**: `llm_usage_logs` (SELECT), `central_credit_ledger` (SELECT), `tasks` (SELECT)

### 59. `GET` /api/v1/admin/payment-reconciliation/orders
- **Grep Confirmation**: Line 1829 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, dtoList`
- **Status Engine Terhubung**: Ya — Terkoneksi ke AdminDomainStores & Repository Layer
- **Tabel Supabase Terpengaruh**: `audit_logs` (SELECT), `tenants` (SELECT)

### 60. `GET` /api/v1/admin/payment-reconciliation/queue
- **Grep Confirmation**: Line 1863 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, items`
- **Status Engine Terhubung**: Ya — Terkoneksi ke AdminDomainStores & Repository Layer
- **Tabel Supabase Terpengaruh**: `audit_logs` (SELECT), `tenants` (SELECT)

### 61. `POST` /api/v1/admin/payment-reconciliation/{id}/confirm
- **Grep Confirmation**: Line 1876 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `ConfirmPaymentReconciliationRequest`: `(val reason: String)`
- **Response Body Schema**: `HttpStatusCode.OK, mapOf( "status" to "CONFIRMED", "queueId" to item.id, "orderId" to item.orderI...`
- **Status Engine Terhubung**: Ya — Terkoneksi ke AdminDomainStores & Repository Layer
- **Tabel Supabase Terpengaruh**: `audit_logs` (SELECT), `tenants` (SELECT)

### 62. `POST` /api/v1/admin/payment-reconciliation/{id}/reject
- **Grep Confirmation**: Line 1956 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `RejectPaymentReconciliationRequest`: `(val reason: String)`
- **Response Body Schema**: `HttpStatusCode.OK, mapOf( "status" to "REJECTED", "queueId" to item.id, "orderId" to item.orderId...`
- **Status Engine Terhubung**: Ya — Terkoneksi ke AdminDomainStores & Repository Layer
- **Tabel Supabase Terpengaruh**: `audit_logs` (SELECT), `tenants` (SELECT)

### 63. `POST` /api/v1/admin/payment-reconciliation/trigger-check
- **Grep Confirmation**: Line 2012 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, mapOf( "status" to "COMPLETED", "checkedCount" to res.checkedCount, "autoRecon...`
- **Status Engine Terhubung**: Ya — Terkoneksi ke AdminDomainStores & Repository Layer
- **Tabel Supabase Terpengaruh**: `audit_logs` (SELECT), `tenants` (SELECT)

### 64. `POST` /api/v1/admin/payment-reconciliation/simulate-stuck
- **Grep Confirmation**: Line 2028 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `X-Tenant-ID: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.Created, mapOf( "status" to "SIMULATED", // allowed: drill status indicator "order...`
- **Status Engine Terhubung**: Ya — Terkoneksi ke AdminDomainStores & Repository Layer
- **Tabel Supabase Terpengaruh**: `audit_logs` (SELECT), `tenants` (SELECT)

### 65. `GET` /api/v1/admin/commercial/plans
- **Grep Confirmation**: Line 2089 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, plans`
- **Status Engine Terhubung**: Ya — Terkoneksi ke CommercialCreditEngine & PlanManagementService
- **Tabel Supabase Terpengaruh**: `commercial_plans` (SELECT/INSERT/UPDATE), `tenant_subscriptions` (SELECT/UPDATE)

### 66. `POST` /api/v1/admin/commercial/plans
- **Grep Confirmation**: Line 2099 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `CommercialPlanUpsertRequest`: `(val id: String? = null, val planCode: String, val planName: String, val billingInterval: String = "monthly", val price: Double? = null, val currency: String = "IDR", val creditAllocation: Double? = null, val humanSeatLimit: Int? = null, val aiAgentLimit: Int? = null, val isPriceVisible: Boolean = true, val isActive: Boolean = true, val sortOrder: Int? = null)`
- **Response Body Schema**: `HttpStatusCode.OK, saved`
- **Status Engine Terhubung**: Ya — Terkoneksi ke CommercialCreditEngine & PlanManagementService
- **Tabel Supabase Terpengaruh**: `commercial_plans` (SELECT/INSERT/UPDATE), `tenant_subscriptions` (SELECT/UPDATE)

### 67. `DELETE` /api/v1/admin/commercial/plans/{id}
- **Grep Confirmation**: Line 2124 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, mapOf("success" to deleted, "id" to id`
- **Status Engine Terhubung**: Ya — Terkoneksi ke CommercialCreditEngine & PlanManagementService
- **Tabel Supabase Terpengaruh**: `commercial_plans` (SELECT/INSERT/UPDATE), `tenant_subscriptions` (SELECT/UPDATE)

### 68. `GET` /api/v1/admin/commercial/entitlements-matrix
- **Grep Confirmation**: Line 2136 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, matrix`
- **Status Engine Terhubung**: Ya — Terkoneksi ke CommercialCreditEngine & PlanManagementService
- **Tabel Supabase Terpengaruh**: `commercial_plans` (SELECT/INSERT/UPDATE), `tenant_subscriptions` (SELECT/UPDATE)

### 69. `POST` /api/v1/admin/commercial/entitlements
- **Grep Confirmation**: Line 2146 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `EntitlementUpdateRequest`: `(val planCode: String, val featureKey: String, val value: String)`
- **Response Body Schema**: `HttpStatusCode.OK, mapOf("success" to ok`
- **Status Engine Terhubung**: Ya — Terkoneksi ke CommercialCreditEngine & PlanManagementService
- **Tabel Supabase Terpengaruh**: `commercial_plans` (SELECT/INSERT/UPDATE), `tenant_subscriptions` (SELECT/UPDATE)

### 70. `GET` /api/v1/admin/commercial/custom-override/{tenantId}
- **Grep Confirmation**: Line 2158 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `TenantCustomOverrideResponse`: `(val tenantId: String, val customEntitlementOverride: String?)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke CommercialCreditEngine & PlanManagementService
- **Tabel Supabase Terpengaruh**: `commercial_plans` (SELECT/INSERT/UPDATE), `tenant_subscriptions` (SELECT/UPDATE)

### 71. `POST` /api/v1/admin/commercial/custom-override/{tenantId}
- **Grep Confirmation**: Line 2169 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `TenantCustomOverrideRequest`: `(val overrideJson: String)`
- **Response Body Schema**: `HttpStatusCode.OK, mapOf("success" to ok, "tenantId" to tenantId`
- **Status Engine Terhubung**: Ya — Terkoneksi ke CommercialCreditEngine & PlanManagementService
- **Tabel Supabase Terpengaruh**: `commercial_plans` (SELECT/INSERT/UPDATE), `tenant_subscriptions` (SELECT/UPDATE)

### 72. `GET` /api/v1/admin/commercial/metering-rules
- **Grep Confirmation**: Line 2182 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, rules`
- **Status Engine Terhubung**: Ya — Terkoneksi ke CommercialCreditEngine & PlanManagementService
- **Tabel Supabase Terpengaruh**: `commercial_plans` (SELECT/INSERT/UPDATE), `tenant_subscriptions` (SELECT/UPDATE)

### 73. `POST` /api/v1/admin/commercial/metering-rules
- **Grep Confirmation**: Line 2192 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `CreditMeteringRule`: `(val id: String = UUID.randomUUID()`
- **Response Body Schema**: `HttpStatusCode.OK, saved`
- **Status Engine Terhubung**: Ya — Terkoneksi ke CommercialCreditEngine & PlanManagementService
- **Tabel Supabase Terpengaruh**: `commercial_plans` (SELECT/INSERT/UPDATE), `tenant_subscriptions` (SELECT/UPDATE)

### 74. `DELETE` /api/v1/admin/commercial/metering-rules/{activityType}
- **Grep Confirmation**: Line 2203 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, mapOf("success" to ok, "activityType" to activityType`
- **Status Engine Terhubung**: Ya — Terkoneksi ke CommercialCreditEngine & PlanManagementService
- **Tabel Supabase Terpengaruh**: `commercial_plans` (SELECT/INSERT/UPDATE), `tenant_subscriptions` (SELECT/UPDATE)

### 75. `GET` /api/v1/admin/commercial/cost-factors
- **Grep Confirmation**: Line 2215 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, factors`
- **Status Engine Terhubung**: Ya — Terkoneksi ke CommercialCreditEngine & PlanManagementService
- **Tabel Supabase Terpengaruh**: `commercial_plans` (SELECT/INSERT/UPDATE), `tenant_subscriptions` (SELECT/UPDATE)

### 76. `POST` /api/v1/admin/commercial/cost-factors
- **Grep Confirmation**: Line 2225 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `CreditCostFactor`: `(val id: String = UUID.randomUUID()`
- **Response Body Schema**: `HttpStatusCode.OK, saved`
- **Status Engine Terhubung**: Ya — Terkoneksi ke CommercialCreditEngine & PlanManagementService
- **Tabel Supabase Terpengaruh**: `commercial_plans` (SELECT/INSERT/UPDATE), `tenant_subscriptions` (SELECT/UPDATE)

### 77. `DELETE` /api/v1/admin/commercial/cost-factors/{factorType}/{factorKey}
- **Grep Confirmation**: Line 2236 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, mapOf("success" to ok, "factorType" to factorType, "factorKey" to factorKey`
- **Status Engine Terhubung**: Ya — Terkoneksi ke CommercialCreditEngine & PlanManagementService
- **Tabel Supabase Terpengaruh**: `commercial_plans` (SELECT/INSERT/UPDATE), `tenant_subscriptions` (SELECT/UPDATE)

### 78. `POST` /api/v1/admin/commercial/simulate-cost
- **Grep Confirmation**: Line 2249 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `CreditCostContext`: `(val activityType: String, val complexityLevel: String = "simple", // 'simple'/'medium'/'complex' val modelUsed: String = "standard", // 'openrouter'/'groq'/'deepseek'/'claude'/'kimi'/'standard' val toolsInvoked: Int = 0, val executionType: String = "single_step" // 'single_step'/'multi_step'/'autonomous')`
- **Response Body Schema**: `HttpStatusCode.OK, res`
- **Status Engine Terhubung**: Ya — Terkoneksi ke CommercialCreditEngine & PlanManagementService
- **Tabel Supabase Terpengaruh**: `commercial_plans` (SELECT/INSERT/UPDATE), `tenant_subscriptions` (SELECT/UPDATE)

### 79. `POST` /api/v1/admin/billing/credit-adjustment
- **Grep Confirmation**: Line 2265 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `ManualCreditAdjustmentRequest`: `(val tenantId: String, val amount: Double, val ledgerType: String, // 'CREDIT_ADJUSTMENT', 'CREDIT_BONUS', 'CREDIT_REFUNDED', 'CREDIT_EXPIRED' val reason: String, val operatorId: String = "superadmin@orchestree.ai")`
- **Response Body Schema**: `ManualCreditAdjustmentResponse`: `(val status: String, val tenantId: String, val amount: Double, val ledgerType: String, val newAvailableBalance: Double, val operatorId: String, val reason: String, val wallet: AiCreditWallet)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke AdminDomainStores & Repository Layer
- **Tabel Supabase Terpengaruh**: `audit_logs` (SELECT), `tenants` (SELECT)

### 80. `GET` /api/v1/admin/billing/tenant-wallet/{tenantId}
- **Grep Confirmation**: Line 2296 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, mapOf( "wallet" to wallet, "availableBalance" to totalAvail, "totalLedger" to ...`
- **Status Engine Terhubung**: Ya — Terkoneksi ke AdminDomainStores & Repository Layer
- **Tabel Supabase Terpengaruh**: `audit_logs` (SELECT), `tenants` (SELECT)

### 81. `GET` /api/v1/admin/financial-command-center
- **Grep Confirmation**: Line 2321 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, data`
- **Status Engine Terhubung**: Ya — Terkoneksi ke AdminDomainStores & Repository Layer
- **Tabel Supabase Terpengaruh**: `audit_logs` (SELECT), `tenants` (SELECT)

### 82. `GET` /api/v1/admin/analytics/financial-command-center
- **Grep Confirmation**: Line 2331 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, data`
- **Status Engine Terhubung**: Ya — Terkoneksi ke PlatformAnalyticsRepository
- **Tabel Supabase Terpengaruh**: `llm_usage_logs` (SELECT), `central_credit_ledger` (SELECT), `tasks` (SELECT)

### 83. `POST` /api/v1/admin/platform-assets/icon-logo
- **Grep Confirmation**: Line 2346 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `ai.orchestree.backend.api.BrandLogoUploadRequest`
- **Response Body Schema**: `HttpStatusCode.Created, result`
- **Status Engine Terhubung**: Ya — Terkoneksi ke AdminDomainStores & Repository Layer
- **Tabel Supabase Terpengaruh**: `audit_logs` (SELECT), `tenants` (SELECT)

### 84. `GET` /api/v1/admin/platform-assets/icon-logo
- **Grep Confirmation**: Line 2361 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, mapOf("platformIconLogoUrl" to (url ?: ""`
- **Status Engine Terhubung**: Ya — Terkoneksi ke AdminDomainStores & Repository Layer
- **Tabel Supabase Terpengaruh**: `audit_logs` (SELECT), `tenants` (SELECT)

### 85. `GET` /api/v1/admin/security/ip-allowlist
- **Grep Confirmation**: Line 2369 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `AdminIpAllowlistDto`: `(val enabled: Boolean, val allowedIps: List<String> = emptyList()`
- **Status Engine Terhubung**: Ya — Terkoneksi ke AdminDomainStores & Repository Layer
- **Tabel Supabase Terpengaruh**: `audit_logs` (SELECT), `tenants` (SELECT)

### 86. `POST` /api/v1/admin/security/ip-allowlist
- **Grep Confirmation**: Line 2375 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`, `X-Operator-Id: <operator-id>`
- **Request Body Schema**: `AdminIpAllowlistDto`: `(val enabled: Boolean, val allowedIps: List<String> = emptyList()`
- **Response Body Schema**: `HttpStatusCode.OK, req`
- **Status Engine Terhubung**: Ya — Terkoneksi ke AdminDomainStores & Repository Layer
- **Tabel Supabase Terpengaruh**: `audit_logs` (SELECT), `tenants` (SELECT)

### 87. `POST` /api/v1/admin/support/impersonate
- **Grep Confirmation**: Line 2395 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`, `X-Operator-Id: <operator-id>`
- **Request Body Schema**: `AdminSupportImpersonateRequest`: `(val targetTenantId: String, val reason: String, val durationMinutes: Long = 30)`
- **Response Body Schema**: `AdminSupportSessionResponse`: `(val sessionId: String, val operatorId: String, val targetTenantId: String, val reason: String, val token: String, val expiresAt: Long)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke AdminDomainStores & Repository Layer
- **Tabel Supabase Terpengaruh**: `audit_logs` (SELECT), `tenants` (SELECT)

### 88. `GET` /api/v1/admin/support/impersonate/{sessionId}
- **Grep Confirmation**: Line 2420 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `AdminSupportSessionResponse`: `(val sessionId: String, val operatorId: String, val targetTenantId: String, val reason: String, val token: String, val expiresAt: Long)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke AdminDomainStores & Repository Layer
- **Tabel Supabase Terpengaruh**: `audit_logs` (SELECT), `tenants` (SELECT)

### 89. `GET` /api/v1/admin/security/csrf-token
- **Grep Confirmation**: Line 2446 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `X-CSRF-Token: <csrf-token>`
- **Request Body Schema**: None (No Request Body / Parameterized Query)
- **Response Body Schema**: `HttpStatusCode.OK, mapOf("csrfToken" to token`
- **Status Engine Terhubung**: Ya — Terkoneksi ke AdminDomainStores & Repository Layer
- **Tabel Supabase Terpengaruh**: `audit_logs` (SELECT), `tenants` (SELECT)

### 90. `POST` /api/v1/admin/login
- **Grep Confirmation**: Line 2460 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`, `X-Forwarded-For: <client-ip>`, `X-Forwarded-For: <ip-address>`
- **Request Body Schema**: `AdminLoginRequest`: `(val email: String, val password: String)`
- **Response Body Schema**: `AdminLockoutResponse`: `(val error: String, val isLocked: Boolean, val remainingSeconds: Long = 0, val failedAttempts: Int = 0)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke AdminDomainStores & Repository Layer
- **Tabel Supabase Terpengaruh**: `audit_logs` (SELECT), `tenants` (SELECT)

### 91. `POST` /api/v1/admin/auth/login
- **Grep Confirmation**: Line 2525 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`, `X-Forwarded-For: <client-ip>`, `X-Forwarded-For: <ip-address>`
- **Request Body Schema**: `AdminLoginRequest`: `(val email: String, val password: String)`
- **Response Body Schema**: `AdminLockoutResponse`: `(val error: String, val isLocked: Boolean, val remainingSeconds: Long = 0, val failedAttempts: Int = 0)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke AdminDomainStores & Repository Layer
- **Tabel Supabase Terpengaruh**: `audit_logs` (SELECT), `tenants` (SELECT)

### 92. `POST` /api/v1/admin/auth/verify-mfa
- **Grep Confirmation**: Line 2567 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`, `X-Forwarded-For: <client-ip>`, `X-Forwarded-For: <ip-address>`
- **Request Body Schema**: `AdminVerifyMfaRequest`: `(val email: String, val totpCode: String)`
- **Response Body Schema**: `AdminLockoutResponse`: `(val error: String, val isLocked: Boolean, val remainingSeconds: Long = 0, val failedAttempts: Int = 0)`
- **Status Engine Terhubung**: Ya — Terkoneksi ke AdminDomainStores & Repository Layer
- **Tabel Supabase Terpengaruh**: `audit_logs` (SELECT), `tenants` (SELECT)
