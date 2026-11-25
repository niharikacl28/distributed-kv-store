package com.distributedkv.api;

import com.distributedkv.cluster.RaftCluster;
import com.distributedkv.node.RaftNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/nodes")
public class NodeController {

    private final RaftCluster raftCluster;

    public NodeController(RaftCluster raftCluster) {
        this.raftCluster = raftCluster;
    }

    @PostMapping("/{nodeId}/crash")
    public ResponseEntity<?> crash(@PathVariable String nodeId) {

        RaftNode node = findNode(nodeId);

        if (node == null) {
            return unknownNode(nodeId);
        }

        if (!node.isAlive()) {
            return conflict(nodeId + " is already crashed.");
        }

        raftCluster.crashNode(nodeId);

        return ResponseEntity.ok(toStatus(node));
    }

    @PostMapping("/{nodeId}/recover")
    public ResponseEntity<?> recover(@PathVariable String nodeId) {

        RaftNode node = findNode(nodeId);

        if (node == null) {
            return unknownNode(nodeId);
        }

        if (node.isAlive()) {
            return conflict(nodeId + " is already running.");
        }

        raftCluster.recoverNode(nodeId);

        return ResponseEntity.ok(toStatus(node));
    }

    private RaftNode findNode(String nodeId) {

        for (RaftNode node : raftCluster.getNodes()) {

            if (node.getNodeId().equals(nodeId)) {
                return node;
            }
        }

        return null;
    }

    private NodeStatusResponse toStatus(RaftNode node) {

        return new NodeStatusResponse(
                node.getNodeId(),
                node.getRole().name(),
                node.getCurrentTerm(),
                node.isAlive(),
                node.getKnownLeader(),
                node.getLogEntries().size()
        );
    }

    private ResponseEntity<ErrorResponse> unknownNode(String nodeId) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("Unknown node: " + nodeId));
    }

    private ResponseEntity<ErrorResponse> conflict(String message) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(message));
    }
}
