package com.scalebiometrics.api.service;

import com.google.protobuf.ByteString;
import com.scalebiometrics.api.dto.*;
import com.scalebiometrics.proto.matcher.*;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingService {

    private final MatcherServiceGrpc.MatcherServiceBlockingStub matcherStub;

    public MatchResponseDto match1N(MatchRequestDto requestDto) {
        String traceId = UUID.randomUUID().toString();
        log.info("Initiating 1:N match. TraceID: {}, ProbeRID: {}", traceId, requestDto.getProbeRid());

        MatchRequest.Builder requestBuilder = MatchRequest.newBuilder()
                .setTraceId(traceId)
                .setProbeRid(requestDto.getProbeRid())
                .setTopK(requestDto.getTopK())
                .setThreshold(requestDto.getThreshold())
                .setTimeoutMs(2000); // Default timeout

        if (requestDto.getProbeTemplate() != null) {
            requestBuilder.setProbeTemplate(ByteString.copyFrom(requestDto.getProbeTemplate()));
        }
        
        // If embedding is provided, add it
        if (requestDto.getProbeEmbedding() != null) {
            for (float f : requestDto.getProbeEmbedding()) {
                requestBuilder.addProbeEmbedding(f);
            }
        }

        try {
            MatchResponse response = matcherStub.match1N(requestBuilder.build());
            
            return MatchResponseDto.builder()
                    .traceId(response.getTraceId())
                    .status(response.getStatus().name())
                    .matchingTimeMs(response.getMatchingTimeMs())
                    .errorMessage(response.getErrorMessage())
                    .candidates(response.getCandidatesList().stream()
                            .map(c -> MatchResponseDto.CandidateDto.builder()
                                    .targetRid(c.getTargetRid())
                                    .score(c.getFinalScore())
                                    .match(c.getIsMatch())
                                    .build())
                            .collect(Collectors.toList()))
                    .build();

        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed for 1:N match", e);
            throw new RuntimeException("Matching service unavailable: " + e.getMessage());
        }
    }

    public VerificationResponseDto match1To1(VerificationRequestDto requestDto) {
        String traceId = UUID.randomUUID().toString();
        log.info("Initiating 1:1 match. TraceID: {}, ProbeRID: {}, TargetRID: {}", 
                traceId, requestDto.getProbeRid(), requestDto.getTargetRid());

        VerificationRequest.Builder requestBuilder = VerificationRequest.newBuilder()
                .setTraceId(traceId)
                .setProbeRid(requestDto.getProbeRid())
                .setTargetRid(requestDto.getTargetRid())
                .setTimeoutMs(2000);

        if (requestDto.getProbeTemplate() != null) {
            requestBuilder.setProbeTemplate(ByteString.copyFrom(requestDto.getProbeTemplate()));
        }
        if (requestDto.getTargetTemplate() != null) {
            requestBuilder.setTargetTemplate(ByteString.copyFrom(requestDto.getTargetTemplate()));
        }

        try {
            VerificationResponse response = matcherStub.match1To1(requestBuilder.build());

            return VerificationResponseDto.builder()
                    .traceId(response.getTraceId())
                    .match(response.getIsMatch())
                    .score(response.getScore())
                    .matchingTimeMs(response.getMatchingTimeMs())
                    .errorMessage(response.getErrorMessage())
                    .build();

        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed for 1:1 match", e);
            throw new RuntimeException("Matching service unavailable: " + e.getMessage());
        }
    }
}
