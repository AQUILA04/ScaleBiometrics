package com.scalebiometrics.master.ha;

import com.scalebiometrics.master.grpc.WorkerPool;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

/**
 * Health Monitoring Service - Monitors health of all components.
 * 
 * Responsibilities:
 * - Monitor worker health
 * - Monitor master health
 * - Detect failures and trigger recovery
 * - Collect health metrics
 */
@Slf4j
@Service
public class HealthMonitoringService {

    private static final long HEALTH_CHECK_INTERVAL_MS = 10000;  // 10 seconds
    private static final long WORKER_HEARTBEAT_TIMEOUT_MS = 30000;  // 30 seconds
    private static final int CONSECUTIVE_FAILURES_THRESHOLD = 3;

    @Value("${master.health-monitoring.enabled:true}")
    private boolean healthMonitoringEnabled;

    private final WorkerPool workerPool;
    private final MeterRegistry meterRegistry;
    private final ExecutorService executorService;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    private final Map<String, WorkerHealthStatus> workerHealthStatus = new ConcurrentHashMap<>();

    public HealthMonitoringService(WorkerPool workerPool, MeterRegistry meterRegistry) {
        this.workerPool = workerPool;
        this.meterRegistry = meterRegistry;
        this.executorService = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "health-monitoring");
            t.setDaemon(false);
            return t;
        });
    }

    /**
     * Start health monitoring
     */
    public void start() {
        if (!healthMonitoringEnabled) {
            log.info("Health monitoring is disabled");
            return;
        }

        if (isRunning.getAndSet(true)) {
            log.warn("Health monitoring already running");
            return;
        }

        log.info("Starting health monitoring service");
        executorService.submit(this::monitoringLoop);
    }

    /**
     * Stop health monitoring
     */
    public void stop() {
        if (!isRunning.getAndSet(false)) {
            return;
        }

        log.info("Stopping health monitoring service");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Main monitoring loop
     */
    private void monitoringLoop() {
        while (isRunning.get()) {
            try {
                monitorWorkerHealth();
                recordMetrics();
                Thread.sleep(HEALTH_CHECK_INTERVAL_MS);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error in monitoring loop", e);
            }
        }
    }

    /**
     * Monitor health of all workers
     */
    private void monitorWorkerHealth() {
        List<WorkerPool.WorkerMetadata> allWorkers = new ArrayList<>(workerPool.getAllWorkerMetadata());

        for (WorkerPool.WorkerMetadata worker : allWorkers) {
            String workerId = worker.getWorkerId();
            WorkerHealthStatus status = workerHealthStatus.computeIfAbsent(
                    workerId,
                    k -> new WorkerHealthStatus(workerId)
            );

            try {
                // Check if worker is healthy
                boolean isHealthy = workerPool.getWorker(workerId).healthCheck();

                if (isHealthy) {
                    status.recordSuccess();
                    log.debug("Worker {} is healthy", workerId);
                } else {
                    status.recordFailure();
                    log.warn("Worker {} health check failed", workerId);
                }

            } catch (Exception e) {
                status.recordFailure();
                log.warn("Worker {} health check error: {}", workerId, e.getMessage());
            }

            // Check if worker should be marked as unhealthy
            if (status.getConsecutiveFailures() >= CONSECUTIVE_FAILURES_THRESHOLD) {
                if (status.isHealthy()) {
                    markWorkerUnhealthy(workerId, status);
                }
            }
        }
    }

    /**
     * Mark worker as unhealthy and trigger recovery
     */
    private void markWorkerUnhealthy(String workerId, WorkerHealthStatus status) {
        status.setHealthy(false);
        log.error("🚨 Worker {} marked as UNHEALTHY after {} consecutive failures", 
                workerId, status.getConsecutiveFailures());

        // Trigger recovery
        try {
            workerPool.unregisterWorker(workerId);
            log.info("Unregistered unhealthy worker {}", workerId);
        } catch (Exception e) {
            log.error("Error unregistering worker {}", workerId, e);
        }
    }

    /**
     * Record health metrics
     */
    private void recordMetrics() {
        int healthyWorkers = 0;
        int unhealthyWorkers = 0;

        for (WorkerHealthStatus status : workerHealthStatus.values()) {
            if (status.isHealthy()) {
                healthyWorkers++;
            } else {
                unhealthyWorkers++;
            }
        }

        meterRegistry.gauge("workers.healthy", healthyWorkers);
        meterRegistry.gauge("workers.unhealthy", unhealthyWorkers);
        meterRegistry.gauge("workers.total", workerHealthStatus.size());
    }

    /**
     * Get health status of a worker
     */
    public WorkerHealthStatus getWorkerHealthStatus(String workerId) {
        return workerHealthStatus.get(workerId);
    }

    /**
     * Get all worker health statuses
     */
    public Collection<WorkerHealthStatus> getAllWorkerHealthStatuses() {
        return workerHealthStatus.values();
    }

    /**
     * Get overall system health
     */
    public SystemHealthStatus getSystemHealth() {
        int totalWorkers = workerHealthStatus.size();
        int healthyWorkers = (int) workerHealthStatus.values().stream()
                .filter(WorkerHealthStatus::isHealthy)
                .count();

        String status = healthyWorkers > 0 ? "HEALTHY" : "UNHEALTHY";
        double healthPercentage = totalWorkers > 0 ? (double) healthyWorkers / totalWorkers * 100 : 0;

        return SystemHealthStatus.builder()
                .status(status)
                .totalWorkers(totalWorkers)
                .healthyWorkers(healthyWorkers)
                .unhealthyWorkers(totalWorkers - healthyWorkers)
                .healthPercentage(healthPercentage)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * Worker Health Status
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    public static class WorkerHealthStatus {
        private String workerId;
        private boolean healthy = true;
        private int consecutiveFailures = 0;
        private int consecutiveSuccesses = 0;
        private long lastCheckTime;
        private long lastFailureTime;

        public WorkerHealthStatus(String workerId) {
            this.workerId = workerId;
            this.lastCheckTime = System.currentTimeMillis();
        }

        public void recordSuccess() {
            this.consecutiveFailures = 0;
            this.consecutiveSuccesses++;
            this.lastCheckTime = System.currentTimeMillis();
        }

        public void recordFailure() {
            this.consecutiveSuccesses = 0;
            this.consecutiveFailures++;
            this.lastCheckTime = System.currentTimeMillis();
            this.lastFailureTime = System.currentTimeMillis();
        }
    }

    /**
     * System Health Status
     */
    @lombok.Data
    @lombok.Builder
    public static class SystemHealthStatus {
        private String status;
        private int totalWorkers;
        private int healthyWorkers;
        private int unhealthyWorkers;
        private double healthPercentage;
        private long timestamp;
    }
}
