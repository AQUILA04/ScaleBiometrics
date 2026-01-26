package com.scalebiometrics.master.ha;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Circuit Breaker Service - Implements circuit breaker pattern for fault tolerance.
 * 
 * States:
 * - CLOSED: Normal operation, requests pass through
 * - OPEN: Failures detected, requests rejected immediately
 * - HALF_OPEN: Testing if service recovered, limited requests allowed
 * 
 * Transitions:
 * - CLOSED → OPEN: When failure threshold exceeded
 * - OPEN → HALF_OPEN: After timeout period
 * - HALF_OPEN → CLOSED: If requests succeed
 * - HALF_OPEN → OPEN: If requests still fail
 */
@Slf4j
@Service
public class CircuitBreakerService {

    public enum CircuitState {
        CLOSED,      // Normal operation
        OPEN,        // Failures detected, rejecting requests
        HALF_OPEN    // Testing recovery
    }

    @Value("${master.circuit-breaker.enabled:true}")
    private boolean circuitBreakerEnabled;

    @Value("${master.circuit-breaker.failure-threshold:5}")
    private int failureThreshold;

    @Value("${master.circuit-breaker.success-threshold:2}")
    private int successThreshold;

    @Value("${master.circuit-breaker.timeout-ms:60000}")
    private long timeoutMs;

    private final Map<String, CircuitBreakerState> breakers = new ConcurrentHashMap<>();

    /**
     * Record success for a service
     */
    public void recordSuccess(String serviceId) {
        if (!circuitBreakerEnabled) {
            return;
        }

        CircuitBreakerState state = breakers.computeIfAbsent(
                serviceId,
                k -> new CircuitBreakerState(serviceId)
        );

        synchronized (state) {
            if (state.getState() == CircuitState.CLOSED) {
                // Already closed, nothing to do
                return;
            }

            if (state.getState() == CircuitState.HALF_OPEN) {
                state.incrementSuccesses();
                log.debug("Circuit breaker {} - Half-open success: {}/{}", 
                        serviceId, state.getSuccesses(), successThreshold);

                if (state.getSuccesses() >= successThreshold) {
                    state.setState(CircuitState.CLOSED);
                    state.resetCounters();
                    log.info("✅ Circuit breaker {} - CLOSED (recovered)", serviceId);
                }
            }
        }
    }

    /**
     * Record failure for a service
     */
    public void recordFailure(String serviceId) {
        if (!circuitBreakerEnabled) {
            return;
        }

        CircuitBreakerState state = breakers.computeIfAbsent(
                serviceId,
                k -> new CircuitBreakerState(serviceId)
        );

        synchronized (state) {
            state.incrementFailures();
            log.debug("Circuit breaker {} - Failure: {}/{}", 
                    serviceId, state.getFailures(), failureThreshold);

            if (state.getState() == CircuitState.CLOSED) {
                if (state.getFailures() >= failureThreshold) {
                    state.setState(CircuitState.OPEN);
                    state.setOpenedAt(System.currentTimeMillis());
                    log.warn("⚠️  Circuit breaker {} - OPEN (threshold exceeded)", serviceId);
                }
            } else if (state.getState() == CircuitState.HALF_OPEN) {
                // Failure in half-open state, go back to open
                state.setState(CircuitState.OPEN);
                state.setOpenedAt(System.currentTimeMillis());
                state.resetCounters();
                log.warn("⚠️  Circuit breaker {} - OPEN (failed during recovery)", serviceId);
            }
        }
    }

    /**
     * Check if request is allowed
     */
    public boolean isRequestAllowed(String serviceId) {
        if (!circuitBreakerEnabled) {
            return true;
        }

        CircuitBreakerState state = breakers.computeIfAbsent(
                serviceId,
                k -> new CircuitBreakerState(serviceId)
        );

        synchronized (state) {
            if (state.getState() == CircuitState.CLOSED) {
                return true;
            }

            if (state.getState() == CircuitState.OPEN) {
                // Check if timeout has passed
                long elapsedMs = System.currentTimeMillis() - state.getOpenedAt();
                if (elapsedMs >= timeoutMs) {
                    state.setState(CircuitState.HALF_OPEN);
                    state.resetCounters();
                    log.info("🔄 Circuit breaker {} - HALF_OPEN (testing recovery)", serviceId);
                    return true;
                }
                return false;
            }

            if (state.getState() == CircuitState.HALF_OPEN) {
                // Allow limited requests in half-open state
                return true;
            }

            return false;
        }
    }

    /**
     * Get circuit breaker state
     */
    public CircuitState getState(String serviceId) {
        CircuitBreakerState state = breakers.get(serviceId);
        return state != null ? state.getState() : CircuitState.CLOSED;
    }

    /**
     * Get all circuit breaker states
     */
    public Map<String, CircuitBreakerState> getAllStates() {
        return new HashMap<>(breakers);
    }

    /**
     * Reset circuit breaker
     */
    public void reset(String serviceId) {
        CircuitBreakerState state = breakers.get(serviceId);
        if (state != null) {
            synchronized (state) {
                state.setState(CircuitState.CLOSED);
                state.resetCounters();
                log.info("Circuit breaker {} - RESET", serviceId);
            }
        }
    }

    /**
     * Reset all circuit breakers
     */
    public void resetAll() {
        for (CircuitBreakerState state : breakers.values()) {
            synchronized (state) {
                state.setState(CircuitState.CLOSED);
                state.resetCounters();
            }
        }
        log.info("All circuit breakers RESET");
    }

    /**
     * Circuit Breaker State
     */
    @lombok.Data
    public static class CircuitBreakerState {
        private String serviceId;
        private CircuitState state = CircuitState.CLOSED;
        private AtomicInteger failures = new AtomicInteger(0);
        private AtomicInteger successes = new AtomicInteger(0);
        private long openedAt;

        public CircuitBreakerState(String serviceId) {
            this.serviceId = serviceId;
        }

        public void incrementFailures() {
            failures.incrementAndGet();
        }

        public void incrementSuccesses() {
            successes.incrementAndGet();
        }

        public int getFailures() {
            return failures.get();
        }

        public int getSuccesses() {
            return successes.get();
        }

        public void resetCounters() {
            failures.set(0);
            successes.set(0);
        }
    }
}
