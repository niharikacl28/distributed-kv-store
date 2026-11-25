
package com.distributedkv.model;

public class Heartbeat {

    private final String leaderId;
    private final long term;

    public Heartbeat(String leaderId, long term) {
        this.leaderId = leaderId;
        this.term = term;
    }

    public String getLeaderId() {
        return leaderId;
    }

    public long getTerm() {
        return term;
    }
}
