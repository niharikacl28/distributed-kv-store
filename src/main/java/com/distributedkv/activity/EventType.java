package com.distributedkv.activity;

/**
 * The kinds of real events the Raft cluster records for the activity timeline.
 * These mirror the operations that actually happen inside the cluster.
 */
public enum EventType {

    LEADER_ELECTED,
    HEARTBEAT,
    PUT_RECEIVED,
    ENTRY_REPLICATED,
    ENTRY_COMMITTED,
    DELETE_RECEIVED,
    NODE_CRASHED,
    NODE_RECOVERED,
    WRITE_FAILED
}
