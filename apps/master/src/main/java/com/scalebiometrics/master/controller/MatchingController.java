package com.scalebiometrics.master.controller;

import com.scalebiometrics.core.domain.Fingerprint;
import com.scalebiometrics.core.domain.MatchResult;
import com.scalebiometrics.core.exception.BiometricException;
import com.scalebiometrics.master.orchestrator.ScatterGatherOrchestrator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Matching Controller - REST API for matching operations
 */
@Slf4j
@RestController
@RequestMapping("/api/matching")
public class MatchingController {

    private final ScatterGatherOrchestrator orchestrator;

    public MatchingController(ScatterGatherOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    /**
     * Perform 1:N matching (search/deduplication)
     * 
     * POST /api/matching/1n
     * {
     *   "probeFingerprint": { ... },
     *   "topK": 10
     * }
     */
    @PostMapping("/1n")
    public ResponseEntity<?> match1N(@RequestBody Match1NRequest request) {
        try {
            log.info("Received 1:N matching request for probe RID: {}", request.getProbeRid());

            Fingerprint probeFingerprint = request.getProbeFingerprint();
            int topK = request.getTopK() > 0 ? request.getTopK() : 10;

            MatchResult result = orchestrator.match1NDistributed(probeFingerprint, topK);

            return ResponseEntity.ok(result);

        } catch (BiometricException e) {
            log.error("Biometric error during 1:N matching", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("BIOMETRIC_ERROR", e.getMessage()));
        } catch (Exception e) {
            log.error("Error during 1:N matching", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
        }
    }

    /**
     * Perform 1:1 matching (verification)
     * 
     * POST /api/matching/1to1
     * {
     *   "probeFingerprint": { ... },
     *   "targetFingerprint": { ... }
     * }
     */
    @PostMapping("/1to1")
    public ResponseEntity<?> match1To1(@RequestBody Match1To1Request request) {
        try {
            log.info("Received 1:1 matching request - Probe: {}, Target: {}", 
                    request.getProbeRid(), request.getTargetRid());

            // For 1:1, we can route to a specific worker based on target RID
            // For now, just use scatter-gather with topK=1
            Fingerprint probeFingerprint = request.getProbeFingerprint();
            MatchResult result = orchestrator.match1NDistributed(probeFingerprint, 1);

            return ResponseEntity.ok(result);

        } catch (BiometricException e) {
            log.error("Biometric error during 1:1 matching", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("BIOMETRIC_ERROR", e.getMessage()));
        } catch (Exception e) {
            log.error("Error during 1:1 matching", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
        }
    }

    /**
     * Get system health
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(new HealthResponse("healthy", "Master orchestrator is operational"));
    }

    /**
     * Request DTOs
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class Match1NRequest {
        private String probeRid;
        private Fingerprint probeFingerprint;
        private int topK;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class Match1To1Request {
        private String probeRid;
        private String targetRid;
        private Fingerprint probeFingerprint;
        private Fingerprint targetFingerprint;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ErrorResponse {
        private String code;
        private String message;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class HealthResponse {
        private String status;
        private String message;
    }
}
