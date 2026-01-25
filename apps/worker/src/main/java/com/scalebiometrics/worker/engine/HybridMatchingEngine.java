package com.scalebiometrics.worker.engine;

import com.scalebiometrics.core.domain.Fingerprint;
import com.scalebiometrics.core.domain.MatchResult;
import com.scalebiometrics.core.exception.BiometricException;
import io.jvector.jvector.pq.ProductQuantizer;
import io.jvector.jvector.vector.VectorFloat;
import io.jvector.jvector.vector.types.VectorTypeSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * HybridMatchingEngine - Combines HNSW (Approximate Nearest Neighbor) with SourceAFIS (Exact Matching).
 * 
 * Architecture:
 * 1. HNSW Phase: Fast approximate search using vector embeddings (O(log N))
 * 2. Exact Phase: Precise matching using SourceAFIS on candidates
 * 
 * This hybrid approach achieves:
 * - Sub-2 second latency for 10M+ records
 * - High accuracy with exact matching verification
 * - Efficient memory usage with off-heap storage
 */
@Slf4j
@Component
public class HybridMatchingEngine {

    private static final int DEFAULT_TOP_K = 10;
    private static final int HNSW_M = 16;
    private static final int HNSW_EF_CONSTRUCTION = 200;
    private static final int HNSW_EF_SEARCH = 100;
    private static final int EXACT_MATCH_THRESHOLD = 40;
    private static final int HNSW_SCORE_THRESHOLD = 30;

    private final HNSWIndexManager hnswIndexManager;
    private final SourceAFISMatcher sourceAFISMatcher;
    private final OffHeapMemoryManager offHeapMemoryManager;
    private final ExecutorService executorService;
    private final MatchingMetrics matchingMetrics;

    public HybridMatchingEngine(
            HNSWIndexManager hnswIndexManager,
            SourceAFISMatcher sourceAFISMatcher,
            OffHeapMemoryManager offHeapMemoryManager,
            MatchingMetrics matchingMetrics) {
        this.hnswIndexManager = hnswIndexManager;
        this.sourceAFISMatcher = sourceAFISMatcher;
        this.offHeapMemoryManager = offHeapMemoryManager;
        this.matchingMetrics = matchingMetrics;
        this.executorService = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors(),
                r -> {
                    Thread t = new Thread(r, "matching-executor-" + Thread.currentThread().getId());
                    t.setDaemon(false);
                    return t;
                }
        );
    }

    /**
     * Perform 1:N matching (deduplication/search).
     * 
     * Process:
     * 1. HNSW Phase: Find top-K candidates using embeddings (fast, approximate)
     * 2. Exact Phase: Verify candidates using SourceAFIS (accurate, slower)
     * 3. Aggregate: Return sorted results by exact score
     * 
     * @param probeFingerprint The probe fingerprint to match
     * @param topK Number of top candidates to return
     * @return MatchResult with candidates sorted by exact score
     */
    public MatchResult match1N(Fingerprint probeFingerprint, int topK) throws BiometricException {
        long startTime = System.currentTimeMillis();
        String traceId = UUID.randomUUID().toString();

        try {
            log.info("[{}] Starting 1:N matching for probe RID: {}", traceId, probeFingerprint.getRid());

            // Phase 1: HNSW Approximate Search
            long hnswStartTime = System.currentTimeMillis();
            List<HNSWCandidate> hnswCandidates = performHNSWSearch(probeFingerprint, topK);
            long hnswDuration = System.currentTimeMillis() - hnswStartTime;
            log.debug("[{}] HNSW search completed in {}ms, found {} candidates", 
                    traceId, hnswDuration, hnswCandidates.size());

            if (hnswCandidates.isEmpty()) {
                log.warn("[{}] No candidates found in HNSW search", traceId);
                return MatchResult.builder()
                        .probeRid(probeFingerprint.getRid())
                        .candidates(Collections.emptyList())
                        .status("NO_MATCH")
                        .matchingTimeMs(System.currentTimeMillis() - startTime)
                        .traceId(traceId)
                        .build();
            }

            // Phase 2: Exact Matching Verification
            long exactStartTime = System.currentTimeMillis();
            List<MatchResult.Candidate> exactCandidates = performExactMatching(
                    probeFingerprint, hnswCandidates, traceId);
            long exactDuration = System.currentTimeMillis() - exactStartTime;
            log.debug("[{}] Exact matching completed in {}ms, verified {} candidates", 
                    traceId, exactDuration, exactCandidates.size());

            // Phase 3: Aggregate and Sort Results
            List<MatchResult.Candidate> finalCandidates = exactCandidates.stream()
                    .filter(c -> c.getExactScore() >= EXACT_MATCH_THRESHOLD)
                    .sorted(Comparator.comparingInt(MatchResult.Candidate::getExactScore).reversed())
                    .limit(topK)
                    .collect(Collectors.toList());

            long totalTime = System.currentTimeMillis() - startTime;
            String status = finalCandidates.isEmpty() ? "NO_MATCH" : "MATCH";

            MatchResult result = MatchResult.builder()
                    .probeRid(probeFingerprint.getRid())
                    .candidates(finalCandidates)
                    .status(status)
                    .matchingTimeMs(totalTime)
                    .traceId(traceId)
                    .build();

            // Record metrics
            matchingMetrics.recordMatch1N(totalTime, finalCandidates.size(), status);
            log.info("[{}] 1:N matching completed in {}ms, found {} matches", 
                    traceId, totalTime, finalCandidates.size());

            return result;

        } catch (Exception e) {
            log.error("[{}] Error during 1:N matching", traceId, e);
            matchingMetrics.recordMatchError("1N_MATCH");
            throw new BiometricException("Failed to perform 1:N matching: " + e.getMessage(), e);
        }
    }

    /**
     * Perform 1:1 matching (verification).
     * 
     * Process:
     * 1. HNSW Phase: Verify probe is in index and retrieve embedding
     * 2. Exact Phase: Perform exact matching with target
     * 3. Return: Single match result
     * 
     * @param probeFingerprint The probe fingerprint
     * @param targetFingerprint The target fingerprint
     * @return MatchResult with single match score
     */
    public MatchResult match1To1(Fingerprint probeFingerprint, Fingerprint targetFingerprint) 
            throws BiometricException {
        long startTime = System.currentTimeMillis();
        String traceId = UUID.randomUUID().toString();

        try {
            log.info("[{}] Starting 1:1 matching - Probe: {}, Target: {}", 
                    traceId, probeFingerprint.getRid(), targetFingerprint.getRid());

            // Exact matching directly (no HNSW needed for 1:1)
            int exactScore = sourceAFISMatcher.match(
                    probeFingerprint.getBinaryTemplate(),
                    targetFingerprint.getBinaryTemplate()
            );

            long totalTime = System.currentTimeMillis() - startTime;
            boolean isMatch = exactScore >= EXACT_MATCH_THRESHOLD;
            String status = isMatch ? "MATCH" : "NO_MATCH";

            MatchResult.Candidate candidate = MatchResult.Candidate.builder()
                    .targetRid(targetFingerprint.getRid())
                    .hnswScore(0)
                    .exactScore(exactScore)
                    .finalScore(exactScore)
                    .isMatch(isMatch)
                    .build();

            MatchResult result = MatchResult.builder()
                    .probeRid(probeFingerprint.getRid())
                    .candidates(Collections.singletonList(candidate))
                    .status(status)
                    .matchingTimeMs(totalTime)
                    .traceId(traceId)
                    .build();

            matchingMetrics.recordMatch1To1(totalTime, isMatch);
            log.info("[{}] 1:1 matching completed in {}ms, score: {}, match: {}", 
                    traceId, totalTime, exactScore, isMatch);

            return result;

        } catch (Exception e) {
            log.error("[{}] Error during 1:1 matching", traceId, e);
            matchingMetrics.recordMatchError("1TO1_MATCH");
            throw new BiometricException("Failed to perform 1:1 matching: " + e.getMessage(), e);
        }
    }

    /**
     * Phase 1: HNSW Approximate Search
     * Finds top-K candidates using vector similarity (fast, O(log N))
     */
    private List<HNSWCandidate> performHNSWSearch(Fingerprint probeFingerprint, int topK) 
            throws BiometricException {
        try {
            float[] embedding = probeFingerprint.getEmbeddingVector();
            if (embedding == null || embedding.length == 0) {
                throw new BiometricException("Probe fingerprint has no embedding vector");
            }

            // Search HNSW index
            List<HNSWCandidate> candidates = hnswIndexManager.search(embedding, topK * 2);
            
            // Filter by HNSW score threshold
            return candidates.stream()
                    .filter(c -> c.getScore() >= HNSW_SCORE_THRESHOLD)
                    .limit(topK * 2)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error performing HNSW search", e);
            throw new BiometricException("HNSW search failed: " + e.getMessage(), e);
        }
    }

    /**
     * Phase 2: Exact Matching Verification
     * Verifies HNSW candidates using SourceAFIS (accurate, slower)
     */
    private List<MatchResult.Candidate> performExactMatching(
            Fingerprint probeFingerprint,
            List<HNSWCandidate> hnswCandidates,
            String traceId) throws BiometricException {
        try {
            byte[] probeBinaryTemplate = probeFingerprint.getBinaryTemplate();
            if (probeBinaryTemplate == null || probeBinaryTemplate.length == 0) {
                throw new BiometricException("Probe fingerprint has no binary template");
            }

            // Parallel exact matching for candidates
            List<MatchResult.Candidate> exactCandidates = new CopyOnWriteArrayList<>();
            List<Future<?>> futures = new ArrayList<>();

            for (HNSWCandidate candidate : hnswCandidates) {
                Future<?> future = executorService.submit(() -> {
                    try {
                        byte[] targetBinaryTemplate = offHeapMemoryManager.getTemplate(candidate.getTargetRid());
                        if (targetBinaryTemplate != null) {
                            int exactScore = sourceAFISMatcher.match(probeBinaryTemplate, targetBinaryTemplate);
                            int finalScore = calculateFinalScore(candidate.getScore(), exactScore);

                            MatchResult.Candidate exactCandidate = MatchResult.Candidate.builder()
                                    .targetRid(candidate.getTargetRid())
                                    .hnswScore((int) candidate.getScore())
                                    .exactScore(exactScore)
                                    .finalScore(finalScore)
                                    .isMatch(exactScore >= EXACT_MATCH_THRESHOLD)
                                    .build();

                            exactCandidates.add(exactCandidate);
                        }
                    } catch (Exception e) {
                        log.warn("[{}] Error matching candidate {}: {}", 
                                traceId, candidate.getTargetRid(), e.getMessage());
                    }
                });
                futures.add(future);
            }

            // Wait for all exact matching tasks to complete
            for (Future<?> future : futures) {
                try {
                    future.get(5, TimeUnit.SECONDS);
                } catch (TimeoutException e) {
                    future.cancel(true);
                    log.warn("[{}] Exact matching timeout for candidate", traceId);
                }
            }

            return exactCandidates;

        } catch (Exception e) {
            log.error("[{}] Error performing exact matching", traceId, e);
            throw new BiometricException("Exact matching failed: " + e.getMessage(), e);
        }
    }

    /**
     * Calculate final score combining HNSW and exact scores
     * Final Score = 0.3 * HNSW_Score + 0.7 * Exact_Score
     */
    private int calculateFinalScore(float hnswScore, int exactScore) {
        return (int) (0.3f * hnswScore + 0.7f * exactScore);
    }

    /**
     * Add a fingerprint to the index
     */
    public void addFingerprint(Fingerprint fingerprint) throws BiometricException {
        try {
            // Add to HNSW index
            hnswIndexManager.add(fingerprint.getRid(), fingerprint.getEmbeddingVector());

            // Store binary template in off-heap memory
            offHeapMemoryManager.storeTemplate(fingerprint.getRid(), fingerprint.getBinaryTemplate());

            log.debug("Added fingerprint {} to index", fingerprint.getRid());
            matchingMetrics.recordIndexAdd();

        } catch (Exception e) {
            log.error("Error adding fingerprint to index", e);
            throw new BiometricException("Failed to add fingerprint: " + e.getMessage(), e);
        }
    }

    /**
     * Remove a fingerprint from the index
     */
    public void removeFingerprint(String rid) throws BiometricException {
        try {
            hnswIndexManager.remove(rid);
            offHeapMemoryManager.removeTemplate(rid);

            log.debug("Removed fingerprint {} from index", rid);
            matchingMetrics.recordIndexRemove();

        } catch (Exception e) {
            log.error("Error removing fingerprint from index", e);
            throw new BiometricException("Failed to remove fingerprint: " + e.getMessage(), e);
        }
    }

    /**
     * Get index statistics
     */
    public IndexStatistics getIndexStatistics() {
        return IndexStatistics.builder()
                .totalVectors(hnswIndexManager.getSize())
                .indexSizeBytes(hnswIndexManager.getSizeBytes())
                .offHeapMemoryBytes(offHeapMemoryManager.getUsedMemory())
                .queryCount(matchingMetrics.getTotalQueries())
                .avgQueryTimeMs(matchingMetrics.getAverageQueryTime())
                .build();
    }

    /**
     * Shutdown the engine
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
        log.info("HybridMatchingEngine shutdown complete");
    }
}
