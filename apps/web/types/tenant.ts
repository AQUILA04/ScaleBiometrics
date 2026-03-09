export type TenantStatus = 'ACTIVE' | 'SUSPENDED' | 'PENDING' | 'DELETED';

export type TenantShardStrategy = 'NONE' | 'MODULO' | 'RANGE' | 'GEO';

export interface Tenant {
  id: string;
  tenantId: string;
  name: string;
  description?: string;
  status: TenantStatus;
  shardStrategy: TenantShardStrategy;
  shardCount: number;
  maxRecords: number;
  createdAt: string;
  updatedAt: string;
}

export interface TenantConfig {
  key: string;
  value: string;
  category: string;
}

export interface TenantUsage {
  tenantId: string;
  currentRecords: number;
  storageUsedBytes: number;
  requestsThisMonth: number;
  apiCallsRemaining: number;
}

export interface TenantWithUsage extends Tenant {
  usage: TenantUsage;
}
