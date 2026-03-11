export type AuditAction = 
  | 'TENANT_CREATED'
  | 'TENANT_UPDATED'
  | 'TENANT_SUSPENDED'
  | 'TENANT_DELETED'
  | 'WORKER_DRAINED'
  | 'WORKER_RESTARTED'
  | 'SETTINGS_UPDATED'
  | 'API_KEY_CREATED'
  | 'API_KEY_REVOKED'
  | 'WEBHOOK_CREATED'
  | 'WEBHOOK_DELETED';

export type ActorRole = 'SUPER_ADMIN' | 'TENANT_ADMIN' | 'SYSTEM';

export interface AuditLog {
  id: string;
  actor: string;
  actorRole: ActorRole;
  action: AuditAction;
  tenantId?: string;
  resource: string;
  resourceId: string;
  details: Record<string, unknown>;
  changes?: {
    before: Record<string, unknown>;
    after: Record<string, unknown>;
  };
  ipAddress: string;
  timestamp: string;
}

export interface AuditLogsResponse {
  logs: AuditLog[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface AuditLogFilters {
  actor?: string;
  action?: AuditAction;
  tenantId?: string;
  fromDate?: string;
  toDate?: string;
}
