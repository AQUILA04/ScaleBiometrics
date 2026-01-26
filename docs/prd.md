# Product Requirements Document (PRD)

**Title:** ScaleBiometrics SaaS Platform
**Version:** 2.0 (High Performance & Operations)
**Status:** Approved

## 1. Goals and Background Context

### Goals
* **Performance:** Achieve 1:N deduplication matching in under **2 seconds (P95)** for databases up to **10M records**.
* **Throughput:** Support **100 concurrent requests/sec** per scaling unit.
* **Scalability:** Leverage Java 21 (Panama/Virtual Threads) and distributed worker nodes to handle massive concurrency without OOM errors.
* **Reliability:** Zero data loss (RPO=0) for ingested requests via persistent event buffering.
* **Multi-tenancy:** Strict data isolation (Schema-per-tenant) and operational sovereignty.

### Background
The previous POC implementation hit memory ceilings and $O(N)$ scanning limits. We are building a national-grade SaaS platform. The core value is providing a "Source of Truth" for identity using a **Hybrid Matching Engine** (ANN + Exact).

## 2. Requirements

### Functional Requirements (FR)
* **FR1 - Deduplication (1:N):**
  * *Match Found:* Return `MATCH_FOUND`, `Client_RID`, `Matched_RID(s)`, scores. Do not store new data.
  * *No Match:* Store images (MinIO), templates (Off-Heap Store), metadata (DB). Return `REGISTERED`.
* **FR2 - Identification (1:1):** Verify `Probe_Fingerprint` against stored template for specific `Target_RID`.
* **FR3 - Multi-Protocol Ingestion:** REST (Multipart/form-data), gRPC (Protobuf), Kafka (Batch).
* **FR4 - Queue Management:** FIFO per tenant. Priority Escalation support (VIP lane).
* **FR5 - Tenant Dashboard:** Real-time visibility into queue status, health scores, and throughput.
* **FR6 - Fingerprint Validation:** Max 10 fingers, taxonomy validation, image quality check.
* **FR7 - IAM & Security:** Keycloak integration (Service Accounts for M2M, OIDC for Humans).
* **FR8 - Observability:** Distributed logging (Trace ID propagation) and immutable Audit Trail.
* **FR10 - Automated Onboarding (Saga):** API to provision Tenant + DB Schema + Keycloak Client + Storage Bucket in one atomic transaction.
* **FR11 - System Health:** "Deep Health" probes verifying DB, Kafka, and Worker connectivity (not just HTTP ping).

### Non-Functional Requirements (NFR)
* **NFR1 - Latency:** Matching latency < 2s (95th percentile) for 10M dataset.
* **NFR2 - Isolation:** Schema-per-tenant enforcement.
* **NFR3 - Resilience:** DLQ/Retry for worker failures. Master node failover.
* **NFR4 - Scalability:** Horizontal scaling of workers. Autoscaling based on CPU/Queue depth.
* **NFR5 - Observability:** OpenTelemetry implementation with native memory metrics.
* **NFR6 - Security:** Perimeter Security Model (Strict Gateway validation).
* **NFR7 - Memory Safety:** Workers must operate without Stop-the-World GC pauses > 100ms. **Off-Heap memory usage is mandatory.**
* **NFR8 - Disaster Recovery:** Automated Daily Backups of all Tenant Schemas. RTO < 4h.
* **NFR9 - Indexing:** Hybrid ANN (Approximate Nearest Neighbor) Indexing required for datasets > 100k records to maintain SLA.

## 3. Benchmarking Plan (Must pass for Release)
1.  **Load Test:** Inject 10M synthetic templates. Fire 100 req/s. Verify P95 < 2s.
2.  **Chaos Test:** Kill 1 Worker node. Verify Kafka Consumer rebalancing and 0 failed requests (Retries).
3.  **Memory Test:** Load 20M templates. Verify Off-Heap usage matches sizing, Heap remains stable (< 4GB).

## 4. Epic List

### Epic 1: Foundation & Ops
Goal: Setup Monorepo, IAM, **Automated Onboarding Saga**, Multi-tenant DB patterns, and Observability stack.

### Epic 2: High-Perf Matching Engine
Goal: Implement **Hybrid Matching (HNSW + SourceAFIS)**, **Off-Heap Memory Manager (Panama)**, Master-Worker gRPC, and Warmup logic.

### Epic 3: Ingestion Gateways
Goal: Build REST (Multipart), gRPC, and Kafka interfaces with strict validation.

### Epic 4: Logic & Persistence
Goal: Implement Deduplication business logic (Match/No-Match decision tree) and image storage.

### Epic 5: Management Dashboard
Goal: Build the Next.js Tenant Dashboard (BFF pattern) and Superadmin console.