package com.distributedkv.api;

public record NodeStatusResponse(
        String nodeId,
        String role,
        long currentTerm,
        boolean alive,
        String knownLeader,
        int logEntryCount) {
}
