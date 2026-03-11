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
import { useWebhooks } from "@/hooks/use-webhooks";
import { Plus, Webhook, Play, Trash2, Clock, CheckCircle, XCircle } from "lucide-react";
import type { Webhook as WebhookType, WebhookEvent } from "@/types/webhook";
import { toast } from "sonner";

const mockWebhooks: WebhookType[] = [
  {
    id: "wh-001",
    name: "Production Webhook",
    url: "https://api.example.com/webhooks/scalebiometrics",
    events: ["job.completed", "job.failed"],
    status: "ACTIVE",
    healthStatus: "healthy",
    lastCheckAt: "2026-03-09T10:30:00Z",
    retryPolicy: { maxRetries: 3, retryDelay: 1000 },
    createdAt: "2026-01-15T10:00:00Z",
    updatedAt: "2026-03-01T14:30:00Z",
  },
  {
    id: "wh-002",
    name: "Staging Webhook",
    url: "https://staging.example.com/webhooks/biometrics",
    events: ["job.completed"],
    status: "ACTIVE",
    healthStatus: "degraded",
    lastCheckAt: "2026-03-09T10:25:00Z",
    retryPolicy: { maxRetries: 2, retryDelay: 500 },
    createdAt: "2026-02-20T09:00:00Z",
    updatedAt: "2026-02-28T11:00:00Z",
  },
  {
    id: "wh-003",
    name: "Backup Notification",
    url: "https://backup.example.com/notifications",
    events: ["job.completed", "job.failed", "job.cancelled"],
    status: "INACTIVE",
    healthStatus: "unknown",
    retryPolicy: { maxRetries: 5, retryDelay: 2000 },
    createdAt: "2025-12-01T10:00:00Z",
    updatedAt: "2026-01-10T16:00:00Z",
  },
];

const eventLabels: Record<WebhookEvent, string> = {
  "job.queued": "Job Queued",
  "job.processing": "Job Processing",
  "job.completed": "Job Completed",
  "job.failed": "Job Failed",
  "job.cancelled": "Job Cancelled",
  "record.created": "Record Created",
  "record.deleted": "Record Deleted",
};

const columns: ColumnDef<WebhookType>[] = [
  {
    accessorKey: "name",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="Name" />
    ),
  },
  {
    accessorKey: "url",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="URL" />
    ),
    cell: ({ row }) => (
      <code className="text-xs break-all">{row.getValue("url")}</code>
    ),
  },
  {
    accessorKey: "events",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="Events" />
    ),
    cell: ({ row }) => {
      const events = row.getValue("events") as WebhookEvent[];
      return (
        <div className="flex flex-wrap gap-1">
          {events.slice(0, 2).map((event) => (
            <span key={event} className="text-xs bg-primary/10 text-primary px-2 py-0.5 rounded">
              {eventLabels[event]}
            </span>
          ))}
          {events.length > 2 && (
            <span className="text-xs text-muted-foreground">+{events.length - 2}</span>
          )}
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
    accessorKey: "healthStatus",
    header: ({ column }) => (
      <DataTableColumnHeader column={column} title="Health" />
    ),
    cell: ({ row }) => {
      const status = row.getValue("healthStatus") as string;
      const colors: Record<string, string> = {
        healthy: "text-green-500",
        degraded: "text-yellow-500",
        down: "text-red-500",
        unknown: "text-muted-foreground",
      };
      return (
        <div className="flex items-center gap-1">
          {status === "healthy" && <CheckCircle className={`h-4 w-4 ${colors[status]}`} />}
          {status === "degraded" && <Clock className={`h-4 w-4 ${colors[status]}`} />}
          {status === "down" && <XCircle className={`h-4 w-4 ${colors[status]}`} />}
          <span className={`capitalize ${colors[status]}`}>{status}</span>
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
    id: "actions",
    cell: ({ row }) => {
      return (
        <DataTableRowActions
          row={row.original}
          onView={() => {}}
          customActions={[
            {
              label: "Test",
              onClick: (row) => {
                toast.info(`Testing webhook: ${row.name}`);
              },
              icon: <Play className="h-4 w-4" />,
            },
            {
              label: "Delete",
              onClick: (row) => {
                toast.error(`Deleted webhook: ${row.name}`);
              },
              icon: <Trash2 className="h-4 w-4" />,
            },
          ]}
        />
      );
    },
  },
];

export default function WebhooksPage() {
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [newWebhookName, setNewWebhookName] = useState("");
  const [newWebhookUrl, setNewWebhookUrl] = useState("");
  const [selectedEvents, setSelectedEvents] = useState<WebhookEvent[]>([]);
  const [maxRetries, setMaxRetries] = useState(3);
  const [retryDelay, setRetryDelay] = useState(1000);

  const { data: webhooks } = useWebhooks("default");

  const hooks = webhooks || mockWebhooks;
  const activeHooks = hooks.filter(h => h.status === "ACTIVE");
  const healthyHooks = hooks.filter(h => h.healthStatus === "healthy");

  const handleCreateWebhook = () => {
    if (!newWebhookName.trim() || !newWebhookUrl.trim()) {
      toast.error("Please fill in all required fields");
      return;
    }
    if (selectedEvents.length === 0) {
      toast.error("Please select at least one event");
      return;
    }
    toast.success(`Webhook "${newWebhookName}" created successfully`);
    setShowCreateModal(false);
    setNewWebhookName("");
    setNewWebhookUrl("");
    setSelectedEvents([]);
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">Webhooks</h1>
          <p className="text-muted-foreground mt-1">
            Configure webhooks for real-time notifications
          </p>
        </div>
        <Button onClick={() => setShowCreateModal(true)}>
          <Plus className="mr-2 h-4 w-4" />
          Create Webhook
        </Button>
      </div>

      <div className="grid gap-4 md:grid-cols-3">
        <Card>
          <CardContent className="pt-6">
            <div className="text-center">
              <p className="text-3xl font-bold">{hooks.length}</p>
              <p className="text-sm text-muted-foreground">Total Webhooks</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="text-center">
              <p className="text-3xl font-bold text-green-500">{activeHooks.length}</p>
              <p className="text-sm text-muted-foreground">Active</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="text-center">
              <p className="text-3xl font-bold text-green-500">{healthyHooks.length}</p>
              <p className="text-sm text-muted-foreground">Healthy</p>
            </div>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Webhook Endpoints</CardTitle>
        </CardHeader>
        <CardContent>
          <DataTable
            columns={columns}
            data={hooks}
            enableSorting
            emptyMessage="No webhooks configured"
          />
        </CardContent>
      </Card>

      {showCreateModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <Card className="w-full max-w-lg">
            <CardHeader>
              <CardTitle>Create Webhook</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="webhookName">Name</Label>
                <Input
                  id="webhookName"
                  placeholder="e.g., Production Webhook"
                  value={newWebhookName}
                  onChange={(e) => setNewWebhookName(e.target.value)}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="webhookUrl">URL</Label>
                <Input
                  id="webhookUrl"
                  placeholder="https://api.example.com/webhooks"
                  value={newWebhookUrl}
                  onChange={(e) => setNewWebhookUrl(e.target.value)}
                />
              </div>
              <div className="space-y-2">
                <Label>Events</Label>
                <div className="grid grid-cols-2 gap-2">
                  {Object.entries(eventLabels).map(([event, label]) => (
                    <div key={event} className="flex items-center gap-2">
                      <Checkbox
                        id={event}
                        checked={selectedEvents.includes(event as WebhookEvent)}
                        onCheckedChange={(checked) => {
                          if (checked) {
                            setSelectedEvents([...selectedEvents, event as WebhookEvent]);
                          } else {
                            setSelectedEvents(selectedEvents.filter(e => e !== event));
                          }
                        }}
                      />
                      <label htmlFor={event} className="text-sm">{label}</label>
                    </div>
                  ))}
                </div>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="maxRetries">Max Retries</Label>
                  <Input
                    id="maxRetries"
                    type="number"
                    min={0}
                    max={10}
                    value={maxRetries}
                    onChange={(e) => setMaxRetries(parseInt(e.target.value))}
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="retryDelay">Retry Delay (ms)</Label>
                  <Input
                    id="retryDelay"
                    type="number"
                    min={100}
                    step={100}
                    value={retryDelay}
                    onChange={(e) => setRetryDelay(parseInt(e.target.value))}
                  />
                </div>
              </div>
              <div className="flex gap-2 pt-4">
                <Button variant="outline" className="flex-1" onClick={() => setShowCreateModal(false)}>
                  Cancel
                </Button>
                <Button className="flex-1" onClick={handleCreateWebhook}>
                  <Webhook className="mr-2 h-4 w-4" />
                  Create Webhook
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  );
}
