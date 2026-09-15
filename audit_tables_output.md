# HASIL AUDIT ULANG DEFINITIF (LANGKAH 0) — SEMUA 16 DOMAIN (335 ENDPOINTS)

### Domain 1: Core Infrastructure & Webhook Gateways (10 Endpoints) — Prioritas: DASAR / INTI

| No | Method | Endpoint | Status Saat Ini | Tabel Supabase Tujuan | Prioritas |
|:---:|:---:|---|---|---|:---:|
| 1 | `GET` | `/api/v1/` | Stateless (Root HTTP Banner) | Stateless / System Info | DASAR / INTI |
| 2 | `GET` | `/api/v1/health` | Sudah Diperbaiki (Terhubung DB/Engine) | Stateless / Live Health Diagnostics | DASAR / INTI |
| 3 | `GET` | `/api/v1/api/health` | Sudah Diperbaiki (Terhubung DB/Engine) | Stateless / Live Health Diagnostics | DASAR / INTI |
| 4 | `GET` | `/api/v1/api/v1/platform-assets/icon-logo` | Stateless (Root HTTP Banner) | Stateless / System Info | DASAR / INTI |
| 5 | `POST` | `/api/v1/api/v1/payments/webhook/{gateway}` | Sudah Diperbaiki (Terhubung DB/Engine) | `channel_webhook_events` (INSERT), `orders` (SELECT/UPDATE) | DASAR / INTI |
| 6 | `POST` | `/api/v1/api/v1/shipments/webhook/{courier}` | Sudah Diperbaiki (Terhubung DB/Engine) | `channel_webhook_events` (INSERT), `orders` (SELECT/UPDATE) | DASAR / INTI |
| 7 | `POST` | `/api/v1/api/v1/webhooks/whatsapp` | Sudah Diperbaiki (Terhubung DB/Engine) | `channel_webhook_events` (INSERT), `orders` (SELECT/UPDATE) | DASAR / INTI |
| 8 | `POST` | `/api/v1/api/v1/webhooks/telegram` | Sudah Diperbaiki (Terhubung DB/Engine) | `channel_webhook_events` (INSERT), `orders` (SELECT/UPDATE) | DASAR / INTI |
| 9 | `POST` | `/api/v1/api/v1/webhook/whatsapp` | Sudah Diperbaiki (Terhubung DB/Engine) | `channel_webhook_events` (INSERT), `orders` (SELECT/UPDATE) | DASAR / INTI |
| 10 | `POST` | `/api/v1/api/v1/webhook/telegram` | Sudah Diperbaiki (Terhubung DB/Engine) | `channel_webhook_events` (INSERT), `orders` (SELECT/UPDATE) | DASAR / INTI |

---

### Domain 2: Core Authentication & Profile Lifecycle (6 Endpoints) — Prioritas: DASAR / INTI

| No | Method | Endpoint | Status Saat Ini | Tabel Supabase Tujuan | Prioritas |
|:---:|:---:|---|---|---|:---:|
| 1 | `POST` | `/auth/login` | Sudah Diperbaiki (Terhubung DB/Engine) | `users` (SELECT), `user_sessions` (INSERT/UPDATE) | DASAR / INTI |
| 2 | `POST` | `/auth/refresh` | Sudah Diperbaiki (Terhubung DB/Engine) | `users` (SELECT/UPDATE) | DASAR / INTI |
| 3 | `GET` | `/auth/sessions` | Sudah Diperbaiki (Terhubung DB/Engine) | `users` (SELECT/UPDATE) | DASAR / INTI |
| 4 | `POST` | `/auth/sessions/revoke/{id}` | Sudah Diperbaiki (Terhubung DB/Engine) | `users` (SELECT/UPDATE) | DASAR / INTI |
| 5 | `GET` | `/auth/profile` | Sudah Diperbaiki (Terhubung DB/Engine) | `users` (SELECT/UPDATE), `tenants` (SELECT) | DASAR / INTI |
| 6 | `POST` | `/auth/profile` | Sudah Diperbaiki (Terhubung DB/Engine) | `users` (SELECT/UPDATE), `tenants` (SELECT) | DASAR / INTI |

---

### Domain 3: Core Master Data & Structural Role Catalog (9 Endpoints) — Prioritas: DASAR / INTI

| No | Method | Endpoint | Status Saat Ini | Tabel Supabase Tujuan | Prioritas |
|:---:|:---:|---|---|---|:---:|
| 1 | `GET` | `/api/v1/public/department-categories` | Sudah Diperbaiki (Terhubung DB/Engine) | `master_data_catalog` (SELECT) | DASAR / INTI |
| 2 | `POST` | `/api/v1/public/department-categories` | Sudah Diperbaiki (Terhubung DB/Engine) | `master_data_catalog` (SELECT) | DASAR / INTI |
| 3 | `GET` | `/api/v1/public/industry-catalog` | Sudah Diperbaiki (Terhubung DB/Engine) | `master_data_catalog` (SELECT) | DASAR / INTI |
| 4 | `GET` | `/api/v1/public/job-level-catalog` | Sudah Diperbaiki (Terhubung DB/Engine) | `master_data_catalog` (SELECT) | DASAR / INTI |
| 5 | `GET` | `/api/v1/public/job-sub-title-catalog` | Sudah Diperbaiki (Terhubung DB/Engine) | `master_data_catalog` (SELECT) | DASAR / INTI |
| 6 | `GET` | `/api/v1/tenants/{id}/ai-job-titles` | Sudah Diperbaiki (Terhubung DB/Engine) | `master_data_catalog` (SELECT) | DASAR / INTI |
| 7 | `POST` | `/api/v1/tenants/{id}/ai-job-titles` | Sudah Diperbaiki (Terhubung DB/Engine) | `master_data_catalog` (SELECT) | DASAR / INTI |
| 8 | `GET` | `/api/v1/tenants/{id}/ai-job-titles/{jobTitleId}/structural-roles` | Sudah Diperbaiki (Terhubung DB/Engine) | `ai_structural_roles` (SELECT), `master_data` (SELECT) | DASAR / INTI |
| 9 | `GET` | `/api/v1/tenants/{id}/ai-job-titles/{jobTitleId}/available-skills` | Sudah Diperbaiki (Terhubung DB/Engine) | `master_data_catalog` (SELECT) | DASAR / INTI |

---

### Domain 4: Core Biometric Presence & Liveness (6 Endpoints) — Prioritas: DASAR / INTI

| No | Method | Endpoint | Status Saat Ini | Tabel Supabase Tujuan | Prioritas |
|:---:|:---:|---|---|---|:---:|
| 1 | `POST` | `/api/v1/presence/enroll` | Sudah Diperbaiki (Terhubung DB/Engine) | `biometric_verification_logs` (SELECT) | DASAR / INTI |
| 2 | `GET` | `/api/v1/presence/requirement-check` | Sudah Diperbaiki (Terhubung DB/Engine) | `biometric_verification_logs` (SELECT) | DASAR / INTI |
| 3 | `POST` | `/api/v1/presence/verify` | Sudah Diperbaiki (Terhubung DB/Engine) | `biometric_verification_logs` (INSERT), `users` (SELECT) | DASAR / INTI |
| 4 | `GET` | `/api/v1/presence/enrollment` | Sudah Diperbaiki (Terhubung DB/Engine) | `biometric_verification_logs` (SELECT) | DASAR / INTI |
| 5 | `GET` | `/api/v1/presence/logs` | Sudah Diperbaiki (Terhubung DB/Engine) | `biometric_verification_logs` (SELECT) | DASAR / INTI |
| 6 | `GET` | `/api/v1/presence/security-audit-stats` | Sudah Diperbaiki (Terhubung DB/Engine) | `biometric_verification_logs` (SELECT) | DASAR / INTI |

---

### Domain 5: Core Attendance & Geofencing Intelligence (5 Endpoints) — Prioritas: DASAR / INTI

| No | Method | Endpoint | Status Saat Ini | Tabel Supabase Tujuan | Prioritas |
|:---:|:---:|---|---|---|:---:|
| 1 | `POST` | `/api/v1/attendance/check-in` | Sudah Diperbaiki (Terhubung DB/Engine) | `attendance_records` (INSERT), `work_locations` (SELECT) | DASAR / INTI |
| 2 | `GET` | `/api/v1/attendance/anomalies` | Sudah Diperbaiki (Terhubung DB/Engine) | `attendance_records` (SELECT/INSERT) | DASAR / INTI |
| 3 | `GET` | `/api/v1/attendance/history` | Sudah Diperbaiki (Terhubung DB/Engine) | `attendance_records` (SELECT) | DASAR / INTI |
| 4 | `GET` | `/api/v1/tenants/{id}/geofences` | Sudah Diperbaiki (Terhubung DB/Engine) | `attendance_records` (SELECT/INSERT) | DASAR / INTI |
| 5 | `POST` | `/api/v1/tenants/{id}/geofences` | Sudah Diperbaiki (Terhubung DB/Engine) | `attendance_records` (SELECT/INSERT) | DASAR / INTI |

---

### Domain 6: Core Prospect Registration & Enterprise Trials (6 Endpoints) — Prioritas: DASAR / INTI

| No | Method | Endpoint | Status Saat Ini | Tabel Supabase Tujuan | Prioritas |
|:---:|:---:|---|---|---|:---:|
| 1 | `POST` | `/api/v1/public/prospect-registration` | Sudah Diperbaiki (Terhubung DB/Engine) | `prospects` (SELECT) | DASAR / INTI |
| 2 | `GET` | `/api/v1/admin/prospect-registrations` | Sudah Diperbaiki (Terhubung DB/Engine) | `prospects` (SELECT) | DASAR / INTI |
| 3 | `GET` | `/api/v1/admin/prospect-registrations/analytics` | Sudah Diperbaiki (Terhubung DB/Engine) | `prospects` (SELECT) | DASAR / INTI |
| 4 | `PATCH` | `/api/v1/admin/prospect-registrations/{id}/select-trial` | Sudah Diperbaiki (Terhubung DB/Engine) | `prospects` (SELECT) | DASAR / INTI |
| 5 | `PATCH` | `/api/v1/admin/prospect-registrations/{id}/schedule-meeting` | Sudah Diperbaiki (Terhubung DB/Engine) | `prospects` (SELECT) | DASAR / INTI |
| 6 | `POST` | `/api/v1/admin/prospect-registrations/{id}/activate-trial` | Sudah Diperbaiki (Terhubung DB/Engine) | `prospects` (SELECT) | DASAR / INTI |

---

### Domain 7: Tenant & Hybrid Workforce Management (46 Endpoints) — Prioritas: TINGGI

| No | Method | Endpoint | Status Saat Ini | Tabel Supabase Tujuan | Prioritas |
|:---:|:---:|---|---|---|:---:|
| 1 | `GET` | `/api/v1/tenants/{id}/dashboard/overview` | Sudah Diperbaiki (Terhubung DB/Engine) | `tasks` (SELECT/INSERT/UPDATE), `board_columns` (SELECT) | TINGGI |
| 2 | `GET` | `/api/v1/tenants/{id}/departments` | Sudah Diperbaiki (Terhubung DB/Engine) | `departments` (SELECT/INSERT/UPDATE) | TINGGI |
| 3 | `POST` | `/api/v1/tenants/{id}/departments` | Sudah Diperbaiki (Terhubung DB/Engine) | `departments` (SELECT/INSERT/UPDATE) | TINGGI |
| 4 | `DELETE` | `/api/v1/tenants/{id}/departments/{deptId}` | Sudah Diperbaiki (Terhubung DB/Engine) | `departments` (SELECT/INSERT/UPDATE) | TINGGI |
| 5 | `GET` | `/api/v1/tenants/{id}/staff` | Sudah Diperbaiki (Terhubung DB/Engine) | `users` (SELECT/INSERT/UPDATE), `departments` (SELECT) | TINGGI |
| 6 | `DELETE` | `/api/v1/tenants/{id}/staff/{staffId}` | Sudah Diperbaiki (Terhubung DB/Engine) | `users` (SELECT/INSERT/UPDATE), `departments` (SELECT) | TINGGI |
| 7 | `POST` | `/api/v1/tenants/{id}/staff` | Sudah Diperbaiki (Terhubung DB/Engine) | `users` (SELECT/INSERT/UPDATE), `departments` (SELECT) | TINGGI |
| 8 | `GET` | `/api/v1/tenants/{id}/agents` | Sudah Diperbaiki (Terhubung DB/Engine) | `ai_agents` (SELECT/INSERT/UPDATE), `agent_skill_confidence` (SELECT) | TINGGI |
| 9 | `POST` | `/api/v1/tenants/{id}/agents` | Sudah Diperbaiki (Terhubung DB/Engine) | `ai_agents` (SELECT/INSERT/UPDATE), `agent_skill_confidence` (SELECT) | TINGGI |
| 10 | `GET` | `/api/v1/tenants/{id}/tasks` | Sudah Diperbaiki (Terhubung DB/Engine) | `tasks` (SELECT/INSERT/UPDATE), `board_columns` (SELECT) | TINGGI |
| 11 | `POST` | `/api/v1/tenants/{id}/tasks` | Sudah Diperbaiki (Terhubung DB/Engine) | `tasks` (SELECT/INSERT/UPDATE), `board_columns` (SELECT) | TINGGI |
| 12 | `GET` | `/api/v1/tenants/{id}/boards/{boardId}` | Sudah Diperbaiki (Terhubung DB/Engine) | `tasks` (SELECT/INSERT/UPDATE), `board_columns` (SELECT) | TINGGI |
| 13 | `GET` | `/api/v1/tasks` | Sudah Diperbaiki (Terhubung DB/Engine) | `tasks` (SELECT/INSERT/UPDATE), `board_columns` (SELECT) | TINGGI |
| 14 | `POST` | `/api/v1/tasks/inbound-channel-message` | Sudah Diperbaiki (Terhubung DB/Engine) | `tasks` (SELECT/INSERT/UPDATE), `board_columns` (SELECT) | TINGGI |
| 15 | `GET` | `/api/v1/tasks/proactive/subscriptions` | Sudah Diperbaiki (Terhubung DB/Engine) | `tasks` (SELECT/INSERT/UPDATE), `board_columns` (SELECT) | TINGGI |
| 16 | `GET` | `/api/v1/tasks/proactive/scope/{staffId}` | Sudah Diperbaiki (Terhubung DB/Engine) | `tasks` (SELECT/INSERT/UPDATE), `board_columns` (SELECT) | TINGGI |
| 17 | `POST` | `/api/v1/tasks/proactive/subscriptions` | Sudah Diperbaiki (Terhubung DB/Engine) | `tasks` (SELECT/INSERT/UPDATE), `board_columns` (SELECT) | TINGGI |
| 18 | `POST` | `/api/v1/tenants/tasks/inbound-channel-message` | Sudah Diperbaiki (Terhubung DB/Engine) | `tasks` (SELECT/INSERT/UPDATE), `board_columns` (SELECT) | TINGGI |
| 19 | `PATCH` | `/api/v1/tasks/{taskId}/move` | Sudah Diperbaiki (Terhubung DB/Engine) | `tasks` (UPDATE), `task_activity_logs` (INSERT) | TINGGI |
| 20 | `GET` | `/api/v1/tasks/{taskId}/checklists` | Sudah Diperbaiki (Terhubung DB/Engine) | `task_checklists` (INSERT/UPDATE) | TINGGI |
| 21 | `POST` | `/api/v1/tasks/{taskId}/checklists` | Sudah Diperbaiki (Terhubung DB/Engine) | `task_checklists` (INSERT/UPDATE) | TINGGI |
| 22 | `PATCH` | `/api/v1/tasks/{taskId}/checklists/{checklistId}/toggle` | Sudah Diperbaiki (Terhubung DB/Engine) | `task_checklists` (INSERT/UPDATE) | TINGGI |
| 23 | `GET` | `/api/v1/tasks/{taskId}/activity-log` | Sudah Diperbaiki (Terhubung DB/Engine) | `tasks` (SELECT/INSERT/UPDATE), `board_columns` (SELECT) | TINGGI |
| 24 | `PATCH` | `/api/v1/tasks/{taskId}/description` | Sudah Diperbaiki (Terhubung DB/Engine) | `tasks` (SELECT/INSERT/UPDATE), `board_columns` (SELECT) | TINGGI |
| 25 | `GET` | `/api/v1/tasks/{taskId}/attachments` | Sudah Diperbaiki (Terhubung DB/Engine) | `task_attachments` (INSERT/SELECT) | TINGGI |
| 26 | `POST` | `/api/v1/tasks/{taskId}/attachments` | Sudah Diperbaiki (Terhubung DB/Engine) | `task_attachments` (INSERT/SELECT) | TINGGI |
| 27 | `GET` | `/api/v1/intel/competitors` | Sudah Diperbaiki (Terhubung DB/Engine) | `tenants` (SELECT/UPDATE), `work_locations` (SELECT) | TINGGI |
| 28 | `POST` | `/api/v1/intel/competitors` | Sudah Diperbaiki (Terhubung DB/Engine) | `tenants` (SELECT/UPDATE), `work_locations` (SELECT) | TINGGI |
| 29 | `GET` | `/api/v1/intel/competitors/{id}/insights` | Sudah Diperbaiki (Terhubung DB/Engine) | `tenants` (SELECT/UPDATE), `work_locations` (SELECT) | TINGGI |
| 30 | `GET` | `/api/v1/intel/world-trends` | Sudah Diperbaiki (Terhubung DB/Engine) | `tenants` (SELECT/UPDATE), `work_locations` (SELECT) | TINGGI |
| 31 | `POST` | `/api/v1/integrations/{platform}/connect` | Sudah Diperbaiki (Terhubung DB/Engine) | `tenants` (SELECT/UPDATE), `work_locations` (SELECT) | TINGGI |
| 32 | `GET` | `/api/v1/proactive/subscriptions` | Sudah Diperbaiki (Terhubung DB/Engine) | `tenants` (SELECT/UPDATE), `work_locations` (SELECT) | TINGGI |
| 33 | `GET` | `/api/v1/proactive/scope/{staffId}` | Sudah Diperbaiki (Terhubung DB/Engine) | `users` (SELECT/INSERT/UPDATE), `departments` (SELECT) | TINGGI |
| 34 | `POST` | `/api/v1/proactive/subscriptions` | Sudah Diperbaiki (Terhubung DB/Engine) | `tenants` (SELECT/UPDATE), `work_locations` (SELECT) | TINGGI |
| 35 | `GET` | `/api/v1/analytics/scores` | Sudah Diperbaiki (Terhubung DB/Engine) | `tenants` (SELECT/UPDATE), `work_locations` (SELECT) | TINGGI |
| 36 | `GET` | `/api/v1/performance/reports` | Sudah Diperbaiki (Terhubung DB/Engine) | `tenants` (SELECT/UPDATE), `work_locations` (SELECT) | TINGGI |
| 37 | `POST` | `/api/v1/performance/reports` | Sudah Diperbaiki (Terhubung DB/Engine) | `tenants` (SELECT/UPDATE), `work_locations` (SELECT) | TINGGI |
| 38 | `GET` | `/api/v1/performance/goals` | Sudah Diperbaiki (Terhubung DB/Engine) | `tenants` (SELECT/UPDATE), `work_locations` (SELECT) | TINGGI |
| 39 | `GET` | `/api/v1/performance/reviews` | Sudah Diperbaiki (Terhubung DB/Engine) | `tenants` (SELECT/UPDATE), `work_locations` (SELECT) | TINGGI |
| 40 | `GET` | `/api/v1/performance/predictions` | Sudah Diperbaiki (Terhubung DB/Engine) | `tenants` (SELECT/UPDATE), `work_locations` (SELECT) | TINGGI |
| 41 | `GET` | `/api/v1/performance/executive-briefs` | Sudah Diperbaiki (Terhubung DB/Engine) | `tenants` (SELECT/UPDATE), `work_locations` (SELECT) | TINGGI |
| 42 | `GET` | `/api/v1/security/anomalies` | Sudah Diperbaiki (Terhubung DB/Engine) | `tenants` (SELECT/UPDATE), `work_locations` (SELECT) | TINGGI |
| 43 | `GET` | `/api/v1/security/dsr` | Sudah Diperbaiki (Terhubung DB/Engine) | `tenants` (SELECT/UPDATE), `work_locations` (SELECT) | TINGGI |
| 44 | `POST` | `/api/v1/security/dsr` | Sudah Diperbaiki (Terhubung DB/Engine) | `tenants` (SELECT/UPDATE), `work_locations` (SELECT) | TINGGI |
| 45 | `GET` | `/api/v1/attendance/anomalies` | Sudah Diperbaiki (Terhubung DB/Engine) | `tenants` (SELECT/UPDATE), `work_locations` (SELECT) | TINGGI |
| 46 | `POST` | `/api/v1/attendance/anomalies/{anomalyId}/resolve` | Sudah Diperbaiki (Terhubung DB/Engine) | `tenants` (SELECT/UPDATE), `work_locations` (SELECT) | TINGGI |

---

### Domain 8: Orchestration & Autonomous Workflow DAG (14 Endpoints) — Prioritas: TINGGI

| No | Method | Endpoint | Status Saat Ini | Tabel Supabase Tujuan | Prioritas |
|:---:|:---:|---|---|---|:---:|
| 1 | `POST` | `/api/v1/orchestration/dispatch` | Sudah Diperbaiki (Terhubung DB/Engine) | `workflow_executions` (INSERT/UPDATE), `workflow_definitions` (SELECT) | TINGGI |
| 2 | `POST` | `/api/v1/orchestration/workflows/run` | Sudah Diperbaiki (Terhubung DB/Engine) | `workflow_executions` (INSERT/UPDATE), `workflow_definitions` (SELECT) | TINGGI |
| 3 | `GET` | `/api/v1/orchestration/status/{executionId}` | Sudah Diperbaiki (Terhubung DB/Engine) | `workflow_executions` (SELECT) | TINGGI |
| 4 | `GET` | `/api/v1/orchestration/executions/{executionId}` | Sudah Diperbaiki (Terhubung DB/Engine) | `workflow_executions` (SELECT) | TINGGI |
| 5 | `GET` | `/api/v1/orchestration/approvals` | Sudah Diperbaiki (Terhubung DB/Engine) | `pending_approvals` (SELECT/UPDATE), `workflow_executions` (UPDATE) | TINGGI |
| 6 | `POST` | `/api/v1/orchestration/approvals/{id}/decision` | Sudah Diperbaiki (Terhubung DB/Engine) | `pending_approvals` (SELECT/UPDATE), `workflow_executions` (UPDATE) | TINGGI |
| 7 | `GET` | `/api/v1/orchestration/traces` | Sudah Diperbaiki (Terhubung DB/Engine) | `workflow_definitions` (SELECT), `workflow_executions` (SELECT) | TINGGI |
| 8 | `GET` | `/api/v1/orchestration/traces/{executionId}` | Sudah Diperbaiki (Terhubung DB/Engine) | `workflow_definitions` (SELECT), `workflow_executions` (SELECT) | TINGGI |
| 9 | `GET` | `/api/v1/orchestration/traces/by-trace/{traceId}` | Sudah Diperbaiki (Terhubung DB/Engine) | `workflow_definitions` (SELECT), `workflow_executions` (SELECT) | TINGGI |
| 10 | `GET` | `/api/v1/intelligence/confidence/calibration` | Sudah Diperbaiki (Terhubung DB/Engine) | `workflow_definitions` (SELECT), `workflow_executions` (SELECT) | TINGGI |
| 11 | `POST` | `/api/v1/intelligence/confidence/calibrate` | Sudah Diperbaiki (Terhubung DB/Engine) | `workflow_definitions` (SELECT), `workflow_executions` (SELECT) | TINGGI |
| 12 | `GET` | `/api/v1/intelligence/confidence/audit` | Sudah Diperbaiki (Terhubung DB/Engine) | `workflow_definitions` (SELECT), `workflow_executions` (SELECT) | TINGGI |
| 13 | `GET` | `/api/v1/mcp/tools` | Sudah Diperbaiki (Terhubung DB/Engine) | `workflow_definitions` (SELECT), `workflow_executions` (SELECT) | TINGGI |
| 14 | `POST` | `/api/v1/mcp/execute` | Sudah Diperbaiki (Terhubung DB/Engine) | `workflow_definitions` (SELECT), `workflow_executions` (SELECT) | TINGGI |

---

### Domain 9: AI Chat & Brain Knowledge RAG (6 Endpoints) — Prioritas: DASAR / INTI

| No | Method | Endpoint | Status Saat Ini | Tabel Supabase Tujuan | Prioritas |
|:---:|:---:|---|---|---|:---:|
| 1 | `POST` | `/api/v1/chat` | Sudah Diperbaiki (Terhubung DB/Engine) | `chat_messages` (INSERT/SELECT), `chat_sessions` (INSERT/SELECT), `memory_documents` (SELECT) | DASAR / INTI |
| 2 | `POST` | `/api/v1/chat/messages` | Sudah Diperbaiki (Terhubung DB/Engine) | `chat_messages` (INSERT/SELECT), `chat_sessions` (INSERT/SELECT), `memory_documents` (SELECT) | DASAR / INTI |
| 3 | `GET` | `/api/v1/chat/history/{conversationId}` | Sudah Diperbaiki (Terhubung DB/Engine) | `chat_messages` (INSERT/SELECT), `chat_sessions` (INSERT/SELECT), `memory_documents` (SELECT) | DASAR / INTI |
| 4 | `POST` | `/api/v1/agents/{agentId}/chat` | Sudah Diperbaiki (Terhubung DB/Engine) | `chat_messages` (INSERT/SELECT), `chat_sessions` (INSERT/SELECT), `memory_documents` (SELECT) | DASAR / INTI |
| 5 | `POST` | `/api/v1/company-brain/documents` | Sudah Diperbaiki (Terhubung DB/Engine) | `chat_messages` (INSERT/SELECT), `chat_sessions` (INSERT/SELECT), `memory_documents` (SELECT) | DASAR / INTI |
| 6 | `GET` | `/api/v1/company-brain/search` | Sudah Diperbaiki (Terhubung DB/Engine) | `chat_messages` (INSERT/SELECT), `chat_sessions` (INSERT/SELECT), `memory_documents` (SELECT) | DASAR / INTI |

---

### Domain 10: Universal Selection & Autonomous Ranking (22 Endpoints) — Prioritas: TINGGI

| No | Method | Endpoint | Status Saat Ini | Tabel Supabase Tujuan | Prioritas |
|:---:|:---:|---|---|---|:---:|
| 1 | `POST` | `/api/v1/selection/upload` | Sudah Diperbaiki (Terhubung DB/Engine) | `selection_requests` (INSERT), `selection_criteria` (INSERT), `selection_results` (INSERT) | TINGGI |
| 2 | `POST` | `/api/v1/selection/prompt-only` | Sudah Diperbaiki (Terhubung DB/Engine) | `selection_requests` (INSERT), `selection_criteria` (INSERT), `selection_results` (INSERT) | TINGGI |
| 3 | `POST` | `/api/v1/selection/api-database` | Sudah Diperbaiki (Terhubung DB/Engine) | `selection_requests` (SELECT), `selection_results` (SELECT) | TINGGI |
| 4 | `GET` | `/api/v1/selection/requests` | Sudah Diperbaiki (Terhubung DB/Engine) | `selection_requests` (SELECT), `selection_results` (SELECT) | TINGGI |
| 5 | `GET` | `/api/v1/selection/{id}` | Sudah Diperbaiki (Terhubung DB/Engine) | `selection_requests` (SELECT), `selection_results` (SELECT) | TINGGI |
| 6 | `GET` | `/api/v1/selection/{id}/results` | Sudah Diperbaiki (Terhubung DB/Engine) | `selection_requests` (SELECT), `selection_results` (SELECT) | TINGGI |
| 7 | `GET` | `/api/v1/selection/{id}/analytics` | Sudah Diperbaiki (Terhubung DB/Engine) | `selection_results` (SELECT), `selection_requests` (SELECT) | TINGGI |
| 8 | `POST` | `/api/v1/selection/documents/{documentId}/understand` | Sudah Diperbaiki (Terhubung DB/Engine) | `selection_requests` (SELECT), `selection_results` (SELECT) | TINGGI |
| 9 | `GET` | `/api/v1/selection/documents/{documentId}/understanding` | Sudah Diperbaiki (Terhubung DB/Engine) | `selection_requests` (SELECT), `selection_results` (SELECT) | TINGGI |
| 10 | `POST` | `/api/v1/selection/calibration` | Sudah Diperbaiki (Terhubung DB/Engine) | `selection_requests` (SELECT), `selection_results` (SELECT) | TINGGI |
| 11 | `GET` | `/api/v1/selection/calibration` | Sudah Diperbaiki (Terhubung DB/Engine) | `selection_requests` (SELECT), `selection_results` (SELECT) | TINGGI |
| 12 | `GET` | `/api/v1/selection/calibration/{id}` | Sudah Diperbaiki (Terhubung DB/Engine) | `selection_requests` (SELECT), `selection_results` (SELECT) | TINGGI |
| 13 | `POST` | `/api/v1/selection/calibration/{id}/validate-dataset/{documentId}` | Sudah Diperbaiki (Terhubung DB/Engine) | `selection_requests` (SELECT), `selection_results` (SELECT) | TINGGI |
| 14 | `POST` | `/api/v1/selection/results/{id}/review` | Sudah Diperbaiki (Terhubung DB/Engine) | `selection_requests` (SELECT), `selection_results` (SELECT) | TINGGI |
| 15 | `POST` | `/api/v1/selection/results/{id}/execute-downstream` | Sudah Diperbaiki (Terhubung DB/Engine) | `selection_requests` (SELECT), `selection_results` (SELECT) | TINGGI |
| 16 | `GET` | `/api/v1/selection/{id}/export` | Sudah Diperbaiki (Terhubung DB/Engine) | `selection_requests` (SELECT), `selection_results` (SELECT) | TINGGI |
| 17 | `POST` | `/api/v1/selection/{id}/export` | Sudah Diperbaiki (Terhubung DB/Engine) | `selection_requests` (SELECT), `selection_results` (SELECT) | TINGGI |
| 18 | `GET` | `/api/v1/selection/auto-selection/configs` | Sudah Diperbaiki (Terhubung DB/Engine) | `selection_requests` (SELECT), `selection_results` (SELECT) | TINGGI |
| 19 | `POST` | `/api/v1/selection/auto-selection/configs` | Sudah Diperbaiki (Terhubung DB/Engine) | `selection_requests` (SELECT), `selection_results` (SELECT) | TINGGI |
| 20 | `DELETE` | `/api/v1/selection/auto-selection/configs/{id}` | Sudah Diperbaiki (Terhubung DB/Engine) | `selection_requests` (SELECT), `selection_results` (SELECT) | TINGGI |
| 21 | `POST` | `/api/v1/selection/webhook/storage` | Sudah Diperbaiki (Terhubung DB/Engine) | `selection_requests` (SELECT), `selection_results` (SELECT) | TINGGI |
| 22 | `POST` | `/api/v1/selection/webhook/integration-fabric` | Sudah Diperbaiki (Terhubung DB/Engine) | `selection_requests` (SELECT), `selection_results` (SELECT) | TINGGI |

---

### Domain 11: Generative Studio & Brand Asset Management (8 Endpoints) — Prioritas: TINGGI

| No | Method | Endpoint | Status Saat Ini | Tabel Supabase Tujuan | Prioritas |
|:---:|:---:|---|---|---|:---:|
| 1 | `POST` | `/api/v1/studio/generate-image` | Sudah Diperbaiki (Terhubung DB/Engine) | `creative_layout_templates` (SELECT), `brand_asset_overlays` (SELECT) | TINGGI |
| 2 | `POST` | `/api/v1/studio/compose-prompt` | Sudah Diperbaiki (Terhubung DB/Engine) | `creative_layout_templates` (SELECT), `brand_asset_overlays` (SELECT) | TINGGI |
| 3 | `POST` | `/api/v1/studio/campaign-creative` | Sudah Diperbaiki (Terhubung DB/Engine) | `creative_layout_templates` (SELECT), `brand_asset_overlays` (SELECT) | TINGGI |
| 4 | `GET` | `/api/v1/studio/templates` | Sudah Diperbaiki (Terhubung DB/Engine) | `creative_layout_templates` (SELECT) | TINGGI |
| 5 | `POST` | `/api/v1/studio/brand-assets/upload-logo` | Sudah Diperbaiki (Terhubung DB/Engine) | `brand_asset_overlays` (INSERT/SELECT) | TINGGI |
| 6 | `POST` | `/api/v1/studio/brand-assets/logo` | Sudah Diperbaiki (Terhubung DB/Engine) | `brand_asset_overlays` (INSERT/SELECT) | TINGGI |
| 7 | `GET` | `/api/v1/studio/assets` | Sudah Diperbaiki (Terhubung DB/Engine) | `brand_asset_overlays` (INSERT/SELECT) | TINGGI |
| 8 | `POST` | `/api/v1/studio/assets` | Sudah Diperbaiki (Terhubung DB/Engine) | `brand_asset_overlays` (INSERT/SELECT) | TINGGI |

---

### Domain 12: Enterprise Governance, Context Fabric & Chief of Staff (41 Endpoints) — Prioritas: SEDANG

| No | Method | Endpoint | Status Saat Ini | Tabel Supabase Tujuan | Prioritas |
|:---:|:---:|---|---|---|:---:|
| 1 | `GET` | `/api/v1/tenants/{id}/enterprise-connections` | Sudah Diperbaiki (Terhubung DB/Engine) | `enterprise_connections` (SELECT/INSERT) | SEDANG |
| 2 | `POST` | `/api/v1/tenants/{id}/enterprise-connections` | Sudah Diperbaiki (Terhubung DB/Engine) | `enterprise_connections` (SELECT/INSERT) | SEDANG |
| 3 | `GET` | `/api/v1/tenants/{id}/ai-data-permissions` | Sudah Diperbaiki (Terhubung DB/Engine) | `ai_data_permission_policies` (SELECT/INSERT) | SEDANG |
| 4 | `POST` | `/api/v1/tenants/{id}/ai-data-permissions` | Sudah Diperbaiki (Terhubung DB/Engine) | `ai_data_permission_policies` (SELECT/INSERT) | SEDANG |
| 5 | `POST` | `/api/v1/tenants/{id}/ai-data-permissions/check` | Sudah Diperbaiki (Terhubung DB/Engine) | `ai_data_permission_policies` (SELECT/INSERT) | SEDANG |
| 6 | `POST` | `/api/v1/tenants/{id}/tier` | Sudah Diperbaiki (Terhubung DB/Engine) | `context_fabric_snapshots` (SELECT/INSERT) | SEDANG |
| 7 | `POST` | `/api/v1/tenants/{id}/downgrade` | Sudah Diperbaiki (Terhubung DB/Engine) | `context_fabric_snapshots` (SELECT/INSERT) | SEDANG |
| 8 | `GET` | `/api/v1/tenants/{id}/activity-stream` | Sudah Diperbaiki (Terhubung DB/Engine) | `context_fabric_snapshots` (SELECT/INSERT) | SEDANG |
| 9 | `GET` | `/api/v1/tenants/{id}/context-fabric/{entityId}` | Sudah Diperbaiki (Terhubung DB/Engine) | `context_fabric_snapshots` (SELECT/INSERT) | SEDANG |
| 10 | `POST` | `/api/v1/tenants/{id}/management-query` | Sudah Diperbaiki (Terhubung DB/Engine) | `context_fabric_snapshots` (SELECT/INSERT) | SEDANG |
| 11 | `POST` | `/api/v1/tenants/{id}/correlate-signals` | Sudah Diperbaiki (Terhubung DB/Engine) | `context_fabric_snapshots` (SELECT/INSERT) | SEDANG |
| 12 | `GET` | `/api/v1/tenants/{id}/reports/daily` | Sudah Diperbaiki (Terhubung DB/Engine) | `context_fabric_snapshots` (SELECT/INSERT) | SEDANG |
| 13 | `GET` | `/api/v1/tenants/{id}/knowledge-rules` | Sudah Diperbaiki (Terhubung DB/Engine) | `context_fabric_snapshots` (SELECT/INSERT) | SEDANG |
| 14 | `POST` | `/api/v1/tenants/{id}/knowledge-rules` | Sudah Diperbaiki (Terhubung DB/Engine) | `context_fabric_snapshots` (SELECT/INSERT) | SEDANG |
| 15 | `POST` | `/api/v1/tenants/{id}/knowledge-rules/{ruleId}/approve` | Sudah Diperbaiki (Terhubung DB/Engine) | `context_fabric_snapshots` (SELECT/INSERT) | SEDANG |
| 16 | `POST` | `/api/v1/tenants/{id}/knowledge-rules/fuse` | Sudah Diperbaiki (Terhubung DB/Engine) | `context_fabric_snapshots` (SELECT/INSERT) | SEDANG |
| 17 | `GET` | `/api/v1/tenants/{id}/events` | Sudah Diperbaiki (Terhubung DB/Engine) | `ai_events` (SELECT/INSERT) | SEDANG |
| 18 | `POST` | `/api/v1/tenants/{id}/events` | Sudah Diperbaiki (Terhubung DB/Engine) | `ai_events` (SELECT/INSERT) | SEDANG |
| 19 | `GET` | `/api/v1/tenants/{id}/finance/cashflow-pressure` | Sudah Diperbaiki (Terhubung DB/Engine) | `context_fabric_snapshots` (SELECT/INSERT) | SEDANG |
| 20 | `GET` | `/api/v1/tenants/{id}/actions` | Sudah Diperbaiki (Terhubung DB/Engine) | `ai_proposed_actions` (SELECT/INSERT) | SEDANG |
| 21 | `POST` | `/api/v1/tenants/{id}/actions/propose` | Sudah Diperbaiki (Terhubung DB/Engine) | `ai_proposed_actions` (SELECT/INSERT) | SEDANG |
| 22 | `POST` | `/api/v1/tenants/{id}/actions/{actionId}/execute` | Sudah Diperbaiki (Terhubung DB/Engine) | `ai_proposed_actions` (SELECT/INSERT) | SEDANG |
| 23 | `GET` | `/api/v1/tenants/{id}/monitoring-loops` | Sudah Diperbaiki (Terhubung DB/Engine) | `monitoring_loops` (SELECT/INSERT) | SEDANG |
| 24 | `POST` | `/api/v1/tenants/{id}/monitoring-loops` | Sudah Diperbaiki (Terhubung DB/Engine) | `monitoring_loops` (SELECT/INSERT) | SEDANG |
| 25 | `POST` | `/api/v1/tenants/{id}/monitoring-loops/tick` | Sudah Diperbaiki (Terhubung DB/Engine) | `monitoring_loops` (SELECT/INSERT) | SEDANG |
| 26 | `GET` | `/api/v1/tenants/{id}/swarm/status` | Sudah Diperbaiki (Terhubung DB/Engine) | `context_fabric_snapshots` (SELECT/INSERT) | SEDANG |
| 27 | `POST` | `/api/v1/tenants/{id}/swarm/freeze` | Sudah Diperbaiki (Terhubung DB/Engine) | `context_fabric_snapshots` (SELECT/INSERT) | SEDANG |
| 28 | `POST` | `/api/v1/tenants/{id}/swarm/resume` | Sudah Diperbaiki (Terhubung DB/Engine) | `context_fabric_snapshots` (SELECT/INSERT) | SEDANG |
| 29 | `GET` | `/api/v1/tenants/{id}/chief-of-staff/briefings` | Sudah Diperbaiki (Terhubung DB/Engine) | `chief_of_staff_briefings` (SELECT/INSERT) | SEDANG |
| 30 | `POST` | `/api/v1/tenants/{id}/chief-of-staff/synthesize` | Sudah Diperbaiki (Terhubung DB/Engine) | `context_fabric_snapshots` (SELECT/INSERT) | SEDANG |
| 31 | `POST` | `/api/v1/tenants/{id}/chief-of-staff/research-directives` | Sudah Diperbaiki (Terhubung DB/Engine) | `context_fabric_snapshots` (SELECT/INSERT) | SEDANG |
| 32 | `GET` | `/api/v1/tenants/{id}/agents/{agentId}/skill-confidence` | Sudah Diperbaiki (Terhubung DB/Engine) | `agent_skill_confidence` (SELECT/UPDATE) | SEDANG |
| 33 | `GET` | `/api/v1/tenants/{id}/data-quality-issues` | Sudah Diperbaiki (Terhubung DB/Engine) | `data_quality_issues` (SELECT/UPDATE) | SEDANG |
| 34 | `POST` | `/api/v1/tenants/{id}/chief-of-staff/briefings/generate` | Sudah Diperbaiki (Terhubung DB/Engine) | `chief_of_staff_briefings` (SELECT/INSERT) | SEDANG |
| 35 | `GET` | `/api/v1/tenants/{id}/chief-of-staff/research-directives` | Sudah Diperbaiki (Terhubung DB/Engine) | `context_fabric_snapshots` (SELECT/INSERT) | SEDANG |
| 36 | `POST` | `/api/v1/tenants/{id}/data-quality-issues/resolve` | Sudah Diperbaiki (Terhubung DB/Engine) | `data_quality_issues` (SELECT/UPDATE) | SEDANG |
| 37 | `GET` | `/api/v1/tenants/{id}/project-health/{projectId}` | Sudah Diperbaiki (Terhubung DB/Engine) | `context_fabric_snapshots` (SELECT/INSERT) | SEDANG |
| 38 | `POST` | `/api/v1/tenants/{id}/project-health/evaluate` | Sudah Diperbaiki (Terhubung DB/Engine) | `context_fabric_snapshots` (SELECT/INSERT) | SEDANG |
| 39 | `POST` | `/api/v1/tenants/{id}/multi-agent-collaborations/initiate` | Sudah Diperbaiki (Terhubung DB/Engine) | `context_fabric_snapshots` (SELECT/INSERT) | SEDANG |
| 40 | `GET` | `/api/v1/tenants/{id}/multi-agent-collaborations` | Sudah Diperbaiki (Terhubung DB/Engine) | `context_fabric_snapshots` (SELECT/INSERT) | SEDANG |
| 41 | `GET` | `/api/v1/tenants/{id}/explainability/{executionId}` | Sudah Diperbaiki (Terhubung DB/Engine) | `context_fabric_snapshots` (SELECT/INSERT) | SEDANG |

---

### Domain 13: Autonomous Memory Consolidation & Decay (6 Endpoints) — Prioritas: SEDANG

| No | Method | Endpoint | Status Saat Ini | Tabel Supabase Tujuan | Prioritas |
|:---:|:---:|---|---|---|:---:|
| 1 | `POST` | `/api/v1/memory/consolidate/evaluate` | Sudah Diperbaiki (Terhubung DB/Engine) | `memory_documents` (INSERT), `memory_consolidation_logs` (INSERT) | SEDANG |
| 2 | `POST` | `/api/v1/memory/consolidate/batch` | Sudah Diperbaiki (Terhubung DB/Engine) | `memory_documents` (INSERT), `memory_consolidation_logs` (INSERT) | SEDANG |
| 3 | `POST` | `/api/v1/memory/decay` | Sudah Diperbaiki (Terhubung DB/Engine) | `memory_documents` (UPDATE) | SEDANG |
| 4 | `POST` | `/api/v1/memory/search` | Sudah Diperbaiki (Terhubung DB/Engine) | `memory_documents` (SELECT) | SEDANG |
| 5 | `POST` | `/api/v1/memory/search/ab-compare` | Sudah Diperbaiki (Terhubung DB/Engine) | `memory_documents` (SELECT) | SEDANG |
| 6 | `GET` | `/api/v1/memory/documents` | Sudah Diperbaiki (Terhubung DB/Engine) | `memory_documents` (SELECT/UPDATE) | SEDANG |

---

### Domain 14: Omnichannel Sales, CRM & Channel Gateway (28 Endpoints) — Prioritas: TINGGI

| No | Method | Endpoint | Status Saat Ini | Tabel Supabase Tujuan | Prioritas |
|:---:|:---:|---|---|---|:---:|
| 1 | `GET` | `/api/v1/tenants/{id}/channel-accounts` | Sudah Diperbaiki (Terhubung DB/Engine) | `channel_accounts` (SELECT), `leads` (SELECT) | TINGGI |
| 2 | `POST` | `/api/v1/tenants/{id}/channel-accounts` | Sudah Diperbaiki (Terhubung DB/Engine) | `channel_accounts` (SELECT), `leads` (SELECT) | TINGGI |
| 3 | `POST` | `/api/v1/tenants/{id}/channel-accounts/{caId}/verify` | Sudah Diperbaiki (Terhubung DB/Engine) | `channel_accounts` (SELECT), `leads` (SELECT) | TINGGI |
| 4 | `PATCH` | `/api/v1/tenants/{id}/channel-accounts/{caId}/approve` | Sudah Diperbaiki (Terhubung DB/Engine) | `channel_accounts` (SELECT), `leads` (SELECT) | TINGGI |
| 5 | `GET` | `/api/v1/tenants/{id}/channel-accounts/{caId}/health` | Sudah Diperbaiki (Terhubung DB/Engine) | `channel_accounts` (SELECT), `leads` (SELECT) | TINGGI |
| 6 | `GET` | `/api/v1/tenants/{id}/credit-wallet` | Sudah Diperbaiki (Terhubung DB/Engine) | `channel_accounts` (SELECT), `leads` (SELECT) | TINGGI |
| 7 | `GET` | `/api/v1/tenants/{id}/customers/search` | Sudah Diperbaiki (Terhubung DB/Engine) | `channel_accounts` (SELECT), `leads` (SELECT) | TINGGI |
| 8 | `POST` | `/api/v1/tenants/{id}/customers/resolve` | Sudah Diperbaiki (Terhubung DB/Engine) | `channel_accounts` (SELECT), `leads` (SELECT) | TINGGI |
| 9 | `POST` | `/api/v1/tenants/{id}/customers/{cust_id}/merge` | Sudah Diperbaiki (Terhubung DB/Engine) | `channel_accounts` (SELECT), `leads` (SELECT) | TINGGI |
| 10 | `GET` | `/api/v1/tenants/{id}/inbox/conversations` | Sudah Diperbaiki (Terhubung DB/Engine) | `channel_accounts` (SELECT), `leads` (SELECT) | TINGGI |
| 11 | `GET` | `/api/v1/tenants/{id}/leads` | Sudah Diperbaiki (Terhubung DB/Engine) | `leads` (SELECT/INSERT/UPDATE) | TINGGI |
| 12 | `POST` | `/api/v1/tenants/{id}/leads` | Sudah Diperbaiki (Terhubung DB/Engine) | `leads` (SELECT/INSERT/UPDATE) | TINGGI |
| 13 | `GET` | `/api/v1/tenants/{id}/products` | Sudah Diperbaiki (Terhubung DB/Engine) | `channel_accounts` (SELECT), `leads` (SELECT) | TINGGI |
| 14 | `POST` | `/api/v1/tenants/{id}/products` | Sudah Diperbaiki (Terhubung DB/Engine) | `channel_accounts` (SELECT), `leads` (SELECT) | TINGGI |
| 15 | `GET` | `/api/v1/tenants/{id}/inventory` | Sudah Diperbaiki (Terhubung DB/Engine) | `channel_accounts` (SELECT), `leads` (SELECT) | TINGGI |
| 16 | `GET` | `/api/v1/tenants/{id}/inventory/{variantId}` | Sudah Diperbaiki (Terhubung DB/Engine) | `channel_accounts` (SELECT), `leads` (SELECT) | TINGGI |
| 17 | `GET` | `/api/v1/tenants/{id}/orders` | Sudah Diperbaiki (Terhubung DB/Engine) | `orders` (SELECT/INSERT/UPDATE), `order_items` (SELECT/INSERT) | TINGGI |
| 18 | `GET` | `/api/v1/tenants/{id}/campaigns` | Sudah Diperbaiki (Terhubung DB/Engine) | `channel_accounts` (SELECT), `leads` (SELECT) | TINGGI |
| 19 | `POST` | `/api/v1/tenants/{id}/campaigns` | Sudah Diperbaiki (Terhubung DB/Engine) | `channel_accounts` (SELECT), `leads` (SELECT) | TINGGI |
| 20 | `GET` | `/api/v1/tenants/{id}/analytics/revenue-intelligence` | Sudah Diperbaiki (Terhubung DB/Engine) | `orders` (SELECT), `leads` (SELECT), `order_items` (SELECT) | TINGGI |
| 21 | `GET` | `/api/v1/tenants/{id}/analytics/sales-coach` | Sudah Diperbaiki (Terhubung DB/Engine) | `channel_accounts` (SELECT), `leads` (SELECT) | TINGGI |
| 22 | `POST` | `/api/v1/tenants/{id}/experiments` | Sudah Diperbaiki (Terhubung DB/Engine) | `channel_accounts` (SELECT), `leads` (SELECT) | TINGGI |
| 23 | `GET` | `/api/v1/tenants/{id}/service-requests` | Sudah Diperbaiki (Terhubung DB/Engine) | `channel_accounts` (SELECT), `leads` (SELECT) | TINGGI |
| 24 | `POST` | `/api/v1/tenants/{id}/service-requests` | Sudah Diperbaiki (Terhubung DB/Engine) | `channel_accounts` (SELECT), `leads` (SELECT) | TINGGI |
| 25 | `PATCH` | `/api/v1/tenants/{id}/ai-agents/{agentId}/persona` | Sudah Diperbaiki (Terhubung DB/Engine) | `channel_accounts` (SELECT), `leads` (SELECT) | TINGGI |
| 26 | `POST` | `/api/v1/tenants/{id}/conversations/{convId}/persona-reply` | Sudah Diperbaiki (Terhubung DB/Engine) | `channel_accounts` (SELECT), `leads` (SELECT) | TINGGI |
| 27 | `POST` | `/api/v1/conversations/{id}/takeover` | Sudah Diperbaiki (Terhubung DB/Engine) | `channel_accounts` (SELECT), `leads` (SELECT) | TINGGI |
| 28 | `POST` | `/api/v1/carts/{id}/checkout` | Sudah Diperbaiki (Terhubung DB/Engine) | `channel_accounts` (SELECT), `leads` (SELECT) | TINGGI |

---

### Domain 15: Commercial Billing, Quota & Dunning (30 Endpoints) — Prioritas: TINGGI

| No | Method | Endpoint | Status Saat Ini | Tabel Supabase Tujuan | Prioritas |
|:---:|:---:|---|---|---|:---:|
| 1 | `GET` | `/api/v1/plans` | Sudah Diperbaiki (Terhubung DB/Engine) | `commercial_plans` (SELECT) | TINGGI |
| 2 | `GET` | `/api/v1/plans/{id}` | Sudah Diperbaiki (Terhubung DB/Engine) | `commercial_plans` (SELECT) | TINGGI |
| 3 | `GET` | `/api/v1/plans/entitlements-matrix` | Sudah Diperbaiki (Terhubung DB/Engine) | `commercial_plans` (SELECT) | TINGGI |
| 4 | `GET` | `/api/v1/tenant/entitlements` | Sudah Diperbaiki (Terhubung DB/Engine) | `tenant_subscriptions` (SELECT), `commercial_plans` (SELECT) | TINGGI |
| 5 | `GET` | `/api/v1/billing/plans` | Sudah Diperbaiki (Terhubung DB/Engine) | `commercial_plans` (SELECT) | TINGGI |
| 6 | `GET` | `/api/v1/billing/plans/{id}` | Sudah Diperbaiki (Terhubung DB/Engine) | `commercial_plans` (SELECT) | TINGGI |
| 7 | `POST` | `/api/v1/billing/calculate-cost` | Sudah Diperbaiki (Terhubung DB/Engine) | `tenant_subscriptions` (SELECT/UPDATE) | TINGGI |
| 8 | `GET` | `/api/v1/billing/subscription` | Sudah Diperbaiki (Terhubung DB/Engine) | `tenant_subscriptions` (SELECT/UPDATE) | TINGGI |
| 9 | `POST` | `/api/v1/billing/subscription` | Sudah Diperbaiki (Terhubung DB/Engine) | `tenant_subscriptions` (SELECT/UPDATE) | TINGGI |
| 10 | `POST` | `/api/v1/billing/subscription/upgrade` | Sudah Diperbaiki (Terhubung DB/Engine) | `tenant_subscriptions` (SELECT/UPDATE) | TINGGI |
| 11 | `POST` | `/api/v1/billing/subscription/downgrade` | Sudah Diperbaiki (Terhubung DB/Engine) | `tenant_subscriptions` (SELECT/UPDATE) | TINGGI |
| 12 | `POST` | `/api/v1/billing/subscription/cancel` | Sudah Diperbaiki (Terhubung DB/Engine) | `tenant_subscriptions` (SELECT/UPDATE) | TINGGI |
| 13 | `GET` | `/api/v1/billing/credits` | Sudah Diperbaiki (Terhubung DB/Engine) | `central_credit_ledger` (SELECT/INSERT), `tenant_subscriptions` (SELECT) | TINGGI |
| 14 | `GET` | `/api/v1/billing/credits/summary` | Sudah Diperbaiki (Terhubung DB/Engine) | `central_credit_ledger` (SELECT/INSERT), `tenant_subscriptions` (SELECT) | TINGGI |
| 15 | `GET` | `/api/v1/billing/credits/ledger` | Sudah Diperbaiki (Terhubung DB/Engine) | `central_credit_ledger` (SELECT/INSERT), `tenant_subscriptions` (SELECT) | TINGGI |
| 16 | `GET` | `/api/v1/billing/ledger` | Sudah Diperbaiki (Terhubung DB/Engine) | `central_credit_ledger` (SELECT/INSERT), `tenant_subscriptions` (SELECT) | TINGGI |
| 17 | `POST` | `/api/v1/billing/credits/topup` | Sudah Diperbaiki (Terhubung DB/Engine) | `central_credit_ledger` (SELECT/INSERT), `tenant_subscriptions` (SELECT) | TINGGI |
| 18 | `GET` | `/api/v1/billing/seats` | Sudah Diperbaiki (Terhubung DB/Engine) | `tenant_subscriptions` (SELECT/UPDATE) | TINGGI |
| 19 | `POST` | `/api/v1/billing/seats` | Sudah Diperbaiki (Terhubung DB/Engine) | `tenant_subscriptions` (SELECT/UPDATE) | TINGGI |
| 20 | `DELETE` | `/api/v1/billing/seats/{id}` | Sudah Diperbaiki (Terhubung DB/Engine) | `tenant_subscriptions` (SELECT/UPDATE) | TINGGI |
| 21 | `GET` | `/api/v1/billing/agents` | Sudah Diperbaiki (Terhubung DB/Engine) | `tenant_subscriptions` (SELECT/UPDATE) | TINGGI |
| 22 | `POST` | `/api/v1/billing/agents` | Sudah Diperbaiki (Terhubung DB/Engine) | `tenant_subscriptions` (SELECT/UPDATE) | TINGGI |
| 23 | `DELETE` | `/api/v1/billing/agents/{id}` | Sudah Diperbaiki (Terhubung DB/Engine) | `tenant_subscriptions` (SELECT/UPDATE) | TINGGI |
| 24 | `GET` | `/api/v1/billing/invoices` | Sudah Diperbaiki (Terhubung DB/Engine) | `invoices` (SELECT/INSERT), `dunning_logs` (SELECT/INSERT) | TINGGI |
| 25 | `GET` | `/api/v1/billing/invoices/{id}` | Sudah Diperbaiki (Terhubung DB/Engine) | `invoices` (SELECT/INSERT), `dunning_logs` (SELECT/INSERT) | TINGGI |
| 26 | `POST` | `/api/v1/billing/invoices/{id}/fail-dunning` | Sudah Diperbaiki (Terhubung DB/Engine) | `invoices` (SELECT/INSERT), `dunning_logs` (SELECT/INSERT) | TINGGI |
| 27 | `POST` | `/api/v1/billing/payment` | Sudah Diperbaiki (Terhubung DB/Engine) | `tenant_subscriptions` (SELECT/UPDATE) | TINGGI |
| 28 | `POST` | `/api/v1/billing/payment/webhook` | Sudah Diperbaiki (Terhubung DB/Engine) | `tenant_subscriptions` (SELECT/UPDATE) | TINGGI |
| 29 | `POST` | `/api/v1/billing/webhook/midtrans` | Sudah Diperbaiki (Terhubung DB/Engine) | `tenant_subscriptions` (SELECT/UPDATE) | TINGGI |
| 30 | `GET` | `/api/v1/billing/upgrade-recommendation` | Sudah Diperbaiki (Terhubung DB/Engine) | `tenant_subscriptions` (SELECT/UPDATE) | TINGGI |

---

### Domain 16: Super Admin, Operations & Security Command (92 Endpoints) — Prioritas: SEDANG / RENDAH

| No | Method | Endpoint | Status Saat Ini | Tabel Supabase Tujuan | Prioritas |
|:---:|:---:|---|---|---|:---:|
| 1 | `GET` | `/api/v1/admin/presence/security-stats` | Sudah Diperbaiki (Terhubung DB/Engine) | `audit_logs` (SELECT), `tenants` (SELECT) | SEDANG / RENDAH |
| 2 | `GET` | `/api/v1/admin/tenants` | Sudah Diperbaiki (Terhubung DB/Engine) | `audit_logs` (SELECT), `tenants` (SELECT) | SEDANG / RENDAH |
| 3 | `POST` | `/api/v1/admin/tenants` | Sudah Diperbaiki (Terhubung DB/Engine) | `audit_logs` (SELECT), `tenants` (SELECT) | SEDANG / RENDAH |
| 4 | `GET` | `/api/v1/admin/llm-providers` | Sudah Diperbaiki (Terhubung DB/Engine) | `llm_providers` (SELECT/INSERT/UPDATE), `llm_provider_models` (SELECT/INSERT) | SEDANG / RENDAH |
| 5 | `POST` | `/api/v1/admin/llm-providers` | Sudah Diperbaiki (Terhubung DB/Engine) | `llm_providers` (SELECT/INSERT/UPDATE), `llm_provider_models` (SELECT/INSERT) | SEDANG / RENDAH |
| 6 | `PUT` | `/api/v1/admin/llm-providers/{id}` | Sudah Diperbaiki (Terhubung DB/Engine) | `llm_providers` (SELECT/INSERT/UPDATE), `llm_provider_models` (SELECT/INSERT) | SEDANG / RENDAH |
| 7 | `DELETE` | `/api/v1/admin/llm-providers/{id}` | Sudah Diperbaiki (Terhubung DB/Engine) | `llm_providers` (SELECT/INSERT/UPDATE), `llm_provider_models` (SELECT/INSERT) | SEDANG / RENDAH |
| 8 | `POST` | `/api/v1/admin/llm-providers/{id}/toggle-status` | Sudah Diperbaiki (Terhubung DB/Engine) | `llm_providers` (SELECT/INSERT/UPDATE), `llm_provider_models` (SELECT/INSERT) | SEDANG / RENDAH |
| 9 | `GET` | `/api/v1/admin/image-providers` | Sudah Diperbaiki (Terhubung DB/Engine) | `llm_providers` (SELECT/INSERT/UPDATE), `llm_provider_models` (SELECT/INSERT) | SEDANG / RENDAH |
| 10 | `POST` | `/api/v1/admin/image-providers` | Sudah Diperbaiki (Terhubung DB/Engine) | `llm_providers` (SELECT/INSERT/UPDATE), `llm_provider_models` (SELECT/INSERT) | SEDANG / RENDAH |
| 11 | `DELETE` | `/api/v1/admin/image-providers/{id}` | Sudah Diperbaiki (Terhubung DB/Engine) | `llm_providers` (SELECT/INSERT/UPDATE), `llm_provider_models` (SELECT/INSERT) | SEDANG / RENDAH |
| 12 | `GET` | `/api/v1/admin/master-data/categories` | Sudah Diperbaiki (Terhubung DB/Engine) | `master_data_catalog` (SELECT/INSERT/UPDATE) | SEDANG / RENDAH |
| 13 | `GET` | `/api/v1/admin/master-data` | Sudah Diperbaiki (Terhubung DB/Engine) | `master_data_catalog` (SELECT/INSERT/UPDATE) | SEDANG / RENDAH |
| 14 | `POST` | `/api/v1/admin/master-data` | Sudah Diperbaiki (Terhubung DB/Engine) | `master_data_catalog` (SELECT/INSERT/UPDATE) | SEDANG / RENDAH |
| 15 | `DELETE` | `/api/v1/admin/master-data/{id}` | Sudah Diperbaiki (Terhubung DB/Engine) | `master_data_catalog` (SELECT/INSERT/UPDATE) | SEDANG / RENDAH |
| 16 | `DELETE` | `/api/v1/admin/master-data/{category}/{id}` | Sudah Diperbaiki (Terhubung DB/Engine) | `master_data_catalog` (SELECT/INSERT/UPDATE) | SEDANG / RENDAH |
| 17 | `GET` | `/api/v1/admin/skill-plugins` | Sudah Diperbaiki (Terhubung DB/Engine) | `mcp_tools` (SELECT/INSERT/UPDATE), `skill_plugins` (SELECT/INSERT) | SEDANG / RENDAH |
| 18 | `POST` | `/api/v1/admin/skill-plugins` | Sudah Diperbaiki (Terhubung DB/Engine) | `mcp_tools` (SELECT/INSERT/UPDATE), `skill_plugins` (SELECT/INSERT) | SEDANG / RENDAH |
| 19 | `PUT` | `/api/v1/admin/skill-plugins/{id}` | Sudah Diperbaiki (Terhubung DB/Engine) | `mcp_tools` (SELECT/INSERT/UPDATE), `skill_plugins` (SELECT/INSERT) | SEDANG / RENDAH |
| 20 | `DELETE` | `/api/v1/admin/skill-plugins/{id}` | Sudah Diperbaiki (Terhubung DB/Engine) | `mcp_tools` (SELECT/INSERT/UPDATE), `skill_plugins` (SELECT/INSERT) | SEDANG / RENDAH |
| 21 | `PATCH` | `/api/v1/admin/skill-plugins/{id}/status` | Sudah Diperbaiki (Terhubung DB/Engine) | `mcp_tools` (SELECT/INSERT/UPDATE), `skill_plugins` (SELECT/INSERT) | SEDANG / RENDAH |
| 22 | `POST` | `/api/v1/admin/skill-plugins/upload` | Sudah Diperbaiki (Terhubung DB/Engine) | `mcp_tools` (SELECT/INSERT/UPDATE), `skill_plugins` (SELECT/INSERT) | SEDANG / RENDAH |
| 23 | `GET` | `/api/v1/admin/mcp-tools` | Sudah Diperbaiki (Terhubung DB/Engine) | `mcp_tools` (SELECT/INSERT/UPDATE), `skill_plugins` (SELECT/INSERT) | SEDANG / RENDAH |
| 24 | `POST` | `/api/v1/admin/mcp-tools` | Sudah Diperbaiki (Terhubung DB/Engine) | `mcp_tools` (SELECT/INSERT/UPDATE), `skill_plugins` (SELECT/INSERT) | SEDANG / RENDAH |
| 25 | `PUT` | `/api/v1/admin/mcp-tools/{id}` | Sudah Diperbaiki (Terhubung DB/Engine) | `mcp_tools` (SELECT/INSERT/UPDATE), `skill_plugins` (SELECT/INSERT) | SEDANG / RENDAH |
| 26 | `DELETE` | `/api/v1/admin/mcp-tools/{id}` | Sudah Diperbaiki (Terhubung DB/Engine) | `mcp_tools` (SELECT/INSERT/UPDATE), `skill_plugins` (SELECT/INSERT) | SEDANG / RENDAH |
| 27 | `PATCH` | `/api/v1/admin/mcp-tools/{id}/kill-switch` | Sudah Diperbaiki (Terhubung DB/Engine) | `mcp_tools` (SELECT/INSERT/UPDATE), `skill_plugins` (SELECT/INSERT) | SEDANG / RENDAH |
| 28 | `GET` | `/api/v1/admin/app-registry` | Sudah Diperbaiki (Terhubung DB/Engine) | `audit_logs` (SELECT), `tenants` (SELECT) | SEDANG / RENDAH |
| 29 | `POST` | `/api/v1/admin/app-registry` | Sudah Diperbaiki (Terhubung DB/Engine) | `audit_logs` (SELECT), `tenants` (SELECT) | SEDANG / RENDAH |
| 30 | `PUT` | `/api/v1/admin/app-registry/{id}` | Sudah Diperbaiki (Terhubung DB/Engine) | `audit_logs` (SELECT), `tenants` (SELECT) | SEDANG / RENDAH |
| 31 | `DELETE` | `/api/v1/admin/app-registry/{id}` | Sudah Diperbaiki (Terhubung DB/Engine) | `audit_logs` (SELECT), `tenants` (SELECT) | SEDANG / RENDAH |
| 32 | `PATCH` | `/api/v1/admin/app-registry/{id}/mark-migration` | Sudah Diperbaiki (Terhubung DB/Engine) | `audit_logs` (SELECT), `tenants` (SELECT) | SEDANG / RENDAH |
| 33 | `GET` | `/api/v1/admin/analytics/tenant-workforce-summary` | Sudah Diperbaiki (Terhubung DB/Engine) | `llm_usage_logs` (SELECT), `central_credit_ledger` (SELECT), `tasks` (SELECT) | SEDANG / RENDAH |
| 34 | `GET` | `/api/v1/admin/monitoring/system-overview` | Sudah Diperbaiki (Terhubung DB/Engine) | Stateless / Live JVM & OS System Metrics, `dead_letter_queue` (SELECT) | SEDANG / RENDAH |
| 35 | `POST` | `/api/v1/admin/swarm/freeze` | Sudah Diperbaiki (Terhubung DB/Engine) | `audit_logs` (SELECT), `tenants` (SELECT) | SEDANG / RENDAH |
| 36 | `POST` | `/api/v1/admin/swarm/resume` | Sudah Diperbaiki (Terhubung DB/Engine) | `audit_logs` (SELECT), `tenants` (SELECT) | SEDANG / RENDAH |
| 37 | `GET` | `/api/v1/admin/swarm/status` | Sudah Diperbaiki (Terhubung DB/Engine) | `audit_logs` (SELECT), `tenants` (SELECT) | SEDANG / RENDAH |
| 38 | `GET` | `/api/v1/admin/audit-logs` | Sudah Diperbaiki (Terhubung DB/Engine) | `audit_logs` (SELECT) | SEDANG / RENDAH |
| 39 | `GET` | `/api/v1/admin/usage` | Sudah Diperbaiki (Terhubung DB/Engine) | `llm_usage_logs` (SELECT), `central_credit_ledger` (SELECT), `tasks` (SELECT) | SEDANG / RENDAH |
| 40 | `GET` | `/api/v1/admin/llm-usage` | Sudah Diperbaiki (Terhubung DB/Engine) | `llm_usage_logs` (SELECT), `central_credit_ledger` (SELECT), `tasks` (SELECT) | SEDANG / RENDAH |
| 41 | `GET` | `/api/v1/admin/health-check` | Sudah Diperbaiki (Terhubung DB/Engine) | `audit_logs` (SELECT), `tenants` (SELECT) | SEDANG / RENDAH |
| 42 | `POST` | `/api/v1/admin/jobs/trigger` | Sudah Diperbaiki (Terhubung DB/Engine) | `audit_logs` (SELECT), `tenants` (SELECT) | SEDANG / RENDAH |
| 43 | `GET` | `/api/v1/admin/billing/subscriptions` | Sudah Diperbaiki (Terhubung DB/Engine) | `audit_logs` (SELECT), `tenants` (SELECT) | SEDANG / RENDAH |
| 44 | `GET` | `/api/v1/admin/billing/invoices` | Sudah Diperbaiki (Terhubung DB/Engine) | `audit_logs` (SELECT), `tenants` (SELECT) | SEDANG / RENDAH |
| 45 | `GET` | `/api/v1/admin/specialist-agents` | Sudah Diperbaiki (Terhubung DB/Engine) | `audit_logs` (SELECT), `tenants` (SELECT) | SEDANG / RENDAH |
| 46 | `GET` | `/api/v1/admin/studio/templates` | Sudah Diperbaiki (Terhubung DB/Engine) | `audit_logs` (SELECT), `tenants` (SELECT) | SEDANG / RENDAH |
| 47 | `GET` | `/api/v1/admin/dead-letter-queue` | Sudah Diperbaiki (Terhubung DB/Engine) | `dead_letter_queue` (SELECT/UPDATE) | SEDANG / RENDAH |
| 48 | `POST` | `/api/v1/admin/dead-letter-queue/{id}/reprocess` | Sudah Diperbaiki (Terhubung DB/Engine) | `dead_letter_queue` (SELECT/UPDATE) | SEDANG / RENDAH |
| 49 | `POST` | `/api/v1/admin/workflow-executions/{id}/replay` | Sudah Diperbaiki (Terhubung DB/Engine) | `workflow_executions` (SELECT/UPDATE) | SEDANG / RENDAH |
| 50 | `GET` | `/api/v1/admin/workflow-executions` | Sudah Diperbaiki (Terhubung DB/Engine) | `workflow_executions` (SELECT/UPDATE) | SEDANG / RENDAH |
| 51 | `GET` | `/api/v1/admin/analytics/overview` | Sudah Diperbaiki (Terhubung DB/Engine) | `llm_usage_logs` (SELECT), `central_credit_ledger` (SELECT), `tasks` (SELECT) | SEDANG / RENDAH |
| 52 | `GET` | `/api/v1/admin/analytics/usage-credit` | Sudah Diperbaiki (Terhubung DB/Engine) | `llm_usage_logs` (SELECT), `central_credit_ledger` (SELECT), `tasks` (SELECT) | SEDANG / RENDAH |
| 53 | `GET` | `/api/v1/admin/analytics/llm-usage-platform-wide` | Sudah Diperbaiki (Terhubung DB/Engine) | `llm_usage_logs` (SELECT), `central_credit_ledger` (SELECT), `tasks` (SELECT) | SEDANG / RENDAH |
| 54 | `GET` | `/api/v1/admin/analytics/kpi-summary` | Sudah Diperbaiki (Terhubung DB/Engine) | `llm_usage_logs` (SELECT), `central_credit_ledger` (SELECT), `tasks` (SELECT) | SEDANG / RENDAH |
| 55 | `GET` | `/api/v1/admin/analytics/daily-task-performance` | Sudah Diperbaiki (Terhubung DB/Engine) | `llm_usage_logs` (SELECT), `central_credit_ledger` (SELECT), `tasks` (SELECT) | SEDANG / RENDAH |
| 56 | `GET` | `/api/v1/admin/analytics/task-activity-summary` | Sudah Diperbaiki (Terhubung DB/Engine) | `llm_usage_logs` (SELECT), `central_credit_ledger` (SELECT), `tasks` (SELECT) | SEDANG / RENDAH |
| 57 | `GET` | `/api/v1/admin/analytics/universal-selection-usage` | Sudah Diperbaiki (Terhubung DB/Engine) | `llm_usage_logs` (SELECT), `central_credit_ledger` (SELECT), `tasks` (SELECT) | SEDANG / RENDAH |
| 58 | `POST` | `/api/v1/admin/analytics/transactions` | Sudah Diperbaiki (Terhubung DB/Engine) | `llm_usage_logs` (SELECT), `central_credit_ledger` (SELECT), `tasks` (SELECT) | SEDANG / RENDAH |
| 59 | `GET` | `/api/v1/admin/payment-reconciliation/orders` | Sudah Diperbaiki (Terhubung DB/Engine) | `audit_logs` (SELECT), `tenants` (SELECT) | SEDANG / RENDAH |
| 60 | `GET` | `/api/v1/admin/payment-reconciliation/queue` | Sudah Diperbaiki (Terhubung DB/Engine) | `audit_logs` (SELECT), `tenants` (SELECT) | SEDANG / RENDAH |
| 61 | `POST` | `/api/v1/admin/payment-reconciliation/{id}/confirm` | Sudah Diperbaiki (Terhubung DB/Engine) | `audit_logs` (SELECT), `tenants` (SELECT) | SEDANG / RENDAH |
| 62 | `POST` | `/api/v1/admin/payment-reconciliation/{id}/reject` | Sudah Diperbaiki (Terhubung DB/Engine) | `audit_logs` (SELECT), `tenants` (SELECT) | SEDANG / RENDAH |
| 63 | `POST` | `/api/v1/admin/payment-reconciliation/trigger-check` | Sudah Diperbaiki (Terhubung DB/Engine) | `audit_logs` (SELECT), `tenants` (SELECT) | SEDANG / RENDAH |
| 64 | `POST` | `/api/v1/admin/payment-reconciliation/simulate-stuck` | Sudah Diperbaiki (Terhubung DB/Engine) | `audit_logs` (SELECT), `tenants` (SELECT) | SEDANG / RENDAH |
| 65 | `GET` | `/api/v1/admin/commercial/plans` | Sudah Diperbaiki (Terhubung DB/Engine) | `commercial_plans` (SELECT/INSERT/UPDATE), `tenant_subscriptions` (SELECT/UPDATE) | SEDANG / RENDAH |
| 66 | `POST` | `/api/v1/admin/commercial/plans` | Sudah Diperbaiki (Terhubung DB/Engine) | `commercial_plans` (SELECT/INSERT/UPDATE), `tenant_subscriptions` (SELECT/UPDATE) | SEDANG / RENDAH |
| 67 | `DELETE` | `/api/v1/admin/commercial/plans/{id}` | Sudah Diperbaiki (Terhubung DB/Engine) | `commercial_plans` (SELECT/INSERT/UPDATE), `tenant_subscriptions` (SELECT/UPDATE) | SEDANG / RENDAH |
| 68 | `GET` | `/api/v1/admin/commercial/entitlements-matrix` | Sudah Diperbaiki (Terhubung DB/Engine) | `commercial_plans` (SELECT/INSERT/UPDATE), `tenant_subscriptions` (SELECT/UPDATE) | SEDANG / RENDAH |
| 69 | `POST` | `/api/v1/admin/commercial/entitlements` | Sudah Diperbaiki (Terhubung DB/Engine) | `commercial_plans` (SELECT/INSERT/UPDATE), `tenant_subscriptions` (SELECT/UPDATE) | SEDANG / RENDAH |
| 70 | `GET` | `/api/v1/admin/commercial/custom-override/{tenantId}` | Sudah Diperbaiki (Terhubung DB/Engine) | `commercial_plans` (SELECT/INSERT/UPDATE), `tenant_subscriptions` (SELECT/UPDATE) | SEDANG / RENDAH |
| 71 | `POST` | `/api/v1/admin/commercial/custom-override/{tenantId}` | Sudah Diperbaiki (Terhubung DB/Engine) | `commercial_plans` (SELECT/INSERT/UPDATE), `tenant_subscriptions` (SELECT/UPDATE) | SEDANG / RENDAH |
| 72 | `GET` | `/api/v1/admin/commercial/metering-rules` | Sudah Diperbaiki (Terhubung DB/Engine) | `commercial_plans` (SELECT/INSERT/UPDATE), `tenant_subscriptions` (SELECT/UPDATE) | SEDANG / RENDAH |
| 73 | `POST` | `/api/v1/admin/commercial/metering-rules` | Sudah Diperbaiki (Terhubung DB/Engine) | `commercial_plans` (SELECT/INSERT/UPDATE), `tenant_subscriptions` (SELECT/UPDATE) | SEDANG / RENDAH |
| 74 | `DELETE` | `/api/v1/admin/commercial/metering-rules/{activityType}` | Sudah Diperbaiki (Terhubung DB/Engine) | `commercial_plans` (SELECT/INSERT/UPDATE), `tenant_subscriptions` (SELECT/UPDATE) | SEDANG / RENDAH |
| 75 | `GET` | `/api/v1/admin/commercial/cost-factors` | Sudah Diperbaiki (Terhubung DB/Engine) | `commercial_plans` (SELECT/INSERT/UPDATE), `tenant_subscriptions` (SELECT/UPDATE) | SEDANG / RENDAH |
| 76 | `POST` | `/api/v1/admin/commercial/cost-factors` | Sudah Diperbaiki (Terhubung DB/Engine) | `commercial_plans` (SELECT/INSERT/UPDATE), `tenant_subscriptions` (SELECT/UPDATE) | SEDANG / RENDAH |
| 77 | `DELETE` | `/api/v1/admin/commercial/cost-factors/{factorType}/{factorKey}` | Sudah Diperbaiki (Terhubung DB/Engine) | `commercial_plans` (SELECT/INSERT/UPDATE), `tenant_subscriptions` (SELECT/UPDATE) | SEDANG / RENDAH |
| 78 | `POST` | `/api/v1/admin/commercial/simulate-cost` | Sudah Diperbaiki (Terhubung DB/Engine) | `commercial_plans` (SELECT/INSERT/UPDATE), `tenant_subscriptions` (SELECT/UPDATE) | SEDANG / RENDAH |
| 79 | `POST` | `/api/v1/admin/billing/credit-adjustment` | Sudah Diperbaiki (Terhubung DB/Engine) | `audit_logs` (SELECT), `tenants` (SELECT) | SEDANG / RENDAH |
| 80 | `GET` | `/api/v1/admin/billing/tenant-wallet/{tenantId}` | Sudah Diperbaiki (Terhubung DB/Engine) | `audit_logs` (SELECT), `tenants` (SELECT) | SEDANG / RENDAH |
| 81 | `GET` | `/api/v1/admin/financial-command-center` | Sudah Diperbaiki (Terhubung DB/Engine) | `audit_logs` (SELECT), `tenants` (SELECT) | SEDANG / RENDAH |
| 82 | `GET` | `/api/v1/admin/analytics/financial-command-center` | Sudah Diperbaiki (Terhubung DB/Engine) | `llm_usage_logs` (SELECT), `central_credit_ledger` (SELECT), `tasks` (SELECT) | SEDANG / RENDAH |
| 83 | `POST` | `/api/v1/admin/platform-assets/icon-logo` | Sudah Diperbaiki (Terhubung DB/Engine) | `audit_logs` (SELECT), `tenants` (SELECT) | SEDANG / RENDAH |
| 84 | `GET` | `/api/v1/admin/platform-assets/icon-logo` | Sudah Diperbaiki (Terhubung DB/Engine) | `audit_logs` (SELECT), `tenants` (SELECT) | SEDANG / RENDAH |
| 85 | `GET` | `/api/v1/admin/security/ip-allowlist` | Sudah Diperbaiki (Terhubung DB/Engine) | `audit_logs` (SELECT), `tenants` (SELECT) | SEDANG / RENDAH |
| 86 | `POST` | `/api/v1/admin/security/ip-allowlist` | Sudah Diperbaiki (Terhubung DB/Engine) | `audit_logs` (SELECT), `tenants` (SELECT) | SEDANG / RENDAH |
| 87 | `POST` | `/api/v1/admin/support/impersonate` | Sudah Diperbaiki (Terhubung DB/Engine) | `audit_logs` (SELECT), `tenants` (SELECT) | SEDANG / RENDAH |
| 88 | `GET` | `/api/v1/admin/support/impersonate/{sessionId}` | Sudah Diperbaiki (Terhubung DB/Engine) | `audit_logs` (SELECT), `tenants` (SELECT) | SEDANG / RENDAH |
| 89 | `GET` | `/api/v1/admin/security/csrf-token` | Sudah Diperbaiki (Terhubung DB/Engine) | `audit_logs` (SELECT), `tenants` (SELECT) | SEDANG / RENDAH |
| 90 | `POST` | `/api/v1/admin/login` | Sudah Diperbaiki (Terhubung DB/Engine) | `audit_logs` (SELECT), `tenants` (SELECT) | SEDANG / RENDAH |
| 91 | `POST` | `/api/v1/admin/auth/login` | Sudah Diperbaiki (Terhubung DB/Engine) | `audit_logs` (SELECT), `tenants` (SELECT) | SEDANG / RENDAH |
| 92 | `POST` | `/api/v1/admin/auth/verify-mfa` | Sudah Diperbaiki (Terhubung DB/Engine) | `audit_logs` (SELECT), `tenants` (SELECT) | SEDANG / RENDAH |

---

