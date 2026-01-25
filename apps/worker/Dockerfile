# Multi-stage build for ScaleBiometrics Worker

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /build

# Copy the entire project structure from the root context
COPY . .

# Install parent POM and download dependencies
RUN mvn install -DskipTests -B -pl . && \
    mvn dependency:go-offline -B -pl apps/worker -am

# Build the Worker module
RUN mvn clean package -DskipTests -B -pl apps/worker -am

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="ScaleBiometrics Team"
LABEL description="ScaleBiometrics Worker - Distributed biometric matching engine"

# Create non-root user
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

WORKDIR /app

# Copy JAR from build stage
COPY --from=build /build/apps/worker/target/worker-*.jar app.jar

# Change ownership
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose ports
EXPOSE 50051 9091

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:9091/actuator/health || exit 1

# Run application with Virtual Threads enabled
ENTRYPOINT ["java", \
            "-XX:+UseZGC", \
            "-XX:+UseStringDeduplication", \
            "-Xms1g", \
            "-Xmx4g", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar", \
            "app.jar"]
