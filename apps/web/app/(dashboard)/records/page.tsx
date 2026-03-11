"use client";

import { useState } from "react";
import Link from "next/link";
import { ColumnDef } from "@tanstack/react-table";
import { DataTable, DataTableColumnHeader } from "@/components/ui/data-table";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useRecords } from "@/hooks/use-records";
import { Search, Plus, Eye, Trash2, Fingerprint } from "lucide-react";
import type { BiometricRecord } from "@/types/record";
import { toast } from "sonner";

const mockRecords: BiometricRecord[] = [
  {
    rid: "RID-001",
    name: "John Doe",
    email: "john.doe@example.com",
    phone: "+1234567890",
    metadata: { department: "HR", employeeId: "EMP-001" },
    createdAt: "2025-01-15T10:00:00Z",
    updatedAt: "2026-03-01T14:30:00Z",
    fingerprintCount: 10,
    lastMatchAt: "2026-03-09T10:35:00Z",
  },
  {
    rid: "RID-002",
    name: "Jane Smith",
    email: "jane.smith@example.com",
    phone: "+1234567891",
    metadata: { department: "Engineering", employeeId: "EMP-002" },
    createdAt: "2025-02-20T09:00:00Z",
    updatedAt: "2026-02-28T11:00:00Z",
    fingerprintCount: 8,
    lastMatchAt: "2026-03-09T10:32:00Z",
  },
  {
    rid: "RID-003",
    name: "Bob Johnson",
    email: "bob.johnson@example.com",
    phone: "+1234567892",
    metadata: { department: "Sales", employeeId: "EMP-003" },
    createdAt: "2025-03-10T08:00:00Z",
    updatedAt: "2026-01-15T16:00:00Z",
    fingerprintCount: 10,
    lastMatchAt: "2026-03-09T10:28:00Z",
  },
  {
    rid: "RID-004",
    name: "Alice Williams",
    email: "alice.williams@example.com",
    phone: "+1234567893",
    metadata: { department: "Marketing", employeeId: "EMP-004" },
    createdAt: "2025-04-05T12:00:00Z",
    updatedAt: "2026-02-20T09:00:00Z",
    fingerprintCount: 6,
    lastMatchAt: "2026-03-09T10:25:00Z",
  },
  {
    rid: "RID-005",
    name: "Charlie Brown",
    email: "charlie.brown@example.com",
    phone: "+1234567894",
    metadata: { department: "Finance", employeeId: "EMP-005" },
    createdAt: "2025-05-12T10:00:00Z",
    updatedAt: "2026-03-05T14:00:00Z",
    fingerprintCount: 10,
    lastMatchAt: "2026-03-09T10:20:00Z",
  },
  {
    rid: "RID-006",
    name: "Diana Ross",
    email: "diana.ross@example.com",
    phone: "+1234567895",
    metadata: { department: "HR", employeeId: "EMP-006" },
    createdAt: "2025-06-18T09:00:00Z",
    updatedAt: "2026-02-10T11:00:00Z",
    fingerprintCount: 10,
    lastMatchAt: "2026-03-09T10:15:00Z",
  },
  {
    rid: "RID-007",
    name: "Edward Miller",
    email: "edward.miller@example.com",
    phone: "+1234567896",
    metadata: { department: "Operations", employeeId: "EMP-007" },
    createdAt: "2025-07-22T08:00:00Z",
    updatedAt: "2026-01-25T16:00:00Z",
    fingerprintCount: 4,
    lastMatchAt: "2026-03-09T10:10:00Z",
  },
  {
    rid: "RID-008",
    name: "Fiona Garcia",
    email: "fiona.garcia@example.com",
    phone: "+1234567897",
    metadata: { department: "Engineering", employeeId: "EMP-008" },
    createdAt: "2025-08-30T12:00:00Z",
    updatedAt: "2026-03-01T09:00:00Z",
    fingerprintCount: 10,
    lastMatchAt: "2026-03-09T10:05:00Z",
  },
  {
    rid: "RID-009",
    name: "George Wilson",
    email: "george.wilson@example.com",
    phone: "+1234567898",
    metadata: { department: "Legal", employeeId: "EMP-009" },
    createdAt: "2025-09-14T10:00:00Z",
    updatedAt: "2026-02-15T14:00:00Z",
    fingerprintCount: 8,
  },
  {
    rid: "RID-010",
    name: "Hannah Lee",
    email: "hannah.lee@example.com",
    phone: "+1234567899",
    metadata: { department: "Engineering", employeeId: "EMP-010" },
    createdAt: "2025-10-20T09:00:00Z",
    updatedAt: "2026-03-08T11:00:00Z",
    fingerprintCount: 10,
    lastMatchAt: "2026-03-09T10:00:00Z",
  },
];

const columns: ColumnDef<BiometricRecord>[] = [
  {
    accessorKey: "rid",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="RID" />
    ),
    cell: ({ row }) => (
      <Link
        href={`/records/${row.getValue("rid")}`}
        className="font-medium hover:underline"
      >
        {row.getValue("rid")}
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
    accessorKey: "email",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="Email" />
    ),
  },
  {
    accessorKey: "metadata",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="Department" />
    ),
    cell: ({ row }) => {
      const metadata = row.getValue("metadata") as Record<string, string> | undefined;
      return metadata?.department || "-";
    },
  },
  {
    accessorKey: "fingerprintCount",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="Fingerprints" />
    ),
    cell: ({ row }) => {
      const count = row.getValue("fingerprintCount") as number;
      return (
        <div className="flex items-center gap-1">
          <Fingerprint className="h-4 w-4 text-muted-foreground" />
          {count}
        </div>
      );
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
    accessorKey: "lastMatchAt",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="Last Match" />
    ),
    cell: ({ row }) => {
      const date = row.getValue("lastMatchAt") as string | undefined;
      return date ? new Date(date).toLocaleString() : "-";
    },
  },
  {
    id: "actions",
    cell: ({ row }) => {
      const record = row.original;
      return (
        <div className="flex items-center gap-2">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => {
              toast.info(`View record: ${record.rid}`);
            }}
          >
            <Eye className="h-4 w-4" />
          </Button>
          <Button
            variant="ghost"
            size="sm"
            onClick={() => {
              toast.error(`Delete record: ${record.rid}`);
            }}
            className="text-red-500 hover:text-red-600"
          >
            <Trash2 className="h-4 w-4" />
          </Button>
        </div>
      );
    },
  },
];

export default function RecordsPage() {
  const [pageIndex, setPageIndex] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [searchTerm, setSearchTerm] = useState("");

  const { data } = useRecords("default", pageIndex, pageSize, searchTerm || undefined);

  const records = data?.content || mockRecords;
  const totalPages = data?.totalPages || Math.ceil(mockRecords.length / pageSize);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">Records</h1>
          <p className="text-muted-foreground mt-1">
            Manage biometric records for your organization
          </p>
        </div>
        <Button>
          <Plus className="mr-2 h-4 w-4" />
          Add Record
        </Button>
      </div>

      <div className="grid gap-4 md:grid-cols-3">
        <Card>
          <CardContent className="pt-6">
            <div className="text-center">
              <p className="text-3xl font-bold">{mockRecords.length}</p>
              <p className="text-sm text-muted-foreground">Total Records</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="text-center">
              <p className="text-3xl font-bold">
                {mockRecords.reduce((sum, r) => sum + r.fingerprintCount, 0)}
              </p>
              <p className="text-sm text-muted-foreground">Total Fingerprints</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="text-center">
              <p className="text-3xl font-bold">
                {mockRecords.filter(r => r.lastMatchAt).length}
              </p>
              <p className="text-sm text-muted-foreground">Active Records</p>
            </div>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Biometric Records</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex items-center gap-4 mb-4">
            <div className="relative flex-1 max-w-sm">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <input
                type="text"
                placeholder="Search by RID or name..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 pl-9 text-sm"
              />
            </div>
          </div>

          <DataTable
            columns={columns}
            data={records}
            pageCount={totalPages}
            pageIndex={pageIndex}
            pageSize={pageSize}
            onPaginationChange={(newPageIndex, newPageSize) => {
              setPageIndex(newPageIndex);
              setPageSize(newPageSize);
            }}
            enableSorting
            emptyMessage="No records found"
          />
        </CardContent>
      </Card>
    </div>
  );
}
