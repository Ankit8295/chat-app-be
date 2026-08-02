# Runtime image only — JAR is built on the host so Docker does not need
# to download Gradle (avoids services.gradle.org network failures).
#
# Build first:  .\gradlew.bat bootJar -x test
# Then:         docker compose up --build
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY build/libs/the-chat-backend-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
