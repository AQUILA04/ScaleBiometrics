package com.scalebiometrics.master.routing;

import com.scalebiometrics.master.grpc.WorkerClient;
import com.scalebiometrics.master.grpc.WorkerPool;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Load Balancing Service - Tracks and optimizes load distribution across workers.
 * 
 * Responsibilities:
 * - Track request count per worker
 * - Track response time per worker
 * - Detect overloaded workers
 * - Suggest load rebalancing
 */
@Slf4j
@Service
public class LoadBalancingService {

    private final WorkerPool workerPool;
    private final MeterRegistry meterRegistry;
    private final Map<String, WorkerLoadMetrics> loadMetrics = new ConcurrentHashMap<>();

    private static final long OVERLOAD_THRESHOLD = 1000;  // Requests
    private static final long SLOW_RESPONSE_THRESHOLD_MS = 500;  // ms

    public LoadBalancingService(WorkerPool workerPool, MeterRegistry meterRegistry) {
        this.workerPool = workerPool;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Record request sent to worker
     */
    public void recordRequestSent(String workerId) {
        WorkerLoadMetrics metrics = loadMetrics.computeIfAbsent(
                workerId,
                k -> new WorkerLoadMetrics(workerId)
        );
        metrics.incrementRequestCount();
        meterRegistry.gauge("worker.requests.pending", () -> metrics.getPendingRequests());
    }

    /**
     * Record response received from worker
     */
    public void recordResponseReceived(String workerId, long responseTimeMs) {
        WorkerLoadMetrics metrics = loadMetrics.get(workerId);
        if (metrics != null) {
            metrics.recordResponse(responseTimeMs);
            meterRegistry.gauge("worker.response.time.ms", 
                    Collections.singletonMap("worker_id", workerId), 
                    () -> metrics.getAverageResponseTimeMs());
        }
    }

    /**
     * Get load metrics for a worker
     */
    public WorkerLoadMetrics getWorkerLoadMetrics(String workerId) {
        return loadMetrics.get(workerId);
    }

    /**
     * Get all load metrics
     */
    public Collection<WorkerLoadMetrics> getAllLoadMetrics() {
        return loadMetrics.values();
    }

    /**
     * Check if worker is overloaded
     */
    public boolean isWorkerOverloaded(String workerId) {
        WorkerLoadMetrics metrics = loadMetrics.get(workerId);
        if (metrics == null) {
            return false;
        }

        return metrics.getTotalRequests() > OVERLOAD_THRESHOLD ||
               metrics.getAverageResponseTimeMs() > SLOW_RESPONSE_THRESHOLD_MS;
    }

    /**
     * Get overloaded workers
     */
    public List<String> getOverloadedWorkers() {
        return loadMetrics.entrySet().stream()
                .filter(e -> isWorkerOverloaded(e.getKey()))
                .map(Map.Entry::getKey)
                .toList();
    }

    /**
     * Get least loaded worker
     */
    public String getLeastLoadedWorker() {
        return loadMetrics.entrySet().stream()
                .min(Comparator.comparingLong(e -> e.getValue().getTotalRequests()))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * Get most loaded worker
     */
    public String getMostLoadedWorker() {
        return loadMetrics.entrySet().stream()
                .max(Comparator.comparingLong(e -> e.getValue().getTotalRequests()))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * Get load distribution
     */
    public LoadDistribution getLoadDistribution() {
        long totalRequests = loadMetrics.values().stream()
                .mapToLong(WorkerLoadMetrics::getTotalRequests)
                .sum();

        double avgLoad = totalRequests > 0 ? 
                (double) totalRequests / loadMetrics.size() : 0;

        double maxLoad = loadMetrics.values().stream()
                .mapToLong(WorkerLoadMetrics::getTotalRequests)
                .max()
                .orElse(0);

        double minLoad = loadMetrics.values().stream()
                .mapToLong(WorkerLoadMetrics::getTotalRequests)
                .min()
                .orElse(0);

        // Calculate standard deviation
        double variance = loadMetrics.values().stream()
                .mapToDouble(m -> Math.pow(m.getTotalRequests() - avgLoad, 2))
                .average()
                .orElse(0);
        double stdDev = Math.sqrt(variance);

        return LoadDistribution.builder()
                .totalRequests(totalRequests)
                .averageLoad(avgLoad)
                .maxLoad(maxLoad)
                .minLoad(minLoad)
                .standardDeviation(stdDev)
                .isBalanced(stdDev < avgLoad * 0.2)  // Balanced if stdDev < 20% of avg
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * Reset metrics
     */
    public void resetMetrics() {
        loadMetrics.clear();
        log.info("Reset all load metrics");
    }

    /**
     * Worker Load Metrics
     */
    @lombok.Data
    public static class WorkerLoadMetrics {
        private String workerId;
        private AtomicLong totalRequests = new AtomicLong(0);
        private AtomicLong pendingRequests = new AtomicLong(0);
        private AtomicLong totalResponseTime = new AtomicLong(0);
        private AtomicLong responseCount = new AtomicLong(0);
        private long createdAt;

        public WorkerLoadMetrics(String workerId) {
            this.workerId = workerId;
            this.createdAt = System.currentTimeMillis();
        }

        public void incrementRequestCount() {
            totalRequests.incrementAndGet();
            pendingRequests.incrementAndGet();
        }

        public void recordResponse(long responseTimeMs) {
            pendingRequests.decrementAndGet();
            totalResponseTime.addAndGet(responseTimeMs);
            responseCount.incrementAndGet();
        }

        public long getTotalRequests() {
            return totalRequests.get();
        }

        public long getPendingRequests() {
            return pendingRequests.get();
        }

        public double getAverageResponseTimeMs() {
            long count = responseCount.get();
            if (count == 0) {
                return 0;
            }
            return (double) totalResponseTime.get() / count;
        }
    }

    /**
     * Load Distribution
     */
    @lombok.Data
    @lombok.Builder
    public static class LoadDistribution {
        private long totalRequests;
        private double averageLoad;
        private double maxLoad;
        private double minLoad;
        private double standardDeviation;
        private boolean isBalanced;
        private long timestamp;
    }
}
