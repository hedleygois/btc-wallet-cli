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
# Using 'go-offline' equivalent or just resolving dependencies
RUN ./gradlew --no-daemon dependencies

# Copy the source code
COPY src src

# Build the application and create the distribution
# skipping tests to speed up the build in Docker
RUN ./gradlew --no-daemon installDist -x test

# Stage 2: Create the lightweight runtime image
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create a non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy the installation from the builder stage
COPY --from=builder /src/build/install/btc-wallet-app .

# Change ownership to the non-root user
RUN chown -R appuser:appgroup /app

# Switch to the non-root user
USER appuser

# Expose any necessary ports (none for a CLI app, but good practice if it had a server)
# EXPOSE 8080 

# Define the entry point
ENTRYPOINT ["/app/bin/btc-wallet-app"]
