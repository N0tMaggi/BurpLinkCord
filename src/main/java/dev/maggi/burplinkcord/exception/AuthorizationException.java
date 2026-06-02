package dev.maggi.burplinkcord.exception;

/**
 * Signals rejected authorization decisions.
 */
public class AuthorizationException extends BurpLinkCordException {

    /**
     * Creates an authorization exception.
     *
     * @param message exception message
     */
    public AuthorizationException(String message) {
        super(message);
    }
}
