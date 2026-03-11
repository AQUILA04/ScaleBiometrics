import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import type { BiometricRecord, RecordDetail, RecordHistory, PaginatedRecords } from "@/types/record";

async function fetchRecords(
  tenantId: string,
  page: number = 0,
  size: number = 10,
  search?: string
): Promise<PaginatedRecords> {
  const params = new URLSearchParams({
    page: page.toString(),
    size: size.toString(),
  });
  
  if (search) params.append("search", search);

  const response = await fetch(`/api/proxy/v1/tenants/${tenantId}/records?${params}`);
  if (!response.ok) {
    throw new Error("Failed to fetch records");
  }
  return response.json();
}

async function fetchRecordDetail(tenantId: string, rid: string): Promise<RecordDetail> {
  const response = await fetch(`/api/proxy/v1/tenants/${tenantId}/records/${rid}`);
  if (!response.ok) {
    throw new Error("Failed to fetch record details");
  }
  return response.json();
}

async function fetchRecordHistory(tenantId: string, rid: string): Promise<RecordHistory> {
  const response = await fetch(`/api/proxy/v1/tenants/${tenantId}/records/${rid}/history`);
  if (!response.ok) {
    throw new Error("Failed to fetch record history");
  }
  return response.json();
}

async function deleteRecord(tenantId: string, rid: string, confirmedRid: string, reason: string): Promise<void> {
  const response = await fetch(`/api/proxy/v1/tenants/${tenantId}/records/${rid}`, {
    method: "DELETE",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ confirmedRid, reason }),
  });
  if (!response.ok) {
    throw new Error("Failed to delete record");
  }
}

export function useRecords(tenantId: string, page: number = 0, size: number = 10, search?: string) {
  return useQuery({
    queryKey: ["records", tenantId, page, size, search],
    queryFn: () => fetchRecords(tenantId, page, size, search),
  });
}

export function useRecordDetail(tenantId: string, rid: string) {
  return useQuery({
    queryKey: ["record", tenantId, rid],
    queryFn: () => fetchRecordDetail(tenantId, rid),
    enabled: !!rid,
  });
}

export function useRecordHistory(tenantId: string, rid: string) {
  return useQuery({
    queryKey: ["record-history", tenantId, rid],
    queryFn: () => fetchRecordHistory(tenantId, rid),
    enabled: !!rid,
  });
}

export function useDeleteRecord() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ tenantId, rid, confirmedRid, reason }: { 
      tenantId: string; 
      rid: string; 
      confirmedRid: string; 
      reason: string 
    }) => deleteRecord(tenantId, rid, confirmedRid, reason),
    onSuccess: (_, { tenantId }) => {
      queryClient.invalidateQueries({ queryKey: ["records", tenantId] });
    },
  });
}
