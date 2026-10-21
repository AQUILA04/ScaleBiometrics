package com.scalebiometrics.starter.webhook;

import com.scalebiometrics.starter.dto.MatchResultEvent;

/**
 * Interface for handling incoming webhooks from ScaleBiometrics API.
 * Client applications should implement this interface as a Spring Bean to process results.
 */
public interface ScaleBiometricsWebhookHandler {

    /**
     * Process an incoming match result webhook
     * 
     * @param result The matching result event
     */
    void handleMatchResult(MatchResultEvent result);
}
