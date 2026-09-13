# DOKUMEN METHOD & ENDPOINT FINAL (GREP-CONFIRMED)
> **Definitive Reference Document**: Dihasilkan secara langsung dari verifikasi grep kode sumber aktual Ktor backend server (`ai.orchestree.backend`). Dokumen ini menggantikan `methodendpoint.txt` dan seluruh dokumen audit parsial sebelumnya.

## Ringkasan Eksekutif & Statistik Endpoint
- **Total Endpoint Terverifikasi (Grep-Confirmed)**: **308 Endpoint**
- **Total Domain Operasional**: **16 Domain**

| Domain | File Sumber | Jumlah Endpoint |
|---|---|:---:|
| 1. Core Domain - System & Webhook Gateways | `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/plugins/Routing.kt` | **10** |
| 2. Core Domain - Authentication & Profile Lifecycle | `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AuthRoutes.kt` | **6** |
| 3. Core Domain - Master Data & Public Catalog | `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/MasterDataRoutes.kt` | **9** |
| 4. Core Domain - Biometric Presence & Liveness | `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/PresenceRoutes.kt` | **6** |
| 5. Core Domain - Attendance & Geofencing | `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AttendanceRoutes.kt` | **5** |
| 6. Core Domain - Prospect Registration & Onboarding | `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/prospect/ProspectRoutes.kt` | **6** |
| 7. Core Domain - Tenants, Hybrid Workforce, Taskboard & Intel | `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt` | **46** |
| 8. Orchestration Domain - Workflow DAG, Checkpoints & MCP Execution | `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OrchestrationRoutes.kt` | **14** |
| 9. Chat Domain - Agent Direct Conversation & Company Brain Knowledge | `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/ChatRoutes.kt` | **6** |
| 10. Selection Domain - Universal Selection, Understanding & Calibration | `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/SelectionRoutes.kt` | **22** |
| 11. Generative Studio Domain - Creative Assets & Multimodal Synthesis | `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/GenerativeStudioRoutes.kt` | **8** |
| 12. Enterprise Domain - Governance, Context Fabric & Chief of Staff | `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt` | **17** |
| 13. Memory Domain - Autonomous Consolidation, Decay & Hybrid Search | `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/MemoryRoutes.kt` | **6** |
| 14. Omnichannel & Sales Domain - Channels, CRM, Catalog & Commerce | `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt` | **28** |
| 15. Billing Domain - Commercial Plans, Credits, Quota & Dunning | `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt` | **30** |
| 16. Admin Domain - Operations, Security, MCP & Financial Command | `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt` | **89** |
| **TOTAL** | | **308** |

---

## 1. Core Domain - System & Webhook Gateways
- **File Sumber**: `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/plugins/Routing.kt`
- **Jumlah Endpoint**: **10**

### 1. `GET` /
- **Grep Confirmation**: Line 58 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/plugins/Routing.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `"OrchestreeAI Enterprise Autonomous AI Workforce Server - Running"`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 2. `GET` /health
- **Grep Confirmation**: Line 62 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/plugins/Routing.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 3. `GET` /api/health
- **Grep Confirmation**: Line 72 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/plugins/Routing.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 4. `GET` /api/v1/platform-assets/icon-logo
- **Grep Confirmation**: Line 93 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/plugins/Routing.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `mapOf("platformIconLogoUrl" to (url ?: ""`
- **Status Engine Terhubung**: Ya — Terkoneksi ke BrandAssetService (Tenant Brand Voice, Logo Storage & Visual Style Injection)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 5. `POST` /api/v1/payments/webhook/{gateway}
- **Grep Confirmation**: Line 107 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/plugins/Routing.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 6. `POST` /api/v1/shipments/webhook/{courier}
- **Grep Confirmation**: Line 138 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/plugins/Routing.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 7. `POST` /api/v1/webhooks/whatsapp
- **Grep Confirmation**: Line 182 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/plugins/Routing.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 8. `POST` /api/v1/webhooks/telegram
- **Grep Confirmation**: Line 183 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/plugins/Routing.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 9. `POST` /api/v1/webhook/whatsapp
- **Grep Confirmation**: Line 187 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/plugins/Routing.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 10. `POST` /api/v1/webhook/telegram
- **Grep Confirmation**: Line 188 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/plugins/Routing.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

## 2. Core Domain - Authentication & Profile Lifecycle
- **File Sumber**: `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AuthRoutes.kt`
- **Jumlah Endpoint**: **6**

### 1. `POST` /auth/login
- **Grep Confirmation**: Line 74 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AuthRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`, `User-Agent: <user-id>`, `X-Forwarded-For: <ip-address>`, `X-Tenant-ID: <tenant-uuid>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `LoginApiRequest`: `val email: String, val password: String? = null, val tenantId: String? = null`
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: `user_sessions` (INSERT/SELECT/UPDATE), `users` (INSERT/SELECT/UPDATE)

### 2. `POST` /auth/refresh
- **Grep Confirmation**: Line 173 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AuthRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`, `X-Tenant-ID: <tenant-uuid>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `RefreshApiRequest`: `val refreshToken: String`
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 3. `GET` /auth/sessions
- **Grep Confirmation**: Line 212 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AuthRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `User-Agent: <user-id>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.Unauthorized: `mapOf("error" to "Token otentikasi dibutuhkan"`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: `user_sessions` (SELECT)

### 4. `POST` /auth/sessions/revoke/{id}
- **Grep Confirmation**: Line 277 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AuthRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 5. `GET` /auth/profile
- **Grep Confirmation**: Line 293 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AuthRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-ID: <tenant-uuid>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.Unauthorized: `mapOf("error" to "Token otentikasi dibutuhkan"`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: `users` (SELECT)

### 6. `POST` /auth/profile
- **Grep Confirmation**: Line 362 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AuthRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `UserProfileDto`: `val userId: String, val tenantId: String, val name: String, val email: String, val phone: String = "", val telegramChatId: String = "", val themePreference: String = "SYSTEM", val languagePreference: String = "id"`
- **Response Body Schema**: HttpStatusCode.OK: `req`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: `users` (INSERT/UPDATE)

## 3. Core Domain - Master Data & Public Catalog
- **File Sumber**: `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/MasterDataRoutes.kt`
- **Jumlah Endpoint**: **9**

### 1. `GET` /api/v1/public/public/department-categories
- **Grep Confirmation**: Line 16 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/MasterDataRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `items`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 2. `POST` /api/v1/public/public/department-categories
- **Grep Confirmation**: Line 22 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/MasterDataRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `DepartmentCategoryRecord`: `val id: String, val category_code: String, val category_name: String, val description: String? = null, val icon_key: String? = null, val is_active: Boolean = true`
- **Response Body Schema**: HttpStatusCode.Created: `mapOf("status" to "CREATED"`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 3. `GET` /api/v1/public/public/industry-catalog
- **Grep Confirmation**: Line 29 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/MasterDataRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `items`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 4. `GET` /api/v1/public/public/job-level-catalog
- **Grep Confirmation**: Line 35 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/MasterDataRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `items`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 5. `GET` /api/v1/public/public/job-sub-title-catalog
- **Grep Confirmation**: Line 41 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/MasterDataRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `items`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 6. `GET` /api/v1/public/tenants/{id}/ai-job-titles
- **Grep Confirmation**: Line 53 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/MasterDataRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `items`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 7. `POST` /api/v1/public/tenants/{id}/ai-job-titles
- **Grep Confirmation**: Line 60 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/MasterDataRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `AiJobTitleRecord`: `val id: String, val job_code: String, val job_name: String, val icon_key: String? = null, val star_rating: Int = 4, val description: String, val maps_to_persona_type: String, val is_top_coordinator: Boolean = false, val relevant_department_category_id: String? = null`
- **Response Body Schema**: HttpStatusCode.Created: `mapOf("status" to "CREATED"`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 8. `GET` /api/v1/public/tenants/{id}/ai-job-titles/{jobTitleId}/structural-roles
- **Grep Confirmation**: Line 67 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/MasterDataRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 9. `GET` /api/v1/public/tenants/{id}/ai-job-titles/{jobTitleId}/available-skills
- **Grep Confirmation**: Line 75 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/MasterDataRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

## 4. Core Domain - Biometric Presence & Liveness
- **File Sumber**: `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/PresenceRoutes.kt`
- **Jumlah Endpoint**: **6**

### 1. `POST` /api/v1/presence/presence/enroll
- **Grep Confirmation**: Line 31 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/PresenceRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `PresenceEnrollRequest`: `val userId: String, val method: String? = null, // "FACE", "FINGERPRINT" val faceEmbedding: List<Float>? = null, val deviceId: String? = null, val isEnabled: Boolean = true`
- **Response Body Schema**: HttpStatusCode.BadRequest: `mapOf("error" to "userId is required"`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 2. `GET` /api/v1/presence/presence/requirement-check
- **Grep Confirmation**: Line 58 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/PresenceRoutes.kt`
- **Header Wajib**: `X-Device-Id: <device-fingerprint>`, `X-Forwarded-For: <ip-address>`, `X-User-Id: <user-id>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.BadRequest: `mapOf("error" to "Query parameter 'userId' or 'X-User-Id' header is required"`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 3. `POST` /api/v1/presence/presence/verify
- **Grep Confirmation**: Line 98 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/PresenceRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`, `X-Device-Id: <device-fingerprint>`
- **Request Body Schema**: `PresenceVerifyRequest`: `val userId: String, val checkType: String = "CHECK_IN", // "CHECK_IN", "CHECK_OUT", "LOGIN" val methodUsed: String, // "FACE", "FINGERPRINT" val biometricSuccess: Boolean? = null, // for fingerprint from BiometricPrompt val faceEmbedding: List<Float>? = null, // on-device generated face embedding val deviceId: String? = null, val ipAddress: String? = null, val locationApprox: String? = null`
- **Response Body Schema**: HttpStatusCode.BadRequest: `mapOf("error" to "userId is required"`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 4. `GET` /api/v1/presence/presence/enrollment
- **Grep Confirmation**: Line 131 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/PresenceRoutes.kt`
- **Header Wajib**: `X-User-Id: <user-id>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.BadRequest: `mapOf("error" to "userId is required"`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 5. `GET` /api/v1/presence/presence/logs
- **Grep Confirmation**: Line 153 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/PresenceRoutes.kt`
- **Header Wajib**: `X-User-Id: <user-id>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.BadRequest: `mapOf("error" to "userId is required"`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 6. `GET` /api/v1/presence/presence/security-audit-stats
- **Grep Confirmation**: Line 172 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/PresenceRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `stats`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

## 5. Core Domain - Attendance & Geofencing
- **File Sumber**: `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AttendanceRoutes.kt`
- **Jumlah Endpoint**: **5**

### 1. `POST` /api/v1/attendance/attendance/check-in
- **Grep Confirmation**: Line 67 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AttendanceRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `AttendanceCheckInRequest`: `val userId: String, val tenantId: String = "tenant-default", val latitude: Double, val longitude: Double, val locationName: String = "Headquarters Jakarta", val type: String = "CHECK_IN"`
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Ya — Terkoneksi ke GeofenceEngine (Polygon & Haversine GPS Radius Verification), AttendanceAnomalyEngine (Facial Liveness, Shift Deviation & Spoof Detection)
- **Tabel Supabase Terpengaruh**: `attendance_records` (INSERT)

### 2. `GET` /api/v1/attendance/attendance/anomalies
- **Grep Confirmation**: Line 133 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AttendanceRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `res.getOrDefault("[]"`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: `attendance_anomalies` (SELECT)

### 3. `GET` /api/v1/attendance/attendance/history
- **Grep Confirmation**: Line 139 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AttendanceRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 4. `GET` /api/v1/attendance/tenants/{id}/geofences
- **Grep Confirmation**: Line 169 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AttendanceRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 5. `POST` /api/v1/attendance/tenants/{id}/geofences
- **Grep Confirmation**: Line 196 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AttendanceRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `GeofenceCreateRequest`: `val name: String, val latitude: Double, val longitude: Double, val radiusMeters: Double = 100.0`
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

## 6. Core Domain - Prospect Registration & Onboarding
- **File Sumber**: `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/prospect/ProspectRoutes.kt`
- **Jumlah Endpoint**: **6**

### 1. `POST` /api/v1/prospect/public/prospect-registration
- **Grep Confirmation**: Line 44 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/prospect/ProspectRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `ProspectRegistrationRequest`: `val fullName: String, val email: String, val phoneNumber: String, val whatsappNumber: String? = null, val address: String? = null, val companyName: String, val jobTitle: String, val industryCategoryId: String? = null, val companySizeRange: String? = null, val interestOption: String, // 'schedule_meeting_presentation' or 'direct_trial_or_subscription' val interestedPlanId: String? = null, val captchaToken: String? = null`
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 2. `GET` /api/v1/prospect/admin/prospect-registrations
- **Grep Confirmation**: Line 119 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/prospect/ProspectRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `list`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 3. `GET` /api/v1/prospect/admin/prospect-registrations/analytics
- **Grep Confirmation**: Line 138 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/prospect/ProspectRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `analytics`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 4. `PATCH` /api/v1/prospect/admin/prospect-registrations/{id}/select-trial
- **Grep Confirmation**: Line 152 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/prospect/ProspectRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 5. `PATCH` /api/v1/prospect/admin/prospect-registrations/{id}/schedule-meeting
- **Grep Confirmation**: Line 169 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/prospect/ProspectRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 6. `POST` /api/v1/prospect/admin/prospect-registrations/{id}/activate-trial
- **Grep Confirmation**: Line 186 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/prospect/ProspectRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

## 7. Core Domain - Tenants, Hybrid Workforce, Taskboard & Intel
- **File Sumber**: `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Jumlah Endpoint**: **46**

### 1. `GET` /api/v1/tenants/{id}/dashboard/overview
- **Grep Confirmation**: Line 311 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 2. `GET` /api/v1/tenants/{id}/departments
- **Grep Confirmation**: Line 327 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `result.getOrDefault("[]"`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: `departments` (SELECT)

### 3. `POST` /api/v1/tenants/{id}/departments
- **Grep Confirmation**: Line 333 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `DepartmentCreateRequest`: `val name: String, val description: String = "", val managerUserId: String? = null, val colorTag: String = "#1E6FE0"`
- **Response Body Schema**: HttpStatusCode.Created: `GenericStatusResponse(status = "created"`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: `departments` (INSERT)

### 4. `DELETE` /api/v1/tenants/{id}/departments/{deptId}
- **Grep Confirmation**: Line 351 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 5. `GET` /api/v1/tenants/{id}/staff
- **Grep Confirmation**: Line 359 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `result.getOrDefault("[]"`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 6. `DELETE` /api/v1/tenants/{id}/staff/{staffId}
- **Grep Confirmation**: Line 365 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 7. `POST` /api/v1/tenants/{id}/staff
- **Grep Confirmation**: Line 372 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `StaffCreateRequest`: `val name: String, val email: String, val role: String, val departmentId: String`
- **Response Body Schema**: HttpStatusCode.Created: `GenericStatusResponse(status = "created"`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: `users` (INSERT)

### 8. `GET` /api/v1/tenants/{id}/agents
- **Grep Confirmation**: Line 401 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `result.getOrDefault("[]"`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: `ai_agents` (SELECT)

### 9. `POST` /api/v1/tenants/{id}/agents
- **Grep Confirmation**: Line 407 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `AgentCreateRequest`: `val name: String, val jobTitleId: String, val departmentId: String, val structuralRoleId: String? = null`
- **Response Body Schema**: HttpStatusCode.Created: `GenericStatusResponse(status = "created"`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: `ai_agents` (INSERT)

### 10. `GET` /api/v1/tenants/{id}/tasks
- **Grep Confirmation**: Line 432 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`, `X-User-Id: <user-id>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.Unauthorized: `mapOf("error" to "User identity is required to fetch assigned tasks"`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: `tasks` (SELECT)

### 11. `POST` /api/v1/tenants/{id}/tasks
- **Grep Confirmation**: Line 448 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `Map<String, String`
- **Response Body Schema**: HttpStatusCode.Created: `GenericStatusResponse(status = "created"`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: `tasks` (INSERT)

### 12. `GET` /api/v1/tenants/{id}/boards/{boardId}
- **Grep Confirmation**: Line 471 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 13. `GET` /api/v1/tasks
- **Grep Confirmation**: Line 487 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`, `X-User-Id: <user-id>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.Unauthorized: `mapOf("error" to "User identity is required to fetch assigned tasks"`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: `tasks` (SELECT)

### 14. `POST` /api/v1/tasks/inbound-channel-message
- **Grep Confirmation**: Line 503 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `InboundChannelMessageApiRequest`: `val channel: String, // 'telegram', 'whatsapp', 'dashboard' val senderId: String, val senderName: String? = null, val message: String, val tenantId: String = "tenant-default"`
- **Response Body Schema**: HttpStatusCode.OK: `result`
- **Status Engine Terhubung**: Ya — Terkoneksi ke OrchestrationEngine (Workflow DAG & Autonomous Execution), ModelRouter (Multi-LLM Routing, Latency-Cost Optimization & Fallbacks), ChannelGateway (Unified Omnichannel Inbound/Outbound Message Routing & Event Gateway)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 15. `GET` /api/v1/tasks/proactive/subscriptions
- **Grep Confirmation**: Line 521 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 16. `GET` /api/v1/tasks/proactive/scope/{staffId}
- **Grep Confirmation**: Line 539 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 17. `POST` /api/v1/tasks/proactive/subscriptions
- **Grep Confirmation**: Line 548 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `ProactiveSubscriptionRequest`: `val staffId: String, val channel: String, val types: List<String>, val sendTimes: List<String>`
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 18. `POST` /api/v1/tenants/tasks/inbound-channel-message
- **Grep Confirmation**: Line 561 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `InboundChannelMessageApiRequest`: `val channel: String, // 'telegram', 'whatsapp', 'dashboard' val senderId: String, val senderName: String? = null, val message: String, val tenantId: String = "tenant-default"`
- **Response Body Schema**: HttpStatusCode.OK: `result`
- **Status Engine Terhubung**: Ya — Terkoneksi ke OrchestrationEngine (Workflow DAG & Autonomous Execution), ModelRouter (Multi-LLM Routing, Latency-Cost Optimization & Fallbacks), ChannelGateway (Unified Omnichannel Inbound/Outbound Message Routing & Event Gateway)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 19. `PATCH` /api/v1/tasks/{taskId}/move
- **Grep Confirmation**: Line 580 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `TaskMoveRequest`: `val toColumn: String = "", val targetColumn: String = "", val version: Int = 1, val expectedVersion: Int = 1, val targetIndex: Int = 0`
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: `tasks` (UPDATE)

### 20. `GET` /api/v1/tasks/{taskId}/checklists
- **Grep Confirmation**: Line 596 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `items`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 21. `POST` /api/v1/tasks/{taskId}/checklists
- **Grep Confirmation**: Line 602 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `ChecklistItemCreateRequest`: `val itemText: String, val orderIndex: Int = 0`
- **Response Body Schema**: HttpStatusCode.Created: `created`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 22. `PATCH` /api/v1/tasks/{taskId}/checklists/{checklistId}/toggle
- **Grep Confirmation**: Line 616 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 23. `GET` /api/v1/tasks/{taskId}/activity-log
- **Grep Confirmation**: Line 624 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `logs`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 24. `PATCH` /api/v1/tasks/{taskId}/description
- **Grep Confirmation**: Line 630 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `TaskDescriptionUpdateRequest`: `val descriptionRichText: String`
- **Response Body Schema**: HttpStatusCode.OK: `result.getOrThrow(`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 25. `GET` /api/v1/tasks/{taskId}/attachments
- **Grep Confirmation**: Line 641 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `items`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 26. `POST` /api/v1/tasks/{taskId}/attachments
- **Grep Confirmation**: Line 647 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `AttachmentCreateRequest`: `val fileName: String, val fileUrl: String, val fileSizeBytes: Long = 0L, val uploadedBy: String = "staff"`
- **Response Body Schema**: HttpStatusCode.Created: `created`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 27. `GET` /api/v1/intel/competitors
- **Grep Confirmation**: Line 666 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `list`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 28. `POST` /api/v1/intel/competitors
- **Grep Confirmation**: Line 706 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `CompetitorTargetRequest`: `val name: String, val category: String, val urls: List<String>, val frequency: String = "daily", val assignedAgentId: String? = null`
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Ya — Terkoneksi ke OrchestrationEngine (Workflow DAG & Autonomous Execution), ModelRouter (Multi-LLM Routing, Latency-Cost Optimization & Fallbacks), ContinuousLearningCore (Reinforcement Feedback Loop, Node Outcomes & Skill Evolution), CompetitorIntelligenceEngine (Autonomous Competitor Crawling & Market Radar)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 29. `GET` /api/v1/intel/competitors/{id}/insights
- **Grep Confirmation**: Line 797 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 30. `GET` /api/v1/intel/world-trends
- **Grep Confirmation**: Line 875 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 31. `POST` /api/v1/integrations/{platform}/connect
- **Grep Confirmation**: Line 900 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `IntegrationConnectRequest`: `val authCode: String? = null, val redirectUri: String? = null, val scopes: List<String> = emptyList(`
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 32. `GET` /api/v1/proactive/subscriptions
- **Grep Confirmation**: Line 912 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 33. `GET` /api/v1/proactive/scope/{staffId}
- **Grep Confirmation**: Line 930 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 34. `POST` /api/v1/proactive/subscriptions
- **Grep Confirmation**: Line 939 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `ProactiveSubscriptionRequest`: `val staffId: String, val channel: String, val types: List<String>, val sendTimes: List<String>`
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 35. `GET` /api/v1/analytics/scores
- **Grep Confirmation**: Line 952 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 36. `GET` /api/v1/performance/reports
- **Grep Confirmation**: Line 973 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 37. `POST` /api/v1/performance/reports
- **Grep Confirmation**: Line 992 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `CreateWorkReportRequest`: `val staffId: String, val staffName: String, val reportDate: String, val accomplishments: String, val blockers: String = "", val plannedNext: String = ""`
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 38. `GET` /api/v1/performance/goals
- **Grep Confirmation**: Line 1010 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 39. `GET` /api/v1/performance/reviews
- **Grep Confirmation**: Line 1039 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 40. `GET` /api/v1/performance/predictions
- **Grep Confirmation**: Line 1058 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 41. `GET` /api/v1/performance/executive-briefs
- **Grep Confirmation**: Line 1077 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 42. `GET` /api/v1/security/anomalies
- **Grep Confirmation**: Line 1098 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 43. `GET` /api/v1/security/dsr
- **Grep Confirmation**: Line 1115 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 44. `POST` /api/v1/security/dsr
- **Grep Confirmation**: Line 1131 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `CreateDataSubjectRequest`: `val requestType: String, val requesterEmail: String, val details: String = ""`
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 45. `GET` /api/v1/attendance/anomalies
- **Grep Confirmation**: Line 1149 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 46. `POST` /api/v1/attendance/anomalies/{anomalyId}/resolve
- **Grep Confirmation**: Line 1168 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

## 8. Orchestration Domain - Workflow DAG, Checkpoints & MCP Execution
- **File Sumber**: `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OrchestrationRoutes.kt`
- **Jumlah Endpoint**: **14**

### 1. `POST` /api/v1/orchestration/orchestration/dispatch
- **Grep Confirmation**: Line 59 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OrchestrationRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `WorkflowDispatchRequest`: `val workflowDefId: String, val prompt: String, val tenantId: String = "tenant-default", val contextParams: Map<String, String> = emptyMap(`
- **Response Body Schema**: HttpStatusCode.OK: `result`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 2. `POST` /api/v1/orchestration/orchestration/workflows/run
- **Grep Confirmation**: Line 71 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OrchestrationRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `WorkflowDispatchRequest`: `val workflowDefId: String, val prompt: String, val tenantId: String = "tenant-default", val contextParams: Map<String, String> = emptyMap(`
- **Response Body Schema**: HttpStatusCode.OK: `result`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 3. `GET` /api/v1/orchestration/orchestration/status/{executionId}
- **Grep Confirmation**: Line 83 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OrchestrationRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 4. `GET` /api/v1/orchestration/orchestration/executions/{executionId}
- **Grep Confirmation**: Line 96 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OrchestrationRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 5. `GET` /api/v1/orchestration/orchestration/approvals
- **Grep Confirmation**: Line 109 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OrchestrationRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `pendingList`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 6. `POST` /api/v1/orchestration/orchestration/approvals/{id}/decision
- **Grep Confirmation**: Line 115 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OrchestrationRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 7. `GET` /api/v1/orchestration/orchestration/traces
- **Grep Confirmation**: Line 143 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OrchestrationRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `traces`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 8. `GET` /api/v1/orchestration/orchestration/traces/{executionId}
- **Grep Confirmation**: Line 148 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OrchestrationRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 9. `GET` /api/v1/orchestration/orchestration/traces/by-trace/{traceId}
- **Grep Confirmation**: Line 164 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OrchestrationRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 10. `GET` /api/v1/orchestration/intelligence/confidence/calibration
- **Grep Confirmation**: Line 177 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OrchestrationRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `calibrations`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 11. `POST` /api/v1/orchestration/intelligence/confidence/calibrate
- **Grep Confirmation**: Line 186 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OrchestrationRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `mapOf(`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 12. `GET` /api/v1/orchestration/intelligence/confidence/audit
- **Grep Confirmation**: Line 197 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OrchestrationRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `report`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 13. `GET` /api/v1/orchestration/mcp/tools
- **Grep Confirmation**: Line 206 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OrchestrationRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `tools`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 14. `POST` /api/v1/orchestration/mcp/execute
- **Grep Confirmation**: Line 211 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OrchestrationRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `McpExecuteApiRequest`: `val toolName: String, val params: Map<String, String>, val tenantId: String = "tenant-default", val callerRole: String = "STAFF_HUMAN"`
- **Response Body Schema**: HttpStatusCode.OK: `result`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

## 9. Chat Domain - Agent Direct Conversation & Company Brain Knowledge
- **File Sumber**: `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/ChatRoutes.kt`
- **Jumlah Endpoint**: **6**

### 1. `POST` /api/v1/chat
- **Grep Confirmation**: Line 107 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/ChatRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `ChatApiRequest`: `val message: String = "", val prompt: String = "", val tenantId: String = "tenant-default", val conversationId: String? = null, val agentId: String? = null, val systemPrompt: String? = null`
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 2. `POST` /api/v1/chat/messages
- **Grep Confirmation**: Line 112 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/ChatRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `ChatApiRequest`: `val message: String = "", val prompt: String = "", val tenantId: String = "tenant-default", val conversationId: String? = null, val agentId: String? = null, val systemPrompt: String? = null`
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 3. `GET` /api/v1/chat/history/{conversationId}
- **Grep Confirmation**: Line 118 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/ChatRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 4. `POST` /api/v1/agents/{agentId}/chat
- **Grep Confirmation**: Line 160 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/ChatRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 5. `POST` /api/v1/company-brain/documents
- **Grep Confirmation**: Line 231 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/ChatRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `DocumentUploadRequest`: `val title: String, val content: String, val tenantId: String`
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 6. `GET` /api/v1/company-brain/search
- **Grep Confirmation**: Line 252 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/ChatRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `results`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

## 10. Selection Domain - Universal Selection, Understanding & Calibration
- **File Sumber**: `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/SelectionRoutes.kt`
- **Jumlah Endpoint**: **22**

### 1. `POST` /api/v1/selection/selection/upload
- **Grep Confirmation**: Line 211 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/SelectionRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`, `X-User-Id: <user-id>`
- **Request Body Schema**: `String (text/raw body)`
- **Response Body Schema**: HttpStatusCode.BadRequest: `mapOf("error" to "No file uploaded or file is empty"`
- **Status Engine Terhubung**: Ya — Terkoneksi ke SelectionEngine (Universal Data Selection, Matching & Document Understanding)
- **Tabel Supabase Terpengaruh**: `selection_requests` (SELECT)

### 2. `POST` /api/v1/selection/selection/prompt-only
- **Grep Confirmation**: Line 342 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/SelectionRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`, `X-User-Id: <user-id>`
- **Request Body Schema**: `PromptOnlySelectionRequest`: `val prompt: String, val calibrationSettingsId: String? = null`
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 3. `POST` /api/v1/selection/selection/api-database
- **Grep Confirmation**: Line 380 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/SelectionRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`, `X-User-Id: <user-id>`
- **Request Body Schema**: `ApiDatabaseSelectionRequest`: `val prompt: String, val tableName: String? = null, val items: List<Map<String, String>>? = null`
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 4. `GET` /api/v1/selection/selection/requests
- **Grep Confirmation**: Line 422 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/SelectionRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `list`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 5. `GET` /api/v1/selection/selection/{id}
- **Grep Confirmation**: Line 432 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/SelectionRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 6. `GET` /api/v1/selection/selection/{id}/results
- **Grep Confirmation**: Line 458 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/SelectionRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 7. `GET` /api/v1/selection/selection/{id}/analytics
- **Grep Confirmation**: Line 469 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/SelectionRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 8. `POST` /api/v1/selection/selection/documents/{documentId}/understand
- **Grep Confirmation**: Line 480 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/SelectionRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 9. `GET` /api/v1/selection/selection/documents/{documentId}/understanding
- **Grep Confirmation**: Line 500 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/SelectionRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 10. `POST` /api/v1/selection/selection/calibration
- **Grep Confirmation**: Line 520 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/SelectionRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`, `X-User-Id: <user-id>`
- **Request Body Schema**: `CalibrationRequest`: `val calibration_name: String, val items: List<CalibrationItemRequest>, val is_saved_as_preset: Boolean = false`
- **Response Body Schema**: HttpStatusCode.Unauthorized: `mapOf("error" to "Role claim missing from authenticated token"`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 11. `GET` /api/v1/selection/selection/calibration
- **Grep Confirmation**: Line 555 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/SelectionRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `list`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 12. `GET` /api/v1/selection/selection/calibration/{id}
- **Grep Confirmation**: Line 565 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/SelectionRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 13. `POST` /api/v1/selection/selection/calibration/{id}/validate-dataset/{documentId}
- **Grep Confirmation**: Line 584 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/SelectionRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 14. `POST` /api/v1/selection/selection/results/{id}/review
- **Grep Confirmation**: Line 621 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/SelectionRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 15. `POST` /api/v1/selection/selection/results/{id}/execute-downstream
- **Grep Confirmation**: Line 653 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/SelectionRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 16. `GET` /api/v1/selection/selection/{id}/export
- **Grep Confirmation**: Line 719 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/SelectionRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 17. `POST` /api/v1/selection/selection/{id}/export
- **Grep Confirmation**: Line 720 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/SelectionRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 18. `GET` /api/v1/selection/selection/auto-selection/configs
- **Grep Confirmation**: Line 730 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/SelectionRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `configs`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 19. `POST` /api/v1/selection/selection/auto-selection/configs
- **Grep Confirmation**: Line 737 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/SelectionRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `AutoSelectionConfigCreateRequest`: `val folder_path: String, val domain_category: String? = null, val default_prompt: String = "Lakukan evaluasi dan ranking otomatis untuk dokumen yang diunggah", val is_enabled: Boolean = true, val calibration_settings_id: String? = null, val auto_execute_downstream: Boolean = false`
- **Response Body Schema**: HttpStatusCode.Created: `saved`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 20. `DELETE` /api/v1/selection/selection/auto-selection/configs/{id}
- **Grep Confirmation**: Line 755 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/SelectionRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 21. `POST` /api/v1/selection/selection/webhook/storage
- **Grep Confirmation**: Line 773 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/SelectionRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `StorageUploadWebhookPayload`: `val type: String? = null, val table: String? = null, val schema: String? = null, val record: StorageObjectRecord? = null, val tenant_id: String? = null, val bucket: String? = null, val path: String? = null, val file_name: String? = null, val file_type: String? = null, val storage_url: String? = null, val file_content_base64: String? = null`
- **Response Body Schema**: HttpStatusCode.BadRequest: `mapOf("error" to "Invalid storage webhook payload: ${e.message}"`
- **Status Engine Terhubung**: Ya — Terkoneksi ke SelectionEngine (Universal Data Selection, Matching & Document Understanding)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 22. `POST` /api/v1/selection/selection/webhook/integration-fabric
- **Grep Confirmation**: Line 937 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/SelectionRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-ID: <tenant-uuid>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `IntegrationFabricWebhookPayload`: `val tenant_id: String? = null, val source_system: String = "ERP", val domain: String? = null, val prompt: String = "Evaluasi dan ranking otomatis dari Integration Fabric", val table_name: String? = null, val items: List<Map<String, String>> = emptyList(`
- **Response Body Schema**: HttpStatusCode.BadRequest: `mapOf("error" to "Invalid integration fabric payload: ${e.message}"`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

## 11. Generative Studio Domain - Creative Assets & Multimodal Synthesis
- **File Sumber**: `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/GenerativeStudioRoutes.kt`
- **Jumlah Endpoint**: **8**

### 1. `POST` /api/v1/studio/studio/generate-image
- **Grep Confirmation**: Line 73 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/GenerativeStudioRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `GenerateImageRequest`: `val prompt: String? = null, val productName: String? = null, val targetAudience: String? = null, val visualTheme: String? = null, val aspectRatio: String? = "1:1", val tenantId: String? = null`
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Ya — Terkoneksi ke OrchestrationEngine (Workflow DAG & Autonomous Execution), ModelRouter (Multi-LLM Routing, Latency-Cost Optimization & Fallbacks)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 2. `POST` /api/v1/studio/studio/compose-prompt
- **Grep Confirmation**: Line 140 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/GenerativeStudioRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `ComposePromptRequest`: `val productName: String, val targetAudience: String, val visualTheme: String, val aspectRatio: String = "1:1"`
- **Response Body Schema**: HttpStatusCode.OK: `composed`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 3. `POST` /api/v1/studio/studio/campaign-creative
- **Grep Confirmation**: Line 152 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/GenerativeStudioRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `CampaignCreativeRequest`: `val topic: String, val platform: String = "INSTAGRAM", val tenantId: String? = null`
- **Response Body Schema**: HttpStatusCode.OK: `plan`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 4. `GET` /api/v1/studio/studio/templates
- **Grep Confirmation**: Line 160 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/GenerativeStudioRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `emptyList<String>(`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 5. `POST` /api/v1/studio/studio/brand-assets/upload-logo
- **Grep Confirmation**: Line 165 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/GenerativeStudioRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `BrandLogoUploadRequest`: `val fileName: String, val fileBase64: String, val tenantId: String? = null`
- **Response Body Schema**: HttpStatusCode.Created: `result`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 6. `POST` /api/v1/studio/studio/brand-assets/logo
- **Grep Confirmation**: Line 181 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/GenerativeStudioRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `BrandLogoUploadRequest`: `val fileName: String, val fileBase64: String, val tenantId: String? = null`
- **Response Body Schema**: HttpStatusCode.Created: `result`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 7. `GET` /api/v1/studio/studio/assets
- **Grep Confirmation**: Line 197 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/GenerativeStudioRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `items`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 8. `POST` /api/v1/studio/studio/assets
- **Grep Confirmation**: Line 214 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/GenerativeStudioRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `BrandAssetCreateRequest`: `val title: String, val assetType: String, val fileUrl: String, val isLocked: Boolean = false`
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

## 12. Enterprise Domain - Governance, Context Fabric & Chief of Staff
- **File Sumber**: `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Jumlah Endpoint**: **17**

### 1. `GET` /api/v1/tenants/{id}/tenants/{id}/enterprise-connections
- **Grep Confirmation**: Line 157 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `GenericStatusResponse(status = "success"`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 2. `POST` /api/v1/tenants/{id}/tenants/{id}/enterprise-connections
- **Grep Confirmation**: Line 163 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `EnterpriseConnectionCreateRequest`: `val systemType: String, val connectionEndpoint: String, val authType: String = "BEARER_TOKEN", val syncScheduleCron: String = "0 * * * *"`
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 3. `GET` /api/v1/tenants/{id}/tenants/{id}/ai-data-permissions
- **Grep Confirmation**: Line 177 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `GenericStatusResponse(status = "success"`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: `ai_data_permission_policies` (SELECT)

### 4. `POST` /api/v1/tenants/{id}/tenants/{id}/ai-data-permissions
- **Grep Confirmation**: Line 183 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `AiDataPermissionPolicyRequest`: `val agentPersonaType: String, val domainScope: String, val accessLevel: String = "READ_ONLY", val conditionsJson: String = "{}"`
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 5. `GET` /api/v1/tenants/{id}/tenants/{id}/activity-stream
- **Grep Confirmation**: Line 197 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `streamItems`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 6. `GET` /api/v1/tenants/{id}/tenants/{id}/context-fabric/{entityId}
- **Grep Confirmation**: Line 273 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 7. `POST` /api/v1/tenants/{id}/tenants/{id}/management-query
- **Grep Confirmation**: Line 327 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `ManagementQueryRequest`: `val question: String, val entityFocus: String? = null`
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Ya — Terkoneksi ke OrchestrationEngine (Workflow DAG & Autonomous Execution)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 8. `POST` /api/v1/tenants/{id}/tenants/{id}/correlate-signals
- **Grep Confirmation**: Line 375 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `Map<String, String`
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 9. `GET` /api/v1/tenants/{id}/tenants/{id}/reports/daily
- **Grep Confirmation**: Line 398 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 10. `GET` /api/v1/tenants/{id}/tenants/{id}/knowledge-rules
- **Grep Confirmation**: Line 411 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `GenericStatusResponse(status = "success"`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: `knowledge_rules` (SELECT)

### 11. `POST` /api/v1/tenants/{id}/tenants/{id}/knowledge-rules
- **Grep Confirmation**: Line 417 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `KnowledgeRuleCreateRequest`: `val entityType: String, val sopReference: String, val structuredRuleJson: String, val naturalLanguageRule: String`
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: `knowledge_rules` (INSERT)

### 12. `GET` /api/v1/tenants/{id}/tenants/{id}/events
- **Grep Confirmation**: Line 442 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `events`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 13. `GET` /api/v1/tenants/{id}/tenants/{id}/chief-of-staff/briefings
- **Grep Confirmation**: Line 492 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `result.getOrDefault("[]"`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: `chief_of_staff_briefings` (SELECT)

### 14. `POST` /api/v1/tenants/{id}/tenants/{id}/chief-of-staff/synthesize
- **Grep Confirmation**: Line 513 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `briefing`
- **Status Engine Terhubung**: Ya — Terkoneksi ke OrchestrationEngine (Workflow DAG & Autonomous Execution)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 15. `POST` /api/v1/tenants/{id}/tenants/{id}/chief-of-staff/research-directives
- **Grep Confirmation**: Line 526 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `ResearchDirectiveRequest`: `val topic: String, val parametersJson: String = "{}"`
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: `chief_of_staff_briefings` (INSERT)

### 16. `GET` /api/v1/tenants/{id}/tenants/{id}/agents/{agentId}/skill-confidence
- **Grep Confirmation**: Line 567 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 17. `GET` /api/v1/tenants/{id}/tenants/{id}/data-quality-issues
- **Grep Confirmation**: Line 584 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `issues`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

## 13. Memory Domain - Autonomous Consolidation, Decay & Hybrid Search
- **File Sumber**: `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/MemoryRoutes.kt`
- **Jumlah Endpoint**: **6**

### 1. `POST` /api/v1/memory/memory/consolidate/evaluate
- **Grep Confirmation**: Line 38 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/MemoryRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `CandidateInteraction`: `val id: String = UUID.randomUUID(`
- **Response Body Schema**: HttpStatusCode.OK: `decision`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 2. `POST` /api/v1/memory/memory/consolidate/batch
- **Grep Confirmation**: Line 44 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/MemoryRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `List<CandidateInteraction`: `val id: String = UUID.randomUUID(`
- **Response Body Schema**: HttpStatusCode.OK: `decisions`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 3. `POST` /api/v1/memory/memory/decay
- **Grep Confirmation**: Line 51 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/MemoryRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `DecayMemoryRequest`: `val tenantId: String? = null`
- **Response Body Schema**: HttpStatusCode.OK: `summary`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 4. `POST` /api/v1/memory/memory/search
- **Grep Confirmation**: Line 58 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/MemoryRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `SearchMemoryRequest`: `val tenantId: String, val query: String, val enableReranking: Boolean = true`
- **Response Body Schema**: HttpStatusCode.OK: `result`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 5. `POST` /api/v1/memory/memory/search/ab-compare
- **Grep Confirmation**: Line 64 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/MemoryRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `SearchMemoryRequest`: `val tenantId: String, val query: String, val enableReranking: Boolean = true`
- **Response Body Schema**: HttpStatusCode.OK: `comparison`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 6. `GET` /api/v1/memory/memory/documents
- **Grep Confirmation**: Line 71 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/MemoryRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `docs`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

## 14. Omnichannel & Sales Domain - Channels, CRM, Catalog & Commerce
- **File Sumber**: `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Jumlah Endpoint**: **28**

### 1. `GET` /api/v1/tenants/{id}/channel-accounts
- **Grep Confirmation**: Line 256 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `accounts`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: `channel_accounts` (SELECT)

### 2. `POST` /api/v1/tenants/{id}/channel-accounts
- **Grep Confirmation**: Line 298 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `ChannelAccountCreateRequest`: `val channelType: String, val accountLabel: String, val externalIdentifier: String, val departmentId: String? = null`
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: `channel_accounts` (INSERT)

### 3. `POST` /api/v1/tenants/{id}/channel-accounts/{caId}/verify
- **Grep Confirmation**: Line 339 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 4. `PATCH` /api/v1/tenants/{id}/channel-accounts/{caId}/approve
- **Grep Confirmation**: Line 357 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 5. `GET` /api/v1/tenants/{id}/channel-accounts/{caId}/health
- **Grep Confirmation**: Line 375 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 6. `GET` /api/v1/tenants/{id}/credit-wallet
- **Grep Confirmation**: Line 410 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: `ai_credit_wallets` (INSERT/SELECT)

### 7. `GET` /api/v1/tenants/{id}/customers/search
- **Grep Confirmation**: Line 462 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `results`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: `customers` (SELECT)

### 8. `POST` /api/v1/tenants/{id}/customers/resolve
- **Grep Confirmation**: Line 515 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `Map<String, String`
- **Response Body Schema**: HttpStatusCode.OK: `profile`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 9. `POST` /api/v1/tenants/{id}/customers/{cust_id}/merge
- **Grep Confirmation**: Line 529 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 10. `GET` /api/v1/tenants/{id}/inbox/conversations
- **Grep Confirmation**: Line 545 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `list`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: `customers` (SELECT)

### 11. `GET` /api/v1/tenants/{id}/leads
- **Grep Confirmation**: Line 608 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `list`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: `leads` (SELECT)

### 12. `POST` /api/v1/tenants/{id}/leads
- **Grep Confirmation**: Line 647 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `LeadCreateRequest`: `val customerName: String, val contactIdentifier: String? = null, val sourceChannel: String = "WHATSAPP", val status: String = "NEW", val budget: Double = 0.0, val notes: String = ""`
- **Response Body Schema**: HttpStatusCode.Created: `GenericStatusResponse(status = "CREATED"`
- **Status Engine Terhubung**: Ya — Terkoneksi ke LeadQualificationEngine (Real-time BANT Scoring & Prospect Segmentation)
- **Tabel Supabase Terpengaruh**: `leads` (INSERT)

### 13. `GET` /api/v1/tenants/{id}/products
- **Grep Confirmation**: Line 686 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `list`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: `products` (SELECT)

### 14. `POST` /api/v1/tenants/{id}/products
- **Grep Confirmation**: Line 723 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `ProductCreateRequest`: `val sku: String, val name: String, val description: String = "", val category: String, val basePrice: Double, val currency: String = "IDR"`
- **Response Body Schema**: HttpStatusCode.Created: `GenericStatusResponse(status = "CREATED"`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: `products` (INSERT)

### 15. `GET` /api/v1/tenants/{id}/inventory
- **Grep Confirmation**: Line 754 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `list`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 16. `GET` /api/v1/tenants/{id}/inventory/{variantId}
- **Grep Confirmation**: Line 788 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 17. `GET` /api/v1/tenants/{id}/orders
- **Grep Confirmation**: Line 825 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `list`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: `orders` (SELECT)

### 18. `GET` /api/v1/tenants/{id}/campaigns
- **Grep Confirmation**: Line 867 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `list`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: `campaigns` (SELECT)

### 19. `POST` /api/v1/tenants/{id}/campaigns
- **Grep Confirmation**: Line 908 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `CampaignCreateRequest`: `val name: String, val instruction: String, val targetChannels: List<String> = listOf("WHATSAPP", "INSTAGRAM"`
- **Response Body Schema**: HttpStatusCode.Created: `GenericStatusResponse(status = "SCHEDULED"`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: `campaigns` (INSERT)

### 20. `GET` /api/v1/tenants/{id}/analytics/revenue-intelligence
- **Grep Confirmation**: Line 937 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: `orders` (SELECT)

### 21. `GET` /api/v1/tenants/{id}/analytics/sales-coach
- **Grep Confirmation**: Line 991 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 22. `POST` /api/v1/tenants/{id}/experiments
- **Grep Confirmation**: Line 1006 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `ExperimentCreateRequest`: `val experimentName: String, val variantAContent: String, val variantBContent: String`
- **Response Body Schema**: HttpStatusCode.Created: `GenericStatusResponse(status = "RUNNING"`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 23. `GET` /api/v1/tenants/{id}/service-requests
- **Grep Confirmation**: Line 1061 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `list`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: `customers` (SELECT), `service_requests` (SELECT)

### 24. `POST` /api/v1/tenants/{id}/service-requests
- **Grep Confirmation**: Line 1097 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `ServiceRequestCreateRequest`: `val customerId: String? = null, val customerName: String, val requestType: String = "INQUIRY", val priority: String = "MEDIUM", val subject: String, val description: String`
- **Response Body Schema**: HttpStatusCode.Created: `GenericStatusResponse(status = "OPEN"`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: `service_requests` (INSERT)

### 25. `PATCH` /api/v1/tenants/{id}/ai-agents/{agentId}/persona
- **Grep Confirmation**: Line 1131 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 26. `POST` /api/v1/tenants/{id}/conversations/{convId}/persona-reply
- **Grep Confirmation**: Line 1163 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 27. `POST` /api/v1/conversations/{id}/takeover
- **Grep Confirmation**: Line 1310 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: `staff` (INSERT/UPDATE)

### 28. `POST` /api/v1/carts/{id}/checkout
- **Grep Confirmation**: Line 1352 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `CheckoutRequest`: `val paymentMethod: String, val shippingAddress: String, val courier: String, val customerName: String? = null, val customerPhone: String? = null`
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: `cart_items` (INSERT/SELECT/UPDATE), `carts` (INSERT/SELECT/UPDATE), `orders` (INSERT/SELECT/UPDATE)

## 15. Billing Domain - Commercial Plans, Credits, Quota & Dunning
- **File Sumber**: `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Jumlah Endpoint**: **30**

### 1. `GET` /api/v1/plans
- **Grep Confirmation**: Line 73 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `plans`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 2. `GET` /api/v1/plans/{id}
- **Grep Confirmation**: Line 83 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 3. `GET` /api/v1/plans/entitlements-matrix
- **Grep Confirmation**: Line 94 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `matrix`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 4. `GET` /api/v1/tenant/entitlements
- **Grep Confirmation**: Line 109 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `entitlements`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 5. `GET` /api/v1/billing/plans
- **Grep Confirmation**: Line 126 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `plans`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 6. `GET` /api/v1/billing/plans/{id}
- **Grep Confirmation**: Line 135 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 7. `POST` /api/v1/billing/calculate-cost
- **Grep Confirmation**: Line 146 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `CreditCostContext`: `val activityType: String, val complexityLevel: String = "simple", // 'simple'/'medium'/'complex' val modelUsed: String = "standard", // 'openrouter'/'groq'/'deepseek'/'claude'/'kimi'/'standard' val toolsInvoked: Int = 0, val executionType: String = "single_step" // 'single_step'/'multi_step'/'autonomous'`
- **Response Body Schema**: HttpStatusCode.OK: `result`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 8. `GET` /api/v1/billing/subscription
- **Grep Confirmation**: Line 162 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `SubscriptionPlanResponse(status = "none"`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 9. `POST` /api/v1/billing/subscription
- **Grep Confirmation**: Line 187 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `SubscriptionStartRequest`: `val planId: String, val billingInterval: String = "monthly"`
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 10. `POST` /api/v1/billing/subscription/upgrade
- **Grep Confirmation**: Line 209 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `SubscriptionUpgradeRequest`: `val targetPlanId: String`
- **Response Body Schema**: HttpStatusCode.OK: `result`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 11. `POST` /api/v1/billing/subscription/downgrade
- **Grep Confirmation**: Line 222 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `SubscriptionDowngradeRequest`: `val targetPlanId: String`
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 12. `POST` /api/v1/billing/subscription/cancel
- **Grep Confirmation**: Line 257 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `CancelSubscriptionResponse(status = "cancelled"`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 13. `GET` /api/v1/billing/credits
- **Grep Confirmation**: Line 273 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 14. `GET` /api/v1/billing/credits/summary
- **Grep Confirmation**: Line 298 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `summary`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 15. `GET` /api/v1/billing/credits/ledger
- **Grep Confirmation**: Line 335 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 16. `GET` /api/v1/billing/ledger
- **Grep Confirmation**: Line 336 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 17. `POST` /api/v1/billing/credits/topup
- **Grep Confirmation**: Line 339 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `CreditTopupRequest`: `val amount: Double, val amountPaid: Double = 0.0, val currency: String = "IDR", val reference: String? = null`
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 18. `GET` /api/v1/billing/seats
- **Grep Confirmation**: Line 371 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 19. `POST` /api/v1/billing/seats
- **Grep Confirmation**: Line 393 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `SeatCreateRequest`: `val name: String, val email: String, val role: String = "STAFF_HUMAN", val departmentId: String = "general"`
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: `staff` (SELECT)

### 20. `DELETE` /api/v1/billing/seats/{id}
- **Grep Confirmation**: Line 431 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 21. `GET` /api/v1/billing/agents
- **Grep Confirmation**: Line 453 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 22. `POST` /api/v1/billing/agents
- **Grep Confirmation**: Line 475 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `BillingAgentCreateRequest`: `val name: String, val role: String = "Autonomous AI Specialist", val personaCode: String = "AGENT", val departmentId: String? = null`
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 23. `DELETE` /api/v1/billing/agents/{id}
- **Grep Confirmation**: Line 513 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 24. `GET` /api/v1/billing/invoices
- **Grep Confirmation**: Line 534 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `invoices`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: `invoices` (SELECT)

### 25. `GET` /api/v1/billing/invoices/{id}
- **Grep Confirmation**: Line 545 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 26. `POST` /api/v1/billing/invoices/{id}/fail-dunning
- **Grep Confirmation**: Line 561 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 27. `POST` /api/v1/billing/payment
- **Grep Confirmation**: Line 580 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `Content-Type: application/json`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: `PaymentCreateRequest`: `val planId: String? = null, val invoiceId: String? = null, val amount: Double? = null, val currency: String = "IDR"`
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 28. `POST` /api/v1/billing/payment/webhook
- **Grep Confirmation**: Line 623 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 29. `POST` /api/v1/billing/webhook/midtrans
- **Grep Confirmation**: Line 624 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 30. `GET` /api/v1/billing/upgrade-recommendation
- **Grep Confirmation**: Line 630 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`
- **Header Wajib**: `Authorization: Bearer <jwt>`, `X-Tenant-Id: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

## 16. Admin Domain - Operations, Security, MCP & Financial Command
- **File Sumber**: `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Jumlah Endpoint**: **89**

### 1. `GET` /admin/admin/presence/security-stats
- **Grep Confirmation**: Line 439 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `stats`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 2. `GET` /admin/admin/tenants
- **Grep Confirmation**: Line 446 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 3. `POST` /admin/admin/tenants
- **Grep Confirmation**: Line 458 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `AdminTenantCreateRequest`: `val name: String, val tier: String = "GROWTH", val ownerEmail: String`
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 4. `GET` /admin/admin/llm-providers
- **Grep Confirmation**: Line 475 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `AdminDomainStores.llmProviders.toList(`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 5. `POST` /admin/admin/llm-providers
- **Grep Confirmation**: Line 479 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `AdminLlmProviderCreateRequest`: `val name: String, val providerType: String = "OPENROUTER", val baseUrl: String? = null, val enabled: Boolean = true, val taskSpecialization: String = "general", val fallbackPriority: Int = 1, val apiKey: String? = null, val models: List<String> = emptyList(`
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 6. `PUT` /admin/admin/llm-providers/{id}
- **Grep Confirmation**: Line 501 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 7. `DELETE` /admin/admin/llm-providers/{id}
- **Grep Confirmation**: Line 522 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 8. `POST` /admin/admin/llm-providers/{id}/toggle-status
- **Grep Confirmation**: Line 532 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 9. `GET` /admin/admin/image-providers
- **Grep Confirmation**: Line 547 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `AdminDomainStores.imageProviders.toList(`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 10. `POST` /admin/admin/image-providers
- **Grep Confirmation**: Line 551 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `AdminImageProviderCreateRequest`: `val name: String, val providerType: String, val models: List<String> = listOf("image-gen-v1"`
- **Response Body Schema**: HttpStatusCode.Created: `newImgProv`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 11. `DELETE` /admin/admin/image-providers/{id}
- **Grep Confirmation**: Line 567 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 12. `GET` /admin/admin/master-data/categories
- **Grep Confirmation**: Line 580 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `categories`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: `commercial_plans` (SELECT)

### 13. `GET` /admin/admin/master-data
- **Grep Confirmation**: Line 596 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `allItems`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 14. `POST` /admin/admin/master-data
- **Grep Confirmation**: Line 607 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `AdminMasterDataCreateRequest`: `val category: String, val key: String, val value: String, val description: String? = null`
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 15. `DELETE` /admin/admin/master-data/{id}
- **Grep Confirmation**: Line 629 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 16. `DELETE` /admin/admin/master-data/{category}/{id}
- **Grep Confirmation**: Line 645 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 17. `GET` /admin/admin/skill-plugins
- **Grep Confirmation**: Line 660 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `AdminDomainStores.skillPlugins.toList(`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 18. `POST` /admin/admin/skill-plugins
- **Grep Confirmation**: Line 664 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `AdminSkillPluginCreateRequest`: `val name: String, val version: String, val author: String, val executionRuntime: String = "WASM", val status: String = "PENDING_APPROVAL"`
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 19. `PUT` /admin/admin/skill-plugins/{id}
- **Grep Confirmation**: Line 684 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 20. `DELETE` /admin/admin/skill-plugins/{id}
- **Grep Confirmation**: Line 704 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 21. `PATCH` /admin/admin/skill-plugins/{id}/status
- **Grep Confirmation**: Line 714 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 22. `POST` /admin/admin/skill-plugins/upload
- **Grep Confirmation**: Line 728 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `AdminSkillPluginUploadRequest`: `val pluginName: String, val version: String = "1.0.0", val author: String = "Super Admin", val manifestJson: String, val skillDefinitionMd: String = "", val zipBase64: String? = null`
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 23. `GET` /admin/admin/mcp-tools
- **Grep Confirmation**: Line 782 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `AdminDomainStores.mcpTools.toList(`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 24. `POST` /admin/admin/mcp-tools
- **Grep Confirmation**: Line 786 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `AdminMcpToolCreateRequest`: `val name: String, val description: String, val riskLevel: String = "LOW", val requiredRole: String = "STAFF_HUMAN", val restrictedToOperationMode: String = "UNRESTRICTED", val inputSchema: String = "{}"`
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 25. `PUT` /admin/admin/mcp-tools/{id}
- **Grep Confirmation**: Line 808 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 26. `DELETE` /admin/admin/mcp-tools/{id}
- **Grep Confirmation**: Line 828 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 27. `PATCH` /admin/admin/mcp-tools/{id}/kill-switch
- **Grep Confirmation**: Line 838 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 28. `GET` /admin/admin/app-registry
- **Grep Confirmation**: Line 855 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `AdminDomainStores.appRegistry.toList(`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 29. `POST` /admin/admin/app-registry
- **Grep Confirmation**: Line 859 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `AdminAppRegistryCreateRequest`: `val appName: String, val appType: String, val clientId: String, val scopes: List<String> = emptyList(`
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 30. `PUT` /admin/admin/app-registry/{id}
- **Grep Confirmation**: Line 879 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 31. `DELETE` /admin/admin/app-registry/{id}
- **Grep Confirmation**: Line 900 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 32. `PATCH` /admin/admin/app-registry/{id}/mark-migration
- **Grep Confirmation**: Line 910 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 33. `GET` /admin/admin/analytics/tenant-workforce-summary
- **Grep Confirmation**: Line 931 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `summary`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 34. `GET` /admin/admin/monitoring/system-overview
- **Grep Confirmation**: Line 957 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `overview`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 35. `GET` /admin/admin/audit-logs
- **Grep Confirmation**: Line 1001 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `baseLogs`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 36. `GET` /admin/admin/usage
- **Grep Confirmation**: Line 1023 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 37. `GET` /admin/admin/llm-usage
- **Grep Confirmation**: Line 1039 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 38. `GET` /admin/admin/health-check
- **Grep Confirmation**: Line 1050 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 39. `POST` /admin/admin/jobs/trigger
- **Grep Confirmation**: Line 1071 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `TriggerJobApiRequest`: `val jobName: String, val tenantId: String = "tenant-admin", val forceTestFailure: Boolean = false`
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 40. `GET` /admin/admin/billing/subscriptions
- **Grep Confirmation**: Line 1090 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 41. `GET` /admin/admin/billing/invoices
- **Grep Confirmation**: Line 1107 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 42. `GET` /admin/admin/specialist-agents
- **Grep Confirmation**: Line 1123 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 43. `GET` /admin/admin/studio/templates
- **Grep Confirmation**: Line 1148 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 44. `GET` /admin/admin/dead-letter-queue
- **Grep Confirmation**: Line 1171 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `items`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 45. `POST` /admin/admin/dead-letter-queue/{id}/reprocess
- **Grep Confirmation**: Line 1178 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 46. `POST` /admin/admin/workflow-executions/{id}/replay
- **Grep Confirmation**: Line 1209 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 47. `GET` /admin/admin/workflow-executions
- **Grep Confirmation**: Line 1232 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `executions`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 48. `GET` /admin/admin/analytics/overview
- **Grep Confirmation**: Line 1246 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `overview`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 49. `GET` /admin/admin/analytics/usage-credit
- **Grep Confirmation**: Line 1254 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `usageList`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 50. `GET` /admin/admin/analytics/llm-usage-platform-wide
- **Grep Confirmation**: Line 1262 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `llmUsage`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 51. `GET` /admin/admin/analytics/kpi-summary
- **Grep Confirmation**: Line 1270 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `kpi`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 52. `GET` /admin/admin/analytics/daily-task-performance
- **Grep Confirmation**: Line 1278 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `metrics`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 53. `GET` /admin/admin/analytics/task-activity-summary
- **Grep Confirmation**: Line 1289 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `summary`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 54. `GET` /admin/admin/analytics/universal-selection-usage
- **Grep Confirmation**: Line 1300 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `usage`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 55. `POST` /admin/admin/analytics/transactions
- **Grep Confirmation**: Line 1307 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `AdminRecordTransactionRequest`: `val tenantId: String, val customerId: String = "cust-new-001", val amount: Double, val orderNumber: String? = null`
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 56. `GET` /admin/admin/payment-reconciliation/orders
- **Grep Confirmation**: Line 1339 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `dtoList`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 57. `GET` /admin/admin/payment-reconciliation/queue
- **Grep Confirmation**: Line 1373 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `items`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 58. `POST` /admin/admin/payment-reconciliation/{id}/confirm
- **Grep Confirmation**: Line 1386 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 59. `POST` /admin/admin/payment-reconciliation/{id}/reject
- **Grep Confirmation**: Line 1466 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 60. `POST` /admin/admin/payment-reconciliation/trigger-check
- **Grep Confirmation**: Line 1522 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 61. `POST` /admin/admin/payment-reconciliation/simulate-stuck
- **Grep Confirmation**: Line 1538 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `X-Tenant-ID: <tenant-uuid>`
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 62. `GET` /admin/admin/commercial/plans
- **Grep Confirmation**: Line 1599 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `plans`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 63. `POST` /admin/admin/commercial/plans
- **Grep Confirmation**: Line 1609 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `CommercialPlanUpsertRequest`: `val id: String? = null, val planCode: String, val planName: String, val billingInterval: String = "monthly", val price: Double? = null, val currency: String = "IDR", val creditAllocation: Double? = null, val humanSeatLimit: Int? = null, val aiAgentLimit: Int? = null, val isPriceVisible: Boolean = true, val isActive: Boolean = true, val sortOrder: Int? = null`
- **Response Body Schema**: HttpStatusCode.OK: `saved`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 64. `DELETE` /admin/admin/commercial/plans/{id}
- **Grep Confirmation**: Line 1634 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 65. `GET` /admin/admin/commercial/entitlements-matrix
- **Grep Confirmation**: Line 1646 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `matrix`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 66. `POST` /admin/admin/commercial/entitlements
- **Grep Confirmation**: Line 1656 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `EntitlementUpdateRequest`: `val planCode: String, val featureKey: String, val value: String`
- **Response Body Schema**: HttpStatusCode.OK: `mapOf("success" to ok`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 67. `GET` /admin/admin/commercial/custom-override/{tenantId}
- **Grep Confirmation**: Line 1668 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 68. `POST` /admin/admin/commercial/custom-override/{tenantId}
- **Grep Confirmation**: Line 1679 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 69. `GET` /admin/admin/commercial/metering-rules
- **Grep Confirmation**: Line 1692 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `rules`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 70. `POST` /admin/admin/commercial/metering-rules
- **Grep Confirmation**: Line 1702 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `CreditMeteringRule`: `val id: String = UUID.randomUUID(`
- **Response Body Schema**: HttpStatusCode.OK: `saved`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 71. `DELETE` /admin/admin/commercial/metering-rules/{activityType}
- **Grep Confirmation**: Line 1713 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 72. `GET` /admin/admin/commercial/cost-factors
- **Grep Confirmation**: Line 1725 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `factors`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 73. `POST` /admin/admin/commercial/cost-factors
- **Grep Confirmation**: Line 1735 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `CreditCostFactor`: `val id: String = UUID.randomUUID(`
- **Response Body Schema**: HttpStatusCode.OK: `saved`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 74. `DELETE` /admin/admin/commercial/cost-factors/{factorType}/{factorKey}
- **Grep Confirmation**: Line 1746 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 75. `POST` /admin/admin/commercial/simulate-cost
- **Grep Confirmation**: Line 1759 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `CreditCostContext`: `val activityType: String, val complexityLevel: String = "simple", // 'simple'/'medium'/'complex' val modelUsed: String = "standard", // 'openrouter'/'groq'/'deepseek'/'claude'/'kimi'/'standard' val toolsInvoked: Int = 0, val executionType: String = "single_step" // 'single_step'/'multi_step'/'autonomous'`
- **Response Body Schema**: HttpStatusCode.OK: `res`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 76. `POST` /admin/admin/billing/credit-adjustment
- **Grep Confirmation**: Line 1775 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `ManualCreditAdjustmentRequest`: `val tenantId: String, val amount: Double, val ledgerType: String, // 'CREDIT_ADJUSTMENT', 'CREDIT_BONUS', 'CREDIT_REFUNDED', 'CREDIT_EXPIRED' val reason: String, val operatorId: String = "superadmin@orchestree.ai"`
- **Response Body Schema**: ``
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 77. `GET` /admin/admin/billing/tenant-wallet/{tenantId}
- **Grep Confirmation**: Line 1806 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 78. `GET` /admin/admin/financial-command-center
- **Grep Confirmation**: Line 1831 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `data`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 79. `GET` /admin/admin/analytics/financial-command-center
- **Grep Confirmation**: Line 1841 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `data`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 80. `POST` /admin/admin/platform-assets/icon-logo
- **Grep Confirmation**: Line 1856 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`
- **Request Body Schema**: `ai.orchestree.backend.api.BrandLogoUploadRequest`
- **Response Body Schema**: HttpStatusCode.Created: `result`
- **Status Engine Terhubung**: Ya — Terkoneksi ke BrandAssetService (Tenant Brand Voice, Logo Storage & Visual Style Injection)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 81. `GET` /admin/admin/platform-assets/icon-logo
- **Grep Confirmation**: Line 1871 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `mapOf("platformIconLogoUrl" to (url ?: ""`
- **Status Engine Terhubung**: Ya — Terkoneksi ke BrandAssetService (Tenant Brand Voice, Logo Storage & Visual Style Injection)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 82. `GET` /admin/admin/security/ip-allowlist
- **Grep Confirmation**: Line 1879 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `AdminIpAllowlistDto(enabled = enabled`
- **Status Engine Terhubung**: Ya — Terkoneksi ke AdminSecurityService (IP Allowlisting, Step-up MFA & CSRF Token Validation)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 83. `POST` /admin/admin/security/ip-allowlist
- **Grep Confirmation**: Line 1885 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`, `X-Operator-Id: <operator-id>`
- **Request Body Schema**: `AdminIpAllowlistDto`: `val enabled: Boolean, val allowedIps: List<String> = emptyList(`
- **Response Body Schema**: HttpStatusCode.OK: `req`
- **Status Engine Terhubung**: Ya — Terkoneksi ke AdminSecurityService (IP Allowlisting, Step-up MFA & CSRF Token Validation)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 84. `POST` /admin/admin/support/impersonate
- **Grep Confirmation**: Line 1905 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`, `X-Operator-Id: <operator-id>`
- **Request Body Schema**: `AdminSupportImpersonateRequest`: `val targetTenantId: String, val reason: String, val durationMinutes: Long = 30`
- **Response Body Schema**: HttpStatusCode.Created: `AdminSupportSessionResponse(`
- **Status Engine Terhubung**: Ya — Terkoneksi ke AdminSecurityService (IP Allowlisting, Step-up MFA & CSRF Token Validation)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 85. `GET` /admin/admin/support/impersonate/{sessionId}
- **Grep Confirmation**: Line 1930 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: `HttpStatusCode.OK`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 86. `GET` /admin/admin/security/csrf-token
- **Grep Confirmation**: Line 1956 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: None (No special headers required)
- **Request Body Schema**: None (No Request Body)
- **Response Body Schema**: HttpStatusCode.OK: `mapOf("csrfToken" to token`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 87. `POST` /admin/admin/login
- **Grep Confirmation**: Line 1970 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`, `X-Forwarded-For: <ip-address>`
- **Request Body Schema**: `AdminLoginRequest`: `val email: String, val password: String`
- **Response Body Schema**: HttpStatusCode.Forbidden: `mapOf("error" to "IP Access Forbidden by Super Admin Allowlist policy"`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 88. `POST` /admin/admin/auth/login
- **Grep Confirmation**: Line 2035 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`, `X-Forwarded-For: <ip-address>`
- **Request Body Schema**: `AdminLoginRequest`: `val email: String, val password: String`
- **Response Body Schema**: HttpStatusCode.Forbidden: `mapOf("error" to "IP Access Forbidden by Super Admin Allowlist policy"`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless

### 89. `POST` /admin/admin/auth/verify-mfa
- **Grep Confirmation**: Line 2077 in `orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`
- **Header Wajib**: `Content-Type: application/json`, `X-Forwarded-For: <ip-address>`
- **Request Body Schema**: `AdminVerifyMfaRequest`: `val email: String, val totpCode: String`
- **Response Body Schema**: HttpStatusCode.Forbidden: `mapOf("error" to "IP Access Forbidden by Super Admin Allowlist policy"`
- **Status Engine Terhubung**: Tidak (Direct Service/Repo Call)
- **Tabel Supabase Terpengaruh**: Tidak langsung / Stateless
