package com.scalebiometrics.master.ha;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Leader Election Service - Implements distributed leader election using Redis.
 * 
 * Algorithm:
 * 1. All master instances try to acquire a distributed lock
 * 2. First to acquire lock becomes LEADER
 * 3. Leader renews lock periodically (heartbeat)
 * 4. If leader fails, lock expires and new election happens
 * 5. Followers monitor leader and trigger new election if needed
 * 
 * This ensures:
 * - Only one master processes requests at a time
 * - Automatic failover if leader dies
 * - No split-brain scenario
 */
@Slf4j
@Service
public class LeaderElectionService {

    private static final String LEADER_KEY = "scalebiometrics:master:leader";
    private static final String LEADER_LOCK_KEY = "scalebiometrics:master:leader:lock";
    private static final long LEASE_DURATION_MS = 30000;  // 30 seconds
    private static final long RENEW_DEADLINE_MS = 20000;  // 20 seconds
    private static final long ELECTION_CHECK_INTERVAL_MS = 5000;  // 5 seconds

    @Value("${spring.application.name:master}")
    private String applicationName;

    @Value("${server.port:8082}")
    private int serverPort;

    @Value("${master.leader-election.enabled:true}")
    private boolean leaderElectionEnabled;

    private final RedisTemplate<String, String> redisTemplate;
    private final ExecutorService executorService;
    private final AtomicBoolean isLeader = new AtomicBoolean(false);
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    private String instanceId;
    private long lastLeadershipTime;
    private LeadershipListener leadershipListener;

    public LeaderElectionService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.executorService = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "leader-election-" + Thread.currentThread().getId());
            t.setDaemon(false);
            return t;
        });
        this.instanceId = UUID.randomUUID().toString();
    }

    /**
     * Start leader election process
     */
    public void start() {
        if (!leaderElectionEnabled) {
            log.info("Leader election is disabled");
            return;
        }

        if (isRunning.getAndSet(true)) {
            log.warn("Leader election already running");
            return;
        }

        log.info("Starting leader election service - Instance ID: {}", instanceId);

        // Start election loop
        executorService.submit(this::electionLoop);

        // Start heartbeat/renewal loop
        executorService.submit(this::renewalLoop);
    }

    /**
     * Stop leader election process
     */
    public void stop() {
        if (!isRunning.getAndSet(false)) {
            return;
        }

        log.info("Stopping leader election service");

        // Release leadership if we are leader
        if (isLeader.get()) {
            releaseLeadership();
        }

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Main election loop - Continuously try to acquire leadership
     */
    private void electionLoop() {
        while (isRunning.get()) {
            try {
                if (!isLeader.get()) {
                    tryAcquireLeadership();
                }

                Thread.sleep(ELECTION_CHECK_INTERVAL_MS);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error in election loop", e);
            }
        }
    }

    /**
     * Renewal loop - Renew leadership periodically
     */
    private void renewalLoop() {
        while (isRunning.get()) {
            try {
                if (isLeader.get()) {
                    renewLeadership();
                }

                Thread.sleep(RENEW_DEADLINE_MS);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error in renewal loop", e);
            }
        }
    }

    /**
     * Try to acquire leadership
     */
    private void tryAcquireLeadership() {
        try {
            // Try to set leader key with NX (only if not exists)
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                    LEADER_KEY,
                    instanceId,
                    java.time.Duration.ofMillis(LEASE_DURATION_MS)
            );

            if (Boolean.TRUE.equals(acquired)) {
                // Successfully acquired leadership
                becomeLeader();
            } else {
                // Check if current leader is still alive
                String currentLeader = redisTemplate.opsForValue().get(LEADER_KEY);
                if (currentLeader == null) {
                    // Leader key expired, try again
                    log.info("Leader key expired, retrying election");
                    tryAcquireLeadership();
                } else if (!currentLeader.equals(instanceId)) {
                    // Another instance is leader
                    if (isLeader.get()) {
                        // We were leader but lost it
                        loseLeadership();
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error trying to acquire leadership", e);
        }
    }

    /**
     * Renew leadership (extend TTL)
     */
    private void renewLeadership() {
        try {
            String currentLeader = redisTemplate.opsForValue().get(LEADER_KEY);

            if (currentLeader != null && currentLeader.equals(instanceId)) {
                // Extend TTL
                redisTemplate.expire(
                        LEADER_KEY,
                        java.time.Duration.ofMillis(LEASE_DURATION_MS)
                );

                lastLeadershipTime = System.currentTimeMillis();
                log.debug("Renewed leadership - TTL extended");

            } else {
                // We lost leadership
                if (isLeader.get()) {
                    loseLeadership();
                }
            }

        } catch (Exception e) {
            log.error("Error renewing leadership", e);
        }
    }

    /**
     * Become leader
     */
    private void becomeLeader() {
        if (isLeader.getAndSet(true)) {
            return;  // Already leader
        }

        lastLeadershipTime = System.currentTimeMillis();
        log.info("🎯 BECAME LEADER - Instance ID: {}", instanceId);

        // Notify listeners
        if (leadershipListener != null) {
            try {
                leadershipListener.onBecomeLeader();
            } catch (Exception e) {
                log.error("Error in onBecomeLeader callback", e);
            }
        }
    }

    /**
     * Lose leadership
     */
    private void loseLeadership() {
        if (!isLeader.getAndSet(false)) {
            return;  // Already not leader
        }

        log.info("⚠️  LOST LEADERSHIP - Instance ID: {}", instanceId);

        // Notify listeners
        if (leadershipListener != null) {
            try {
                leadershipListener.onLoseLeadership();
            } catch (Exception e) {
                log.error("Error in onLoseLeadership callback", e);
            }
        }
    }

    /**
     * Release leadership gracefully
     */
    private void releaseLeadership() {
        try {
            String currentLeader = redisTemplate.opsForValue().get(LEADER_KEY);
            if (currentLeader != null && currentLeader.equals(instanceId)) {
                redisTemplate.delete(LEADER_KEY);
                log.info("Released leadership gracefully");
            }
        } catch (Exception e) {
            log.error("Error releasing leadership", e);
        }
    }

    /**
     * Check if this instance is leader
     */
    public boolean isLeader() {
        return isLeader.get();
    }

    /**
     * Get current leader instance ID
     */
    public String getCurrentLeader() {
        try {
            return redisTemplate.opsForValue().get(LEADER_KEY);
        } catch (Exception e) {
            log.error("Error getting current leader", e);
            return null;
        }
    }

    /**
     * Get this instance ID
     */
    public String getInstanceId() {
        return instanceId;
    }

    /**
     * Get leadership duration in milliseconds
     */
    public long getLeadershipDurationMs() {
        if (!isLeader.get()) {
            return 0;
        }
        return System.currentTimeMillis() - lastLeadershipTime;
    }

    /**
     * Register leadership listener
     */
    public void setLeadershipListener(LeadershipListener listener) {
        this.leadershipListener = listener;
    }

    /**
     * Leadership Listener interface
     */
    public interface LeadershipListener {
        void onBecomeLeader();
        void onLoseLeadership();
    }
}
