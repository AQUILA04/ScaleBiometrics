package com.scalebiometrics.api.controller;

import com.scalebiometrics.api.dto.TenantDto;
import com.scalebiometrics.api.entity.TenantConfig;
import com.scalebiometrics.api.service.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @PostMapping
    public ResponseEntity<TenantDto> createTenant(@Valid @RequestBody TenantDto tenantDto) {
        return ResponseEntity.ok(tenantService.createTenant(tenantDto));
    }

    @GetMapping
    public ResponseEntity<Page<TenantDto>> getAllTenants(Pageable pageable) {
        return ResponseEntity.ok(tenantService.getAllTenants(pageable));
    }

    @GetMapping("/{tenantId}")
    public ResponseEntity<TenantDto> getTenant(@PathVariable String tenantId) {
        return ResponseEntity.ok(tenantService.getTenant(tenantId));
    }

    @GetMapping("/{tenantId}/config")
    public ResponseEntity<List<TenantConfig>> getTenantConfig(@PathVariable String tenantId) {
        return ResponseEntity.ok(tenantService.getTenantConfig(tenantId));
    }

    @PutMapping("/{tenantId}/config")
    public ResponseEntity<Void> updateTenantConfig(
            @PathVariable String tenantId,
            @RequestBody Map<String, String> configs) {
        tenantService.updateTenantConfig(tenantId, configs);
        return ResponseEntity.ok().build();
    }
}
