package dev.maggi.burplinkcord.exception;

/**
 * Signals failed authentication attempts.
 */
public class AuthenticationException extends BurpLinkCordException {

    /**
     * Creates an authentication exception.
     *
     * @param message exception message
     */
    public AuthenticationException(String message) {
        super(message);
    }
}
