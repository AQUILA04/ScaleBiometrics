package com.scalebiometrics.api.service;

import com.scalebiometrics.api.entity.MatchingPerformance;
import com.scalebiometrics.api.entity.SystemEvent;
import com.scalebiometrics.api.entity.WorkerMetric;
import com.scalebiometrics.api.repository.MatchingPerformanceRepository;
import com.scalebiometrics.api.repository.SystemEventRepository;
import com.scalebiometrics.api.repository.WorkerMetricRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ObservabilityService {

    private final WorkerMetricRepository workerMetricRepository;
    private final MatchingPerformanceRepository matchingPerformanceRepository;
    private final SystemEventRepository systemEventRepository;

    public List<WorkerMetric> getRecentWorkerMetrics(int minutes) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(minutes);
        return workerMetricRepository.findByRecordedAtAfter(since);
    }

    public List<MatchingPerformance> getRecentPerformance(String tenantId, int minutes) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(minutes);
        return matchingPerformanceRepository.findByTenantIdAndRecordedAtAfter(tenantId, since);
    }

    public Page<SystemEvent> getSystemEvents(String tenantId, Pageable pageable) {
        if (tenantId != null) {
            return systemEventRepository.findByTenantId(tenantId, pageable);
        }
        return systemEventRepository.findAll(pageable);
    }
}
