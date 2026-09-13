const { Client } = require("pg");

async function main() {
  const query = process.argv[2] || "SELECT id, tenant_id, workflow_def_id, status, started_at, completed_at, result_summary FROM workflow_executions ORDER BY started_at DESC LIMIT 5;";
  const client = new Client({
    connectionString: "postgresql://postgres:BlNnlC7wG1xa611t@db.exfvfyiwftywqjcsofgf.supabase.co:5432/postgres",
    connectionTimeoutMillis: 5000
  });

  try {
    await client.connect();
    const res = await client.query(query);
    console.log(JSON.stringify(res.rows, null, 2));
  } catch (err) {
    console.error("Query error:", err.message);
  } finally {
    await client.end();
    process.exit(0);
  }
}

main();
