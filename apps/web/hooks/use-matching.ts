import { useQuery } from "@tanstack/react-query";
import type { MatchingResult, MatchingResultDetail, MatchingFilters, PaginatedMatchingResults } from "@/types/matching";

async function fetchMatchingResults(
  tenantId: string,
  page: number = 0,
  size: number = 10,
  filters?: MatchingFilters
): Promise<PaginatedMatchingResults> {
  const params = new URLSearchParams({
    page: page.toString(),
    size: size.toString(),
  });
  
  if (filters?.result) params.append("result", filters.result);
  if (filters?.fromDate) params.append("fromDate", filters.fromDate);
  if (filters?.toDate) params.append("toDate", filters.toDate);
  if (filters?.search) params.append("search", filters.search);

  const response = await fetch(`/api/proxy/v1/tenants/${tenantId}/matching/results?${params}`);
  if (!response.ok) {
    throw new Error("Failed to fetch matching results");
  }
  return response.json();
}

async function fetchMatchingResultDetail(tenantId: string, resultId: string): Promise<MatchingResultDetail> {
  const response = await fetch(`/api/proxy/v1/tenants/${tenantId}/matching/results/${resultId}`);
  if (!response.ok) {
    throw new Error("Failed to fetch matching result details");
  }
  return response.json();
}

export function useMatchingResults(tenantId: string, page: number = 0, size: number = 10, filters?: MatchingFilters) {
  return useQuery({
    queryKey: ["matching-results", tenantId, page, size, filters],
    queryFn: () => fetchMatchingResults(tenantId, page, size, filters),
  });
}

export function useMatchingResultDetail(tenantId: string, resultId: string) {
  return useQuery({
    queryKey: ["matching-result", tenantId, resultId],
    queryFn: () => fetchMatchingResultDetail(tenantId, resultId),
    enabled: !!resultId,
  });
}
