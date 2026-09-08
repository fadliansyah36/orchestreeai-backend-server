package ai.orchestree.backend

import ai.orchestree.backend.config.EnvLoader

/**
 * BuildConfig for orchestreeai-backend-server.
 * Contains configuration keys and environment-backed properties.
 */
object BuildConfig {
    const val APPLICATION_ID = "ai.orchestree.backend"
    const val BUILD_TYPE = "release"
    const val DEBUG = false
    const val VERSION_NAME = "1.0.0"

    // Supabase
    val SUPABASE_URL: String
        get() = EnvLoader.get("SUPABASE_URL", "https://exfvfyiwftywqjcsofgf.supabase.co")

    val SUPABASE_ANON_KEY: String
        get() = EnvLoader.get("SUPABASE_ANON_KEY", "sb_publishable_s7b47MyyC53aD0HmVdhspQ_Xmeh0gbG")

    val SUPABASE_SERVICE_ROLE_KEY: String
        get() = EnvLoader.get("SUPABASE_SERVICE_ROLE_KEY", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImV4ZnZmeWl3ZnR5d3FqY3NvZmdmIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc4NzYxOTkxMSwiZXhwIjoyMTAzMTk1OTExfQ.KlgZ2nzkE18uj6xcj1m-3M3IbAJ-40J6XmoRLhxUEUo")

    val SUPABASE_ACCESS_TOKEN: String
        get() = EnvLoader.get("SUPABASE_ACCESS_TOKEN", "sbp_d47e89d4bed78b2e7ab71c5fa099106785cd5fc4")

    // Database URLs
    val DATABASE_URL: String
        get() = EnvLoader.get("DATABASE_URL", "postgresql://postgres:BlNnlC7wG1xa611t@db.exfvfyiwftywqjcsofgf.supabase.co:5432/postgres")

    val DATABASE_DIRECT_URL: String
        get() = EnvLoader.get("DATABASE_DIRECT_URL", "postgresql://postgres:BlNnlC7wG1xa611t@db.exfvfyiwftywqjcsofgf.supabase.co:5432/postgres")

    val DATABASE_POOL_URL: String
        get() = EnvLoader.get("DATABASE_POOL_URL", "postgresql://postgres.exfvfyiwftywqjcsofgf:BlNnlC7wG1xa611t@aws-0-ap-south-1.pooler.supabase.com:6543/postgres")

    // Security & Auth
    val JWT_SECRET_KEY: String
        get() = EnvLoader.get("JWT_SECRET_KEY", "101ffa9b-10c9-4e15-9390-90c2d32ed6c8")

    val ENCRYPTION_MASTER_KEY_REF: String
        get() = EnvLoader.get("ENCRYPTION_MASTER_KEY_REF", "default_master_key_ref")

    // Backend Base URL & Cache
    val BACKEND_API_BASE_URL: String
        get() = EnvLoader.get("BACKEND_API_BASE_URL", "http://localhost:8082")

    val REDIS_URL: String
        get() = EnvLoader.get("REDIS_URL", "redis://localhost:6379")

    // AI & LLM Providers
    val GROQ_API_KEY: String
        get() = EnvLoader.get("GROQ_API_KEY", "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")

    val GROQ_BASE_URL: String
        get() = EnvLoader.get("GROQ_BASE_URL", "https://api.groq.com/openai/v1")

    val GROQ_API_URL: String
        get() = EnvLoader.get("GROQ_API_URL", "https://api.groq.com/openai/v1")

    val DEEPSEEK_API_KEY: String
        get() = EnvLoader.get("DEEPSEEK_API_KEY", "sk-7f4157350c014ed383024d181f83a6db")

    val DEEPSEEK_API_URL: String
        get() = EnvLoader.get("DEEPSEEK_API_URL", "https://api.deepseek.com")

    val OPENROUTER_API_KEY: String
        get() = EnvLoader.get("OPENROUTER_API_KEY", "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")

    val OPENROUTER_BASE_URL: String
        get() = EnvLoader.get("OPENROUTER_BASE_URL", "https://openrouter.ai/api/v1")

    val ANTHROPIC_API_KEY: String
        get() = EnvLoader.get("ANTHROPIC_API_KEY", "sk-f360fe7928d9b08b-37zjb6-5a0be5b1")

    val OPENAI_API_KEY: String
        get() = EnvLoader.get("OPENAI_API_KEY", "default_openai_api_key")

    val GPT_IMAGE_2_API_KEY: String
        get() = EnvLoader.get("GPT_IMAGE_2_API_KEY", "sk-eITKIEoeQDMnT0gpdx4wtau4GdQBmaaJoHwiwd9jXLhK5XtD")

    val GPT_IMAGE_2_API_URL: String
        get() = EnvLoader.get("GPT_IMAGE_2_API_URL", "https://api.apimart.ai/v1/images/generations")

    val GROK_API_KEY: String
        get() = EnvLoader.get("GROK_API_KEY", "")

    val GROK_API_URL: String
        get() = EnvLoader.get("GROK_API_URL", "https://api.groq.com/openai/v1")

    val VIBE_PROSPECTING_API_KEY: String
        get() = EnvLoader.get("VIBE_PROSPECTING_API_KEY", "c868f2995f424a05a328215d8e6cbldfd")

    val VIBE_PROSPECTING_API_URL: String
        get() = EnvLoader.get("VIBE_PROSPECTING_API_URL", "https://api.vibeprospecting.com/v1")

    val WORLD_MONITOR_API_KEY: String
        get() = EnvLoader.get("WORLD_MONITOR_API_KEY", "")

    val WORLD_MONITOR_API_URL: String
        get() = EnvLoader.get("WORLD_MONITOR_API_URL", "https://api.worldmonitor.ai/v1")

    // Omnichannel & Bots
    val TELEGRAM_OFFICIAL_BOT_TOKEN: String
        get() = EnvLoader.get("TELEGRAM_OFFICIAL_BOT_TOKEN", "7829980615:AAGJodM2pggk9hyrhy2Jn_gqFWzNE3oeUOY")

    val TELEGRAM_BOT_TOKEN: String
        get() = EnvLoader.get("TELEGRAM_BOT_TOKEN", "7829980615:AAGJodM2pggk9hyrhy2Jn_gqFWzNE3oeUOY")

    val TELEGRAM_BOT_USERNAME: String
        get() = EnvLoader.get("TELEGRAM_BOT_USERNAME", "OrchestreeAI.bot")

    val TELEGRAM_WEBHOOK_URL: String
        get() = EnvLoader.get("TELEGRAM_WEBHOOK_URL", "https://placeholder.supabase.co/functions/v1/telegram-webhook")

    val WHATSAPP_OFFICIAL_PHONE_NUMBER_ID: String
        get() = EnvLoader.get("WHATSAPP_OFFICIAL_PHONE_NUMBER_ID", "default_whatsapp_phone_number_id")

    // Payment Gateway
    val PAYMENT_GATEWAY_CLIENT_KEY: String
        get() = EnvLoader.get("PAYMENT_GATEWAY_CLIENT_KEY", "Mid-client-QszZgKEEZ9rAkQgO")

    val PAYMENT_GATEWAY_SERVER_KEY: String
        get() = EnvLoader.get("PAYMENT_GATEWAY_SERVER_KEY", "Mid-server-uUD3vFaO_hBkxelpU7BNPYXl")

    // OAuth & Identity
    val GOOGLE_OAUTH_CLIENT_ID_ANDROID: String
        get() = EnvLoader.get("GOOGLE_OAUTH_CLIENT_ID_ANDROID", "default_google_oauth_android")

    val GOOGLE_OAUTH_CLIENT_ID_WEB: String
        get() = EnvLoader.get("GOOGLE_OAUTH_CLIENT_ID_WEB", "default_google_oauth_web")

    val GOOGLE_OAUTH_CLIENT_SECRET: String
        get() = EnvLoader.get("GOOGLE_OAUTH_CLIENT_SECRET", "default_google_oauth_secret")

    val META_APP_ID: String
        get() = EnvLoader.get("META_APP_ID", "default_meta_app_id")

    val META_APP_SECRET: String
        get() = EnvLoader.get("META_APP_SECRET", "default_meta_app_secret")
}
