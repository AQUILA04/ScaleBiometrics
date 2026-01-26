package com.scalebiometrics.master.grpc;

import com.scalebiometrics.core.exception.BiometricException;
import com.scalebiometrics.proto.matcher.*;
import com.scalebiometrics.proto.matcher.MatcherServiceGrpc;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class WorkerClient {

    private final String workerId;
    private final String host;
    private final int port;
    private ManagedChannel channel;
    private MatcherServiceGrpc.MatcherServiceBlockingStub stub;
    private static final long DEADLINE_SECONDS = 2;

    public WorkerClient(String workerId, String host, int port) {
        this.workerId = workerId;
        this.host = host;
        this.port = port;
    }

    public void connect() throws BiometricException {
        try {
            this.channel = ManagedChannelBuilder.forAddress(host, port)
                    .usePlaintext()
                    .keepAliveTime(30, TimeUnit.SECONDS)
                    .keepAliveTimeout(10, TimeUnit.SECONDS)
                    .build();

            this.stub = MatcherServiceGrpc.newBlockingStub(channel)
                    .withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS);

            log.info("Connected to worker {} at {}:{}", workerId, host, port);

        } catch (Exception e) {
            log.error("Error connecting to worker {}", workerId, e);
            throw new BiometricException("Failed to connect to worker", e.getMessage(), e);
        }
    }

    public MatchResponse match1N(MatchRequest request) throws BiometricException {
        try {
            log.debug("Sending 1:N matching request to worker {} - Probe: {}",
                    workerId, request.getProbeRid());

            MatchResponse response = stub.match1N(request);

            log.debug("Received 1:N response from worker {} - Candidates: {}",
                    workerId, response.getCandidatesCount());

            return response;

        } catch (io.grpc.StatusRuntimeException e) {
            if (e.getStatus() == Status.DEADLINE_EXCEEDED) {
                log.warn("1:N matching timeout on worker {}", workerId);
                throw new BiometricException("Worker timeout", workerId, e);
            }
            log.error("1:N matching failed on worker {}", workerId, e);
            throw new BiometricException("Worker error", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error performing 1:N matching on worker {}", workerId, e);
            throw new BiometricException("Failed to perform 1:N matching", e.getMessage(), e);
        }
    }

    public VerificationResponse match1To1(VerificationRequest request) throws BiometricException {
        try {
            log.debug("Sending 1:1 matching request to worker {} - Probe: {}, Target: {}",
                    workerId, request.getProbeRid(), request.getTargetRid());

            VerificationResponse response = stub.match1To1(request);

            log.debug("Received 1:1 response from worker {} - Score: {}",
                    workerId, response.getScore());

            return response;

        } catch (io.grpc.StatusRuntimeException e) {
            if (e.getStatus() == Status.DEADLINE_EXCEEDED) {
                log.warn("1:1 matching timeout on worker {}", workerId);
                throw new BiometricException("Worker timeout", workerId, e);
            }
            log.error("1:1 matching failed on worker {}", workerId, e);
            throw new BiometricException("Worker error", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error performing 1:1 matching on worker {}", workerId, e);
            throw new BiometricException("Failed to perform 1:1 matching", e.getMessage(), e);
        }
    }

    public boolean healthCheck() {
        try {
            HealthCheckResponse response = stub.healthCheck(HealthCheckRequest.newBuilder().build());
            boolean isHealthy = response.getIsHealthy();

            if (isHealthy) {
                log.debug("Worker {} is healthy", workerId);
            } else {
                log.warn("Worker {} health check failed: {}", workerId, response.getErrorMessage());
            }

            return isHealthy;

        } catch (Exception e) {
            log.warn("Health check failed for worker {}: {}", workerId, e.getMessage());
            return false;
        }
    }

    public StatusResponse getWorkerStatus() throws BiometricException {
        try {
            StatusResponse response = stub.getWorkerStatus(StatusRequest.newBuilder().build());
            log.debug("Retrieved worker status - Uptime: {}ms",
                    response.getUptimeMs());
            return response;

        } catch (Exception e) {
            log.error("Error getting worker status for {}", workerId, e);
            throw new BiometricException("Failed to get worker status", e.getMessage(), e);
        }
    }

    public void shutdown() {
        try {
            if (channel != null) {
                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            log.warn("Interrupted while shutting down worker client {}", workerId, e);
            Thread.currentThread().interrupt();
        }
    }

    public String getWorkerId() {
        return workerId;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
