package com.scalebiometrics.worker.engine;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Matching Metrics - Tracks performance metrics for matching operations
 */
@Slf4j
@Component
public class MatchingMetrics {

    private final MeterRegistry meterRegistry;
    private final AtomicLong totalQueries = new AtomicLong(0);
    private final AtomicLong totalTime = new AtomicLong(0);
    private final AtomicLong totalMatches = new AtomicLong(0);
    private final AtomicLong totalErrors = new AtomicLong(0);
    private final long startTime = System.currentTimeMillis();

    private Counter match1NCounter;
    private Counter match1To1Counter;
    private Counter matchErrorCounter;
    private Counter indexAddCounter;
    private Counter indexRemoveCounter;
    private Timer matchingLatencyTimer;

    public MatchingMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        initializeMetrics();
    }

    private void initializeMetrics() {
        match1NCounter = Counter.builder("matching.1n.total")
                .description("Total 1:N matching operations")
                .register(meterRegistry);

        match1To1Counter = Counter.builder("matching.1to1.total")
                .description("Total 1:1 matching operations")
                .register(meterRegistry);

        matchErrorCounter = Counter.builder("matching.errors.total")
                .description("Total matching errors")
                .register(meterRegistry);

        indexAddCounter = Counter.builder("index.add.total")
                .description("Total fingerprints added to index")
                .register(meterRegistry);

        indexRemoveCounter = Counter.builder("index.remove.total")
                .description("Total fingerprints removed from index")
                .register(meterRegistry);

        matchingLatencyTimer = Timer.builder("matching.latency")
                .description("Matching operation latency")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
    }

    public void recordMatching1N(long durationMs) {
        match1NCounter.increment();
        matchingLatencyTimer.record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        totalQueries.incrementAndGet();
        totalTime.addAndGet(durationMs);
        totalMatches.incrementAndGet();
    }

    public void recordMatching1To1(long durationMs) {
        match1To1Counter.increment();
        matchingLatencyTimer.record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        totalQueries.incrementAndGet();
        totalTime.addAndGet(durationMs);
        totalMatches.incrementAndGet();
    }

    public void recordMatchingError() {
        matchErrorCounter.increment();
        totalErrors.incrementAndGet();
    }

    public void recordFingerprintAdded() {
        indexAddCounter.increment();
    }

    public void recordFingerprintRemoved() {
        indexRemoveCounter.increment();
    }

    public long getTotalMatches() {
        return totalMatches.get();
    }

    public long getTotalErrors() {
        return totalErrors.get();
    }

    public double getAverageLatency() {
        long queries = totalQueries.get();
        if (queries == 0) {
            return 0;
        }
        return (double) totalTime.get() / queries;
    }

    public double getP95Latency() {
        // This would be obtained from the Timer metrics
        // For now, return average as placeholder
        return getAverageLatency();
    }

    public double getP99Latency() {
        // This would be obtained from the Timer metrics
        // For now, return average as placeholder
        return getAverageLatency();
    }

    public long getUptime() {
        return System.currentTimeMillis() - startTime;
    }

    public long getTotalQueries() {
        return totalQueries.get();
    }

    public double getAverageQueryTime() {
        long queries = totalQueries.get();
        if (queries == 0) {
            return 0;
        }
        return (double) totalTime.get() / queries;
    }
}
