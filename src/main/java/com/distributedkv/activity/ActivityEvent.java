package com.distributedkv.activity;

/**
 * A single recorded cluster event. Immutable.
 *
 * @param id          monotonically increasing identifier assigned when recorded
 * @param type        the kind of event
 * @param description human-readable summary of what happened
 * @param nodeId      the node the event relates to, or null when not applicable
 * @param term        the Raft term in effect when the event occurred (0 if unknown)
 * @param timestamp   epoch millis when the event was recorded
 */
public record ActivityEvent(
        long id,
        EventType type,
        String description,
        String nodeId,
        long term,
        long timestamp) {
}
