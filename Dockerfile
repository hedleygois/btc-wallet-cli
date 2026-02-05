# Stage 1: Build the application
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /src

# Copy Gradle wrapper and configuration files
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Make gradlew executable
RUN chmod +x gradlew

# Download dependencies (this layer will be cached if dependencies don't change)
RUN ./gradlew --no-daemon dependencies

# Copy the source code
COPY src src

# Build the Spring Boot application into a fat JAR
# skipping tests to speed up the build in Docker
RUN ./gradlew --no-daemon bootJar -x test

# Stage 2: Create the lightweight runtime image
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create a non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy the fat JAR from the builder stage
COPY --from=builder /src/build/libs/*.jar ./app.jar

# Change ownership to the non-root user
RUN chown -R appuser:appgroup /app

# Switch to the non-root user
USER appuser

# Expose the application port (e.g., 8080)
EXPOSE 8080 

# Define the entry point to run the Spring Boot fat JAR
ENTRYPOINT ["java", "-jar", "app.jar"]
