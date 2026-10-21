package com.scalebiometrics.starter.webhook;

import com.scalebiometrics.starter.dto.MatchResultEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class WebhookController {

    private final ScaleBiometricsWebhookHandler webhookHandler;

    @PostMapping("${scalebiometrics.webhook.path:/api/webhooks/scalebiometrics}")
    public ResponseEntity<Void> receiveWebhook(@RequestBody MatchResultEvent event) {
        log.debug("Webhook endpoint hit for TraceId: {}", event.getTraceId());
        try {
            webhookHandler.handleMatchResult(event);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error processing webhook for TraceId: {}", event.getTraceId(), e);
            // Return 200 anyway so ScaleBiometrics doesn't retry unnecessarily for app-specific errors,
            // or 500 if you want retries. Usually, 200 is safer for async processing.
            return ResponseEntity.ok().build();
        }
    }
}
