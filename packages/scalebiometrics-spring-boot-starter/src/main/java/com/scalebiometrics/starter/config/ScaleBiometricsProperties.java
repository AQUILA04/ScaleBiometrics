package com.scalebiometrics.starter.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "scalebiometrics")
public class ScaleBiometricsProperties {

    /**
     * Base URL of the ScaleBiometrics API (e.g., https://api.scalebiometrics.com)
     */
    private String apiUrl;

    /**
     * Tenant ID for identifying the client
     */
    private String tenantId;

    /**
     * Client ID for OAuth2 authentication
     */
    private String clientId;

    /**
     * Client Secret for OAuth2 authentication
     */
    private String clientSecret;

    /**
     * OAuth2 Token URI (e.g., https://auth.scalebiometrics.com/realms/scalebiometrics/protocol/openid-connect/token)
     */
    private String tokenUri;

    /**
     * Webhook configuration
     */
    private Webhook webhook = new Webhook();

    @Data
    public static class Webhook {
        /**
         * Enable the default webhook endpoint
         */
        private boolean enabled = true;

        /**
         * The path where the webhook will be exposed
         */
        private String path = "/api/webhooks/scalebiometrics";
    }
}
