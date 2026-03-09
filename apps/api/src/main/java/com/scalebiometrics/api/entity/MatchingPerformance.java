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
@Table(name = "matching_performance", schema = "public")
public class MatchingPerformance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "probe_rid")
    private String probeRid;

    @Column(name = "target_rid")
    private String targetRid;

    @Column(name = "hnn_score")
    private Integer hnnScore;

    @Column(name = "exact_score")
    private Integer exactScore;

    @Column(name = "final_score")
    private Integer finalScore;

    @Column(name = "matching_time_ms")
    private Long matchingTimeMs;

    @Column(name = "worker_id")
    private String workerId;

    @Column(name = "trace_id")
    private String traceId;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;
}
