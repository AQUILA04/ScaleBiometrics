"use client";

import { useState } from "react";
import { ColumnDef } from "@tanstack/react-table";
import { DataTable, DataTableColumnHeader } from "@/components/ui/data-table";
import { StatusBadge } from "@/components/ui/status-badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useAuditLogs, useExportAuditLogs } from "@/hooks/use-audit-logs";
import {
  Download,
  Eye,
  User,
  Building,
} from "lucide-react";
import type { AuditLog, AuditAction } from "@/types/audit-log";
import { toast } from "sonner";

const mockLogs: AuditLog[] = [
  {
    id: "log-001",
    actor: "admin@scalebiometrics.com",
    actorRole: "SUPER_ADMIN",
    action: "TENANT_CREATED",
    tenantId: "acme-corp",
    resource: "tenant",
    resourceId: "acme-corp",
    details: { name: "Acme Corp", maxRecords: 100000 },
    ipAddress: "10.0.0.50",
    timestamp: "2026-03-09T10:30:00Z",
  },
  {
    id: "log-002",
    actor: "admin@acme.com",
    actorRole: "TENANT_ADMIN",
    action: "API_KEY_CREATED",
    tenantId: "acme-corp",
    resource: "api-key",
    resourceId: "key-001",
    details: { name: "Production Key", scopes: ["read", "write"] },
    ipAddress: "10.0.1.100",
    timestamp: "2026-03-09T10:25:00Z",
  },
  {
    id: "log-003",
    actor: "admin@scalebiometrics.com",
    actorRole: "SUPER_ADMIN",
    action: "WORKER_RESTARTED",
    tenantId: undefined,
    resource: "worker",
    resourceId: "worker-003",
    details: { hostname: "worker-node-3" },
    ipAddress: "10.0.0.50",
    timestamp: "2026-03-09T10:20:00Z",
  },
  {
    id: "log-004",
    actor: "admin@acme.com",
    actorRole: "TENANT_ADMIN",
    action: "TENANT_UPDATED",
    tenantId: "acme-corp",
    resource: "tenant",
    resourceId: "acme-corp",
    details: { updatedFields: ["maxRecords"] },
    changes: {
      before: { maxRecords: 100000 },
      after: { maxRecords: 250000 },
    },
    ipAddress: "10.0.1.100",
    timestamp: "2026-03-09T10:15:00Z",
  },
  {
    id: "log-005",
    actor: "system",
    actorRole: "SYSTEM",
    action: "TENANT_SUSPENDED",
    tenantId: "suspended-tenant",
    resource: "tenant",
    resourceId: "suspended-tenant",
    details: { reason: "Payment overdue" },
    ipAddress: "10.0.0.1",
    timestamp: "2026-03-09T10:10:00Z",
  },
  {
    id: "log-006",
    actor: "admin@scalebiometrics.com",
    actorRole: "SUPER_ADMIN",
    action: "SETTINGS_UPDATED",
    tenantId: undefined,
    resource: "settings",
    resourceId: "global",
    details: { updatedFields: ["defaultThreshold"] },
    changes: {
      before: { defaultThreshold: 30 },
      after: { defaultThreshold: 35 },
    },
    ipAddress: "10.0.0.50",
    timestamp: "2026-03-09T10:05:00Z",
  },
  {
    id: "log-007",
    actor: "admin@securebank.com",
    actorRole: "TENANT_ADMIN",
    action: "WEBHOOK_CREATED",
    tenantId: "secure-bank",
    resource: "webhook",
    resourceId: "wh-001",
    details: { name: "Production Webhook", url: "https://..." },
    ipAddress: "10.0.2.50",
    timestamp: "2026-03-09T10:00:00Z",
  },
  {
    id: "log-008",
    actor: "admin@acme.com",
    actorRole: "TENANT_ADMIN",
    action: "API_KEY_REVOKED",
    tenantId: "acme-corp",
    resource: "api-key",
    resourceId: "key-old-001",
    details: { name: "Old Key" },
    ipAddress: "10.0.1.100",
    timestamp: "2026-03-09T09:55:00Z",
  },
];

const actionColors: Record<AuditAction, string> = {
  TENANT_CREATED: "bg-green-500/10 text-green-500",
  TENANT_UPDATED: "bg-blue-500/10 text-blue-500",
  TENANT_SUSPENDED: "bg-yellow-500/10 text-yellow-500",
  TENANT_DELETED: "bg-red-500/10 text-red-500",
  WORKER_DRAINED: "bg-orange-500/10 text-orange-500",
  WORKER_RESTARTED: "bg-purple-500/10 text-purple-500",
  SETTINGS_UPDATED: "bg-cyan-500/10 text-cyan-500",
  API_KEY_CREATED: "bg-green-500/10 text-green-500",
  API_KEY_REVOKED: "bg-red-500/10 text-red-500",
  WEBHOOK_CREATED: "bg-green-500/10 text-green-500",
  WEBHOOK_DELETED: "bg-red-500/10 text-red-500",
};

const columns: ColumnDef<AuditLog>[] = [
  {
    accessorKey: "timestamp",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="Time" />
    ),
    cell: ({ row }) => {
      const date = new Date(row.getValue("timestamp"));
      return date.toLocaleString();
    },
  },
  {
    accessorKey: "actor",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="Actor" />
    ),
    cell: ({ row }) => (
      <div className="flex items-center gap-2">
        <User className="h-4 w-4 text-muted-foreground" />
        <span>{row.getValue("actor")}</span>
      </div>
    ),
  },
  {
    accessorKey: "actorRole",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="Role" />
    ),
    cell: ({ row }) => {
      const role = row.getValue("actorRole") as string;
      return <StatusBadge status={role.replace("_", " ")} />;
    },
  },
  {
    accessorKey: "action",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="Action" />
    ),
    cell: ({ row }) => {
      const action = row.getValue("action") as AuditAction;
      return (
        <span className={`rounded-full px-2 py-1 text-xs font-medium ${actionColors[action]}`}>
          {action.replace(/_/g, " ")}
        </span>
      );
    },
  },
  {
    accessorKey: "tenantId",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="Tenant" />
    ),
    cell: ({ row }) => {
      const tenant = row.getValue("tenantId") as string | undefined;
      return tenant || "-";
    },
  },
  {
    accessorKey: "resource",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="Resource" />
    ),
  },
  {
    id: "actions",
    cell: ({ row }) => {
      const log = row.original;
      return (
        <Button
          variant="ghost"
          size="sm"
          onClick={() => {
            toast.info(`View details for log: ${log.id}`);
          }}
        >
          <Eye className="h-4 w-4" />
        </Button>
      );
    },
  },
];

export default function AuditLogsPage() {
  const [pageIndex, setPageIndex] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [actorFilter, setActorFilter] = useState("");
  const [actionFilter, setActionFilter] = useState("");
  const [tenantFilter, setTenantFilter] = useState("");

  const { data } = useAuditLogs(pageIndex, pageSize, {
    actor: actorFilter || undefined,
    action: actionFilter as AuditAction || undefined,
    tenantId: tenantFilter || undefined,
  });

  const exportUrl = useExportAuditLogs({
    actor: actorFilter || undefined,
    action: actionFilter as AuditAction || undefined,
    tenantId: tenantFilter || undefined,
  });

  const logs = data?.logs || mockLogs;
  const totalPages = data?.totalPages || Math.ceil(mockLogs.length / pageSize);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">Audit Logs</h1>
          <p className="text-muted-foreground mt-1">
            Track all administrative actions on the platform
          </p>
        </div>
        <Button variant="outline" onClick={() => {
          window.open(exportUrl, "_blank");
          toast.success("Export started");
        }}>
          <Download className="mr-2 h-4 w-4" />
          Export CSV
        </Button>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Filters</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex flex-wrap items-end gap-4">
            <div className="space-y-2">
              <Label>Actor</Label>
              <div className="relative">
                <User className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                <Input
                  placeholder="Search by actor..."
                  value={actorFilter}
                  onChange={(e) => setActorFilter(e.target.value)}
                  className="pl-9 w-48"
                />
              </div>
            </div>
            <div className="space-y-2">
              <Label>Action</Label>
              <select
                className="h-10 rounded-md border border-input bg-background px-3 py-2 w-48"
                value={actionFilter}
                onChange={(e) => setActionFilter(e.target.value)}
              >
                <option value="">All Actions</option>
                <option value="TENANT_CREATED">Tenant Created</option>
                <option value="TENANT_UPDATED">Tenant Updated</option>
                <option value="TENANT_SUSPENDED">Tenant Suspended</option>
                <option value="TENANT_DELETED">Tenant Deleted</option>
                <option value="API_KEY_CREATED">API Key Created</option>
                <option value="API_KEY_REVOKED">API Key Revoked</option>
                <option value="WEBHOOK_CREATED">Webhook Created</option>
                <option value="WEBHOOK_DELETED">Webhook Deleted</option>
                <option value="WORKER_RESTARTED">Worker Restarted</option>
                <option value="SETTINGS_UPDATED">Settings Updated</option>
              </select>
            </div>
            <div className="space-y-2">
              <Label>Tenant</Label>
              <div className="relative">
                <Building className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                <Input
                  placeholder="Search by tenant..."
                  value={tenantFilter}
                  onChange={(e) => setTenantFilter(e.target.value)}
                  className="pl-9 w-48"
                />
              </div>
            </div>
            <Button
              variant="outline"
              onClick={() => {
                setActorFilter("");
                setActionFilter("");
                setTenantFilter("");
              }}
            >
              Clear Filters
            </Button>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Audit Log Entries</CardTitle>
        </CardHeader>
        <CardContent>
          <DataTable
            columns={columns}
            data={logs}
            pageCount={totalPages}
            pageIndex={pageIndex}
            pageSize={pageSize}
            onPaginationChange={(newPageIndex, newPageSize) => {
              setPageIndex(newPageIndex);
              setPageSize(newPageSize);
            }}
            enableSorting
            emptyMessage="No audit logs found"
          />
        </CardContent>
      </Card>
    </div>
  );
}
