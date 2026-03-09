package com.scalebiometrics.api.service;

import com.scalebiometrics.api.dto.TenantDto;
import com.scalebiometrics.api.entity.Tenant;
import com.scalebiometrics.api.entity.TenantConfig;
import com.scalebiometrics.api.repository.TenantConfigRepository;
import com.scalebiometrics.api.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final TenantConfigRepository tenantConfigRepository;

    @Transactional
    public TenantDto createTenant(TenantDto dto) {
        if (tenantRepository.findByTenantId(dto.getTenantId()).isPresent()) {
            throw new IllegalArgumentException("Tenant ID already exists: " + dto.getTenantId());
        }

        Tenant tenant = Tenant.builder()
                .tenantId(dto.getTenantId())
                .name(dto.getName())
                .description(dto.getDescription())
                .status(Tenant.TenantStatus.PENDING)
                .shardStrategy(dto.getShardStrategy() != null ? dto.getShardStrategy() : "SINGLE")
                .shardCount(dto.getShardCount() != null ? dto.getShardCount() : 1)
                .maxRecords(dto.getMaxRecords() != null ? dto.getMaxRecords() : 1000000)
                .createdBy("API") // In real app, extract from SecurityContext
                .build();

        Tenant saved = tenantRepository.save(tenant);
        log.info("Created new tenant: {}", saved.getTenantId());
        return TenantDto.fromEntity(saved);
    }

    public Page<TenantDto> getAllTenants(Pageable pageable) {
        return tenantRepository.findAll(pageable).map(TenantDto::fromEntity);
    }

    public TenantDto getTenant(String tenantId) {
        return tenantRepository.findByTenantId(tenantId)
                .map(TenantDto::fromEntity)
                .orElseThrow(() -> new RuntimeException("Tenant not found: " + tenantId));
    }

    @Transactional
    public void updateTenantConfig(String tenantId, Map<String, String> configs) {
        // Verify tenant exists
        if (tenantRepository.findByTenantId(tenantId).isEmpty()) {
            throw new RuntimeException("Tenant not found: " + tenantId);
        }

        configs.forEach((key, value) -> {
            TenantConfig config = tenantConfigRepository.findByTenantIdAndConfigKey(tenantId, key)
                    .orElse(TenantConfig.builder()
                            .tenantId(tenantId)
                            .configKey(key)
                            .build());
            
            config.setConfigValue(value);
            tenantConfigRepository.save(config);
        });
        log.info("Updated configuration for tenant: {}", tenantId);
    }

    public List<TenantConfig> getTenantConfig(String tenantId) {
        return tenantConfigRepository.findByTenantId(tenantId);
    }
}
