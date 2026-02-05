# API Implementation Plan V2 (Enhanced)

**Status:** Draft
**Target Module:** `apps/api`
**Date:** 2025-12-25

## Overview
This updated plan includes the implementation of Tenant Management and Observability features, bridging the gap between the Database Schema (V001, V004) and the Frontend requirements.

## 1. Entity Implementation (JPA)
Create JPA entities mapping to the PostgreSQL tables.

### Tenant Management (V001)
- **Package**: `com.scalebiometrics.api.entity`
- **Entities**:
  - `Tenant`: Maps to `public.tenants`.
  - `TenantConfig`: Maps to `public.tenant_config`.
  - `AuditLog`: Maps to `public.audit_logs`.

### Observability (V004)
- **Package**: `com.scalebiometrics.api.entity`
- **Entities**:
  - `WorkerMetric`: Maps to `public.worker_metrics`.
  - `MatchingPerformance`: Maps to `public.matching_performance`.
  - `SystemEvent`: Maps to `public.system_events`.
  - `SloTracking`: Maps to `public.slo_tracking`.
  - `IndexStatistic`: Maps to `public.index_statistics`.

## 2. Repository Layer
Create Spring Data JPA repositories for data access.
- `TenantRepository`
- `TenantConfigRepository`
- `AuditLogRepository`
- `WorkerMetricRepository`
- `MatchingPerformanceRepository`
- `SystemEventRepository`

## 3. Service Layer
Implement business logic for management and reporting.

### TenantService
- `createTenant(TenantDto)`: Onboard new tenant.
- `updateConfig(id, config)`: Update tenant settings.
- `getTenant(id)`: Retrieve details.
- `listTenants()`: List all (paginated).

### ObservabilityService
- `getWorkerMetrics(timeRange)`: Aggregated metrics for dashboard.
- `getMatchingPerformance(tenantId)`: Latency and score stats.
- `getSystemEvents(severity)`: Filtered event log.

## 4. REST Controllers
Expose endpoints for the Frontend Console (`apps/web`).

### TenantController
- `POST /api/v1/tenants`: Create tenant.
- `GET /api/v1/tenants`: List tenants.
- `GET /api/v1/tenants/{id}`: Get details.
- `PUT /api/v1/tenants/{id}/config`: Update config.

### ObservabilityController
- `GET /api/v1/metrics/workers`: Worker health and load.
- `GET /api/v1/metrics/performance`: Matching latency/throughput.
- `GET /api/v1/events`: System audit and error logs.

## 5. DTOs
Define data transfer objects for the new endpoints.
- `TenantDto`, `TenantConfigDto`
- `WorkerMetricDto`, `SystemEventDto`

## 6. Integration
- Ensure `SecurityConfig` allows Admin access to these endpoints.
- Add `AuditLog` generation for sensitive actions (create/update tenant).
