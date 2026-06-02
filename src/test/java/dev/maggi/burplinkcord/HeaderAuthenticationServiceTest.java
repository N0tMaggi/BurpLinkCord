package dev.maggi.burplinkcord;

import dev.maggi.burplinkcord.config.RuntimeSettings;
import dev.maggi.burplinkcord.domain.service.SettingsService;
import dev.maggi.burplinkcord.exception.AuthenticationException;
import dev.maggi.burplinkcord.security.AuthenticatedPrincipal;
import dev.maggi.burplinkcord.security.HeaderAuthenticationService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeaderAuthenticationServiceTest {

    @Test
    void shouldAuthenticateValidBearerToken() {
        HeaderAuthenticationService service = new HeaderAuthenticationService(new FixedSettingsService("super-secret"));

        AuthenticatedPrincipal principal = service.authenticate("Bearer super-secret");

        assertEquals("local-api-client", principal.name());
        assertTrue(principal.authenticated());
    }

    @Test
    void shouldRejectInvalidBearerToken() {
        HeaderAuthenticationService service = new HeaderAuthenticationService(new FixedSettingsService("super-secret"));

        assertThrows(AuthenticationException.class, () -> service.authenticate("Bearer wrong"));
    }

    private record FixedSettingsService(String apiSecret) implements SettingsService {

        @Override
        public RuntimeSettings getSettings() {
            return RuntimeSettings.defaults();
        }

        @Override
        public void saveSettings(RuntimeSettings settings) {
        }

        @Override
        public String loadDiscordBotToken() {
            return "";
        }

        @Override
        public void saveDiscordBotToken(String token) {
        }

        @Override
        public String loadApiSharedSecret() {
            return apiSecret;
        }

        @Override
        public void saveApiSharedSecret(String secret) {
        }
    }
}
