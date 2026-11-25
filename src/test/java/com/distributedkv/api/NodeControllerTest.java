package com.distributedkv.api;

import com.distributedkv.cluster.RaftCluster;
import com.distributedkv.node.RaftNode;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NodeControllerTest {

    private MockMvc mockMvcFor(RaftCluster cluster) {
        return MockMvcBuilders
                .standaloneSetup(
                        new NodeController(cluster),
                        new ClusterStatusController(cluster))
                .build();
    }

    private RaftCluster clusterWithLeader() {
        RaftCluster cluster = new RaftCluster(5);
        cluster.startElection("Node-1");
        return cluster;
    }

    private RaftNode node(RaftCluster cluster, String nodeId) {
        return cluster.getNodes().stream()
                .filter(n -> n.getNodeId().equals(nodeId))
                .findFirst()
                .orElseThrow();
    }

    // ---------------- FOLLOWER CRASH ----------------

    @Test
    void crashFollowerReturnsOkAndMarksNodeDown() throws Exception {
        RaftCluster cluster = clusterWithLeader();

        mockMvcFor(cluster)
                .perform(post("/api/nodes/Node-2/crash"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodeId").value("Node-2"))
                .andExpect(jsonPath("$.alive").value(false));

        assertFalse(node(cluster, "Node-2").isAlive());
    }

    // ---------------- LEADER CRASH ----------------

    @Test
    void crashLeaderReturnsOkAndClearsLeader() throws Exception {
        RaftCluster cluster = clusterWithLeader();

        mockMvcFor(cluster)
                .perform(post("/api/nodes/Node-1/crash"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodeId").value("Node-1"))
                .andExpect(jsonPath("$.alive").value(false));

        assertNull(cluster.getLeader());
    }

    // ---------------- RECOVERY ----------------

    @Test
    void recoverFollowerReturnsOkAndMarksNodeUp() throws Exception {
        RaftCluster cluster = clusterWithLeader();
        cluster.crashNode("Node-2");

        mockMvcFor(cluster)
                .perform(post("/api/nodes/Node-2/recover"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodeId").value("Node-2"))
                .andExpect(jsonPath("$.alive").value(true));

        assertTrue(node(cluster, "Node-2").isAlive());
    }

    // ---------------- INVALID OPERATIONS ----------------

    @Test
    void crashAlreadyCrashedNodeReturnsConflict() throws Exception {
        RaftCluster cluster = clusterWithLeader();
        cluster.crashNode("Node-2");

        mockMvcFor(cluster)
                .perform(post("/api/nodes/Node-2/crash"))
                .andExpect(status().isConflict());
    }

    @Test
    void recoverAlreadyAliveNodeReturnsConflict() throws Exception {
        mockMvcFor(clusterWithLeader())
                .perform(post("/api/nodes/Node-2/recover"))
                .andExpect(status().isConflict());
    }

    // ---------------- INVALID NODE IDS ----------------

    @Test
    void crashUnknownNodeReturnsNotFound() throws Exception {
        mockMvcFor(clusterWithLeader())
                .perform(post("/api/nodes/Node-99/crash"))
                .andExpect(status().isNotFound());
    }

    @Test
    void recoverUnknownNodeReturnsNotFound() throws Exception {
        mockMvcFor(clusterWithLeader())
                .perform(post("/api/nodes/Node-99/recover"))
                .andExpect(status().isNotFound());
    }

    // ---------------- CLUSTER STATUS ----------------

    @Test
    void clusterStatusReflectsCrashedNode() throws Exception {
        RaftCluster cluster = clusterWithLeader();
        cluster.crashNode("Node-2");

        mockMvcFor(cluster)
                .perform(get("/api/cluster/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalNodes").value(5))
                .andExpect(jsonPath("$.aliveNodes").value(4))
                .andExpect(jsonPath("$.crashedNodes").value(1))
                .andExpect(jsonPath("$.leaderId").value("Node-1"));
    }
}
