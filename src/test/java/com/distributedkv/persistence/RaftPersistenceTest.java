package com.distributedkv.persistence;

import com.distributedkv.cluster.RaftCluster;
import com.distributedkv.node.RaftNode;
import com.distributedkv.node.RaftRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that durable Raft state survives a complete cluster restart and
 * that recovery never applies uncommitted entries or restores leadership.
 */
class RaftPersistenceTest {

    @TempDir
    Path stateDirectory;

    @Test
    void committedDataSurvivesCompleteRestart() {

        RaftCluster first = new RaftCluster(5, stateDirectory);
        first.startElection("Node-1");

        assertTrue(first.replicateLog("PUT", "project", "Raft"));
        assertEquals("Raft", first.readFromLeader("project"));

        // Simulate a full JVM/cluster restart against the same directory.
        RaftCluster restarted = new RaftCluster(5, stateDirectory);

        // The committed entry is restored before any election happens.
        assertEquals(0, restarted.getCommitIndex());

        // A fresh election is required after a restart.
        restarted.startElection("Node-1");
        assertNotNull(restarted.getLeader());
        assertEquals("Raft", restarted.readFromLeader("project"));
    }

    @Test
    void uncommittedEntriesAreNotAppliedAfterRestart() {

        RaftCluster first = new RaftCluster(5, stateDirectory);
        first.startElection("Node-1");

        // Break quorum so the entry can never be committed.
        first.crashNode("Node-2");
        first.crashNode("Node-3");
        first.crashNode("Node-4");

        assertFalse(first.replicateLog("PUT", "project", "Raft"));
        assertNull(first.readFromLeader("project"));

        // Restart: the entry is durable in the log but must not be applied.
        RaftCluster restarted = new RaftCluster(5, stateDirectory);

        assertEquals(-1, restarted.getCommitIndex());

        // The uncommitted entry is present in the leader's log ...
        assertEquals(1, restarted.getNodes().get(0).getLogEntries().size());

        // ... but was never applied to the key-value store.
        assertNull(restarted.readFromNode("Node-1", "project"));
    }

    @Test
    void currentTermAndVotedForSurviveRestart() {

        RaftCluster first = new RaftCluster(5, stateDirectory);
        first.startElection("Node-1");

        RaftNode leader = first.getLeader();
        assertNotNull(leader);

        long term = leader.getCurrentTerm();
        assertTrue(term > 0);

        RaftCluster restarted = new RaftCluster(5, stateDirectory);

        for (RaftNode node : restarted.getNodes()) {
            assertEquals(term, node.getCurrentTerm());
            assertEquals("Node-1", node.getVotedFor());
        }
    }

    @Test
    void restartedClusterElectsNewLeader() {

        RaftCluster first = new RaftCluster(5, stateDirectory);
        first.startElection("Node-1");
        assertNotNull(first.getLeader());

        RaftCluster restarted = new RaftCluster(5, stateDirectory);

        // Leadership is never restored; every node returns as a follower.
        assertNull(restarted.getLeader());
        for (RaftNode node : restarted.getNodes()) {
            assertEquals(RaftRole.FOLLOWER, node.getRole());
        }

        // A fresh election succeeds and produces a new leader.
        restarted.startElection("Node-3");

        RaftNode newLeader = restarted.getLeader();
        assertNotNull(newLeader);
        assertEquals("Node-3", newLeader.getNodeId());
        assertTrue(newLeader.getCurrentTerm() > 1);
    }

    @Test
    void corruptPersistenceFileIsHandledSafely() throws IOException {

        Path corrupt = stateDirectory.resolve("Node-1.json");
        Files.writeString(corrupt, "{ this is not valid json ");

        // Construction must not throw and must not treat the file as valid.
        RaftCluster cluster = new RaftCluster(5, stateDirectory);

        RaftNode restored = cluster.getNodes().get(0);
        assertEquals(0, restored.getCurrentTerm());
        assertNull(restored.getVotedFor());
        assertEquals(0, restored.getLogEntries().size());
        assertEquals(-1, cluster.getCommitIndex());

        // The corrupt file is quarantined rather than silently reused.
        assertTrue(Files.exists(stateDirectory.resolve("Node-1.json.corrupt")));
    }

    @Test
    void fileNodeStateStoreRoundTripsState() {

        FileNodeStateStore store =
                new FileNodeStateStore(stateDirectory, "Node-X");

        RaftPersistentState state = new RaftPersistentState(
                3L,
                "Node-X",
                1,
                1,
                List.of(new LogEntryState(0, 3, "PUT", "k", "v"))
        );

        store.save(state);

        Optional<RaftPersistentState> loaded = store.load();

        assertTrue(loaded.isPresent());
        assertEquals(state, loaded.get());
    }

    @Test
    void loadingAnUnknownStoreReturnsEmpty() {

        FileNodeStateStore store =
                new FileNodeStateStore(stateDirectory, "Node-Missing");

        assertTrue(store.load().isEmpty());
    }
}
