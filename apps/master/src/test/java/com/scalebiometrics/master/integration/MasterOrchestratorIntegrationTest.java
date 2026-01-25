package com.scalebiometrics.master.integration;

import com.scalebiometrics.core.domain.Fingerprint;
import com.scalebiometrics.core.domain.MatchResult;
import com.scalebiometrics.core.exception.BiometricException;
import com.scalebiometrics.master.cache.CacheService;
import com.scalebiometrics.master.deduplication.RequestDeduplicationService;
import com.scalebiometrics.master.grpc.WorkerPool;
import com.scalebiometrics.master.routing.LoadBalancingService;
import com.scalebiometrics.master.routing.RequestRouter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Master Orchestrator Integration Tests
 */
@SpringBootTest
@ActiveProfiles("test")
class MasterOrchestratorIntegrationTest {

    @Autowired
    private RequestRouter requestRouter;

    @Autowired
    private LoadBalancingService loadBalancingService;

    @Autowired
    private RequestDeduplicationService deduplicationService;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private WorkerPool workerPool;

    @BeforeEach
    void setUp() {
        // Clear state before each test
        cacheService.clearAll();
        deduplicationService.clearExpiredPendingRequests();
        loadBalancingService.resetMetrics();
    }

    @Test
    void testRequestRoutingIntegration() throws BiometricException {
        // Arrange
        requestRouter.setRoutingStrategy(RequestRouter.RoutingStrategy.HASH);

        // Act
        var worker1 = requestRouter.routeRequest("RID-123");
        var worker2 = requestRouter.routeRequest("RID-123");
        var worker3 = requestRouter.routeRequest("RID-456");

        // Assert
        assertNotNull(worker1);
        assertNotNull(worker2);
        assertNotNull(worker3);
        assertEquals(worker1, worker2);  // Same RID should route to same worker
    }

    @Test
    void testLoadBalancingIntegration() {
        // Arrange
        loadBalancingService.recordRequestSent("worker-1");
        loadBalancingService.recordRequestSent("worker-1");
        loadBalancingService.recordResponseReceived("worker-1", 100);
        loadBalancingService.recordResponseReceived("worker-1", 150);

        // Act
        LoadBalancingService.LoadDistribution distribution = loadBalancingService.getLoadDistribution();

        // Assert
        assertNotNull(distribution);
        assertEquals(2, distribution.getTotalRequests());
        assertEquals(125, distribution.getAverageLoad());
    }

    @Test
    void testDeduplicationIntegration() throws BiometricException {
        // Arrange
        String fingerprint = deduplicationService.generateRequestFingerprint("RID-123", 10, "1N");
        MatchResult result = createTestMatchResult();

        // Act
        deduplicationService.registerPendingRequest(fingerprint);
        deduplicationService.completePendingRequest(fingerprint, result);

        // Assert
        assertFalse(deduplicationService.hasPendingRequest(fingerprint));
    }

    @Test
    void testCachingIntegration() throws BiometricException {
        // Arrange
        String cacheKey = cacheService.generateCacheKey("probe-1", "target-1", "1TO1");
        MatchResult result = createTestMatchResult();

        // Act
        cacheService.cacheResult(cacheKey, result);
        CacheService.CacheStatistics stats = cacheService.getStatistics();

        // Assert
        assertNotNull(stats);
        assertTrue(stats.getSize() > 0);
    }

    @Test
    void testEndToEndRequestFlow() throws BiometricException {
        // Arrange
        String probeRid = "probe-1";
        int topK = 10;
        String matchingType = "1N";

        // Step 1: Generate request fingerprint
        String fingerprint = deduplicationService.generateRequestFingerprint(probeRid, topK, matchingType);

        // Step 2: Check for duplicates
        boolean hasPending = deduplicationService.hasPendingRequest(fingerprint);
        assertFalse(hasPending);

        // Step 3: Register pending request
        deduplicationService.registerPendingRequest(fingerprint);
        assertTrue(deduplicationService.hasPendingRequest(fingerprint));

        // Step 4: Route to worker
        requestRouter.setRoutingStrategy(RequestRouter.RoutingStrategy.HASH);
        var worker = requestRouter.routeRequest(probeRid);
        assertNotNull(worker);

        // Step 5: Record load
        loadBalancingService.recordRequestSent(worker.getWorkerId());

        // Step 6: Simulate response
        loadBalancingService.recordResponseReceived(worker.getWorkerId(), 150);

        // Step 7: Complete request
        MatchResult result = createTestMatchResult();
        deduplicationService.completePendingRequest(fingerprint, result);

        // Step 8: Cache result
        String cacheKey = cacheService.generateCacheKey(probeRid, "target-1", matchingType);
        cacheService.cacheResult(cacheKey, result);

        // Assert
        assertFalse(deduplicationService.hasPendingRequest(fingerprint));
        CacheService.CacheStatistics stats = cacheService.getStatistics();
        assertTrue(stats.getSize() > 0);
    }

    @Test
    void testMultipleRoutingStrategies() throws BiometricException {
        // Arrange
        String rid = "RID-123";

        // Act & Assert - HASH
        requestRouter.setRoutingStrategy(RequestRouter.RoutingStrategy.HASH);
        var workerHash1 = requestRouter.routeRequest(rid);
        var workerHash2 = requestRouter.routeRequest(rid);
        assertEquals(workerHash1, workerHash2);

        // Act & Assert - ROUND_ROBIN
        requestRouter.setRoutingStrategy(RequestRouter.RoutingStrategy.ROUND_ROBIN);
        var workerRR1 = requestRouter.routeRequest("RID-1");
        var workerRR2 = requestRouter.routeRequest("RID-2");
        // May or may not be different depending on worker count

        // Act & Assert - LEAST_LOADED
        requestRouter.setRoutingStrategy(RequestRouter.RoutingStrategy.LEAST_LOADED);
        var workerLL = requestRouter.routeRequest(rid);
        assertNotNull(workerLL);
    }

    // Helper methods

    private MatchResult createTestMatchResult() {
        MatchResult result = new MatchResult();
        result.setProbeRid("probe-1");
        result.setStatus("MATCH");
        result.setMatchingTimeMs(150);
        return result;
    }
}
