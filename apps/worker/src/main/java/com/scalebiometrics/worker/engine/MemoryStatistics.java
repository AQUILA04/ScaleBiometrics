package com.scalebiometrics.worker.engine;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Memory Statistics - Metrics for off-heap memory
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryStatistics {
    private long totalAllocatedBytes;
    private long maxAllocatedBytes;
    private double usagePercentage;
    private long templateCount;
    private long averageTemplateSize;
}
