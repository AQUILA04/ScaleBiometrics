package com.scalebiometrics.master;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * ScaleBiometrics Master Orchestrator Application.
 * Orchestrates distributed matching across worker nodes.
 * 
 * Responsibilities:
 * - Consume matching requests from Kafka
 * - Implement scatter-gather pattern to query worker shards
 * - Aggregate results from multiple workers
 * - Handle worker failures and circuit breaking
 * - Implement leader election for high availability
 */
@SpringBootApplication
@EnableAsync
public class ScaleBiometricsMasterApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScaleBiometricsMasterApplication.class, args);
    }
}
