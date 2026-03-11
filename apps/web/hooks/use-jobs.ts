import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import type { Job, JobDetail, JobFilters, PaginatedJobs } from "@/types/job";

async function fetchJobs(
  tenantId: string,
  page: number = 0,
  size: number = 10,
  filters?: JobFilters
): Promise<PaginatedJobs> {
  const params = new URLSearchParams({
    page: page.toString(),
    size: size.toString(),
  });
  
  if (filters?.status) params.append("status", filters.status);
  if (filters?.type) params.append("type", filters.type);
  if (filters?.priority) params.append("priority", filters.priority);
  if (filters?.search) params.append("search", filters.search);

  const response = await fetch(`/api/proxy/v1/tenants/${tenantId}/jobs?${params}`);
  if (!response.ok) {
    throw new Error("Failed to fetch jobs");
  }
  return response.json();
}

async function fetchJobDetail(tenantId: string, jobId: string): Promise<JobDetail> {
  const response = await fetch(`/api/proxy/v1/tenants/${tenantId}/jobs/${jobId}`);
  if (!response.ok) {
    throw new Error("Failed to fetch job details");
  }
  return response.json();
}

async function escalateJobPriority(tenantId: string, jobId: string): Promise<void> {
  const response = await fetch(`/api/proxy/v1/tenants/${tenantId}/jobs/${jobId}/escalate`, {
    method: "POST",
  });
  if (!response.ok) {
    throw new Error("Failed to escalate job priority");
  }
}

async function cancelJob(tenantId: string, jobId: string): Promise<void> {
  const response = await fetch(`/api/proxy/v1/tenants/${tenantId}/jobs/${jobId}/cancel`, {
    method: "POST",
  });
  if (!response.ok) {
    throw new Error("Failed to cancel job");
  }
}

export function useJobs(tenantId: string, page: number = 0, size: number = 10, filters?: JobFilters) {
  return useQuery({
    queryKey: ["jobs", tenantId, page, size, filters],
    queryFn: () => fetchJobs(tenantId, page, size, filters),
    refetchInterval: 10000,
  });
}

export function useJobDetail(tenantId: string, jobId: string) {
  return useQuery({
    queryKey: ["job", tenantId, jobId],
    queryFn: () => fetchJobDetail(tenantId, jobId),
    enabled: !!jobId,
  });
}

export function useEscalateJob() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ tenantId, jobId }: { tenantId: string; jobId: string }) =>
      escalateJobPriority(tenantId, jobId),
    onSuccess: (_, { tenantId }) => {
      queryClient.invalidateQueries({ queryKey: ["jobs", tenantId] });
    },
  });
}

export function useCancelJob() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ tenantId, jobId }: { tenantId: string; jobId: string }) =>
      cancelJob(tenantId, jobId),
    onSuccess: (_, { tenantId }) => {
      queryClient.invalidateQueries({ queryKey: ["jobs", tenantId] });
    },
  });
}
