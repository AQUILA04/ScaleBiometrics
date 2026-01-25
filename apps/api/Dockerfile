# Multi-stage build for ScaleBiometrics API

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /build

# Copy the entire project structure from the root context
COPY . .

# Download dependencies
RUN mvn dependency:go-offline -B

# Build the API module
RUN mvn clean package -DskipTests -B -pl apps/api -am

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="ScaleBiometrics Team"
LABEL description="ScaleBiometrics API - National-grade biometric deduplication platform"

# Create non-root user
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

WORKDIR /app

# Copy JAR from build stage
COPY --from=build /build/apps/api/target/api-*.jar app.jar

# Change ownership
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose ports
EXPOSE 8081 9090

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/api/actuator/health || exit 1

# Run application with Virtual Threads enabled
ENTRYPOINT ["java", \
            "-XX:+UseZGC", \
            "-XX:+UseStringDeduplication", \
            "-Xms512m", \
            "-Xmx2g", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar", \
            "app.jar"]
