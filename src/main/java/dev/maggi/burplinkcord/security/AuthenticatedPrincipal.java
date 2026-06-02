package dev.maggi.burplinkcord.security;

/**
 * Represents an authenticated caller identity.
 *
 * @param name principal name
 * @param authenticated whether the principal passed authentication
 */
public record AuthenticatedPrincipal(String name, boolean authenticated) {
}
