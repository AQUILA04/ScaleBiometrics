export interface TenantDashboardKPIs {
  pendingQueue: number;
  processed24h: number;
  successRate: number;
  avgWaitTime: number;
  activeWorkers: number;
  storageUsed: number;
  recordsCount: number;
  trends: {
    queue: number;
    processed: number;
    successRate: number;
    waitTime: number;
  };
}

export interface ThroughputPoint {
  timestamp: string;
  totalRequests: number;
  successfulRequests: number;
  failedRequests: number;
  avgLatencyMs: number;
}

export interface RecentJob {
  id: string;
  type: '1:N' | '1:1';
  status: JobStatus;
  priority: JobPriority;
  probeRid: string;
  createdAt: string;
  completedAt?: string;
  duration?: number;
  result?: {
    matchFound: boolean;
    candidateCount: number;
  };
}

export type JobStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';
export type JobPriority = 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT';
export type JobType = '1:N' | '1:1';

export interface TenantDashboardData {
  kpis: TenantDashboardKPIs;
  throughputData: ThroughputPoint[];
  recentJobs: RecentJob[];
}
