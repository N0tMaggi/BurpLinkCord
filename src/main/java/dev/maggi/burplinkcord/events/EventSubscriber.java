package dev.maggi.burplinkcord.events;

/**
 * Receives internal application events.
 */
@FunctionalInterface
public interface EventSubscriber {

    /**
     * Handles an event.
     *
     * @param eventType event type
     * @param payload event payload
     */
    void onEvent(String eventType, Object payload);
}
