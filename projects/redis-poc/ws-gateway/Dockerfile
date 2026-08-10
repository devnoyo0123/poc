FROM gradle:8.7-jdk17 AS builder
WORKDIR /app

COPY settings.gradle.kts build.gradle.kts ./
COPY shared ./shared
COPY ws-gateway ./ws-gateway
COPY api-server ./api-server
COPY gradle ./gradle
COPY gradlew ./

RUN gradle :ws-gateway:bootJar --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/ws-gateway/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
