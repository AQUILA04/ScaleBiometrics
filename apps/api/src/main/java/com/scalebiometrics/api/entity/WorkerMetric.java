package com.scalebiometrics.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "worker_metrics", schema = "public")
public class WorkerMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "worker_id", nullable = false)
    private String workerId;

    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "total_matches")
    private Long totalMatches;

    @Column(name = "total_errors")
    private Long totalErrors;

    @Column(name = "avg_latency_ms")
    private Double avgLatencyMs;

    @Column(name = "p95_latency_ms")
    private Double p95LatencyMs;

    @Column(name = "p99_latency_ms")
    private Double p99LatencyMs;

    @Column(name = "heap_memory_bytes")
    private Long heapMemoryBytes;

    @Column(name = "offheap_memory_bytes")
    private Long offheapMemoryBytes;

    @Column(name = "hnsw_index_size_bytes")
    private Long hnswIndexSizeBytes;

    @Column(name = "cpu_usage_percent")
    private Integer cpuUsagePercent;

    @Column(name = "grpc_queue_depth")
    private Integer grpcQueueDepth;

    @Column(name = "kafka_lag")
    private Long kafkaLag;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;
}
