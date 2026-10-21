package com.scalebiometrics.master.ha;

import com.scalebiometrics.master.grpc.WorkerPool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsulWorkerDiscoveryService {

    private final DiscoveryClient discoveryClient;
    private final WorkerPool workerPool;
    
    private static final String WORKER_SERVICE_NAME = "biometric-worker";

    /**
     * Synchronize worker pool with Consul discovery client
     * Runs every 15 seconds
     */
    @Scheduled(fixedDelayString = "${master.worker-discovery.refresh-interval-ms:15000}")
    public void syncWorkers() {
        log.debug("Synchronizing workers with Consul...");
        try {
            List<ServiceInstance> instances = discoveryClient.getInstances(WORKER_SERVICE_NAME);
            
            Set<String> activeWorkerIds = instances.stream()
                    .map(this::extractWorkerId)
                    .collect(Collectors.toSet());
            
            // Register new workers
            for (ServiceInstance instance : instances) {
                String workerId = extractWorkerId(instance);
                String host = instance.getHost();
                int grpcPort = extractGrpcPort(instance);
                
                if (workerId != null && grpcPort > 0) {
                    try {
                        // Register will ignore if already registered
                        workerPool.registerWorker(workerId, host, grpcPort);
                    } catch (Exception e) {
                        log.error("Failed to register worker {} from Consul", workerId, e);
                    }
                }
            }
            
            // Unregister dead/removed workers
            Set<String> registeredWorkerIds = workerPool.getAllWorkers().stream()
                    .map(client -> client.getWorkerId())
                    .collect(Collectors.toSet());
                    
            for (String registeredId : registeredWorkerIds) {
                if (!activeWorkerIds.contains(registeredId)) {
                    log.info("Worker {} not found in Consul, unregistering...", registeredId);
                    workerPool.unregisterWorker(registeredId);
                }
            }
            
        } catch (Exception e) {
            log.error("Error during Consul worker synchronization", e);
        }
    }
    
    private String extractWorkerId(ServiceInstance instance) {
        Map<String, String> metadata = instance.getMetadata();
        if (metadata != null && metadata.containsKey("workerId")) {
            return metadata.get("workerId");
        }
        return instance.getInstanceId();
    }
    
    private int extractGrpcPort(ServiceInstance instance) {
        Map<String, String> metadata = instance.getMetadata();
        if (metadata != null && metadata.containsKey("grpcPort")) {
            try {
                return Integer.parseInt(metadata.get("grpcPort"));
            } catch (NumberFormatException e) {
                log.warn("Invalid grpcPort in metadata for instance {}", instance.getInstanceId());
            }
        }
        // Fallback to standard port if not provided in metadata
        return instance.getPort();
    }
}
