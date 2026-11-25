
package com.distributedkv;

import com.distributedkv.cluster.RaftCluster;
import com.distributedkv.model.LogEntry;
import com.distributedkv.model.AppendEntriesRequest;
import com.distributedkv.model.AppendEntriesResponse;
import com.distributedkv.node.RaftNode;

import java.util.List;

public class RaftCommittedEntryTest {

    public static void main(String[] args) {

        System.out.println("====================================");
        System.out.println(" COMMITTED LOG PROTECTION TEST");
        System.out.println("====================================");

        // Step 1: Create a five-node cluster
        RaftCluster cluster = new RaftCluster(5);

        // Step 2: Elect Node-1 as leader
        cluster.startElection("Node-1");

        // Access nodes using the public getNodes() method
        RaftNode leader = cluster.getNodes().get(0);
        RaftNode follower = cluster.getNodes().get(1);

        // Step 3: Add two entries and commit them
        System.out.println("\n===== ADDING COMMITTED ENTRIES =====");

        cluster.replicateLog("PUT", "name", "Niharika");
        cluster.replicateLog("PUT", "college", "VVCE");

        System.out.println("\nNode-2 commit state:");
        System.out.println("Name: " + follower.getStoredValue("name"));
        System.out.println("College: " + follower.getStoredValue("college"));

        // Step 4: Create a conflicting entry at committed index 1
        System.out.println("\n===== ATTEMPTING TO OVERWRITE COMMITTED ENTRY =====");

        LogEntry wrongEntry = new LogEntry(
                1,
                99,
                "PUT",
                "college",
                "WRONG"
        );

        AppendEntriesRequest request = new AppendEntriesRequest(
                leader.getCurrentTerm(),
                leader.getNodeId(),
                0,
                1,
                List.of(wrongEntry),
                1
        );

        AppendEntriesResponse response =
                follower.handleAppendEntries(request);

        System.out.println("\nFollower accepted request: "
                + response.isSuccess());

        // Step 5: Verify committed entry was not changed
        System.out.println("\n===== VERIFYING COMMITTED ENTRY =====");

        LogEntry actualEntry = follower.getLogEntries().get(1);

        System.out.println("Actual entry: " + actualEntry);
        System.out.println("Stored college value: "
                + follower.getStoredValue("college"));

        if (!response.isSuccess()
                && actualEntry.getTerm() == 1
                && actualEntry.getKey().equals("college")
                && actualEntry.getValue().equals("VVCE")
                && "VVCE".equals(follower.getStoredValue("college"))) {

            System.out.println("\nSUCCESS: COMMITTED ENTRY PROTECTED!");

        } else {
            throw new RuntimeException(
                    "FAILED: Committed entry was not protected!"
            );
        }

        System.out.println("\n===== CLUSTER STATUS =====");
        cluster.displayClusterStatus();

        System.out.println("\n===== TEST COMPLETED =====");
    }
}
