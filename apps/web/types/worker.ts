export type WorkerStatus = 'HEALTHY' | 'DEGRADED' | 'OFFLINE' | 'DRAINING';

export interface WorkerMetrics {
  avgLatency: number;
  p95Latency: number;
  successRate: number;
}

export interface WorkerNode {
  id: string;
  hostname: string;
  ip: string;
  status: WorkerStatus;
  cpu: number;
  memory: number;
  memoryTotal: number;
  templatesLoaded: number;
  requestsProcessed: number;
  currentJobs: number;
  uptime: string;
  lastHeartbeat: string;
  metrics?: WorkerMetrics;
}

export interface WorkersResponse {
  workers: WorkerNode[];
}
