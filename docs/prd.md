# Product Requirements Document (PRD)

**Title:** ScaleBiometrics SaaS Platform
**Version:** 1.1 (Final)
**Status:** Approved

## 1. Goals and Background Context

### Goals
* **Performance:** Achieve 1:N deduplication matching in under 2 seconds for databases up to 10M records.
* **Scalability:** Leverage Java 21 Virtual Threads and distributed worker nodes to handle massive concurrency without OOM errors.
* **Multi-tenancy:** Provide strict data isolation (Schema-per-tenant) and operational security.
* **Interoperability:** Offer standardized integration via REST (Multipart), gRPC, and Kafka.

### Background
The current POC implementation hits memory ceilings. We are building a national-grade SaaS platform. The core value is providing a "Source of Truth" for identity, preventing double-registration fraud.

## 2. Requirements

### Functional Requirements (FR)
* **FR1 - Deduplication (1:N):**
    * *Match Found:* Return `MATCH_FOUND`, `Client_RID`, `Matched_RID(s)`, scores. Do not store new data.
    * *No Match:* Store images (Object Store), templates (Vector Cache), metadata (DB). Return `REGISTERED`.
* **FR2 - Identification (1:1):** Verify `Probe_Fingerprint` against stored template for specific `Target_RID`.
* **FR3 - Multi-Protocol Ingestion:** REST (Multipart), gRPC (Protobuf), Kafka (Batch).
* **FR4 - Queue Management:** FIFO per tenant. Priority Escalation support.
* **FR5 - Tenant Dashboard:** Real-time visibility into queue status, health scores, and throughput.
* **FR6 - Fingerprint Validation:** Max 10 fingers, taxonomy validation.
* **FR7 - IAM & Security:**
    * Keycloak integration (Service Accounts for API, OIDC for Dashboard).
    * Automated Tenant Onboarding (Schema creation + Keycloak Client creation).
* **FR8 - RBAC:** Superadmin (Global view) vs Tenant Admin (Restricted view).
* **FR9 - Observability:** Distributed logging (Trace ID) and immutable Audit Trail.

### Non-Functional Requirements (NFR)
* **NFR1 - Performance:** Matching latency < 2s (P95).
* **NFR2 - Isolation:** Schema-per-tenant enforcement.
* **NFR3 - Resilience:** DLQ/Retry for worker failures.
* **NFR4 - Scalability:** Horizontal scaling of workers independent of Gateway.
* **NFR5 - Observability:** OpenTelemetry implementation.
* **NFR6 - Security:** Perimeter Security Model (Strict Gateway validation).

## 3. Technical Assumptions
* **Stack:** Java 21, Spring Boot 3.2, Next.js 14, PostgreSQL 16, Kafka, MinIO, Keycloak.
* **Architecture:** Distributed Master-Worker with Virtual Threads.

## 4. Epic List

### Epic 1: Foundation & Infrastructure (+ IAM)
Goal: Setup Monorepo, Multi-tenant DB patterns, Keycloak integration, and Observability stack.

### Epic 2: Distributed Matching Engine
Goal: Implement Master-Worker architecture, memory management, and SourceAFIS integration.

### Epic 3: Ingestion Gateways
Goal: Build REST, gRPC, and Kafka interfaces.

### Epic 4: Logic & Persistence
Goal: Implement Deduplication business logic and image storage.

### Epic 5: Management Dashboard
Goal: Build the Next.js Tenant Dashboard and Superadmin console.