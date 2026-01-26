package com.scalebiometrics.worker.engine;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Index Statistics - Metrics for HNSW index
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndexStatistics {
    private long totalVectors;
    private long indexSizeBytes;
    private long offHeapMemoryBytes;
    private long queryCount;
    private double avgQueryTimeMs;
    private long maxSize;
    private int m;
    private int efConstruction;
    private int efSearch;
    private long createdAt;

    // Methods for updating statistics
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public void incrementTotalVectors() {
        this.totalVectors++;
    }

    public void decrementTotalVectors() {
        if (this.totalVectors > 0) {
            this.totalVectors--;
        }
    }

    public void updateIndexSizeBytes(int sizeBytes) {
        this.indexSizeBytes += sizeBytes;
    }

    public void recordQueryTime(long durationMs) {
        if (queryCount == 0) {
            avgQueryTimeMs = durationMs;
        } else {
            avgQueryTimeMs = (avgQueryTimeMs * queryCount + durationMs) / (queryCount + 1);
        }
    }

    public void incrementQueryCount() {
        this.queryCount++;
    }
}
