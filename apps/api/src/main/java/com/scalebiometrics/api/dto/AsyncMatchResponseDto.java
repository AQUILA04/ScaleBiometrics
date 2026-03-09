package com.scalebiometrics.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsyncMatchResponseDto {
    private UUID requestId;
    private String status;
    private String message;
    private String traceId;
}
