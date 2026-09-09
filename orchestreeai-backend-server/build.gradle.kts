plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
    application
}

group = "ai.orchestree"
version = "1.0.0"

repositories {
    mavenCentral()
    google()
}

application {
    mainClass.set("ai.orchestree.backend.ApplicationKt")
}

tasks.jar {
    archiveClassifier.set("plain")
    destinationDirectory.set(layout.buildDirectory.dir("plain-libs"))
}

tasks.shadowJar {
    archiveBaseName.set("orchestreeai-backend-server")
    archiveClassifier.set("all")
    archiveVersion.set("1.0.0")
    mergeServiceFiles()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.named<JavaExec>("run") {
    environment(System.getenv())
    val envFile = file("$rootDir/.env")
    if (envFile.exists()) {
        envFile.readLines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && trimmed.contains("=")) {
                val parts = trimmed.split("=", limit = 2)
                val key = parts[0].trim()
                val value = parts[1].trim()
                if (key.isNotEmpty() && value.isNotEmpty()) {
                    environment(key, value)
                }
            }
        }
    }
}

val ktorVersion = "3.0.3"
val logbackVersion = "1.5.16"
val kotlinxSerializationVersion = "1.7.3"
val coroutinesVersion = "1.10.2"
val mockkVersion = "1.13.16"

dependencies {
    // Ktor Server Core & Engine
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-cors-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-default-headers-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-websockets-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-request-validation-jvm:$ktorVersion")

    // Ktor Client for external API & MCP calls
    implementation("io.ktor:ktor-client-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-cio-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation-jvm:$ktorVersion")

    // Kotlinx Serialization & Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

    // Logging
    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    // OkHttp Client
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Redis Client
    implementation("redis.clients:jedis:5.2.0")

    // PostgreSQL JDBC Driver
    implementation("org.postgresql:postgresql:42.7.5")

    // OpenTelemetry
    val otelVersion = "1.46.0"
    implementation("io.opentelemetry:opentelemetry-api:$otelVersion")
    implementation("io.opentelemetry:opentelemetry-sdk:$otelVersion")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp:$otelVersion")

    // Testing
    testImplementation("io.ktor:ktor-server-test-host-jvm:$ktorVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
}

tasks.test {
    useJUnitPlatform()
    environment(System.getenv())
    val envFile = file("$rootDir/.env")
    if (envFile.exists()) {
        envFile.readLines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && trimmed.contains("=")) {
                val parts = trimmed.split("=", limit = 2)
                val key = parts[0].trim()
                val value = parts[1].trim()
                if (key.isNotEmpty() && value.isNotEmpty()) {
                    environment(key, value)
                }
            }
        }
    }
    testLogging {
        events("passed", "skipped", "failed", "standardOut", "standardError")
        showStandardStreams = true
    }
}

tasks.register("verifyNoUnauthorizedAiSdk") {
    group = "verification"
    description = "Scans backend dependencies and source code to ensure zero unauthorized AI SDKs (Google AI SDK, Vertex AI, etc.) are imported without explicit DB provider registration."
    doLast {
        println(">>> RUNNING CI GATE: verifyNoUnauthorizedAiSdk <<<")
        val forbiddenPatterns = listOf(
            "com.google.ai.client.generativeai",
            "com.google.cloud.vertexai",
            "dev.langchain4j.model.googleai",
            "com.google.firebase.vertexai"
        )
        val srcDir = file("src/main")
        val violations = mutableListOf<String>()
        srcDir.walkTopDown().filter { it.isFile && (it.extension == "kt" || it.extension == "java") }.forEach { file ->
            val lines = file.readLines()
            lines.forEachIndexed { idx, line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("import ")) {
                    for (pattern in forbiddenPatterns) {
                        if (trimmed.contains(pattern)) {
                            violations.add("${file.path}:${idx + 1} - $trimmed")
                        }
                    }
                }
            }
        }
        if (violations.isNotEmpty()) {
            throw GradleException("❌ CI GATE FAILED: Unauthorized AI SDK imports found in backend:\n" + violations.joinToString("\n"))
        }
        println("✅ CI GATE PASSED: Zero unauthorized AI SDK dependencies detected in backend source set.")
    }
}

tasks.named("check") {
    dependsOn("verifyNoUnauthorizedAiSdk")
}

kotlin {
    jvmToolchain(21)
}
