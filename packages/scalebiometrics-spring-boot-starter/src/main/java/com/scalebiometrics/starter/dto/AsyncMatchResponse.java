package com.scalebiometrics.starter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsyncMatchResponse {
    private String requestId;
    private String status;
    private String message;
    private String traceId;
}
