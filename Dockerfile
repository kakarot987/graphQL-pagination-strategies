# Multi-stage Dockerfile: build with Gradle, run with a slim JRE
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /workspace

# Copy project files
COPY . /workspace

# Ensure the Gradle wrapper is executable and build the jar (skip tests)
RUN chmod +x ./gradlew \
	&& ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the built jar from the builder stage
COPY --from=builder /workspace/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]

