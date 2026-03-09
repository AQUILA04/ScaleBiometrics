"use client";

import { MetricCard } from "@/components/ui/metric-card";
import { ServiceStatus } from "@/components/ui/service-status";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { useKPIs, useServiceHealth, useThroughputData, useLatencyData } from "@/hooks/use-dashboard";
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  LineChart,
  Line,
  Legend,
} from "recharts";
import { Users, Cpu, Clock, Zap, Activity, Database, Server, HardDrive } from "lucide-react";

const mockKPIs = {
  totalTenants: 24,
  activeWorkers: 8,
  latencyP95: 145,
  throughput: 1250,
  tenantTrend: 12,
  workerTrend: 0,
  latencyTrend: -8,
  throughputTrend: 23,
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

const mockLatencyData = [
  { time: "00:00", p50: 45, p90: 95, p95: 130, p99: 220 },
  { time: "04:00", p50: 42, p90: 88, p95: 120, p99: 200 },
  { time: "08:00", p50: 55, p90: 115, p95: 155, p99: 280 },
  { time: "12:00", p50: 62, p90: 130, p95: 175, p99: 320 },
  { time: "16:00", p50: 58, p90: 125, p95: 165, p99: 290 },
  { time: "20:00", p50: 50, p90: 105, p95: 145, p99: 250 },
  { time: "24:00", p50: 48, p90: 100, p95: 135, p99: 230 },
];

const mockServiceHealth = [
  { name: "API Gateway", status: "healthy" as const, latency: 12, lastCheck: new Date().toISOString() },
  { name: "PostgreSQL", status: "healthy" as const, latency: 8, lastCheck: new Date().toISOString() },
  { name: "Kafka", status: "healthy" as const, latency: 5, lastCheck: new Date().toISOString() },
  { name: "MinIO", status: "degraded" as const, latency: 45, lastCheck: new Date().toISOString() },
  { name: "Redis", status: "healthy" as const, latency: 2, lastCheck: new Date().toISOString() },
];

export default function SuperAdminDashboard() {
  const { data: kpis } = useKPIs();
  const { data: serviceHealth } = useServiceHealth();
  const { data: throughputData } = useThroughputData();
  const { data: latencyData } = useLatencyData();

  const displayKPIs = kpis || mockKPIs;
  const displayThroughput = throughputData || mockThroughputData;
  const displayLatency = latencyData || mockLatencyData;
  const displayHealth = serviceHealth || mockServiceHealth;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold">SuperAdmin Console</h1>
        <p className="text-muted-foreground mt-1">
          Monitor platform health, performance, and tenant status
        </p>
      </div>

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <MetricCard
          title="Total Tenants"
          value={displayKPIs.totalTenants}
          trend={displayKPIs.tenantTrend > 0 ? "up" : "down"}
          trendValue={displayKPIs.tenantTrend}
          icon={<Users className="h-4 w-4" />}
        />
        <MetricCard
          title="Active Workers"
          value={displayKPIs.activeWorkers}
          trend={displayKPIs.workerTrend > 0 ? "up" : displayKPIs.workerTrend < 0 ? "down" : "neutral"}
          trendValue={displayKPIs.workerTrend}
          unit="nodes"
          icon={<Cpu className="h-4 w-4" />}
        />
        <MetricCard
          title="Latency P95"
          value={displayKPIs.latencyP95}
          unit="ms"
          trend={displayKPIs.latencyTrend > 0 ? "up" : "down"}
          trendValue={displayKPIs.latencyTrend}
          icon={<Clock className="h-4 w-4" />}
        />
        <MetricCard
          title="Throughput"
          value={displayKPIs.throughput}
          unit="req/s"
          trend={displayKPIs.throughputTrend > 0 ? "up" : "down"}
          trendValue={displayKPIs.throughputTrend}
          icon={<Zap className="h-4 w-4" />}
        />
      </div>

      <div className="grid gap-4 md:grid-cols-2">
        <Card className="col-span-1">
          <CardHeader>
            <CardTitle>Throughput (24h)</CardTitle>
            <CardDescription>Requests processed per hour</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="h-[300px]">
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={displayThroughput}>
                  <CartesianGrid strokeDasharray="3 3" className="stroke-muted" />
                  <XAxis
                    dataKey="time"
                    className="text-xs fill-muted-foreground"
                    tickLine={false}
                    axisLine={false}
                  />
                  <YAxis
                    className="text-xs fill-muted-foreground"
                    tickLine={false}
                    axisLine={false}
                    tickFormatter={(value) => `${value}`}
                  />
                  <Tooltip
                    contentStyle={{
                      backgroundColor: "hsl(var(--card))",
                      border: "1px solid hsl(var(--border))",
                      borderRadius: "8px",
                    }}
                  />
                  <Area
                    type="monotone"
                    dataKey="success"
                    stackId="1"
                    stroke="hsl(var(--primary))"
                    fill="hsl(var(--primary) / 0.3)"
                    name="Success"
                  />
                  <Area
                    type="monotone"
                    dataKey="failed"
                    stackId="2"
                    stroke="hsl(var(--destructive))"
                    fill="hsl(var(--destructive) / 0.3)"
                    name="Failed"
                  />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          </CardContent>
        </Card>

        <Card className="col-span-1">
          <CardHeader>
            <CardTitle>Latency Distribution</CardTitle>
            <CardDescription>Response time percentiles (ms)</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="h-[300px]">
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={displayLatency}>
                  <CartesianGrid strokeDasharray="3 3" className="stroke-muted" />
                  <XAxis
                    dataKey="time"
                    className="text-xs fill-muted-foreground"
                    tickLine={false}
                    axisLine={false}
                  />
                  <YAxis
                    className="text-xs fill-muted-foreground"
                    tickLine={false}
                    axisLine={false}
                    tickFormatter={(value) => `${value}ms`}
                  />
                  <Tooltip
                    contentStyle={{
                      backgroundColor: "hsl(var(--card))",
                      border: "1px solid hsl(var(--border))",
                      borderRadius: "8px",
                    }}
                  />
                  <Legend />
                  <Line
                    type="monotone"
                    dataKey="p50"
                    stroke="hsl(var(--primary))"
                    strokeWidth={2}
                    dot={false}
                    name="p50"
                  />
                  <Line
                    type="monotone"
                    dataKey="p90"
                    stroke="hsl(var(--secondary))"
                    strokeWidth={2}
                    dot={false}
                    name="p90"
                  />
                  <Line
                    type="monotone"
                    dataKey="p95"
                    stroke="hsl(var(--accent))"
                    strokeWidth={2}
                    dot={false}
                    name="p95"
                  />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </CardContent>
        </Card>
      </div>

      <div className="grid gap-4 md:grid-cols-3">
        <Card className="col-span-2">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Activity className="h-5 w-5" />
              Service Health
            </CardTitle>
            <CardDescription>Real-time status of platform services</CardDescription>
          </CardHeader>
          <CardContent>
            <ServiceStatus services={displayHealth} />
          </CardContent>
        </Card>

        <Card className="col-span-1">
          <CardHeader>
            <CardTitle>Quick Stats</CardTitle>
            <CardDescription>Platform overview</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Database className="h-4 w-4 text-muted-foreground" />
                <span className="text-sm">Database Size</span>
              </div>
              <span className="font-semibold">2.4 GB</span>
            </div>
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Server className="h-4 w-4 text-muted-foreground" />
                <span className="text-sm">API Requests (24h)</span>
              </div>
              <span className="font-semibold">1.2M</span>
            </div>
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <HardDrive className="h-4 w-4 text-muted-foreground" />
                <span className="text-sm">Storage Used</span>
              </div>
              <span className="font-semibold">156 GB</span>
            </div>
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Activity className="h-4 w-4 text-muted-foreground" />
                <span className="text-sm">Avg CPU</span>
              </div>
              <span className="font-semibold">42%</span>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
