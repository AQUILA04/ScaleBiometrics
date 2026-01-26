# Architecture Document: ScaleBiometrics SaaS Platform

**Version:** 3.0 (High Performance & Scalability Hardening)

## 1. High Level Architecture

ScaleBiometrics is a Distributed, Event-Driven Platform designed for massive scale biometric identification.
To support 10M+ records with <2s latency, it utilizes a **Hybrid Matching Engine** combining Approximate Nearest Neighbor (ANN) search with Exact Biometric Matching.

### High Level Diagram
```mermaid
graph TD
    Client[Client System] -->|REST/gRPC/Kafka| Gateway[API Gateway]
    Gateway -->|AuthZ| KC[Keycloak]
    Gateway -->|Images| MinIO[Object Store]
    Gateway -->|Metadata| PG[(PostgreSQL)]
    
    Gateway -->|Job Request| Kafka[Kafka Event Bus]
    
    subgraph "Matching Engine Cluster"
        Orchestrator[Master Orchestrator]
        Kafka --> Orchestrator
        
        Orchestrator -->|Scatter (gRPC)| Worker1
        Orchestrator -->|Scatter (gRPC)| Worker2
        
        subgraph "Worker Internals"
            W_ANN[ANN Index (HNSW) - Off-Heap]
            W_Exact[SourceAFIS Matcher]
            W_Cache[Template Cache - Off-Heap]
        end
    end
    
    Orchestrator -->|Result| Gateway
    
    subgraph "Ops & Observability"
        Otel[OpenTelemetry Collector]
        Prom[Prometheus]
        Grafana[Grafana]
    end
```

## 2. Technology Stack & Tuning

| Category | Technology | Version | Purpose | Tuning Note |
| --- | --- | --- | --- | --- |
| **Language** | Java | 21 (LTS) | Runtime | **Panama API** for Off-Heap memory access. Virtual Threads for I/O. |
| **Framework** | Spring Boot | 3.2+ | Core | Native Image compilation capability for Workers (faster startup). |
| **ANN Index** | HNSW (Java) | Latest | Pre-filtering | Reduces search space from O(N) to O(log N). |
| **Exact Match** | SourceAFIS | Latest | Verification | CPU-heavy verification on top-K candidates only. |
| **Messaging** | Apache Kafka | 3.6+ | Event Bus | Partitions based on `tenant_id` hash for locality. |
| **RPC** | gRPC | 1.60+ | Internal Comm | **Protobuf** serialization for minimal overhead. |
| **DB** | PostgreSQL | 16+ | Metadata | **Schema-per-Tenant**. JSONB for flexible metadata. |
| **Storage** | MinIO | Latest | Images | High-performance binary object storage. |
| **Memory** | MemorySegment | Java 21 | Off-Heap Store | Store binary templates outside Java Heap to prevent GC pauses. |

## 3. Core Component Design (Hardened)

### 3.1 Matcher Worker (The Performance Engine)

* **Hybrid Pipeline:**
1. **Step 1 (Coarse Search):** Query In-Memory **HNSW Index** (Off-Heap) to find Top-500 candidates. Complexity: .
2. **Step 2 (Fine Match):** Load full templates for these 500 candidates from Off-Heap Cache. Run `SourceAFIS` exact match. Complexity:  where .
3. **Step 3 (Early Exit):** If any Exact Match Score > Threshold, stop and return immediately.


* **Memory Management:**
* **Off-Heap:** Templates and HNSW Graph stored in `java.lang.foreign.MemorySegment` (Arena).
* **Heap:** Only holds request/response objects and transient matching structures.


* **Autoscaling Strategy:**
* Scale Out Trigger: `worker_cpu_usage > 70%` OR `grpc_queue_depth > 100`.
* Scale In Trigger: `worker_cpu_usage < 30%` (with cooldown).



### 3.2 Master Orchestrator (Reliability)

* **Leader Election:** Stateless for MVP (Kafka Consumer Groups handle load balancing).
* **Scatter-Gather:** Uses `StructuredTaskScope` (Java 21) to parallelize gRPC calls to shards with a timeout (e.g., 1.5s).
* **Circuit Breaker:** If a Worker shard is down, Master marks shard as "Degraded", returns partial results with a warning flag, and triggers alert.

### 3.3 Tenant Onboarding Orchestrator (Saga)

A dedicated service handles the complexity of creating a tenant transactionally:

1. Create Tenant Record (Pending).
2. Provision PostgreSQL Schema (Liquibase).
3. Create Keycloak Client (Admin API) with Service Account.
4. Create MinIO Bucket + Policies.
5. Mark Tenant Active.

* **Failure:** Compensating transactions (Rollback created resources) if any step fails.

## 4. Security Architecture

### 4.1 MVP Model (Perimeter Security)

* **External:** HTTPS + JWT (Strict validation via Keycloak). API Gateway acts as Policy Enforcement Point (PEP).
* **Internal:** Plaintext gRPC in Private VPC (Private Subnets).
* **Network:** Security Groups deny all non-whitelisted traffic. Workers have NO internet access.

### 4.2 Production Hardening Path (Post-MVP)

* **Service Mesh:** Introduce Istio or Linkerd to transparently upgrade internal gRPC to **mTLS** without code changes.
* **Secrets:** Migrate `.env` files to **HashiCorp Vault** with rotation.

## 5. Data Architecture & Sharding

* **Sharding Strategy:**
* **Small/Medium Tenants:** All data on one shard (assigned by `hash(tenant_id)`).
* **Giant Tenants (>5M):** Internal sharding. `shard_key = hash(rid) % N_shards`. Master queries all N shards.


* **Schema Design (Multi-Tenant):**
* `public.tenants`: Registry with `shard_strategy`, `status`.
* `tenant_{uuid}.identities`: `(rid PK, metadata JSONB, created_at)`.
* `tenant_{uuid}.templates`: `(id PK, rid FK, finger_index, embedding_vector, binary_template_ref)`.



## 6. Observability & SLOs

* **SLOs (Service Level Objectives):**
* **Latency:** 95% of 1:N requests < 2s.
* **Availability:** 99.9% uptime per tenant.


* **Key Metrics:**
* `match_latency_p95{tenant}`
* `worker_offheap_usage_bytes`
* `hnsw_index_size`
* `kafka_consumer_lag`


* **Tracing:** OpenTelemetry (TraceId) propagated via Kafka Headers and gRPC Metadata.
