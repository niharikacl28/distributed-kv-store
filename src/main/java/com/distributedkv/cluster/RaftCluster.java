
package com.distributedkv.cluster;

import com.distributedkv.activity.EventHistory;
import com.distributedkv.activity.EventType;
import com.distributedkv.node.RaftNode;
import com.distributedkv.node.RaftRole;
import com.distributedkv.model.VoteRequest;
import com.distributedkv.model.VoteResponse;
import com.distributedkv.model.Heartbeat;
import com.distributedkv.model.LogEntry;
import com.distributedkv.model.AppendEntriesRequest;
import com.distributedkv.model.AppendEntriesResponse;
import com.distributedkv.persistence.FileNodeStateStore;
import com.distributedkv.persistence.NodeStateStore;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RaftCluster {

    private final List<RaftNode> nodes;

    private volatile int commitIndex = -1;

    private final Object electionLock = new Object();

    // Bounded history of real cluster events, surfaced by the activity timeline.
    private final EventHistory eventHistory = new EventHistory();

    // Throttle interval so the 100 ms heartbeat loop cannot flood the history.
    private static final long HEARTBEAT_EVENT_INTERVAL_MS = 5000L;

    private volatile long lastHeartbeatEventAt = 0L;

    // Leader replication tracking
    private final Map<String, Integer> nextIndex = new HashMap<>();
    private final Map<String, Integer> matchIndex = new HashMap<>();

    public RaftCluster(int numberOfNodes) {
        this(numberOfNodes, null);
    }

    /**
     * Creates a cluster whose nodes persist their Raft state as JSON files
     * under {@code stateDirectory}. When {@code stateDirectory} is
     * {@code null} the cluster is purely in-memory.
     *
     * <p>Any previously persisted state is restored while the nodes are
     * constructed, and each node's key-value store is rebuilt by replaying
     * committed entries only.</p>
     */
    public RaftCluster(int numberOfNodes, Path stateDirectory) {

        nodes = new ArrayList<>();

        for (int i = 1; i <= numberOfNodes; i++) {

            String nodeId = "Node-" + i;

            NodeStateStore stateStore = stateDirectory == null
                    ? null
                    : new FileNodeStateStore(stateDirectory, nodeId);

            nodes.add(new RaftNode(nodeId, stateStore));
        }

        restoreCommitIndex();
    }

    /**
     * Restores the cluster commit index from the persisted node state and
     * rebuilds each node's key-value store by replaying committed entries.
     * The commit index is clamped so it can never exceed the available log,
     * and uncommitted entries are never applied.
     */
    private void restoreCommitIndex() {

        int restored = -1;

        for (RaftNode node : nodes) {
            // A node can only vouch for entries it actually holds.
            int nodeCommit = Math.min(
                    node.getCommitIndex(),
                    node.getLastLogIndex()
            );
            restored = Math.max(restored, nodeCommit);
        }

        commitIndex = Math.max(-1, restored);

        if (commitIndex < 0) {
            return;
        }

        for (RaftNode node : nodes) {
            int safeCommit = Math.min(commitIndex, node.getLastLogIndex());
            if (safeCommit >= 0) {
                node.applyCommittedEntries(safeCommit);
            }
        }

        System.out.println(
                "Restored cluster commit index " + commitIndex
                        + " and replayed committed entries."
        );
    }

    // ---------------- LEADER ELECTION ----------------

    public void startElection(String candidateId) {

        synchronized (electionLock) {

            if (getLeader() != null) {
                return;
            }

            RaftNode candidate = findNode(candidateId);

            if (candidate == null || !candidate.isAlive()) {
                return;
            }

            VoteRequest request = candidate.startElection();

            if (request == null) {
                return;
            }

            int votes = 1;

            for (RaftNode node : nodes) {

                if (node == candidate || !node.isAlive()) {
                    continue;
                }

                VoteResponse response =
                        node.handleVoteRequest(request);

                if (response.getTerm() > request.getTerm()) {
                    candidate.stepDown(response.getTerm());
                    return;
                }

                if (response.isVoteGranted()) {
                    votes++;
                }
            }

            int majority = (nodes.size() / 2) + 1;

            System.out.println("\nElection results for " + candidateId);
            System.out.println("Votes received: " + votes);
            System.out.println("Majority required: " + majority);

            if (votes >= majority
                    && candidate.getRole() == RaftRole.CANDIDATE
                    && candidate.getCurrentTerm() == request.getTerm()) {

                candidate.becomeLeader();

                initializeLeaderReplicationState(candidate);

                eventHistory.record(
                        EventType.LEADER_ELECTED,
                        candidate.getNodeId() + " became leader for term "
                                + candidate.getCurrentTerm(),
                        candidate.getNodeId(),
                        candidate.getCurrentTerm());

            } else {

                System.out.println(
                        candidateId + " failed to win the election.");
            }
        }
    }

    // ---------------- INITIALIZE LEADER STATE ----------------

    private void initializeLeaderReplicationState(RaftNode leader) {

        nextIndex.clear();
        matchIndex.clear();

        int initialNextIndex = leader.getLastLogIndex() + 1;

        for (RaftNode node : nodes) {

            if (node == leader) {
                continue;
            }

            nextIndex.put(node.getNodeId(), initialNextIndex);
            matchIndex.put(node.getNodeId(), -1);
        }

        System.out.println(
                "Leader replication state initialized for "
                + leader.getNodeId()
        );
    }

    // ---------------- AUTOMATIC ELECTION CHECK ----------------

    public void checkElectionTimeouts() {

        if (getLeader() != null) {
            return;
        }

        for (RaftNode node : nodes) {

            if (node.isAlive()
                    && node.hasElectionTimeoutExpired()) {

                System.out.println(
                        "\nElection timeout detected by "
                        + node.getNodeId()
                );

                startElection(node.getNodeId());

                return;
            }
        }
    }

    // ---------------- HEARTBEAT AND APPEND ENTRIES ----------------

    public synchronized void sendHeartbeats() {

        RaftNode leader = getLeader();

        if (leader == null) {
            return;
        }

        int replicatedFollowers = 0;

        // Use AppendEntries with empty entries for heartbeats.
        for (RaftNode follower : nodes) {

            if (follower == leader || !follower.isAlive()) {
                continue;
            }

            if (replicateToFollower(leader, follower)) {
                replicatedFollowers++;
            }
        }

        recordHeartbeatActivity(leader, replicatedFollowers);
    }

    /**
     * Records a heartbeat activity event, throttled to at most one every
     * {@link #HEARTBEAT_EVENT_INTERVAL_MS}. The heartbeat loop fires every
     * 100 ms, so throttling prevents it from flooding the bounded history.
     */
    private void recordHeartbeatActivity(RaftNode leader, int followers) {

        long now = System.currentTimeMillis();

        if (now - lastHeartbeatEventAt < HEARTBEAT_EVENT_INTERVAL_MS) {
            return;
        }

        lastHeartbeatEventAt = now;

        eventHistory.record(
                EventType.HEARTBEAT,
                "Leader " + leader.getNodeId() + " sent heartbeats to "
                        + followers + " follower(s)",
                leader.getNodeId(),
                leader.getCurrentTerm());
    }

    // ---------------- REPLICATE TO ONE FOLLOWER ----------------

    private boolean replicateToFollower(
            RaftNode leader,
            RaftNode follower) {

        String followerId = follower.getNodeId();

        if (!nextIndex.containsKey(followerId)) {

            nextIndex.put(
                    followerId,
                    leader.getLastLogIndex() + 1
            );

            matchIndex.put(followerId, -1);
        }

        // Retry with earlier previous-log indexes if necessary.
        int attempts = leader.getLastLogIndex() + 2;

        while (attempts-- > 0 && follower.isAlive()) {

            List<LogEntry> leaderLog = leader.getLogEntries();

            int followerNextIndex = nextIndex.get(followerId);

            followerNextIndex = Math.max(0, Math.min(
                    followerNextIndex,
                    leaderLog.size()
            ));

            int previousLogIndex = followerNextIndex - 1;

            long previousLogTerm = 0;

            if (previousLogIndex >= 0) {
                previousLogTerm =
                        leaderLog.get(previousLogIndex).getTerm();
            }

            List<LogEntry> entriesToSend = new ArrayList<>(
                    leaderLog.subList(
                            followerNextIndex,
                            leaderLog.size()
                    )
            );

            AppendEntriesRequest request =
                    new AppendEntriesRequest(
                            leader.getCurrentTerm(),
                            leader.getNodeId(),
                            previousLogIndex,
                            previousLogTerm,
                            entriesToSend,
                            commitIndex
                    );

            AppendEntriesResponse response =
                    follower.handleAppendEntries(request);

            if (response.getTerm() > leader.getCurrentTerm()) {

                leader.stepDown(response.getTerm());

                return false;
            }

            if (response.isSuccess()) {

                nextIndex.put(
                        followerId,
                        response.getMatchIndex() + 1
                );

                matchIndex.put(
                        followerId,
                        response.getMatchIndex()
                );

                return true;
            }

            // Previous log did not match. Retry from an earlier index.
            int currentNextIndex = nextIndex.get(followerId);

            if (currentNextIndex == 0) {
                return false;
            }

            nextIndex.put(
                    followerId,
                    currentNextIndex - 1
            );
        }

        return false;
    }

    // ---------------- LOG REPLICATION ----------------

    public synchronized boolean replicateLog(
            String operation,
            String key,
            String value) {

        RaftNode leader = getLeader();

        if (leader == null) {

            System.out.println(
                    "Cannot write: No active leader!");

            eventHistory.record(
                    EventType.WRITE_FAILED,
                    "Write rejected: no active leader for "
                            + operation + " '" + key + "'",
                    null,
                    highestTerm());

            return false;
        }

        long term = leader.getCurrentTerm();

        if ("PUT".equals(operation)) {

            eventHistory.record(
                    EventType.PUT_RECEIVED,
                    "PUT request for key '" + key + "' received",
                    leader.getNodeId(),
                    term);

        } else {

            eventHistory.record(
                    EventType.DELETE_RECEIVED,
                    "DELETE request for key '" + key + "' received",
                    leader.getNodeId(),
                    term);
        }

        int index = leader.getLastLogIndex() + 1;

        LogEntry entry = new LogEntry(
                index,
                leader.getCurrentTerm(),
                operation,
                key,
                value
        );

        System.out.println("\nLeader creating log entry...");

        leader.addLogEntry(entry);

        int acknowledgements = 1;

        System.out.println(
                "\nReplicating through AppendEntries..."
        );

        for (RaftNode follower : nodes) {

            if (follower == leader || !follower.isAlive()) {
                continue;
            }

            if (replicateToFollower(leader, follower)) {
                acknowledgements++;
            }
        }

        int majority = (nodes.size() / 2) + 1;

        System.out.println(
                "\nAcknowledgements: " + acknowledgements
        );

        System.out.println(
                "Majority required: " + majority
        );

        if (acknowledgements > 1) {

            eventHistory.record(
                    EventType.ENTRY_REPLICATED,
                    "Log entry #" + index + " (" + operation + " '"
                            + key + "') replicated to "
                            + acknowledgements + "/" + nodes.size()
                            + " nodes",
                    leader.getNodeId(),
                    term);
        }

        // Attempt to advance the commit index. The write is only
        // considered successful when the entry is actually committed,
        // which is verified through the commit index rather than the
        // raw acknowledgement count.
        updateCommitIndex(leader);

        if (commitIndex >= index) {

            System.out.println(
                    "\nLog entry committed at index " + commitIndex
            );

            eventHistory.record(
                    EventType.ENTRY_COMMITTED,
                    "Log entry #" + index + " (" + operation + " '"
                            + key + "') committed at index " + commitIndex,
                    leader.getNodeId(),
                    term);

            // Inform followers of the updated commit index.
            sendHeartbeats();

            return true;
        }

        System.out.println(
                "Majority not reached. Entry remains uncommitted."
        );

        eventHistory.record(
                EventType.WRITE_FAILED,
                "Write failed: majority not reached for entry #"
                        + index + " (" + operation + " '" + key + "')",
                leader.getNodeId(),
                term);

        return false;
    }

    /** Highest current term observed across all nodes; used when no leader exists. */
    private long highestTerm() {

        long term = 0;

        for (RaftNode node : nodes) {
            term = Math.max(term, node.getCurrentTerm());
        }

        return term;
    }

    // ---------------- MAJORITY COMMITMENT ----------------

    private void updateCommitIndex(RaftNode leader) {

        List<LogEntry> leaderLog = leader.getLogEntries();

        for (int index = leaderLog.size() - 1;
                index > commitIndex;
                index--) {

            // Raft directly advances commit index only for
            // entries from the leader's current term.
            if (leaderLog.get(index).getTerm()
                    != leader.getCurrentTerm()) {
                continue;
            }

            int replicatedCount = 1;

            for (RaftNode node : nodes) {

                if (node == leader || !node.isAlive()) {
                    continue;
                }

                int replicatedIndex =
                        matchIndex.getOrDefault(
                                node.getNodeId(), -1);

                if (replicatedIndex >= index) {
                    replicatedCount++;
                }
            }

            int majority = (nodes.size() / 2) + 1;

            if (replicatedCount >= majority) {

                commitIndex = index;

                for (RaftNode node : nodes) {

                    if (node.isAlive()) {
                        node.applyCommittedEntries(commitIndex);
                    }
                }

                return;
            }
        }
    }

    // ---------------- READ FROM NODE ----------------

    public String readFromNode(String nodeId, String key) {

        RaftNode node = findNode(nodeId);

        if (node == null || !node.isAlive()) {
            System.out.println("Node unavailable!");
            return null;
        }

        return node.getStoredValue(key);
    }

    // ---------------- READ FROM LEADER ----------------

    public String readFromLeader(String key) {

        RaftNode leader = getLeader();

        if (leader == null) {
            return null;
        }

        return leader.getStoredValue(key);
    }

    // ---------------- NODE CRASH ----------------

    public synchronized void crashNode(String nodeId) {

        RaftNode node = findNode(nodeId);

        if (node == null) {
            System.out.println("Node not found!");
            return;
        }

        if (!node.isAlive()) {
            System.out.println(nodeId + " is already down!");
            return;
        }

        node.setAlive(false);

        eventHistory.record(
                EventType.NODE_CRASHED,
                nodeId + " crashed",
                nodeId,
                node.getCurrentTerm());
    }

    // ---------------- NODE RECOVERY ----------------

    public synchronized void recoverNode(String nodeId) {

        RaftNode node = findNode(nodeId);

        if (node == null) {
            System.out.println("Node not found!");
            return;
        }

        if (node.isAlive()) {
            System.out.println(nodeId + " is already running!");
            return;
        }

        node.setAlive(true);

        RaftNode leader = getLeader();

        if (leader == null) {

            System.out.println(
                    "No active leader available for synchronization."
            );

            eventHistory.record(
                    EventType.NODE_RECOVERED,
                    nodeId + " recovered (no active leader to synchronize from)",
                    nodeId,
                    node.getCurrentTerm());

            return;
        }

        System.out.println("\nStarting synchronization...");
        System.out.println("Current leader: " + leader.getNodeId());

        boolean success = node.synchronizeFromLeader(
                leader.getNodeId(),
                leader.getCurrentTerm(),
                leader.getLogEntries(),
                commitIndex
        );

        if (success) {

            nextIndex.put(
                    nodeId,
                    leader.getLastLogIndex() + 1
            );

            matchIndex.put(
                    nodeId,
                    leader.getLastLogIndex()
            );

            System.out.println(
                    nodeId + " is now synchronized with the cluster."
            );

            eventHistory.record(
                    EventType.NODE_RECOVERED,
                    nodeId + " recovered and synchronized from "
                            + leader.getNodeId(),
                    nodeId,
                    node.getCurrentTerm());

        } else {

            System.out.println(
                    nodeId + " could not be synchronized."
            );

            eventHistory.record(
                    EventType.NODE_RECOVERED,
                    nodeId + " recovered (synchronization failed)",
                    nodeId,
                    node.getCurrentTerm());
        }
    }

    // ---------------- HELPER METHODS ----------------

    public RaftNode getLeader() {

        for (RaftNode node : nodes) {

            if (node.isAlive()
                    && node.getRole() == RaftRole.LEADER) {

                return node;
            }
        }

        return null;
    }

    private RaftNode findNode(String nodeId) {

        for (RaftNode node : nodes) {

            if (node.getNodeId().equals(nodeId)) {
                return node;
            }
        }

        return null;
    }

    // ---------------- CLUSTER STATUS ----------------

    public void displayClusterStatus() {

        System.out.println("\n===== CLUSTER STATUS =====");

        for (RaftNode node : nodes) {

            System.out.println(
                    node.getNodeId()
                    + " | Role: " + node.getRole()
                    + " | Term: " + node.getCurrentTerm()
                    + " | Alive: " + node.isAlive()
                    + " | Known Leader: " + node.getKnownLeader()
                    + " | Log Entries: "
                    + node.getLogEntries().size()
            );
        }

        System.out.println("Commit Index: " + commitIndex);
    }

    public List<RaftNode> getNodes() {
        return nodes;
    }

    public int getCommitIndex() {
        return commitIndex;
    }

    /** The bounded history of recent cluster events. */
    public EventHistory getEventHistory() {
        return eventHistory;
    }
}
