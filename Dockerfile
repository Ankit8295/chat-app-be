# Stage 1: Build the fat JAR
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

COPY gradle/ gradle/
COPY gradlew settings.gradle build.gradle gradle.properties ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

COPY src/ src/
RUN ./gradlew bootJar --no-daemon -x test

# Stage 2: Lightweight runtime image
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
