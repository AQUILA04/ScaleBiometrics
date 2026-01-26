package com.scalebiometrics.master.grpc;

import com.scalebiometrics.core.exception.BiometricException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Worker Pool - Manages connections to all worker nodes
 * 
 * Responsibilities:
 * - Maintain connections to all workers
 * - Health check and failover
 * - Load balancing
 * - Dynamic worker discovery
 */
@Slf4j
@Component
public class WorkerPool {

    private final Map<String, WorkerClient> workers = new ConcurrentHashMap<>();
    private final Map<String, WorkerMetadata> workerMetadata = new ConcurrentHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Register a worker
     */
    public void registerWorker(String workerId, String host, int port) throws BiometricException {
        try {
            lock.writeLock().lock();

            if (workers.containsKey(workerId)) {
                log.warn("Worker {} already registered", workerId);
                return;
            }

            WorkerClient client = new WorkerClient(workerId, host, port);
            client.connect();

            workers.put(workerId, client);
            workerMetadata.put(workerId, WorkerMetadata.builder()
                    .workerId(workerId)
                    .host(host)
                    .port(port)
                    .status("ACTIVE")
                    .registeredAt(System.currentTimeMillis())
                    .lastHeartbeat(System.currentTimeMillis())
                    .build());

            log.info("Registered worker {} at {}:{}", workerId, host, port);

        } catch (Exception e) {
            log.error("Failed to register worker {}", workerId, e);
            throw new BiometricException("Failed to register worker", e.getMessage(), e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Unregister a worker
     */
    public void unregisterWorker(String workerId) {
        try {
            lock.writeLock().lock();

            WorkerClient client = workers.remove(workerId);
            if (client != null) {
                client.shutdown();
            }

            workerMetadata.remove(workerId);
            log.info("Unregistered worker {}", workerId);

        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Get worker by ID
     */
    public WorkerClient getWorker(String workerId) throws BiometricException {
        try {
            lock.readLock().lock();

            WorkerClient client = workers.get(workerId);
            if (client == null) {
                throw new BiometricException("Worker not found", workerId);
            }

            if (client.healthCheck() == false) {
                throw new BiometricException("Worker not connected", workerId);
            }

            return client;

        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get all active workers
     */
    public List<WorkerClient> getAllWorkers() {
        try {
            lock.readLock().lock();
            return new ArrayList<>(workers.values());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get healthy workers (pass health check)
     */
    public List<WorkerClient> getHealthyWorkers() {
        try {
            lock.readLock().lock();

            List<WorkerClient> healthyWorkers = new ArrayList<>();
            for (WorkerClient client : workers.values()) {
                if (client.healthCheck()) {
                    healthyWorkers.add(client);
                }
            }

            return healthyWorkers;

        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get worker count
     */
    public int getWorkerCount() {
        return workers.size();
    }

    /**
     * Update worker heartbeat
     */
    public void updateHeartbeat(String workerId) {
        WorkerMetadata metadata = workerMetadata.get(workerId);
        if (metadata != null) {
            metadata.setLastHeartbeat(System.currentTimeMillis());
        }
    }

    /**
     * Get worker metadata
     */
    public WorkerMetadata getWorkerMetadata(String workerId) {
        return workerMetadata.get(workerId);
    }

    /**
     * Get all worker metadata
     */
    public Collection<WorkerMetadata> getAllWorkerMetadata() {
        return workerMetadata.values();
    }

    /**
     * Shutdown all workers
     */
    public void shutdown() {
        try {
            lock.writeLock().lock();

            for (WorkerClient client : workers.values()) {
                try {
                    client.shutdown();
                } catch (Exception e) {
                    log.error("Error closing worker connection", e);
                }
            }

            workers.clear();
            workerMetadata.clear();
            log.info("Worker pool shutdown complete");

        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Worker Metadata
     */
    @lombok.Data
    @lombok.Builder
    public static class WorkerMetadata {
        private String workerId;
        private String host;
        private int port;
        private String status;
        private long registeredAt;
        private long lastHeartbeat;
        private int totalMatches;
        private long totalVectors;
        private long indexSizeBytes;
        private long offheapMemoryBytes;
    }
}
