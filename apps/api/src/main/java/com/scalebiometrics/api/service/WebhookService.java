package com.scalebiometrics.api.service;

import com.scalebiometrics.api.entity.Tenant;
import com.scalebiometrics.api.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final TenantRepository tenantRepository;
    private final WebClient.Builder webClientBuilder;

    public void sendWebhook(String tenantId, Object payload) {
        Tenant tenant = tenantRepository.findByTenantId(tenantId)
                .orElse(null);

        if (tenant == null || tenant.getCallbackUrl() == null || tenant.getCallbackUrl().isEmpty()) {
            log.warn("No callback URL configured for tenant {}", tenantId);
            return;
        }

        log.info("Sending webhook to {} for tenant {}", tenant.getCallbackUrl(), tenantId);

        webClientBuilder.build()
                .post()
                .uri(tenant.getCallbackUrl())
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(5))
                .retry(3)
                .doOnSuccess(response -> log.info("Webhook sent successfully to tenant {}", tenantId))
                .doOnError(error -> log.error("Failed to send webhook to tenant {}", tenantId, error))
                .subscribe();
    }
}
