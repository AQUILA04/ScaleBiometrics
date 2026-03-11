"use client";

import { MetricCard } from "@/components/ui/metric-card";
import { StatusBadge } from "@/components/ui/status-badge";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { useTenantDashboardKPIs, useTenantThroughput, useTenantRecentJobs } from "@/hooks/use-tenant-dashboard";
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
} from "recharts";
import { Clock, CheckCircle, AlertCircle, Users, Database, HardDrive, ArrowRight } from "lucide-react";
import Link from "next/link";

const mockKPIs = {
  pendingQueue: 42,
  processed24h: 15820,
  successRate: 98.5,
  avgWaitTime: 125,
  activeWorkers: 4,
  storageUsed: 15600000000,
  recordsCount: 25000,
  trends: {
    queue: -12,
    processed: 8,
    successRate: 0.3,
    waitTime: -5,
  },
};

const mockThroughputData = [
  { time: "00:00", requests: 580, success: 570, failed: 10 },
  { time: "04:00", requests: 420, success: 415, failed: 5 },
  { time: "08:00", requests: 950, success: 935, failed: 15 },
  { time: "12:00", requests: 1420, success: 1395, failed: 25 },
  { time: "16:00", requests: 1180, success: 1160, failed: 20 },
  { time: "20:00", requests: 890, success: 875, failed: 15 },
  { time: "24:00", requests: 680, success: 670, failed: 10 },
];

const mockRecentJobs = [
  {
    id: "1",
    type: "1:N" as const,
    status: "COMPLETED" as const,
    priority: "NORMAL" as const,
    probeRid: "RID-001",
    createdAt: "2026-03-09T10:30:00Z",
    completedAt: "2026-03-09T10:30:45Z",
    duration: 45,
    result: { matchFound: true, candidateCount: 3 },
  },
  {
    id: "2",
    type: "1:1" as const,
    status: "COMPLETED" as const,
    priority: "HIGH" as const,
    probeRid: "RID-002",
    createdAt: "2026-03-09T10:28:00Z",
    completedAt: "2026-03-09T10:28:12Z",
    duration: 12,
    result: { matchFound: true, candidateCount: 1 },
  },
  {
    id: "3",
    type: "1:N" as const,
    status: "PROCESSING" as const,
    priority: "NORMAL" as const,
    probeRid: "RID-003",
    createdAt: "2026-03-09T10:27:00Z",
  },
  {
    id: "4",
    type: "1:N" as const,
    status: "COMPLETED" as const,
    priority: "LOW" as const,
    probeRid: "RID-004",
    createdAt: "2026-03-09T10:25:00Z",
    completedAt: "2026-03-09T10:26:30Z",
    duration: 90,
    result: { matchFound: false, candidateCount: 0 },
  },
  {
    id: "5",
    type: "1:1" as const,
    status: "PENDING" as const,
    priority: "URGENT" as const,
    probeRid: "RID-005",
    createdAt: "2026-03-09T10:24:00Z",
  },
  {
    id: "6",
    type: "1:N" as const,
    status: "COMPLETED" as const,
    priority: "NORMAL" as const,
    probeRid: "RID-006",
    createdAt: "2026-03-09T10:22:00Z",
    completedAt: "2026-03-09T10:23:15Z",
    duration: 75,
    result: { matchFound: true, candidateCount: 5 },
  },
  {
    id: "7",
    type: "1:N" as const,
    status: "FAILED" as const,
    priority: "NORMAL" as const,
    probeRid: "RID-007",
    createdAt: "2026-03-09T10:20:00Z",
    completedAt: "2026-03-09T10:20:05Z",
    duration: 5,
    result: { matchFound: false, candidateCount: 0 },
  },
  {
    id: "8",
    type: "1:1" as const,
    status: "COMPLETED" as const,
    priority: "HIGH" as const,
    probeRid: "RID-008",
    createdAt: "2026-03-09T10:18:00Z",
    completedAt: "2026-03-09T10:18:08Z",
    duration: 8,
    result: { matchFound: true, candidateCount: 1 },
  },
  {
    id: "9",
    type: "1:N" as const,
    status: "COMPLETED" as const,
    priority: "NORMAL" as const,
    probeRid: "RID-009",
    createdAt: "2026-03-09T10:15:00Z",
    completedAt: "2026-03-09T10:16:40Z",
    duration: 100,
    result: { matchFound: false, candidateCount: 0 },
  },
  {
    id: "10",
    type: "1:N" as const,
    status: "COMPLETED" as const,
    priority: "LOW" as const,
    probeRid: "RID-010",
    createdAt: "2026-03-09T10:12:00Z",
    completedAt: "2026-03-09T10:13:50Z",
    duration: 110,
    result: { matchFound: true, candidateCount: 2 },
  },
];

export default function TenantDashboardPage() {
  const { data: kpis } = useTenantDashboardKPIs("default");
  const { data: throughputData } = useTenantThroughput("default");
  const { data: recentJobs } = useTenantRecentJobs("default");

  const displayKPIs = kpis || mockKPIs;
  const displayThroughput = throughputData || mockThroughputData;
  const displayRecentJobs = recentJobs || mockRecentJobs;

  const formatStorage = (bytes: number) => {
    const gb = bytes / (1024 * 1024 * 1024);
    return gb.toFixed(1) + " GB";
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold">Dashboard</h1>
        <p className="text-muted-foreground mt-1">
          Monitor your biometric operations and queue status
        </p>
      </div>

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <MetricCard
          title="Pending Queue"
          value={displayKPIs.pendingQueue}
          trend={displayKPIs.trends.queue > 0 ? "up" : "down"}
          trendValue={displayKPIs.trends.queue}
          icon={<Clock className="h-4 w-4" />}
        />
        <MetricCard
          title="Processed (24h)"
          value={displayKPIs.processed24h}
          trend={displayKPIs.trends.processed > 0 ? "up" : "down"}
          trendValue={displayKPIs.trends.processed}
          icon={<CheckCircle className="h-4 w-4" />}
        />
        <MetricCard
          title="Success Rate"
          value={displayKPIs.successRate}
          unit="%"
          trend={displayKPIs.trends.successRate > 0 ? "up" : "down"}
          trendValue={displayKPIs.trends.successRate}
          icon={<AlertCircle className="h-4 w-4" />}
        />
        <MetricCard
          title="Avg Wait Time"
          value={displayKPIs.avgWaitTime}
          unit="ms"
          trend={displayKPIs.trends.waitTime > 0 ? "up" : "down"}
          trendValue={displayKPIs.trends.waitTime}
          icon={<Clock className="h-4 w-4" />}
        />
      </div>

      <div className="grid gap-4 md:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Throughput (24h)</CardTitle>
            <CardDescription>Requests processed per hour</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="h-[250px]">
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

        <Card>
          <CardHeader>
            <CardTitle>Average Wait Time</CardTitle>
            <CardDescription>Time in queue before processing (ms)</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="h-[250px]">
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={displayThroughput}>
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
                  <Line
                    type="monotone"
                    dataKey="avgLatencyMs"
                    stroke="hsl(var(--primary))"
                    strokeWidth={2}
                    dot={false}
                    name="Avg Latency (ms)"
                  />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <div>
            <CardTitle>Recent Jobs</CardTitle>
            <CardDescription>Last 10 processed jobs</CardDescription>
          </div>
          <Link
            href="/queue"
            className="text-sm text-primary hover:underline flex items-center gap-1"
          >
            View all <ArrowRight className="h-4 w-4" />
          </Link>
        </CardHeader>
        <CardContent>
          <div className="space-y-3">
            {displayRecentJobs.map((job) => (
              <div
                key={job.id}
                className="flex items-center justify-between rounded-lg border p-3 hover:bg-muted/50"
              >
                <div className="flex items-center gap-4">
                  <div
                    className={`rounded-full px-2 py-1 text-xs font-medium ${
                      job.type === "1:N"
                        ? "bg-blue-500/10 text-blue-500"
                        : "bg-purple-500/10 text-purple-500"
                    }`}
                  >
                    {job.type}
                  </div>
                  <div>
                    <p className="font-medium">{job.probeRid}</p>
                    <p className="text-xs text-muted-foreground">
                      {new Date(job.createdAt).toLocaleString()}
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-4">
                  {job.duration && (
                    <span className="text-sm text-muted-foreground">
                      {job.duration}s
                    </span>
                  )}
                  {job.result && (
                    <span
                      className={`text-sm ${
                        job.result.matchFound
                          ? "text-green-500"
                          : "text-muted-foreground"
                      }`}
                    >
                      {job.result.matchFound
                        ? `${job.result.candidateCount} match(es)`
                        : "No match"}
                    </span>
                  )}
                  <StatusBadge status={job.status} />
                </div>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>

      <div className="grid gap-4 md:grid-cols-3">
        <Card>
          <CardContent className="pt-6">
            <div className="flex items-center gap-4">
              <div className="rounded-full bg-primary/10 p-3">
                <Users className="h-6 w-6 text-primary" />
              </div>
              <div>
                <p className="text-2xl font-bold">{displayKPIs.activeWorkers}</p>
                <p className="text-sm text-muted-foreground">Active Workers</p>
              </div>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="flex items-center gap-4">
              <div className="rounded-full bg-primary/10 p-3">
                <Database className="h-6 w-6 text-primary" />
              </div>
              <div>
                <p className="text-2xl font-bold">{displayKPIs.recordsCount.toLocaleString()}</p>
                <p className="text-sm text-muted-foreground">Total Records</p>
              </div>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="flex items-center gap-4">
              <div className="rounded-full bg-primary/10 p-3">
                <HardDrive className="h-6 w-6 text-primary" />
              </div>
              <div>
                <p className="text-2xl font-bold">{formatStorage(displayKPIs.storageUsed)}</p>
                <p className="text-sm text-muted-foreground">Storage Used</p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
