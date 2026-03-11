import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import type { TenantSettings, UpdateSettingsRequest } from "@/types/tenant-settings";

async function fetchTenantSettings(tenantId: string): Promise<TenantSettings> {
  const response = await fetch(`/api/proxy/v1/tenants/${tenantId}/settings`);
  if (!response.ok) {
    throw new Error("Failed to fetch tenant settings");
  }
  return response.json();
}

async function updateTenantSettings(tenantId: string, request: UpdateSettingsRequest): Promise<{ success: boolean; updatedAt: string }> {
  const response = await fetch(`/api/proxy/v1/tenants/${tenantId}/settings`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
  if (!response.ok) {
    throw new Error("Failed to update tenant settings");
  }
  return response.json();
}

async function uploadLogo(tenantId: string, file: File): Promise<{ success: boolean; logoUrl: string }> {
  const formData = new FormData();
  formData.append("file", file);
  
  const response = await fetch(`/api/proxy/v1/tenants/${tenantId}/settings/logo`, {
    method: "POST",
    body: formData,
  });
  if (!response.ok) {
    throw new Error("Failed to upload logo");
  }
  return response.json();
}

export function useTenantSettings(tenantId: string) {
  return useQuery({
    queryKey: ["tenant-settings", tenantId],
    queryFn: () => fetchTenantSettings(tenantId),
  });
}

export function useUpdateTenantSettings() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ tenantId, request }: { tenantId: string; request: UpdateSettingsRequest }) =>
      updateTenantSettings(tenantId, request),
    onSuccess: (_, { tenantId }) => {
      queryClient.invalidateQueries({ queryKey: ["tenant-settings", tenantId] });
    },
  });
}

export function useUploadLogo() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ tenantId, file }: { tenantId: string; file: File }) =>
      uploadLogo(tenantId, file),
    onSuccess: () => {
      // Logo upload doesn't require invalidation as it's included in settings
    },
  });
}
