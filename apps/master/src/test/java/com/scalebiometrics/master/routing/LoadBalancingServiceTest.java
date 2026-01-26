package com.scalebiometrics.master.routing;

import com.scalebiometrics.master.grpc.WorkerPool;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LoadBalancingService Unit Tests
 */
@ExtendWith(MockitoExtension.class)
class LoadBalancingServiceTest {

    @Mock
    private WorkerPool workerPool;

    private MeterRegistry meterRegistry;
    private LoadBalancingService loadBalancingService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        loadBalancingService = new LoadBalancingService(workerPool, meterRegistry);
    }

    @Test
    void testRecordRequestSent() {
        // Act
        loadBalancingService.recordRequestSent("worker-1");
        loadBalancingService.recordRequestSent("worker-1");
        loadBalancingService.recordRequestSent("worker-2");

        // Assert
        LoadBalancingService.WorkerLoadMetrics metrics1 = loadBalancingService.getWorkerLoadMetrics("worker-1");
        LoadBalancingService.WorkerLoadMetrics metrics2 = loadBalancingService.getWorkerLoadMetrics("worker-2");

        assertNotNull(metrics1);
        assertNotNull(metrics2);
        assertEquals(2, metrics1.getTotalRequests());
        assertEquals(1, metrics2.getTotalRequests());
    }

    @Test
    void testRecordResponseReceived() {
        // Arrange
        loadBalancingService.recordRequestSent("worker-1");
        loadBalancingService.recordRequestSent("worker-1");

        // Act
        loadBalancingService.recordResponseReceived("worker-1", 100);
        loadBalancingService.recordResponseReceived("worker-1", 150);

        // Assert
        LoadBalancingService.WorkerLoadMetrics metrics = loadBalancingService.getWorkerLoadMetrics("worker-1");
        assertEquals(0, metrics.getPendingRequests());
        assertEquals(125, metrics.getAverageResponseTimeMs(), 0.1);
    }

    @Test
    void testIsWorkerOverloaded() {
        // Arrange
        for (int i = 0; i < 1001; i++) {
            loadBalancingService.recordRequestSent("worker-1");
        }

        // Act
        boolean isOverloaded = loadBalancingService.isWorkerOverloaded("worker-1");

        // Assert
        assertTrue(isOverloaded);
    }

    @Test
    void testIsWorkerNotOverloaded() {
        // Arrange
        for (int i = 0; i < 500; i++) {
            loadBalancingService.recordRequestSent("worker-1");
        }

        // Act
        boolean isOverloaded = loadBalancingService.isWorkerOverloaded("worker-1");

        // Assert
        assertFalse(isOverloaded);
    }

    @Test
    void testGetOverloadedWorkers() {
        // Arrange
        for (int i = 0; i < 1001; i++) {
            loadBalancingService.recordRequestSent("worker-1");
        }
        for (int i = 0; i < 500; i++) {
            loadBalancingService.recordRequestSent("worker-2");
        }

        // Act
        var overloadedWorkers = loadBalancingService.getOverloadedWorkers();

        // Assert
        assertEquals(1, overloadedWorkers.size());
        assertTrue(overloadedWorkers.contains("worker-1"));
    }

    @Test
    void testGetLeastLoadedWorker() {
        // Arrange
        for (int i = 0; i < 100; i++) {
            loadBalancingService.recordRequestSent("worker-1");
        }
        for (int i = 0; i < 50; i++) {
            loadBalancingService.recordRequestSent("worker-2");
        }
        for (int i = 0; i < 200; i++) {
            loadBalancingService.recordRequestSent("worker-3");
        }

        // Act
        String leastLoaded = loadBalancingService.getLeastLoadedWorker();

        // Assert
        assertEquals("worker-2", leastLoaded);
    }

    @Test
    void testGetMostLoadedWorker() {
        // Arrange
        for (int i = 0; i < 100; i++) {
            loadBalancingService.recordRequestSent("worker-1");
        }
        for (int i = 0; i < 50; i++) {
            loadBalancingService.recordRequestSent("worker-2");
        }
        for (int i = 0; i < 200; i++) {
            loadBalancingService.recordRequestSent("worker-3");
        }

        // Act
        String mostLoaded = loadBalancingService.getMostLoadedWorker();

        // Assert
        assertEquals("worker-3", mostLoaded);
    }

    @Test
    void testGetLoadDistribution() {
        // Arrange
        for (int i = 0; i < 100; i++) {
            loadBalancingService.recordRequestSent("worker-1");
        }
        for (int i = 0; i < 100; i++) {
            loadBalancingService.recordRequestSent("worker-2");
        }
        for (int i = 0; i < 100; i++) {
            loadBalancingService.recordRequestSent("worker-3");
        }

        // Act
        LoadBalancingService.LoadDistribution distribution = loadBalancingService.getLoadDistribution();

        // Assert
        assertNotNull(distribution);
        assertEquals(300, distribution.getTotalRequests());
        assertEquals(100, distribution.getAverageLoad());
        assertEquals(100, distribution.getMaxLoad());
        assertEquals(100, distribution.getMinLoad());
        assertTrue(distribution.isBalanced());
    }

    @Test
    void testGetLoadDistributionUnbalanced() {
        // Arrange
        for (int i = 0; i < 500; i++) {
            loadBalancingService.recordRequestSent("worker-1");
        }
        for (int i = 0; i < 50; i++) {
            loadBalancingService.recordRequestSent("worker-2");
        }
        for (int i = 0; i < 50; i++) {
            loadBalancingService.recordRequestSent("worker-3");
        }

        // Act
        LoadBalancingService.LoadDistribution distribution = loadBalancingService.getLoadDistribution();

        // Assert
        assertNotNull(distribution);
        assertEquals(600, distribution.getTotalRequests());
        assertFalse(distribution.isBalanced());
    }

    @Test
    void testResetMetrics() {
        // Arrange
        loadBalancingService.recordRequestSent("worker-1");
        loadBalancingService.recordRequestSent("worker-2");

        // Act
        loadBalancingService.resetMetrics();

        // Assert
        assertTrue(loadBalancingService.getAllLoadMetrics().isEmpty());
    }
}
