"use client";

import { useState } from "react";
import { ColumnDef } from "@tanstack/react-table";
import { DataTable, DataTableColumnHeader, DataTableRowActions } from "@/components/ui/data-table";
import { StatusBadge } from "@/components/ui/status-badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useJobs } from "@/hooks/use-jobs";
import { Search, ArrowUpCircle, XCircle } from "lucide-react";
import type { Job, JobStatus, JobType, JobPriority } from "@/types/job";
import { toast } from "sonner";

const mockJobs: Job[] = [
  {
    id: "1",
    type: "1:N",
    status: "PENDING",
    priority: "URGENT",
    probeRid: "RID-001",
    probeFilename: "fingerprint_urgent.png",
    candidateCount: 5000,
    createdAt: "2026-03-09T10:35:00Z",
    updatedAt: "2026-03-09T10:35:00Z",
  },
  {
    id: "2",
    type: "1:N",
    status: "PENDING",
    priority: "HIGH",
    probeRid: "RID-002",
    probeFilename: "fingerprint_002.png",
    candidateCount: 2500,
    createdAt: "2026-03-09T10:34:00Z",
    updatedAt: "2026-03-09T10:34:00Z",
  },
  {
    id: "3",
    type: "1:1",
    status: "PROCESSING",
    priority: "HIGH",
    probeRid: "RID-003",
    probeFilename: "fingerprint_003.png",
    createdAt: "2026-03-09T10:33:00Z",
    updatedAt: "2026-03-09T10:33:30Z",
    startedAt: "2026-03-09T10:33:30Z",
  },
  {
    id: "4",
    type: "1:N",
    status: "PENDING",
    priority: "NORMAL",
    probeRid: "RID-004",
    probeFilename: "fingerprint_004.png",
    candidateCount: 1000,
    createdAt: "2026-03-09T10:32:00Z",
    updatedAt: "2026-03-09T10:32:00Z",
  },
  {
    id: "5",
    type: "1:N",
    status: "PENDING",
    priority: "LOW",
    probeRid: "RID-005",
    probeFilename: "fingerprint_005.png",
    candidateCount: 1000,
    createdAt: "2026-03-09T10:31:00Z",
    updatedAt: "2026-03-09T10:31:00Z",
  },
  {
    id: "6",
    type: "1:1",
    status: "COMPLETED",
    priority: "HIGH",
    probeRid: "RID-006",
    probeFilename: "fingerprint_006.png",
    createdAt: "2026-03-09T10:30:00Z",
    updatedAt: "2026-03-09T10:30:10Z",
    startedAt: "2026-03-09T10:30:00Z",
    completedAt: "2026-03-09T10:30:10Z",
    duration: 10,
  },
  {
    id: "7",
    type: "1:N",
    status: "COMPLETED",
    priority: "NORMAL",
    probeRid: "RID-007",
    probeFilename: "fingerprint_007.png",
    candidateCount: 1000,
    createdAt: "2026-03-09T10:28:00Z",
    updatedAt: "2026-03-09T10:29:00Z",
    startedAt: "2026-03-09T10:28:00Z",
    completedAt: "2026-03-09T10:29:00Z",
    duration: 60,
  },
  {
    id: "8",
    type: "1:N",
    status: "FAILED",
    priority: "NORMAL",
    probeRid: "RID-008",
    probeFilename: "fingerprint_008.png",
    candidateCount: 1000,
    createdAt: "2026-03-09T10:25:00Z",
    updatedAt: "2026-03-09T10:25:05Z",
    startedAt: "2026-03-09T10:25:00Z",
    completedAt: "2026-03-09T10:25:05Z",
    duration: 5,
  },
  {
    id: "9",
    type: "1:1",
    status: "CANCELLED",
    priority: "NORMAL",
    probeRid: "RID-009",
    probeFilename: "fingerprint_009.png",
    createdAt: "2026-03-09T10:20:00Z",
    updatedAt: "2026-03-09T10:21:00Z",
  },
  {
    id: "10",
    type: "1:N",
    status: "PENDING",
    priority: "NORMAL",
    probeRid: "RID-010",
    probeFilename: "fingerprint_010.png",
    candidateCount: 1000,
    createdAt: "2026-03-09T10:19:00Z",
    updatedAt: "2026-03-09T10:19:00Z",
  },
];

const priorityColors: Record<JobPriority, string> = {
  LOW: "bg-gray-500/10 text-gray-500",
  NORMAL: "bg-blue-500/10 text-blue-500",
  HIGH: "bg-orange-500/10 text-orange-500",
  URGENT: "bg-red-500/10 text-red-500",
};

const columns: ColumnDef<Job>[] = [
  {
    accessorKey: "id",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="Job ID" />
    ),
    cell: ({ row }) => (
      <span className="font-mono text-xs">{row.getValue("id")}</span>
    ),
  },
  {
    accessorKey: "type",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="Type" />
    ),
    cell: ({ row }) => (
      <span
        className={`rounded-full px-2 py-1 text-xs font-medium ${
          row.getValue("type") === "1:N"
            ? "bg-blue-500/10 text-blue-500"
            : "bg-purple-500/10 text-purple-500"
        }`}
      >
        {row.getValue("type")}
      </span>
    ),
  },
  {
    accessorKey: "status",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="Status" />
    ),
    cell: ({ row }) => <StatusBadge status={row.getValue("status")} />,
  },
  {
    accessorKey: "priority",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="Priority" />
    ),
    cell: ({ row }) => {
      const priority = row.getValue("priority") as JobPriority;
      return (
        <span className={`rounded-full px-2 py-1 text-xs font-medium ${priorityColors[priority]}`}>
          {priority}
        </span>
      );
    },
  },
  {
    accessorKey: "probeRid",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="Probe RID" />
    ),
  },
  {
    accessorKey: "candidateCount",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="Candidates" />
    ),
    cell: ({ row }) => {
      const count = row.getValue("candidateCount") as number | undefined;
      return count ? count.toLocaleString() : "-";
    },
  },
  {
    accessorKey: "createdAt",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="Created" />
    ),
    cell: ({ row }) => {
      const date = new Date(row.getValue("createdAt"));
      return date.toLocaleString();
    },
  },
  {
    accessorKey: "duration",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="Duration" />
    ),
    cell: ({ row }) => {
      const duration = row.getValue("duration") as number | undefined;
      return duration ? `${duration}s` : "-";
    },
  },
  {
    id: "actions",
    cell: ({ row }) => (
      <DataTableRowActions
        row={row.original}
        onView={(row) => {
          toast.info(`View job: ${row.id}`);
        }}
        customActions={[
          {
            label: "Escalate Priority",
            onClick: (row) => {
              toast.success(`Escalated priority for job: ${row.id}`);
            },
            icon: <ArrowUpCircle className="h-4 w-4" />,
          },
          {
            label: "Cancel Job",
            onClick: (row) => {
              toast.error(`Cancelled job: ${row.id}`);
            },
            icon: <XCircle className="h-4 w-4" />,
          },
        ]}
      />
    ),
  },
];

export default function QueuePage() {
  const [pageIndex, setPageIndex] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [statusFilter, setStatusFilter] = useState<JobStatus | "">("");
  const [typeFilter, setTypeFilter] = useState<JobType | "">("");
  const [priorityFilter, setPriorityFilter] = useState<JobPriority | "">("");
  const [searchTerm, setSearchTerm] = useState("");

  const { data } = useJobs("default", pageIndex, pageSize, {
    status: statusFilter || undefined,
    type: typeFilter || undefined,
    priority: priorityFilter || undefined,
    search: searchTerm || undefined,
  });

  const jobs = data?.content || mockJobs;
  const totalPages = data?.totalPages || Math.ceil(mockJobs.length / pageSize);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold">Queue</h1>
        <p className="text-muted-foreground mt-1">
          Manage and monitor your biometric processing queue
        </p>
      </div>

      <div className="grid gap-4 md:grid-cols-4">
        <Card>
          <CardContent className="pt-6">
            <div className="text-center">
              <p className="text-3xl font-bold">{mockJobs.filter(j => j.status === "PENDING").length}</p>
              <p className="text-sm text-muted-foreground">Pending</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="text-center">
              <p className="text-3xl font-bold">{mockJobs.filter(j => j.status === "PROCESSING").length}</p>
              <p className="text-sm text-muted-foreground">Processing</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="text-center">
              <p className="text-3xl font-bold">{mockJobs.filter(j => j.status === "COMPLETED").length}</p>
              <p className="text-sm text-muted-foreground">Completed</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="text-center">
              <p className="text-3xl font-bold">{mockJobs.filter(j => j.status === "FAILED").length}</p>
              <p className="text-sm text-muted-foreground">Failed</p>
            </div>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Jobs Queue</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex flex-wrap items-center gap-4 mb-4">
            <div className="relative flex-1 max-w-sm">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <input
                type="text"
                placeholder="Search by RID..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 pl-9 text-sm"
              />
            </div>
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value as JobStatus | "")}
              className="h-10 rounded-md border border-input bg-background px-3 py-2 text-sm"
            >
              <option value="">All Status</option>
              <option value="PENDING">Pending</option>
              <option value="PROCESSING">Processing</option>
              <option value="COMPLETED">Completed</option>
              <option value="FAILED">Failed</option>
              <option value="CANCELLED">Cancelled</option>
            </select>
            <select
              value={typeFilter}
              onChange={(e) => setTypeFilter(e.target.value as JobType | "")}
              className="h-10 rounded-md border border-input bg-background px-3 py-2 text-sm"
            >
              <option value="">All Types</option>
              <option value="1:N">1:N</option>
              <option value="1:1">1:1</option>
            </select>
            <select
              value={priorityFilter}
              onChange={(e) => setPriorityFilter(e.target.value as JobPriority | "")}
              className="h-10 rounded-md border border-input bg-background px-3 py-2 text-sm"
            >
              <option value="">All Priorities</option>
              <option value="URGENT">Urgent</option>
              <option value="HIGH">High</option>
              <option value="NORMAL">Normal</option>
              <option value="LOW">Low</option>
            </select>
          </div>

          <DataTable
            columns={columns}
            data={jobs}
            pageCount={totalPages}
            pageIndex={pageIndex}
            pageSize={pageSize}
            onPaginationChange={(newPageIndex, newPageSize) => {
              setPageIndex(newPageIndex);
              setPageSize(newPageSize);
            }}
            enableSorting
            emptyMessage="No jobs found"
          />
        </CardContent>
      </Card>
    </div>
  );
}
