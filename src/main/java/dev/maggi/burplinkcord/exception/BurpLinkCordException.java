package dev.maggi.burplinkcord.exception;

/**
 * Base exception for all BurpLinkCord application errors.
 */
public class BurpLinkCordException extends RuntimeException {

    /**
     * Creates a new exception with a message.
     *
     * @param message exception message
     */
    public BurpLinkCordException(String message) {
        super(message);
    }

    /**
     * Creates a new exception with a message and cause.
     *
     * @param message exception message
     * @param cause root cause
     */
    public BurpLinkCordException(String message, Throwable cause) {
        super(message, cause);
    }
}
