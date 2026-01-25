package com.scalebiometrics.worker.engine;

import com.scalebiometrics.core.domain.Fingerprint;
import com.scalebiometrics.core.domain.MatchResult;
import com.scalebiometrics.core.exception.BiometricException;
import com.github.jelmerk.hnswlib.hnswlib;
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

            // Phase 2: SourceAFIS Exact Matching (Parallel)
            long exactStartTime = System.currentTimeMillis();
            List<MatchResult.Candidate> exactMatches = performExactMatching(
                    probeFingerprint, hnswCandidates, traceId);
            long exactDuration = System.currentTimeMillis() - exactStartTime;
            log.debug("[{}] Exact matching completed in {}ms, {} candidates matched", 
                    traceId, exactDuration, exactMatches.size());

            // Phase 3: Aggregate and Sort
            List<MatchResult.Candidate> finalResults = exactMatches.stream()
                    .filter(c -> c.getScore() >= EXACT_MATCH_THRESHOLD)
                    .sorted(Comparator.comparingInt(MatchResult.Candidate::getScore).reversed())
                    .limit(topK)
                    .collect(Collectors.toList());

            long totalTime = System.currentTimeMillis() - startTime;
            matchingMetrics.recordMatching1N(totalTime);
            
            log.info("[{}] 1:N matching completed in {}ms with {} results", 
                    traceId, totalTime, finalResults.size());

            return MatchResult.builder()
                    .probeRid(probeFingerprint.getRid())
                    .candidates(finalResults)
                    .status(finalResults.isEmpty() ? "NO_MATCH" : "MATCH")
                    .matchingTimeMs(totalTime)
                    .traceId(traceId)
                    .hnswPhaseTimeMs(hnswDuration)
                    .exactPhaseTimeMs(exactDuration)
                    .build();

        } catch (Exception e) {
            log.error("[{}] Error during 1:N matching", traceId, e);
            matchingMetrics.recordMatchingError();
            throw new BiometricException("1:N matching failed: " + e.getMessage(), e);
        }
    }

    /**
     * Perform 1:1 verification (authentication).
     * 
     * @param probeFingerprint The probe fingerprint
     * @param targetRid The target record ID to verify against
     * @return MatchResult with verification score
     */
    public MatchResult match1To1(Fingerprint probeFingerprint, String targetRid) throws BiometricException {
        long startTime = System.currentTimeMillis();
        String traceId = UUID.randomUUID().toString();

        try {
            log.info("[{}] Starting 1:1 verification for probe RID: {} vs target RID: {}", 
                    traceId, probeFingerprint.getRid(), targetRid);

            // Retrieve target fingerprint
            Fingerprint targetFingerprint = hnswIndexManager.getFingerprint(targetRid);
            if (targetFingerprint == null) {
                log.warn("[{}] Target fingerprint not found: {}", traceId, targetRid);
                return MatchResult.builder()
                        .probeRid(probeFingerprint.getRid())
                        .candidates(Collections.emptyList())
                        .status("TARGET_NOT_FOUND")
                        .matchingTimeMs(System.currentTimeMillis() - startTime)
                        .traceId(traceId)
                        .build();
            }

            // Perform exact matching
            int score = sourceAFISMatcher.match(probeFingerprint, targetFingerprint);
            long totalTime = System.currentTimeMillis() - startTime;
            matchingMetrics.recordMatching1To1(totalTime);

            log.info("[{}] 1:1 verification completed in {}ms with score: {}", 
                    traceId, totalTime, score);

            return MatchResult.builder()
                    .probeRid(probeFingerprint.getRid())
                    .candidates(Collections.singletonList(
                            MatchResult.Candidate.builder()
                                    .targetRid(targetRid)
                                    .score(score)
                                    .build()
                    ))
                    .status(score >= EXACT_MATCH_THRESHOLD ? "MATCH" : "NO_MATCH")
                    .matchingTimeMs(totalTime)
                    .traceId(traceId)
                    .build();

        } catch (Exception e) {
            log.error("[{}] Error during 1:1 verification", traceId, e);
            matchingMetrics.recordMatchingError();
            throw new BiometricException("1:1 verification failed: " + e.getMessage(), e);
        }
    }

    /**
     * Add fingerprint to HNSW index.
     */
    public void addFingerprint(Fingerprint fingerprint) throws BiometricException {
        try {
            hnswIndexManager.addFingerprint(fingerprint);
            matchingMetrics.recordFingerprintAdded();
            log.debug("Fingerprint added to index: {}", fingerprint.getRid());
        } catch (Exception e) {
            log.error("Error adding fingerprint to index", e);
            throw new BiometricException("Failed to add fingerprint: " + e.getMessage(), e);
        }
    }

    /**
     * Remove fingerprint from HNSW index.
     */
    public void removeFingerprint(String rid) throws BiometricException {
        try {
            hnswIndexManager.removeFingerprint(rid);
            matchingMetrics.recordFingerprintRemoved();
            log.debug("Fingerprint removed from index: {}", rid);
        } catch (Exception e) {
            log.error("Error removing fingerprint from index", e);
            throw new BiometricException("Failed to remove fingerprint: " + e.getMessage(), e);
        }
    }

    /**
     * Phase 1: HNSW Approximate Search
     * Uses hnswlib-core for fast approximate nearest neighbor search.
     */
    private List<HNSWCandidate> performHNSWSearch(Fingerprint probeFingerprint, int topK) 
            throws BiometricException {
        try {
            // Get embedding vector from fingerprint
            float[] embedding = probeFingerprint.getEmbedding();
            if (embedding == null || embedding.length == 0) {
                log.warn("No embedding found for probe fingerprint: {}", probeFingerprint.getRid());
                return Collections.emptyList();
            }

            // Search in HNSW index
            List<HNSWCandidate> candidates = hnswIndexManager.search(embedding, topK);
            
            // Filter by score threshold
            return candidates.stream()
                    .filter(c -> c.getScore() >= HNSW_SCORE_THRESHOLD)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error during HNSW search", e);
            throw new BiometricException("HNSW search failed: " + e.getMessage(), e);
        }
    }

    /**
     * Phase 2: SourceAFIS Exact Matching
     * Performs parallel exact matching on HNSW candidates.
     */
    private List<MatchResult.Candidate> performExactMatching(
            Fingerprint probeFingerprint,
            List<HNSWCandidate> hnswCandidates,
            String traceId) throws BiometricException {
        
        try {
            List<Future<MatchResult.Candidate>> futures = new ArrayList<>();

            for (HNSWCandidate candidate : hnswCandidates) {
                futures.add(executorService.submit(() -> {
                    try {
                        Fingerprint targetFingerprint = hnswIndexManager.getFingerprint(candidate.getRid());
                        if (targetFingerprint == null) {
                            return null;
                        }

                        int score = sourceAFISMatcher.match(probeFingerprint, targetFingerprint);
                        return MatchResult.Candidate.builder()
                                .targetRid(candidate.getRid())
                                .score(score)
                                .build();
                    } catch (Exception e) {
                        log.warn("[{}] Error matching candidate: {}", traceId, candidate.getRid(), e);
                        return null;
                    }
                }));
            }

            // Collect results
            List<MatchResult.Candidate> results = new ArrayList<>();
            for (Future<MatchResult.Candidate> future : futures) {
                try {
                    MatchResult.Candidate candidate = future.get(5, TimeUnit.SECONDS);
                    if (candidate != null) {
                        results.add(candidate);
                    }
                } catch (TimeoutException e) {
                    log.warn("[{}] Timeout waiting for exact matching result", traceId);
                    future.cancel(true);
                }
            }

            return results;

        } catch (Exception e) {
            log.error("Error during exact matching", e);
            throw new BiometricException("Exact matching failed: " + e.getMessage(), e);
        }
    }

    /**
     * Get index statistics.
     */
    public IndexStatistics getIndexStatistics() {
        return hnswIndexManager.getStatistics();
    }

    /**
     * Get memory statistics.
     */
    public MemoryStatistics getMemoryStatistics() {
        return offHeapMemoryManager.getStatistics();
    }

    /**
     * Shutdown the engine.
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
