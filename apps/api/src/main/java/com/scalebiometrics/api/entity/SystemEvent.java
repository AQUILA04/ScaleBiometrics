package com.scalebiometrics.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "system_events", schema = "public")
public class SystemEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "tenant_id")
    private String tenantId;

    private String component;

    private String severity;

    private String message;

    @JdbcTypeCode(SqlTypes.JSON)
    private String details; // JSONB

    @Column(name = "trace_id")
    private String traceId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
