package com.scalebiometrics.master.controller;

import com.scalebiometrics.master.cache.CacheService;
import com.scalebiometrics.master.deduplication.RequestDeduplicationService;
import com.scalebiometrics.master.routing.LoadBalancingService;
import com.scalebiometrics.master.routing.RequestRouter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Advanced Features Controller - REST API for advanced features
 */
@Slf4j
@RestController
@RequestMapping("/api/advanced")
public class AdvancedFeaturesController {

    private final RequestRouter requestRouter;
    private final LoadBalancingService loadBalancingService;
    private final RequestDeduplicationService deduplicationService;
    private final CacheService cacheService;

    public AdvancedFeaturesController(
            RequestRouter requestRouter,
            LoadBalancingService loadBalancingService,
            RequestDeduplicationService deduplicationService,
            CacheService cacheService) {
        this.requestRouter = requestRouter;
        this.loadBalancingService = loadBalancingService;
        this.deduplicationService = deduplicationService;
        this.cacheService = cacheService;
    }

    /**
     * Get routing configuration
     * GET /api/advanced/routing/config
     */
    @GetMapping("/routing/config")
    public ResponseEntity<?> getRoutingConfig() {
        RoutingConfig config = RoutingConfig.builder()
                .strategy(requestRouter.getRoutingStrategy().toString())
                .replicationFactor(requestRouter.getReplicationFactor())
                .timestamp(System.currentTimeMillis())
                .build();
        return ResponseEntity.ok(config);
    }

    /**
     * Change routing strategy
     * POST /api/advanced/routing/strategy
     */
    @PostMapping("/routing/strategy")
    public ResponseEntity<?> changeRoutingStrategy(@RequestBody RoutingStrategyRequest request) {
        try {
            RequestRouter.RoutingStrategy strategy = 
                    RequestRouter.RoutingStrategy.valueOf(request.getStrategy().toUpperCase());
            requestRouter.setRoutingStrategy(strategy);
            log.info("Changed routing strategy to: {}", strategy);
            return ResponseEntity.ok(new MessageResponse("Routing strategy changed to: " + strategy));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("INVALID_STRATEGY", "Invalid routing strategy: " + request.getStrategy()));
        }
    }

    /**
     * Get load balancing metrics
     * GET /api/advanced/load-balancing/metrics
     */
    @GetMapping("/load-balancing/metrics")
    public ResponseEntity<?> getLoadBalancingMetrics() {
        LoadBalancingService.LoadDistribution distribution = loadBalancingService.getLoadDistribution();
        return ResponseEntity.ok(distribution);
    }

    /**
     * Get worker load metrics
     * GET /api/advanced/load-balancing/workers
     */
    @GetMapping("/load-balancing/workers")
    public ResponseEntity<?> getWorkerLoadMetrics() {
        return ResponseEntity.ok(loadBalancingService.getAllLoadMetrics());
    }

    /**
     * Get overloaded workers
     * GET /api/advanced/load-balancing/overloaded
     */
    @GetMapping("/load-balancing/overloaded")
    public ResponseEntity<?> getOverloadedWorkers() {
        return ResponseEntity.ok(loadBalancingService.getOverloadedWorkers());
    }

    /**
     * Get deduplication statistics
     * GET /api/advanced/deduplication/stats
     */
    @GetMapping("/deduplication/stats")
    public ResponseEntity<?> getDeduplicationStats() {
        DeduplicationStats stats = DeduplicationStats.builder()
                .pendingRequestCount(deduplicationService.getPendingRequestCount())
                .timestamp(System.currentTimeMillis())
                .build();
        return ResponseEntity.ok(stats);
    }

    /**
     * Get cache statistics
     * GET /api/advanced/cache/stats
     */
    @GetMapping("/cache/stats")
    public ResponseEntity<?> getCacheStats() {
        CacheService.CacheStatistics stats = cacheService.getStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * Clear cache
     * POST /api/advanced/cache/clear
     */
    @PostMapping("/cache/clear")
    public ResponseEntity<?> clearCache() {
        cacheService.clearAll();
        log.info("Cache cleared");
        return ResponseEntity.ok(new MessageResponse("Cache cleared"));
    }

    /**
     * Invalidate cache by pattern
     * POST /api/advanced/cache/invalidate
     */
    @PostMapping("/cache/invalidate")
    public ResponseEntity<?> invalidateCacheByPattern(@RequestBody CacheInvalidateRequest request) {
        cacheService.invalidateByPattern(request.getPattern());
        log.info("Cache invalidated for pattern: {}", request.getPattern());
        return ResponseEntity.ok(new MessageResponse("Cache invalidated for pattern: " + request.getPattern()));
    }

    /**
     * Request DTOs
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RoutingStrategyRequest {
        private String strategy;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CacheInvalidateRequest {
        private String pattern;
    }

    @lombok.Data
    @lombok.Builder
    public static class RoutingConfig {
        private String strategy;
        private int replicationFactor;
        private long timestamp;
    }

    @lombok.Data
    @lombok.Builder
    public static class DeduplicationStats {
        private int pendingRequestCount;
        private long timestamp;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class MessageResponse {
        private String message;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ErrorResponse {
        private String code;
        private String message;
    }
}
