"use client";

import { useState } from "react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { MetricCard } from "@/components/ui/metric-card";
import { StatusBadge } from "@/components/ui/status-badge";
import {
  Database,
  HardDrive,
  Activity,
  Server,
  Cpu,
  MemoryStick,
  Clock,
  AlertTriangle,
  CheckCircle,
} from "lucide-react";
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from "recharts";

const mockPostgresMetrics = {
  activeConnections: 45,
  maxConnections: 100,
  qps: 1250,
  avgLatencyMs: 8,
  dbSize: 2500000000,
  slowQueries: [
    { query: "SELECT * FROM records WHERE...", duration: 2500, timestamp: "2026-03-09T10:30:00Z" },
    { query: "UPDATE matching_results SET...", duration: 1800, timestamp: "2026-03-09T10:28:00Z" },
  ],
  replicationLag: 0,
  uptime: "99.99%",
};

const mockRedisMetrics = {
  memoryUsed: 524288000,
  memoryTotal: 1073741824,
  hitRate: 98.5,
  connectedClients: 25,
  keysCount: 15000,
  opsPerSecond: 5000,
  uptime: "99.99%",
};

const mockKafkaMetrics = {
  brokers: [
    { id: 0, host: "kafka-0", port: 9092, status: "ONLINE" },
    { id: 1, host: "kafka-1", port: 9092, status: "ONLINE" },
    { id: 2, host: "kafka-2", port: 9092, status: "ONLINE" },
  ],
  consumerLag: 150,
  messagesPerSecond: 2500,
  topics: [
    { name: "biometric-jobs", partitions: 12, replicationFactor: 3, messages: 150000 },
    { name: "matching-results", partitions: 6, replicationFactor: 3, messages: 75000 },
  ],
  underReplicatedPartitions: 0,
  offlinePartitions: 0,
};

const mockMinIOMetrics = {
  storageUsed: 156000000000,
  storageTotal: 500000000000,
  objectsCount: 250000,
  bucketsCount: 25,
  uploadSpeed: 50000000,
  downloadSpeed: 100000000,
  s3RequestsTotal: 1500000,
  s3RequestsFailed: 150,
};

const mockClusterHealth = {
  overallStatus: "healthy" as const,
  score: 98,
  services: [
    { name: "PostgreSQL", status: "healthy" as const, uptime: 99.99 },
    { name: "Redis", status: "healthy" as const, uptime: 99.99 },
    { name: "Kafka", status: "healthy" as const, uptime: 99.95 },
    { name: "MinIO", status: "degraded" as const, uptime: 99.50 },
  ],
  lastChecked: new Date().toISOString(),
};

const mockThroughputData = [
  { time: "00:00", requests: 980, success: 950, failed: 30 },
  { time: "04:00", requests: 720, success: 700, failed: 20 },
  { time: "08:00", requests: 1450, success: 1400, failed: 50 },
  { time: "12:00", requests: 2100, success: 2030, failed: 70 },
  { time: "16:00", requests: 1850, success: 1790, failed: 60 },
  { time: "20:00", requests: 1300, success: 1260, failed: 40 },
  { time: "24:00", requests: 1100, success: 1070, failed: 30 },
];

const formatBytes = (bytes: number) => {
  const gb = bytes / (1024 * 1024 * 1024);
  if (gb >= 1000) return (gb / 1024).toFixed(1) + " TB";
  return gb.toFixed(1) + " GB";
};

export default function InfrastructurePage() {
  const [activeTab, setActiveTab] = useState("postgres");

  const postgres = mockPostgresMetrics;
  const redis = mockRedisMetrics;
  const kafka = mockKafkaMetrics;
  const minio = mockMinIOMetrics;
  const cluster = mockClusterHealth;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold">Infrastructure</h1>
        <p className="text-muted-foreground mt-1">
          Monitor platform infrastructure and services
        </p>
      </div>

      <div className="grid gap-4 md:grid-cols-4">
        <Card>
          <CardContent className="pt-6">
            <div className="flex items-center gap-4">
              <div className={`rounded-full p-3 ${cluster.overallStatus === 'healthy' ? 'bg-green-500/10' : 'bg-yellow-500/10'}`}>
                <Activity className={`h-6 w-6 ${cluster.overallStatus === 'healthy' ? 'text-green-500' : 'text-yellow-500'}`} />
              </div>
              <div>
                <p className="text-2xl font-bold">{cluster.score}%</p>
                <p className="text-sm text-muted-foreground">Cluster Health</p>
              </div>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="text-center">
              <p className="text-3xl font-bold">{postgres.activeConnections}/{postgres.maxConnections}</p>
              <p className="text-sm text-muted-foreground">PostgreSQL Connections</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="text-center">
              <p className="text-3xl font-bold">{kafka.consumerLag}</p>
              <p className="text-sm text-muted-foreground">Kafka Consumer Lag</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="text-center">
              <p className="text-3xl font-bold">{formatBytes(minio.storageUsed)}</p>
              <p className="text-sm text-muted-foreground">MinIO Storage Used</p>
            </div>
          </CardContent>
        </Card>
      </div>

      <div className="grid gap-4 md:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Services Health</CardTitle>
            <CardDescription>Current status of all services</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              {cluster.services.map((service) => (
                <div key={service.name} className="flex items-center justify-between rounded-lg border p-3">
                  <div className="flex items-center gap-3">
                    {service.status === "healthy" ? (
                      <CheckCircle className="h-5 w-5 text-green-500" />
                    ) : service.status === "degraded" ? (
                      <AlertTriangle className="h-5 w-5 text-yellow-500" />
                    ) : (
                      <AlertTriangle className="h-5 w-5 text-red-500" />
                    )}
                    <span className="font-medium">{service.name}</span>
                  </div>
                  <div className="flex items-center gap-4">
                    <span className="text-sm text-muted-foreground">{service.uptime}%</span>
                    <StatusBadge status={service.status} />
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Throughput (24h)</CardTitle>
            <CardDescription>Requests per hour</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="h-[200px]">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={mockThroughputData}>
                  <CartesianGrid strokeDasharray="3 3" className="stroke-muted" />
                  <XAxis dataKey="time" className="text-xs fill-muted-foreground" />
                  <YAxis className="text-xs fill-muted-foreground" />
                  <Tooltip />
                  <Bar dataKey="requests" fill="hsl(var(--primary) / 0.5)" name="Total" />
                  <Bar dataKey="failed" fill="hsl(var(--destructive) / 0.5)" name="Failed" />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </CardContent>
        </Card>
      </div>

      <Tabs value={activeTab} onValueChange={setActiveTab}>
        <TabsList className="grid w-full grid-cols-4">
          <TabsTrigger value="postgres" className="gap-2">
            <Database className="h-4 w-4" /> PostgreSQL
          </TabsTrigger>
          <TabsTrigger value="redis" className="gap-2">
            <MemoryStick className="h-4 w-4" /> Redis
          </TabsTrigger>
          <TabsTrigger value="kafka" className="gap-2">
            <Server className="h-4 w-4" /> Kafka
          </TabsTrigger>
          <TabsTrigger value="minio" className="gap-2">
            <HardDrive className="h-4 w-4" /> MinIO
          </TabsTrigger>
        </TabsList>

        <TabsContent value="postgres" className="space-y-4">
          <div className="grid gap-4 md:grid-cols-4">
            <MetricCard title="QPS" value={postgres.qps} icon={<Activity className="h-4 w-4" />} />
            <MetricCard title="Avg Latency" value={postgres.avgLatencyMs} unit="ms" icon={<Clock className="h-4 w-4" />} />
            <MetricCard title="DB Size" value={formatBytes(postgres.dbSize)} icon={<Database className="h-4 w-4" />} />
            <MetricCard title="Uptime" value={postgres.uptime} icon={<CheckCircle className="h-4 w-4" />} />
          </div>
          <Card>
            <CardHeader>
              <CardTitle>Slow Queries</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                {postgres.slowQueries.map((query, i) => (
                  <div key={i} className="flex items-center justify-between rounded-lg border p-3">
                    <code className="text-xs truncate flex-1">{query.query}</code>
                    <span className="text-sm text-muted-foreground ml-4">{query.duration}ms</span>
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="redis" className="space-y-4">
          <div className="grid gap-4 md:grid-cols-4">
            <MetricCard title="Memory Used" value={formatBytes(redis.memoryUsed)} icon={<MemoryStick className="h-4 w-4" />} />
            <MetricCard title="Hit Rate" value={redis.hitRate} unit="%" icon={<Activity className="h-4 w-4" />} />
            <MetricCard title="Connected Clients" value={redis.connectedClients} icon={<Server className="h-4 w-4" />} />
            <MetricCard title="Ops/sec" value={redis.opsPerSecond} icon={<Cpu className="h-4 w-4" />} />
          </div>
        </TabsContent>

        <TabsContent value="kafka" className="space-y-4">
          <div className="grid gap-4 md:grid-cols-4">
            <MetricCard title="Brokers" value={kafka.brokers.length} icon={<Server className="h-4 w-4" />} />
            <MetricCard title="Messages/sec" value={kafka.messagesPerSecond} icon={<Activity className="h-4 w-4" />} />
            <MetricCard title="Consumer Lag" value={kafka.consumerLag} icon={<Clock className="h-4 w-4" />} />
            <MetricCard title="Topics" value={kafka.topics.length} icon={<Database className="h-4 w-4" />} />
          </div>
          <Card>
            <CardHeader>
              <CardTitle>Brokers</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                {kafka.brokers.map((broker) => (
                  <div key={broker.id} className="flex items-center justify-between rounded-lg border p-3">
                    <span className="font-medium">{broker.host}:{broker.port}</span>
                    <StatusBadge status={broker.status === "ONLINE" ? "healthy" : "down"} />
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="minio" className="space-y-4">
          <div className="grid gap-4 md:grid-cols-4">
            <MetricCard title="Storage Used" value={formatBytes(minio.storageUsed)} icon={<HardDrive className="h-4 w-4" />} />
            <MetricCard title="Objects" value={minio.objectsCount.toLocaleString()} icon={<Database className="h-4 w-4" />} />
            <MetricCard title="Buckets" value={minio.bucketsCount} icon={<Server className="h-4 w-4" />} />
            <MetricCard title="Failed Requests" value={minio.s3RequestsFailed} icon={<AlertTriangle className="h-4 w-4" />} />
          </div>
        </TabsContent>
      </Tabs>
    </div>
  );
}
