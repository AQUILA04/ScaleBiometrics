import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import type { ApiKey, CreateApiKeyRequest, CreatedApiKey } from "@/types/api-key";

async function fetchApiKeys(tenantId: string): Promise<ApiKey[]> {
  const response = await fetch(`/api/proxy/v1/tenants/${tenantId}/api-keys`);
  if (!response.ok) {
    throw new Error("Failed to fetch API keys");
  }
  const data = await response.json();
  return data.keys || [];
}

async function createApiKey(tenantId: string, request: CreateApiKeyRequest): Promise<CreatedApiKey> {
  const response = await fetch(`/api/proxy/v1/tenants/${tenantId}/api-keys`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
  if (!response.ok) {
    throw new Error("Failed to create API key");
  }
  return response.json();
}

async function revokeApiKey(tenantId: string, keyId: string): Promise<void> {
  const response = await fetch(`/api/proxy/v1/tenants/${tenantId}/api-keys/${keyId}/revoke`, {
    method: "POST",
  });
  if (!response.ok) {
    throw new Error("Failed to revoke API key");
  }
}

async function deleteApiKey(tenantId: string, keyId: string): Promise<void> {
  const response = await fetch(`/api/proxy/v1/tenants/${tenantId}/api-keys/${keyId}`, {
    method: "DELETE",
  });
  if (!response.ok) {
    throw new Error("Failed to delete API key");
  }
}

export function useApiKeys(tenantId: string) {
  return useQuery({
    queryKey: ["api-keys", tenantId],
    queryFn: () => fetchApiKeys(tenantId),
  });
}

export function useCreateApiKey() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ tenantId, request }: { tenantId: string; request: CreateApiKeyRequest }) =>
      createApiKey(tenantId, request),
    onSuccess: (_, { tenantId }) => {
      queryClient.invalidateQueries({ queryKey: ["api-keys", tenantId] });
    },
  });
}

export function useRevokeApiKey() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ tenantId, keyId }: { tenantId: string; keyId: string }) =>
      revokeApiKey(tenantId, keyId),
    onSuccess: (_, { tenantId }) => {
      queryClient.invalidateQueries({ queryKey: ["api-keys", tenantId] });
    },
  });
}

export function useDeleteApiKey() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ tenantId, keyId }: { tenantId: string; keyId: string }) =>
      deleteApiKey(tenantId, keyId),
    onSuccess: (_, { tenantId }) => {
      queryClient.invalidateQueries({ queryKey: ["api-keys", tenantId] });
    },
  });
}
