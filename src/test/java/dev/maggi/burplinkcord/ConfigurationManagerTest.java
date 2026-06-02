package dev.maggi.burplinkcord;

import dev.maggi.burplinkcord.config.ApplicationConfiguration;
import dev.maggi.burplinkcord.config.ConfigurationManager;
import dev.maggi.burplinkcord.config.RuntimeSettings;
import dev.maggi.burplinkcord.config.SettingsStore;
import dev.maggi.burplinkcord.exception.ConfigurationException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigurationManagerTest {

    @Test
    void shouldLoadApplicationYamlIntoTypedConfiguration() {
        ConfigurationManager manager = new ConfigurationManager(
                () -> new ByteArrayInputStream(("""
                        server:
                          host: localhost
                          port: 8765
                        security:
                          enabled: true
                        discord:
                          enabled: false
                          presence: Watching BurpLinkCord
                        logging:
                          audit: true
                        """).getBytes(StandardCharsets.UTF_8)),
                new InMemorySettingsStore()
        );

        ApplicationConfiguration configuration = manager.loadApplicationConfiguration();

        assertEquals("localhost", configuration.serverHost());
        assertEquals(8765, configuration.serverPort());
        assertTrue(configuration.securityConfiguration().enabled());
        assertFalse(configuration.discordEnabled());
        assertEquals("Watching BurpLinkCord", configuration.discordPresence());
        assertTrue(configuration.auditLoggingEnabled());
    }

    @Test
    void shouldThrowConfigurationExceptionForInvalidYaml() {
        ConfigurationManager manager = new ConfigurationManager(
                () -> new ByteArrayInputStream("server: [broken".getBytes(StandardCharsets.UTF_8)),
                new InMemorySettingsStore()
        );

        assertThrows(ConfigurationException.class, manager::loadApplicationConfiguration);
    }

    @Test
    void shouldExposeDefaultRuntimeSettingsForDiscordControlFields() {
        RuntimeSettings defaults = RuntimeSettings.defaults();

        assertEquals("", defaults.discordGuildId());
        assertEquals("", defaults.discordUpdateChannelId());
        assertTrue(defaults.whitelistedDiscordIds().isEmpty());
        assertTrue(defaults.allowedDiscordChannelIds().isEmpty());
        assertEquals("Balanced", defaults.defaultScanProfile());
        assertEquals("Default", defaults.defaultScanConfiguration());
    }

    private static final class InMemorySettingsStore implements SettingsStore {

        private RuntimeSettings settings = RuntimeSettings.defaults();

        @Override
        public RuntimeSettings load() {
            return settings;
        }

        @Override
        public void save(RuntimeSettings settings) {
            this.settings = settings;
        }

        @Override
        public Optional<String> loadSecret(String key) {
            return Optional.empty();
        }

        @Override
        public void saveSecret(String key, String value) {
        }
    }
}
