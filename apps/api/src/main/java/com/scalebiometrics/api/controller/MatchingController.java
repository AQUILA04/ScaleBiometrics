package com.scalebiometrics.api.controller;

import com.scalebiometrics.api.dto.MatchRequestDto;
import com.scalebiometrics.api.dto.MatchResponseDto;
import com.scalebiometrics.api.dto.VerificationRequestDto;
import com.scalebiometrics.api.dto.VerificationResponseDto;
import com.scalebiometrics.api.service.MatchingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/matching")
@RequiredArgsConstructor
public class MatchingController {

    private final MatchingService matchingService;

    @PostMapping("/1n")
    public ResponseEntity<MatchResponseDto> match1N(@Valid @RequestBody MatchRequestDto request) {
        return ResponseEntity.ok(matchingService.match1N(request));
    }

    @PostMapping("/1to1")
    public ResponseEntity<VerificationResponseDto> match1To1(@Valid @RequestBody VerificationRequestDto request) {
        return ResponseEntity.ok(matchingService.match1To1(request));
    }
}
