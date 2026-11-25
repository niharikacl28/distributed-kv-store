
package com.distributedkv.model;

public class AppendEntriesResponse {

    private final long term;
    private final boolean success;
    private final int matchIndex;

    public AppendEntriesResponse(
            long term,
            boolean success,
            int matchIndex) {

        this.term = term;
        this.success = success;
        this.matchIndex = matchIndex;
    }

    public long getTerm() {
        return term;
    }

    public boolean isSuccess() {
        return success;
    }

    public int getMatchIndex() {
        return matchIndex;
    }
}
