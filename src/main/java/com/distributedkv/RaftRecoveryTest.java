
package com.distributedkv;

import com.distributedkv.cluster.RaftCluster;
import com.distributedkv.node.RaftNode;

public class RaftRecoveryTest {

    public static void main(String[] args) {

        System.out.println("====================================");
        System.out.println(" RAFT NODE RECOVERY TEST");
        System.out.println("====================================");

        // STEP 1: Create a five-node cluster
        RaftCluster cluster = new RaftCluster(5);

        // STEP 2: Elect Node-1 as leader
        System.out.println("\n===== INITIAL LEADER ELECTION =====");

        cluster.startElection("Node-1");

        RaftNode leader = cluster.getNodes().get(0);

        // STEP 3: Crash two follower nodes
        System.out.println("\n===== CRASHING TWO FOLLOWERS =====");

        cluster.crashNode("Node-4");
        cluster.crashNode("Node-5");

        // STEP 4: Write data with three active nodes
        System.out.println("\n===== WRITING DATA WHILE NODES ARE DOWN =====");

        cluster.replicateLog("PUT", "name", "Niharika");
        cluster.replicateLog("PUT", "college", "VVCE");
        cluster.replicateLog("PUT", "project", "Raft");

        System.out.println("\nLeader log entries: "
                + leader.getLogEntries().size());

        // STEP 5: Recover Node-4
        System.out.println("\n===== RECOVERING NODE-4 =====");

        cluster.recoverNode("Node-4");

        // STEP 6: Recover Node-5
        System.out.println("\n===== RECOVERING NODE-5 =====");

        cluster.recoverNode("Node-5");

        // STEP 7: Verify recovered nodes
        System.out.println("\n===== VERIFYING RECOVERED NODES =====");

        RaftNode node4 = cluster.getNodes().get(3);
        RaftNode node5 = cluster.getNodes().get(4);

        System.out.println("\nNode-4 data:");
        System.out.println("Name: " + node4.getStoredValue("name"));
        System.out.println("College: " + node4.getStoredValue("college"));
        System.out.println("Project: " + node4.getStoredValue("project"));

        System.out.println("\nNode-5 data:");
        System.out.println("Name: " + node5.getStoredValue("name"));
        System.out.println("College: " + node5.getStoredValue("college"));
        System.out.println("Project: " + node5.getStoredValue("project"));

        // STEP 8: Verify log synchronization
        boolean node4Correct =
                node4.getLogEntries().size() == 3
                && "Niharika".equals(node4.getStoredValue("name"))
                && "VVCE".equals(node4.getStoredValue("college"))
                && "Raft".equals(node4.getStoredValue("project"));

        boolean node5Correct =
                node5.getLogEntries().size() == 3
                && "Niharika".equals(node5.getStoredValue("name"))
                && "VVCE".equals(node5.getStoredValue("college"))
                && "Raft".equals(node5.getStoredValue("project"));

        if (node4Correct && node5Correct) {

            System.out.println(
                    "\nSUCCESS: BOTH NODES RECOVERED AND SYNCHRONIZED!"
            );

        } else {

            throw new RuntimeException(
                    "FAILED: Node recovery or synchronization failed!"
            );
        }

        // STEP 9: Display final cluster status
        System.out.println("\n===== FINAL CLUSTER STATUS =====");

        cluster.displayClusterStatus();

        System.out.println("\n===== RECOVERY TEST COMPLETED =====");
    }
}
