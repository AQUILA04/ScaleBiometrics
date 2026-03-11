import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import type { WorkerNode, WorkersResponse } from "@/types/worker";

async function fetchWorkers(): Promise<WorkersResponse> {
  const response = await fetch("/api/proxy/superadmin/workers");
  if (!response.ok) throw new Error("Failed to fetch workers");
  return response.json();
}

async function fetchWorker(workerId: string): Promise<WorkerNode> {
  const response = await fetch(`/api/proxy/superadmin/workers/${workerId}`);
  if (!response.ok) throw new Error("Failed to fetch worker");
  return response.json();
}

async function drainWorker(workerId: string): Promise<{ success: boolean; message: string }> {
  const response = await fetch(`/api/proxy/superadmin/workers/${workerId}/drain`, {
    method: "POST",
  });
  if (!response.ok) throw new Error("Failed to drain worker");
  return response.json();
}

async function restartWorker(workerId: string): Promise<{ success: boolean; message: string }> {
  const response = await fetch(`/api/proxy/superadmin/workers/${workerId}/restart`, {
    method: "POST",
  });
  if (!response.ok) throw new Error("Failed to restart worker");
  return response.json();
}

export function useWorkers() {
  return useQuery({
    queryKey: ["workers"],
    queryFn: fetchWorkers,
    refetchInterval: 10000,
  });
}

export function useWorker(workerId: string) {
  return useQuery({
    queryKey: ["worker", workerId],
    queryFn: () => fetchWorker(workerId),
    enabled: !!workerId,
  });
}

export function useDrainWorker() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: drainWorker,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["workers"] });
    },
  });
}

export function useRestartWorker() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: restartWorker,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["workers"] });
    },
  });
}
