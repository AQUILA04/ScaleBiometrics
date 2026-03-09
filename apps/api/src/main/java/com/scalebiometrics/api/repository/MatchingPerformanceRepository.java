package com.scalebiometrics.api.repository;

import com.scalebiometrics.api.entity.MatchingPerformance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface MatchingPerformanceRepository extends JpaRepository<MatchingPerformance, UUID> {
    List<MatchingPerformance> findByTenantIdAndRecordedAtAfter(String tenantId, LocalDateTime timestamp);
}
