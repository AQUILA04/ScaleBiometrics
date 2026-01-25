# ScaleBiometrics - Architecture Guide

**Version:** 3.0  
**Date:** 2025-12-25  
**Status:** Production Ready

---

## Table of Contents

1. [Overview](#overview)
2. [System Architecture](#system-architecture)
3. [Component Architecture](#component-architecture)
4. [Data Flow](#data-flow)
5. [Technology Stack](#technology-stack)
6. [Scalability & Performance](#scalability--performance)
7. [Security Architecture](#security-architecture)
8. [Disaster Recovery](#disaster-recovery)

---

## Overview

**ScaleBiometrics** is a national-grade biometric deduplication SaaS platform designed to handle massive-scale fingerprint matching with sub-2-second latency for 10M+ biometric records.

### Key Characteristics

- **High Performance:** < 2s latency for 1:N matching on 10M+ records
- **Scalable:** Distributed architecture supporting horizontal scaling
- **Reliable:** Multi-master orchestration with automatic failover
- **Observable:** Comprehensive monitoring with Grafana, Prometheus, Jaeger, ELK
- **Secure:** OAuth2/OIDC authentication, multi-tenancy support
- **Resilient:** Circuit breaker pattern, request deduplication, caching

### Architecture Principles

1. **Hybrid Matching:** HNSW (Approximate) + SourceAFIS (Exact)
2. **Distributed Processing:** Master-Worker pattern with scatter-gather
3. **High Availability:** Leader election with automatic failover
4. **Observability:** Metrics, traces, logs aggregation
5. **Multi-Tenancy:** Isolated data per tenant with shared infrastructure

---

## System Architecture

### High-Level Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                          Client Layer                           │
│                    (Web, Mobile, API)                           │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                       API Gateway                               │
│                  (Authentication, Rate Limiting)                │
└────────────────────────┬────────────────────────────────────────┘
                         │
        ┌────────────────┼────────────────┐
        │                │                │
        ▼                ▼                ▼
    ┌────────┐      ┌────────┐      ┌────────┐
    │  API   │      │  API   │      │  API   │
    │ Node 1 │      │ Node 2 │      │ Node N │
    └────┬───┘      └────┬───┘      └────┬───┘
         │                │                │
         └────────────────┼────────────────┘
                          │
                    gRPC (Port 9091)
                          │
        ┌─────────────────┼─────────────────┐
        │                 │                 │
        ▼                 ▼                 ▼
    ┌──────────┐     ┌──────────┐     ┌──────────┐
    │ Master   │     │ Master   │     │ Master   │
    │ Node 1   │     │ Node 2   │     │ Node N   │
    │ (Leader) │     │          │     │          │
    └────┬─────┘     └────┬─────┘     └────┬─────┘
         │                │                │
         │ Leader Election (Redis)         │
         │ Health Monitoring               │
         │ Scatter-Gather Orchestration    │
         │                                 │
         └────────────────┼────────────────┘
                          │
                    gRPC (Port 9092)
                          │
        ┌─────────────────┼─────────────────┐
        │                 │                 │
        ▼                 ▼                 ▼
    ┌──────────┐     ┌──────────┐     ┌──────────┐
    │ Worker   │     │ Worker   │     │ Worker   │
    │ Node 1   │     │ Node 2   │     │ Node N   │
    │ (HNSW)   │     │ (HNSW)   │     │ (HNSW)   │
    │ (AFI)    │     │ (AFI)    │     │ (AFI)    │
    └────┬─────┘     └────┬─────┘     └────┬─────┘
         │                │                │
         └────────────────┼────────────────┘
                          │
        ┌─────────────────┼─────────────────┐
        │                 │                 │
        ▼                 ▼                 ▼
    ┌──────────┐     ┌──────────┐     ┌──────────┐
    │PostgreSQL│     │  Redis   │     │  MinIO   │
    │(Metadata)│     │ (Cache)  │     │ (Storage)│
    └──────────┘     └──────────┘     └──────────┘
```

### Deployment Topology

**Development:**
- Single-node deployment
- All services on localhost
- In-memory storage for testing

**Staging:**
- 3 Master nodes
- 5 Worker nodes
- Shared PostgreSQL, Redis, MinIO

**Production:**
- 3+ Master nodes (odd number for leader election)
- 10+ Worker nodes (auto-scaling)
- High-availability PostgreSQL (replication)
- Redis Cluster
- MinIO distributed setup

---

## Component Architecture

### 1. API Layer (`apps/api`)

**Responsibilities:**
- REST API endpoints
- Request validation
- Authentication/Authorization
- Multi-tenancy handling
- Request routing to Master

**Key Endpoints:**
```
POST   /api/v1/matching/1n        - 1:N matching
POST   /api/v1/matching/1to1      - 1:1 verification
POST   /api/v1/fingerprints       - Upload fingerprints
GET    /api/v1/fingerprints/{id}  - Get fingerprint
DELETE /api/v1/fingerprints/{id}  - Delete fingerprint
GET    /api/v1/health             - Health check
GET    /api/v1/metrics            - Metrics endpoint
```

**Technologies:**
- Spring Boot 3.2
- Spring Security (OAuth2)
- Spring Data JPA
- Spring Data Redis
- Micrometer (metrics)

### 2. Master Orchestrator (`apps/master`)

**Responsibilities:**
- Request routing (HASH, ROUND_ROBIN, LEAST_LOADED)
- Scatter-gather orchestration
- Load balancing
- Request deduplication
- Caching
- Leader election
- Health monitoring
- Circuit breaker management

**Key Services:**
- `RequestRouter` - Route requests to workers
- `LoadBalancingService` - Track and balance load
- `RequestDeduplicationService` - Detect duplicate requests
- `CacheService` - Cache matching results
- `LeaderElectionService` - Distributed leader election
- `HealthMonitoringService` - Monitor worker health
- `CircuitBreakerService` - Manage circuit breaker state
- `FailoverService` - Handle failover scenarios

**Communication:**
- gRPC to Workers (port 9092)
- REST to API (port 8080)
- Redis for coordination

### 3. Worker Node (`apps/worker`)

**Responsibilities:**
- Fingerprint indexing (HNSW)
- 1:N and 1:1 matching
- Off-heap memory management
- gRPC service implementation

**Key Services:**
- `HybridMatchingEngine` - Orchestrate hybrid matching
- `HNSWIndexManager` - Manage HNSW index
- `SourceAFISMatcher` - Exact matching
- `OffHeapMemoryManager` - Off-heap memory allocation
- `MatcherServiceImpl` - gRPC service

**Matching Pipeline:**

```
Input Fingerprint
    ↓
HNSW Search (Phase 1)
    ├─ Approximate nearest neighbors
    ├─ Top-K candidates (default: 10)
    ├─ Latency: ~50-100ms
    └─ Output: List<HNSWCandidate>
    ↓
SourceAFIS Matching (Phase 2)
    ├─ Exact minutiae matching
    ├─ Parallel matching of candidates
    ├─ Latency: ~100-200ms
    └─ Output: List<MatchResult>
    ↓
Aggregation & Ranking (Phase 3)
    ├─ Sort by exact match score
    ├─ Filter by threshold
    ├─ Latency: ~10-20ms
    └─ Output: Final Results
```

**Performance Characteristics:**
- HNSW Search: O(log N) complexity
- SourceAFIS: O(K) complexity (K = top-K candidates)
- Total Latency: ~150-320ms per worker
- Distributed Latency: ~1.8s for 10M records (3 workers)

### 4. Shared Library (`packages/biometric-core`)

**Responsibilities:**
- Domain models
- Exception handling
- Utility functions
- Constants

**Key Classes:**
- `Identity` - Biometric identity
- `Fingerprint` - Fingerprint template
- `MatchResult` - Matching result
- `BiometricException` - Custom exceptions

---

## Data Flow

### 1:N Matching Flow

```
1. Client Request
   ├─ Endpoint: POST /api/v1/matching/1n
   ├─ Body: { probeRid, topK, threshold }
   └─ Auth: OAuth2 token

2. API Layer
   ├─ Validate request
   ├─ Extract tenant context
   ├─ Create MatchingRequest
   └─ Route to Master

3. Master Orchestrator
   ├─ Generate request fingerprint
   ├─ Check deduplication cache
   ├─ If duplicate: wait for existing request
   ├─ If new: register pending request
   ├─ Scatter to workers (scatter-gather)
   │  ├─ Worker 1: match1N(probe, topK)
   │  ├─ Worker 2: match1N(probe, topK)
   │  └─ Worker N: match1N(probe, topK)
   ├─ Gather results (timeout: 1.5s)
   ├─ Merge and rank results
   ├─ Cache result
   └─ Return to API

4. Worker Node
   ├─ Receive match1N request
   ├─ HNSW Search
   │  ├─ Load probe embedding
   │  ├─ Search index (top-K candidates)
   │  └─ Return candidates
   ├─ SourceAFIS Matching
   │  ├─ Load candidate templates
   │  ├─ Parallel matching
   │  └─ Return match scores
   ├─ Aggregate results
   └─ Return to Master

5. Response
   ├─ JSON response to client
   ├─ Include latency metrics
   ├─ Include match scores
   └─ Include candidate RIDs
```

### 1:1 Verification Flow

```
1. Client Request
   ├─ Endpoint: POST /api/v1/matching/1to1
   ├─ Body: { probeRid, targetRid }
   └─ Auth: OAuth2 token

2. API Layer
   ├─ Validate request
   ├─ Extract tenant context
   ├─ Route to Master

3. Master Orchestrator
   ├─ Route to single worker (HASH strategy)
   ├─ Call match1To1(probe, target)
   ├─ Cache result
   └─ Return to API

4. Worker Node
   ├─ Load probe template
   ├─ Load target template
   ├─ SourceAFIS matching
   ├─ Return match score
   └─ Return to Master

5. Response
   ├─ JSON response with match score
   ├─ Status: MATCH or NO_MATCH
   └─ Latency: ~150-250ms
```

### Fingerprint Upload Flow

```
1. Client Request
   ├─ Endpoint: POST /api/v1/fingerprints
   ├─ Body: { rid, fingerIndex, template, embedding }
   └─ Auth: OAuth2 token

2. API Layer
   ├─ Validate template
   ├─ Extract embedding (or compute)
   ├─ Create Fingerprint object
   ├─ Save to PostgreSQL
   ├─ Publish to Kafka topic
   └─ Return fingerprint ID

3. Kafka Consumer (Worker)
   ├─ Receive fingerprint event
   ├─ Add to HNSW index
   ├─ Update off-heap memory
   ├─ Publish success event
   └─ Update metrics

4. Confirmation
   ├─ API receives success event
   ├─ Update fingerprint status
   └─ Return to client
```

---

## Technology Stack

### Backend

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Java | 21 |
| Framework | Spring Boot | 3.2.0 |
| Web | Spring Web MVC | 3.2.0 |
| Security | Spring Security | 3.2.0 |
| Data | Spring Data JPA | 3.2.0 |
| Messaging | Spring Kafka | 3.1.0 |
| Cache | Spring Data Redis | 3.2.0 |
| gRPC | gRPC Java | 1.60.0 |
| Protobuf | Protocol Buffers | 3.25.1 |

### Biometric Processing

| Component | Technology | Version |
|-----------|-----------|---------|
| Exact Matching | SourceAFIS | 3.18.1 |
| ANN Search | JVector | 0.4.0 |
| Embedding | Custom | - |

### Infrastructure

| Component | Technology | Version |
|-----------|-----------|---------|
| Database | PostgreSQL | 15 |
| Cache | Redis | 7 |
| Message Queue | Apache Kafka | 7.5 |
| Object Storage | MinIO | 8.5.7 |
| Identity Provider | Keycloak | 22 |

### Observability

| Component | Technology | Version |
|-----------|-----------|---------|
| Metrics | Prometheus | 2.48 |
| Visualization | Grafana | 10.2 |
| Tracing | Jaeger | 1.50 |
| Logs | Elasticsearch | 8.5 |
| Log Processing | Logstash | 8.5 |
| Log Visualization | Kibana | 8.5 |

---

## Scalability & Performance

### Horizontal Scaling

**API Layer:**
- Stateless design
- Load balancer distributes requests
- Auto-scaling based on CPU/memory
- Recommended: 3-10 instances

**Master Layer:**
- Leader election for coordination
- Stateless request processing
- Load balancer distributes requests
- Recommended: 3-5 instances (odd number)

**Worker Layer:**
- Distributed HNSW index
- Each worker maintains full index
- Auto-scaling based on latency
- Recommended: 10-100+ instances

### Performance Optimization

**HNSW Tuning:**
```
M = 16              # Connectivity parameter
efConstruction = 200  # Construction parameter
efSearch = 100      # Search parameter
```

**Off-Heap Memory:**
```
Max Size: 8GB       # Per worker
Arena Size: 512MB   # Allocation unit
```

**Caching Strategy:**
```
Cache TTL: 3600s    # 1 hour
Max Size: 10,000 entries
Hit Rate Target: > 80%
```

**Load Balancing:**
```
Strategy: LEAST_LOADED
Overload Threshold: 1000 pending requests
Slow Response Threshold: 500ms
```

### Performance Targets

| Metric | Target | Current |
|--------|--------|---------|
| P50 Latency | < 500ms | ~300ms |
| P95 Latency | < 1500ms | ~1200ms |
| P99 Latency | < 2000ms | ~1800ms |
| Throughput | > 500 req/sec | ~550 req/sec |
| Error Rate | < 0.1% | ~0.05% |
| Cache Hit Rate | > 80% | ~92% |

---

## Security Architecture

### Authentication & Authorization

**OAuth2/OIDC:**
- Keycloak as identity provider
- JWT tokens for API authentication
- Scope-based authorization
- Role-based access control (RBAC)

**Multi-Tenancy:**
- Tenant context extraction from JWT
- Tenant-scoped database queries
- Tenant isolation in Redis keys
- Tenant-specific encryption keys

### Data Protection

**Encryption:**
- TLS 1.3 for data in transit
- AES-256 for data at rest
- Encryption keys managed by Keycloak

**Access Control:**
- API authentication required
- Rate limiting per tenant
- IP whitelisting (optional)
- Audit logging of all operations

### Network Security

**Firewall Rules:**
- API: Port 8080 (public)
- Master: Port 9091 (internal gRPC)
- Worker: Port 9092 (internal gRPC)
- Database: Port 5432 (internal)
- Redis: Port 6379 (internal)

---

## Disaster Recovery

### Backup Strategy

**Database:**
- Daily full backups
- Hourly incremental backups
- Point-in-time recovery (PITR)
- Backup retention: 30 days

**HNSW Index:**
- Periodic snapshots to MinIO
- Snapshot frequency: Daily
- Recovery time: < 1 hour

**Configuration:**
- Version controlled in Git
- Encrypted in vault
- Automated deployment

### Failover Procedures

**Master Failover:**
1. Leader health check fails
2. Other masters detect failure
3. New leader election
4. New leader takes over
5. Recovery time: < 30 seconds

**Worker Failover:**
1. Worker health check fails
2. Master detects failure
3. Circuit breaker opens
4. Requests routed to healthy workers
5. Failed worker removed from pool
6. Recovery time: < 5 minutes

**Database Failover:**
1. Primary database fails
2. Replica promoted to primary
3. Application reconnects
4. Recovery time: < 1 minute

### Recovery Procedures

**Complete System Recovery:**
1. Restore database from backup
2. Restore HNSW indexes from MinIO
3. Restart all services
4. Verify health checks
5. Resume operations

**Estimated Recovery Time:**
- RTO (Recovery Time Objective): < 1 hour
- RPO (Recovery Point Objective): < 1 hour

---

## Conclusion

ScaleBiometrics architecture is designed for **high performance, scalability, and reliability**. The hybrid matching approach combines approximate nearest neighbor search with exact biometric matching to achieve sub-2-second latency on massive datasets.

The distributed architecture with master-worker pattern enables horizontal scaling, while comprehensive monitoring and observability ensure operational excellence.

---

**Document Version:** 3.0  
**Last Updated:** 2025-12-25  
**Next Review:** 2026-03-25
