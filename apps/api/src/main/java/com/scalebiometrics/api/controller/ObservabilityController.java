package com.scalebiometrics.api.controller;

import com.scalebiometrics.api.entity.MatchingPerformance;
import com.scalebiometrics.api.entity.SystemEvent;
import com.scalebiometrics.api.entity.WorkerMetric;
import com.scalebiometrics.api.service.ObservabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/observability")
@RequiredArgsConstructor
public class ObservabilityController {

    private final ObservabilityService observabilityService;

    @GetMapping("/metrics/workers")
    public ResponseEntity<List<WorkerMetric>> getWorkerMetrics(
            @RequestParam(defaultValue = "5") int minutes) {
        return ResponseEntity.ok(observabilityService.getRecentWorkerMetrics(minutes));
    }

    @GetMapping("/metrics/performance")
    public ResponseEntity<List<MatchingPerformance>> getPerformanceMetrics(
            @RequestParam String tenantId,
            @RequestParam(defaultValue = "60") int minutes) {
        return ResponseEntity.ok(observabilityService.getRecentPerformance(tenantId, minutes));
    }

    @GetMapping("/events")
    public ResponseEntity<Page<SystemEvent>> getSystemEvents(
            @RequestParam(required = false) String tenantId,
            Pageable pageable) {
        return ResponseEntity.ok(observabilityService.getSystemEvents(tenantId, pageable));
    }
}
