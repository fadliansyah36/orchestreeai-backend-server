# Multi-stage Docker build for OrchestreeAI Ktor Backend Server
FROM gradle:8.10-jdk21-alpine AS build
WORKDIR /app
COPY gradle.properties settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
COPY src ./src
RUN gradle shadowJar --no-daemon -x test || gradle build --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=build /app/build/libs/*.jar app.jar
RUN chown -R appuser:appgroup /app
USER appuser

EXPOSE 8080
ENV PORT=8080
ENV APPLICATION_ENV=production
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]

