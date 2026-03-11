"use client";

import { useState } from "react";
import { ColumnDef } from "@tanstack/react-table";
import { DataTable, DataTableColumnHeader, DataTableRowActions } from "@/components/ui/data-table";
import { StatusBadge } from "@/components/ui/status-badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Checkbox } from "@/components/ui/checkbox";
import { useApiKeys } from "@/hooks/use-api-keys";
import { Plus, Copy, Check, Key, AlertTriangle } from "lucide-react";
import type { ApiKey, ApiKeyScope } from "@/types/api-key";
import { toast } from "sonner";

const mockApiKeys: ApiKey[] = [
  {
    id: "key-001",
    name: "Production API Key",
    prefix: "sk_live_",
    scopes: ["read", "write", "matching", "admin"],
    status: "ACTIVE",
    expiresAt: "2026-12-31T23:59:59Z",
    createdAt: "2026-01-15T10:00:00Z",
    lastUsedAt: "2026-03-09T10:30:00Z",
  },
  {
    id: "key-002",
    name: "Development API Key",
    prefix: "sk_test_",
    scopes: ["read", "matching"],
    status: "ACTIVE",
    expiresAt: "2026-04-08T23:59:59Z",
    createdAt: "2026-02-20T09:00:00Z",
    lastUsedAt: "2026-03-08T16:45:00Z",
  },
  {
    id: "key-003",
    name: "Old API Key",
    prefix: "sk_live_",
    scopes: ["read"],
    status: "REVOKED",
    expiresAt: "2025-12-31T23:59:59Z",
    createdAt: "2025-06-01T10:00:00Z",
    lastUsedAt: "2025-11-15T10:30:00Z",
  },
  {
    id: "key-004",
    name: "Temporary Key",
    prefix: "sk_live_",
    scopes: ["read", "write"],
    status: "EXPIRED",
    expiresAt: "2026-01-01T00:00:00Z",
    createdAt: "2025-12-01T10:00:00Z",
  },
];

const scopeLabels: Record<ApiKeyScope, string> = {
  read: "Read",
  write: "Write",
  delete: "Delete",
  matching: "Matching",
  admin: "Admin",
};

const columns: ColumnDef<ApiKey>[] = [
  {
    accessorKey: "name",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="Name" />
    ),
  },
  {
    accessorKey: "prefix",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="Key Prefix" />
    ),
    cell: ({ row }) => (
      <code className="text-xs bg-muted px-2 py-1 rounded">
        {row.getValue("prefix")}****
      </code>
    ),
  },
  {
    accessorKey: "scopes",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="Scopes" />
    ),
    cell: ({ row }) => {
      const scopes = row.getValue("scopes") as ApiKeyScope[];
      return (
        <div className="flex flex-wrap gap-1">
          {scopes.map((scope) => (
            <span
              key={scope}
              className="text-xs bg-primary/10 text-primary px-2 py-0.5 rounded"
            >
              {scopeLabels[scope]}
            </span>
          ))}
        </div>
      );
    },
  },
  {
    accessorKey: "status",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="Status" />
    ),
    cell: ({ row }) => <StatusBadge status={row.getValue("status")} />,
  },
  {
    accessorKey: "expiresAt",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="Expires" />
    ),
    cell: ({ row }) => {
      const date = new Date(row.getValue("expiresAt"));
      return date.toLocaleDateString();
    },
  },
  {
    accessorKey: "lastUsedAt",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="Last Used" />
    ),
    cell: ({ row }) => {
      const lastUsed = row.getValue("lastUsedAt") as string | undefined;
      return lastUsed ? new Date(lastUsed).toLocaleString() : "Never";
    },
  },
  {
    id: "actions",
    cell: ({ row }) => {
      const key = row.original;
      if (key.status !== "ACTIVE") return null;
      return (
        <DataTableRowActions
          row={row.original}
          customActions={[
            {
              label: "Revoke",
              onClick: (row) => {
                toast.success(`Revoked API key: ${row.name}`);
              },
              icon: <AlertTriangle className="h-4 w-4" />,
            },
          ]}
        />
      );
    },
  },
];

export default function ApiKeysPage() {
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [newKeyName, setNewKeyName] = useState("");
  const [selectedScopes, setSelectedScopes] = useState<ApiKeyScope[]>(["read"]);
  const [expiration, setExpiration] = useState("30d");
  const [createdKey, setCreatedKey] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);

  const { data: apiKeys } = useApiKeys("default");

  const keys = apiKeys || mockApiKeys;
  const activeKeys = keys.filter(k => k.status === "ACTIVE");
  const revokedKeys = keys.filter(k => k.status === "REVOKED");

  const handleCopy = () => {
    if (createdKey) {
      navigator.clipboard.writeText(createdKey);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  const handleCreateKey = () => {
    if (!newKeyName.trim()) {
      toast.error("Please enter a name for the API key");
      return;
    }
    if (selectedScopes.length === 0) {
      toast.error("Please select at least one scope");
      return;
    }
    // Mock - in real implementation would call createApiKey.mutate
    setCreatedKey("sk_live_abc123xyz789def456ghi");
    toast.success("API key created successfully");
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">API Keys</h1>
          <p className="text-muted-foreground mt-1">
            Manage API keys for your applications
          </p>
        </div>
        <Button onClick={() => setShowCreateModal(true)}>
          <Plus className="mr-2 h-4 w-4" />
          Create API Key
        </Button>
      </div>

      <div className="grid gap-4 md:grid-cols-3">
        <Card>
          <CardContent className="pt-6">
            <div className="text-center">
              <p className="text-3xl font-bold">{keys.length}</p>
              <p className="text-sm text-muted-foreground">Total Keys</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="text-center">
              <p className="text-3xl font-bold text-green-500">{activeKeys.length}</p>
              <p className="text-sm text-muted-foreground">Active Keys</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="text-center">
              <p className="text-3xl font-bold text-muted-foreground">{revokedKeys.length}</p>
              <p className="text-sm text-muted-foreground">Revoked/Expired</p>
            </div>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>API Keys</CardTitle>
        </CardHeader>
        <CardContent>
          <DataTable
            columns={columns}
            data={keys}
            enableSorting
            emptyMessage="No API keys found"
          />
        </CardContent>
      </Card>

      {showCreateModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <Card className="w-full max-w-md">
            <CardHeader>
              <CardTitle>Create API Key</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              {createdKey ? (
                <div className="space-y-4">
                  <div className="p-4 bg-green-50 border border-green-200 rounded-lg">
                    <div className="flex items-center gap-2 text-green-700 mb-2">
                      <Check className="h-4 w-4" />
                      <span className="font-medium">API Key Created</span>
                    </div>
                    <p className="text-sm text-green-600">
                      Copy this key now. You won&apos;t be able to see it again!
                    </p>
                  </div>
                  <div className="flex gap-2">
                    <code className="flex-1 p-3 bg-muted rounded-md text-sm break-all">
                      {createdKey}
                    </code>
                    <Button variant="outline" size="icon" onClick={handleCopy}>
                      {copied ? <Check className="h-4 w-4" /> : <Copy className="h-4 w-4" />}
                    </Button>
                  </div>
                  <Button className="w-full" onClick={() => {
                    setShowCreateModal(false);
                    setCreatedKey(null);
                    setNewKeyName("");
                    setSelectedScopes(["read"]);
                  }}>
                    Done
                  </Button>
                </div>
              ) : (
                <>
                  <div className="space-y-2">
                    <Label htmlFor="keyName">Key Name</Label>
                    <Input
                      id="keyName"
                      placeholder="e.g., Production API Key"
                      value={newKeyName}
                      onChange={(e) => setNewKeyName(e.target.value)}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label>Scopes</Label>
                    <div className="space-y-2">
                      {(["read", "write", "delete", "matching", "admin"] as ApiKeyScope[]).map((scope) => (
                        <div key={scope} className="flex items-center gap-2">
                          <Checkbox
                            id={scope}
                            checked={selectedScopes.includes(scope)}
                            onCheckedChange={(checked) => {
                              if (checked) {
                                setSelectedScopes([...selectedScopes, scope]);
                              } else {
                                setSelectedScopes(selectedScopes.filter(s => s !== scope));
                              }
                            }}
                          />
                          <label htmlFor={scope} className="text-sm">
                            {scopeLabels[scope]}
                          </label>
                        </div>
                      ))}
                    </div>
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="expiration">Expiration</Label>
                    <select
                      id="expiration"
                      className="w-full h-10 rounded-md border border-input bg-background px-3 py-2"
                      value={expiration}
                      onChange={(e) => setExpiration(e.target.value)}
                    >
                      <option value="7d">7 days</option>
                      <option value="30d">30 days</option>
                      <option value="90d">90 days</option>
                      <option value="1y">1 year</option>
                      <option value="never">Never</option>
                    </select>
                  </div>
                  <div className="flex gap-2">
                    <Button variant="outline" className="flex-1" onClick={() => setShowCreateModal(false)}>
                      Cancel
                    </Button>
                    <Button className="flex-1" onClick={handleCreateKey}>
                      <Key className="mr-2 h-4 w-4" />
                      Create Key
                    </Button>
                  </div>
                </>
              )}
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  );
}
