import { useQuery } from "@tanstack/react-query";
import type { DashboardData, DashboardKPIs, ServiceHealth, ThroughputData, LatencyData } from "@/types/dashboard";

async function fetchDashboardData(): Promise<DashboardData> {
  const response = await fetch("/api/proxy/superadmin/dashboard");
  if (!response.ok) {
    throw new Error("Failed to fetch dashboard data");
  }
  return response.json();
}

async function fetchKPIs(): Promise<DashboardKPIs> {
  const response = await fetch("/api/proxy/superadmin/dashboard/kpis");
  if (!response.ok) {
    throw new Error("Failed to fetch KPIs");
  }
  return response.json();
}

async function fetchServiceHealth(): Promise<ServiceHealth[]> {
  const response = await fetch("/api/proxy/superadmin/services/health");
  if (!response.ok) {
    throw new Error("Failed to fetch service health");
  }
  return response.json();
}

async function fetchThroughputData(hours: number = 24): Promise<ThroughputData[]> {
  const response = await fetch(`/api/proxy/superadmin/dashboard/throughput?hours=${hours}`);
  if (!response.ok) {
    throw new Error("Failed to fetch throughput data");
  }
  return response.json();
}

async function fetchLatencyData(hours: number = 24): Promise<LatencyData[]> {
  const response = await fetch(`/api/proxy/superadmin/dashboard/latency?hours=${hours}`);
  if (!response.ok) {
    throw new Error("Failed to fetch latency data");
  }
  return response.json();
}

export function useDashboardData() {
  return useQuery({
    queryKey: ["dashboard"],
    queryFn: fetchDashboardData,
    refetchInterval: 30000,
  });
}

export function useKPIs() {
  return useQuery({
    queryKey: ["dashboard", "kpis"],
    queryFn: fetchKPIs,
    refetchInterval: 30000,
  });
}

export function useServiceHealth() {
  return useQuery({
    queryKey: ["services", "health"],
    queryFn: fetchServiceHealth,
    refetchInterval: 15000,
  });
}

export function useThroughputData(hours: number = 24) {
  return useQuery({
    queryKey: ["dashboard", "throughput", hours],
    queryFn: () => fetchThroughputData(hours),
    refetchInterval: 30000,
  });
}

export function useLatencyData(hours: number = 24) {
  return useQuery({
    queryKey: ["dashboard", "latency", hours],
    queryFn: () => fetchLatencyData(hours),
    refetchInterval: 30000,
  });
}
