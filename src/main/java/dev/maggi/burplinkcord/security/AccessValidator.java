package dev.maggi.burplinkcord.security;

import dev.maggi.burplinkcord.config.ApplicationConfiguration;
import dev.maggi.burplinkcord.exception.AuthenticationException;

import java.util.Objects;

/**
 * Coordinates authentication and authorization for protected entry points.
 */
public class AccessValidator {

    private final ApplicationConfiguration applicationConfiguration;
    private final AuthenticationService authenticationService;
    private final AuthorizationService authorizationService;

    /**
     * Creates an access validator.
     *
     * @param applicationConfiguration bootstrap configuration
     * @param authenticationService authentication dependency
     * @param authorizationService authorization dependency
     */
    public AccessValidator(
            ApplicationConfiguration applicationConfiguration,
            AuthenticationService authenticationService,
            AuthorizationService authorizationService
    ) {
        this.applicationConfiguration = Objects.requireNonNull(applicationConfiguration, "applicationConfiguration");
        this.authenticationService = Objects.requireNonNull(authenticationService, "authenticationService");
        this.authorizationService = Objects.requireNonNull(authorizationService, "authorizationService");
    }

    /**
     * Validates access to a named action.
     *
     * @param authorizationHeader authorization header
     * @param action action name
     * @return authenticated principal
     */
    public AuthenticatedPrincipal validate(String authorizationHeader, String action) {
        if (!applicationConfiguration.securityConfiguration().enabled()) {
            return new AuthenticatedPrincipal("local-extension", true);
        }

        AuthenticatedPrincipal principal = authenticationService.authenticate(authorizationHeader);
        if (!principal.authenticated()) {
            throw new AuthenticationException("Authentication failed for action " + action);
        }

        authorizationService.authorize(principal, action);
        return principal;
    }
}
