package com.scalebiometrics.worker.engine;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * HNSW Candidate - Represents a candidate from HNSW search
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HNSWCandidate {
    private String targetRid;
    private float score;
    private float distance;

    // Alias methods for backward compatibility
    public String getRid() {
        return targetRid;
    }

    public void setRid(String rid) {
        this.targetRid = rid;
    }
}
