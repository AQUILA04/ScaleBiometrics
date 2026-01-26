package com.scalebiometrics.worker.engine;

import com.scalebiometrics.core.exception.BiometricException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.lang.foreign.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * OffHeapMemoryManager - Manages off-heap memory for biometric templates.
 * 
 * Uses Java 21 Foreign Function & Memory API (Panama) for efficient off-heap allocation.
 * 
 * Responsibilities:
 * - Allocate and manage off-heap memory for templates
 * - Provide efficient storage for binary templates
 * - Track memory usage and implement eviction policies
 * - Support serialization/deserialization
 */
@Slf4j
@Component
public class OffHeapMemoryManager {

    @Value("${worker.offheap.max-size-gb:8}")
    private int maxSizeGb;

    @Value("${worker.offheap.arena-size-mb:512}")
    private int arenaSizeMb;

    private final Map<String, OffHeapTemplate> templates = new ConcurrentHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private long totalAllocatedBytes = 0;
    private long maxAllocatedBytes;

    private Arena arena;

    /**
     * Initialize off-heap memory manager
     */
    public void initialize() throws BiometricException {
        try {
            lock.writeLock().lock();

            maxAllocatedBytes = (long) maxSizeGb * 1024 * 1024 * 1024;
            
            // Create arena for off-heap allocation
            // Note: In Java 21+, Arena is used for memory management
            arena = Arena.ofConfined();

            log.info("Initialized OffHeapMemoryManager - Max size: {}GB, Arena size: {}MB", 
                    maxSizeGb, arenaSizeMb);

        } catch (Exception e) {
            log.error("Error initializing OffHeapMemoryManager", e);
            throw new BiometricException("Failed to initialize off-heap memory: " + e.getMessage(), "OFFHEAP_INIT_ERROR", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Store a biometric template in off-heap memory
     */
    public void storeTemplate(String rid, byte[] binaryTemplate) throws BiometricException {
        if (binaryTemplate == null || binaryTemplate.length == 0) {
            throw new BiometricException("Invalid binary template", "INVALID_TEMPLATE");
        }

        try {
            lock.writeLock().lock();

            // Check if already stored
            if (templates.containsKey(rid)) {
                throw new BiometricException("Template already stored for RID: " + rid, "TEMPLATE_ALREADY_EXISTS");
            }

            long templateSize = binaryTemplate.length;

            // Check memory limit
            if (totalAllocatedBytes + templateSize > maxAllocatedBytes) {
                // Try to evict least recently used templates
                if (!evictLRU(templateSize)) {
                    throw new BiometricException("Off-heap memory limit reached", "MEMORY_LIMIT_EXCEEDED");
                }
            }

            // Allocate off-heap memory using MemorySegment
            MemorySegment segment = arena.allocateArray(ValueLayout.JAVA_BYTE, binaryTemplate.length);
            MemorySegment.copy(binaryTemplate, 0, segment, ValueLayout.JAVA_BYTE, 0, binaryTemplate.length);

            OffHeapTemplate template = OffHeapTemplate.builder()
                    .rid(rid)
                    .segment(segment)
                    .size(templateSize)
                    .createdAt(System.currentTimeMillis())
                    .lastAccessedAt(System.currentTimeMillis())
                    .accessCount(0)
                    .build();

            templates.put(rid, template);
            totalAllocatedBytes += templateSize;

            log.debug("Stored template in off-heap memory - RID: {}, Size: {}B, Total: {}B", 
                    rid, templateSize, totalAllocatedBytes);

        } catch (Exception e) {
            log.error("Error storing template in off-heap memory", e);
            throw new BiometricException("Failed to store template: " + e.getMessage(), "STORE_TEMPLATE_ERROR", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Retrieve a biometric template from off-heap memory
     */
    public byte[] getTemplate(String rid) throws BiometricException {
        try {
            lock.readLock().lock();

            OffHeapTemplate template = templates.get(rid);
            if (template == null) {
                return null;
            }

            // Update access statistics
            template.setLastAccessedAt(System.currentTimeMillis());
            template.setAccessCount(template.getAccessCount() + 1);

            // Copy from off-heap to heap
            byte[] result = new byte[(int) template.getSize()];
            MemorySegment.copy(template.getSegment(), ValueLayout.JAVA_BYTE, 0, result, 0, result.length);

            return result;

        } catch (Exception e) {
            log.error("Error retrieving template from off-heap memory", e);
            throw new BiometricException("Failed to retrieve template: " + e.getMessage(), "GET_TEMPLATE_ERROR", e);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Remove a template from off-heap memory
     */
    public void removeTemplate(String rid) throws BiometricException {
        try {
            lock.writeLock().lock();

            OffHeapTemplate template = templates.remove(rid);
            if (template != null) {
                totalAllocatedBytes -= template.getSize();
                // Memory is automatically freed when segment is garbage collected
                log.debug("Removed template from off-heap memory - RID: {}, Size: {}B", 
                        rid, template.getSize());
            }

        } catch (Exception e) {
            log.error("Error removing template from off-heap memory", e);
            throw new BiometricException("Failed to remove template: " + e.getMessage(), "REMOVE_TEMPLATE_ERROR", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Get total used off-heap memory
     */
    public long getUsedMemory() {
        try {
            lock.readLock().lock();
            return totalAllocatedBytes;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get number of stored templates
     */
    public long getTemplateCount() {
        return templates.size();
    }

    /**
     * Evict least recently used templates to free up space
     */
    private boolean evictLRU(long requiredSpace) {
        try {
            List<OffHeapTemplate> sortedTemplates = templates.values().stream()
                    .sorted(Comparator.comparingLong(OffHeapTemplate::getLastAccessedAt))
                    .toList();

            long freedSpace = 0;
            for (OffHeapTemplate template : sortedTemplates) {
                if (freedSpace >= requiredSpace) {
                    break;
                }

                templates.remove(template.getRid());
                totalAllocatedBytes -= template.getSize();
                freedSpace += template.getSize();

                log.debug("Evicted template (LRU) - RID: {}, Size: {}B", 
                        template.getRid(), template.getSize());
            }

            return freedSpace >= requiredSpace;

        } catch (Exception e) {
            log.error("Error during LRU eviction", e);
            return false;
        }
    }

    /**
     * Clear all templates
     */
    public void clear() {
        try {
            lock.writeLock().lock();
            templates.clear();
            totalAllocatedBytes = 0;
            log.info("Cleared all templates from off-heap memory");
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Get memory statistics
     */
    public MemoryStatistics getStatistics() {
        try {
            lock.readLock().lock();
            return MemoryStatistics.builder()
                    .totalAllocatedBytes(totalAllocatedBytes)
                    .maxAllocatedBytes(maxAllocatedBytes)
                    .usagePercentage((double) totalAllocatedBytes / maxAllocatedBytes * 100)
                    .templateCount(templates.size())
                    .averageTemplateSize(templates.isEmpty() ? 0 : totalAllocatedBytes / templates.size())
                    .build();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Shutdown memory manager
     */
    public void shutdown() {
        try {
            lock.writeLock().lock();
            if (arena != null) {
                arena.close();
            }
            templates.clear();
            totalAllocatedBytes = 0;
            log.info("OffHeapMemoryManager shutdown complete");
        } catch (Exception e) {
            log.error("Error shutting down OffHeapMemoryManager", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Inner class for off-heap template metadata
     */
    @lombok.Data
    @lombok.Builder
    public static class OffHeapTemplate {
        private String rid;
        private MemorySegment segment;
        private long size;
        private long createdAt;
        private long lastAccessedAt;
        private int accessCount;
    }
}
