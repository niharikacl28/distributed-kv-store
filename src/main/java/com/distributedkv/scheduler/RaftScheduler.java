
package com.distributedkv.scheduler;

import com.distributedkv.cluster.RaftCluster;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RaftScheduler {

    private final RaftCluster cluster;

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(2);

    public RaftScheduler(RaftCluster cluster) {
        this.cluster = cluster;
    }

    // Start automatic heartbeat and election checking
    public void start() {

        System.out.println("\n===== RAFT SCHEDULER STARTED =====");

        // Send heartbeats every 100 milliseconds
        scheduler.scheduleAtFixedRate(
                cluster::sendHeartbeats,
                0,
                100,
                TimeUnit.MILLISECONDS
        );

        // Check election timeouts every 50 milliseconds
        scheduler.scheduleAtFixedRate(
                cluster::checkElectionTimeouts,
                0,
                50,
                TimeUnit.MILLISECONDS
        );
    }

    // Stop scheduled tasks
    public void stop() {

        scheduler.shutdownNow();

        System.out.println("\n===== RAFT SCHEDULER STOPPED =====");
    }
}
