package dev.maggi.burplinkcord.exception;

/**
 * Signals invalid or unreadable application configuration.
 */
public class ConfigurationException extends BurpLinkCordException {

    /**
     * Creates a configuration exception.
     *
     * @param message exception message
     */
    public ConfigurationException(String message) {
        super(message);
    }

    /**
     * Creates a configuration exception with cause.
     *
     * @param message exception message
     * @param cause root cause
     */
    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
