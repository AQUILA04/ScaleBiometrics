package com.scalebiometrics.starter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalebiometrics.starter.client.ScaleBiometricsClient;
import com.scalebiometrics.starter.webhook.DefaultWebhookHandler;
import com.scalebiometrics.starter.webhook.ScaleBiometricsWebhookHandler;
import com.scalebiometrics.starter.webhook.WebhookController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Configuration
@ConditionalOnClass({RestTemplate.class, ObjectMapper.class})
@EnableConfigurationProperties(ScaleBiometricsProperties.class)
public class ScaleBiometricsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ClientRegistrationRepository clientRegistrationRepository(ScaleBiometricsProperties properties) {
        ClientRegistration registration = ClientRegistration.withRegistrationId("scalebiometrics")
                .clientId(properties.getClientId())
                .clientSecret(properties.getClientSecret())
                .tokenUri(properties.getTokenUri())
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .build();
        return new InMemoryClientRegistrationRepository(registration);
    }

    @Bean
    @ConditionalOnMissingBean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            org.springframework.security.oauth2.client.OAuth2AuthorizedClientService authorizedClientService) {

        OAuth2AuthorizedClientProvider authorizedClientProvider =
                OAuth2AuthorizedClientProviderBuilder.builder()
                        .clientCredentials()
                        .build();

        AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                        clientRegistrationRepository, authorizedClientService);
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        return authorizedClientManager;
    }

    @Bean
    @ConditionalOnMissingBean
    public RestTemplate scaleBiometricsRestTemplate(
            RestTemplateBuilder builder,
            OAuth2AuthorizedClientManager authorizedClientManager) {

        ClientHttpRequestInterceptor interceptor = (request, body, execution) -> {
            OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                    .withClientRegistrationId("scalebiometrics")
                    .principal("scalebiometrics-client")
                    .build();
            
            OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(authorizeRequest);
            if (authorizedClient != null && authorizedClient.getAccessToken() != null) {
                request.getHeaders().setBearerAuth(authorizedClient.getAccessToken().getTokenValue());
            } else {
                log.warn("Could not obtain OAuth2 token for ScaleBiometrics API");
            }
            return execution.execute(request, body);
        };

        return builder
                .additionalInterceptors(interceptor)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public ScaleBiometricsClient scaleBiometricsClient(
            RestTemplate scaleBiometricsRestTemplate,
            ScaleBiometricsProperties properties,
            ObjectMapper objectMapper) {
        log.info("Initializing ScaleBiometricsClient for API URL: {}", properties.getApiUrl());
        return new ScaleBiometricsClient(scaleBiometricsRestTemplate, properties, objectMapper);
    }

    @Configuration
    @ConditionalOnWebApplication
    @ConditionalOnProperty(prefix = "scalebiometrics.webhook", name = "enabled", havingValue = "true", matchIfMissing = true)
    public static class WebhookConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public ScaleBiometricsWebhookHandler defaultWebhookHandler() {
            log.info("No custom ScaleBiometricsWebhookHandler found, using DefaultWebhookHandler");
            return new DefaultWebhookHandler();
        }

        @Bean
        public WebhookController webhookController(ScaleBiometricsWebhookHandler webhookHandler) {
            return new WebhookController(webhookHandler);
        }
    }
}
