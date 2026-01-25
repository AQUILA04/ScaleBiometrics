package com.scalebiometrics.worker.engine;

import com.scalebiometrics.core.domain.Fingerprint;
import com.scalebiometrics.core.exception.BiometricException;
import io.github.jbellis.jvector.graph.GraphIndex;
import io.github.jbellis.jvector.graph.RandomAccessVectorValues;
import io.github.jbellis.jvector.quantization.ScalarQuantizer;
import io.github.jbellis.jvector.vector.VectorFloat;
import io.github.jbellis.jvector.vector.VectorSimilarityFunction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * HNSW Index Manager - Manages Hierarchical Navigable Small World index using JVector 3.0.6.
 * 
 * JVector Features:
 * - Pure Java implementation with Vector API optimization for Java 21
 * - SIMD acceleration via Vector API
 * - Efficient off-heap memory support
 * - Used in production by Apache Cassandra
 * 
 * Responsibilities:
 * - Create and maintain HNSW index for approximate nearest neighbor search
 * - Support add/remove operations with thread-safe locking
 * - Persist index to disk for recovery
 * - Track index statistics and performance metrics
 */
@Slf4j
@Component
public class HNSWIndexManager {

    private static final int DEFAULT_M = 16;
    private static final int DEFAULT_EF_CONSTRUCTION = 200;
    private static final int DEFAULT_EF_SEARCH = 100;
    private static final int VECTOR_DIMENSION = 128;  // Standard fingerprint embedding dimension

    @Value("${worker.hnsw.m:16}")
    private int m;

    @Value("${worker.hnsw.ef-construction:200}")
    private int efConstruction;

    @Value("${worker.hnsw.ef-search:100}")
    private int efSearch;

    @Value("${worker.hnsw.max-size:10000000}")
    private int maxSize;

    @Value("${worker.data.index-path:/tmp/hnsw-index}")
    private String indexPath;

    private GraphIndex<float[]> hnswIndex;
    private RandomAccessVectorValues vectorValues;
    private final Map<String, Integer> ridToIdMap = new ConcurrentHashMap<>();
    private final Map<Integer, String> idToRidMap = new ConcurrentHashMap<>();
    private final Map<Integer, Fingerprint> fingerprintCache = new ConcurrentHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private int nextId = 0;
    private IndexStatistics statistics;

    /**
     * Initialize HNSW index using JVector
     */
    public void initialize() throws BiometricException {
        try {
            lock.writeLock().lock();
            
            log.info("Initializing JVector HNSW index with M={}, efConstruction={}, efSearch={}", 
                    m, efConstruction, efSearch);

            // Try to load from disk
            if (loadFromDisk()) {
                log.info("Loaded existing HNSW index from disk");
                return;
            }

            // Create new index using JVector
            // JVector uses VectorSimilarityFunction.EUCLIDEAN for L2 distance
            vectorValues = new RandomAccessVectorValues.FloatMultiVector(
                    new float[maxSize][VECTOR_DIMENSION]);
            
            hnswIndex = new GraphIndex<>(
                    GraphIndex.EMPTY_GRAPH,
                    vectorValues,
                    VectorSimilarityFunction.EUCLIDEAN,
                    m,
                    efConstruction,
                    efSearch,
                    1.2f  // growthFactor
            );

            statistics = new IndexStatistics();
            log.info("Created new JVector HNSW index with max size: {}", maxSize);

        } catch (Exception e) {
            log.error("Error initializing HNSW index", e);
            throw new BiometricException("Failed to initialize HNSW index: " + e.getMessage(), e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Add fingerprint to index
     */
    public void addFingerprint(Fingerprint fingerprint) throws BiometricException {
        try {
            lock.writeLock().lock();

            if (hnswIndex == null) {
                throw new BiometricException("HNSW index not initialized");
            }

            String rid = fingerprint.getRid();
            if (ridToIdMap.containsKey(rid)) {
                log.warn("Fingerprint already exists in index: {}", rid);
                return;
            }

            int id = nextId++;
            float[] embedding = fingerprint.getEmbedding();

            if (embedding == null || embedding.length != VECTOR_DIMENSION) {
                throw new BiometricException("Invalid embedding dimension: expected " + 
                        VECTOR_DIMENSION + ", got " + (embedding == null ? 0 : embedding.length));
            }

            // Add to HNSW index using JVector
            hnswIndex.addGraphNode(id, VectorFloat.create(embedding));

            // Update mappings
            ridToIdMap.put(rid, id);
            idToRidMap.put(id, rid);
            fingerprintCache.put(id, fingerprint);

            // Update statistics
            statistics.incrementTotalVectors();
            statistics.setIndexSizeBytes(estimateIndexSize());

            log.debug("Added fingerprint to index: {} (id={})", rid, id);

        } catch (Exception e) {
            log.error("Error adding fingerprint to index", e);
            throw new BiometricException("Failed to add fingerprint: " + e.getMessage(), e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Remove fingerprint from index
     */
    public void removeFingerprint(String rid) throws BiometricException {
        try {
            lock.writeLock().lock();

            if (hnswIndex == null) {
                throw new BiometricException("HNSW index not initialized");
            }

            Integer id = ridToIdMap.get(rid);
            if (id == null) {
                log.warn("Fingerprint not found in index: {}", rid);
                return;
            }

            // Note: JVector doesn't support removal directly from graph
            // Mark as deleted in our mapping
            ridToIdMap.remove(rid);
            idToRidMap.remove(id);
            fingerprintCache.remove(id);

            // Update statistics
            statistics.decrementTotalVectors();
            statistics.setIndexSizeBytes(estimateIndexSize());

            log.debug("Removed fingerprint from index: {}", rid);

        } catch (Exception e) {
            log.error("Error removing fingerprint from index", e);
            throw new BiometricException("Failed to remove fingerprint: " + e.getMessage(), e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Search for nearest neighbors using JVector
     */
    public List<HNSWCandidate> search(float[] embedding, int topK) throws BiometricException {
        try {
            lock.readLock().lock();

            if (hnswIndex == null) {
                throw new BiometricException("HNSW index not initialized");
            }

            if (embedding == null || embedding.length != VECTOR_DIMENSION) {
                throw new BiometricException("Invalid embedding dimension");
            }

            // Search in HNSW index using JVector
            VectorFloat queryVector = VectorFloat.create(embedding);
            var results = hnswIndex.search(queryVector, topK, efSearch);

            // Convert results to candidates
            List<HNSWCandidate> candidates = new ArrayList<>();
            for (var result : results) {
                int id = result.node;
                String rid = idToRidMap.get(id);
                if (rid != null) {
                    // Calculate similarity score (0-100)
                    float score = calculateSimilarityScore(embedding, id);
                    candidates.add(HNSWCandidate.builder()
                            .rid(rid)
                            .id(id)
                            .score((int) score)
                            .build());
                }
            }

            return candidates;

        } catch (Exception e) {
            log.error("Error searching HNSW index", e);
            throw new BiometricException("HNSW search failed: " + e.getMessage(), e);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get fingerprint by RID
     */
    public Fingerprint getFingerprint(String rid) {
        try {
            lock.readLock().lock();

            Integer id = ridToIdMap.get(rid);
            if (id == null) {
                return null;
            }

            return fingerprintCache.get(id);

        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get index statistics
     */
    public IndexStatistics getStatistics() {
        try {
            lock.readLock().lock();
            return statistics;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Persist index to disk
     */
    public void saveToDisk() throws BiometricException {
        try {
            lock.readLock().lock();

            if (hnswIndex == null) {
                throw new BiometricException("HNSW index not initialized");
            }

            Path path = Paths.get(indexPath);
            Files.createDirectories(path);

            // Save index using JVector serialization
            String indexFile = path.resolve("hnsw.index").toString();
            hnswIndex.save(indexFile);

            // Save mappings
            String mappingsFile = path.resolve("mappings.dat").toString();
            try (ObjectOutputStream oos = new ObjectOutputStream(
                    new FileOutputStream(mappingsFile))) {
                oos.writeObject(ridToIdMap);
                oos.writeObject(idToRidMap);
                oos.writeInt(nextId);
            }

            log.info("HNSW index saved to disk: {}", indexPath);

        } catch (Exception e) {
            log.error("Error saving HNSW index to disk", e);
            throw new BiometricException("Failed to save index: " + e.getMessage(), e);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Load index from disk
     */
    private boolean loadFromDisk() throws BiometricException {
        try {
            Path path = Paths.get(indexPath);
            if (!Files.exists(path)) {
                return false;
            }

            String indexFile = path.resolve("hnsw.index").toString();
            String mappingsFile = path.resolve("mappings.dat").toString();

            if (!Files.exists(Paths.get(indexFile)) || !Files.exists(Paths.get(mappingsFile))) {
                return false;
            }

            // Load index using JVector deserialization
            hnswIndex = GraphIndex.load(indexFile);

            // Load mappings
            try (ObjectInputStream ois = new ObjectInputStream(
                    new FileInputStream(mappingsFile))) {
                ridToIdMap.putAll((Map<String, Integer>) ois.readObject());
                idToRidMap.putAll((Map<Integer, String>) ois.readObject());
                nextId = ois.readInt();
            }

            statistics = new IndexStatistics();
            statistics.setTotalVectors(ridToIdMap.size());
            statistics.setIndexSizeBytes(estimateIndexSize());

            log.info("Loaded HNSW index from disk with {} vectors", ridToIdMap.size());
            return true;

        } catch (Exception e) {
            log.warn("Could not load HNSW index from disk", e);
            return false;
        }
    }

    /**
     * Calculate similarity score between embedding and indexed vector
     * Uses L2 distance converted to similarity score (0-100)
     */
    private float calculateSimilarityScore(float[] embedding, int id) {
        try {
            // Get indexed vector from JVector
            float[] indexedVector = vectorValues.get(id).toArray();
            if (indexedVector == null) {
                return 0.0f;
            }

            // Calculate L2 distance
            float distance = 0.0f;
            for (int i = 0; i < embedding.length; i++) {
                float diff = embedding[i] - indexedVector[i];
                distance += diff * diff;
            }
            distance = (float) Math.sqrt(distance);

            // Convert distance to similarity score (0-100)
            // Normalize: max distance = sqrt(128) ≈ 11.3
            float maxDistance = (float) Math.sqrt(VECTOR_DIMENSION);
            float similarity = Math.max(0, 100 - (distance / maxDistance * 100));

            return similarity;

        } catch (Exception e) {
            log.warn("Error calculating similarity score", e);
            return 0.0f;
        }
    }

    /**
     * Estimate index size in bytes
     */
    private long estimateIndexSize() {
        // Rough estimation: each vector takes ~512 bytes (128 floats * 4 bytes)
        // Plus index overhead and graph structure
        return ridToIdMap.size() * 512L + 10 * 1024 * 1024;  // +10MB for overhead
    }

    /**
     * Shutdown index manager
     */
    public void shutdown() {
        try {
            lock.writeLock().lock();
            if (hnswIndex != null) {
                saveToDisk();
                hnswIndex = null;
            }
            log.info("HNSW index manager shutdown");
        } catch (Exception e) {
            log.error("Error during shutdown", e);
        } finally {
            lock.writeLock().unlock();
        }
    }
}
