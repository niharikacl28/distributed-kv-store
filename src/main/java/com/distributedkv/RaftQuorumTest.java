
package com.distributedkv;

import com.distributedkv.cluster.RaftCluster;
import com.distributedkv.node.RaftNode;

public class RaftQuorumTest {

    public static void main(String[] args) {

        System.out.println("====================================");
        System.out.println(" RAFT QUORUM AND FAULT TOLERANCE TEST");
        System.out.println("====================================");

        RaftCluster cluster = new RaftCluster(5);

        // Step 1: Elect Node-1 as leader
        cluster.startElection("Node-1");

        // Step 2: Crash two nodes
        System.out.println("\n===== SCENARIO A: TWO NODES CRASH =====");

        cluster.crashNode("Node-4");
        cluster.crashNode("Node-5");

        // Three nodes remain alive
        System.out.println("\nAttempting write with 3 nodes alive...");

        cluster.replicateLog("PUT", "status", "QUORUM_WORKING");

        RaftNode leader = cluster.getNodes().get(0);

        if ("QUORUM_WORKING".equals(
                leader.getStoredValue("status"))) {

            System.out.println(
                    "\nSUCCESS: WRITE COMMITTED WITH 3 NODES!"
            );

        } else {
            throw new RuntimeException(
                    "FAILED: Write did not commit with 3 nodes!"
            );
        }

        // Step 3: Crash one more node
        System.out.println("\n===== SCENARIO B: ONLY TWO NODES ALIVE =====");

        cluster.crashNode("Node-3");

        System.out.println("\nAttempting write with only 2 nodes alive...");

        cluster.replicateLog("PUT", "second", "SHOULD_NOT_COMMIT");

        // Verify that the second entry was not applied
        if (leader.getStoredValue("second") == null) {

            System.out.println(
                    "\nSUCCESS: WRITE NOT COMMITTED WITHOUT MAJORITY!"
            );

        } else {
            throw new RuntimeException(
                    "FAILED: Write committed without majority!"
            );
        }

        // Step 4: Display final cluster state
        System.out.println("\n===== FINAL CLUSTER STATUS =====");

        cluster.displayClusterStatus();

        System.out.println("\n===== QUORUM TEST COMPLETED =====");
    }
}
