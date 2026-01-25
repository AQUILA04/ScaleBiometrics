package com.scalebiometrics.worker.grpc;

import com.scalebiometrics.core.domain.Fingerprint;
import com.scalebiometrics.core.domain.MatchResult;
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
 */
@Slf4j
@Service
public class MatcherServiceImpl extends MatcherGrpc.MatcherImplBase {

    private final HybridMatchingEngine matchingEngine;

    public MatcherServiceImpl(HybridMatchingEngine matchingEngine) {
        this.matchingEngine = matchingEngine;
    }

    /**
     * 1:N Matching RPC
     */
    @Override
    public void match1N(Match1NRequest request, StreamObserver<Match1NResponse> responseObserver) {
        try {
            log.info("Received 1:N matching request - Probe RID: {}", request.getProbeRid());

            // Convert gRPC request to domain object
            Fingerprint probeFingerprint = convertToFingerprint(request.getProbeFingerprint());

            // Perform matching
            MatchResult matchResult = matchingEngine.match1N(
                    probeFingerprint,
                    request.getTopK() > 0 ? request.getTopK() : 10
            );

            // Convert domain result to gRPC response
            Match1NResponse response = convertToMatch1NResponse(matchResult);

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("1:N matching completed - Trace ID: {}", matchResult.getTraceId());

        } catch (Exception e) {
            log.error("Error in 1:N matching", e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Matching failed: " + e.getMessage())
                    .asException());
        }
    }

    /**
     * 1:1 Matching RPC
     */
    @Override
    public void match1To1(Match1To1Request request, StreamObserver<Match1To1Response> responseObserver) {
        try {
            log.info("Received 1:1 matching request - Probe: {}, Target: {}", 
                    request.getProbeRid(), request.getTargetRid());

            // Convert gRPC requests to domain objects
            Fingerprint probeFingerprint = convertToFingerprint(request.getProbeFingerprint());
            Fingerprint targetFingerprint = convertToFingerprint(request.getTargetFingerprint());

            // Perform matching
            MatchResult matchResult = matchingEngine.match1To1(probeFingerprint, targetFingerprint);

            // Convert domain result to gRPC response
            Match1To1Response response = convertToMatch1To1Response(matchResult);

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("1:1 matching completed - Trace ID: {}", matchResult.getTraceId());

        } catch (Exception e) {
            log.error("Error in 1:1 matching", e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Matching failed: " + e.getMessage())
                    .asException());
        }
    }

    /**
     * Health Check RPC
     */
    @Override
    public void healthCheck(HealthCheckRequest request, StreamObserver<HealthCheckResponse> responseObserver) {
        try {
            HealthCheckResponse response = HealthCheckResponse.newBuilder()
                    .setStatus("HEALTHY")
                    .setMessage("Worker is healthy")
                    .setTimestamp(System.currentTimeMillis())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error in health check", e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Health check failed")
                    .asException());
        }
    }

    /**
     * Get Worker Status RPC
     */
    @Override
    public void getWorkerStatus(GetWorkerStatusRequest request, StreamObserver<WorkerMetrics> responseObserver) {
        try {
            // Get metrics from matching engine
            var indexStats = matchingEngine.getIndexStatistics();

            WorkerMetrics metrics = WorkerMetrics.newBuilder()
                    .setWorkerId("worker-1")  // Should come from configuration
                    .setTotalMatches(indexStats.getQueryCount())
                    .setAvgLatencyMs((long) indexStats.getAvgQueryTimeMs())
                    .setIndexSizeBytes(indexStats.getIndexSizeBytes())
                    .setOffheapMemoryBytes(indexStats.getOffHeapMemoryBytes())
                    .setTotalVectors(indexStats.getTotalVectors())
                    .setTimestamp(System.currentTimeMillis())
                    .build();

            responseObserver.onNext(metrics);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error getting worker status", e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Failed to get worker status")
                    .asException());
        }
    }

    /**
     * Convert gRPC Fingerprint to domain Fingerprint
     */
    private Fingerprint convertToFingerprint(FingerprintProto proto) {
        return Fingerprint.builder()
                .rid(proto.getRid())
                .fingerIndex(proto.getFingerIndex())
                .imageUrl(proto.getImageUrl())
                .binaryTemplate(proto.getBinaryTemplate().toByteArray())
                .embeddingVector(proto.getEmbeddingVectorList().stream()
                        .mapToFloat(f -> f)
                        .toArray())
                .quality(proto.getQuality())
                .status(proto.getStatus())
                .build();
    }

    /**
     * Convert domain MatchResult to gRPC Match1NResponse
     */
    private Match1NResponse convertToMatch1NResponse(MatchResult matchResult) {
        Match1NResponse.Builder builder = Match1NResponse.newBuilder()
                .setProbeRid(matchResult.getProbeRid())
                .setStatus(matchResult.getStatus())
                .setMatchingTimeMs(matchResult.getMatchingTimeMs())
                .setTraceId(matchResult.getTraceId());

        for (MatchResult.Candidate candidate : matchResult.getCandidates()) {
            Candidate candidateProto = Candidate.newBuilder()
                    .setTargetRid(candidate.getTargetRid())
                    .setHnnScore(candidate.getHnnScore())
                    .setExactScore(candidate.getExactScore())
                    .setFinalScore(candidate.getFinalScore())
                    .setIsMatch(candidate.isMatch())
                    .build();
            builder.addCandidates(candidateProto);
        }

        return builder.build();
    }

    /**
     * Convert domain MatchResult to gRPC Match1To1Response
     */
    private Match1To1Response convertToMatch1To1Response(MatchResult matchResult) {
        MatchResult.Candidate candidate = matchResult.getCandidates().get(0);

        return Match1To1Response.newBuilder()
                .setProbeRid(matchResult.getProbeRid())
                .setTargetRid(candidate.getTargetRid())
                .setScore(candidate.getExactScore())
                .setIsMatch(candidate.isMatch())
                .setMatchingTimeMs(matchResult.getMatchingTimeMs())
                .setTraceId(matchResult.getTraceId())
                .build();
    }
}
