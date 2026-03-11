import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import type { PostgresMetrics, RedisMetrics, KafkaMetrics, MinIOMetrics, ClusterHealth, InfrastructureAlert } from "@/types/infrastructure";

async function fetchPostgresMetrics(): Promise<PostgresMetrics> {
  const response = await fetch("/api/proxy/superadmin/infrastructure/postgres/metrics");
  if (!response.ok) throw new Error("Failed to fetch Postgres metrics");
  return response.json();
}

async function fetchRedisMetrics(): Promise<RedisMetrics> {
  const response = await fetch("/api/proxy/superadmin/infrastructure/redis/metrics");
  if (!response.ok) throw new Error("Failed to fetch Redis metrics");
  return response.json();
}

async function fetchKafkaMetrics(): Promise<KafkaMetrics> {
  const response = await fetch("/api/proxy/superadmin/infrastructure/kafka/metrics");
  if (!response.ok) throw new Error("Failed to fetch Kafka metrics");
  return response.json();
}

async function fetchMinIOMetrics(): Promise<MinIOMetrics> {
  const response = await fetch("/api/proxy/superadmin/infrastructure/minio/metrics");
  if (!response.ok) throw new Error("Failed to fetch MinIO metrics");
  return response.json();
}

async function fetchClusterHealth(): Promise<ClusterHealth> {
  const response = await fetch("/api/proxy/superadmin/infrastructure/cluster/health");
  if (!response.ok) throw new Error("Failed to fetch cluster health");
  return response.json();
}

async function fetchAlerts(): Promise<{ alerts: InfrastructureAlert[] }> {
  const response = await fetch("/api/proxy/superadmin/infrastructure/alerts");
  if (!response.ok) throw new Error("Failed to fetch alerts");
  return response.json();
}

export function usePostgresMetrics() {
  return useQuery({
    queryKey: ["infrastructure", "postgres"],
    queryFn: fetchPostgresMetrics,
    refetchInterval: 30000,
  });
}

export function useRedisMetrics() {
  return useQuery({
    queryKey: ["infrastructure", "redis"],
    queryFn: fetchRedisMetrics,
    refetchInterval: 30000,
  });
}

export function useKafkaMetrics() {
  return useQuery({
    queryKey: ["infrastructure", "kafka"],
    queryFn: fetchKafkaMetrics,
    refetchInterval: 30000,
  });
}

export function useMinIOMetrics() {
  return useQuery({
    queryKey: ["infrastructure", "minio"],
    queryFn: fetchMinIOMetrics,
    refetchInterval: 30000,
  });
}

export function useClusterHealth() {
  return useQuery({
    queryKey: ["infrastructure", "cluster", "health"],
    queryFn: fetchClusterHealth,
    refetchInterval: 15000,
  });
}

export function useAlerts() {
  return useQuery({
    queryKey: ["infrastructure", "alerts"],
    queryFn: fetchAlerts,
    refetchInterval: 30000,
  });
}
