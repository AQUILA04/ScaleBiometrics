package com.scalebiometrics.master.controller;

import com.scalebiometrics.master.ha.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;

/**
 * HA Controller - REST API for high availability operations
 */
@Slf4j
@RestController
@RequestMapping("/api/ha")
public class HAController {

    private final LeaderElectionService leaderElectionService;
    private final HealthMonitoringService healthMonitoringService;
    private final CircuitBreakerService circuitBreakerService;
    private final FailoverService failoverService;

    public HAController(
            LeaderElectionService leaderElectionService,
            HealthMonitoringService healthMonitoringService,
            CircuitBreakerService circuitBreakerService,
            FailoverService failoverService) {
        this.leaderElectionService = leaderElectionService;
        this.healthMonitoringService = healthMonitoringService;
        this.circuitBreakerService = circuitBreakerService;
        this.failoverService = failoverService;
    }

    /**
     * Get leadership status
     * GET /api/ha/leadership
     */
    @GetMapping("/leadership")
    public ResponseEntity<?> getLeadershipStatus() {
        LeadershipStatus status = LeadershipStatus.builder()
                .isLeader(leaderElectionService.isLeader())
                .instanceId(leaderElectionService.getInstanceId())
                .currentLeader(leaderElectionService.getCurrentLeader())
                .leadershipDurationMs(leaderElectionService.getLeadershipDurationMs())
                .timestamp(System.currentTimeMillis())
                .build();

        return ResponseEntity.ok(status);
    }

    /**
     * Get system health
     * GET /api/ha/health
     */
    @GetMapping("/health")
    public ResponseEntity<?> getSystemHealth() {
        HealthMonitoringService.SystemHealthStatus health = 
                healthMonitoringService.getSystemHealth();
        return ResponseEntity.ok(health);
    }

    /**
     * Get worker health statuses
     * GET /api/ha/workers/health
     */
    @GetMapping("/workers/health")
    public ResponseEntity<?> getWorkersHealth() {
        Collection<HealthMonitoringService.WorkerHealthStatus> statuses = 
                healthMonitoringService.getAllWorkerHealthStatuses();
        return ResponseEntity.ok(statuses);
    }

    /**
     * Get worker health status
     * GET /api/ha/workers/{workerId}/health
     */
    @GetMapping("/workers/{workerId}/health")
    public ResponseEntity<?> getWorkerHealth(@PathVariable String workerId) {
        HealthMonitoringService.WorkerHealthStatus status = 
                healthMonitoringService.getWorkerHealthStatus(workerId);

        if (status == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(status);
    }

    /**
     * Get circuit breaker states
     * GET /api/ha/circuit-breakers
     */
    @GetMapping("/circuit-breakers")
    public ResponseEntity<?> getCircuitBreakerStates() {
        Map<String, CircuitBreakerService.CircuitBreakerState> states = 
                circuitBreakerService.getAllStates();
        return ResponseEntity.ok(states);
    }

    /**
     * Get circuit breaker state
     * GET /api/ha/circuit-breakers/{serviceId}
     */
    @GetMapping("/circuit-breakers/{serviceId}")
    public ResponseEntity<?> getCircuitBreakerState(@PathVariable String serviceId) {
        CircuitBreakerService.CircuitState state = circuitBreakerService.getState(serviceId);
        return ResponseEntity.ok(new CircuitBreakerStateResponse(serviceId, state));
    }

    /**
     * Reset circuit breaker
     * POST /api/ha/circuit-breakers/{serviceId}/reset
     */
    @PostMapping("/circuit-breakers/{serviceId}/reset")
    public ResponseEntity<?> resetCircuitBreaker(@PathVariable String serviceId) {
        circuitBreakerService.reset(serviceId);
        log.info("Reset circuit breaker for service: {}", serviceId);
        return ResponseEntity.ok(new MessageResponse("Circuit breaker reset for: " + serviceId));
    }

    /**
     * Reset all circuit breakers
     * POST /api/ha/circuit-breakers/reset-all
     */
    @PostMapping("/circuit-breakers/reset-all")
    public ResponseEntity<?> resetAllCircuitBreakers() {
        circuitBreakerService.resetAll();
        log.info("Reset all circuit breakers");
        return ResponseEntity.ok(new MessageResponse("All circuit breakers reset"));
    }

    /**
     * Get failover status
     * GET /api/ha/failover/status
     */
    @GetMapping("/failover/status")
    public ResponseEntity<?> getFailoverStatus() {
        FailoverService.FailoverStatus status = failoverService.getFailoverStatus();
        return ResponseEntity.ok(status);
    }

    /**
     * Trigger manual failover
     * POST /api/ha/failover/trigger
     */
    @PostMapping("/failover/trigger")
    public ResponseEntity<?> triggerFailover(@RequestBody FailoverRequest request) {
        log.warn("Manual failover triggered for: {}", request.getComponentId());
        
        if ("worker".equalsIgnoreCase(request.getComponentType())) {
            failoverService.handleWorkerFailure(request.getComponentId());
        } else if ("master".equalsIgnoreCase(request.getComponentType())) {
            failoverService.handleMasterFailure(request.getComponentId());
        }

        return ResponseEntity.ok(new MessageResponse("Failover triggered for: " + request.getComponentId()));
    }

    /**
     * Request DTOs
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class FailoverRequest {
        private String componentType;  // "worker" or "master"
        private String componentId;
    }

    @lombok.Data
    @lombok.Builder
    public static class LeadershipStatus {
        private boolean isLeader;
        private String instanceId;
        private String currentLeader;
        private long leadershipDurationMs;
        private long timestamp;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CircuitBreakerStateResponse {
        private String serviceId;
        private CircuitBreakerService.CircuitState state;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class MessageResponse {
        private String message;
    }
}
