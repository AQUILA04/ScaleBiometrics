package com.scalebiometrics.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationRequestDto {

    @NotBlank(message = "Probe RID is required")
    private String probeRid;

    private byte[] probeTemplate;

    @NotBlank(message = "Target RID is required")
    private String targetRid;

    private byte[] targetTemplate;
}
