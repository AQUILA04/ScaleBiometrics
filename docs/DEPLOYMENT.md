# ScaleBiometrics - Deployment Guide

**Version:** 1.0  
**Date:** 2025-12-25  
**Status:** Production Ready

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Local Development Setup](#local-development-setup)
3. [Docker Deployment](#docker-deployment)
4. [Kubernetes Deployment](#kubernetes-deployment)
5. [Configuration](#configuration)
6. [Verification](#verification)
7. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### System Requirements

**Development:**
- Java 21 JDK
- Maven 3.9.6+
- Docker 24.0+
- Docker Compose 2.20+
- Git 2.40+

**Production:**
- Kubernetes 1.27+ (or Docker Swarm)
- PostgreSQL 15+
- Redis 7+
- Apache Kafka 7.5+
- MinIO 8.5+

### Network Requirements

**Ports:**
- 8080: API (HTTP)
- 9091: Master gRPC
- 9092: Worker gRPC
- 5432: PostgreSQL
- 6379: Redis
- 9092: Kafka
- 9200: Elasticsearch
- 5601: Kibana
- 3001: Grafana
- 16686: Jaeger

---

## Local Development Setup

### 1. Clone Repository

```bash
git clone https://github.com/AQUILA04/ScaleBiometrics.git
cd ScaleBiometrics
git checkout feature/epic-1-foundation-setup
```

### 2. Build Project

```bash
# Build all modules
mvn clean package -DskipTests

# Build with tests
mvn clean package

# Build specific module
mvn clean package -pl apps/api
mvn clean package -pl apps/master
mvn clean package -pl apps/worker
```

### 3. Start Infrastructure

```bash
# Start all services
docker-compose -f infrastructure/local/docker-compose.yml up -d

# Verify services
docker-compose -f infrastructure/local/docker-compose.yml ps

# View logs
docker-compose -f infrastructure/local/docker-compose.yml logs -f
```

### 4. Run Applications

**Terminal 1 - API:**
```bash
cd apps/api
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

**Terminal 2 - Master:**
```bash
cd apps/master
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

**Terminal 3 - Worker 1:**
```bash
cd apps/worker
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev,worker1"
```

**Terminal 4 - Worker 2:**
```bash
cd apps/worker
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev,worker2"
```

### 5. Verify Setup

```bash
# Check API health
curl http://localhost:8080/api/v1/health

# Check Master health
curl http://localhost:9091/health

# Check Worker health
curl http://localhost:9092/health

# View metrics
curl http://localhost:8080/actuator/metrics
```

---

## Docker Deployment

### 1. Build Docker Images

```bash
# Build all images
docker-compose -f infrastructure/local/docker-compose.yml build

# Build specific image
docker build -t scalebiometrics/api:latest -f apps/api/Dockerfile apps/api
docker build -t scalebiometrics/master:latest -f apps/master/Dockerfile apps/master
docker build -t scalebiometrics/worker:latest -f apps/worker/Dockerfile apps/worker
```

### 2. Create Dockerfiles

**apps/api/Dockerfile:**
```dockerfile
FROM eclipse-temurin:21-jdk as builder
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests -pl apps/api

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/apps/api/target/api-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**apps/master/Dockerfile:**
```dockerfile
FROM eclipse-temurin:21-jdk as builder
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests -pl apps/master

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/apps/master/target/master-*.jar app.jar
EXPOSE 9091
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**apps/worker/Dockerfile:**
```dockerfile
FROM eclipse-temurin:21-jdk as builder
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests -pl apps/worker

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/apps/worker/target/worker-*.jar app.jar
EXPOSE 9092
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 3. Deploy with Docker Compose

```bash
# Start all services
docker-compose -f infrastructure/local/docker-compose.yml up -d

# Scale workers
docker-compose -f infrastructure/local/docker-compose.yml up -d --scale worker=5

# View logs
docker-compose -f infrastructure/local/docker-compose.yml logs -f api
docker-compose -f infrastructure/local/docker-compose.yml logs -f master
docker-compose -f infrastructure/local/docker-compose.yml logs -f worker

# Stop all services
docker-compose -f infrastructure/local/docker-compose.yml down

# Remove volumes
docker-compose -f infrastructure/local/docker-compose.yml down -v
```

### 4. Push to Registry

```bash
# Login to registry
docker login -u username -p password registry.example.com

# Tag images
docker tag scalebiometrics/api:latest registry.example.com/scalebiometrics/api:1.0.0
docker tag scalebiometrics/master:latest registry.example.com/scalebiometrics/master:1.0.0
docker tag scalebiometrics/worker:latest registry.example.com/scalebiometrics/worker:1.0.0

# Push images
docker push registry.example.com/scalebiometrics/api:1.0.0
docker push registry.example.com/scalebiometrics/master:1.0.0
docker push registry.example.com/scalebiometrics/worker:1.0.0
```

---

## Kubernetes Deployment

### 1. Create Kubernetes Manifests

**k8s/namespace.yml:**
```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: scalebiometrics
```

**k8s/configmap.yml:**
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: scalebiometrics-config
  namespace: scalebiometrics
data:
  application.yml: |
    spring:
      datasource:
        url: jdbc:postgresql://postgres:5432/scalebiometrics
        username: scalebiometrics
        password: ${DB_PASSWORD}
      redis:
        host: redis
        port: 6379
      kafka:
        bootstrap-servers: kafka:9092
```

**k8s/api-deployment.yml:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: api
  namespace: scalebiometrics
spec:
  replicas: 3
  selector:
    matchLabels:
      app: api
  template:
    metadata:
      labels:
        app: api
    spec:
      containers:
      - name: api
        image: registry.example.com/scalebiometrics/api:1.0.0
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: db-credentials
              key: password
        livenessProbe:
          httpGet:
            path: /api/v1/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /api/v1/health
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 5
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
```

**k8s/master-deployment.yml:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: master
  namespace: scalebiometrics
spec:
  replicas: 3
  selector:
    matchLabels:
      app: master
  template:
    metadata:
      labels:
        app: master
    spec:
      containers:
      - name: master
        image: registry.example.com/scalebiometrics/master:1.0.0
        ports:
        - containerPort: 9091
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: MASTER_PORT
          value: "9091"
        livenessProbe:
          tcpSocket:
            port: 9091
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          tcpSocket:
            port: 9091
          initialDelaySeconds: 10
          periodSeconds: 5
        resources:
          requests:
            memory: "1Gi"
            cpu: "1000m"
          limits:
            memory: "2Gi"
            cpu: "2000m"
```

**k8s/worker-deployment.yml:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: worker
  namespace: scalebiometrics
spec:
  replicas: 10
  selector:
    matchLabels:
      app: worker
  template:
    metadata:
      labels:
        app: worker
    spec:
      containers:
      - name: worker
        image: registry.example.com/scalebiometrics/worker:1.0.0
        ports:
        - containerPort: 9092
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: WORKER_PORT
          value: "9092"
        - name: HNSW_MAX_SIZE_GB
          value: "8"
        livenessProbe:
          tcpSocket:
            port: 9092
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          tcpSocket:
            port: 9092
          initialDelaySeconds: 10
          periodSeconds: 5
        resources:
          requests:
            memory: "4Gi"
            cpu: "2000m"
          limits:
            memory: "8Gi"
            cpu: "4000m"
```

### 2. Deploy to Kubernetes

```bash
# Create namespace
kubectl apply -f k8s/namespace.yml

# Create secrets
kubectl create secret generic db-credentials \
  --from-literal=password=your-secure-password \
  -n scalebiometrics

# Create configmap
kubectl apply -f k8s/configmap.yml

# Deploy applications
kubectl apply -f k8s/api-deployment.yml
kubectl apply -f k8s/master-deployment.yml
kubectl apply -f k8s/worker-deployment.yml

# Verify deployment
kubectl get pods -n scalebiometrics
kubectl get svc -n scalebiometrics

# View logs
kubectl logs -f deployment/api -n scalebiometrics
kubectl logs -f deployment/master -n scalebiometrics
kubectl logs -f deployment/worker -n scalebiometrics
```

### 3. Setup Services

```yaml
apiVersion: v1
kind: Service
metadata:
  name: api
  namespace: scalebiometrics
spec:
  type: LoadBalancer
  selector:
    app: api
  ports:
  - port: 8080
    targetPort: 8080

---
apiVersion: v1
kind: Service
metadata:
  name: master
  namespace: scalebiometrics
spec:
  type: ClusterIP
  selector:
    app: master
  ports:
  - port: 9091
    targetPort: 9091

---
apiVersion: v1
kind: Service
metadata:
  name: worker
  namespace: scalebiometrics
spec:
  type: ClusterIP
  selector:
    app: worker
  ports:
  - port: 9092
    targetPort: 9092
```

---

## Configuration

### Environment Variables

**API:**
```bash
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/scalebiometrics
SPRING_DATASOURCE_USERNAME=scalebiometrics
SPRING_DATASOURCE_PASSWORD=secure-password
SPRING_REDIS_HOST=redis
SPRING_REDIS_PORT=6379
SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
KEYCLOAK_SERVER_URL=https://keycloak.example.com
KEYCLOAK_REALM=scalebiometrics
KEYCLOAK_CLIENT_ID=api-client
KEYCLOAK_CLIENT_SECRET=client-secret
```

**Master:**
```bash
SPRING_PROFILES_ACTIVE=prod
SPRING_REDIS_HOST=redis
SPRING_REDIS_PORT=6379
MASTER_PORT=9091
MASTER_LEADER_ELECTION_ENABLED=true
MASTER_HEALTH_CHECK_INTERVAL_MS=10000
```

**Worker:**
```bash
SPRING_PROFILES_ACTIVE=prod
WORKER_PORT=9092
HNSW_M=16
HNSW_EF_CONSTRUCTION=200
HNSW_EF_SEARCH=100
HNSW_MAX_SIZE_GB=8
OFF_HEAP_MAX_SIZE_GB=8
OFF_HEAP_ARENA_SIZE_MB=512
```

### Application Configuration

**application-prod.yml:**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/scalebiometrics
    username: scalebiometrics
    password: ${SPRING_DATASOURCE_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
  redis:
    host: ${SPRING_REDIS_HOST}
    port: ${SPRING_REDIS_PORT}
    timeout: 2000ms
  kafka:
    bootstrap-servers: ${SPRING_KAFKA_BOOTSTRAP_SERVERS}
    producer:
      acks: all
      retries: 3
    consumer:
      group-id: scalebiometrics
      auto-offset-reset: earliest

server:
  port: 8080
  servlet:
    context-path: /api

management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

---

## Verification

### Health Checks

```bash
# API health
curl http://localhost:8080/api/v1/health

# Master health
curl http://localhost:9091/health

# Worker health
curl http://localhost:9092/health

# Prometheus metrics
curl http://localhost:8080/actuator/metrics
```

### Functional Tests

```bash
# Upload fingerprint
curl -X POST http://localhost:8080/api/v1/fingerprints \
  -H "Content-Type: application/json" \
  -d '{
    "rid": "RID001",
    "fingerIndex": 1,
    "template": "...",
    "embedding": [...]
  }'

# 1:N matching
curl -X POST http://localhost:8080/api/v1/matching/1n \
  -H "Content-Type: application/json" \
  -d '{
    "probeRid": "PROBE001",
    "topK": 10,
    "threshold": 40
  }'

# 1:1 verification
curl -X POST http://localhost:8080/api/v1/matching/1to1 \
  -H "Content-Type: application/json" \
  -d '{
    "probeRid": "PROBE001",
    "targetRid": "RID001"
  }'
```

### Performance Tests

```bash
# Run load tests
mvn -f apps/api/pom.xml test -Dtest=PerformanceTest

# Run integration tests
mvn -f apps/master/pom.xml test -Dtest=MasterOrchestratorIntegrationTest

# Run worker tests
mvn -f apps/worker/pom.xml test -Dtest=MatchingEngineIntegrationTest
```

---

## Troubleshooting

### Common Issues

**1. Database Connection Failed**
```
Error: org.postgresql.util.PSQLException: Connection refused

Solution:
- Verify PostgreSQL is running
- Check database credentials
- Verify network connectivity
```

**2. Redis Connection Failed**
```
Error: io.lettuce.core.RedisConnectionException: Unable to connect

Solution:
- Verify Redis is running
- Check Redis port (default: 6379)
- Verify network connectivity
```

**3. gRPC Connection Failed**
```
Error: io.grpc.StatusRuntimeException: UNAVAILABLE

Solution:
- Verify Master/Worker are running
- Check gRPC ports (9091 for Master, 9092 for Worker)
- Verify network connectivity
```

**4. High Latency**
```
Error: Matching latency > 2000ms

Solution:
- Scale up workers
- Check HNSW index size
- Monitor CPU/memory usage
- Check network latency
```

**5. Out of Memory**
```
Error: java.lang.OutOfMemoryError

Solution:
- Increase JVM heap size
- Reduce HNSW index size
- Enable off-heap memory
- Scale up to more workers
```

### Debug Mode

```bash
# Enable debug logging
export LOGGING_LEVEL_COM_SCALEBIOMETRICS=DEBUG

# Run with debug output
mvn spring-boot:run -Dspring-boot.run.arguments="--debug"

# View detailed logs
docker-compose logs -f --tail=100 api
```

---

## Conclusion

Follow this guide to deploy ScaleBiometrics in development, Docker, or Kubernetes environments. Ensure all prerequisites are met and verify the deployment with health checks and functional tests.

---

**Document Version:** 1.0  
**Last Updated:** 2025-12-25  
**Next Review:** 2026-03-25
