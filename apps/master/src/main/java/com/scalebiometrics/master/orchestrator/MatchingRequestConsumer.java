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

import java.util.List;
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
        List<byte[]> templates = event.getTemplates();
        if (templates == null || templates.isEmpty()) {
            throw new IllegalArgumentException("No templates provided for matching");
        }

        log.info("Processing {} templates for probeRid: {}", templates.size(), event.getProbeRid());

        MatchResult finalResult = null;

        // Stop-on-Match Logic
        for (int i = 0; i < templates.size(); i++) {
            byte[] template = templates.get(i);
            
            Fingerprint probe = new Fingerprint();
            probe.setRid(event.getProbeRid());
            probe.setTemplate(template);
            
            MatchResult result = orchestrator.match1N(probe, event.getTopK());
            
            if (result.getStatus() == MatchResult.MatchStatus.MATCH_FOUND) {
                log.info("Match FOUND for finger index {}. Stopping search.", i);
                finalResult = result;
                break; // Stop processing other fingers
            }
            
            // Keep the last result if no match found yet
            finalResult = result;
        }

        if (finalResult != null && finalResult.getStatus() == MatchResult.MatchStatus.NO_MATCH) {
            log.info("No match found for any of the {} fingerprints. Proceeding to enrollment (if configured).", templates.size());
            // TODO: Trigger enrollment logic here (distribute templates to workers)
            // For now, we just return NO_MATCH
        }
        
        // Map domain result to DTO
        return mapToResponseDto(finalResult, event.getTraceId());
    }

    private MatchResponseDto mapToResponseDto(MatchResult result, String traceId) {
        if (result == null) return null;
        
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
        private String probeRid;
        private List<byte[]> templates;
        private int topK;
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
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class MatchResponseDto {
        private String traceId;
        private String status;
        private long matchingTimeMs;
    }
}
