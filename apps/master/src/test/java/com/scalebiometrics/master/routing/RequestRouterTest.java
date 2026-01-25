package com.scalebiometrics.master.routing;

import com.scalebiometrics.core.exception.BiometricException;
import com.scalebiometrics.master.grpc.WorkerClient;
import com.scalebiometrics.master.grpc.WorkerPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * RequestRouter Unit Tests
 */
@ExtendWith(MockitoExtension.class)
class RequestRouterTest {

    @Mock
    private WorkerPool workerPool;

    @Mock
    private WorkerClient worker1;

    @Mock
    private WorkerClient worker2;

    @Mock
    private WorkerClient worker3;

    private RequestRouter requestRouter;
    private List<WorkerClient> workers;

    @BeforeEach
    void setUp() {
        requestRouter = new RequestRouter(workerPool);

        workers = new ArrayList<>();
        workers.add(worker1);
        workers.add(worker2);
        workers.add(worker3);

        when(workerPool.getHealthyWorkers()).thenReturn(workers);
    }

    @Test
    void testHashRouting() throws BiometricException {
        // Arrange
        lenient().when(worker1.getWorkerId()).thenReturn("worker-1");
        lenient().when(worker2.getWorkerId()).thenReturn("worker-2");
        lenient().when(worker3.getWorkerId()).thenReturn("worker-3");
        requestRouter.setRoutingStrategy(RequestRouter.RoutingStrategy.HASH);
        String rid1 = "RID-123";
        String rid2 = "RID-456";

        // Act
        WorkerClient result1 = requestRouter.routeRequest(rid1);
        WorkerClient result2 = requestRouter.routeRequest(rid1);  // Same RID
        WorkerClient result3 = requestRouter.routeRequest(rid2);  // Different RID

        // Assert
        assertNotNull(result1);
        assertEquals(result1, result2);  // Same RID should route to same worker
        assertTrue(workers.contains(result1));
        assertTrue(workers.contains(result3));
    }

    @Test
    void testRoundRobinRouting() throws BiometricException {
        // Arrange
        requestRouter.setRoutingStrategy(RequestRouter.RoutingStrategy.ROUND_ROBIN);

        // Act
        WorkerClient result1 = requestRouter.routeRequest("RID-1");
        WorkerClient result2 = requestRouter.routeRequest("RID-2");
        WorkerClient result3 = requestRouter.routeRequest("RID-3");
        WorkerClient result4 = requestRouter.routeRequest("RID-4");

        // Assert
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result3);
        assertNotNull(result4);

        // Should cycle through workers
        assertNotEquals(result1, result2);
        assertNotEquals(result2, result3);
        assertNotEquals(result3, result4);
    }

    @Test
    void testLeastLoadedRouting() throws BiometricException {
        // Arrange
        requestRouter.setRoutingStrategy(RequestRouter.RoutingStrategy.LEAST_LOADED);

        // Act
        WorkerClient result = requestRouter.routeRequest("RID-123");

        // Assert
        assertNotNull(result);
        assertTrue(workers.contains(result));
    }

    @Test
    void testNoHealthyWorkers() {
        // Arrange
        when(workerPool.getHealthyWorkers()).thenReturn(new ArrayList<>());
        lenient().when(worker1.getWorkerId()).thenReturn("worker-1");
        lenient().when(worker2.getWorkerId()).thenReturn("worker-2");
        lenient().when(worker3.getWorkerId()).thenReturn("worker-3");

        // Act & Assert
        assertThrows(BiometricException.class, () -> {
            requestRouter.routeRequest("RID-123");
        });
    }

    @Test
    void testGetReplicasForKey() throws BiometricException {
        // Arrange
        lenient().when(worker1.getWorkerId()).thenReturn("worker-1");
        lenient().when(worker2.getWorkerId()).thenReturn("worker-2");
        lenient().when(worker3.getWorkerId()).thenReturn("worker-3");
        requestRouter.setReplicationFactor(2);

        // Act
        List<WorkerClient> replicas = requestRouter.getReplicasForKey("RID-123");

        // Assert
        assertNotNull(replicas);
        assertEquals(2, replicas.size());
        assertTrue(workers.containsAll(replicas));
    }

    @Test
    void testChangeRoutingStrategy() throws BiometricException {
        // Arrange
        assertEquals(RequestRouter.RoutingStrategy.HASH, requestRouter.getRoutingStrategy());

        // Act
        requestRouter.setRoutingStrategy(RequestRouter.RoutingStrategy.ROUND_ROBIN);

        // Assert
        assertEquals(RequestRouter.RoutingStrategy.ROUND_ROBIN, requestRouter.getRoutingStrategy());
    }

    @Test
    void testSetReplicationFactor() {
        // Arrange
        assertEquals(1, requestRouter.getReplicationFactor());

        // Act
        requestRouter.setReplicationFactor(3);

        // Assert
        assertEquals(3, requestRouter.getReplicationFactor());
    }
}
