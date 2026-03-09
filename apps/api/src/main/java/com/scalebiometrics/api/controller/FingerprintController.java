package com.scalebiometrics.api.controller;

import com.scalebiometrics.api.service.FingerprintService;
import com.scalebiometrics.core.domain.Fingerprint;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/fingerprints")
@RequiredArgsConstructor
public class FingerprintController {

    private final FingerprintService fingerprintService;

    @PostMapping
    public ResponseEntity<Fingerprint> uploadFingerprint(
            @RequestParam("rid") String rid,
            @RequestParam("fingerIndex") Fingerprint.FingerIndex fingerIndex,
            @RequestParam("file") MultipartFile file) {
        
        Fingerprint fingerprint = fingerprintService.uploadFingerprint(rid, fingerIndex, file);
        return ResponseEntity.ok(fingerprint);
    }
}
