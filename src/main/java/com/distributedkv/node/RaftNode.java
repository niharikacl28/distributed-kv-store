
package com.distributedkv.node;

import com.distributedkv.model.VoteRequest;
import com.distributedkv.model.VoteResponse;
import com.distributedkv.model.Heartbeat;
import com.distributedkv.model.LogEntry;
import com.distributedkv.model.AppendEntriesRequest;
import com.distributedkv.model.AppendEntriesResponse;
import com.distributedkv.storage.KeyValueStore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RaftNode {

    private final String nodeId;

    private RaftRole role;
    private long currentTerm;
    private String votedFor;
    private boolean alive;

    private int lastLogIndex;
    private long lastLogTerm;

    private String knownLeader;

    private final List<LogEntry> logEntries;
    private final KeyValueStore keyValueStore;

    private int lastApplied = -1;

    private long lastHeartbeatReceivedAt;
    private long electionTimeoutMillis;

    public RaftNode(String nodeId) {

        this.nodeId = nodeId;

        this.role = RaftRole.FOLLOWER;
        this.currentTerm = 0;
        this.votedFor = null;
        this.alive = true;

        this.lastLogIndex = -1;
        this.lastLogTerm = 0;

        this.knownLeader = null;

        this.logEntries = new ArrayList<>();
        this.keyValueStore = new KeyValueStore();

        resetElectionTimeout();
    }

    // ---------------- ELECTION TIMEOUT ----------------

    private synchronized void resetElectionTimeout() {

        lastHeartbeatReceivedAt = System.currentTimeMillis();

        electionTimeoutMillis =
                ThreadLocalRandom.current().nextLong(400, 701);
    }

    public synchronized boolean hasElectionTimeoutExpired() {

        if (!alive || role == RaftRole.LEADER) {
            return false;
        }

        long elapsed =
                System.currentTimeMillis() - lastHeartbeatReceivedAt;

        return elapsed >= electionTimeoutMillis;
    }

    // ---------------- LEADER ELECTION ----------------

    public synchronized VoteRequest startElection() {

        if (!alive) {
            return null;
        }

        currentTerm++;

        role = RaftRole.CANDIDATE;
        votedFor = nodeId;
        knownLeader = null;

        resetElectionTimeout();

        System.out.println(
                nodeId + " started election for term " + currentTerm
        );

        return new VoteRequest(
                nodeId,
                currentTerm,
                lastLogIndex,
                lastLogTerm
        );
    }

    public synchronized VoteResponse handleVoteRequest(
            VoteRequest request) {

        if (!alive) {
            return new VoteResponse(currentTerm, false);
        }

        if (request.getTerm() < currentTerm) {
            return new VoteResponse(currentTerm, false);
        }

        if (request.getTerm() > currentTerm) {

            currentTerm = request.getTerm();

            role = RaftRole.FOLLOWER;
            votedFor = null;
            knownLeader = null;

            resetElectionTimeout();
        }

        boolean candidateLogIsUpToDate =
                request.getLastLogTerm() > lastLogTerm
                || (request.getLastLogTerm() == lastLogTerm
                && request.getLastLogIndex() >= lastLogIndex);

        boolean canVote =
                votedFor == null
                || votedFor.equals(request.getCandidateId());

        if (canVote && candidateLogIsUpToDate) {

            votedFor = request.getCandidateId();

            resetElectionTimeout();

            System.out.println(
                    nodeId + " granted vote to "
                    + request.getCandidateId()
            );

            return new VoteResponse(currentTerm, true);
        }

        return new VoteResponse(currentTerm, false);
    }

    public synchronized void becomeLeader() {

        if (alive) {

            role = RaftRole.LEADER;
            knownLeader = nodeId;

            System.out.println(nodeId + " became LEADER!");
        }
    }

    public synchronized void stepDown(long newTerm) {

        if (newTerm > currentTerm) {

            currentTerm = newTerm;

            role = RaftRole.FOLLOWER;
            votedFor = null;
            knownLeader = null;

            resetElectionTimeout();
        }
    }

    // ---------------- HEARTBEAT ----------------

    public synchronized boolean receiveHeartbeat(
            Heartbeat heartbeat) {

        if (!alive || heartbeat.getTerm() < currentTerm) {
            return false;
        }

        if (heartbeat.getTerm() > currentTerm
                || role != RaftRole.FOLLOWER) {

            currentTerm = heartbeat.getTerm();

            role = RaftRole.FOLLOWER;
            votedFor = null;
        }

        knownLeader = heartbeat.getLeaderId();

        resetElectionTimeout();

        return true;
    }

    // ---------------- APPEND ENTRIES ----------------

    public synchronized AppendEntriesResponse handleAppendEntries(
            AppendEntriesRequest request) {

        if (!alive) {
            return new AppendEntriesResponse(
                    currentTerm, false, lastLogIndex);
        }

        // Reject outdated leader
        if (request.getTerm() < currentTerm) {

            return new AppendEntriesResponse(
                    currentTerm, false, lastLogIndex);
        }

        // Update term if necessary
        if (request.getTerm() > currentTerm) {

            currentTerm = request.getTerm();
            votedFor = null;
        }

        role = RaftRole.FOLLOWER;
        knownLeader = request.getLeaderId();

        resetElectionTimeout();

        // Validate previous log index
        int previousIndex = request.getPreviousLogIndex();

        if (previousIndex >= logEntries.size()) {

            System.out.println(
                    nodeId + " rejected AppendEntries: missing previous entry."
            );

            return new AppendEntriesResponse(
                    currentTerm, false, lastLogIndex);
        }

        // Validate previous log term
        if (previousIndex >= 0) {

            long localPreviousTerm =
                    logEntries.get(previousIndex).getTerm();

            if (localPreviousTerm != request.getPreviousLogTerm()) {

                System.out.println(
                        nodeId + " detected a log term conflict at index "
                        + previousIndex
                );

                return new AppendEntriesResponse(
                        currentTerm, false, lastLogIndex);
            }
        }

        // Process incoming entries
        List<LogEntry> incomingEntries = request.getEntries();

        for (int i = 0; i < incomingEntries.size(); i++) {

            LogEntry incoming = incomingEntries.get(i);

            int incomingIndex =
                    request.getPreviousLogIndex() + 1 + i;

            if (incomingIndex < logEntries.size()) {

                LogEntry existing = logEntries.get(incomingIndex);

                // Detect conflicting log terms
                if (existing.getTerm() != incoming.getTerm()) {

                    // SAFETY: Never overwrite a committed entry
                    if (incomingIndex <= lastApplied) {

                        System.out.println(
                                nodeId
                                + " rejected conflict at committed index "
                                + incomingIndex
                        );

                        return new AppendEntriesResponse(
                                currentTerm, false, lastLogIndex
                        );
                    }

                    System.out.println(
                            nodeId + " detected conflict at index "
                            + incomingIndex
                    );

                    // Remove conflicting entry and its suffix
                    while (logEntries.size() > incomingIndex) {

                        logEntries.remove(
                                logEntries.size() - 1
                        );
                    }

                    // Add leader's correct entry
                    logEntries.add(incoming);

                    System.out.println(
                            nodeId + " repaired log at index "
                            + incomingIndex
                    );
                }

            } else {

                // Append missing entry
                logEntries.add(incoming);

                System.out.println(
                        nodeId + " appended replicated entry: "
                        + incoming
                );
            }
        }

        updateLastLogInformation();

        // Advance commit index
        int newCommitIndex = Math.min(
                request.getLeaderCommit(),
                lastLogIndex
        );

        applyCommittedEntries(newCommitIndex);

        return new AppendEntriesResponse(
                currentTerm,
                true,
                previousIndex + incomingEntries.size()
        );
    }

    // ---------------- LOG HELPERS ----------------

    private void updateLastLogInformation() {

        lastLogIndex = logEntries.size() - 1;

        if (lastLogIndex >= 0) {

            lastLogTerm =
                    logEntries.get(lastLogIndex).getTerm();

        } else {

            lastLogTerm = 0;
        }
    }

    public synchronized void addLogEntry(LogEntry entry) {

        if (!alive) {
            return;
        }

        logEntries.add(entry);

        updateLastLogInformation();

        System.out.println(
                nodeId + " appended log entry: " + entry
        );
    }

    // ---------------- RECOVERY SYNCHRONIZATION ----------------

    public synchronized boolean synchronizeFromLeader(
            String leaderId,
            long leaderTerm,
            List<LogEntry> leaderLog,
            int leaderCommitIndex) {

        if (!alive || leaderTerm < currentTerm) {
            return false;
        }

        currentTerm = leaderTerm;

        role = RaftRole.FOLLOWER;
        votedFor = null;
        knownLeader = leaderId;

        resetElectionTimeout();

        int matchingIndex = 0;

        while (matchingIndex < logEntries.size()
                && matchingIndex < leaderLog.size()) {

            LogEntry local = logEntries.get(matchingIndex);
            LogEntry remote = leaderLog.get(matchingIndex);

            if (local.getTerm() != remote.getTerm()) {
                break;
            }

            matchingIndex++;
        }

        // Do not overwrite committed entries
        if (matchingIndex <= lastApplied) {

            System.out.println(
                    nodeId + " cannot repair a committed log conflict."
            );

            return false;
        }

        // Remove conflicting suffix
        while (logEntries.size() > matchingIndex) {

            logEntries.remove(logEntries.size() - 1);
        }

        // Copy missing entries
        for (int i = matchingIndex; i < leaderLog.size(); i++) {

            logEntries.add(leaderLog.get(i));

            System.out.println(
                    nodeId + " copied missing entry: "
                    + leaderLog.get(i)
            );
        }

        updateLastLogInformation();

        applyCommittedEntries(
                Math.min(leaderCommitIndex, lastLogIndex)
        );

        System.out.println(
                nodeId + " successfully synchronized with " + leaderId
        );

        return true;
    }

    // ---------------- APPLY COMMITTED ENTRIES ----------------

    public synchronized void applyCommittedEntries(int commitIndex) {

        while (lastApplied < commitIndex
                && lastApplied + 1 < logEntries.size()) {

            LogEntry entry = logEntries.get(lastApplied + 1);

            if ("PUT".equals(entry.getOperation())) {

                keyValueStore.put(
                        entry.getKey(),
                        entry.getValue()
                );

            } else if ("DELETE".equals(entry.getOperation())) {

                keyValueStore.delete(entry.getKey());
            }

            lastApplied++;

            System.out.println(
                    nodeId + " applied log entry " + lastApplied
            );
        }
    }

    // ---------------- STORAGE ----------------

    public synchronized String getStoredValue(String key) {

        if (!alive) {
            return null;
        }

        return keyValueStore.get(key);
    }

    public synchronized void displayStorage() {

        keyValueStore.displayAll();
    }

    // ---------------- CRASH AND RECOVERY ----------------

    public synchronized void setAlive(boolean alive) {

        this.alive = alive;

        if (!alive) {

            role = RaftRole.FOLLOWER;
            knownLeader = null;

            System.out.println(nodeId + " has crashed!");

        } else {

            role = RaftRole.FOLLOWER;
            knownLeader = null;
            votedFor = null;

            resetElectionTimeout();

            System.out.println(nodeId + " has recovered!");
        }
    }

    // ---------------- GETTERS ----------------

    public String getNodeId() {
        return nodeId;
    }

    public synchronized RaftRole getRole() {
        return role;
    }

    public synchronized long getCurrentTerm() {
        return currentTerm;
    }

    public synchronized String getVotedFor() {
        return votedFor;
    }

    public synchronized boolean isAlive() {
        return alive;
    }

    public synchronized String getKnownLeader() {
        return knownLeader;
    }

    public synchronized int getLastLogIndex() {
        return lastLogIndex;
    }

    public synchronized long getLastLogTerm() {
        return lastLogTerm;
    }

    public synchronized List<LogEntry> getLogEntries() {
        return new ArrayList<>(logEntries);
    }
}
