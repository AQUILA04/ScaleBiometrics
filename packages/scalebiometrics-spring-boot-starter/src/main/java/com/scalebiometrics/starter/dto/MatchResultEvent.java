package com.scalebiometrics.starter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchResultEvent {
    private String traceId;
    private String status;
    private List<Candidate> candidates;
    private long matchingTimeMs;
    private String errorMessage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Candidate {
        private String targetRid;
        private int score;
        private boolean match;
    }
}
