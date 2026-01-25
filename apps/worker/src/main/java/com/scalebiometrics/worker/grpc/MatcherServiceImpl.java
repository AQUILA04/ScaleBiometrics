package com.scalebiometrics.worker.grpc;

import com.scalebiometrics.core.domain.Fingerprint;
import com.scalebiometrics.core.domain.MatchResult;
import com.scalebiometrics.proto.matcher.*;
import com.scalebiometrics.worker.engine.HybridMatchingEngine;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * gRPC MatcherService Implementation
 * 
 * Provides gRPC endpoints for Master-Worker communication
 * Implements hybrid matching with HNSW + SourceAFIS
 */
@Slf4j
@Service
public class MatcherServiceImpl extends MatcherServiceGrpc.MatcherServiceImplBase {

    private final HybridMatchingEngine matchingEngine;

    public MatcherServiceImpl(HybridMatchingEngine matchingEngine) {
        this.matchingEngine = matchingEngine;
    }

    /**
     * 1:N Matching RPC - Deduplication
     * Matches probe fingerprint against all templates in worker's shard
     */
    @Override
    public void match1N(MatchRequest request, StreamObserver<MatchResponse> responseObserver) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("Received 1:N matching request - Trace ID: {}, Probe RID: {}", 
                    request.getTraceId(), request.getProbeRid());

            // Convert gRPC request to domain object
            Fingerprint probeFingerprint = convertToFingerprint(request.getProbeTemplate());

            // Perform matching
            MatchResult matchResult = matchingEngine.match1N(
                    probeFingerprint,
                    request.getTopK() > 0 ? request.getTopK() : 10
            );

            // Convert domain result to gRPC response
            MatchResponse response = convertToMatch1NResponse(matchResult, request.getTraceId());

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            long duration = System.currentTimeMillis() - startTime;
            log.info("1:N matching completed in {}ms - Trace ID: {}", duration, request.getTraceId());

        } catch (Exception e) {
            log.error("Error in 1:N matching - Trace ID: {}", request.getTraceId(), e);
            MatchResponse errorResponse = MatchResponse.newBuilder()
                    .setTraceId(request.getTraceId())
                    .setStatus(MatchResponse.MatchStatus.WORKER_ERROR)
                    .setErrorMessage(e.getMessage())
                    .setMatchingTimeMs(System.currentTimeMillis() - startTime)
                    .build();
            responseObserver.onNext(errorResponse);
            responseObserver.onCompleted();
        }
    }

    /**
     * 1:1 Verification RPC
     * Matches probe fingerprint against specific target template
     */
    @Override
    public void match1To1(VerificationRequest request, StreamObserver<VerificationResponse> responseObserver) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("Received 1:1 verification request - Trace ID: {}, Probe RID: {}, Target RID: {}", 
                    request.getTraceId(), request.getProbeRid(), request.getTargetRid());

            // Convert gRPC request to domain objects
            Fingerprint probeFingerprint = convertToFingerprint(request.getProbeTemplate());
            Fingerprint targetFingerprint = convertToFingerprint(request.getTargetTemplate());

            // Perform matching
            MatchResult matchResult = matchingEngine.match1To1(probeFingerprint, targetFingerprint);

            // Convert domain result to gRPC response
            VerificationResponse response = convertToVerificationResponse(matchResult, request.getTraceId());

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            long duration = System.currentTimeMillis() - startTime;
            log.info("1:1 verification completed in {}ms - Trace ID: {}", duration, request.getTraceId());

        } catch (Exception e) {
            log.error("Error in 1:1 verification - Trace ID: {}", request.getTraceId(), e);
            VerificationResponse errorResponse = VerificationResponse.newBuilder()
                    .setTraceId(request.getTraceId())
                    .setErrorMessage(e.getMessage())
                    .setMatchingTimeMs(System.currentTimeMillis() - startTime)
                    .build();
            responseObserver.onNext(errorResponse);
            responseObserver.onCompleted();
        }
    }

    /**
     * Health Check RPC
     */
    @Override
    public void healthCheck(HealthCheckRequest request, StreamObserver<HealthCheckResponse> responseObserver) {
        try {
            log.debug("Health check request from worker: {}", request.getWorkerId());
            
            HealthCheckResponse response = HealthCheckResponse.newBuilder()
                    .setWorkerId(request.getWorkerId())
                    .setIsHealthy(true)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error in health check", e);
            HealthCheckResponse errorResponse = HealthCheckResponse.newBuilder()
                    .setWorkerId(request.getWorkerId())
                    .setIsHealthy(false)
                    .setErrorMessage(e.getMessage())
                    .build();
            responseObserver.onNext(errorResponse);
            responseObserver.onCompleted();
        }
    }

    /**
     * Get Worker Status RPC
     */
    @Override
    public void getWorkerStatus(StatusRequest request, StreamObserver<StatusResponse> responseObserver) {
        try {
            log.debug("Status request from worker: {}", request.getWorkerId());

            WorkerMetrics metrics = WorkerMetrics.newBuilder()
                    .setTotalMatches(matchingEngine.getTotalMatches())
                    .setTotalErrors(matchingEngine.getTotalErrors())
                    .setAvgLatencyMs(matchingEngine.getAverageLatency())
                    .setP95LatencyMs(matchingEngine.getP95Latency())
                    .setP99LatencyMs(matchingEngine.getP99Latency())
                    .build();

            StatusResponse response = StatusResponse.newBuilder()
                    .setWorkerId(request.getWorkerId())
                    .setStatus("HEALTHY")
                    .setMetrics(metrics)
                    .setUptimeMs(matchingEngine.getUptime())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error in status check", e);
            StatusResponse errorResponse = StatusResponse.newBuilder()
                    .setWorkerId(request.getWorkerId())
                    .setStatus("UNHEALTHY")
                    .build();
            responseObserver.onNext(errorResponse);
            responseObserver.onCompleted();
        }
    }

    /**
     * Convert gRPC bytes to domain Fingerprint object
     */
    private Fingerprint convertToFingerprint(com.google.protobuf.ByteString template) {
        Fingerprint fingerprint = new Fingerprint();
        fingerprint.setTemplate(template.toByteArray());
        return fingerprint;
    }

    /**
     * Convert domain MatchResult to gRPC Match1NResponse
     */
    private MatchResponse convertToMatch1NResponse(MatchResult matchResult, String traceId) {
        List<MatchResponse.Candidate> candidates = new ArrayList<>();
        
        if (matchResult.getCandidates() != null) {
            for (MatchResult.Candidate candidate : matchResult.getCandidates()) {
                candidates.add(MatchResponse.Candidate.newBuilder()
                        .setTargetRid(candidate.getTargetRid())
                        .setHnnScore(candidate.getHnnScore())
                        .setExactScore(candidate.getExactScore())
                        .setFinalScore(candidate.getFinalScore())
                        .setIsMatch(candidate.isMatch())
                        .build());
            }
        }

        return MatchResponse.newBuilder()
                .setTraceId(traceId)
                .setStatus(MatchResponse.MatchStatus.SUCCESS)
                .addAllCandidates(candidates)
                .setMatchingTimeMs(matchResult.getMatchingTimeMs())
                .build();
    }

    /**
     * Convert domain MatchResult to gRPC VerificationResponse
     */
    private VerificationResponse convertToVerificationResponse(MatchResult matchResult, String traceId) {
        return VerificationResponse.newBuilder()
                .setTraceId(traceId)
                .setIsMatch(matchResult.isMatch())
                .setScore(matchResult.getScore())
                .setMatchingTimeMs(matchResult.getMatchingTimeMs())
                .build();
    }
}
