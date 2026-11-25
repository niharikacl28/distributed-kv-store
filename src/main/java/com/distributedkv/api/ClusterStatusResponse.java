package com.distributedkv.api;

import java.util.List;

public record ClusterStatusResponse(
        int totalNodes,
        int aliveNodes,
        int crashedNodes,
        String leaderId,
        int commitIndex,
        List<NodeStatusResponse> nodes) {
}
