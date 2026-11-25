package com.distributedkv.api;

import com.distributedkv.activity.ActivityEvent;
import com.distributedkv.cluster.RaftCluster;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final RaftCluster raftCluster;

    public EventController(RaftCluster raftCluster) {
        this.raftCluster = raftCluster;
    }

    /**
     * GET /api/events
     * Returns the recent cluster events, newest first. Only events recorded
     * since the application started are returned; none are fabricated.
     */
    @GetMapping
    public List<ActivityEvent> recentEvents() {
        return raftCluster.getEventHistory().recentEvents();
    }
}
