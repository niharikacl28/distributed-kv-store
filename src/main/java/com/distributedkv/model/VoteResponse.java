
package com.distributedkv.model;

public class VoteResponse {

    private final long term;
    private final boolean voteGranted;

    public VoteResponse(long term, boolean voteGranted) {
        this.term = term;
        this.voteGranted = voteGranted;
    }

    public long getTerm() {
        return term;
    }

    public boolean isVoteGranted() {
        return voteGranted;
    }
}
