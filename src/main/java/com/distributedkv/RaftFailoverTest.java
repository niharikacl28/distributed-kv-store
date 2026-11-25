
package com.distributedkv;

import com.distributedkv.cluster.RaftCluster;
import com.distributedkv.node.RaftNode;
import com.distributedkv.scheduler.RaftScheduler;

public class RaftFailoverTest {

    public static void main(String[] args) {

        System.out.println("====================================");
        System.out.println(" AUTOMATIC LEADER FAILOVER TEST");
        System.out.println("====================================");

        RaftCluster cluster = new RaftCluster(5);

        RaftScheduler scheduler = new RaftScheduler(cluster);

        try {

            // STEP 1: Start automatic scheduler
            System.out.println("\n===== STARTING RAFT SCHEDULER =====");

            scheduler.start();

            // STEP 2: Wait for initial leader election
            System.out.println("\n===== WAITING FOR INITIAL LEADER =====");

            RaftNode oldLeader = waitForLeader(cluster, 5000);

            if (oldLeader == null) {
                throw new RuntimeException(
                        "Initial leader election failed!"
                );
            }

            System.out.println(
                    "\nInitial Leader: " + oldLeader.getNodeId()
            );

            // STEP 3: Write initial data
            System.out.println("\n===== WRITING INITIAL DATA =====");

            cluster.replicateLog(
                    "PUT", "status", "BEFORE_FAILURE"
            );

            // STEP 4: Crash the current leader
            System.out.println("\n===== CRASHING CURRENT LEADER =====");

            cluster.crashNode(oldLeader.getNodeId());

            System.out.println(
                    "\nWaiting for automatic leader election..."
            );

            // STEP 5: Wait for a different leader
            RaftNode newLeader = waitForNewLeader(
                    cluster,
                    oldLeader.getNodeId(),
                    7000
            );

            if (newLeader == null) {
                throw new RuntimeException(
                        "Automatic failover failed!"
                );
            }

            System.out.println(
                    "\nNEW LEADER ELECTED: " + newLeader.getNodeId()
            );

            // STEP 6: Verify new leader can process writes
            System.out.println("\n===== TESTING NEW LEADER =====");

            cluster.replicateLog(
                    "PUT", "failover", "SUCCESS"
            );

            if ("SUCCESS".equals(
                    newLeader.getStoredValue("failover"))) {

                System.out.println(
                        "\nSUCCESS: NEW LEADER ACCEPTED WRITE!"
                );

            } else {

                throw new RuntimeException(
                        "New leader failed to commit write!"
                );
            }

            // STEP 7: Final cluster status
            System.out.println("\n===== FINAL CLUSTER STATUS =====");

            cluster.displayClusterStatus();

            System.out.println(
                    "\nSUCCESS: AUTOMATIC LEADER FAILOVER VERIFIED!"
            );

        } finally {

            // Always stop background scheduler
            scheduler.stop();

            System.out.println("\nRaft scheduler stopped.");
        }
    }

    private static RaftNode waitForLeader(
            RaftCluster cluster,
            long timeoutMillis) {

        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < timeoutMillis) {

            RaftNode leader = cluster.getLeader();

            if (leader != null) {
                return leader;
            }

            sleep(100);
        }

        return null;
    }

    private static RaftNode waitForNewLeader(
            RaftCluster cluster,
            String oldLeaderId,
            long timeoutMillis) {

        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < timeoutMillis) {

            RaftNode leader = cluster.getLeader();

            if (leader != null
                    && !leader.getNodeId().equals(oldLeaderId)) {

                return leader;
            }

            sleep(100);
        }

        return null;
    }

    private static void sleep(long milliseconds) {

        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(
                    "Test interrupted", e
            );
        }
    }
}
