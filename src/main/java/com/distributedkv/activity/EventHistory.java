package com.distributedkv.activity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Lightweight, bounded, in-memory history of recent cluster events.
 *
 * <p>Only events that occur while the application is running are stored; nothing
 * is fabricated or back-filled from before the feature existed. The history is
 * capped at {@link #MAX_EVENTS} and evicts the oldest entries first, so memory
 * usage stays bounded.
 */
public final class EventHistory {

    /** Maximum number of events retained; the oldest are evicted first. */
    public static final int MAX_EVENTS = 100;

    private final Deque<ActivityEvent> events = new ArrayDeque<>();

    private long nextId = 1;

    /**
     * Records a new event stamped with the current time.
     *
     * @return the recorded event
     */
    public synchronized ActivityEvent record(
            EventType type,
            String description,
            String nodeId,
            long term) {

        ActivityEvent event = new ActivityEvent(
                nextId++,
                type,
                description,
                nodeId,
                term,
                System.currentTimeMillis());

        events.addFirst(event);

        while (events.size() > MAX_EVENTS) {
            events.removeLast();
        }

        return event;
    }

    /** Returns a snapshot of the retained events, newest first. */
    public synchronized List<ActivityEvent> recentEvents() {
        return new ArrayList<>(events);
    }

    public synchronized int size() {
        return events.size();
    }
}
