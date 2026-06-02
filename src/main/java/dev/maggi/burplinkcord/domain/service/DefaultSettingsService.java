package dev.maggi.burplinkcord.domain.service;

import dev.maggi.burplinkcord.config.ConfigurationManager;
import dev.maggi.burplinkcord.config.RuntimeSettings;

import java.util.Objects;

/**
 * Default runtime settings service backed by the configuration manager.
 */
public class DefaultSettingsService implements SettingsService {

    private static final String DISCORD_BOT_TOKEN_KEY = "discord.botToken";
    private static final String API_SHARED_SECRET_KEY = "api.sharedSecret";

    private final ConfigurationManager configurationManager;

    /**
     * Creates a settings service.
     *
     * @param configurationManager configuration manager dependency
     */
    public DefaultSettingsService(ConfigurationManager configurationManager) {
        this.configurationManager = Objects.requireNonNull(configurationManager, "configurationManager");
    }

    @Override
    public RuntimeSettings getSettings() {
        return configurationManager.loadRuntimeSettings();
    }

    @Override
    public void saveSettings(RuntimeSettings settings) {
        configurationManager.saveRuntimeSettings(settings);
    }

    @Override
    public String loadDiscordBotToken() {
        return configurationManager.loadSecret(DISCORD_BOT_TOKEN_KEY).orElse("");
    }

    @Override
    public void saveDiscordBotToken(String token) {
        configurationManager.saveSecret(DISCORD_BOT_TOKEN_KEY, token);
    }

    @Override
    public String loadApiSharedSecret() {
        return configurationManager.loadSecret(API_SHARED_SECRET_KEY).orElse("");
    }

    @Override
    public void saveApiSharedSecret(String secret) {
        configurationManager.saveSecret(API_SHARED_SECRET_KEY, secret);
    }
}
