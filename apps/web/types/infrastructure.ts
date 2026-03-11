export interface PostgresMetrics {
  activeConnections: number;
  maxConnections: number;
  qps: number;
  avgLatencyMs: number;
  dbSize: number;
  slowQueries: SlowQuery[];
  replicationLag: number;
  uptime: string;
}

export interface SlowQuery {
  query: string;
  duration: number;
  timestamp: string;
}

export interface RedisMetrics {
  memoryUsed: number;
  memoryTotal: number;
  hitRate: number;
  connectedClients: number;
  keysCount: number;
  opsPerSecond: number;
  uptime: string;
}

export interface KafkaBroker {
  id: number;
  host: string;
  port: number;
  status: string;
}

export interface KafkaTopic {
  name: string;
  partitions: number;
  replicationFactor: number;
  messages: number;
}

export interface KafkaMetrics {
  brokers: KafkaBroker[];
  consumerLag: number;
  messagesPerSecond: number;
  topics: KafkaTopic[];
  underReplicatedPartitions: number;
  offlinePartitions: number;
}

export interface MinIOMetrics {
  storageUsed: number;
  storageTotal: number;
  objectsCount: number;
  bucketsCount: number;
  uploadSpeed: number;
  downloadSpeed: number;
  s3RequestsTotal: number;
  s3RequestsFailed: number;
}

export type ServiceHealthStatus = 'healthy' | 'degraded' | 'down';

export interface ServiceHealth {
  name: string;
  status: ServiceHealthStatus;
  uptime: number;
}

export interface ClusterHealth {
  overallStatus: ServiceHealthStatus;
  score: number;
  services: ServiceHealth[];
  lastChecked: string;
}

export interface InfrastructureAlert {
  id: string;
  severity: 'info' | 'warning' | 'critical';
  service: string;
  message: string;
  timestamp: string;
  acknowledged: boolean;
}
