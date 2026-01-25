package com.scalebiometrics.master.config;

import com.scalebiometrics.master.grpc.WorkerPool;
import com.scalebiometrics.master.orchestrator.ScatterGatherOrchestrator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Master Configuration - Initializes beans for master orchestrator
 */
@Slf4j
@Configuration
public class MasterConfiguration {

    /**
     * Initialize Worker Pool
     */
    @Bean
    public WorkerPool workerPool() {
        return new WorkerPool();
    }

    /**
     * Initialize Scatter-Gather Orchestrator
     */
    @Bean
    public ScatterGatherOrchestrator scatterGatherOrchestrator(WorkerPool workerPool) {
        return new ScatterGatherOrchestrator(workerPool);
    }
}
