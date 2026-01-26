package com.scalebiometrics.master.config;

import com.scalebiometrics.master.grpc.WorkerPool;
import com.scalebiometrics.master.ha.*;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * HA Configuration - Initializes beans for high availability
 */
@Slf4j
@Configuration
public class HAConfiguration {

    /**
     * Initialize Leader Election Service
     */
    @Bean
    public LeaderElectionService leaderElectionService(RedisTemplate<String, String> redisTemplate) {
        return new LeaderElectionService(redisTemplate);
    }

    /**
     * Initialize Health Monitoring Service
     */
    @Bean
    public HealthMonitoringService healthMonitoringService(
            WorkerPool workerPool,
            MeterRegistry meterRegistry) {
        return new HealthMonitoringService(workerPool, meterRegistry);
    }

    /**
     * Initialize Circuit Breaker Service
     */
    @Bean
    public CircuitBreakerService circuitBreakerService() {
        return new CircuitBreakerService();
    }

    /**
     * Initialize Failover Service
     */
    @Bean
    public FailoverService failoverService(
            WorkerPool workerPool,
            HealthMonitoringService healthMonitoringService,
            CircuitBreakerService circuitBreakerService) {
        return new FailoverService(workerPool, healthMonitoringService, circuitBreakerService);
    }

    /**
     * Initialize HA Lifecycle Manager
     */
    @Bean
    public HALifecycleManager haLifecycleManager(
            LeaderElectionService leaderElectionService,
            HealthMonitoringService healthMonitoringService) {
        return new HALifecycleManager(leaderElectionService, healthMonitoringService);
    }
}
