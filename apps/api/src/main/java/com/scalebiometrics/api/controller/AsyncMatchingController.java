package com.scalebiometrics.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalebiometrics.api.dto.AsyncMatchResponseDto;
import com.scalebiometrics.api.dto.MatchRequestDto;
import com.scalebiometrics.api.dto.VerificationRequestDto;
import com.scalebiometrics.api.service.AsyncMatchingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/matching/async")
@RequiredArgsConstructor
public class AsyncMatchingController {

    private final AsyncMatchingService asyncMatchingService;
    private final ObjectMapper objectMapper;

    /**
     * Async 1:N Matching (Identification/Deduplication)
     * Accepts multipart/form-data with:
     * - 'request': JSON metadata (MatchRequestDto)
     * - 'fingerprints': List of files (images or templates)
     */
    @PostMapping(value = "/1n", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AsyncMatchResponseDto> match1N(
            @RequestPart("request") String requestJson,
            @RequestPart("fingerprints") List<MultipartFile> fingerprints,
            @AuthenticationPrincipal Jwt jwt) throws IOException {
        
        String tenantId = jwt.getClaimAsString("tenant_id");
        MatchRequestDto requestDto = objectMapper.readValue(requestJson, MatchRequestDto.class);
        
        log.info("Received async 1:N matching request from tenant: {} for probeRid: {} with {} fingerprints", 
                tenantId, requestDto.getProbeRid(), fingerprints.size());
        
        AsyncMatchResponseDto response = asyncMatchingService.submitMatch1N(requestDto, fingerprints, tenantId);
        return ResponseEntity.accepted().body(response);
    }

    @PostMapping("/1to1")
    public ResponseEntity<AsyncMatchResponseDto> match1To1(
            @RequestBody VerificationRequestDto requestDto,
            @AuthenticationPrincipal Jwt jwt) {
        
        String tenantId = jwt.getClaimAsString("tenant_id");
        log.info("Received async 1:1 matching request from tenant: {}", tenantId);
        
        AsyncMatchResponseDto response = asyncMatchingService.submitMatch1To1(requestDto, tenantId);
        return ResponseEntity.accepted().body(response);
    }
}
