package dev.maggi.burplinkcord;

import dev.maggi.burplinkcord.config.RuntimeSettings;
import dev.maggi.burplinkcord.discord.DiscordAccessPolicy;
import dev.maggi.burplinkcord.domain.service.SettingsService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscordAccessPolicyTest {

    @Test
    void shouldAllowWhitelistedUserInAllowedChannel() {
        DiscordAccessPolicy policy = new DiscordAccessPolicy(new FixedSettingsService(new RuntimeSettings(
                true,
                true,
                "1234567890",
                java.util.List.of("42"),
                java.util.List.of("99"),
                "777",
                java.util.List.of("example.org"),
                "Balanced",
                "Default"
        )));

        assertTrue(policy.isAllowed("42", "1234567890", "99"));
    }

    @Test
    void shouldRejectUserOutsideWhitelist() {
        DiscordAccessPolicy policy = new DiscordAccessPolicy(new FixedSettingsService(new RuntimeSettings(
                true,
                true,
                "1234567890",
                java.util.List.of("42"),
                java.util.List.of("99"),
                "777",
                java.util.List.of(),
                "Balanced",
                "Default"
        )));

        assertFalse(policy.isAllowed("1337", "1234567890", "99"));
    }

    @Test
    void shouldRejectGuildOutsideWhitelist() {
        DiscordAccessPolicy policy = new DiscordAccessPolicy(new FixedSettingsService(new RuntimeSettings(
                true,
                true,
                "1234567890",
                java.util.List.of("42"),
                java.util.List.of("99"),
                "777",
                java.util.List.of(),
                "Balanced",
                "Default"
        )));

        assertFalse(policy.isAllowed("42", "other-guild", "99"));
    }

    @Test
    void shouldAllowAnyGuildWhenNoGuildWhitelistIsConfigured() {
        DiscordAccessPolicy policy = new DiscordAccessPolicy(new FixedSettingsService(new RuntimeSettings(
                true,
                true,
                "",
                java.util.List.of("42"),
                java.util.List.of("99"),
                "777",
                java.util.List.of(),
                "Balanced",
                "Default"
        )));

        assertTrue(policy.isAllowed("42", "another-guild", "99"));
    }

    private record FixedSettingsService(RuntimeSettings settings) implements SettingsService {

        @Override
        public RuntimeSettings getSettings() {
            return settings;
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
            return "";
        }

        @Override
        public void saveApiSharedSecret(String secret) {
        }
    }
}
