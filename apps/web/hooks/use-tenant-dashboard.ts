import { useQuery } from "@tanstack/react-query";
import type { TenantDashboardKPIs, ThroughputPoint, RecentJob } from "@/types/tenant-dashboard";

async function fetchTenantDashboardKPIs(tenantId: string): Promise<TenantDashboardKPIs> {
  const response = await fetch(`/api/proxy/v1/tenants/${tenantId}/dashboard/kpis`);
  if (!response.ok) {
    throw new Error("Failed to fetch dashboard KPIs");
  }
  return response.json();
}

async function fetchThroughputData(tenantId: string, hours: number = 24): Promise<ThroughputPoint[]> {
  const response = await fetch(`/api/proxy/v1/tenants/${tenantId}/dashboard/throughput?hours=${hours}`);
  if (!response.ok) {
    throw new Error("Failed to fetch throughput data");
  }
  return response.json();
}

async function fetchRecentJobs(tenantId: string): Promise<RecentJob[]> {
  const response = await fetch(`/api/proxy/v1/tenants/${tenantId}/jobs/recent`);
  if (!response.ok) {
    throw new Error("Failed to fetch recent jobs");
  }
  return response.json();
}

export function useTenantDashboardKPIs(tenantId: string) {
  return useQuery({
    queryKey: ["tenant-dashboard", "kpis", tenantId],
    queryFn: () => fetchTenantDashboardKPIs(tenantId),
    refetchInterval: 30000,
  });
}

export function useTenantThroughput(tenantId: string, hours: number = 24) {
  return useQuery({
    queryKey: ["tenant-dashboard", "throughput", tenantId, hours],
    queryFn: () => fetchThroughputData(tenantId, hours),
    refetchInterval: 30000,
  });
}

export function useTenantRecentJobs(tenantId: string) {
  return useQuery({
    queryKey: ["tenant-dashboard", "recent-jobs", tenantId],
    queryFn: () => fetchRecentJobs(tenantId),
    refetchInterval: 15000,
  });
}
