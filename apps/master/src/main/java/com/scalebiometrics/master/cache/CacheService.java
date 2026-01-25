package com.scalebiometrics.master.cache;

import com.scalebiometrics.core.domain.MatchResult;
import com.scalebiometrics.core.exception.BiometricException;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Cache Service - Manages caching of matching results.
 * 
 * Responsibilities:
 * - Cache matching results
 * - Manage cache TTL
 * - Track cache statistics
 * - Support cache invalidation
 */
@Slf4j
@Service
public class CacheService {

    @Value("${master.cache.enabled:true}")
    private boolean cacheEnabled;

    @Value("${master.cache.ttl-seconds:3600}")
    private long cacheTtlSeconds;

    @Value("${master.cache.max-size:10000}")
    private int maxCacheSize;

    private final RedisTemplate<String, String> redisTemplate;
    private final MeterRegistry meterRegistry;

    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final AtomicLong cacheEvictions = new AtomicLong(0);

    private static final String CACHE_KEY_PREFIX = "scalebiometrics:cache:";
    private static final String CACHE_STATS_KEY = "scalebiometrics:cache:stats";

    public CacheService(RedisTemplate<String, String> redisTemplate, MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.meterRegistry = meterRegistry;
        initializeMetrics();
    }

    /**
     * Initialize metrics
     */
    private void initializeMetrics() {
        meterRegistry.gauge("cache.hits", cacheHits);
        meterRegistry.gauge("cache.misses", cacheMisses);
        meterRegistry.gauge("cache.evictions", cacheEvictions);
    }

    /**
     * Generate cache key
     */
    public String generateCacheKey(String probeRid, String targetRid, String matchingType) {
        return String.format("%s%s:%s:%s", CACHE_KEY_PREFIX, matchingType, probeRid, targetRid);
    }

    /**
     * Get cached result
     */
    public MatchResult getCachedResult(String cacheKey) throws BiometricException {
        if (!cacheEnabled) {
            return null;
        }

        try {
            String cachedValue = redisTemplate.opsForValue().get(cacheKey);
            if (cachedValue != null) {
                cacheHits.incrementAndGet();
                log.debug("Cache hit for key: {}", cacheKey);
                // In production, deserialize from JSON
                return null;
            } else {
                cacheMisses.incrementAndGet();
                log.debug("Cache miss for key: {}", cacheKey);
                return null;
            }

        } catch (Exception e) {
            log.error("Error retrieving from cache", e);
            return null;
        }
    }

    /**
     * Cache result
     */
    public void cacheResult(String cacheKey, MatchResult result) throws BiometricException {
        if (!cacheEnabled) {
            return;
        }

        try {
            // Check cache size
            Long size = redisTemplate.keys(CACHE_KEY_PREFIX + "*").size();
            if (size >= maxCacheSize) {
                log.warn("Cache size limit reached: {}/{}", size, maxCacheSize);
                cacheEvictions.incrementAndGet();
                // In production, implement LRU eviction
            }

            // Serialize result to JSON
            String serializedResult = result.toString();

            // Store in cache with TTL
            redisTemplate.opsForValue().set(
                    cacheKey,
                    serializedResult,
                    cacheTtlSeconds,
                    TimeUnit.SECONDS
            );

            log.debug("Cached result for key: {}", cacheKey);

        } catch (Exception e) {
            log.error("Error caching result", e);
            // Don't throw exception, cache is optional
        }
    }

    /**
     * Invalidate cache entry
     */
    public void invalidate(String cacheKey) {
        try {
            redisTemplate.delete(cacheKey);
            log.debug("Invalidated cache entry: {}", cacheKey);
        } catch (Exception e) {
            log.error("Error invalidating cache", e);
        }
    }

    /**
     * Invalidate cache by pattern
     */
    public void invalidateByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(CACHE_KEY_PREFIX + pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Invalidated {} cache entries matching pattern: {}", keys.size(), pattern);
            }
        } catch (Exception e) {
            log.error("Error invalidating cache by pattern", e);
        }
    }

    /**
     * Clear all cache
     */
    public void clearAll() {
        try {
            Set<String> keys = redisTemplate.keys(CACHE_KEY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Cleared all cache entries ({})", keys.size());
            }
        } catch (Exception e) {
            log.error("Error clearing cache", e);
        }
    }

    /**
     * Get cache statistics
     */
    public CacheStatistics getStatistics() {
        long hits = cacheHits.get();
        long misses = cacheMisses.get();
        long total = hits + misses;
        double hitRate = total > 0 ? (double) hits / total * 100 : 0;

        Long size = 0L;
        try {
            Set<String> keys = redisTemplate.keys(CACHE_KEY_PREFIX + "*");
            size = keys != null ? keys.size() : 0L;
        } catch (Exception e) {
            log.error("Error getting cache size", e);
        }

        return CacheStatistics.builder()
                .hits(hits)
                .misses(misses)
                .total(total)
                .hitRate(hitRate)
                .evictions(cacheEvictions.get())
                .size(size)
                .maxSize(maxCacheSize)
                .ttlSeconds(cacheTtlSeconds)
                .enabled(cacheEnabled)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * Reset statistics
     */
    public void resetStatistics() {
        cacheHits.set(0);
        cacheMisses.set(0);
        cacheEvictions.set(0);
        log.info("Reset cache statistics");
    }

    /**
     * Cache Statistics
     */
    @lombok.Data
    @lombok.Builder
    public static class CacheStatistics {
        private long hits;
        private long misses;
        private long total;
        private double hitRate;
        private long evictions;
        private long size;
        private int maxSize;
        private long ttlSeconds;
        private boolean enabled;
        private long timestamp;
    }
}
