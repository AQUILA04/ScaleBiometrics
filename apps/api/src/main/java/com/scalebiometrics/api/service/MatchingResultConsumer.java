package com.scalebiometrics.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalebiometrics.api.dto.MatchResponseDto;
import com.scalebiometrics.api.dto.VerificationResponseDto;
import com.scalebiometrics.api.entity.MatchRequest;
import com.scalebiometrics.api.repository.MatchRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingResultConsumer {

    private final MatchRequestRepository matchRequestRepository;
    private final WebhookService webhookService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "matching-results", groupId = "api-result-consumer")
    @Transactional
    public void consumeResult(MatchResultEvent event) {
        log.info("Received matching result for request {}", event.getRequestId());

        MatchRequest request = matchRequestRepository.findById(event.getRequestId())
                .orElse(null);

        if (request == null) {
            log.error("Match request {} not found", event.getRequestId());
            return;
        }

        // Update status
        request.setStatus(MatchRequest.RequestStatus.COMPLETED);
        request.setCompletedAt(LocalDateTime.now());
        
        if (event.getError() != null) {
            request.setStatus(MatchRequest.RequestStatus.FAILED);
            request.setErrorMessage(event.getError());
        }
        
        matchRequestRepository.save(request);

        // Send Webhook
        webhookService.sendWebhook(request.getTenantId(), event.getResult());
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class MatchResultEvent {
        private UUID requestId;
        private Object result; // MatchResponseDto or VerificationResponseDto
        private String error;
    }
}
