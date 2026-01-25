package com.scalebiometrics.worker.integration;

import com.scalebiometrics.core.domain.Fingerprint;
import com.scalebiometrics.core.domain.MatchResult;
import com.scalebiometrics.core.exception.BiometricException;
import com.scalebiometrics.worker.engine.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Matching Engine Integration Tests
 */
@SpringBootTest
@ActiveProfiles("test")
class MatchingEngineIntegrationTest {

    @Autowired
    private HybridMatchingEngine hybridMatchingEngine;

    @Autowired
    private HNSWIndexManager hnswIndexManager;

    @Autowired
    private SourceAFISMatcher sourceAFISMatcher;

    @Autowired
    private OffHeapMemoryManager offHeapMemoryManager;

    @BeforeEach
    void setUp() throws BiometricException {
        // Clear index before each test
        hnswIndexManager.clear();
        offHeapMemoryManager.clear();

        // Add test fingerprints
        for (int i = 0; i < 100; i++) {
            Fingerprint fp = createTestFingerprint("RID-" + i);
            hybridMatchingEngine.addFingerprint(fp);
        }
    }

    @Test
    void testMatch1NIntegration() throws BiometricException {
        // Arrange
        Fingerprint probeFingerprint = createTestFingerprint("probe-1");

        // Act
        MatchResult result = hybridMatchingEngine.match1N(probeFingerprint, 10);

        // Assert
        assertNotNull(result);
        assertEquals("probe-1", result.getProbeRid());
        assertTrue(result.getMatchingTimeMs() > 0);
        assertTrue(result.getMatchingTimeMs() < 2000);  // Should be < 2s
    }

    @Test
    void testMatch1To1Integration() throws BiometricException {
        // Arrange
        Fingerprint probeFingerprint = createTestFingerprint("probe-1");
        Fingerprint targetFingerprint = createTestFingerprint("RID-0");

        // Act
        MatchResult result = hybridMatchingEngine.match1To1(probeFingerprint, targetFingerprint);

        // Assert
        assertNotNull(result);
        assertEquals("probe-1", result.getProbeRid());
        assertEquals(1, result.getCandidates().size());
    }

    @Test
    void testAddAndRemoveFingerprint() throws BiometricException {
        // Arrange
        Fingerprint fingerprint = createTestFingerprint("test-fp");

        // Act
        hybridMatchingEngine.addFingerprint(fingerprint);
        IndexStatistics statsAfterAdd = hybridMatchingEngine.getIndexStatistics();
        long countAfterAdd = statsAfterAdd.getTotalVectors();

        hybridMatchingEngine.removeFingerprint("test-fp");
        IndexStatistics statsAfterRemove = hybridMatchingEngine.getIndexStatistics();
        long countAfterRemove = statsAfterRemove.getTotalVectors();

        // Assert
        assertTrue(countAfterAdd > countAfterRemove);
    }

    @Test
    void testIndexStatistics() throws BiometricException {
        // Act
        IndexStatistics stats = hybridMatchingEngine.getIndexStatistics();

        // Assert
        assertNotNull(stats);
        assertTrue(stats.getTotalVectors() > 0);
        assertTrue(stats.getIndexSizeBytes() > 0);
        assertTrue(stats.getQueryCount() >= 0);
    }

    @Test
    void testMemoryStatistics() throws BiometricException {
        // Act
        MemoryStatistics stats = hybridMatchingEngine.getMemoryStatistics();

        // Assert
        assertNotNull(stats);
        assertTrue(stats.getUsedMemoryBytes() >= 0);
        assertTrue(stats.getMaxMemoryBytes() > 0);
        assertTrue(stats.getUsagePercentage() >= 0);
    }

    @Test
    void testPerformanceUnder2Seconds() throws BiometricException {
        // Arrange
        Fingerprint probeFingerprint = createTestFingerprint("probe-perf");

        // Act
        long startTime = System.currentTimeMillis();
        MatchResult result = hybridMatchingEngine.match1N(probeFingerprint, 10);
        long endTime = System.currentTimeMillis();

        // Assert
        long totalTime = endTime - startTime;
        assertTrue(totalTime < 2000, "Matching should complete in < 2 seconds");
        assertEquals("probe-perf", result.getProbeRid());
    }

    @Test
    void testConcurrentMatching() throws BiometricException, InterruptedException {
        // Arrange
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        boolean[] results = new boolean[threadCount];

        // Act
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    Fingerprint probe = createTestFingerprint("probe-" + index);
                    MatchResult result = hybridMatchingEngine.match1N(probe, 10);
                    results[index] = result != null && result.getMatchingTimeMs() < 2000;
                } catch (BiometricException e) {
                    results[index] = false;
                }
            });
            threads[i].start();
        }

        // Wait for all threads
        for (Thread thread : threads) {
            thread.join();
        }

        // Assert
        for (boolean result : results) {
            assertTrue(result);
        }
    }

    // Helper methods

    private Fingerprint createTestFingerprint(String rid) {
        Fingerprint fingerprint = new Fingerprint();
        fingerprint.setRid(rid);
        fingerprint.setFingerIndex(0);
        fingerprint.setBinaryTemplate(new byte[256]);
        fingerprint.setEmbeddingVector(createRandomVector(512));
        fingerprint.setQuality(95);
        fingerprint.setStatus("VALID");
        return fingerprint;
    }

    private float[] createRandomVector(int size) {
        float[] vector = new float[size];
        for (int i = 0; i < size; i++) {
            vector[i] = (float) Math.random();
        }
        return vector;
    }
}
