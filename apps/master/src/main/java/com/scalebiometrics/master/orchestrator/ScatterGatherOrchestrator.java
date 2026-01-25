package com.scalebiometrics.master.orchestrator;

import com.scalebiometrics.core.domain.Fingerprint;
import com.scalebiometrics.core.domain.MatchResult;
import com.scalebiometrics.core.exception.BiometricException;
import com.scalebiometrics.master.grpc.WorkerClient;
import com.scalebiometrics.master.grpc.WorkerPool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Scatter-Gather Orchestrator - Distributes matching requests across workers
 * 
 * Pattern:
 * 1. Scatter: Send matching request to all workers in parallel
 * 2. Gather: Collect results from all workers
 * 3. Aggregate: Merge and sort results
 * 4. Return: Top-K results to client
 * 
 * This achieves sub-2 second latency for 10M+ records by:
 * - Parallelizing across multiple workers
 * - Each worker searches its shard independently
 * - Master aggregates results
 */
@Slf4j
@Component
public class ScatterGatherOrchestrator {

    private final WorkerPool workerPool;
    private final ExecutorService executorService;
    private static final long SCATTER_GATHER_TIMEOUT_MS = 1500;
    private static final int DEFAULT_TOP_K = 10;

    public ScatterGatherOrchestrator(WorkerPool workerPool) {
        this.workerPool = workerPool;
        this.executorService = Executors.newFixedThreadPool(
                Math.max(4, Runtime.getRuntime().availableProcessors()),
                r -> {
                    Thread t = new Thread(r, "scatter-gather-" + Thread.currentThread().getId());
                    t.setDaemon(false);
                    return t;
                }
        );
    }

    /**
     * Perform distributed 1:N matching using scatter-gather pattern
     * 
     * Process:
     * 1. Scatter: Send probe to all workers
     * 2. Gather: Collect results with timeout
     * 3. Aggregate: Merge and rank results
     * 4. Return: Top-K matches
     */
    public MatchResult match1NDistributed(Fingerprint probeFingerprint, int topK) 
            throws BiometricException {
        long startTime = System.currentTimeMillis();
        String traceId = UUID.randomUUID().toString();

        try {
            log.info("[{}] Starting distributed 1:N matching for probe RID: {}", 
                    traceId, probeFingerprint.getRid());

            List<WorkerClient> workers = workerPool.getHealthyWorkers();
            if (workers.isEmpty()) {
                throw new BiometricException("No healthy workers available");
            }

            // Phase 1: Scatter - Send requests to all workers
            log.debug("[{}] Scattering request to {} workers", traceId, workers.size());
            List<Future<ScatterResult>> futures = scatterRequests(probeFingerprint, workers, traceId);

            // Phase 2: Gather - Collect results with timeout
            log.debug("[{}] Gathering results from workers", traceId);
            List<ScatterResult> results = gatherResults(futures, SCATTER_GATHER_TIMEOUT_MS);

            // Phase 3: Aggregate - Merge and rank results
            log.debug("[{}] Aggregating results from {} workers", traceId, results.size());
            List<MatchResult.Candidate> aggregatedCandidates = aggregateResults(results);

            // Phase 4: Return top-K
            List<MatchResult.Candidate> topKCandidates = aggregatedCandidates.stream()
                    .limit(topK)
                    .collect(Collectors.toList());

            long totalTime = System.currentTimeMillis() - startTime;
            String status = topKCandidates.isEmpty() ? "NO_MATCH" : "MATCH";

            MatchResult result = MatchResult.builder()
                    .probeRid(probeFingerprint.getRid())
                    .candidates(topKCandidates)
                    .status(status)
                    .matchingTimeMs(totalTime)
                    .traceId(traceId)
                    .build();

            log.info("[{}] Distributed 1:N matching completed in {}ms, found {} matches", 
                    traceId, totalTime, topKCandidates.size());

            return result;

        } catch (Exception e) {
            log.error("[{}] Error during distributed 1:N matching", traceId, e);
            throw new BiometricException("Distributed matching failed: " + e.getMessage(), e);
        }
    }

    /**
     * Phase 1: Scatter - Send matching requests to all workers in parallel
     */
    private List<Future<ScatterResult>> scatterRequests(
            Fingerprint probeFingerprint,
            List<WorkerClient> workers,
            String traceId) {

        List<Future<ScatterResult>> futures = new ArrayList<>();

        for (WorkerClient worker : workers) {
            Future<ScatterResult> future = executorService.submit(() -> {
                try {
                    log.debug("[{}] Sending request to worker {}", traceId, worker.getWorkerId());

                    // Create gRPC request
                    Match1NRequest request = Match1NRequest.newBuilder()
                            .setProbeRid(probeFingerprint.getRid())
                            .setProbeFingerprint(convertToProto(probeFingerprint))
                            .setTopK(100)  // Get more candidates for aggregation
                            .build();

                    // Send to worker
                    Match1NResponse response = worker.match1N(request);

                    log.debug("[{}] Received {} candidates from worker {}", 
                            traceId, response.getCandidatesCount(), worker.getWorkerId());

                    return ScatterResult.builder()
                            .workerId(worker.getWorkerId())
                            .success(true)
                            .response(response)
                            .build();

                } catch (Exception e) {
                    log.warn("[{}] Error from worker {}: {}", 
                            traceId, worker.getWorkerId(), e.getMessage());

                    return ScatterResult.builder()
                            .workerId(worker.getWorkerId())
                            .success(false)
                            .error(e.getMessage())
                            .build();
                }
            });

            futures.add(future);
        }

        return futures;
    }

    /**
     * Phase 2: Gather - Collect results from all workers with timeout
     */
    private List<ScatterResult> gatherResults(List<Future<ScatterResult>> futures, long timeoutMs) {
        List<ScatterResult> results = new ArrayList<>();
        long deadline = System.currentTimeMillis() + timeoutMs;

        for (Future<ScatterResult> future : futures) {
            try {
                long remainingTime = deadline - System.currentTimeMillis();
                if (remainingTime <= 0) {
                    future.cancel(true);
                    log.warn("Timeout waiting for worker result");
                    continue;
                }

                ScatterResult result = future.get(remainingTime, TimeUnit.MILLISECONDS);
                results.add(result);

            } catch (TimeoutException e) {
                future.cancel(true);
                log.warn("Worker result timeout");
            } catch (Exception e) {
                log.warn("Error gathering result: {}", e.getMessage());
            }
        }

        return results;
    }

    /**
     * Phase 3: Aggregate - Merge results from all workers and rank by score
     */
    private List<MatchResult.Candidate> aggregateResults(List<ScatterResult> results) {
        Map<String, MatchResult.Candidate> candidateMap = new HashMap<>();

        for (ScatterResult result : results) {
            if (!result.isSuccess()) {
                continue;
            }

            Match1NResponse response = result.getResponse();
            for (Candidate candidate : response.getCandidatesList()) {
                String targetRid = candidate.getTargetRid();

                if (candidateMap.containsKey(targetRid)) {
                    // Merge scores from multiple workers
                    MatchResult.Candidate existing = candidateMap.get(targetRid);
                    int mergedScore = Math.max(existing.getExactScore(), candidate.getExactScore());
                    existing.setExactScore(mergedScore);
                    existing.setFinalScore(mergedScore);
                } else {
                    // Add new candidate
                    MatchResult.Candidate newCandidate = MatchResult.Candidate.builder()
                            .targetRid(targetRid)
                            .hnswScore(candidate.getHnnScore())
                            .exactScore(candidate.getExactScore())
                            .finalScore(candidate.getFinalScore())
                            .isMatch(candidate.getIsMatch())
                            .build();
                    candidateMap.put(targetRid, newCandidate);
                }
            }
        }

        // Sort by final score descending
        return candidateMap.values().stream()
                .sorted(Comparator.comparingInt(MatchResult.Candidate::getFinalScore).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Convert domain Fingerprint to gRPC proto
     */
    private FingerprintProto convertToProto(Fingerprint fingerprint) {
        FingerprintProto.Builder builder = FingerprintProto.newBuilder()
                .setRid(fingerprint.getRid())
                .setFingerIndex(fingerprint.getFingerIndex())
                .setImageUrl(fingerprint.getImageUrl() != null ? fingerprint.getImageUrl() : "")
                .setBinaryTemplate(com.google.protobuf.ByteString.copyFrom(fingerprint.getBinaryTemplate()))
                .setQuality(fingerprint.getQuality())
                .setStatus(fingerprint.getStatus());

        if (fingerprint.getEmbeddingVector() != null) {
            for (float value : fingerprint.getEmbeddingVector()) {
                builder.addEmbeddingVector(value);
            }
        }

        return builder.build();
    }

    /**
     * Shutdown orchestrator
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
        log.info("ScatterGatherOrchestrator shutdown complete");
    }

    /**
     * Scatter Result - Result from a single worker
     */
    @lombok.Data
    @lombok.Builder
    public static class ScatterResult {
        private String workerId;
        private boolean success;
        private Match1NResponse response;
        private String error;
    }
}
