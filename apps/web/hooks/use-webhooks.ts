import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import type { Webhook, CreateWebhookRequest, WebhookTestResult, PaginatedDeliveries } from "@/types/webhook";

async function fetchWebhooks(tenantId: string): Promise<Webhook[]> {
  const response = await fetch(`/api/proxy/v1/tenants/${tenantId}/webhooks`);
  if (!response.ok) {
    throw new Error("Failed to fetch webhooks");
  }
  const data = await response.json();
  return data.webhooks || [];
}

async function createWebhook(tenantId: string, request: CreateWebhookRequest): Promise<Webhook> {
  const response = await fetch(`/api/proxy/v1/tenants/${tenantId}/webhooks`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
  if (!response.ok) {
    throw new Error("Failed to create webhook");
  }
  return response.json();
}

async function updateWebhook(tenantId: string, webhookId: string, request: Partial<CreateWebhookRequest>): Promise<Webhook> {
  const response = await fetch(`/api/proxy/v1/tenants/${tenantId}/webhooks/${webhookId}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
  if (!response.ok) {
    throw new Error("Failed to update webhook");
  }
  return response.json();
}

async function deleteWebhook(tenantId: string, webhookId: string): Promise<void> {
  const response = await fetch(`/api/proxy/v1/tenants/${tenantId}/webhooks/${webhookId}`, {
    method: "DELETE",
  });
  if (!response.ok) {
    throw new Error("Failed to delete webhook");
  }
}

async function testWebhook(tenantId: string, webhookId: string): Promise<WebhookTestResult> {
  const response = await fetch(`/api/proxy/v1/tenants/${tenantId}/webhooks/${webhookId}/test`, {
    method: "POST",
  });
  if (!response.ok) {
    throw new Error("Failed to test webhook");
  }
  return response.json();
}

async function fetchDeliveryHistory(
  tenantId: string,
  webhookId: string,
  page: number = 0,
  size: number = 10
): Promise<PaginatedDeliveries> {
  const response = await fetch(
    `/api/proxy/v1/tenants/${tenantId}/webhooks/${webhookId}/deliveries?page=${page}&size=${size}`
  );
  if (!response.ok) {
    throw new Error("Failed to fetch delivery history");
  }
  return response.json();
}

export function useWebhooks(tenantId: string) {
  return useQuery({
    queryKey: ["webhooks", tenantId],
    queryFn: () => fetchWebhooks(tenantId),
  });
}

export function useCreateWebhook() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ tenantId, request }: { tenantId: string; request: CreateWebhookRequest }) =>
      createWebhook(tenantId, request),
    onSuccess: (_, { tenantId }) => {
      queryClient.invalidateQueries({ queryKey: ["webhooks", tenantId] });
    },
  });
}

export function useUpdateWebhook() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ tenantId, webhookId, request }: { tenantId: string; webhookId: string; request: Partial<CreateWebhookRequest> }) =>
      updateWebhook(tenantId, webhookId, request),
    onSuccess: (_, { tenantId }) => {
      queryClient.invalidateQueries({ queryKey: ["webhooks", tenantId] });
    },
  });
}

export function useDeleteWebhook() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ tenantId, webhookId }: { tenantId: string; webhookId: string }) =>
      deleteWebhook(tenantId, webhookId),
    onSuccess: (_, { tenantId }) => {
      queryClient.invalidateQueries({ queryKey: ["webhooks", tenantId] });
    },
  });
}

export function useTestWebhook() {
  return useMutation({
    mutationFn: ({ tenantId, webhookId }: { tenantId: string; webhookId: string }) =>
      testWebhook(tenantId, webhookId),
  });
}

export function useDeliveryHistory(tenantId: string, webhookId: string, page: number = 0, size: number = 10) {
  return useQuery({
    queryKey: ["webhook-deliveries", tenantId, webhookId, page, size],
    queryFn: () => fetchDeliveryHistory(tenantId, webhookId, page, size),
    enabled: !!webhookId,
  });
}
