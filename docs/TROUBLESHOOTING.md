# ScaleBiometrics - Troubleshooting Guide

**Version:** 1.0  
**Date:** 2025-12-25  
**Status:** Production Ready

---

## Table of Contents

1. [Common Issues](#common-issues)
2. [Performance Issues](#performance-issues)
3. [Network Issues](#network-issues)
4. [Database Issues](#database-issues)
5. [Monitoring Issues](#monitoring-issues)
6. [Debugging Techniques](#debugging-techniques)

---

## Common Issues

### Issue: Application Won't Start

**Error:**
```
Exception in thread "main" java.lang.UnsupportedClassVersionError: 
Unsupported major.minor version 21.0
```

**Cause:** Java version mismatch

**Solution:**
```bash
# Verify Java version
java -version

# Should output Java 21
# If not, install Java 21
brew install openjdk@21  # macOS
sudo apt-get install openjdk-21-jdk  # Ubuntu

# Set JAVA_HOME
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
```

---

### Issue: Port Already in Use

**Error:**
```
Address already in use: bind
Port: 8080
```

**Cause:** Another application using the port

**Solution:**
```bash
# Find process using port
lsof -i :8080

# Kill process
kill -9 <PID>

# Or use different port
export SERVER_PORT=8081
mvn spring-boot:run
```

---

### Issue: Database Connection Failed

**Error:**
```
org.postgresql.util.PSQLException: Connection refused. 
Check that the hostname and port are correct and that the postmaster is accepting TCP/IP connections.
```

**Cause:** PostgreSQL not running or wrong credentials

**Solution:**
```bash
# Check if PostgreSQL is running
docker ps | grep postgres

# Start PostgreSQL
docker-compose -f infrastructure/local/docker-compose.yml up -d postgres

# Verify connection
psql -U scalebiometrics -d scalebiometrics -h localhost

# Check credentials in application.yml
cat apps/api/src/main/resources/application-dev.yml | grep datasource
```

---

### Issue: Redis Connection Failed

**Error:**
```
io.lettuce.core.RedisConnectionException: Unable to connect to localhost:6379
```

**Cause:** Redis not running or wrong port

**Solution:**
```bash
# Check if Redis is running
docker ps | grep redis

# Start Redis
docker-compose -f infrastructure/local/docker-compose.yml up -d redis

# Test connection
redis-cli ping
# Should return: PONG

# Check Redis configuration
redis-cli CONFIG GET port
```

---

### Issue: gRPC Connection Failed

**Error:**
```
io.grpc.StatusRuntimeException: UNAVAILABLE: 
io.grpc.netty.NettyChannelBuilder$NettyChannelTransportException: 
Failed to connect to localhost/127.0.0.1:9091
```

**Cause:** Master/Worker not running or wrong port

**Solution:**
```bash
# Check if Master is running
curl http://localhost:9091/health

# Check if Worker is running
curl http://localhost:9092/health

# Start Master
cd apps/master && mvn spring-boot:run

# Start Worker
cd apps/worker && mvn spring-boot:run

# Check port configuration
cat apps/master/src/main/resources/application-dev.yml | grep port
cat apps/worker/src/main/resources/application-dev.yml | grep port
```

---

## Performance Issues

### Issue: High Latency (> 2000ms)

**Symptoms:**
- Matching requests taking > 2 seconds
- P95/P99 latency alerts triggered
- Users reporting slow responses

**Diagnosis:**

```bash
# Check latency metrics
curl http://localhost:8080/actuator/metrics/matching.latency

# Check worker status
curl http://localhost:9091/api/ha/health

# Check load distribution
curl http://localhost:9091/api/advanced/load-balancing/metrics

# Check HNSW index size
curl http://localhost:9092/actuator/metrics/index.total.vectors

# Check off-heap memory
curl http://localhost:9092/actuator/metrics/off.heap.used.bytes

# View Grafana dashboard
# http://localhost:3001 → Matching Engine → Latency Distribution
```

**Solutions:**

1. **Scale up workers:**
   ```bash
   docker-compose -f infrastructure/local/docker-compose.yml up -d --scale worker=10
   ```

2. **Reduce HNSW index size:**
   ```bash
   # Archive old fingerprints
   DELETE FROM fingerprints WHERE created_at < NOW() - INTERVAL '1 year';
   ```

3. **Increase off-heap memory:**
   ```yaml
   # application-prod.yml
   off-heap:
     max-size-gb: 16  # Increase from 8
   ```

4. **Optimize HNSW parameters:**
   ```yaml
   # application-prod.yml
   hnsw:
     ef-search: 50    # Decrease from 100 for faster search
     m: 8             # Decrease from 16 for faster construction
   ```

5. **Check network latency:**
   ```bash
   # Ping between services
   kubectl exec -it api-pod -n scalebiometrics -- ping master-pod
   
   # Check network policy
   kubectl get networkpolicies -n scalebiometrics
   ```

---

### Issue: Low Throughput (< 500 req/sec)

**Symptoms:**
- Matching throughput below target
- Alert: MatchingThroughputLow triggered
- System not handling expected load

**Diagnosis:**

```bash
# Check throughput metrics
curl http://localhost:8080/actuator/metrics/matching.1n.total

# Check error rate
curl http://localhost:8080/actuator/metrics/matching.errors.total

# Check worker pending requests
curl http://localhost:9091/api/advanced/load-balancing/workers

# Check circuit breaker state
curl http://localhost:9091/api/ha/health | grep circuit_breaker
```

**Solutions:**

1. **Scale up workers:**
   ```bash
   kubectl scale deployment worker --replicas=20 -n scalebiometrics
   ```

2. **Scale up API instances:**
   ```bash
   kubectl scale deployment api --replicas=5 -n scalebiometrics
   ```

3. **Check for errors:**
   ```bash
   # View error logs
   kubectl logs -f deployment/worker -n scalebiometrics | grep ERROR
   
   # Check Kibana for errors
   # http://localhost:5601 → scalebiometrics-errors-*
   ```

4. **Increase connection pool:**
   ```yaml
   # application-prod.yml
   spring:
     datasource:
       hikari:
         maximum-pool-size: 30  # Increase from 20
   ```

---

### Issue: Out of Memory

**Symptoms:**
- Error: `java.lang.OutOfMemoryError: Java heap space`
- Pod getting killed by Kubernetes
- Sudden latency spike

**Diagnosis:**

```bash
# Check memory usage
kubectl top pods -n scalebiometrics

# Check JVM memory
curl http://localhost:9092/actuator/metrics/jvm.memory.used

# Check off-heap memory
curl http://localhost:9092/actuator/metrics/off.heap.used.bytes

# View memory logs
kubectl logs -f deployment/worker -n scalebiometrics | grep -i memory
```

**Solutions:**

1. **Increase JVM heap size:**
   ```yaml
   # Kubernetes deployment
   env:
   - name: JAVA_OPTS
     value: "-Xms2g -Xmx4g"
   ```

2. **Reduce HNSW index size:**
   ```bash
   # Archive old fingerprints
   DELETE FROM fingerprints WHERE created_at < NOW() - INTERVAL '6 months';
   ```

3. **Enable off-heap memory:**
   ```yaml
   # application-prod.yml
   off-heap:
     enabled: true
     max-size-gb: 16
   ```

4. **Scale to more workers:**
   ```bash
   kubectl scale deployment worker --replicas=30 -n scalebiometrics
   ```

---

## Network Issues

### Issue: DNS Resolution Failed

**Error:**
```
java.net.UnknownHostException: postgres: Name or service not known
```

**Cause:** DNS not resolving hostname

**Solution:**
```bash
# Check DNS resolution
nslookup postgres
dig postgres

# Check /etc/hosts
cat /etc/hosts

# For Kubernetes, check service DNS
kubectl get svc -n scalebiometrics
kubectl exec -it api-pod -n scalebiometrics -- nslookup postgres

# Check CoreDNS
kubectl get pods -n kube-system | grep coredns
```

---

### Issue: Network Timeout

**Error:**
```
java.net.SocketTimeoutException: Read timed out
```

**Cause:** Network latency or service not responding

**Solution:**
```bash
# Check network connectivity
ping -c 5 localhost

# Check service availability
curl -v http://localhost:8080/api/v1/health

# Check network policy
kubectl get networkpolicies -n scalebiometrics

# Increase timeout in application
# application-prod.yml
spring:
  kafka:
    bootstrap-servers: kafka:9092
    consumer:
      session-timeout-ms: 30000  # Increase from 10000
```

---

## Database Issues

### Issue: Slow Queries

**Symptoms:**
- Database queries taking > 1 second
- High database CPU usage
- Connection pool exhausted

**Diagnosis:**

```bash
# Connect to database
psql -U scalebiometrics -d scalebiometrics

# Check slow queries
SELECT * FROM pg_stat_statements 
ORDER BY total_time DESC LIMIT 10;

# Check query plan
EXPLAIN ANALYZE SELECT * FROM fingerprints WHERE rid = 'RID001';

# Check table statistics
SELECT schemaname, tablename, n_live_tup, n_dead_tup 
FROM pg_stat_user_tables 
ORDER BY n_dead_tup DESC;
```

**Solutions:**

1. **Create indexes:**
   ```sql
   CREATE INDEX idx_fingerprints_rid ON fingerprints(rid);
   CREATE INDEX idx_fingerprints_tenant ON fingerprints(tenant_id);
   ```

2. **Vacuum and analyze:**
   ```sql
   VACUUM ANALYZE fingerprints;
   VACUUM ANALYZE matches;
   ```

3. **Increase work_mem:**
   ```sql
   ALTER SYSTEM SET work_mem = '256MB';
   SELECT pg_reload_conf();
   ```

---

### Issue: Connection Pool Exhausted

**Error:**
```
Cannot get a connection, pool error Timeout waiting for an idle object
```

**Cause:** Too many connections or slow queries

**Solution:**

```bash
# Check connection count
psql -U scalebiometrics -d scalebiometrics -c "SELECT count(*) FROM pg_stat_activity;"

# Check idle connections
psql -U scalebiometrics -d scalebiometrics -c "SELECT * FROM pg_stat_activity WHERE state='idle';"

# Kill idle connections
psql -U scalebiometrics -d scalebiometrics -c "
SELECT pg_terminate_backend(pid) 
FROM pg_stat_activity 
WHERE state='idle' 
AND query_start < now() - interval '10 minutes';"

# Increase connection pool
# application-prod.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 30  # Increase from 20
      minimum-idle: 5
```

---

## Monitoring Issues

### Issue: Prometheus Not Scraping Metrics

**Symptoms:**
- Grafana dashboards showing "No data"
- Prometheus targets showing "DOWN"

**Diagnosis:**

```bash
# Check Prometheus status
curl http://localhost:9090/api/v1/targets

# Check scrape config
curl http://localhost:9090/api/v1/status/config

# View Prometheus logs
docker logs prometheus
```

**Solutions:**

1. **Verify metrics endpoint:**
   ```bash
   curl http://localhost:8080/actuator/prometheus
   ```

2. **Check Prometheus configuration:**
   ```yaml
   # prometheus.yml
   scrape_configs:
   - job_name: 'scalebiometrics'
     static_configs:
     - targets: ['localhost:8080']
     metrics_path: '/actuator/prometheus'
   ```

3. **Restart Prometheus:**
   ```bash
   docker-compose -f infrastructure/local/docker-compose.yml restart prometheus
   ```

---

### Issue: Jaeger Traces Not Showing

**Symptoms:**
- Jaeger UI showing no traces
- Trace search returns empty results

**Diagnosis:**

```bash
# Check Jaeger status
curl http://localhost:16686/api/services

# Check trace count
curl http://localhost:16686/api/traces?service=scalebiometrics-api

# View Jaeger logs
docker logs jaeger
```

**Solutions:**

1. **Enable tracing in application:**
   ```yaml
   # application-prod.yml
   management:
     tracing:
       sampling:
         probability: 0.1  # 10% sampling
   ```

2. **Configure Jaeger exporter:**
   ```yaml
   # application-prod.yml
   management:
     otlp:
       tracing:
         endpoint: http://jaeger:14250
   ```

3. **Restart application:**
   ```bash
   kubectl rollout restart deployment/api -n scalebiometrics
   ```

---

## Debugging Techniques

### Enable Debug Logging

```bash
# Set log level to DEBUG
export LOGGING_LEVEL_COM_SCALEBIOMETRICS=DEBUG

# Run application
mvn spring-boot:run

# Or in Kubernetes
kubectl set env deployment/api LOGGING_LEVEL_COM_SCALEBIOMETRICS=DEBUG -n scalebiometrics
```

### View Application Logs

```bash
# Local
tail -f logs/application.log

# Docker
docker logs -f container_name

# Kubernetes
kubectl logs -f deployment/api -n scalebiometrics
kubectl logs -f deployment/api -n scalebiometrics --previous  # Previous crashed pod

# Kibana
# http://localhost:5601 → scalebiometrics-*
```

### Connect to Running Pod

```bash
# Execute command in pod
kubectl exec -it pod_name -n scalebiometrics -- /bin/bash

# Run debug probe
kubectl debug pod_name -n scalebiometrics -it --image=busybox

# Port forward for debugging
kubectl port-forward pod_name 5005:5005 -n scalebiometrics
```

### Profiling

```bash
# CPU profiling
jps  # Find Java process
jcmd <pid> JFR.start name=myrecording duration=60s

# Memory profiling
jcmd <pid> GC.heap_dump filename=heap.hprof

# Thread analysis
jcmd <pid> Thread.print
```

---

## Conclusion

Use this troubleshooting guide to diagnose and resolve common issues in ScaleBiometrics. Always check logs first, then monitor metrics, and finally apply solutions.

---

**Document Version:** 1.0  
**Last Updated:** 2025-12-25  
**Next Review:** 2026-03-25
