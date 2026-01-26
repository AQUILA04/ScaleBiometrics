package com.scalebiometrics.master.ha;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * HA Lifecycle Manager - Manages startup and shutdown of HA services
 */
@Slf4j
@Component
public class HALifecycleManager {

    private final LeaderElectionService leaderElectionService;
    private final HealthMonitoringService healthMonitoringService;

    public HALifecycleManager(
            LeaderElectionService leaderElectionService,
            HealthMonitoringService healthMonitoringService) {
        this.leaderElectionService = leaderElectionService;
        this.healthMonitoringService = healthMonitoringService;
    }

    /**
     * Handle application startup
     */
    @EventListener(ContextRefreshedEvent.class)
    public void onApplicationStartup() {
        log.info("🚀 Starting HA services");

        try {
            // Start leader election
            leaderElectionService.start();

            // Start health monitoring
            healthMonitoringService.start();

            log.info("✅ HA services started successfully");

        } catch (Exception e) {
            log.error("Error starting HA services", e);
        }
    }

    /**
     * Handle application shutdown
     */
    @EventListener(ContextClosedEvent.class)
    public void onApplicationShutdown() {
        log.info("🛑 Stopping HA services");

        try {
            // Stop health monitoring
            healthMonitoringService.stop();

            // Stop leader election
            leaderElectionService.stop();

            log.info("✅ HA services stopped successfully");

        } catch (Exception e) {
            log.error("Error stopping HA services", e);
        }
    }
}
