package dev.maggi.burplinkcord.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Records security-relevant user and system actions.
 */
@FunctionalInterface
public interface AuditLogger {

    /**
     * Writes an audit entry.
     *
     * @param action action description
     */
    void log(String action);

    /**
     * Creates a logger-backed audit logger.
     *
     * @param enabled whether logging is enabled
     * @return audit logger implementation
     */
    static AuditLogger create(boolean enabled) {
        return new Slf4jAuditLogger(LoggerFactory.getLogger("dev.maggi.burplinkcord.audit"), enabled);
    }
}

final class Slf4jAuditLogger implements AuditLogger {

    private final Logger logger;
    private final boolean enabled;

    Slf4jAuditLogger(Logger logger, boolean enabled) {
        this.logger = logger;
        this.enabled = enabled;
    }

    @Override
    public void log(String action) {
        if (enabled) {
            logger.info(action);
        }
    }
}
