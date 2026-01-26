package com.scalebiometrics.master.ha;

import com.scalebiometrics.master.grpc.WorkerPool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

/**
 * Failover Service - Handles failover and recovery of failed components.
 * 
 * Responsibilities:
 * - Detect component failures
 * - Trigger failover procedures
 * - Coordinate recovery
 * - Maintain system availability
 */
@Slf4j
@Service
public class FailoverService {

    private final WorkerPool workerPool;
    private final HealthMonitoringService healthMonitoringService;
    private final CircuitBreakerService circuitBreakerService;
    private final ExecutorService executorService;

    private final List<FailoverListener> listeners = new CopyOnWriteArrayList<>();

    public FailoverService(
            WorkerPool workerPool,
            HealthMonitoringService healthMonitoringService,
            CircuitBreakerService circuitBreakerService) {
        this.workerPool = workerPool;
        this.healthMonitoringService = healthMonitoringService;
        this.circuitBreakerService = circuitBreakerService;
        this.executorService = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "failover-" + Thread.currentThread().getId());
            t.setDaemon(false);
            return t;
        });
    }

    /**
     * Handle worker failure
     */
    public void handleWorkerFailure(String workerId) {
        log.error("🚨 Handling worker failure: {}", workerId);

        executorService.submit(() -> {
            try {
                // Step 1: Mark circuit breaker as open
                circuitBreakerService.recordFailure(workerId);

                // Step 2: Unregister worker
                workerPool.unregisterWorker(workerId);
                log.info("Unregistered failed worker: {}", workerId);

                // Step 3: Notify listeners
                notifyWorkerFailure(workerId);

                // Step 4: Attempt recovery
                attemptWorkerRecovery(workerId);

            } catch (Exception e) {
                log.error("Error handling worker failure", e);
            }
        });
    }

    /**
     * Attempt to recover a failed worker
     */
    private void attemptWorkerRecovery(String workerId) {
        log.info("Attempting recovery for worker: {}", workerId);

        // Get worker metadata
        WorkerPool.WorkerMetadata metadata = workerPool.getWorkerMetadata(workerId);
        if (metadata == null) {
            log.warn("Worker metadata not found: {}", workerId);
            return;
        }

        // Retry with exponential backoff
        int maxRetries = 3;
        long initialDelayMs = 5000;  // 5 seconds

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                long delayMs = initialDelayMs * (long) Math.pow(2, attempt - 1);
                log.info("Recovery attempt {}/{} for worker {} (delay: {}ms)", 
                        attempt, maxRetries, workerId, delayMs);

                Thread.sleep(delayMs);

                // Try to re-register worker
                workerPool.registerWorker(workerId, metadata.getHost(), metadata.getPort());
                log.info("✅ Worker {} recovered successfully", workerId);
                notifyWorkerRecovered(workerId);
                return;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.warn("Recovery attempt {}/{} failed for worker {}: {}", 
                        attempt, maxRetries, workerId, e.getMessage());
            }
        }

        log.error("❌ Failed to recover worker {} after {} attempts", workerId, maxRetries);
        notifyWorkerRecoveryFailed(workerId);
    }

    /**
     * Handle master failure (for distributed master setup)
     */
    public void handleMasterFailure(String masterId) {
        log.error("🚨 Master {} failed, triggering failover", masterId);
        notifyMasterFailure(masterId);
    }

    /**
     * Get failover status
     */
    public FailoverStatus getFailoverStatus() {
        HealthMonitoringService.SystemHealthStatus systemHealth = 
                healthMonitoringService.getSystemHealth();

        return FailoverStatus.builder()
                .systemHealth(systemHealth)
                .circuitBreakerStates(circuitBreakerService.getAllStates())
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * Register failover listener
     */
    public void addFailoverListener(FailoverListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove failover listener
     */
    public void removeFailoverListener(FailoverListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notify listeners of worker failure
     */
    private void notifyWorkerFailure(String workerId) {
        for (FailoverListener listener : listeners) {
            try {
                listener.onWorkerFailure(workerId);
            } catch (Exception e) {
                log.error("Error notifying listener of worker failure", e);
            }
        }
    }

    /**
     * Notify listeners of worker recovery
     */
    private void notifyWorkerRecovered(String workerId) {
        for (FailoverListener listener : listeners) {
            try {
                listener.onWorkerRecovered(workerId);
            } catch (Exception e) {
                log.error("Error notifying listener of worker recovery", e);
            }
        }
    }

    /**
     * Notify listeners of worker recovery failure
     */
    private void notifyWorkerRecoveryFailed(String workerId) {
        for (FailoverListener listener : listeners) {
            try {
                listener.onWorkerRecoveryFailed(workerId);
            } catch (Exception e) {
                log.error("Error notifying listener of worker recovery failure", e);
            }
        }
    }

    /**
     * Notify listeners of master failure
     */
    private void notifyMasterFailure(String masterId) {
        for (FailoverListener listener : listeners) {
            try {
                listener.onMasterFailure(masterId);
            } catch (Exception e) {
                log.error("Error notifying listener of master failure", e);
            }
        }
    }

    /**
     * Shutdown failover service
     */
    public void shutdown() {
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
     * Failover Listener interface
     */
    public interface FailoverListener {
        void onWorkerFailure(String workerId);
        void onWorkerRecovered(String workerId);
        void onWorkerRecoveryFailed(String workerId);
        void onMasterFailure(String masterId);
    }

    /**
     * Failover Status
     */
    @lombok.Data
    @lombok.Builder
    public static class FailoverStatus {
        private HealthMonitoringService.SystemHealthStatus systemHealth;
        private Map<String, CircuitBreakerService.CircuitBreakerState> circuitBreakerStates;
        private long timestamp;
    }
}
