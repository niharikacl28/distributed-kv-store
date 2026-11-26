package com.distributedkv.persistence;

import java.util.Optional;

/**
 * Persists and restores the durable Raft state of a single node.
 *
 * <p>Implementations must be safe to call concurrently and must never
 * expose partially written state: a {@link #load()} performed after a
 * {@link #save(RaftPersistentState)} must observe either the previous
 * complete state or the new complete state, never a mixture.</p>
 */
public interface NodeStateStore {

    /**
     * Durably stores the supplied node state.
     *
     * @param state the state to persist
     */
    void save(RaftPersistentState state);

    /**
     * Loads the previously persisted node state.
     *
     * @return the stored state, or {@link Optional#empty()} when nothing has
     *         been persisted yet or the stored state is corrupt/unreadable.
     *         Corrupt state is never silently reported as valid.
     */
    Optional<RaftPersistentState> load();
}
