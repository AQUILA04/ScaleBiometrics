"use client";

import { useState } from "react";
import Link from "next/link";
import { ColumnDef } from "@tanstack/react-table";
import { DataTable, DataTableColumnHeader, DataTableRowActions } from "@/components/ui/data-table";
import { StatusBadge } from "@/components/ui/status-badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useTenants } from "@/hooks/use-tenants";
import { Plus, Search, Filter } from "lucide-react";
import type { Tenant } from "@/types/tenant";
import { toast } from "sonner";

const mockTenants: Tenant[] = [
  {
    id: "1",
    tenantId: "acme-corp",
    name: "Acme Corporation",
    description: "Enterprise biometric authentication",
    status: "ACTIVE",
    shardStrategy: "RANGE",
    shardCount: 4,
    maxRecords: 100000,
    createdAt: "2025-01-15T10:00:00Z",
    updatedAt: "2026-03-01T14:30:00Z",
  },
  {
    id: "2",
    tenantId: "secure-bank",
    name: "SecureBank Inc",
    description: "Financial services verification",
    status: "ACTIVE",
    shardStrategy: "MODULO",
    shardCount: 8,
    maxRecords: 500000,
    createdAt: "2025-02-20T09:00:00Z",
    updatedAt: "2026-02-28T11:00:00Z",
  },
  {
    id: "3",
    tenantId: "health-id",
    name: "HealthID System",
    description: "Healthcare patient identification",
    status: "SUSPENDED",
    shardStrategy: "GEO",
    shardCount: 2,
    maxRecords: 250000,
    createdAt: "2025-03-10T08:00:00Z",
    updatedAt: "2026-01-15T16:00:00Z",
  },
  {
    id: "4",
    tenantId: "gov-portal",
    name: "Government Portal",
    description: "Citizen identity verification",
    status: "PENDING",
    shardStrategy: "RANGE",
    shardCount: 6,
    maxRecords: 1000000,
    createdAt: "2026-03-01T12:00:00Z",
    updatedAt: "2026-03-01T12:00:00Z",
  },
  {
    id: "5",
    tenantId: "edu-auth",
    name: "EduAuth",
    description: "Educational institution verification",
    status: "ACTIVE",
    shardStrategy: "MODULO",
    shardCount: 2,
    maxRecords: 50000,
    createdAt: "2025-06-15T10:00:00Z",
    updatedAt: "2026-02-20T09:00:00Z",
  },
];

const columns: ColumnDef<Tenant>[] = [
  {
    accessorKey: "tenantId",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="Tenant ID" />
    ),
    cell: ({ row }) => (
      <Link
        href={`/superadmin/tenants/${row.getValue("tenantId")}`}
        className="font-medium hover:underline"
      >
        {row.getValue("tenantId")}
      </Link>
    ),
  },
  {
    accessorKey: "name",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="Name" />
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
    accessorKey: "shardStrategy",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="Sharding" />
    ),
  },
  {
    accessorKey: "maxRecords",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="Max Records" />
    ),
    cell: ({ row }) => {
      const value = row.getValue("maxRecords") as number;
      return value.toLocaleString();
    },
  },
  {
    accessorKey: "createdAt",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="Created" />
    ),
    cell: ({ row }) => {
      const date = new Date(row.getValue("createdAt"));
      return date.toLocaleDateString();
    },
  },
  {
    id: "actions",
    cell: ({ row }) => (
      <DataTableRowActions
        row={row.original}
        onView={(row) => {
          window.location.href = `/superadmin/tenants/${row.tenantId}`;
        }}
        onEdit={(row) => {
          window.location.href = `/superadmin/tenants/${row.tenantId}/edit`;
        }}
        onDelete={(row) => {
          toast.error(`Delete tenant: ${row.tenantId}`);
        }}
        customActions={[
          {
            label: "Suspend",
            onClick: (row) => {
              toast.info(`Suspend tenant: ${row.tenantId}`);
            },
          },
        ]}
      />
    ),
  },
];

export default function TenantsPage() {
  const [pageIndex, setPageIndex] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [searchTerm, setSearchTerm] = useState("");

  const { data } = useTenants(pageIndex, pageSize, {
    search: searchTerm,
  });

  const tenants = data?.content || mockTenants;
  const totalPages = data?.totalPages || Math.ceil(mockTenants.length / pageSize);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">Tenants</h1>
          <p className="text-muted-foreground mt-1">
            Manage and monitor all tenants on the platform
          </p>
        </div>
        <Button asChild>
          <Link href="/superadmin/tenants/create">
            <Plus className="mr-2 h-4 w-4" />
            Create Tenant
          </Link>
        </Button>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Tenant List</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex items-center gap-4 mb-4">
            <div className="relative flex-1 max-w-sm">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <input
                type="text"
                placeholder="Search tenants..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 pl-9 text-sm ring-offset-background file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
              />
            </div>
            <Button variant="outline" size="sm">
              <Filter className="mr-2 h-4 w-4" />
              Filters
            </Button>
          </div>

          <DataTable
            columns={columns}
            data={tenants}
            pageCount={totalPages}
            pageIndex={pageIndex}
            pageSize={pageSize}
            onPaginationChange={(newPageIndex, newPageSize) => {
              setPageIndex(newPageIndex);
              setPageSize(newPageSize);
            }}
            enableSorting
            enableFiltering
            emptyMessage="No tenants found"
          />
        </CardContent>
      </Card>
    </div>
  );
}
