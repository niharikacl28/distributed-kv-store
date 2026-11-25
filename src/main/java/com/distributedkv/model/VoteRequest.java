
package com.distributedkv.model;

public class VoteRequest {

    private final String candidateId;
    private final long term;
    private final int lastLogIndex;
    private final long lastLogTerm;

    public VoteRequest(String candidateId, long term,
                       int lastLogIndex, long lastLogTerm) {
        this.candidateId = candidateId;
        this.term = term;
        this.lastLogIndex = lastLogIndex;
        this.lastLogTerm = lastLogTerm;
    }

    public String getCandidateId() {
        return candidateId;
    }

    public long getTerm() {
        return term;
    }

    public int getLastLogIndex() {
        return lastLogIndex;
    }

    public long getLastLogTerm() {
        return lastLogTerm;
    }
}
