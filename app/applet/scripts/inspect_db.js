const { Client } = require("pg");
const client = new Client({ connectionString: "postgresql://postgres:BlNnlC7wG1xa611t@db.exfvfyiwftywqjcsofgf.supabase.co:5432/postgres" });

async function run() {
  await client.connect();
  console.log("Connected to Supabase Postgres successfully!");

  // 1. Check flyway / migration tables
  const tablesRes = await client.query(`
    SELECT table_schema, table_name 
    FROM information_schema.tables 
    WHERE table_name LIKE '%migration%' OR table_name LIKE '%flyway%' OR table_name LIKE '%schema%'
  `);
  console.log("Migration tables found:", tablesRes.rows);

  // Check columns of tasks
  const tasksCols = await client.query(`
    SELECT column_name, data_type 
    FROM information_schema.columns 
    WHERE table_schema = 'public' AND table_name = 'tasks'
    ORDER BY ordinal_position;
  `);
  console.log("Tasks columns:", tasksCols.rows.map(r => r.column_name));

  // Check columns of notifications
  const notifCols = await client.query(`
    SELECT column_name, data_type 
    FROM information_schema.columns 
    WHERE table_schema = 'public' AND table_name = 'notifications'
    ORDER BY ordinal_position;
  `);
  console.log("Notifications columns:", notifCols.rows.map(r => `${r.column_name} (${r.data_type})`));

  // Check table names matching activity or stream
  const actTables = await client.query(`
    SELECT table_name 
    FROM information_schema.tables 
    WHERE table_schema = 'public' AND (table_name LIKE '%activity%' OR table_name LIKE '%stream%')
    ORDER BY table_name;
  `);
  console.log("Activity / stream tables:", actTables.rows.map(r => r.table_name));

  // Check proactive_messages_log columns & constraints
  const pmlCols = await client.query(`
    SELECT column_name, is_nullable, data_type 
    FROM information_schema.columns 
    WHERE table_schema = 'public' AND table_name = 'proactive_messages_log'
    ORDER BY ordinal_position;
  `);
  console.log("proactive_messages_log columns:", pmlCols.rows);

  // Check proactive_subscriptions columns
  const psCols = await client.query(`
    SELECT column_name, is_nullable, data_type 
    FROM information_schema.columns 
    WHERE table_schema = 'public' AND table_name = 'proactive_subscriptions'
    ORDER BY ordinal_position;
  `);
  console.log("proactive_subscriptions columns:", psCols.rows);

  // Let's check flyway_schema_history rows if it exists
  try {
    const fsh = await client.query("SELECT installed_rank, version, description, type, script, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 25;");
    console.log("Flyway last 25 rows:", fsh.rows);
  } catch(e) {
    console.log("Error querying flyway_schema_history:", e.message);
  }

  // Let's check supabase_migrations schema
  try {
    const sm = await client.query("SELECT * FROM supabase_migrations.schema_migrations ORDER BY version DESC LIMIT 25;");
    console.log("Supabase schema_migrations:", sm.rows);
  } catch(e) {
    console.log("Error querying supabase_migrations.schema_migrations:", e.message);
  }

  await client.end();
}

run().catch(err => {
  console.error(err);
  process.exit(1);
});
