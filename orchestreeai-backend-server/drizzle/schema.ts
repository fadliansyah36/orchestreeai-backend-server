import { pgTable, text, timestamp, boolean, jsonb, integer, uuid } from "drizzle-orm/pg-core";

// Tenants & Identity
export const tenants = pgTable("tenants", {
  id: uuid("id").primaryKey().defaultRandom(),
  name: text("name").notNull(),
  slug: text("slug").notNull().unique(),
  planTier: text("plan_tier").default("free"),
  isActive: boolean("is_active").default(true),
  createdAt: timestamp("created_at", { withTimezone: true }).defaultNow(),
  updatedAt: timestamp("updated_at", { withTimezone: true }).defaultNow(),
});

export const users = pgTable("users", {
  id: uuid("id").primaryKey().defaultRandom(),
  tenantId: uuid("tenant_id").references(() => tenants.id),
  email: text("email").notNull().unique(),
  fullName: text("full_name"),
  role: text("role").default("STAFF_HUMAN"),
  isActive: boolean("is_active").default(true),
  createdAt: timestamp("created_at", { withTimezone: true }).defaultNow(),
});

// AI Workforce & Job Titles
export const aiJobTitles = pgTable("ai_job_titles", {
  id: uuid("id").primaryKey().defaultRandom(),
  tenantId: uuid("tenant_id").references(() => tenants.id),
  title: text("title").notNull(),
  department: text("department").notNull(),
  personaPrompt: text("persona_prompt"),
  defaultModel: text("default_model").default("auto"),
  operationMode: text("operation_mode").default("AUTONOMOUS"),
  createdAt: timestamp("created_at", { withTimezone: true }).defaultNow(),
});

export const workforceAgents = pgTable("workforce_agents", {
  id: uuid("id").primaryKey().defaultRandom(),
  tenantId: uuid("tenant_id").references(() => tenants.id),
  jobTitleId: uuid("job_title_id").references(() => aiJobTitles.id),
  name: text("name").notNull(),
  avatarUrl: text("avatar_url"),
  status: text("status").default("IDLE"),
  metadata: jsonb("metadata").default({}),
  createdAt: timestamp("created_at", { withTimezone: true }).defaultNow(),
});

// Tasks & Orchestration Workflows
export const tasks = pgTable("tasks", {
  id: uuid("id").primaryKey().defaultRandom(),
  tenantId: uuid("tenant_id").references(() => tenants.id),
  title: text("title").notNull(),
  description: text("description"),
  status: text("status").default("TODO"),
  priority: text("priority").default("MEDIUM"),
  assignedAgentId: uuid("assigned_agent_id").references(() => workforceAgents.id),
  assignedUserId: uuid("assigned_user_id").references(() => users.id),
  createdAt: timestamp("created_at", { withTimezone: true }).defaultNow(),
  updatedAt: timestamp("updated_at", { withTimezone: true }).defaultNow(),
});

export const workflowExecutions = pgTable("workflow_executions", {
  id: uuid("id").primaryKey().defaultRandom(),
  tenantId: uuid("tenant_id").references(() => tenants.id),
  workflowId: text("workflow_id").notNull(),
  status: text("status").default("RUNNING"),
  executionStatus: text("execution_status").default("running"),
  currentStep: integer("current_step").default(0),
  lastCompletedNodeId: text("last_completed_node_id"),
  currentStateSnapshot: jsonb("current_state_snapshot"),
  contextData: jsonb("context_data").default({}),
  resultData: jsonb("result_data"),
  startedAt: timestamp("started_at", { withTimezone: true }).defaultNow(),
  lastUpdatedAt: timestamp("last_updated_at", { withTimezone: true }).defaultNow(),
  completedAt: timestamp("completed_at", { withTimezone: true }),
});

// LLM Logs & Telemetry
export const llmUsageLogs = pgTable("llm_usage_logs", {
  id: uuid("id").primaryKey().defaultRandom(),
  tenantId: uuid("tenant_id").references(() => tenants.id),
  provider: text("provider").notNull(),
  model: text("model").notNull(),
  promptTokens: integer("prompt_tokens").default(0),
  completionTokens: integer("completion_tokens").default(0),
  costUsd: text("cost_usd").default("0.0"),
  latencyMs: integer("latency_ms").default(0),
  createdAt: timestamp("created_at", { withTimezone: true }).defaultNow(),
});

export const auditLogs = pgTable("audit_logs", {
  id: uuid("id").primaryKey().defaultRandom(),
  tenantId: uuid("tenant_id").references(() => tenants.id),
  actorId: text("actor_id"),
  action: text("action").notNull(),
  entityType: text("entity_type").notNull(),
  entityId: text("entity_id"),
  details: jsonb("details").default({}),
  createdAt: timestamp("created_at", { withTimezone: true }).defaultNow(),
});

// Channels & Studio
export const channelAccounts = pgTable("channel_accounts", {
  id: uuid("id").primaryKey().defaultRandom(),
  tenantId: uuid("tenant_id").references(() => tenants.id),
  channelType: text("channel_type").notNull(),
  accountIdentifier: text("account_identifier").notNull(),
  status: text("status").default("ACTIVE"),
  operationMode: text("operation_mode").default("SHARED"),
  config: jsonb("config").default({}),
  createdAt: timestamp("created_at", { withTimezone: true }).defaultNow(),
});

export const generativeAssets = pgTable("generative_assets", {
  id: uuid("id").primaryKey().defaultRandom(),
  tenantId: uuid("tenant_id").references(() => tenants.id),
  assetType: text("asset_type").notNull(),
  prompt: text("prompt").notNull(),
  imageUrl: text("image_url"),
  provider: text("provider").notNull(),
  createdAt: timestamp("created_at", { withTimezone: true }).defaultNow(),
});
