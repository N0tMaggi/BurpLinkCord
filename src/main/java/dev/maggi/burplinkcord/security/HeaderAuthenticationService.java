package dev.maggi.burplinkcord.security;

import dev.maggi.burplinkcord.domain.service.SettingsService;
import dev.maggi.burplinkcord.exception.AuthenticationException;

import java.util.Objects;

/**
 * Header-based authentication service for the local API.
 */
public class HeaderAuthenticationService implements AuthenticationService {

    private final SettingsService settingsService;

    /**
     * Creates a header authentication service.
     *
     * @param settingsService settings service dependency
     */
    public HeaderAuthenticationService(SettingsService settingsService) {
        this.settingsService = Objects.requireNonNull(settingsService, "settingsService");
    }

    @Override
    public AuthenticatedPrincipal authenticate(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new AuthenticationException("Missing Authorization header.");
        }

        String configuredSecret = settingsService.loadApiSharedSecret();
        if (configuredSecret.isBlank()) {
            throw new AuthenticationException("Local API shared secret is not configured.");
        }

        String expectedHeader = "Bearer " + configuredSecret;
        if (!expectedHeader.equals(authorizationHeader.trim())) {
            throw new AuthenticationException("Invalid Authorization header.");
        }

        return new AuthenticatedPrincipal("local-api-client", true);
    }
}
