package com.distributedkv.persistence;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * The durable Raft state of a single node.
 *
 * <p>Only the state that Raft requires to be stable across restarts is
 * included here: {@code currentTerm}, {@code votedFor}, the log,
 * {@code commitIndex} and {@code lastApplied}. Volatile state such as the
 * role, the known leader and the leader replication indexes is deliberately
 * excluded because it is reconstructed after a restart.</p>
 */
public record RaftPersistentState(
        @JsonProperty("currentTerm") long currentTerm,
        @JsonProperty("votedFor") String votedFor,
        @JsonProperty("commitIndex") int commitIndex,
        @JsonProperty("lastApplied") int lastApplied,
        @JsonProperty("log") List<LogEntryState> log) {
}
