export interface MetricData {
  label: string;
  value: string | number;
  previousValue?: string | number;
  trend?: 'up' | 'down' | 'neutral';
  trendValue?: number;
  unit?: string;
}

export interface DashboardKPIs {
  totalTenants: number;
  activeWorkers: number;
  latencyP95: number;
  throughput: number;
  tenantTrend: number;
  workerTrend: number;
  latencyTrend: number;
  throughputTrend: number;
}

export interface TimeSeriesPoint {
  timestamp: string;
  value: number;
}

export interface ThroughputData {
  time: string;
  requests: number;
  success: number;
  failed: number;
}

export interface LatencyData {
  time: string;
  p50: number;
  p90: number;
  p95: number;
  p99: number;
}

export interface ServiceHealth {
  name: string;
  status: 'healthy' | 'degraded' | 'down';
  latency?: number;
  uptime?: number;
  lastCheck: string;
}

export interface DashboardData {
  kpis: DashboardKPIs;
  throughputData: ThroughputData[];
  latencyData: LatencyData[];
  serviceHealth: ServiceHealth[];
}
