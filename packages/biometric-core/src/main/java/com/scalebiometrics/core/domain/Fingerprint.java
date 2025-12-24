package com.scalebiometrics.core.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Biometric fingerprint template and metadata.
 * Stores both the binary template and embedding vector for matching.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fingerprint {
    private UUID id;
    private String tenantId;
    private String rid; // Reference to Identity
    private FingerIndex fingerIndex;
    private String imageUrl; // MinIO reference
    private byte[] binaryTemplate; // SourceAFIS template
    private float[] embeddingVector; // HNSW embedding (e.g., 512-dim)
    private int quality; // 0-100
    private FingerprintStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum FingerIndex {
        RIGHT_THUMB(0),
        RIGHT_INDEX(1),
        RIGHT_MIDDLE(2),
        RIGHT_RING(3),
        RIGHT_PINKY(4),
        LEFT_THUMB(5),
        LEFT_INDEX(6),
        LEFT_MIDDLE(7),
        LEFT_RING(8),
        LEFT_PINKY(9);

        public final int value;

        FingerIndex(int value) {
            this.value = value;
        }
    }

    public enum FingerprintStatus {
        ACTIVE,
        ARCHIVED,
        DELETED
    }
}
