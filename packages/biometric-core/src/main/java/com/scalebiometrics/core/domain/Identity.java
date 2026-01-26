package com.scalebiometrics.core.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Core domain model for a registered identity.
 * Represents a unique individual in the biometric database.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Identity {
    private UUID id;
    private String tenantId;
    private String rid; // Record ID (unique within tenant)
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private IdentityStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String metadata; // JSONB field for flexible metadata

    public enum IdentityStatus {
        ACTIVE,
        INACTIVE,
        SUSPENDED,
        DELETED
    }
}
