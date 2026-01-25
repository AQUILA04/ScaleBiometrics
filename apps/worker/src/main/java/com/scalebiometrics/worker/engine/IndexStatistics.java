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
}
