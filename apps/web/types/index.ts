/**
 * ScaleBiometrics — Shared TypeScript Types
 */

// ─── Auth & Session ────────────────────────────────────────────────────────────

export type UserRole = "superadmin" | "tenant_admin" | "api_user";

export interface AppUser {
  id?: string;
  name?: string | null;
  email?: string | null;
  image?: string | null;
  tenantId?: string;
  roles: UserRole[];
}

// ─── Tenant ────────────────────────────────────────────────────────────────────

export type TenantStatus = "ACTIVE" | "PENDING" | "SUSPENDED" | "DEPROVISIONING";

export interface Tenant {
  id: string;
  name: string;
  slug: string;
  status: TenantStatus;
  shardStrategy: "SINGLE" | "MULTI";
  createdAt: string;
  identityCount?: number;
}

// ─── Queue & Jobs ──────────────────────────────────────────────────────────────

export type JobStatus = "PENDING" | "PROCESSING" | "COMPLETED" | "FAILED" | "ESCALATED";
export type JobType = "DEDUPLICATION" | "IDENTIFICATION";

export interface QueueJob {
  id: string;
  tenantId: string;
  type: JobType;
  status: JobStatus;
  priority: number;
  clientRid?: string;
  submittedAt: string;
  startedAt?: string;
  completedAt?: string;
  latencyMs?: number;
  errorMessage?: string;
}

// ─── Metrics & Health ──────────────────────────────────────────────────────────

export interface SystemHealth {
  status: "UP" | "DEGRADED" | "DOWN";
  components: {
    database: "UP" | "DOWN";
    kafka: "UP" | "DOWN";
    workers: "UP" | "DOWN";
    minio: "UP" | "DOWN";
  };
  workerCount: number;
  activeWorkers: number;
  timestamp: string;
}

export interface ThroughputMetric {
  timestamp: string;
  requestsPerSecond: number;
  p95LatencyMs: number;
  p99LatencyMs: number;
  successRate: number;
}

export interface QueueMetrics {
  tenantId: string;
  pendingCount: number;
  processingCount: number;
  failedCount: number;
  avgLatencyMs: number;
  p95LatencyMs: number;
}

// ─── Pagination ────────────────────────────────────────────────────────────────

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

export interface PageParams {
  page?: number;
  size?: number;
  sort?: string;
  direction?: "ASC" | "DESC";
}

// ─── API Keys ──────────────────────────────────────────────────────────────────

export interface ApiKey {
  id: string;
  name: string;
  prefix: string;
  createdAt: string;
  lastUsedAt?: string;
  expiresAt?: string;
  scopes: string[];
}

// ─── Audit Log ─────────────────────────────────────────────────────────────────

export interface AuditEntry {
  id: string;
  tenantId: string;
  userId?: string;
  action: string;
  resource: string;
  resourceId?: string;
  outcome: "SUCCESS" | "FAILURE";
  ipAddress?: string;
  traceId?: string;
  timestamp: string;
  details?: Record<string, unknown>;
}
