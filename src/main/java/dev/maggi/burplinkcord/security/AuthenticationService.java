package dev.maggi.burplinkcord.security;

/**
 * Authenticates incoming callers.
 */
@FunctionalInterface
public interface AuthenticationService {

    /**
     * Authenticates a caller from an authorization header.
     *
     * @param authorizationHeader authorization header value
     * @return authenticated principal
     */
    AuthenticatedPrincipal authenticate(String authorizationHeader);
}
