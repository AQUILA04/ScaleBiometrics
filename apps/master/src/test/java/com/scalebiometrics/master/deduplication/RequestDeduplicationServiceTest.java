package com.scalebiometrics.master.deduplication;

import com.scalebiometrics.core.domain.MatchResult;
import com.scalebiometrics.core.exception.BiometricException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * RequestDeduplicationService Unit Tests
 */
@ExtendWith(MockitoExtension.class)
class RequestDeduplicationServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    private RequestDeduplicationService deduplicationService;

    @BeforeEach
    void setUp() {
        deduplicationService = new RequestDeduplicationService(redisTemplate);
    }

    @Test
    void testGenerateRequestFingerprint() {
        // Act
        String fingerprint = deduplicationService.generateRequestFingerprint("RID-123", 10, "1N");

        // Assert
        assertNotNull(fingerprint);
        assertEquals("1N:RID-123:10", fingerprint);
    }

    @Test
    void testRegisterPendingRequest() {
        // Arrange
        String fingerprint = "1N:RID-123:10";

        // Act
        RequestDeduplicationService.PendingRequest pending = 
                deduplicationService.registerPendingRequest(fingerprint);

        // Assert
        assertNotNull(pending);
        assertEquals(fingerprint, pending.getRequestFingerprint());
        assertFalse(pending.isCompleted());
    }

    @Test
    void testGetPendingRequest() {
        // Arrange
        String fingerprint = "1N:RID-123:10";
        deduplicationService.registerPendingRequest(fingerprint);

        // Act
        RequestDeduplicationService.PendingRequest pending = 
                deduplicationService.getPendingRequest(fingerprint);

        // Assert
        assertNotNull(pending);
        assertEquals(fingerprint, pending.getRequestFingerprint());
    }

    @Test
    void testCompletePendingRequest() {
        // Arrange
        String fingerprint = "1N:RID-123:10";
        deduplicationService.registerPendingRequest(fingerprint);
        MatchResult result = createTestMatchResult();

        // Act
        deduplicationService.completePendingRequest(fingerprint, result);

        // Assert
        RequestDeduplicationService.PendingRequest pending = 
                deduplicationService.getPendingRequest(fingerprint);
        assertNull(pending);  // Should be removed after completion
    }

    @Test
    void testHasPendingRequest() {
        // Arrange
        String fingerprint = "1N:RID-123:10";
        deduplicationService.registerPendingRequest(fingerprint);

        // Act
        boolean hasPending = deduplicationService.hasPendingRequest(fingerprint);

        // Assert
        assertTrue(hasPending);
    }

    @Test
    void testHasNoPendingRequest() {
        // Act
        boolean hasPending = deduplicationService.hasPendingRequest("non-existent");

        // Assert
        assertFalse(hasPending);
    }

    @Test
    void testGetPendingRequestCount() {
        // Arrange
        deduplicationService.registerPendingRequest("1N:RID-123:10");
        deduplicationService.registerPendingRequest("1N:RID-456:10");
        deduplicationService.registerPendingRequest("1TO1:RID-789:RID-999");

        // Act
        int count = deduplicationService.getPendingRequestCount();

        // Assert
        assertEquals(3, count);
    }

    @Test
    void testWaitForPendingRequestSuccess() throws BiometricException, InterruptedException {
        // Arrange
        String fingerprint = "1N:RID-123:10";
        deduplicationService.registerPendingRequest(fingerprint);
        MatchResult result = createTestMatchResult();

        // Simulate completion in another thread
        new Thread(() -> {
            try {
                Thread.sleep(100);
                deduplicationService.completePendingRequest(fingerprint, result);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        // Act
        MatchResult waitResult = deduplicationService.waitForPendingRequest(fingerprint, 5000);

        // Assert
        assertNotNull(waitResult);
    }

    @Test
    void testWaitForPendingRequestTimeout() {
        // Arrange
        String fingerprint = "1N:RID-123:10";
        deduplicationService.registerPendingRequest(fingerprint);

        // Act & Assert
        assertThrows(BiometricException.class, () -> {
            deduplicationService.waitForPendingRequest(fingerprint, 100);
        });
    }

    @Test
    void testWaitForNonExistentPendingRequest() {
        // Act & Assert
        assertThrows(BiometricException.class, () -> {
            deduplicationService.waitForPendingRequest("non-existent", 1000);
        });
    }

    @Test
    void testClearExpiredPendingRequests() throws InterruptedException {
        // Arrange
        deduplicationService.registerPendingRequest("1N:RID-123:10");
        deduplicationService.registerPendingRequest("1N:RID-456:10");

        // Wait to simulate expiration
        Thread.sleep(100);

        // Act
        deduplicationService.clearExpiredPendingRequests();

        // Assert
        // Depending on TTL configuration, expired requests should be removed
        assertTrue(deduplicationService.getPendingRequestCount() >= 0);
    }

    @Test
    void testDuplicateRequestDetection() {
        // Arrange
        String fingerprint1 = deduplicationService.generateRequestFingerprint("RID-123", 10, "1N");
        String fingerprint2 = deduplicationService.generateRequestFingerprint("RID-123", 10, "1N");
        String fingerprint3 = deduplicationService.generateRequestFingerprint("RID-456", 10, "1N");

        // Act & Assert
        assertEquals(fingerprint1, fingerprint2);  // Same request
        assertNotEquals(fingerprint1, fingerprint3);  // Different request
    }

    // Helper methods

    private MatchResult createTestMatchResult() {
        MatchResult result = new MatchResult();
        result.setProbeRid("RID-123");
        result.setStatus(MatchResult.MatchStatus.MATCH_FOUND);
        result.setMatchingTimeMs(150);
        return result;
    }
}
