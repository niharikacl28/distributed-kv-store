package com.distributedkv.api;

import com.distributedkv.cluster.RaftCluster;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class KeyValueControllerTest {

    private MockMvc mockMvcFor(RaftCluster cluster) {
        return MockMvcBuilders
                .standaloneSetup(new KeyValueController(cluster))
                .build();
    }

    private RaftCluster clusterWithLeader() {
        RaftCluster cluster = new RaftCluster(5);
        cluster.startElection("Node-1");
        return cluster;
    }

    private RaftCluster clusterWithoutQuorum() {
        RaftCluster cluster = clusterWithLeader();
        cluster.crashNode("Node-2");
        cluster.crashNode("Node-3");
        cluster.crashNode("Node-4");
        return cluster;
    }

    @Test
    void putThenGetReturnsCommittedValue() throws Exception {
        MockMvc mockMvc = mockMvcFor(clusterWithLeader());

        mockMvc.perform(put("/api/kv/name")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"Niharika\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value("name"))
                .andExpect(jsonPath("$.value").value("Niharika"));

        mockMvc.perform(get("/api/kv/name"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value("name"))
                .andExpect(jsonPath("$.value").value("Niharika"));
    }

    @Test
    void getMissingKeyReturnsNotFound() throws Exception {
        mockMvcFor(clusterWithLeader())
                .perform(get("/api/kv/unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteRemovesCommittedValue() throws Exception {
        MockMvc mockMvc = mockMvcFor(clusterWithLeader());

        mockMvc.perform(put("/api/kv/name")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"Niharika\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/kv/name"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/kv/name"))
                .andExpect(status().isNotFound());
    }

    @Test
    void blankValueReturnsBadRequest() throws Exception {
        mockMvcFor(clusterWithLeader())
                .perform(put("/api/kv/name")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void putWithoutLeaderReturnsServiceUnavailable() throws Exception {
        mockMvcFor(new RaftCluster(5))
                .perform(put("/api/kv/name")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"Niharika\"}"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void getWithoutLeaderReturnsServiceUnavailable() throws Exception {
        mockMvcFor(new RaftCluster(5))
                .perform(get("/api/kv/name"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void putWithoutQuorumReturnsServiceUnavailable() throws Exception {
        MockMvc mockMvc = mockMvcFor(clusterWithoutQuorum());

        mockMvc.perform(put("/api/kv/name")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"Niharika\"}"))
                .andExpect(status().isServiceUnavailable());

        // The value must not be readable because it was never committed.
        mockMvc.perform(get("/api/kv/name"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteWithoutQuorumReturnsServiceUnavailable() throws Exception {
        MockMvc mockMvc = mockMvcFor(clusterWithoutQuorum());

        mockMvc.perform(delete("/api/kv/name"))
                .andExpect(status().isServiceUnavailable());
    }
}
