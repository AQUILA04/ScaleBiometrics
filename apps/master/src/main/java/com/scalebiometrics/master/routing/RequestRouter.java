package com.scalebiometrics.master.routing;

import com.scalebiometrics.core.exception.BiometricException;
import com.scalebiometrics.master.grpc.WorkerClient;
import com.scalebiometrics.master.grpc.WorkerPool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Request Router - Routes matching requests to appropriate workers.
 * 
 * Strategies:
 * - HASH: Route based on fingerprint RID hash (consistent hashing)
 * - ROUND_ROBIN: Distribute requests sequentially across workers
 * - LEAST_LOADED: Route to worker with lowest load
 */
@Slf4j
@Service
public class RequestRouter {

    public enum RoutingStrategy {
        HASH,           // Consistent hashing based on RID
        ROUND_ROBIN,    // Sequential distribution
        LEAST_LOADED    // Route to least loaded worker
    }

    @Value("${master.routing.strategy:HASH}")
    private String routingStrategyStr;

    @Value("${master.routing.replication-factor:1}")
    private int replicationFactor;

    private final WorkerPool workerPool;
    private RoutingStrategy routingStrategy;
    private final AtomicInteger roundRobinIndex = new AtomicInteger(0);

    public RequestRouter(WorkerPool workerPool) {
        this.workerPool = workerPool;
    }

    /**
     * Initialize routing strategy
     */
    public void initialize() {
        try {
            this.routingStrategy = RoutingStrategy.valueOf(routingStrategyStr.toUpperCase());
            log.info("Initialized request router with strategy: {}", routingStrategy);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid routing strategy: {}, defaulting to HASH", routingStrategyStr);
            this.routingStrategy = RoutingStrategy.HASH;
        }
    }

    /**
     * Route request to a worker
     */
    public WorkerClient routeRequest(String ridOrKey) throws BiometricException {
        List<WorkerClient> workers = workerPool.getHealthyWorkers();
        if (workers.isEmpty()) {
            throw new BiometricException("No healthy workers available");
        }

        WorkerClient worker = switch (routingStrategy) {
            case HASH -> routeByHash(ridOrKey, workers);
            case ROUND_ROBIN -> routeByRoundRobin(workers);
            case LEAST_LOADED -> routeByLeastLoaded(workers);
            default -> routeByHash(ridOrKey, workers);
        };

        log.debug("Routed request for {} to worker {}", ridOrKey, worker.getWorkerId());
        return worker;
    }

    /**
     * Route by consistent hash (HASH strategy)
     */
    private WorkerClient routeByHash(String key, List<WorkerClient> workers) {
        int hash = Math.abs(key.hashCode());
        int index = hash % workers.size();
        return workers.get(index);
    }

    /**
     * Route by round robin (ROUND_ROBIN strategy)
     */
    private WorkerClient routeByRoundRobin(List<WorkerClient> workers) {
        int index = roundRobinIndex.getAndIncrement() % workers.size();
        return workers.get(index);
    }

    /**
     * Route to least loaded worker (LEAST_LOADED strategy)
     */
    private WorkerClient routeByLeastLoaded(List<WorkerClient> workers) {
        WorkerClient leastLoadedWorker = workers.get(0);
        long minLoad = Long.MAX_VALUE;

        for (WorkerClient worker : workers) {
            try {
                // Get worker metrics
                var metrics = worker.getWorkerStatus();
                long load = metrics.getTotalMatches();

                if (load < minLoad) {
                    minLoad = load;
                    leastLoadedWorker = worker;
                }
            } catch (Exception e) {
                log.debug("Error getting worker status for {}: {}", worker.getWorkerId(), e.getMessage());
            }
        }

        return leastLoadedWorker;
    }

    /**
     * Get replicas for a key (for replication)
     */
    public List<WorkerClient> getReplicasForKey(String key) throws BiometricException {
        List<WorkerClient> workers = workerPool.getHealthyWorkers();
        if (workers.isEmpty()) {
            throw new BiometricException("No healthy workers available");
        }

        if (replicationFactor <= 0 || replicationFactor >= workers.size()) {
            return workers;  // Return all workers
        }

        List<WorkerClient> replicas = new ArrayList<>();
        int hash = Math.abs(key.hashCode());

        for (int i = 0; i < replicationFactor; i++) {
            int index = (hash + i) % workers.size();
            replicas.add(workers.get(index));
        }

        return replicas;
    }

    /**
     * Get routing strategy
     */
    public RoutingStrategy getRoutingStrategy() {
        return routingStrategy;
    }

    /**
     * Set routing strategy
     */
    public void setRoutingStrategy(RoutingStrategy strategy) {
        this.routingStrategy = strategy;
        log.info("Changed routing strategy to: {}", strategy);
    }

    /**
     * Get replication factor
     */
    public int getReplicationFactor() {
        return replicationFactor;
    }

    /**
     * Set replication factor
     */
    public void setReplicationFactor(int factor) {
        this.replicationFactor = factor;
        log.info("Changed replication factor to: {}", factor);
    }
}
