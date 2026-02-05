package com.scalebiometrics.worker.config;

import com.scalebiometrics.worker.engine.*;
import com.scalebiometrics.worker.grpc.MatcherServiceImpl;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * Worker Configuration - Initializes beans for distributed matching engine
 */
@Slf4j
@Configuration
public class WorkerConfiguration {

    @Value("${grpc.server.port:39092}")
    private int grpcPort;

    /**
     * Initialize HNSW Index Manager
     */
//    @Bean(initMethod = "initialize")
//    public HNSWIndexManager hnswIndexManager() {
//        return new HNSWIndexManager();
//    }

    /**
     * Initialize Off-Heap Memory Manager
     */
//    @Bean(initMethod = "initialize")
//    public OffHeapMemoryManager offHeapMemoryManager() {
//        return new OffHeapMemoryManager();
//    }

    /**
     * Initialize SourceAFIS Matcher
     */
//    @Bean
//    public SourceAFISMatcher sourceAFISMatcher() {
//        return new SourceAFISMatcher();
//    }

    /**
     * Initialize Matching Metrics
     */
//    @Bean
//    public MatchingMetrics matchingMetrics(MeterRegistry meterRegistry) {
//        return new MatchingMetrics(meterRegistry);
//    }

    /**
     * Initialize Hybrid Matching Engine
     */
//    @Bean
//    public HybridMatchingEngine hybridMatchingEngine(
//            HNSWIndexManager hnswIndexManager,
//            SourceAFISMatcher sourceAFISMatcher,
//            OffHeapMemoryManager offHeapMemoryManager,
//            MatchingMetrics matchingMetrics) {
//        return new HybridMatchingEngine(
//                hnswIndexManager,
//                sourceAFISMatcher,
//                offHeapMemoryManager,
//                matchingMetrics
//        );
//    }

    /**
     * Initialize gRPC Server
     */
    @Bean
    public Server grpcServer(MatcherServiceImpl matcherService) throws IOException {
        Server server = ServerBuilder.forPort(grpcPort)
                .addService(matcherService)
                .maxInboundMessageSize(4 * 1024 * 1024)  // 4MB
                .build()
                .start();

        log.info("gRPC Server started on port {}", grpcPort);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down gRPC server");
            server.shutdown();
        }));

        return server;
    }

    /**
     * Initialize gRPC Matcher Service
     */
//    @Bean
//    public MatcherServiceImpl matcherService(HybridMatchingEngine matchingEngine, MatchingMetrics matchingMetrics) {
//        return new MatcherServiceImpl(matchingEngine, matchingMetrics);
//    }
}
