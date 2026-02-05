package com.scalebiometrics.api.service;

import com.scalebiometrics.api.dto.AsyncMatchResponseDto;
import com.scalebiometrics.api.dto.MatchRequestDto;
import com.scalebiometrics.api.dto.VerificationRequestDto;
import com.scalebiometrics.api.entity.MatchRequest;
import com.scalebiometrics.api.repository.MatchRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncMatchingService {

    private final MatchRequestRepository matchRequestRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC_MATCHING_REQUESTS = "matching-requests";

    @Transactional
    public AsyncMatchResponseDto submitMatch1N(MatchRequestDto requestDto, String tenantId) {
        String traceId = UUID.randomUUID().toString();
        
        // 1. Persist request
        MatchRequest matchRequest = MatchRequest.builder()
                .tenantId(tenantId)
                .traceId(traceId)
                .status(MatchRequest.RequestStatus.PENDING)
                .requestType(MatchRequest.RequestType.MATCH_1_N)
                .probeRid(requestDto.getProbeRid())
                .build();
        
        matchRequest = matchRequestRepository.save(matchRequest);
        
        // 2. Publish to Kafka
        // We'll need a proper event object, but for now let's assume we send the DTO wrapped with metadata
        // In a real implementation, we should map to a specific Avro/Protobuf/JSON event class
        AsyncMatchRequestEvent event = AsyncMatchRequestEvent.builder()
                .requestId(matchRequest.getId())
                .tenantId(tenantId)
                .traceId(traceId)
                .type("MATCH_1_N")
                .payload(requestDto)
                .build();
                
        kafkaTemplate.send(TOPIC_MATCHING_REQUESTS, tenantId, event);
        
        return AsyncMatchResponseDto.builder()
                .requestId(matchRequest.getId())
                .status("ACCEPTED")
                .message("Request submitted for processing")
                .traceId(traceId)
                .build();
    }

    @Transactional
    public AsyncMatchResponseDto submitMatch1To1(VerificationRequestDto requestDto, String tenantId) {
        String traceId = UUID.randomUUID().toString();
        
        MatchRequest matchRequest = MatchRequest.builder()
                .tenantId(tenantId)
                .traceId(traceId)
                .status(MatchRequest.RequestStatus.PENDING)
                .requestType(MatchRequest.RequestType.MATCH_1_1)
                .probeRid(requestDto.getProbeRid())
                .targetRid(requestDto.getTargetRid())
                .build();
        
        matchRequest = matchRequestRepository.save(matchRequest);
        
        AsyncMatchRequestEvent event = AsyncMatchRequestEvent.builder()
                .requestId(matchRequest.getId())
                .tenantId(tenantId)
                .traceId(traceId)
                .type("MATCH_1_1")
                .payload(requestDto)
                .build();
                
        kafkaTemplate.send(TOPIC_MATCHING_REQUESTS, tenantId, event);
        
        return AsyncMatchResponseDto.builder()
                .requestId(matchRequest.getId())
                .status("ACCEPTED")
                .message("Request submitted for processing")
                .traceId(traceId)
                .build();
    }
    
    @lombok.Data
    @lombok.Builder
    public static class AsyncMatchRequestEvent {
        private UUID requestId;
        private String tenantId;
        private String traceId;
        private String type;
        private Object payload;
    }
}
