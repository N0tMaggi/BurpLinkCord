package dev.maggi.burplinkcord.events;

/**
 * Publishes internal application events.
 */
@FunctionalInterface
public interface EventPublisher {

    /**
     * Publishes an event.
     *
     * @param eventType event type
     * @param payload event payload
     */
    void publish(String eventType, Object payload);
}
