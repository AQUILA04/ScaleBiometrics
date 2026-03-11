"use client";

import { ColumnDef } from "@tanstack/react-table";
import { DataTable, DataTableColumnHeader, DataTableRowActions } from "@/components/ui/data-table";
import { StatusBadge } from "@/components/ui/status-badge";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { useWorkers } from "@/hooks/use-workers";
import {
  Cpu,
  MemoryStick,
  Power,
  RefreshCw,
} from "lucide-react";
import type { WorkerNode, WorkerStatus } from "@/types/worker";
import { toast } from "sonner";

const mockWorkers: WorkerNode[] = [
  {
    id: "worker-001",
    hostname: "worker-node-1",
    ip: "10.0.1.10",
    status: "HEALTHY",
    cpu: 45,
    memory: 62,
    memoryTotal: 16000000000,
    templatesLoaded: 50000,
    requestsProcessed: 150000,
    currentJobs: 3,
    uptime: "99.99%",
    lastHeartbeat: new Date().toISOString(),
    metrics: { avgLatency: 125, p95Latency: 250, successRate: 98.5 },
  },
  {
    id: "worker-002",
    hostname: "worker-node-2",
    ip: "10.0.1.11",
    status: "HEALTHY",
    cpu: 38,
    memory: 55,
    memoryTotal: 16000000000,
    templatesLoaded: 48000,
    requestsProcessed: 145000,
    currentJobs: 2,
    uptime: "99.99%",
    lastHeartbeat: new Date().toISOString(),
    metrics: { avgLatency: 118, p95Latency: 235, successRate: 98.8 },
  },
  {
    id: "worker-003",
    hostname: "worker-node-3",
    ip: "10.0.1.12",
    status: "DEGRADED",
    cpu: 82,
    memory: 88,
    memoryTotal: 16000000000,
    templatesLoaded: 52000,
    requestsProcessed: 180000,
    currentJobs: 8,
    uptime: "98.50%",
    lastHeartbeat: new Date().toISOString(),
    metrics: { avgLatency: 450, p95Latency: 890, successRate: 95.2 },
  },
  {
    id: "worker-004",
    hostname: "worker-node-4",
    ip: "10.0.1.13",
    status: "OFFLINE",
    cpu: 0,
    memory: 0,
    memoryTotal: 16000000000,
    templatesLoaded: 0,
    requestsProcessed: 120000,
    currentJobs: 0,
    uptime: "95.00%",
    lastHeartbeat: "2026-03-09T08:00:00Z",
  },
];

const columns: ColumnDef<WorkerNode>[] = [
  {
    accessorKey: "hostname",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="Hostname" />
    ),
  },
  {
    accessorKey: "ip",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="IP Address" />
    ),
  },
  {
    accessorKey: "status",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="Status" />
    ),
    cell: ({ row }) => {
      const status = row.getValue("status") as WorkerStatus;
      const statusMap: Record<WorkerStatus, string> = {
        HEALTHY: "healthy",
        DEGRADED: "degraded",
        OFFLINE: "down",
        DRAINING: "pending",
      };
      return <StatusBadge status={statusMap[status]} />;
    },
  },
  {
    accessorKey: "cpu",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="CPU" />
    ),
    cell: ({ row }) => {
      const cpu = row.getValue("cpu") as number;
      return (
        <div className="flex items-center gap-2 w-24">
          <Progress value={cpu} className="h-2" />
          <span className="text-xs w-8">{cpu}%</span>
        </div>
      );
    },
  },
  {
    accessorKey: "memory",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="Memory" />
    ),
    cell: ({ row }) => {
      const mem = row.getValue("memory") as number;
      return (
        <div className="flex items-center gap-2 w-24">
          <Progress value={mem} className="h-2" />
          <span className="text-xs w-8">{mem}%</span>
        </div>
      );
    },
  },
  {
    accessorKey: "templatesLoaded",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="Templates" />
    ),
    cell: ({ row }) => {
      const templates = row.getValue("templatesLoaded") as number;
      return templates.toLocaleString();
    },
  },
  {
    accessorKey: "requestsProcessed",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="Requests" />
    ),
    cell: ({ row }) => {
      const requests = row.getValue("requestsProcessed") as number;
      return requests.toLocaleString();
    },
  },
  {
    accessorKey: "uptime",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="Uptime" />
    ),
  },
  {
    id: "actions",
    cell: ({ row }) => {
      const worker = row.original;
      if (worker.status === "OFFLINE") return null;
      return (
        <DataTableRowActions
          row={row.original}
          customActions={[
            {
              label: "Drain",
              onClick: (row) => {
                toast.warning(`Draining worker: ${row.hostname}`);
              },
              icon: <Power className="h-4 w-4" />,
            },
            {
              label: "Restart",
              onClick: (row) => {
                toast.info(`Restarting worker: ${row.hostname}`);
              },
              icon: <RefreshCw className="h-4 w-4" />,
            },
          ]}
        />
      );
    },
  },
];

export default function WorkersPage() {
  const { data: workers } = useWorkers();
  const workerList = workers?.workers || mockWorkers;
  
  const healthyWorkers = workerList.filter(w => w.status === "HEALTHY").length;
  const degradedWorkers = workerList.filter(w => w.status === "DEGRADED").length;
  const offlineWorkers = workerList.filter(w => w.status === "OFFLINE").length;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold">Workers</h1>
        <p className="text-muted-foreground mt-1">
          Monitor and manage biometric processing workers
        </p>
      </div>

      <div className="grid gap-4 md:grid-cols-4">
        <Card>
          <CardContent className="pt-6">
            <div className="text-center">
              <p className="text-3xl font-bold">{workerList.length}</p>
              <p className="text-sm text-muted-foreground">Total Workers</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="text-center">
              <p className="text-3xl font-bold text-green-500">{healthyWorkers}</p>
              <p className="text-sm text-muted-foreground">Healthy</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="text-center">
              <p className="text-3xl font-bold text-yellow-500">{degradedWorkers}</p>
              <p className="text-sm text-muted-foreground">Degraded</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="text-center">
              <p className="text-3xl font-bold text-red-500">{offlineWorkers}</p>
              <p className="text-sm text-muted-foreground">Offline</p>
            </div>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Worker Nodes</CardTitle>
          <CardDescription>All biometric processing workers</CardDescription>
        </CardHeader>
        <CardContent>
          <DataTable
            columns={columns}
            data={workerList}
            enableSorting
            emptyMessage="No workers found"
          />
        </CardContent>
      </Card>

      <div className="grid gap-4 md:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Load Distribution</CardTitle>
            <CardDescription>Current job distribution across workers</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {workerList.filter(w => w.status !== "OFFLINE").map((worker) => (
                <div key={worker.id} className="space-y-2">
                  <div className="flex items-center justify-between text-sm">
                    <span className="font-medium">{worker.hostname}</span>
                    <span className="text-muted-foreground">{worker.currentJobs} jobs</span>
                  </div>
                  <Progress 
                    value={(worker.currentJobs / 10) * 100} 
                    className="h-2" 
                  />
                </div>
              ))}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Resource Usage</CardTitle>
            <CardDescription>Average CPU and Memory usage</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-2 gap-4">
              <div className="text-center p-4 rounded-lg bg-muted">
                <Cpu className="h-8 w-8 mx-auto mb-2 text-primary" />
                <p className="text-2xl font-bold">
                  {Math.round(workerList.filter(w => w.status !== "OFFLINE").reduce((sum, w) => sum + w.cpu, 0) / workerList.filter(w => w.status !== "OFFLINE").length)}%
                </p>
                <p className="text-sm text-muted-foreground">Avg CPU</p>
              </div>
              <div className="text-center p-4 rounded-lg bg-muted">
                <MemoryStick className="h-8 w-8 mx-auto mb-2 text-primary" />
                <p className="text-2xl font-bold">
                  {Math.round(workerList.filter(w => w.status !== "OFFLINE").reduce((sum, w) => sum + w.memory, 0) / workerList.filter(w => w.status !== "OFFLINE").length)}%
                </p>
                <p className="text-sm text-muted-foreground">Avg Memory</p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
