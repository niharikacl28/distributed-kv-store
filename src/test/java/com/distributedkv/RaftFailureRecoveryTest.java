package com.distributedkv;

import com.distributedkv.cluster.RaftCluster;
import com.distributedkv.node.RaftNode;
import com.distributedkv.scheduler.RaftScheduler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RaftFailureRecoveryTest {

    @Test
    void recoverySynchronizesMissingLogEntries() {
        RaftCluster cluster = new RaftCluster(5);
        cluster.startElection("Node-1");

        // A follower goes down and misses a committed write.
        cluster.crashNode("Node-2");
        assertTrue(cluster.replicateLog("PUT", "project", "Raft"));
        assertNull(cluster.readFromNode("Node-2", "project"));

        // Recovering the node restores it from the leader's log.
        cluster.recoverNode("Node-2");
        assertEquals("Raft", cluster.readFromNode("Node-2", "project"));
    }

    @Test
    void crashedLeaderTriggersAutomaticReElectionAndRecovery()
            throws Exception {

        RaftCluster cluster = new RaftCluster(5);
        RaftScheduler scheduler = new RaftScheduler(cluster);
        scheduler.start();

        try {
            RaftNode initialLeader = waitForLeader(cluster, null, 10_000);
            assertNotNull(initialLeader, "Initial leader was not elected");

            assertTrue(cluster.replicateLog("PUT", "name", "Niharika"));

            String oldLeaderId = initialLeader.getNodeId();
            cluster.crashNode(oldLeaderId);
            assertNull(cluster.getLeader());

            // The scheduler must detect the failure and elect a new leader.
            RaftNode newLeader = waitForLeader(cluster, oldLeaderId, 10_000);
            assertNotNull(newLeader, "No re-election after leader crash");
            assertNotEquals(oldLeaderId, newLeader.getNodeId());

            // The committed value survives the failover.
            assertEquals("Niharika", cluster.readFromLeader("name"));

            // Recovering the old leader re-synchronizes its log.
            cluster.recoverNode(oldLeaderId);
            assertEquals("Niharika", cluster.readFromNode(oldLeaderId, "name"));

        } finally {
            scheduler.stop();
        }
    }

    private RaftNode waitForLeader(
            RaftCluster cluster,
            String previousLeaderId,
            long timeoutMillis) throws InterruptedException {

        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < timeoutMillis) {

            RaftNode leader = cluster.getLeader();

            if (leader != null
                    && !leader.getNodeId().equals(previousLeaderId)) {

                return leader;
            }

            Thread.sleep(100);
        }

        return null;
    }
}
