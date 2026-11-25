
package com.distributedkv;

import com.distributedkv.cluster.RaftCluster;
import com.distributedkv.model.LogEntry;
import com.distributedkv.node.RaftNode;

public class RaftConflictTest {

    public static void main(String[] args) {

        System.out.println("====================================");
        System.out.println(" RAFT LOG CONFLICT REPAIR TEST");
        System.out.println("====================================");

        // Step 1: Create a five-node cluster
        RaftCluster cluster = new RaftCluster(5);

        // Step 2: Elect Node-1 as leader
        System.out.println("\n===== LEADER ELECTION =====");

        cluster.startElection("Node-1");

        // Step 3: Add two correct entries
        System.out.println("\n===== ADDING CORRECT ENTRIES =====");

        cluster.replicateLog("PUT", "name", "Niharika");
        cluster.replicateLog("PUT", "college", "VVCE");

        // Step 4: Find Node-3
        RaftNode follower = cluster.getNodes().get(2);

        // Step 5: Introduce an incorrect uncommitted entry
        System.out.println("\n===== INTRODUCING LOG CONFLICT =====");

        LogEntry wrongEntry = new LogEntry(
                2,
                99,
                "PUT",
                "wrong",
                "BAD DATA"
        );

        follower.addLogEntry(wrongEntry);

        System.out.println(
                "\nIncorrect entry added to " + follower.getNodeId()
        );

        // Step 6: Ask the leader to replicate the correct entry
        System.out.println("\n===== LEADER REPAIRING FOLLOWER LOG =====");

        cluster.replicateLog(
                "PUT",
                "project",
                "Raft"
        );

        // Step 7: Verify the repaired entry
        System.out.println("\n===== VERIFYING LOG REPAIR =====");

        LogEntry repairedEntry =
                follower.getLogEntries().get(2);

        System.out.println("Follower: " + follower.getNodeId());
        System.out.println("Entry at index 2: " + repairedEntry);

        if (repairedEntry.getTerm() == 1
                && repairedEntry.getKey().equals("project")
                && repairedEntry.getValue().equals("Raft")) {

            System.out.println("\nSUCCESS: LOG CONFLICT REPAIRED!");

        } else {

            throw new IllegalStateException(
                    "Log conflict repair failed!"
            );
        }

        // Step 8: Verify stored data
        System.out.println("\n===== VERIFYING STORED DATA =====");

        System.out.println(
                "Name: " + cluster.readFromNode("Node-3", "name")
        );

        System.out.println(
                "College: " + cluster.readFromNode("Node-3", "college")
        );

        System.out.println(
                "Project: " + cluster.readFromNode("Node-3", "project")
        );

        // Step 9: Display final cluster status
        cluster.displayClusterStatus();

        System.out.println("\n===== CONFLICT TEST COMPLETED =====");
    }
}
