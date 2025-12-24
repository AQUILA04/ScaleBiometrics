package com.scalebiometrics.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * ScaleBiometrics Worker Node Application.
 * Distributed matcher worker with hybrid matching engine (HNSW + SourceAFIS).
 * 
 * Responsibilities:
 * - Maintain HNSW index for ANN search (Off-Heap)
 * - Cache biometric templates (Off-Heap)
 * - Execute exact matching via SourceAFIS
 * - Expose gRPC service for Master-Worker communication
 * - Consume Kafka events for template updates
 */
@SpringBootApplication
@EnableAsync
public class ScaleBiometricsWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScaleBiometricsWorkerApplication.class, args);
    }
}
