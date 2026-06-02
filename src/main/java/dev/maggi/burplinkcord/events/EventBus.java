package dev.maggi.burplinkcord.events;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Coordinates application event publication and subscription.
 */
public interface EventBus extends EventPublisher {

    /**
     * Subscribes to a named event type.
     *
     * @param eventType event type
     * @param subscriber subscriber callback
     * @return handle for unsubscribing
     */
    AutoCloseable subscribe(String eventType, EventSubscriber subscriber);

    /**
     * Creates an in-memory event bus.
     *
     * @return event bus instance
     */
    static EventBus inMemory() {
        return new InMemoryEventBus();
    }
}

final class InMemoryEventBus implements EventBus {

    private final Map<String, List<EventSubscriber>> subscribers = new ConcurrentHashMap<>();

    @Override
    public AutoCloseable subscribe(String eventType, EventSubscriber subscriber) {
        subscribers.computeIfAbsent(eventType, ignored -> new CopyOnWriteArrayList<>()).add(subscriber);
        return () -> subscribers.getOrDefault(eventType, List.of()).remove(subscriber);
    }

    @Override
    public void publish(String eventType, Object payload) {
        subscribers.getOrDefault(eventType, List.of()).forEach(subscriber -> subscriber.onEvent(eventType, payload));
    }
}
