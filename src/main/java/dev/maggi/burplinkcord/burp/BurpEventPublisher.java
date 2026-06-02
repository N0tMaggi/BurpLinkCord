package dev.maggi.burplinkcord.burp;

import dev.maggi.burplinkcord.events.EventBus;
import dev.maggi.burplinkcord.events.RuntimeEvent;
import dev.maggi.burplinkcord.logging.AuditLogger;

import java.util.Objects;

/**
 * Bridges Burp lifecycle events into the internal event bus.
 */
public class BurpEventPublisher {

    private final EventBus eventBus;
    private final AuditLogger auditLogger;

    /**
     * Creates a Burp event publisher.
     *
     * @param eventBus event bus dependency
     * @param auditLogger audit logger dependency
     */
    public BurpEventPublisher(EventBus eventBus, AuditLogger auditLogger) {
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
        this.auditLogger = Objects.requireNonNull(auditLogger, "auditLogger");
    }

    /**
     * Registers Burp-facing event hooks.
     */
    public void register() {
        auditLogger.log("burp.events.register");
        eventBus.publish("burp.lifecycle.ready", "BurpLinkCord lifecycle ready");
        eventBus.publish("runtime.notification", new RuntimeEvent(
                "BurpLinkCord Ready",
                "BurpLinkCord registered its Burp lifecycle hooks and is ready.",
                "INFO"
        ));
    }
}
