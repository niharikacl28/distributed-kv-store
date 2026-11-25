
package com.distributedkv.model;

import java.util.ArrayList;
import java.util.List;

public class AppendEntriesRequest {

    private final long term;
    private final String leaderId;

    private final int previousLogIndex;
    private final long previousLogTerm;

    private final List<LogEntry> entries;

    private final int leaderCommit;

    public AppendEntriesRequest(
            long term,
            String leaderId,
            int previousLogIndex,
            long previousLogTerm,
            List<LogEntry> entries,
            int leaderCommit) {

        this.term = term;
        this.leaderId = leaderId;
        this.previousLogIndex = previousLogIndex;
        this.previousLogTerm = previousLogTerm;

        this.entries = new ArrayList<>(entries);

        this.leaderCommit = leaderCommit;
    }

    public long getTerm() {
        return term;
    }

    public String getLeaderId() {
        return leaderId;
    }

    public int getPreviousLogIndex() {
        return previousLogIndex;
    }

    public long getPreviousLogTerm() {
        return previousLogTerm;
    }

    public List<LogEntry> getEntries() {
        return new ArrayList<>(entries);
    }

    public int getLeaderCommit() {
        return leaderCommit;
    }
}
