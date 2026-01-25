package com.scalebiometrics.master.orchestrator;

import com.scalebiometrics.core.domain.Fingerprint;
import com.scalebiometrics.core.domain.MatchResult;
import com.scalebiometrics.core.exception.BiometricException;
import com.scalebiometrics.master.grpc.WorkerClient;
import com.scalebiometrics.master.grpc.WorkerPool;
import com.scalebiometrics.proto.matcher.MatchRequest;
import com.scalebiometrics.proto.matcher.MatchResponse;

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

    public ScatterGatherOrchestrator(WorkerPool workerPool) {
        this.workerPool = workerPool;
        this.executorService = Executors.newCachedThreadPool();
    }

    /**
     * Perform 1:N matching (identification)
     */
    public MatchResult match1N(Fingerprint probe, int topK) throws BiometricException {
        long startTime = System.currentTimeMillis();
        List<WorkerClient> workers = workerPool.getHealthyWorkers();

        if (workers.isEmpty()) {
            throw new BiometricException("No healthy workers available for 1:N matching", "NO_WORKERS_AVAILABLE");
        }

        // Scatter: Send requests to all workers in parallel
        List<Future<ScatterResult>> futures = new ArrayList<>();
        for (WorkerClient worker : workers) {
            Future<ScatterResult> future = executorService.submit(() -> {
                try {
                    MatchRequest request = MatchRequest.newBuilder()
                            .setProbeRid(probe.getRid())
                            .setTopK(topK)
                            .build();

                    MatchResponse response = worker.match1N(request);
                    return new ScatterResult(worker.getWorkerId(), response);
                } catch (Exception e) {
                    log.error("Error during 1:N matching on worker {}", worker.getWorkerId(), e);
                    return new ScatterResult(worker.getWorkerId(), e);
                }
            });
            futures.add(future);
        }

        // Gather: Collect results from all workers
        List<ScatterResult> results = new ArrayList<>();
        for (Future<ScatterResult> future : futures) {
            try {
                results.add(future.get(10, TimeUnit.SECONDS)); // 10-second timeout
            } catch (Exception e) {
                log.error("Error getting 1:N result from worker", e);
            }
        }

        // Aggregate: Merge and sort results
        MatchResult finalResult = aggregateResults(results, probe.getRid(), topK);

        long endTime = System.currentTimeMillis();
        finalResult.setMatchingTimeMs(endTime - startTime);
        log.info("1:N matching completed in {}ms for probe {}", finalResult.getMatchingTimeMs(), probe.getRid());

        return finalResult;
    }

    /**
     * Aggregate results from all workers
     */
    private MatchResult aggregateResults(List<ScatterResult> scatterResults, String probeRid, int topK) {
        List<MatchResult.Candidate> allCandidates = new ArrayList<>();
        boolean hasErrors = false;

        for (ScatterResult result : scatterResults) {
            if (result.isSuccess()) {
                allCandidates.addAll(result.getResponse().getCandidatesList().stream()
                        .map(this::convertToDomain)
                        .collect(Collectors.toList()));
            } else {
                hasErrors = true;
            }
        }

        // Sort by score (descending)
        allCandidates.sort(Comparator.comparingDouble(MatchResult.Candidate::getScore).reversed());

        // Get top-K candidates
        List<MatchResult.Candidate> topCandidates = allCandidates.stream()
                .limit(topK)
                .collect(Collectors.toList());

        MatchResult finalResult = new MatchResult();
        finalResult.setProbeRid(probeRid);
        finalResult.setCandidates(topCandidates);
        finalResult.setStatus(hasErrors ? MatchResult.MatchStatus.AMBIGUOUS : MatchResult.MatchStatus.MATCH_FOUND);

        if (!hasErrors && topCandidates.isEmpty()) {
            finalResult.setStatus(MatchResult.MatchStatus.NO_MATCH);
        }

        return finalResult;
    }

    /**
     * Convert gRPC Candidate to domain Candidate
     */
    private MatchResult.Candidate convertToDomain(com.scalebiometrics.proto.matcher.Candidate protoCandidate) {
        return MatchResult.Candidate.builder()
                .targetRid(protoCandidate.getTargetRid())
                .score(protoCandidate.getFinalScore())
                .isMatch(protoCandidate.getIsMatch())
                .build();
    }

    /**
     * Convert domain Fingerprint to gRPC proto
     */
    

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
    }

    /**
     * Inner class for scatter-gather results
     */
        private static class ScatterResult {
        private final String workerId;
        private final MatchResponse response;
        private final Exception exception;

        public ScatterResult(String workerId, MatchResponse response) {
            this.workerId = workerId;
            this.response = response;
            this.exception = null;
        }

        public ScatterResult(String workerId, Exception exception) {
            this.workerId = workerId;
            this.response = null;
            this.exception = exception;
        }

        public boolean isSuccess() {
            return exception == null;
        }

        public String getWorkerId() {
            return workerId;
        }

        public MatchResponse getResponse() {
            return response;
        }

        public Exception getException() {
            return exception;
        }
    }
}
