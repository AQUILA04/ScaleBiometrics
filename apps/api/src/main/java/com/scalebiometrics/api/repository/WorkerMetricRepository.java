package com.scalebiometrics.api.repository;

import com.scalebiometrics.api.entity.WorkerMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface WorkerMetricRepository extends JpaRepository<WorkerMetric, UUID> {
    List<WorkerMetric> findByRecordedAtAfter(LocalDateTime timestamp);
    
    @Query("SELECT w FROM WorkerMetric w WHERE w.workerId = :workerId ORDER BY w.recordedAt DESC LIMIT 1")
    WorkerMetric findLatestByWorkerId(String workerId);
}
