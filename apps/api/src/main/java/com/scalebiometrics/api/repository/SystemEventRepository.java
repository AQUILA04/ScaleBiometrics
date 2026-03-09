package com.scalebiometrics.api.repository;

import com.scalebiometrics.api.entity.SystemEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SystemEventRepository extends JpaRepository<SystemEvent, UUID> {
    Page<SystemEvent> findBySeverity(String severity, Pageable pageable);
    Page<SystemEvent> findByTenantId(String tenantId, Pageable pageable);
}
