# ---------- Этап 1: сборка ----------
FROM gradle:8.14.3-jdk17 AS build

WORKDIR /home/gradle/project

# Сначала только файлы сборки — слой с зависимостями кэшируется между сборками
COPY --chown=gradle:gradle settings.gradle.kts build.gradle.kts gradle.properties ./
RUN gradle --no-daemon dependencies --quiet || true

COPY --chown=gradle:gradle src ./src
RUN gradle --no-daemon installDist -x test

# ---------- Этап 2: запуск ----------
FROM eclipse-temurin:17-jre-alpine

RUN addgroup -S ktor && adduser -S ktor -G ktor
WORKDIR /app

COPY --from=build /home/gradle/project/build/install/ktor-task-api /app
RUN chown -R ktor:ktor /app

USER ktor

ENV PORT=8080
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=15s --retries=3 \
    CMD wget -q --spider "http://127.0.0.1:${PORT}/health" || exit 1

ENTRYPOINT ["/app/bin/ktor-task-api"]
