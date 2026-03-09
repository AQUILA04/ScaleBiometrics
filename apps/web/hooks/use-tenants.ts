import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import type { Tenant, TenantWithUsage } from "@/types/tenant";

interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

interface TenantFilters {
  status?: string;
  search?: string;
}

async function fetchTenants(
  page: number = 0,
  size: number = 10,
  filters?: TenantFilters
): Promise<PaginatedResponse<Tenant>> {
  const params = new URLSearchParams({
    page: page.toString(),
    size: size.toString(),
  });
  
  if (filters?.status) {
    params.append("status", filters.status);
  }
  if (filters?.search) {
    params.append("search", filters.search);
  }

  const response = await fetch(`/api/proxy/v1/tenants?${params}`);
  if (!response.ok) {
    throw new Error("Failed to fetch tenants");
  }
  return response.json();
}

async function fetchTenant(tenantId: string): Promise<TenantWithUsage> {
  const response = await fetch(`/api/proxy/v1/tenants/${tenantId}`);
  if (!response.ok) {
    throw new Error("Failed to fetch tenant");
  }
  return response.json();
}

async function createTenant(data: Partial<Tenant>): Promise<Tenant> {
  const response = await fetch("/api/proxy/v1/tenants", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });
  if (!response.ok) {
    throw new Error("Failed to create tenant");
  }
  return response.json();
}

async function updateTenant(tenantId: string, data: Partial<Tenant>): Promise<Tenant> {
  const response = await fetch(`/api/proxy/v1/tenants/${tenantId}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });
  if (!response.ok) {
    throw new Error("Failed to update tenant");
  }
  return response.json();
}

async function suspendTenant(tenantId: string): Promise<void> {
  const response = await fetch(`/api/proxy/v1/tenants/${tenantId}/suspend`, {
    method: "POST",
  });
  if (!response.ok) {
    throw new Error("Failed to suspend tenant");
  }
}

async function deleteTenant(tenantId: string): Promise<void> {
  const response = await fetch(`/api/proxy/v1/tenants/${tenantId}`, {
    method: "DELETE",
  });
  if (!response.ok) {
    throw new Error("Failed to delete tenant");
  }
}

export function useTenants(page: number = 0, size: number = 10, filters?: TenantFilters) {
  return useQuery({
    queryKey: ["tenants", page, size, filters],
    queryFn: () => fetchTenants(page, size, filters),
  });
}

export function useTenant(tenantId: string) {
  return useQuery({
    queryKey: ["tenant", tenantId],
    queryFn: () => fetchTenant(tenantId),
    enabled: !!tenantId,
  });
}

export function useCreateTenant() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: createTenant,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["tenants"] });
    },
  });
}

export function useUpdateTenant() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ tenantId, data }: { tenantId: string; data: Partial<Tenant> }) =>
      updateTenant(tenantId, data),
    onSuccess: (_, { tenantId }) => {
      queryClient.invalidateQueries({ queryKey: ["tenants"] });
      queryClient.invalidateQueries({ queryKey: ["tenant", tenantId] });
    },
  });
}

export function useSuspendTenant() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: suspendTenant,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["tenants"] });
    },
  });
}

export function useDeleteTenant() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: deleteTenant,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["tenants"] });
    },
  });
}
