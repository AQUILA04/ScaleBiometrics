package com.scalebiometrics.master.orchestrator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalebiometrics.core.domain.Fingerprint;
import com.scalebiometrics.core.domain.MatchResult;
import com.scalebiometrics.master.grpc.WorkerClient;
import com.scalebiometrics.proto.matcher.MatchRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchingRequestConsumer {

    private final ScatterGatherOrchestrator orchestrator;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String TOPIC_MATCHING_RESULTS = "matching-results";

    /**
     * Consumes matching requests sequentially (FIFO).
     * The listener blocks until processing is complete.
     */
    @KafkaListener(topics = "matching-requests", groupId = "master-matching-group", concurrency = "1")
    public void consume(AsyncMatchRequestEvent event) {
        log.info("Processing async matching request: {} (Type: {})", event.getRequestId(), event.getType());

        try {
            Object resultPayload = null;
            String error = null;

            if ("MATCH_1_N".equals(event.getType())) {
                resultPayload = processMatch1N(event);
            } else if ("MATCH_1_1".equals(event.getType())) {
                // TODO: Implement 1:1 async processing if needed
                // resultPayload = processMatch1To1(event);
            } else {
                log.warn("Unknown request type: {}", event.getType());
                error = "Unknown request type";
            }

            // Publish result
            MatchResultEvent resultEvent = new MatchResultEvent(event.getRequestId(), resultPayload, error);
            kafkaTemplate.send(TOPIC_MATCHING_RESULTS, resultEvent);
            
            log.info("Completed async matching request: {}", event.getRequestId());

        } catch (Exception e) {
            log.error("Error processing async request {}", event.getRequestId(), e);
            // Publish error result
            MatchResultEvent resultEvent = new MatchResultEvent(event.getRequestId(), null, e.getMessage());
            kafkaTemplate.send(TOPIC_MATCHING_RESULTS, resultEvent);
        }
    }

    private Object processMatch1N(AsyncMatchRequestEvent event) throws Exception {
        // Convert payload map to DTO (since it comes as LinkedHashMap from JSON)
        MatchRequestDto dto = objectMapper.convertValue(event.getPayload(), MatchRequestDto.class);
        
        Fingerprint probe = new Fingerprint();
        probe.setRid(dto.getProbeRid());
        if (dto.getProbeTemplate() != null) {
            probe.setTemplate(dto.getProbeTemplate());
        }
        
        MatchResult result = orchestrator.match1N(probe, dto.getTopK());
        
        // Map domain result to DTO
        return mapToResponseDto(result, event.getTraceId());
    }

    private MatchResponseDto mapToResponseDto(MatchResult result, String traceId) {
        // Simple mapping logic
        return MatchResponseDto.builder()
                .traceId(traceId)
                .status(result.getStatus().name())
                .matchingTimeMs(result.getMatchingTimeMs())
                // .candidates(...) // Map candidates
                .build();
    }

    // DTOs (duplicated here for simplicity, should be in a shared lib)
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AsyncMatchRequestEvent {
        private UUID requestId;
        private String tenantId;
        private String traceId;
        private String type;
        private Object payload;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class MatchResultEvent {
        private UUID requestId;
        private Object result;
        private String error;
    }
    
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class MatchRequestDto {
        private String probeRid;
        private byte[] probeTemplate;
        private int topK;
    }
    
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class MatchResponseDto {
        private String traceId;
        private String status;
        private long matchingTimeMs;
    }
}
