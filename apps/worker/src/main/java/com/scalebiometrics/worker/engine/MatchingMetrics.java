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

    public void recordMatch1N(long durationMs, int candidatesFound, String status) {
        match1NCounter.increment();
        matchingLatencyTimer.record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        totalQueries.incrementAndGet();
        totalTime.addAndGet(durationMs);

        meterRegistry.gauge("matching.1n.candidates", () -> candidatesFound);
    }

    public void recordMatch1To1(long durationMs, boolean isMatch) {
        match1To1Counter.increment();
        matchingLatencyTimer.record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        totalQueries.incrementAndGet();
        totalTime.addAndGet(durationMs);

        if (isMatch) {
            meterRegistry.counter("matching.1to1.matches").increment();
        }
    }

    public void recordMatchError(String errorType) {
        matchErrorCounter.increment();
        meterRegistry.counter("matching.errors", "type", errorType).increment();
    }

    public void recordIndexAdd() {
        indexAddCounter.increment();
    }

    public void recordIndexRemove() {
        indexRemoveCounter.increment();
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
