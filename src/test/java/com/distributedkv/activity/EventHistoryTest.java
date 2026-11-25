package com.distributedkv.activity;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventHistoryTest {

    @Test
    void recentEventsAreReturnedNewestFirstWithIncreasingIds() {
        EventHistory history = new EventHistory();

        history.record(EventType.LEADER_ELECTED, "first", "Node-1", 1);
        history.record(EventType.PUT_RECEIVED, "second", "Node-1", 1);

        List<ActivityEvent> events = history.recentEvents();

        assertEquals(2, events.size());
        assertEquals("second", events.get(0).description());
        assertEquals("first", events.get(1).description());
        assertTrue(events.get(0).id() > events.get(1).id());
    }

    @Test
    void historyIsBoundedToMaxEventsAndDropsOldest() {
        EventHistory history = new EventHistory();

        for (int i = 0; i < EventHistory.MAX_EVENTS + 25; i++) {
            history.record(EventType.HEARTBEAT, "event-" + i, "Node-1", 1);
        }

        List<ActivityEvent> events = history.recentEvents();

        assertEquals(EventHistory.MAX_EVENTS, events.size());
        // Newest retained event is the last one recorded.
        assertEquals(
                "event-" + (EventHistory.MAX_EVENTS + 24),
                events.get(0).description());
    }
}
