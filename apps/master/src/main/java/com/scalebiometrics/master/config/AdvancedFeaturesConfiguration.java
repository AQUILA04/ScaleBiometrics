package com.scalebiometrics.master.config;

import com.scalebiometrics.master.cache.CacheService;
import com.scalebiometrics.master.deduplication.RequestDeduplicationService;
import com.scalebiometrics.master.grpc.WorkerPool;
import com.scalebiometrics.master.routing.LoadBalancingService;
import com.scalebiometrics.master.routing.RequestRouter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Advanced Features Configuration - Initializes beans for advanced features
 */
@Slf4j
@Configuration
public class AdvancedFeaturesConfiguration {

    /**
     * Initialize Request Router
     */
    @Bean
    public RequestRouter requestRouter(WorkerPool workerPool) {
        RequestRouter router = new RequestRouter(workerPool);
        router.initialize();
        return router;
    }

    /**
     * Initialize Load Balancing Service
     */
    @Bean
    public LoadBalancingService loadBalancingService(
            WorkerPool workerPool,
            MeterRegistry meterRegistry) {
        return new LoadBalancingService(workerPool, meterRegistry);
    }

    /**
     * Initialize Request Deduplication Service
     */
    @Bean
    public RequestDeduplicationService requestDeduplicationService(
            RedisTemplate<String, String> redisTemplate) {
        return new RequestDeduplicationService(redisTemplate);
    }

    /**
     * Initialize Cache Service
     */
    @Bean
    public CacheService cacheService(
            RedisTemplate<String, String> redisTemplate,
            MeterRegistry meterRegistry) {
        return new CacheService(redisTemplate, meterRegistry);
    }
}
