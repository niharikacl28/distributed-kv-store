package com.distributedkv.cluster;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RaftClusterWriteTest {

    @Test
    void replicateLogCommitsWithMajorityAndAdvancesCommitIndex() {
        RaftCluster cluster = new RaftCluster(5);
        cluster.startElection("Node-1");

        int before = cluster.getCommitIndex();

        boolean committed =
                cluster.replicateLog("PUT", "name", "Niharika");

        assertTrue(committed);
        assertTrue(cluster.getCommitIndex() > before);
        assertEquals("Niharika", cluster.readFromLeader("name"));
    }

    @Test
    void deleteIsReplicatedAndCommitted() {
        RaftCluster cluster = new RaftCluster(5);
        cluster.startElection("Node-1");
        cluster.replicateLog("PUT", "name", "Niharika");

        boolean committed =
                cluster.replicateLog("DELETE", "name", null);

        assertTrue(committed);
        assertNull(cluster.readFromLeader("name"));
    }

    @Test
    void replicateLogReturnsFalseWithoutLeader() {
        RaftCluster cluster = new RaftCluster(5);

        assertFalse(cluster.replicateLog("PUT", "name", "Niharika"));
    }

    @Test
    void replicateLogReturnsFalseWithoutQuorum() {
        RaftCluster cluster = new RaftCluster(5);
        cluster.startElection("Node-1");

        cluster.crashNode("Node-2");
        cluster.crashNode("Node-3");
        cluster.crashNode("Node-4");

        int before = cluster.getCommitIndex();

        boolean committed =
                cluster.replicateLog("PUT", "name", "Niharika");

        assertFalse(committed);
        assertEquals(before, cluster.getCommitIndex());
        assertNull(cluster.readFromLeader("name"));
    }
}
