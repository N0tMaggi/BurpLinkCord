package dev.maggi.burplinkcord.config;

import burp.api.montoya.persistence.Preferences;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.maggi.burplinkcord.exception.ConfigurationException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Persistence abstraction for user-managed runtime settings.
 */
public interface SettingsStore {

    /**
     * Loads persisted runtime settings.
     *
     * @return stored runtime settings or defaults
     */
    RuntimeSettings load();

    /**
     * Persists runtime settings.
     *
     * @param settings settings to store
     */
    void save(RuntimeSettings settings);

    /**
     * Loads a sensitive value.
     *
     * @param key secret key
     * @return optional secret value
     */
    Optional<String> loadSecret(String key);

    /**
     * Persists a sensitive value.
     *
     * @param key secret key
     * @param value secret value
     */
    void saveSecret(String key, String value);

    /**
     * Creates a Burp-backed settings store.
     *
     * @param preferences Burp preferences API
     * @return settings store implementation
     */
    static SettingsStore burpPreferences(Preferences preferences) {
        return new BurpPreferencesSettingsStore(preferences);
    }
}

final class BurpPreferencesSettingsStore implements SettingsStore {

    private static final String KEY_DISCORD_ENABLED = "burplinkcord.discord.enabled";
    private static final String KEY_AUTOSTART = "burplinkcord.autostart";
    private static final String KEY_DISCORD_GUILD = "burplinkcord.discord.guild";
    private static final String KEY_WHITELIST = "burplinkcord.discord.whitelist";
    private static final String KEY_ALLOWED_CHANNELS = "burplinkcord.discord.channels";
    private static final String KEY_UPDATE_CHANNEL = "burplinkcord.discord.updateChannel";
    private static final String KEY_ALLOWED_DOMAINS = "burplinkcord.allowed.domains";
    private static final String KEY_DEFAULT_SCAN_PROFILE = "burplinkcord.scan.profile";
    private static final String KEY_DEFAULT_SCAN_CONFIGURATION = "burplinkcord.scan.configuration";
    private static final String KEY_DISCORD_TOKEN = "burplinkcord.discord.token";

    private final Preferences preferences;
    private final ObjectMapper objectMapper;

    BurpPreferencesSettingsStore(Preferences preferences) {
        this.preferences = Objects.requireNonNull(preferences, "preferences");
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Override
    public RuntimeSettings load() {
        return new RuntimeSettings(
                Boolean.TRUE.equals(preferences.getBoolean(KEY_DISCORD_ENABLED)),
                Boolean.TRUE.equals(preferences.getBoolean(KEY_AUTOSTART)),
                Optional.ofNullable(preferences.getString(KEY_DISCORD_GUILD)).orElse(""),
                readStringList(KEY_WHITELIST),
                readStringList(KEY_ALLOWED_CHANNELS),
                Optional.ofNullable(preferences.getString(KEY_UPDATE_CHANNEL)).orElse(""),
                readStringList(KEY_ALLOWED_DOMAINS),
                Optional.ofNullable(preferences.getString(KEY_DEFAULT_SCAN_PROFILE)).orElse("Balanced"),
                Optional.ofNullable(preferences.getString(KEY_DEFAULT_SCAN_CONFIGURATION)).orElse("Default")
        );
    }

    @Override
    public void save(RuntimeSettings settings) {
        preferences.setBoolean(KEY_DISCORD_ENABLED, settings.discordIntegrationEnabled());
        preferences.setBoolean(KEY_AUTOSTART, settings.autostartEnabled());
        preferences.setString(KEY_DISCORD_GUILD, settings.discordGuildId());
        preferences.setString(KEY_WHITELIST, writeStringList(settings.whitelistedDiscordIds()));
        preferences.setString(KEY_ALLOWED_CHANNELS, writeStringList(settings.allowedDiscordChannelIds()));
        preferences.setString(KEY_UPDATE_CHANNEL, settings.discordUpdateChannelId());
        preferences.setString(KEY_ALLOWED_DOMAINS, writeStringList(settings.allowedDomains()));
        preferences.setString(KEY_DEFAULT_SCAN_PROFILE, settings.defaultScanProfile());
        preferences.setString(KEY_DEFAULT_SCAN_CONFIGURATION, settings.defaultScanConfiguration());
    }

    @Override
    public Optional<String> loadSecret(String key) {
        return Optional.ofNullable(preferences.getString(resolveSecretKey(key)));
    }

    @Override
    public void saveSecret(String key, String value) {
        preferences.setString(resolveSecretKey(key), value == null ? "" : value);
    }

    private List<String> readStringList(String key) {
        String serialized = preferences.getString(key);
        if (serialized == null || serialized.isBlank()) {
            return List.of();
        }

        try {
            return objectMapper.readValue(
                    serialized,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
            );
        } catch (JsonProcessingException exception) {
            throw new ConfigurationException("Unable to deserialize settings list for key " + key, exception);
        }
    }

    private String writeStringList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException exception) {
            throw new ConfigurationException("Unable to serialize runtime settings.", exception);
        }
    }

    private String resolveSecretKey(String key) {
        return switch (key) {
            case "discord.botToken" -> KEY_DISCORD_TOKEN;
            default -> "burplinkcord.secret." + key;
        };
    }
}
