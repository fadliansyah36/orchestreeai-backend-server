const { Client } = require("pg");
const client = new Client({ connectionString: "postgresql://postgres:BlNnlC7wG1xa611t@db.exfvfyiwftywqjcsofgf.supabase.co:5432/postgres" });

async function run() {
  await client.connect();
  console.log("Connected to Supabase Postgres.");

  const tables = [
    "workflow_executions",
    "llm_usage_logs",
    "selection_results",
    "proactive_messages_log",
    "tasks",
    "chief_of_staff_briefings",
    "agent_decision_outcomes"
  ];

  for (const t of tables) {
    try {
      const res = await client.query(`SELECT COUNT(*) as count, MAX(created_at) as max_created_at FROM ${t};`);
      console.log(`[${t}] count: ${res.rows[0].count}, max_created_at: ${res.rows[0].max_created_at}`);
    } catch (e) {
      console.log(`[${t}] error: ${e.message}`);
    }
  }

  // Also query 3.2 details
  try {
    const resTasks = await client.query(`SELECT COUNT(*) as count, MAX(created_at) as max_created_at FROM tasks WHERE agent_id = 'agent-orchestrator' OR agent_id = 'agent-proactive-reporter';`);
    console.log(`[tasks (orchestrator/proactive)] count: ${resTasks.rows[0].count}, max_created_at: ${resTasks.rows[0].max_created_at}`);
  } catch (e) {
    console.log(`[tasks filtered] error: ${e.message}`);
  }

  await client.end();
}

run().catch(err => {
  console.error(err);
  process.exit(1);
});
