
package com.distributedkv;

import com.distributedkv.cluster.RaftCluster;
import com.distributedkv.node.RaftNode;
import com.distributedkv.node.RaftRole;

public class RaftElectionFailureTest {

    public static void main(String[] args) {

        System.out.println("====================================");
        System.out.println(" RAFT ELECTION WITHOUT MAJORITY TEST");
        System.out.println("====================================");

        // Step 1: Create a five-node cluster
        RaftCluster cluster = new RaftCluster(5);

        // Step 2: Elect Node-1 initially
        cluster.startElection("Node-1");

        // Step 3: Crash three nodes, including the leader
        System.out.println("\n===== CRASHING THREE NODES =====");

        cluster.crashNode("Node-1");
        cluster.crashNode("Node-4");
        cluster.crashNode("Node-5");

        // Step 4: Only Node-2 and Node-3 remain alive
        System.out.println("\n===== ATTEMPTING NEW ELECTION =====");

        System.out.println("Alive nodes: 2");
        System.out.println("Majority required: 3");

        cluster.startElection("Node-2");

        // Step 5: Verify no leader was elected
        System.out.println("\n===== VERIFYING ELECTION RESULT =====");

        RaftNode node2 = cluster.getNodes().get(1);

        if (cluster.getLeader() == null
                && node2.getRole() != RaftRole.LEADER) {

            System.out.println(
                    "\nSUCCESS: NO LEADER ELECTED WITHOUT MAJORITY!"
            );

        } else {

            throw new RuntimeException(
                    "FAILED: A leader was elected without majority!"
            );
        }

        // Step 6: Display cluster status
        System.out.println("\n===== FINAL CLUSTER STATUS =====");

        cluster.displayClusterStatus();

        System.out.println("\n===== ELECTION TEST COMPLETED =====");
    }
}
