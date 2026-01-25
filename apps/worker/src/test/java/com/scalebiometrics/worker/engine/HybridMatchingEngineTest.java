package com.scalebiometrics.worker.engine;

import com.scalebiometrics.core.domain.Fingerprint;
import com.scalebiometrics.core.domain.MatchResult;
import com.scalebiometrics.core.exception.BiometricException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * HybridMatchingEngine Unit Tests
 */
@ExtendWith(MockitoExtension.class)
class HybridMatchingEngineTest {

    @Mock
    private HNSWIndexManager hnswIndexManager;

    @Mock
    private SourceAFISMatcher sourceAFISMatcher;

    @Mock
    private OffHeapMemoryManager offHeapMemoryManager;

    @Mock
    private MatchingMetrics matchingMetrics;

    private HybridMatchingEngine hybridMatchingEngine;

    @BeforeEach
    void setUp() {
        hybridMatchingEngine = new HybridMatchingEngine(
                hnswIndexManager,
                sourceAFISMatcher,
                offHeapMemoryManager,
                matchingMetrics
        );
    }

    @Test
    void testMatch1NSuccessful() throws BiometricException {
        // Arrange
        Fingerprint probeFingerprint = createTestFingerprint("probe-1");
        int topK = 10;

        // Mock HNSW search results
        when(hnswIndexManager.search(any(), eq(topK)))
                .thenReturn(createTestCandidates(5));

        // Mock SourceAFIS matching
        when(sourceAFISMatcher.match(any(), any()))
                .thenReturn(85);  // 85% match score

        // Act
        MatchResult result = hybridMatchingEngine.match1N(probeFingerprint, topK);

        // Assert
        assertNotNull(result);
        assertEquals("probe-1", result.getProbeRid());
        assertFalse(result.getCandidates().isEmpty());
        assertTrue(result.getMatchingTimeMs() > 0);

        // Verify interactions
        verify(hnswIndexManager, times(1)).search(any(), eq(topK));
        verify(sourceAFISMatcher, atLeast(1)).match(any(), any());
    }

    @Test
    void testMatch1To1Successful() throws BiometricException {
        // Arrange
        Fingerprint probeFingerprint = createTestFingerprint("probe-1");
        Fingerprint targetFingerprint = createTestFingerprint("target-1");

        // Mock SourceAFIS matching
        when(sourceAFISMatcher.match(any(), any()))
                .thenReturn(90);  // 90% match score

        // Act
        MatchResult result = hybridMatchingEngine.match1To1(probeFingerprint, targetFingerprint);

        // Assert
        assertNotNull(result);
        assertEquals("probe-1", result.getProbeRid());
        assertEquals(1, result.getCandidates().size());
        assertTrue(result.getCandidates().get(0).isMatch());

        // Verify interactions
        verify(sourceAFISMatcher, times(1)).match(any(), any());
        verify(matchingMetrics, times(1)).recordMatch1To1(anyLong(), anyBoolean());
    }

    @Test
    void testAddFingerprint() throws BiometricException {
        // Arrange
        Fingerprint fingerprint = createTestFingerprint("fp-1");

        // Act
        hybridMatchingEngine.addFingerprint(fingerprint);

        // Assert
        verify(hnswIndexManager, times(1)).add(any(), any());
        verify(offHeapMemoryManager, times(1)).storeTemplate(anyString(), any());
        verify(matchingMetrics, times(1)).recordIndexAdd();
    }

    @Test
    void testRemoveFingerprint() throws BiometricException {
        // Arrange
        String rid = "fp-1";

        // Act
        hybridMatchingEngine.removeFingerprint(rid);

        // Assert
        verify(hnswIndexManager, times(1)).remove(rid);
        verify(offHeapMemoryManager, times(1)).removeTemplate(rid);
        verify(matchingMetrics, times(1)).recordIndexRemove();
    }

    @Test
    void testMatch1NWithNoResults() throws BiometricException {
        // Arrange
        Fingerprint probeFingerprint = createTestFingerprint("probe-1");

        // Mock HNSW search returns empty
        when(hnswIndexManager.search(any(), anyInt()))
                .thenReturn(java.util.List.of());

        // Act
        MatchResult result = hybridMatchingEngine.match1N(probeFingerprint, 10);

        // Assert
        assertNotNull(result);
        assertTrue(result.getCandidates().isEmpty());
        assertEquals("NO_MATCH", result.getStatus());
    }

    @Test
    void testMatch1NWithInvalidProbe() {
        // Arrange
        Fingerprint invalidProbe = new Fingerprint();
        invalidProbe.setRid(null);  // Invalid

        // Act & Assert
        assertThrows(BiometricException.class, () -> {
            hybridMatchingEngine.match1N(invalidProbe, 10);
        });
    }

    @Test
    void testGetIndexStatistics() {
        // Arrange
        IndexStatistics expectedStats = IndexStatistics.builder()
                .totalVectors(1000000)
                .indexSizeBytes(1024 * 1024 * 100)  // 100MB
                .queryCount(5000)
                .avgQueryTimeMs(75.5)
                .build();

        when(hnswIndexManager.getStatistics())
                .thenReturn(expectedStats);

        // Act
        IndexStatistics stats = hybridMatchingEngine.getIndexStatistics();

        // Assert
        assertNotNull(stats);
        assertEquals(1000000, stats.getTotalVectors());
        assertEquals(5000, stats.getQueryCount());
    }

    // Helper methods

    private Fingerprint createTestFingerprint(String rid) {
        Fingerprint fingerprint = new Fingerprint();
        fingerprint.setRid(rid);
        fingerprint.setFingerIndex(0);
        fingerprint.setBinaryTemplate(new byte[256]);
        fingerprint.setEmbeddingVector(new float[512]);
        fingerprint.setQuality(95);
        fingerprint.setStatus("VALID");
        return fingerprint;
    }

    private java.util.List<HNSWCandidate> createTestCandidates(int count) {
        java.util.List<HNSWCandidate> candidates = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            HNSWCandidate candidate = new HNSWCandidate();
            candidate.setTargetRid("target-" + i);
            candidate.setScore(0.9f - (i * 0.05f));
            candidates.add(candidate);
        }
        return candidates;
    }
}
