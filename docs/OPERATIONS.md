# ScaleBiometrics - Operations Guide

**Version:** 1.0  
**Date:** 2025-12-25  
**Status:** Production Ready

---

## Table of Contents

1. [Monitoring & Alerting](#monitoring--alerting)
2. [Scaling Operations](#scaling-operations)
3. [Maintenance Procedures](#maintenance-procedures)
4. [Incident Response](#incident-response)
5. [Performance Tuning](#performance-tuning)
6. [Backup & Recovery](#backup--recovery)

---

## Monitoring & Alerting

### Key Metrics to Monitor

**Matching Engine:**
```
matching_1n_total              - Total 1:N requests
matching_1to1_total            - Total 1:1 requests
matching_latency               - Latency histogram (p50, p95, p99)
matching_errors_total          - Total errors
index_total_vectors            - Total vectors in HNSW index
index_size_bytes               - HNSW index size
off_heap_used_bytes            - Off-heap memory usage
```

**Master Orchestrator:**
```
workers_healthy                - Number of healthy workers
workers_unhealthy              - Number of unhealthy workers
circuit_breaker_state          - Circuit breaker state (0=CLOSED, 1=OPEN)
worker_requests_pending        - Pending requests per worker
worker_response_time_ms        - Response time per worker
cache_hits_total               - Cache hits
cache_misses_total             - Cache misses
deduplication_detected_total   - Duplicate requests detected
failover_events_total          - Failover events
```

**Infrastructure:**
```
db_connection_pool_active      - Active database connections
redis_memory_used_bytes        - Redis memory usage
kafka_producer_record_send_total_lag_sum - Kafka lag
node_filesystem_avail_bytes    - Disk space available
```

### Alert Thresholds

**Critical Alerts:**
- P99 Latency > 2000ms
- No active master instances
- Database connection pool exhausted (> 90%)
- Availability < 99.9%

**Warning Alerts:**
- P95 Latency > 1500ms
- Error rate > 0.01 errors/sec
- Worker health < 80%
- Cache miss rate > 50%
- Disk space < 10%

**Info Alerts:**
- Throughput < 500 req/sec
- Failover events > 0.1/sec
- Index growth > 100k vectors/sec

### Dashboard Access

**Grafana:**
- URL: http://localhost:3001
- Default Credentials: admin/admin
- Dashboards:
  - Matching Engine
  - Master Orchestrator
  - Infrastructure

**Prometheus:**
- URL: http://localhost:9090
- Query Examples:
  ```
  # P95 Latency
  histogram_quantile(0.95, rate(matching_latency_bucket[5m]))
  
  # Error Rate
  rate(matching_errors_total[5m])
  
  # Worker Health
  workers_healthy / workers_total
  ```

**Jaeger:**
- URL: http://localhost:16686
- Trace Services:
  - scalebiometrics-api
  - scalebiometrics-master
  - scalebiometrics-worker

**Kibana:**
- URL: http://localhost:5601
- Index Patterns:
  - scalebiometrics-*
  - scalebiometrics-errors-*
  - scalebiometrics-performance-*

---

## Scaling Operations

### Horizontal Scaling

**Scale Up Workers:**

```bash
# Docker Compose
docker-compose -f infrastructure/local/docker-compose.yml up -d --scale worker=10

# Kubernetes
kubectl scale deployment worker --replicas=10 -n scalebiometrics

# Verify
kubectl get pods -n scalebiometrics | grep worker
```

**Scale Up Masters:**

```bash
# Kubernetes
kubectl scale deployment master --replicas=5 -n scalebiometrics

# Verify
kubectl get pods -n scalebiometrics | grep master
```

**Scale Up API:**

```bash
# Kubernetes
kubectl scale deployment api --replicas=5 -n scalebiometrics

# Verify
kubectl get pods -n scalebiometrics | grep api
```

### Auto-Scaling Configuration

**Kubernetes HPA:**

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: worker-hpa
  namespace: scalebiometrics
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: worker
  minReplicas: 10
  maxReplicas: 100
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

### Load Testing

**JMeter Test Plan:**

```bash
# Install JMeter
brew install jmeter

# Run load test
jmeter -n -t load-test.jmx -l results.jtl -j jmeter.log

# Analyze results
jmeter -g results.jtl -o report
```

**Apache Bench:**

```bash
# 1000 requests, 100 concurrent
ab -n 1000 -c 100 http://localhost:8080/api/v1/health

# With POST data
ab -n 1000 -c 100 -p data.json -T application/json \
  http://localhost:8080/api/v1/matching/1n
```

---

## Maintenance Procedures

### Rolling Updates

**Update API:**

```bash
# Kubernetes rolling update
kubectl set image deployment/api api=registry.example.com/scalebiometrics/api:1.0.1 \
  -n scalebiometrics

# Monitor rollout
kubectl rollout status deployment/api -n scalebiometrics

# Rollback if needed
kubectl rollout undo deployment/api -n scalebiometrics
```

**Update Master:**

```bash
# Kubernetes rolling update
kubectl set image deployment/master master=registry.example.com/scalebiometrics/master:1.0.1 \
  -n scalebiometrics

# Monitor rollout
kubectl rollout status deployment/master -n scalebiometrics
```

**Update Worker:**

```bash
# Kubernetes rolling update
kubectl set image deployment/worker worker=registry.example.com/scalebiometrics/worker:1.0.1 \
  -n scalebiometrics

# Monitor rollout
kubectl rollout status deployment/worker -n scalebiometrics
```

### Database Maintenance

**Vacuum & Analyze:**

```sql
-- Connect to PostgreSQL
psql -U scalebiometrics -d scalebiometrics

-- Vacuum
VACUUM ANALYZE;

-- Check table sizes
SELECT schemaname, tablename, pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) 
FROM pg_tables 
WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

**Index Maintenance:**

```sql
-- Reindex
REINDEX DATABASE scalebiometrics;

-- Check index size
SELECT schemaname, tablename, indexname, pg_size_pretty(pg_relation_size(indexrelid))
FROM pg_indexes
WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
ORDER BY pg_relation_size(indexrelid) DESC;
```

### Redis Maintenance

**Memory Management:**

```bash
# Connect to Redis
redis-cli

# Check memory usage
INFO memory

# Clear expired keys
MEMORY DOCTOR

# Eviction policy
CONFIG GET maxmemory-policy
CONFIG SET maxmemory-policy allkeys-lru
```

**Persistence:**

```bash
# Enable AOF
CONFIG SET appendonly yes

# Rewrite AOF
BGREWRITEAOF

# Create snapshot
BGSAVE

# Check last save
LASTSAVE
```

### Certificate Renewal

**TLS Certificates:**

```bash
# Generate self-signed certificate (development)
openssl req -x509 -newkey rsa:4096 -keyout key.pem -out cert.pem -days 365

# Generate certificate signing request (production)
openssl req -new -key key.pem -out csr.pem

# Sign with CA
openssl x509 -req -in csr.pem -CA ca.pem -CAkey ca-key.pem -out cert.pem -days 365

# Update Kubernetes secret
kubectl create secret tls tls-secret --cert=cert.pem --key=key.pem \
  -n scalebiometrics --dry-run=client -o yaml | kubectl apply -f -
```

---

## Incident Response

### Critical Incident: No Active Master

**Symptoms:**
- Alert: NoActiveMaster (CRITICAL)
- All requests failing with "No active master"
- Master pods not responding

**Response Steps:**

1. **Immediate Actions:**
   ```bash
   # Check master pod status
   kubectl get pods -n scalebiometrics | grep master
   
   # View master logs
   kubectl logs -f deployment/master -n scalebiometrics
   
   # Check Redis connection
   redis-cli ping
   ```

2. **Diagnosis:**
   ```bash
   # Check leader election status
   redis-cli GET master:leader:id
   
   # Check master health
   curl http://master-pod:9091/health
   
   # Check network connectivity
   kubectl exec -it master-pod -n scalebiometrics -- ping redis
   ```

3. **Recovery:**
   ```bash
   # Restart master pods
   kubectl rollout restart deployment/master -n scalebiometrics
   
   # Wait for leader election
   sleep 30
   
   # Verify recovery
   curl http://localhost:8080/api/v1/health
   ```

### Critical Incident: High Latency

**Symptoms:**
- Alert: HighMatchingLatency (CRITICAL)
- P95 latency > 1500ms
- P99 latency > 2000ms

**Response Steps:**

1. **Immediate Actions:**
   ```bash
   # Check worker status
   kubectl get pods -n scalebiometrics | grep worker
   
   # Check worker metrics
   curl http://worker-pod:9092/actuator/metrics
   
   # Check load distribution
   curl http://master-pod:9091/api/ha/load-balancing/metrics
   ```

2. **Diagnosis:**
   ```bash
   # Check HNSW index size
   curl http://worker-pod:9092/actuator/metrics/index.total.vectors
   
   # Check off-heap memory
   curl http://worker-pod:9092/actuator/metrics/off.heap.used.bytes
   
   # Check pending requests
   curl http://master-pod:9091/api/advanced/load-balancing/workers
   ```

3. **Recovery:**
   ```bash
   # Scale up workers
   kubectl scale deployment worker --replicas=20 -n scalebiometrics
   
   # Monitor latency
   watch -n 5 'curl http://localhost:8080/actuator/metrics/matching.latency'
   
   # Verify recovery
   kubectl get pods -n scalebiometrics | grep worker | wc -l
   ```

### Critical Incident: Database Connection Pool Exhausted

**Symptoms:**
- Alert: DatabaseConnectionPoolExhausted (CRITICAL)
- Error: "Cannot get a connection, pool error"

**Response Steps:**

1. **Immediate Actions:**
   ```bash
   # Check connection pool status
   curl http://api-pod:8080/actuator/metrics/db.connection.pool.active
   
   # Check database connections
   psql -U scalebiometrics -d scalebiometrics -c "SELECT count(*) FROM pg_stat_activity;"
   
   # View active queries
   psql -U scalebiometrics -d scalebiometrics -c "SELECT * FROM pg_stat_activity WHERE state='active';"
   ```

2. **Diagnosis:**
   ```bash
   # Check slow queries
   psql -U scalebiometrics -d scalebiometrics -c "SELECT * FROM pg_stat_statements ORDER BY total_time DESC LIMIT 10;"
   
   # Check connection idle time
   psql -U scalebiometrics -d scalebiometrics -c "SELECT * FROM pg_stat_activity WHERE state='idle' AND query_start < now() - interval '10 minutes';"
   ```

3. **Recovery:**
   ```bash
   # Increase connection pool size
   kubectl set env deployment/api SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=30 -n scalebiometrics
   
   # Kill idle connections
   psql -U scalebiometrics -d scalebiometrics -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE state='idle' AND query_start < now() - interval '10 minutes';"
   
   # Restart API pods
   kubectl rollout restart deployment/api -n scalebiometrics
   ```

---

## Performance Tuning

### HNSW Index Tuning

**Parameters:**

```yaml
# Current settings
M: 16                    # Connectivity (higher = more accurate, slower)
efConstruction: 200      # Construction parameter
efSearch: 100            # Search parameter

# Tuning recommendations
# For 10M vectors:
M: 16-32                 # Increase for better accuracy
efConstruction: 200-400  # Increase for better index quality
efSearch: 100-200        # Increase for better search accuracy

# For 100M vectors:
M: 8-16                  # Decrease for memory efficiency
efConstruction: 100-200  # Decrease for faster construction
efSearch: 50-100         # Decrease for faster search
```

**Configuration:**

```yaml
# application-prod.yml
hnsw:
  m: 16
  ef-construction: 200
  ef-search: 100
  max-size-gb: 8
  enable-persistence: true
  persistence-path: /data/hnsw-index
```

### Off-Heap Memory Tuning

**Parameters:**

```yaml
# Current settings
max-size-gb: 8           # Maximum off-heap memory
arena-size-mb: 512       # Arena allocation unit

# Tuning recommendations
# For 10M vectors:
max-size-gb: 8-16        # Increase for more vectors
arena-size-mb: 512-1024  # Increase for better allocation

# For 100M vectors:
max-size-gb: 32-64       # Increase significantly
arena-size-mb: 2048      # Increase for large allocations
```

**Configuration:**

```yaml
# application-prod.yml
off-heap:
  max-size-gb: 16
  arena-size-mb: 1024
  enable-monitoring: true
```

### Cache Tuning

**Parameters:**

```yaml
# Current settings
ttl-seconds: 3600        # Cache TTL
max-size: 10000          # Maximum cache entries
hit-rate-target: 0.80    # Target hit rate

# Tuning recommendations
# For high hit rate:
ttl-seconds: 7200        # Increase TTL
max-size: 50000          # Increase cache size
eviction-policy: LRU     # Use LRU eviction

# For memory efficiency:
ttl-seconds: 1800        # Decrease TTL
max-size: 5000           # Decrease cache size
eviction-policy: LFU     # Use LFU eviction
```

**Configuration:**

```yaml
# application-prod.yml
cache:
  ttl-seconds: 7200
  max-size: 50000
  eviction-policy: LRU
  enable-statistics: true
```

### Load Balancing Tuning

**Parameters:**

```yaml
# Current settings
strategy: LEAST_LOADED   # Load balancing strategy
overload-threshold: 1000 # Pending requests threshold
slow-response-threshold-ms: 500  # Response time threshold

# Tuning recommendations
# For even distribution:
strategy: ROUND_ROBIN    # Simple round-robin
overload-threshold: 500  # Lower threshold

# For optimal latency:
strategy: LEAST_LOADED   # Least loaded strategy
overload-threshold: 100  # Very low threshold
slow-response-threshold-ms: 200  # Strict threshold
```

**Configuration:**

```yaml
# application-prod.yml
load-balancing:
  strategy: LEAST_LOADED
  overload-threshold: 100
  slow-response-threshold-ms: 200
  enable-metrics: true
```

---

## Backup & Recovery

### Database Backup

**Daily Backup:**

```bash
#!/bin/bash
# backup-db.sh

BACKUP_DIR="/backups/postgresql"
DB_NAME="scalebiometrics"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Create backup
pg_dump -U scalebiometrics -d $DB_NAME | gzip > $BACKUP_DIR/db_$TIMESTAMP.sql.gz

# Upload to S3
aws s3 cp $BACKUP_DIR/db_$TIMESTAMP.sql.gz s3://backups/scalebiometrics/

# Cleanup old backups (keep 30 days)
find $BACKUP_DIR -name "db_*.sql.gz" -mtime +30 -delete

echo "Backup completed: db_$TIMESTAMP.sql.gz"
```

**Restore from Backup:**

```bash
# Restore database
gunzip -c /backups/postgresql/db_20251225_120000.sql.gz | psql -U scalebiometrics -d scalebiometrics

# Verify restore
psql -U scalebiometrics -d scalebiometrics -c "SELECT count(*) FROM fingerprints;"
```

### HNSW Index Backup

**Snapshot to MinIO:**

```bash
#!/bin/bash
# backup-index.sh

WORKER_POD="worker-0"
NAMESPACE="scalebiometrics"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Create snapshot
kubectl exec -it $WORKER_POD -n $NAMESPACE -- \
  curl -X POST http://localhost:9092/api/worker/index/snapshot

# Upload to MinIO
kubectl exec -it $WORKER_POD -n $NAMESPACE -- \
  mc cp /data/hnsw-index/snapshot_$TIMESTAMP.tar.gz \
  minio/backups/scalebiometrics/

echo "Index snapshot completed: snapshot_$TIMESTAMP.tar.gz"
```

**Restore Index:**

```bash
# Download from MinIO
kubectl exec -it worker-0 -n scalebiometrics -- \
  mc cp minio/backups/scalebiometrics/snapshot_20251225_120000.tar.gz /data/

# Extract snapshot
kubectl exec -it worker-0 -n scalebiometrics -- \
  tar -xzf /data/snapshot_20251225_120000.tar.gz -C /data/hnsw-index/

# Restart worker
kubectl rollout restart deployment/worker -n scalebiometrics
```

---

## Conclusion

Follow these operational procedures to maintain, scale, and troubleshoot ScaleBiometrics in production. Regularly monitor key metrics and be prepared for incident response.

---

**Document Version:** 1.0  
**Last Updated:** 2025-12-25  
**Next Review:** 2026-03-25
