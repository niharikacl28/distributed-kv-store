package com.distributedkv.api;

import com.distributedkv.cluster.RaftCluster;
import com.distributedkv.node.RaftNode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/cluster")
public class ClusterStatusController {

    private final RaftCluster raftCluster;

    public ClusterStatusController(RaftCluster raftCluster) {
        this.raftCluster = raftCluster;
    }

    @GetMapping("/status")
    public ClusterStatusResponse status() {
        List<RaftNode> nodes = raftCluster.getNodes();
        List<NodeStatusResponse> nodeStatuses = new ArrayList<>();
        int aliveNodes = 0;

        for (RaftNode node : nodes) {
            if (node.isAlive()) {
                aliveNodes++;
            }

            nodeStatuses.add(new NodeStatusResponse(
                    node.getNodeId(),
                    node.getRole().name(),
                    node.getCurrentTerm(),
                    node.isAlive(),
                    node.getKnownLeader(),
                    node.getLogEntries().size()
            ));
        }

        RaftNode leader = raftCluster.getLeader();

        return new ClusterStatusResponse(
                nodes.size(),
                aliveNodes,
                nodes.size() - aliveNodes,
                leader == null ? null : leader.getNodeId(),
                raftCluster.getCommitIndex(),
                nodeStatuses
        );
    }
}
