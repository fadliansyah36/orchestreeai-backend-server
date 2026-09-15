### Domain 1: Core Infrastructure & Webhook Gateways (`orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/plugins/Routing.kt`)

| No | Endpoint | Status Saat Ini | Tabel Supabase Tujuan | Prioritas |
|---|---|---|---|:---:|
| 1 | `GET` `/` | Stateless (Health Check) | `-` | RENDAH |
| 2 | `GET` `/health` | Stateless (Health Check) | `-` | RENDAH |
| 3 | `GET` `/api/health` | Stateless (Health Check) | `-` | RENDAH |
| 4 | `GET` `/api/v1/platform-assets/icon-logo` | Sudah Terhubung | `-` | RENDAH |
| 5 | `POST` `/api/v1/payments/webhook/{gateway}` | Sudah Terhubung | `-` | RENDAH |
| 6 | `POST` `/api/v1/shipments/webhook/{courier}` | Belum Terhubung | `-` | RENDAH |
| 7 | `POST` `/api/v1/webhooks/whatsapp` | Belum Terhubung | `-` | RENDAH |
| 8 | `POST` `/api/v1/webhooks/telegram` | Belum Terhubung | `-` | RENDAH |
| 9 | `POST` `/api/v1/webhook/whatsapp` | Belum Terhubung | `-` | RENDAH |
| 10 | `POST` `/api/v1/webhook/telegram` | Belum Terhubung | `-` | RENDAH |

### Domain 2: Core Authentication & Profile Lifecycle (`orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AuthRoutes.kt`)

| No | Endpoint | Status Saat Ini | Tabel Supabase Tujuan | Prioritas |
|---|---|---|---|:---:|
| 1 | `POST` `/api/v1/auth/login` | Belum Terhubung | `users` | TINGGI |
| 2 | `POST` `/api/v1/auth/refresh` | Belum Terhubung | `-` | TINGGI |
| 3 | `GET` `/api/v1/auth/sessions` | Belum Terhubung | `sessions` | TINGGI |
| 4 | `POST` `/api/v1/auth/sessions/revoke/{id}` | Belum Terhubung | `user_sessions` | TINGGI |
| 5 | `GET` `/api/v1/auth/profile` | Belum Terhubung | `-` | TINGGI |
| 6 | `POST` `/api/v1/auth/profile` | Belum Terhubung | `users` | TINGGI |

### Domain 3: Core Master Data & Structural Role Catalog (`orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/MasterDataRoutes.kt`)

| No | Endpoint | Status Saat Ini | Tabel Supabase Tujuan | Prioritas |
|---|---|---|---|:---:|
| 1 | `GET` `/api/v1/public/department-categories` | Belum Terhubung | `departments` | RENDAH |
| 2 | `POST` `/api/v1/public/department-categories` | Belum Terhubung | `departments` | RENDAH |
| 3 | `GET` `/api/v1/public/industry-catalog` | Belum Terhubung | `-` | RENDAH |
| 4 | `GET` `/api/v1/public/job-level-catalog` | Belum Terhubung | `-` | RENDAH |
| 5 | `GET` `/api/v1/public/job-sub-title-catalog` | Belum Terhubung | `-` | RENDAH |
| 6 | `GET` `/api/v1/tenants/{id}/ai-job-titles` | Belum Terhubung | `-` | RENDAH |
| 7 | `POST` `/api/v1/tenants/{id}/ai-job-titles` | Belum Terhubung | `-` | RENDAH |
| 8 | `GET` `/api/v1/tenants/{id}/ai-job-titles/{jobTitleId}/structural-roles` | Belum Terhubung | `-` | RENDAH |
| 9 | `GET` `/api/v1/tenants/{id}/ai-job-titles/{jobTitleId}/available-skills` | Belum Terhubung | `-` | RENDAH |

### Domain 4: Core Biometric Presence & Liveness (`orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/PresenceRoutes.kt`)

| No | Endpoint | Status Saat Ini | Tabel Supabase Tujuan | Prioritas |
|---|---|---|---|:---:|
| 1 | `POST` `/api/v1/presence/enroll` | Belum Terhubung | `-` | TINGGI |
| 2 | `GET` `/api/v1/presence/requirement-check` | Belum Terhubung | `-` | TINGGI |
| 3 | `POST` `/api/v1/presence/verify` | Belum Terhubung | `-` | TINGGI |
| 4 | `GET` `/api/v1/presence/enrollment` | Sudah Terhubung | `-` | TINGGI |
| 5 | `GET` `/api/v1/presence/logs` | Sudah Terhubung | `-` | TINGGI |
| 6 | `GET` `/api/v1/presence/security-audit-stats` | Belum Terhubung | `-` | TINGGI |

### Domain 5: Core Attendance & Geofencing Intelligence (`orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AttendanceRoutes.kt`)

| No | Endpoint | Status Saat Ini | Tabel Supabase Tujuan | Prioritas |
|---|---|---|---|:---:|
| 1 | `POST` `/api/v1/attendance/check-in` | Sudah Terhubung | `attendance_records` | TINGGI |
| 2 | `GET` `/api/v1/attendance/anomalies` | Sudah Terhubung | `attendance_anomalies` | TINGGI |
| 3 | `GET` `/api/v1/attendance/history` | Masih Hardcode (Hardcode listOf DTO) | `-` | TINGGI |
| 4 | `GET` `/api/v1/tenants/{id}/geofences` | Masih Hardcode (Hardcode listOf DTO) | `geofences` | TINGGI |
| 5 | `POST` `/api/v1/tenants/{id}/geofences` | Belum Terhubung | `geofences` | TINGGI |

### Domain 6: Core Prospect Registration & Enterprise Trials (`orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/prospect/ProspectRoutes.kt`)

| No | Endpoint | Status Saat Ini | Tabel Supabase Tujuan | Prioritas |
|---|---|---|---|:---:|
| 1 | `POST` `/api/v1/public/prospect-registration` | Belum Terhubung | `-` | SEDANG |
| 2 | `GET` `/api/v1/admin/prospect-registrations` | Belum Terhubung | `-` | SEDANG |
| 3 | `GET` `/api/v1/admin/prospect-registrations/analytics` | Belum Terhubung | `-` | SEDANG |
| 4 | `PATCH` `/api/v1/admin/prospect-registrations/{id}/select-trial` | Belum Terhubung | `-` | SEDANG |
| 5 | `PATCH` `/api/v1/admin/prospect-registrations/{id}/schedule-meeting` | Belum Terhubung | `-` | SEDANG |
| 6 | `POST` `/api/v1/admin/prospect-registrations/{id}/activate-trial` | Belum Terhubung | `-` | SEDANG |

### Domain 7: Tenant & Hybrid Workforce Management (`orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/TenantRoutes.kt`)

| No | Endpoint | Status Saat Ini | Tabel Supabase Tujuan | Prioritas |
|---|---|---|---|:---:|
| 1 | `GET` `/api/v1/tenants/{id}/dashboard/overview` | Masih Hardcode (Hardcode dashboard numbers) | `-` | TINGGI |
| 2 | `GET` `/api/v1/tenants/{id}/departments` | Sudah Terhubung | `departments` | TINGGI |
| 3 | `POST` `/api/v1/tenants/{id}/departments` | Sudah Terhubung | `departments` | TINGGI |
| 4 | `DELETE` `/api/v1/tenants/{id}/departments/{deptId}` | Sudah Terhubung | `departments` | TINGGI |
| 5 | `GET` `/api/v1/tenants/{id}/staff` | Sudah Terhubung | `staff` | TINGGI |
| 6 | `DELETE` `/api/v1/tenants/{id}/staff/{staffId}` | Sudah Terhubung | `staff` | TINGGI |
| 7 | `POST` `/api/v1/tenants/{id}/staff` | Sudah Terhubung | `users` | TINGGI |
| 8 | `GET` `/api/v1/tenants/{id}/agents` | Sudah Terhubung | `ai_agents` | TINGGI |
| 9 | `POST` `/api/v1/tenants/{id}/agents` | Sudah Terhubung | `ai_agents` | TINGGI |
| 10 | `GET` `/api/v1/tenants/{id}/tasks` | Sudah Terhubung | `tasks` | TINGGI |
| 11 | `POST` `/api/v1/tenants/{id}/tasks` | Sudah Terhubung | `tasks` | TINGGI |
| 12 | `GET` `/api/v1/tenants/{id}/boards/{boardId}` | Masih Hardcode (Hardcode board columns) | `-` | TINGGI |
| 13 | `GET` `/api/v1/tasks` | Sudah Terhubung | `tasks` | TINGGI |
| 14 | `POST` `/api/v1/tasks/inbound-channel-message` | Sudah Terhubung | `tasks` | TINGGI |
| 15 | `GET` `/api/v1/tasks/proactive/subscriptions` | Masih Hardcode (Hardcode listOf DTO) | `tasks` | TINGGI |
| 16 | `GET` `/api/v1/tasks/proactive/scope/{staffId}` | Sudah Terhubung | `staff` | TINGGI |
| 17 | `POST` `/api/v1/tasks/proactive/subscriptions` | Sudah Terhubung | `staff` | TINGGI |
| 18 | `POST` `/api/v1/tenants/tasks/inbound-channel-message` | Sudah Terhubung | `tasks` | TINGGI |
| 19 | `PATCH` `/api/v1/tasks/{taskId}/move` | Sudah Terhubung | `tasks` | TINGGI |
| 20 | `GET` `/api/v1/tasks/{taskId}/checklists` | Sudah Terhubung | `tasks` | TINGGI |
| 21 | `POST` `/api/v1/tasks/{taskId}/checklists` | Sudah Terhubung | `tasks` | TINGGI |
| 22 | `PATCH` `/api/v1/tasks/{taskId}/checklists/{checklistId}/toggle` | Sudah Terhubung | `tasks` | TINGGI |
| 23 | `GET` `/api/v1/tasks/{taskId}/activity-log` | Sudah Terhubung | `tasks` | TINGGI |
| 24 | `PATCH` `/api/v1/tasks/{taskId}/description` | Sudah Terhubung | `tasks` | TINGGI |
| 25 | `GET` `/api/v1/tasks/{taskId}/attachments` | Sudah Terhubung | `tasks` | TINGGI |
| 26 | `POST` `/api/v1/tasks/{taskId}/attachments` | Sudah Terhubung | `tasks` | TINGGI |
| 27 | `GET` `/api/v1/intel/competitors` | Masih Hardcode (Hardcode emptyList()) | `-` | TINGGI |
| 28 | `POST` `/api/v1/intel/competitors` | Sudah Terhubung | `-` | TINGGI |
| 29 | `GET` `/api/v1/intel/competitors/{id}/insights` | Masih Hardcode (Hardcode listOf DTO) | `-` | TINGGI |
| 30 | `GET` `/api/v1/intel/world-trends` | Masih Hardcode (Hardcode listOf DTO) | `-` | TINGGI |
| 31 | `POST` `/api/v1/integrations/{platform}/connect` | Belum Terhubung | `-` | TINGGI |
| 32 | `GET` `/api/v1/proactive/subscriptions` | Masih Hardcode (Hardcode listOf DTO) | `-` | TINGGI |
| 33 | `GET` `/api/v1/proactive/scope/{staffId}` | Sudah Terhubung | `staff` | TINGGI |
| 34 | `POST` `/api/v1/proactive/subscriptions` | Sudah Terhubung | `staff` | TINGGI |
| 35 | `GET` `/api/v1/analytics/scores` | Belum Terhubung | `-` | TINGGI |
| 36 | `GET` `/api/v1/performance/reports` | Masih Hardcode (Hardcode listOf DTO) | `-` | TINGGI |
| 37 | `POST` `/api/v1/performance/reports` | Belum Terhubung | `staff` | TINGGI |
| 38 | `GET` `/api/v1/performance/goals` | Masih Hardcode (Hardcode listOf DTO) | `-` | TINGGI |
| 39 | `GET` `/api/v1/performance/reviews` | Masih Hardcode (Hardcode listOf DTO) | `-` | TINGGI |
| 40 | `GET` `/api/v1/performance/predictions` | Masih Hardcode (Hardcode listOf DTO) | `-` | TINGGI |
| 41 | `GET` `/api/v1/performance/executive-briefs` | Masih Hardcode (Hardcode listOf DTO) | `-` | TINGGI |
| 42 | `GET` `/api/v1/security/anomalies` | Masih Hardcode (Hardcode listOf DTO) | `-` | TINGGI |
| 43 | `GET` `/api/v1/security/dsr` | Masih Hardcode (Hardcode listOf DTO) | `-` | TINGGI |
| 44 | `POST` `/api/v1/security/dsr` | Belum Terhubung | `-` | TINGGI |
| 45 | `GET` `/api/v1/attendance/anomalies` | Masih Hardcode (Hardcode listOf DTO) | `-` | TINGGI |
| 46 | `POST` `/api/v1/attendance/anomalies/{anomalyId}/resolve` | Belum Terhubung | `-` | TINGGI |

### Domain 8: Orchestration & Autonomous Workflow DAG (`orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OrchestrationRoutes.kt`)

| No | Endpoint | Status Saat Ini | Tabel Supabase Tujuan | Prioritas |
|---|---|---|---|:---:|
| 1 | `POST` `/api/v1/orchestration/dispatch` | Belum Terhubung | `-` | TINGGI |
| 2 | `POST` `/api/v1/orchestration/workflows/run` | Belum Terhubung | `-` | TINGGI |
| 3 | `GET` `/api/v1/orchestration/status/{executionId}` | Belum Terhubung | `-` | TINGGI |
| 4 | `GET` `/api/v1/orchestration/executions/{executionId}` | Belum Terhubung | `-` | TINGGI |
| 5 | `GET` `/api/v1/orchestration/approvals` | Sudah Terhubung | `-` | TINGGI |
| 6 | `POST` `/api/v1/orchestration/approvals/{id}/decision` | Belum Terhubung | `-` | TINGGI |
| 7 | `GET` `/api/v1/orchestration/traces` | Belum Terhubung | `-` | TINGGI |
| 8 | `GET` `/api/v1/orchestration/traces/{executionId}` | Belum Terhubung | `-` | TINGGI |
| 9 | `GET` `/api/v1/orchestration/traces/by-trace/{traceId}` | Belum Terhubung | `-` | TINGGI |
| 10 | `GET` `/api/v1/intelligence/confidence/calibration` | Sudah Terhubung | `-` | TINGGI |
| 11 | `POST` `/api/v1/intelligence/confidence/calibrate` | Belum Terhubung | `-` | TINGGI |
| 12 | `GET` `/api/v1/intelligence/confidence/audit` | Belum Terhubung | `-` | TINGGI |
| 13 | `GET` `/api/v1/mcp/tools` | Belum Terhubung | `-` | TINGGI |
| 14 | `POST` `/api/v1/mcp/execute` | Belum Terhubung | `-` | TINGGI |

### Domain 9: AI Chat & Brain Knowledge RAG (`orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/ChatRoutes.kt`)

| No | Endpoint | Status Saat Ini | Tabel Supabase Tujuan | Prioritas |
|---|---|---|---|:---:|
| 1 | `POST` `/api/v1/chat` | Masih Hardcode (Hardcode emptyList()) | `-` | TINGGI |
| 2 | `POST` `/api/v1/chat/messages` | Masih Hardcode (Hardcode emptyList()) | `-` | TINGGI |
| 3 | `GET` `/api/v1/chat/history/{conversationId}` | Belum Terhubung | `-` | TINGGI |
| 4 | `POST` `/api/v1/agents/{agentId}/chat` | Sudah Terhubung | `-` | TINGGI |
| 5 | `POST` `/api/v1/company-brain/documents` | Belum Terhubung | `-` | TINGGI |
| 6 | `GET` `/api/v1/company-brain/search` | Belum Terhubung | `-` | TINGGI |

### Domain 10: Universal Selection & Autonomous Ranking (`orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/SelectionRoutes.kt`)

| No | Endpoint | Status Saat Ini | Tabel Supabase Tujuan | Prioritas |
|---|---|---|---|:---:|
| 1 | `POST` `/api/v1/selection/upload` | Sudah Terhubung | `-` | TINGGI |
| 2 | `POST` `/api/v1/selection/prompt-only` | Sudah Terhubung | `-` | TINGGI |
| 3 | `POST` `/api/v1/selection/api-database` | Sudah Terhubung | `-` | TINGGI |
| 4 | `GET` `/api/v1/selection/requests` | Sudah Terhubung | `-` | TINGGI |
| 5 | `GET` `/api/v1/selection/{id}` | Sudah Terhubung | `-` | TINGGI |
| 6 | `GET` `/api/v1/selection/{id}/results` | Sudah Terhubung | `-` | TINGGI |
| 7 | `GET` `/api/v1/selection/{id}/analytics` | Sudah Terhubung | `-` | TINGGI |
| 8 | `POST` `/api/v1/selection/documents/{documentId}/understand` | Belum Terhubung | `-` | TINGGI |
| 9 | `GET` `/api/v1/selection/documents/{documentId}/understanding` | Belum Terhubung | `-` | TINGGI |
| 10 | `POST` `/api/v1/selection/calibration` | Belum Terhubung | `-` | TINGGI |
| 11 | `GET` `/api/v1/selection/calibration` | Sudah Terhubung | `-` | TINGGI |
| 12 | `GET` `/api/v1/selection/calibration/{id}` | Sudah Terhubung | `-` | TINGGI |
| 13 | `POST` `/api/v1/selection/calibration/{id}/validate-dataset/{documentId}` | Sudah Terhubung | `-` | TINGGI |
| 14 | `POST` `/api/v1/selection/results/{id}/review` | Sudah Terhubung | `-` | TINGGI |
| 15 | `POST` `/api/v1/selection/results/{id}/execute-downstream` | Sudah Terhubung | `-` | TINGGI |
| 16 | `GET` `/api/v1/selection/{id}/export` | Sudah Terhubung | `-` | TINGGI |
| 17 | `POST` `/api/v1/selection/{id}/export` | Sudah Terhubung | `-` | TINGGI |
| 18 | `GET` `/api/v1/selection/auto-selection/configs` | Sudah Terhubung | `-` | TINGGI |
| 19 | `POST` `/api/v1/selection/auto-selection/configs` | Sudah Terhubung | `-` | TINGGI |
| 20 | `DELETE` `/api/v1/selection/auto-selection/configs/{id}` | Sudah Terhubung | `-` | TINGGI |
| 21 | `POST` `/api/v1/selection/webhook/storage` | Sudah Terhubung | `-` | TINGGI |
| 22 | `POST` `/api/v1/selection/webhook/integration-fabric` | Sudah Terhubung | `-` | TINGGI |

### Domain 11: Generative Studio & Brand Asset Management (`orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/GenerativeStudioRoutes.kt`)

| No | Endpoint | Status Saat Ini | Tabel Supabase Tujuan | Prioritas |
|---|---|---|---|:---:|
| 1 | `POST` `/api/v1/studio/generate-image` | Sudah Terhubung | `-` | TINGGI |
| 2 | `POST` `/api/v1/studio/compose-prompt` | Belum Terhubung | `-` | TINGGI |
| 3 | `POST` `/api/v1/studio/campaign-creative` | Belum Terhubung | `campaigns` | TINGGI |
| 4 | `GET` `/api/v1/studio/templates` | Masih Hardcode (Hardcode emptyList()) | `-` | TINGGI |
| 5 | `POST` `/api/v1/studio/brand-assets/upload-logo` | Belum Terhubung | `-` | TINGGI |
| 6 | `POST` `/api/v1/studio/brand-assets/logo` | Belum Terhubung | `-` | TINGGI |
| 7 | `GET` `/api/v1/studio/assets` | Sudah Terhubung | `-` | TINGGI |
| 8 | `POST` `/api/v1/studio/assets` | Sudah Terhubung | `-` | TINGGI |

### Domain 12: Enterprise Governance, Context Fabric & Chief of Staff (`orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/EnterpriseRoutes.kt`)

| No | Endpoint | Status Saat Ini | Tabel Supabase Tujuan | Prioritas |
|---|---|---|---|:---:|
| 1 | `GET` `/api/v1/tenants/{id}/enterprise-connections` | Belum Terhubung | `-` | SEDANG |
| 2 | `POST` `/api/v1/tenants/{id}/enterprise-connections` | Belum Terhubung | `-` | SEDANG |
| 3 | `GET` `/api/v1/tenants/{id}/ai-data-permissions` | Belum Terhubung | `-` | SEDANG |
| 4 | `POST` `/api/v1/tenants/{id}/ai-data-permissions` | Belum Terhubung | `-` | SEDANG |
| 5 | `POST` `/api/v1/tenants/{id}/ai-data-permissions/check` | Belum Terhubung | `-` | SEDANG |
| 6 | `POST` `/api/v1/tenants/{id}/tier` | Belum Terhubung | `-` | SEDANG |
| 7 | `POST` `/api/v1/tenants/{id}/downgrade` | Belum Terhubung | `-` | SEDANG |
| 8 | `GET` `/api/v1/tenants/{id}/activity-stream` | Belum Terhubung | `-` | SEDANG |
| 9 | `GET` `/api/v1/tenants/{id}/context-fabric/{entityId}` | Belum Terhubung | `-` | SEDANG |
| 10 | `POST` `/api/v1/tenants/{id}/management-query` | Masih Hardcode (Hardcode emptyList()) | `-` | SEDANG |
| 11 | `POST` `/api/v1/tenants/{id}/correlate-signals` | Belum Terhubung | `-` | SEDANG |
| 12 | `GET` `/api/v1/tenants/{id}/reports/daily` | Belum Terhubung | `-` | SEDANG |
| 13 | `GET` `/api/v1/tenants/{id}/knowledge-rules` | Belum Terhubung | `-` | SEDANG |
| 14 | `POST` `/api/v1/tenants/{id}/knowledge-rules` | Belum Terhubung | `-` | SEDANG |
| 15 | `POST` `/api/v1/tenants/{id}/knowledge-rules/{ruleId}/approve` | Belum Terhubung | `-` | SEDANG |
| 16 | `POST` `/api/v1/tenants/{id}/knowledge-rules/fuse` | Belum Terhubung | `-` | SEDANG |
| 17 | `GET` `/api/v1/tenants/{id}/events` | Belum Terhubung | `-` | SEDANG |
| 18 | `POST` `/api/v1/tenants/{id}/events` | Belum Terhubung | `-` | SEDANG |
| 19 | `GET` `/api/v1/tenants/{id}/finance/cashflow-pressure` | Belum Terhubung | `-` | SEDANG |
| 20 | `GET` `/api/v1/tenants/{id}/actions` | Belum Terhubung | `-` | SEDANG |
| 21 | `POST` `/api/v1/tenants/{id}/actions/propose` | Belum Terhubung | `-` | SEDANG |
| 22 | `POST` `/api/v1/tenants/{id}/actions/{actionId}/execute` | Belum Terhubung | `-` | SEDANG |
| 23 | `GET` `/api/v1/tenants/{id}/monitoring-loops` | Belum Terhubung | `-` | SEDANG |
| 24 | `POST` `/api/v1/tenants/{id}/monitoring-loops` | Belum Terhubung | `-` | SEDANG |
| 25 | `POST` `/api/v1/tenants/{id}/monitoring-loops/tick` | Belum Terhubung | `-` | SEDANG |
| 26 | `GET` `/api/v1/tenants/{id}/swarm/status` | Belum Terhubung | `-` | SEDANG |
| 27 | `POST` `/api/v1/tenants/{id}/swarm/freeze` | Belum Terhubung | `-` | SEDANG |
| 28 | `POST` `/api/v1/tenants/{id}/swarm/resume` | Belum Terhubung | `-` | SEDANG |
| 29 | `GET` `/api/v1/tenants/{id}/chief-of-staff/briefings` | Masih Hardcode (Hardcode listOf DTO) | `chief_of_staff_briefings` | SEDANG |
| 30 | `POST` `/api/v1/tenants/{id}/chief-of-staff/synthesize` | Sudah Terhubung | `staff` | SEDANG |
| 31 | `POST` `/api/v1/tenants/{id}/chief-of-staff/research-directives` | Sudah Terhubung | `chief_of_staff_briefings` | SEDANG |
| 32 | `GET` `/api/v1/tenants/{id}/agents/{agentId}/skill-confidence` | Belum Terhubung | `-` | SEDANG |
| 33 | `GET` `/api/v1/tenants/{id}/data-quality-issues` | Belum Terhubung | `company_activity_stream` | SEDANG |
| 34 | `POST` `/api/v1/tenants/{id}/chief-of-staff/briefings/generate` | Belum Terhubung | `staff` | SEDANG |
| 35 | `GET` `/api/v1/tenants/{id}/chief-of-staff/research-directives` | Belum Terhubung | `staff` | SEDANG |
| 36 | `POST` `/api/v1/tenants/{id}/data-quality-issues/resolve` | Belum Terhubung | `-` | SEDANG |
| 37 | `GET` `/api/v1/tenants/{id}/project-health/{projectId}` | Belum Terhubung | `-` | SEDANG |
| 38 | `POST` `/api/v1/tenants/{id}/project-health/evaluate` | Belum Terhubung | `-` | SEDANG |
| 39 | `POST` `/api/v1/tenants/{id}/multi-agent-collaborations/initiate` | Belum Terhubung | `-` | SEDANG |
| 40 | `GET` `/api/v1/tenants/{id}/multi-agent-collaborations` | Belum Terhubung | `-` | SEDANG |
| 41 | `GET` `/api/v1/tenants/{id}/explainability/{executionId}` | Belum Terhubung | `-` | SEDANG |

### Domain 13: Autonomous Memory Consolidation & Decay (`orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/MemoryRoutes.kt`)

| No | Endpoint | Status Saat Ini | Tabel Supabase Tujuan | Prioritas |
|---|---|---|---|:---:|
| 1 | `POST` `/api/v1/memory/consolidate/evaluate` | Belum Terhubung | `-` | SEDANG |
| 2 | `POST` `/api/v1/memory/consolidate/batch` | Belum Terhubung | `-` | SEDANG |
| 3 | `POST` `/api/v1/memory/decay` | Belum Terhubung | `-` | SEDANG |
| 4 | `POST` `/api/v1/memory/search` | Belum Terhubung | `-` | SEDANG |
| 5 | `POST` `/api/v1/memory/search/ab-compare` | Belum Terhubung | `-` | SEDANG |
| 6 | `GET` `/api/v1/memory/documents` | Sudah Terhubung | `-` | SEDANG |

### Domain 14: Omnichannel Sales, CRM & Channel Gateway (`orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/OmnichannelSalesRoutes.kt`)

| No | Endpoint | Status Saat Ini | Tabel Supabase Tujuan | Prioritas |
|---|---|---|---|:---:|
| 1 | `GET` `/api/v1/tenants/{id}/channel-accounts` | Sudah Terhubung | `channel_accounts` | TINGGI |
| 2 | `POST` `/api/v1/tenants/{id}/channel-accounts` | Belum Terhubung | `channel_accounts` | TINGGI |
| 3 | `POST` `/api/v1/tenants/{id}/channel-accounts/{caId}/verify` | Belum Terhubung | `channel_accounts` | TINGGI |
| 4 | `PATCH` `/api/v1/tenants/{id}/channel-accounts/{caId}/approve` | Belum Terhubung | `channel_accounts` | TINGGI |
| 5 | `GET` `/api/v1/tenants/{id}/channel-accounts/{caId}/health` | Belum Terhubung | `channel_accounts` | TINGGI |
| 6 | `GET` `/api/v1/tenants/{id}/credit-wallet` | Belum Terhubung | `ai_credit_wallets` | TINGGI |
| 7 | `GET` `/api/v1/tenants/{id}/customers/search` | Belum Terhubung | `customers` | TINGGI |
| 8 | `POST` `/api/v1/tenants/{id}/customers/resolve` | Belum Terhubung | `customers` | TINGGI |
| 9 | `POST` `/api/v1/tenants/{id}/customers/{cust_id}/merge` | Masih Hardcode (Hardcode emptyList()) | `customers` | TINGGI |
| 10 | `GET` `/api/v1/tenants/{id}/inbox/conversations` | Belum Terhubung | `customers` | TINGGI |
| 11 | `GET` `/api/v1/tenants/{id}/leads` | Belum Terhubung | `leads` | TINGGI |
| 12 | `POST` `/api/v1/tenants/{id}/leads` | Sudah Terhubung | `leads` | TINGGI |
| 13 | `GET` `/api/v1/tenants/{id}/products` | Belum Terhubung | `products` | TINGGI |
| 14 | `POST` `/api/v1/tenants/{id}/products` | Belum Terhubung | `products` | TINGGI |
| 15 | `GET` `/api/v1/tenants/{id}/inventory` | Belum Terhubung | `-` | TINGGI |
| 16 | `GET` `/api/v1/tenants/{id}/inventory/{variantId}` | Belum Terhubung | `-` | TINGGI |
| 17 | `GET` `/api/v1/tenants/{id}/orders` | Belum Terhubung | `orders` | TINGGI |
| 18 | `GET` `/api/v1/tenants/{id}/campaigns` | Belum Terhubung | `campaigns` | TINGGI |
| 19 | `POST` `/api/v1/tenants/{id}/campaigns` | Belum Terhubung | `campaigns` | TINGGI |
| 20 | `GET` `/api/v1/tenants/{id}/analytics/revenue-intelligence` | Belum Terhubung | `orders` | TINGGI |
| 21 | `GET` `/api/v1/tenants/{id}/analytics/sales-coach` | Belum Terhubung | `-` | TINGGI |
| 22 | `POST` `/api/v1/tenants/{id}/experiments` | Belum Terhubung | `-` | TINGGI |
| 23 | `GET` `/api/v1/tenants/{id}/service-requests` | Belum Terhubung | `customers` | TINGGI |
| 24 | `POST` `/api/v1/tenants/{id}/service-requests` | Belum Terhubung | `service_requests` | TINGGI |
| 25 | `PATCH` `/api/v1/tenants/{id}/ai-agents/{agentId}/persona` | Belum Terhubung | `ai_agents` | TINGGI |
| 26 | `POST` `/api/v1/tenants/{id}/conversations/{convId}/persona-reply` | Sudah Terhubung | `-` | TINGGI |
| 27 | `POST` `/api/v1/conversations/{id}/takeover` | Belum Terhubung | `staff` | TINGGI |
| 28 | `POST` `/api/v1/carts/{id}/checkout` | Belum Terhubung | `orders` | TINGGI |

### Domain 15: Commercial Billing, Quota & Dunning (`orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/BillingRoutes.kt`)

| No | Endpoint | Status Saat Ini | Tabel Supabase Tujuan | Prioritas |
|---|---|---|---|:---:|
| 1 | `GET` `/api/v1/plans` | Belum Terhubung | `-` | TINGGI |
| 2 | `GET` `/api/v1/plans/{id}` | Belum Terhubung | `-` | TINGGI |
| 3 | `GET` `/api/v1/plans/entitlements-matrix` | Belum Terhubung | `-` | TINGGI |
| 4 | `GET` `/api/v1/tenant/entitlements` | Belum Terhubung | `-` | TINGGI |
| 5 | `GET` `/api/v1/billing/plans` | Belum Terhubung | `-` | TINGGI |
| 6 | `GET` `/api/v1/billing/plans/{id}` | Belum Terhubung | `-` | TINGGI |
| 7 | `POST` `/api/v1/billing/calculate-cost` | Belum Terhubung | `-` | TINGGI |
| 8 | `GET` `/api/v1/billing/subscription` | Belum Terhubung | `-` | TINGGI |
| 9 | `POST` `/api/v1/billing/subscription` | Belum Terhubung | `-` | TINGGI |
| 10 | `POST` `/api/v1/billing/subscription/upgrade` | Belum Terhubung | `-` | TINGGI |
| 11 | `POST` `/api/v1/billing/subscription/downgrade` | Belum Terhubung | `-` | TINGGI |
| 12 | `POST` `/api/v1/billing/subscription/cancel` | Belum Terhubung | `-` | TINGGI |
| 13 | `GET` `/api/v1/billing/credits` | Belum Terhubung | `-` | TINGGI |
| 14 | `GET` `/api/v1/billing/credits/summary` | Belum Terhubung | `-` | TINGGI |
| 15 | `GET` `/api/v1/billing/credits/ledger` | Belum Terhubung | `-` | TINGGI |
| 16 | `GET` `/api/v1/billing/ledger` | Belum Terhubung | `-` | TINGGI |
| 17 | `POST` `/api/v1/billing/credits/topup` | Belum Terhubung | `-` | TINGGI |
| 18 | `GET` `/api/v1/billing/seats` | Sudah Terhubung | `-` | TINGGI |
| 19 | `POST` `/api/v1/billing/seats` | Sudah Terhubung | `staff` | TINGGI |
| 20 | `DELETE` `/api/v1/billing/seats/{id}` | Sudah Terhubung | `-` | TINGGI |
| 21 | `GET` `/api/v1/billing/agents` | Sudah Terhubung | `-` | TINGGI |
| 22 | `POST` `/api/v1/billing/agents` | Sudah Terhubung | `-` | TINGGI |
| 23 | `DELETE` `/api/v1/billing/agents/{id}` | Sudah Terhubung | `-` | TINGGI |
| 24 | `GET` `/api/v1/billing/invoices` | Belum Terhubung | `invoices` | TINGGI |
| 25 | `GET` `/api/v1/billing/invoices/{id}` | Belum Terhubung | `invoices` | TINGGI |
| 26 | `POST` `/api/v1/billing/invoices/{id}/fail-dunning` | Sudah Terhubung | `invoices` | TINGGI |
| 27 | `POST` `/api/v1/billing/payment` | Belum Terhubung | `-` | TINGGI |
| 28 | `POST` `/api/v1/billing/payment/webhook` | Belum Terhubung | `-` | TINGGI |
| 29 | `POST` `/api/v1/billing/webhook/midtrans` | Belum Terhubung | `-` | TINGGI |
| 30 | `GET` `/api/v1/billing/upgrade-recommendation` | Sudah Terhubung | `-` | TINGGI |

### Domain 16: Super Admin, Operations & Security Command (`orchestreeai-backend-server/src/main/kotlin/ai/orchestree/backend/api/AdminRoutes.kt`)

| No | Endpoint | Status Saat Ini | Tabel Supabase Tujuan | Prioritas |
|---|---|---|---|:---:|
| 1 | `GET` `/api/v1/admin/presence/security-stats` | Belum Terhubung | `-` | SEDANG |
| 2 | `GET` `/api/v1/admin/tenants` | Sudah Terhubung | `-` | SEDANG |
| 3 | `POST` `/api/v1/admin/tenants` | Belum Terhubung | `-` | SEDANG |
| 4 | `GET` `/api/v1/admin/llm-providers` | Masih Hardcode (In-memory AdminDomainStores) | `-` | SEDANG |
| 5 | `POST` `/api/v1/admin/llm-providers` | Masih Hardcode (In-memory AdminDomainStores) | `-` | SEDANG |
| 6 | `PUT` `/api/v1/admin/llm-providers/{id}` | Masih Hardcode (In-memory AdminDomainStores) | `-` | SEDANG |
| 7 | `DELETE` `/api/v1/admin/llm-providers/{id}` | Masih Hardcode (In-memory AdminDomainStores) | `-` | SEDANG |
| 8 | `POST` `/api/v1/admin/llm-providers/{id}/toggle-status` | Masih Hardcode (In-memory AdminDomainStores) | `-` | SEDANG |
| 9 | `GET` `/api/v1/admin/image-providers` | Masih Hardcode (In-memory AdminDomainStores) | `-` | SEDANG |
| 10 | `POST` `/api/v1/admin/image-providers` | Masih Hardcode (In-memory AdminDomainStores) | `-` | SEDANG |
| 11 | `DELETE` `/api/v1/admin/image-providers/{id}` | Masih Hardcode (In-memory AdminDomainStores) | `-` | SEDANG |
| 12 | `GET` `/api/v1/admin/master-data/categories` | Masih Hardcode (In-memory AdminDomainStores) | `commercial_plans` | SEDANG |
| 13 | `GET` `/api/v1/admin/master-data` | Masih Hardcode (In-memory AdminDomainStores) | `-` | SEDANG |
| 14 | `POST` `/api/v1/admin/master-data` | Masih Hardcode (In-memory AdminDomainStores) | `-` | SEDANG |
| 15 | `DELETE` `/api/v1/admin/master-data/{id}` | Masih Hardcode (In-memory AdminDomainStores) | `-` | SEDANG |
| 16 | `DELETE` `/api/v1/admin/master-data/{category}/{id}` | Masih Hardcode (In-memory AdminDomainStores) | `-` | SEDANG |
| 17 | `GET` `/api/v1/admin/skill-plugins` | Masih Hardcode (In-memory AdminDomainStores) | `-` | SEDANG |
| 18 | `POST` `/api/v1/admin/skill-plugins` | Masih Hardcode (In-memory AdminDomainStores) | `-` | SEDANG |
| 19 | `PUT` `/api/v1/admin/skill-plugins/{id}` | Masih Hardcode (In-memory AdminDomainStores) | `-` | SEDANG |
| 20 | `DELETE` `/api/v1/admin/skill-plugins/{id}` | Masih Hardcode (In-memory AdminDomainStores) | `-` | SEDANG |
| 21 | `PATCH` `/api/v1/admin/skill-plugins/{id}/status` | Masih Hardcode (In-memory AdminDomainStores) | `-` | SEDANG |
| 22 | `POST` `/api/v1/admin/skill-plugins/upload` | Masih Hardcode (In-memory AdminDomainStores) | `-` | SEDANG |
| 23 | `GET` `/api/v1/admin/mcp-tools` | Masih Hardcode (In-memory AdminDomainStores) | `mcp_tools` | SEDANG |
| 24 | `POST` `/api/v1/admin/mcp-tools` | Masih Hardcode (In-memory AdminDomainStores) | `mcp_tools` | SEDANG |
| 25 | `PUT` `/api/v1/admin/mcp-tools/{id}` | Masih Hardcode (In-memory AdminDomainStores) | `-` | SEDANG |
| 26 | `DELETE` `/api/v1/admin/mcp-tools/{id}` | Masih Hardcode (In-memory AdminDomainStores) | `mcp_tools` | SEDANG |
| 27 | `PATCH` `/api/v1/admin/mcp-tools/{id}/kill-switch` | Masih Hardcode (In-memory AdminDomainStores) | `-` | SEDANG |
| 28 | `GET` `/api/v1/admin/app-registry` | Masih Hardcode (In-memory AdminDomainStores) | `-` | SEDANG |
| 29 | `POST` `/api/v1/admin/app-registry` | Masih Hardcode (In-memory AdminDomainStores) | `-` | SEDANG |
| 30 | `PUT` `/api/v1/admin/app-registry/{id}` | Masih Hardcode (In-memory AdminDomainStores) | `-` | SEDANG |
| 31 | `DELETE` `/api/v1/admin/app-registry/{id}` | Masih Hardcode (In-memory AdminDomainStores) | `-` | SEDANG |
| 32 | `PATCH` `/api/v1/admin/app-registry/{id}/mark-migration` | Masih Hardcode (In-memory AdminDomainStores) | `-` | SEDANG |
| 33 | `GET` `/api/v1/admin/analytics/tenant-workforce-summary` | Masih Hardcode (Hardcode listOf DTO) | `-` | SEDANG |
| 34 | `GET` `/api/v1/admin/monitoring/system-overview` | Sudah Terhubung | `-` | SEDANG |
| 35 | `POST` `/api/v1/admin/swarm/freeze` | Belum Terhubung | `-` | SEDANG |
| 36 | `POST` `/api/v1/admin/swarm/resume` | Belum Terhubung | `-` | SEDANG |
| 37 | `GET` `/api/v1/admin/swarm/status` | Belum Terhubung | `-` | SEDANG |
| 38 | `GET` `/api/v1/admin/audit-logs` | Belum Terhubung | `audit_logs` | SEDANG |
| 39 | `GET` `/api/v1/admin/usage` | Sudah Terhubung | `-` | SEDANG |
| 40 | `GET` `/api/v1/admin/llm-usage` | Sudah Terhubung | `-` | SEDANG |
| 41 | `GET` `/api/v1/admin/health-check` | Belum Terhubung | `-` | SEDANG |
| 42 | `POST` `/api/v1/admin/jobs/trigger` | Belum Terhubung | `-` | SEDANG |
| 43 | `GET` `/api/v1/admin/billing/subscriptions` | Belum Terhubung | `tenants` | SEDANG |
| 44 | `GET` `/api/v1/admin/billing/invoices` | Belum Terhubung | `invoices` | SEDANG |
| 45 | `GET` `/api/v1/admin/specialist-agents` | Masih Hardcode (Hardcode listOf DTO) | `ai_agents` | SEDANG |
| 46 | `GET` `/api/v1/admin/studio/templates` | Masih Hardcode (Hardcode listOf DTO) | `-` | SEDANG |
| 47 | `GET` `/api/v1/admin/dead-letter-queue` | Sudah Terhubung | `-` | SEDANG |
| 48 | `POST` `/api/v1/admin/dead-letter-queue/{id}/reprocess` | Belum Terhubung | `-` | SEDANG |
| 49 | `POST` `/api/v1/admin/workflow-executions/{id}/replay` | Belum Terhubung | `-` | SEDANG |
| 50 | `GET` `/api/v1/admin/workflow-executions` | Sudah Terhubung | `-` | SEDANG |
| 51 | `GET` `/api/v1/admin/analytics/overview` | Sudah Terhubung | `-` | SEDANG |
| 52 | `GET` `/api/v1/admin/analytics/usage-credit` | Sudah Terhubung | `-` | SEDANG |
| 53 | `GET` `/api/v1/admin/analytics/llm-usage-platform-wide` | Sudah Terhubung | `-` | SEDANG |
| 54 | `GET` `/api/v1/admin/analytics/kpi-summary` | Sudah Terhubung | `-` | SEDANG |
| 55 | `GET` `/api/v1/admin/analytics/daily-task-performance` | Sudah Terhubung | `tasks` | SEDANG |
| 56 | `GET` `/api/v1/admin/analytics/task-activity-summary` | Sudah Terhubung | `tasks` | SEDANG |
| 57 | `GET` `/api/v1/admin/analytics/universal-selection-usage` | Sudah Terhubung | `-` | SEDANG |
| 58 | `POST` `/api/v1/admin/analytics/transactions` | Sudah Terhubung | `-` | SEDANG |
| 59 | `GET` `/api/v1/admin/payment-reconciliation/orders` | Sudah Terhubung | `orders` | SEDANG |
| 60 | `GET` `/api/v1/admin/payment-reconciliation/queue` | Sudah Terhubung | `-` | SEDANG |
| 61 | `POST` `/api/v1/admin/payment-reconciliation/{id}/confirm` | Sudah Terhubung | `-` | SEDANG |
| 62 | `POST` `/api/v1/admin/payment-reconciliation/{id}/reject` | Sudah Terhubung | `-` | SEDANG |
| 63 | `POST` `/api/v1/admin/payment-reconciliation/trigger-check` | Belum Terhubung | `-` | SEDANG |
| 64 | `POST` `/api/v1/admin/payment-reconciliation/simulate-stuck` | Sudah Terhubung | `-` | SEDANG |
| 65 | `GET` `/api/v1/admin/commercial/plans` | Belum Terhubung | `-` | SEDANG |
| 66 | `POST` `/api/v1/admin/commercial/plans` | Belum Terhubung | `-` | SEDANG |
| 67 | `DELETE` `/api/v1/admin/commercial/plans/{id}` | Belum Terhubung | `-` | SEDANG |
| 68 | `GET` `/api/v1/admin/commercial/entitlements-matrix` | Belum Terhubung | `-` | SEDANG |
| 69 | `POST` `/api/v1/admin/commercial/entitlements` | Belum Terhubung | `-` | SEDANG |
| 70 | `GET` `/api/v1/admin/commercial/custom-override/{tenantId}` | Belum Terhubung | `-` | SEDANG |
| 71 | `POST` `/api/v1/admin/commercial/custom-override/{tenantId}` | Belum Terhubung | `-` | SEDANG |
| 72 | `GET` `/api/v1/admin/commercial/metering-rules` | Belum Terhubung | `-` | SEDANG |
| 73 | `POST` `/api/v1/admin/commercial/metering-rules` | Belum Terhubung | `-` | SEDANG |
| 74 | `DELETE` `/api/v1/admin/commercial/metering-rules/{activityType}` | Belum Terhubung | `-` | SEDANG |
| 75 | `GET` `/api/v1/admin/commercial/cost-factors` | Belum Terhubung | `-` | SEDANG |
| 76 | `POST` `/api/v1/admin/commercial/cost-factors` | Belum Terhubung | `-` | SEDANG |
| 77 | `DELETE` `/api/v1/admin/commercial/cost-factors/{factorType}/{factorKey}` | Belum Terhubung | `-` | SEDANG |
| 78 | `POST` `/api/v1/admin/commercial/simulate-cost` | Belum Terhubung | `-` | SEDANG |
| 79 | `POST` `/api/v1/admin/billing/credit-adjustment` | Belum Terhubung | `-` | SEDANG |
| 80 | `GET` `/api/v1/admin/billing/tenant-wallet/{tenantId}` | Belum Terhubung | `-` | SEDANG |
| 81 | `GET` `/api/v1/admin/financial-command-center` | Belum Terhubung | `-` | SEDANG |
| 82 | `GET` `/api/v1/admin/analytics/financial-command-center` | Belum Terhubung | `-` | SEDANG |
| 83 | `POST` `/api/v1/admin/platform-assets/icon-logo` | Sudah Terhubung | `-` | SEDANG |
| 84 | `GET` `/api/v1/admin/platform-assets/icon-logo` | Sudah Terhubung | `-` | SEDANG |
| 85 | `GET` `/api/v1/admin/security/ip-allowlist` | Sudah Terhubung | `-` | SEDANG |
| 86 | `POST` `/api/v1/admin/security/ip-allowlist` | Sudah Terhubung | `-` | SEDANG |
| 87 | `POST` `/api/v1/admin/support/impersonate` | Sudah Terhubung | `-` | SEDANG |
| 88 | `GET` `/api/v1/admin/support/impersonate/{sessionId}` | Sudah Terhubung | `sessions` | SEDANG |
| 89 | `GET` `/api/v1/admin/security/csrf-token` | Belum Terhubung | `-` | SEDANG |
| 90 | `POST` `/api/v1/admin/login` | Belum Terhubung | `-` | SEDANG |
| 91 | `POST` `/api/v1/admin/auth/login` | Belum Terhubung | `-` | SEDANG |
| 92 | `POST` `/api/v1/admin/auth/verify-mfa` | Belum Terhubung | `-` | SEDANG |
