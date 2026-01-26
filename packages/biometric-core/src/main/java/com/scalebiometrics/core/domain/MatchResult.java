package com.scalebiometrics.core.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Result of a biometric matching operation (1:N deduplication or 1:1 verification).
 * Stores match candidates with scores and decision.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchResult {
    private UUID id;
    private String tenantId;
    private String probeRid; // Probe fingerprint RID
    private MatchStatus status;
    private List<Candidate> candidates;
    private long matchingTimeMs;
    private String traceId;
    private LocalDateTime createdAt;
    private long hnswPhaseTimeMs;
    private long exactPhaseTimeMs;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Candidate {
        private String targetRid;
        private int hnnScore; // ANN pre-filtering score (0-100)
        private int exactScore; // SourceAFIS exact match score (0-100)
        private int finalScore; // Combined score
        private int score; // Overall score (used for 1:1 verification)
        private boolean isMatch;
    }

    public enum MatchStatus {
        MATCH_FOUND,
        NO_MATCH,
        AMBIGUOUS,
        ERROR
    }

    // Helper methods for backward compatibility
    public boolean isMatch() {
        return status == MatchStatus.MATCH_FOUND;
    }

    public int getScore() {
        if (candidates != null && !candidates.isEmpty()) {
            return candidates.get(0).getScore();
        }
        return 0;
    }
}
