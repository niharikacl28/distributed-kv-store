package com.distributedkv.persistence;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Serializable mirror of {@link com.distributedkv.model.LogEntry}.
 *
 * <p>Kept as a separate DTO so the domain model is not coupled to the
 * persistence format. A one-to-one mapping with {@code LogEntry} is used
 * when saving and restoring the Raft log.</p>
 */
public record LogEntryState(
        @JsonProperty("index") int index,
        @JsonProperty("term") long term,
        @JsonProperty("operation") String operation,
        @JsonProperty("key") String key,
        @JsonProperty("value") String value) {
}
