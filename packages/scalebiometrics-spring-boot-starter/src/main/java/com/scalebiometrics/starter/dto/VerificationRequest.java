package com.scalebiometrics.starter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationRequest {
    private String probeRid;
    private byte[] probeTemplate;
    private String targetRid;
    private byte[] targetTemplate;
}
