import { useQuery } from "@tanstack/react-query";
import type { AuditLogsResponse, AuditLogFilters } from "@/types/audit-log";

async function fetchAuditLogs(
  page: number = 0,
  size: number = 20,
  filters?: AuditLogFilters
): Promise<AuditLogsResponse> {
  const params = new URLSearchParams({
    page: page.toString(),
    size: size.toString(),
  });
  
  if (filters?.actor) params.append("actor", filters.actor);
  if (filters?.action) params.append("action", filters.action);
  if (filters?.tenantId) params.append("tenantId", filters.tenantId);
  if (filters?.fromDate) params.append("fromDate", filters.fromDate);
  if (filters?.toDate) params.append("toDate", filters.toDate);

  const response = await fetch(`/api/proxy/superadmin/audit-logs?${params}`);
  if (!response.ok) throw new Error("Failed to fetch audit logs");
  return response.json();
}

async function fetchAuditLog(logId: string) {
  const response = await fetch(`/api/proxy/superadmin/audit-logs/${logId}`);
  if (!response.ok) throw new Error("Failed to fetch audit log");
  return response.json();
}

export function useAuditLogs(page: number = 0, size: number = 20, filters?: AuditLogFilters) {
  return useQuery({
    queryKey: ["audit-logs", page, size, filters],
    queryFn: () => fetchAuditLogs(page, size, filters),
    refetchInterval: 30000,
  });
}

export function useAuditLog(logId: string) {
  return useQuery({
    queryKey: ["audit-log", logId],
    queryFn: () => fetchAuditLog(logId),
    enabled: !!logId,
  });
}

export function useExportAuditLogs(filters?: AuditLogFilters) {
  const params = new URLSearchParams();
  
  if (filters?.actor) params.append("actor", filters.actor);
  if (filters?.action) params.append("action", filters.action);
  if (filters?.tenantId) params.append("tenantId", filters.tenantId);
  if (filters?.fromDate) params.append("fromDate", filters.fromDate);
  if (filters?.toDate) params.append("toDate", filters.toDate);

  return `/api/proxy/superadmin/audit-logs/export?${params}`;
}
