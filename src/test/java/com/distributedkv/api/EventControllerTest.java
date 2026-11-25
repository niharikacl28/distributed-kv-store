package com.distributedkv.api;

import com.distributedkv.cluster.RaftCluster;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EventControllerTest {

    private MockMvc mockMvcFor(RaftCluster cluster) {
        return MockMvcBuilders
                .standaloneSetup(new EventController(cluster))
                .build();
    }

    private RaftCluster clusterWithLeader() {
        RaftCluster cluster = new RaftCluster(5);
        cluster.startElection("Node-1");
        return cluster;
    }

    @Test
    void electionIsReportedAsNewestEvent() throws Exception {
        RaftCluster cluster = clusterWithLeader();

        mockMvcFor(cluster)
                .perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("LEADER_ELECTED"))
                .andExpect(jsonPath("$[0].nodeId").value("Node-1"))
                .andExpect(jsonPath("$[0].term").value(1));
    }

    @Test
    void putProducesReceivedReplicatedAndCommittedEvents() throws Exception {
        RaftCluster cluster = clusterWithLeader();
        cluster.replicateLog("PUT", "name", "Niharika");

        mockMvcFor(cluster)
                .perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.type=='LEADER_ELECTED')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.type=='PUT_RECEIVED')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.type=='ENTRY_REPLICATED')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.type=='ENTRY_COMMITTED')]").isNotEmpty());
    }

    @Test
    void deleteIsReportedAsDeleteReceived() throws Exception {
        RaftCluster cluster = clusterWithLeader();
        cluster.replicateLog("PUT", "name", "Niharika");
        cluster.replicateLog("DELETE", "name", null);

        mockMvcFor(cluster)
                .perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.type=='DELETE_RECEIVED')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.type=='ENTRY_COMMITTED')]").isNotEmpty());
    }

    @Test
    void failedWriteWithoutQuorumIsReported() throws Exception {
        RaftCluster cluster = clusterWithLeader();
        cluster.crashNode("Node-2");
        cluster.crashNode("Node-3");
        cluster.crashNode("Node-4");

        cluster.replicateLog("PUT", "name", "Niharika");

        mockMvcFor(cluster)
                .perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("WRITE_FAILED"));
    }

    @Test
    void crashAndRecoveryAreReported() throws Exception {
        RaftCluster cluster = clusterWithLeader();
        cluster.crashNode("Node-2");
        cluster.recoverNode("Node-2");

        mockMvcFor(cluster)
                .perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("NODE_RECOVERED"))
                .andExpect(jsonPath("$[0].nodeId").value("Node-2"))
                .andExpect(jsonPath("$[1].type").value("NODE_CRASHED"))
                .andExpect(jsonPath("$[1].nodeId").value("Node-2"));
    }

    @Test
    void noLeaderWriteIsReportedAsWriteFailed() throws Exception {
        RaftCluster cluster = new RaftCluster(5);
        cluster.replicateLog("PUT", "name", "Niharika");

        mockMvcFor(cluster)
                .perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("WRITE_FAILED"));
    }
}
