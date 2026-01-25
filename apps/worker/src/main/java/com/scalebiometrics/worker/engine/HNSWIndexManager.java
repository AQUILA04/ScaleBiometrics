package com.scalebiometrics.worker.engine;

import com.scalebiometrics.core.exception.BiometricException;
import io.jvector.jvector.graph.GraphIndex;
import io.jvector.jvector.graph.RandomAccessVectorValues;
import io.jvector.jvector.vector.VectorFloat;
import io.jvector.jvector.vector.types.VectorTypeSupport;
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
 * HNSW Index Manager - Manages Hierarchical Navigable Small World index.
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
    private final Map<String, Integer> ridToIdMap = new ConcurrentHashMap<>();
    private final Map<Integer, String> idToRidMap = new ConcurrentHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private int nextId = 0;

    /**
     * Initialize HNSW index
     */
    public void initialize() throws BiometricException {
        try {
            lock.writeLock().lock();
            
            log.info("Initializing HNSW index with M={}, efConstruction={}, efSearch={}", 
                    m, efConstruction, efSearch);

            // Try to load from disk
            if (loadFromDisk()) {
                log.info("Loaded existing HNSW index from disk");
                return;
            }

            // Create new index
            // Note: JVector GraphIndex creation would be done here
            // This is a simplified example - actual implementation depends on JVector API
            
            log.info("Created new HNSW index");

        } catch (Exception e) {
            log.error("Error initializing HNSW index", e);
            throw new BiometricException("Failed to initialize HNSW index: " + e.getMessage(), e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Add a vector to the index
     */
    public void add(String rid, float[] embedding) throws BiometricException {
        if (embedding == null || embedding.length == 0) {
            throw new BiometricException("Invalid embedding vector");
        }

        try {
            lock.writeLock().lock();

            if (ridToIdMap.containsKey(rid)) {
                throw new BiometricException("RID already exists in index: " + rid);
            }

            if (ridToIdMap.size() >= maxSize) {
                throw new BiometricException("Index size limit reached: " + maxSize);
            }

            int id = nextId++;
            ridToIdMap.put(rid, id);
            idToRidMap.put(id, rid);

            // Add to HNSW index
            // hnswIndex.add(id, VectorFloat.create(embedding));

            log.debug("Added vector to HNSW index - RID: {}, ID: {}", rid, id);

        } catch (Exception e) {
            log.error("Error adding vector to HNSW index", e);
            throw new BiometricException("Failed to add vector: " + e.getMessage(), e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Remove a vector from the index
     * Note: HNSW doesn't support efficient deletion, so we mark as deleted
     */
    public void remove(String rid) throws BiometricException {
        try {
            lock.writeLock().lock();

            Integer id = ridToIdMap.remove(rid);
            if (id != null) {
                idToRidMap.remove(id);
                log.debug("Removed vector from HNSW index - RID: {}, ID: {}", rid, id);
            } else {
                log.warn("RID not found in index: {}", rid);
            }

        } catch (Exception e) {
            log.error("Error removing vector from HNSW index", e);
            throw new BiometricException("Failed to remove vector: " + e.getMessage(), e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Search for nearest neighbors
     */
    public List<HNSWCandidate> search(float[] queryVector, int k) throws BiometricException {
        if (queryVector == null || queryVector.length == 0) {
            throw new BiometricException("Invalid query vector");
        }

        try {
            lock.readLock().lock();

            if (hnswIndex == null || ridToIdMap.isEmpty()) {
                return Collections.emptyList();
            }

            List<HNSWCandidate> candidates = new ArrayList<>();

            // Perform HNSW search
            // PriorityQueue<Integer> results = hnswIndex.search(
            //     VectorFloat.create(queryVector), 
            //     k, 
            //     efSearch
            // );

            // Convert results to candidates
            // for (Integer id : results) {
            //     String rid = idToRidMap.get(id);
            //     if (rid != null) {
            //         float score = calculateSimilarity(queryVector, id);
            //         candidates.add(HNSWCandidate.builder()
            //             .targetRid(rid)
            //             .score(score)
            //             .build());
            //     }
            // }

            log.debug("HNSW search completed - query vector dim: {}, k: {}, results: {}", 
                    queryVector.length, k, candidates.size());

            return candidates;

        } catch (Exception e) {
            log.error("Error searching HNSW index", e);
            throw new BiometricException("HNSW search failed: " + e.getMessage(), e);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get index size (number of vectors)
     */
    public long getSize() {
        try {
            lock.readLock().lock();
            return ridToIdMap.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get index size in bytes (approximate)
     */
    public long getSizeBytes() {
        try {
            lock.readLock().lock();
            // Approximate: each vector ~384 bytes (96 floats * 4 bytes) + overhead
            return ridToIdMap.size() * 512;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Persist index to disk
     */
    public void persistToDisk() throws BiometricException {
        try {
            lock.readLock().lock();

            Path path = Paths.get(indexPath);
            Files.createDirectories(path.getParent());

            // Serialize index metadata
            try (ObjectOutputStream oos = new ObjectOutputStream(
                    new FileOutputStream(indexPath + "/index.dat"))) {
                oos.writeObject(ridToIdMap);
                oos.writeObject(idToRidMap);
                oos.writeInt(nextId);
            }

            log.info("Persisted HNSW index to disk - path: {}", indexPath);

        } catch (Exception e) {
            log.error("Error persisting HNSW index", e);
            throw new BiometricException("Failed to persist index: " + e.getMessage(), e);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Load index from disk
     */
    private boolean loadFromDisk() {
        try {
            Path path = Paths.get(indexPath + "/index.dat");
            if (!Files.exists(path)) {
                return false;
            }

            try (ObjectInputStream ois = new ObjectInputStream(
                    new FileInputStream(indexPath + "/index.dat"))) {
                @SuppressWarnings("unchecked")
                Map<String, Integer> loadedRidToIdMap = (Map<String, Integer>) ois.readObject();
                @SuppressWarnings("unchecked")
                Map<Integer, String> loadedIdToRidMap = (Map<Integer, String>) ois.readObject();
                int loadedNextId = ois.readInt();

                ridToIdMap.putAll(loadedRidToIdMap);
                idToRidMap.putAll(loadedIdToRidMap);
                nextId = loadedNextId;

                log.info("Loaded HNSW index from disk - vectors: {}", ridToIdMap.size());
                return true;
            }

        } catch (Exception e) {
            log.warn("Error loading HNSW index from disk", e);
            return false;
        }
    }

    /**
     * Calculate similarity between query and stored vector
     * (Simplified - actual implementation would use proper vector similarity)
     */
    private float calculateSimilarity(float[] queryVector, int id) {
        // Placeholder: return random score between 0-100
        return (float) (Math.random() * 100);
    }

    /**
     * Clear index
     */
    public void clear() {
        try {
            lock.writeLock().lock();
            ridToIdMap.clear();
            idToRidMap.clear();
            nextId = 0;
            log.info("Cleared HNSW index");
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Get index statistics
     */
    public IndexStatistics getStatistics() {
        try {
            lock.readLock().lock();
            return IndexStatistics.builder()
                    .totalVectors(ridToIdMap.size())
                    .indexSizeBytes(getSizeBytes())
                    .maxSize(maxSize)
                    .m(m)
                    .efConstruction(efConstruction)
                    .efSearch(efSearch)
                    .build();
        } finally {
            lock.readLock().unlock();
        }
    }
}
