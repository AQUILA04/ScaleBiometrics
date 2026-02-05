package com.scalebiometrics.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchResponseDto {
    private String traceId;
    private String status;
    private List<CandidateDto> candidates;
    private long matchingTimeMs;
    private String errorMessage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CandidateDto {
        private String targetRid;
        private int score;
        private boolean match;
    }
}
