package com.scalebiometrics.api.dto;

import com.scalebiometrics.api.entity.Tenant;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantDto {
    private UUID id;
    
    @NotBlank(message = "Tenant ID is required")
    private String tenantId;
    
    @NotBlank(message = "Name is required")
    private String name;
    
    private String description;
    private String status;
    private String shardStrategy;
    private Integer shardCount;
    private Integer maxRecords;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static TenantDto fromEntity(Tenant tenant) {
        return TenantDto.builder()
                .id(tenant.getId())
                .tenantId(tenant.getTenantId())
                .name(tenant.getName())
                .description(tenant.getDescription())
                .status(tenant.getStatus().name())
                .shardStrategy(tenant.getShardStrategy())
                .shardCount(tenant.getShardCount())
                .maxRecords(tenant.getMaxRecords())
                .createdAt(tenant.getCreatedAt())
                .updatedAt(tenant.getUpdatedAt())
                .build();
    }
}
