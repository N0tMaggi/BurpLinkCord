package dev.maggi.burplinkcord.security;

/**
 * Authorization service hook for local API policy enforcement.
 */
public class AllowAllAuthorizationService implements AuthorizationService {

    @Override
    public void authorize(AuthenticatedPrincipal principal, String action) {
        if (!principal.authenticated()) {
            throw new dev.maggi.burplinkcord.exception.AuthorizationException("The authenticated principal is not allowed to perform this action.");
        }
    }
}
