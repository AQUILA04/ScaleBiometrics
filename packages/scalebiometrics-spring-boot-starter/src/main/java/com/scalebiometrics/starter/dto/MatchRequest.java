package com.scalebiometrics.starter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchRequest {
    private String probeRid;
    private int topK;
    private float threshold;
    private String metadata;
}
