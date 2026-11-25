
package com.distributedkv;

import com.distributedkv.cluster.RaftCluster;
import com.distributedkv.node.RaftNode;
import com.distributedkv.scheduler.RaftScheduler;

public class Main {

    public static void main(String[] args) throws InterruptedException {

        System.out.println("==================================");
        System.out.println(" DISTRIBUTED KEY-VALUE STORE");
        System.out.println(" AUTOMATIC RAFT LEADER ELECTION");
        System.out.println("==================================");

        // Step 1: Create a cluster of five nodes
        RaftCluster cluster = new RaftCluster(5);

        // Step 2: Create and start the scheduler
        RaftScheduler scheduler = new RaftScheduler(cluster);

        scheduler.start();

        try {

            // Step 3: Wait for automatic leader election
            System.out.println("\nWaiting for initial leader election...");

            RaftNode initialLeader = waitForLeader(cluster, null, 5000);

            if (initialLeader == null) {
                System.out.println("Initial leader election failed!");
                return;
            }

            System.out.println(
                    "\nINITIAL LEADER: " + initialLeader.getNodeId()
            );

            cluster.displayClusterStatus();

            // Step 4: Write data using the leader
            System.out.println("\n===== WRITING DATA =====");

            cluster.replicateLog("PUT", "name", "Niharika");
            cluster.replicateLog("PUT", "college", "VVCE");

            // Step 5: Crash the current leader
            String oldLeaderId = initialLeader.getNodeId();

            System.out.println("\n===== CRASHING LEADER =====");

            cluster.crashNode(oldLeaderId);

            // Step 6: Wait for automatic election of a new leader
            System.out.println(
                    "\nWaiting for automatic election of a new leader..."
            );

            RaftNode newLeader = waitForLeader(
                    cluster,
                    oldLeaderId,
                    7000
            );

            if (newLeader == null) {
                System.out.println("Automatic failover failed!");
                return;
            }

            System.out.println(
                    "\nNEW LEADER ELECTED: " + newLeader.getNodeId()
            );

            cluster.displayClusterStatus();

            // Step 7: Write new data using the new leader
            System.out.println("\n===== WRITING AFTER FAILOVER =====");

            cluster.replicateLog("PUT", "project", "Raft");

            // Step 8: Recover the old leader
            System.out.println("\n===== RECOVERING OLD LEADER =====");

            cluster.recoverNode(oldLeaderId);

            // Step 9: Verify data on all nodes
            System.out.println("\n===== VERIFYING DATA =====");

            for (int i = 1; i <= 5; i++) {

                String nodeId = "Node-" + i;

                System.out.println("\n" + nodeId + ":");

                System.out.println(
                        "Name: " + cluster.readFromNode(nodeId, "name")
                );

                System.out.println(
                        "College: " + cluster.readFromNode(nodeId, "college")
                );

                System.out.println(
                        "Project: " + cluster.readFromNode(nodeId, "project")
                );
            }

            // Step 10: Display final cluster status
            cluster.displayClusterStatus();

        } finally {

            // Stop background scheduler
            scheduler.stop();
        }
    }

    // Wait until a leader is elected
    private static RaftNode waitForLeader(
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
