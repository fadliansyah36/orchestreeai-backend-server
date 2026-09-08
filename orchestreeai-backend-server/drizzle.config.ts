import { defineConfig } from "drizzle-kit";

export default defineConfig({
  dialect: "postgresql",
  schema: "./drizzle/schema.ts",
  out: "./db/migrations",
  dbCredentials: {
    url: process.env.DATABASE_URL || "postgresql://postgres:BlNnlC7wG1xa611t@db.exfvfyiwftywqjcsofgf.supabase.co:5432/postgres",
  },
});
