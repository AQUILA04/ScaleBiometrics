package com.scalebiometrics.master.deduplication;

import com.scalebiometrics.core.domain.MatchResult;
import com.scalebiometrics.core.exception.BiometricException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Request Deduplication Service - Prevents duplicate matching requests.
 * 
 * Responsibilities:
 * - Detect duplicate requests
 * - Return cached results for duplicates
 * - Coordinate concurrent identical requests
 * - Manage deduplication cache
 */
@Slf4j
@Service
public class RequestDeduplicationService {

    @Value("${master.deduplication.enabled:true}")
    private boolean deduplicationEnabled;

    @Value("${master.deduplication.ttl-seconds:300}")
    private long cacheTtlSeconds;

    private final RedisTemplate<String, String> redisTemplate;
    private final Map<String, PendingRequest> pendingRequests = new ConcurrentHashMap<>();

    private static final String CACHE_KEY_PREFIX = "scalebiometrics:dedup:";
    private static final String PENDING_KEY_PREFIX = "scalebiometrics:pending:";

    public RequestDeduplicationService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Generate request fingerprint for deduplication
     */
    public String generateRequestFingerprint(String probeRid, int topK, String matchingType) {
        return String.format("%s:%s:%d", matchingType, probeRid, topK);
    }

    /**
     * Check if request is duplicate and get cached result
     */
    public MatchResult getCachedResult(String requestFingerprint) throws BiometricException {
        if (!deduplicationEnabled) {
            return null;
        }

        try {
            String cacheKey = CACHE_KEY_PREFIX + requestFingerprint;
            String cachedResult = redisTemplate.opsForValue().get(cacheKey);

            if (cachedResult != null) {
                log.debug("Found cached result for request: {}", requestFingerprint);
                // In production, deserialize the cached result
                // For now, return null to indicate cache miss
                return null;
            }

            return null;

        } catch (Exception e) {
            log.error("Error checking cache", e);
            return null;
        }
    }

    /**
     * Register pending request
     */
    public PendingRequest registerPendingRequest(String requestFingerprint) {
        PendingRequest pending = new PendingRequest(requestFingerprint);
        pendingRequests.put(requestFingerprint, pending);
        log.debug("Registered pending request: {}", requestFingerprint);
        return pending;
    }

    /**
     * Get pending request
     */
    public PendingRequest getPendingRequest(String requestFingerprint) {
        return pendingRequests.get(requestFingerprint);
    }

    /**
     * Complete pending request
     */
    public void completePendingRequest(String requestFingerprint, MatchResult result) {
        PendingRequest pending = pendingRequests.get(requestFingerprint);
        if (pending != null) {
            pending.setResult(result);
            pending.setCompleted(true);
            log.debug("Completed pending request: {}", requestFingerprint);

            // Notify all waiters
            synchronized (pending) {
                pending.notifyAll();
            }

            // Cache the result
            cacheResult(requestFingerprint, result);

            // Remove from pending after a delay
            pendingRequests.remove(requestFingerprint);
        }
    }

    /**
     * Wait for pending request to complete
     */
    public MatchResult waitForPendingRequest(String requestFingerprint, long timeoutMs) 
            throws BiometricException, InterruptedException {
        PendingRequest pending = pendingRequests.get(requestFingerprint);
        if (pending == null) {
            throw new BiometricException("Pending request not found");
        }

        synchronized (pending) {
            long startTime = System.currentTimeMillis();
            while (!pending.isCompleted()) {
                long elapsedMs = System.currentTimeMillis() - startTime;
                if (elapsedMs >= timeoutMs) {
                    throw new BiometricException("Timeout waiting for pending request");
                }

                long remainingMs = timeoutMs - elapsedMs;
                pending.wait(remainingMs);
            }
        }

        return pending.getResult();
    }

    /**
     * Cache result
     */
    private void cacheResult(String requestFingerprint, MatchResult result) {
        try {
            String cacheKey = CACHE_KEY_PREFIX + requestFingerprint;
            // In production, serialize the result to JSON
            String serializedResult = result.toString();
            redisTemplate.opsForValue().set(
                    cacheKey,
                    serializedResult,
                    cacheTtlSeconds,
                    TimeUnit.SECONDS
            );
            log.debug("Cached result for request: {}", requestFingerprint);
        } catch (Exception e) {
            log.error("Error caching result", e);
        }
    }

    /**
     * Check if there's a pending request for this fingerprint
     */
    public boolean hasPendingRequest(String requestFingerprint) {
        return pendingRequests.containsKey(requestFingerprint);
    }

    /**
     * Get pending request count
     */
    public int getPendingRequestCount() {
        return pendingRequests.size();
    }

    /**
     * Clear expired pending requests
     */
    public void clearExpiredPendingRequests() {
        long now = System.currentTimeMillis();
        pendingRequests.entrySet().removeIf(e -> {
            PendingRequest pending = e.getValue();
            long ageMs = now - pending.getCreatedAt();
            return ageMs > (cacheTtlSeconds * 1000);
        });
    }

    /**
     * Pending Request
     */
    @lombok.Data
    public static class PendingRequest {
        private String requestFingerprint;
        private MatchResult result;
        private boolean completed = false;
        private long createdAt;

        public PendingRequest(String requestFingerprint) {
            this.requestFingerprint = requestFingerprint;
            this.createdAt = System.currentTimeMillis();
        }
    }
}
