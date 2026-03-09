package com.scalebiometrics.api.repository;

import com.scalebiometrics.api.entity.TenantConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantConfigRepository extends JpaRepository<TenantConfig, UUID> {
    List<TenantConfig> findByTenantId(String tenantId);
    Optional<TenantConfig> findByTenantIdAndConfigKey(String tenantId, String configKey);
}
