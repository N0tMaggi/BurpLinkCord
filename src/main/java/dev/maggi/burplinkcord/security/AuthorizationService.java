package dev.maggi.burplinkcord.security;

/**
 * Authorizes authenticated callers for named actions.
 */
@FunctionalInterface
public interface AuthorizationService {

    /**
     * Authorizes an action for a principal.
     *
     * @param principal authenticated principal
     * @param action action name
     */
    void authorize(AuthenticatedPrincipal principal, String action);
}
