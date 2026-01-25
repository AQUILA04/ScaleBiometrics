package com.scalebiometrics.master.grpc;

import com.scalebiometrics.core.exception.BiometricException;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * Worker Client - gRPC client for communicating with Worker nodes
 * 
 * Responsibilities:
 * - Establish gRPC connections to workers
 * - Send matching requests
 * - Handle timeouts and retries
 * - Manage connection lifecycle
 */
@Slf4j
public class WorkerClient {

    private final String workerId;
    private final String host;
    private final int port;
    private ManagedChannel channel;
    private MatcherGrpc.MatcherBlockingStub stub;
    private static final long DEADLINE_SECONDS = 2;

    public WorkerClient(String workerId, String host, int port) {
        this.workerId = workerId;
        this.host = host;
        this.port = port;
    }

    /**
     * Initialize connection to worker
     */
    public void connect() throws BiometricException {
        try {
            this.channel = ManagedChannelBuilder
                    .forAddress(host, port)
                    .usePlaintext()
                    .keepAliveWithoutCalls(true)
                    .keepAliveTime(30, TimeUnit.SECONDS)
                    .keepAliveTimeout(10, TimeUnit.SECONDS)
                    .build();

            this.stub = MatcherGrpc.newBlockingStub(channel)
                    .withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS);

            log.info("Connected to worker {} at {}:{}", workerId, host, port);

        } catch (Exception e) {
            log.error("Failed to connect to worker {}", workerId, e);
            throw new BiometricException("Failed to connect to worker: " + e.getMessage(), e);
        }
    }

    /**
     * Perform 1:N matching on worker
     */
    public Match1NResponse match1N(Match1NRequest request) throws BiometricException {
        try {
            log.debug("Sending 1:N matching request to worker {} - Probe: {}", 
                    workerId, request.getProbeRid());

            Match1NResponse response = stub.match1N(request);

            log.debug("Received 1:N response from worker {} - Candidates: {}", 
                    workerId, response.getCandidatesCount());

            return response;

        } catch (io.grpc.StatusRuntimeException e) {
            if (e.getStatus() == Status.DEADLINE_EXCEEDED) {
                log.warn("1:N matching timeout on worker {}", workerId);
                throw new BiometricException("Worker timeout: " + workerId, e);
            }
            log.error("1:N matching failed on worker {}", workerId, e);
            throw new BiometricException("Worker error: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error performing 1:N matching on worker {}", workerId, e);
            throw new BiometricException("Failed to perform 1:N matching: " + e.getMessage(), e);
        }
    }

    /**
     * Perform 1:1 matching on worker
     */
    public Match1To1Response match1To1(Match1To1Request request) throws BiometricException {
        try {
            log.debug("Sending 1:1 matching request to worker {} - Probe: {}, Target: {}", 
                    workerId, request.getProbeRid(), request.getTargetRid());

            Match1To1Response response = stub.match1To1(request);

            log.debug("Received 1:1 response from worker {} - Score: {}", 
                    workerId, response.getScore());

            return response;

        } catch (io.grpc.StatusRuntimeException e) {
            if (e.getStatus() == Status.DEADLINE_EXCEEDED) {
                log.warn("1:1 matching timeout on worker {}", workerId);
                throw new BiometricException("Worker timeout: " + workerId, e);
            }
            log.error("1:1 matching failed on worker {}", workerId, e);
            throw new BiometricException("Worker error: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error performing 1:1 matching on worker {}", workerId, e);
            throw new BiometricException("Failed to perform 1:1 matching: " + e.getMessage(), e);
        }
    }

    /**
     * Check worker health
     */
    public boolean healthCheck() {
        try {
            HealthCheckResponse response = stub.healthCheck(HealthCheckRequest.newBuilder().build());
            boolean isHealthy = "HEALTHY".equals(response.getStatus());

            if (isHealthy) {
                log.debug("Worker {} is healthy", workerId);
            } else {
                log.warn("Worker {} health check failed: {}", workerId, response.getMessage());
            }

            return isHealthy;

        } catch (Exception e) {
            log.warn("Health check failed for worker {}: {}", workerId, e.getMessage());
            return false;
        }
    }

    /**
     * Get worker status
     */
    public WorkerMetrics getWorkerStatus() throws BiometricException {
        try {
            WorkerMetrics metrics = stub.getWorkerStatus(GetWorkerStatusRequest.newBuilder().build());
            log.debug("Retrieved worker status - Vectors: {}, Latency: {}ms", 
                    metrics.getTotalVectors(), metrics.getAvgLatencyMs());
            return metrics;

        } catch (Exception e) {
            log.error("Failed to get worker status", e);
            throw new BiometricException("Failed to get worker status: " + e.getMessage(), e);
        }
    }

    /**
     * Close connection
     */
    public void close() {
        if (channel != null && !channel.isShutdown()) {
            try {
                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
                log.info("Closed connection to worker {}", workerId);
            } catch (InterruptedException e) {
                channel.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Check if connection is active
     */
    public boolean isConnected() {
        return channel != null && !channel.isShutdown();
    }

    /**
     * Get worker ID
     */
    public String getWorkerId() {
        return workerId;
    }
}
