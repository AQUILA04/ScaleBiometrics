package com.scalebiometrics.starter.webhook;

import com.scalebiometrics.starter.dto.MatchResultEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * Default fallback implementation that simply logs the incoming webhook.
 * Active only if the client application doesn't provide its own bean.
 */
@Slf4j
public class DefaultWebhookHandler implements ScaleBiometricsWebhookHandler {

    @Override
    public void handleMatchResult(MatchResultEvent result) {
        log.info("Received ScaleBiometrics Webhook - TraceId: {}, Status: {}, Candidates found: {}", 
                result.getTraceId(), 
                result.getStatus(), 
                result.getCandidates() != null ? result.getCandidates().size() : 0);
        
        if (result.getCandidates() != null && !result.getCandidates().isEmpty()) {
            log.debug("Top match score: {}", result.getCandidates().get(0).getScore());
        }
    }
}
