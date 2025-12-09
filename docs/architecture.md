# Architecture Document: ScaleBiometrics SaaS Platform

## 1. High Level Architecture

ScaleBiometrics is a Distributed, Event-Driven Platform decoupling I/O-bound API Gateway from CPU-bound Matching Engine using a Master-Worker topology.

### High Level Diagram
(Reference: Mermaid diagram in chat history - Gateway -> Kafka -> Master -> Workers)

## 2. Technology Stack

| Category | Technology | Version | Purpose |
| :--- | :--- | :--- | :--- |
| **Language** | Java | 21 (LTS) | Virtual Threads support |
| **Framework** | Spring Boot | 3.2+ | Core Backend |
| **Matcher** | SourceAFIS | Latest | Fingerprint Logic |
| **Bus** | Kafka | 3.6+ | Async Ingestion |
| **RPC** | gRPC | 1.60+ | Internal Master-Worker Comm |
| **DB** | PostgreSQL | 16+ | Multi-Schema Metadata |
| **Storage** | MinIO | Latest | Fingerprint Images |
| **IAM** | Keycloak | 23+ | OIDC/OAuth2 Provider |

## 3. Component Architecture

### 3.1 API Gateway (I/O Bound)
* Validates JWT via Keycloak.
* Streams Multipart images to MinIO.
* Routes requests to Kafka.

### 3.2 Master Orchestrator
* Consumes jobs from Kafka.
* Dispatches to Workers via gRPC.
* Aggregates results (Map-Reduce).

### 3.3 Matcher Worker (CPU Bound)
* Holds template shards in off-heap memory.
* Performs 1:N scanning.

## 4. Security & IAM Architecture (Perimeter Model)

* **External Traffic:** Strictly Authenticated (HTTPS + JWT). API Gateway acts as Policy Enforcement Point.
* **Internal Traffic:** Unencrypted (Plain TCP/gRPC) within Private Subnets for performance.
* **Network Security:** Strict Security Groups. DB accepts traffic ONLY from Gateway/Workers. No internet access for Workers.

## 5. Data Architecture (Schema-per-Tenant)

* **Public Schema:** Global config, Tenant Registry.
* **Tenant Schemas (t_{uuid}):** Isolated identities, templates, and logs.
* **Migration:** Liquibase with multi-tenant orchestration.

## 6. Observability
* **Tracing:** OpenTelemetry (TraceId propagation).
* **Logging:** JSON Structured logging with MDC (`tenant_id`, `trace_id`).