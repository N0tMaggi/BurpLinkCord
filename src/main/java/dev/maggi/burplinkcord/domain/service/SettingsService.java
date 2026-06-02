package dev.maggi.burplinkcord.domain.service;

import dev.maggi.burplinkcord.config.RuntimeSettings;

/**
 * Coordinates runtime settings retrieval and persistence.
 */
public interface SettingsService {

    /**
     * Loads runtime settings.
     *
     * @return settings
     */
    RuntimeSettings getSettings();

    /**
     * Persists runtime settings.
     *
     * @param settings settings to store
     */
    void saveSettings(RuntimeSettings settings);

    /**
     * Loads the Discord bot token value.
     *
     * @return bot token or empty string
     */
    String loadDiscordBotToken();

    /**
     * Saves the Discord bot token value.
     *
     * @param token token value
     */
    void saveDiscordBotToken(String token);

    /**
     * Loads the local API shared secret.
     *
     * @return shared secret or empty string
     */
    String loadApiSharedSecret();

    /**
     * Saves the local API shared secret.
     *
     * @param secret shared secret value
     */
    void saveApiSharedSecret(String secret);
}
