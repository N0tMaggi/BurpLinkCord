package dev.maggi.burplinkcord.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import dev.maggi.burplinkcord.exception.ConfigurationException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Loads bootstrap configuration and delegates persistence of mutable runtime settings.
 */
public class ConfigurationManager {

    private final Supplier<InputStream> configurationStreamSupplier;
    private final SettingsStore settingsStore;
    private final ObjectMapper yamlMapper;

    /**
     * Creates a configuration manager using the default application resource.
     *
     * @param settingsStore runtime settings persistence
     */
    public ConfigurationManager(SettingsStore settingsStore) {
        this(() -> ConfigurationManager.class.getResourceAsStream("/application.yaml"), settingsStore);
    }

    /**
     * Creates a configuration manager with explicit dependencies.
     *
     * @param configurationStreamSupplier resource stream supplier
     * @param settingsStore runtime settings persistence
     */
    public ConfigurationManager(Supplier<InputStream> configurationStreamSupplier, SettingsStore settingsStore) {
        this.configurationStreamSupplier = Objects.requireNonNull(configurationStreamSupplier, "configurationStreamSupplier");
        this.settingsStore = Objects.requireNonNull(settingsStore, "settingsStore");
        this.yamlMapper = new ObjectMapper(new YAMLFactory()).findAndRegisterModules();
    }

    /**
     * Loads the immutable application bootstrap configuration.
     *
     * @return application configuration
     */
    public ApplicationConfiguration loadApplicationConfiguration() {
        try (InputStream stream = configurationStreamSupplier.get()) {
            if (stream == null) {
                throw new ConfigurationException("application.yaml could not be found on the classpath.");
            }

            JsonNode root = yamlMapper.readTree(stream);
            String host = requiredText(root, "server", "host");
            int port = requiredInt(root, "server", "port");
            boolean securityEnabled = requiredBoolean(root, "security", "enabled");
            boolean discordEnabled = requiredBoolean(root, "discord", "enabled");
            String discordPresence = optionalText(root, "discord", "presence").orElse("Watching BurpLinkCord");
            boolean auditEnabled = requiredBoolean(root, "logging", "audit");

            return new ApplicationConfiguration(
                    host,
                    port,
                    new SecurityConfiguration(securityEnabled),
                    discordEnabled,
                    discordPresence,
                    auditEnabled
            );
        } catch (IOException exception) {
            throw new ConfigurationException("Unable to read application.yaml.", exception);
        }
    }

    /**
     * Loads persisted runtime settings.
     *
     * @return runtime settings
     */
    public RuntimeSettings loadRuntimeSettings() {
        return settingsStore.load();
    }

    /**
     * Saves runtime settings.
     *
     * @param settings settings to persist
     */
    public void saveRuntimeSettings(RuntimeSettings settings) {
        settingsStore.save(settings);
    }

    /**
     * Loads a sensitive setting value.
     *
     * @param key secret key
     * @return optional secret value
     */
    public Optional<String> loadSecret(String key) {
        return settingsStore.loadSecret(key);
    }

    /**
     * Saves a sensitive setting value.
     *
     * @param key secret key
     * @param value secret value
     */
    public void saveSecret(String key, String value) {
        settingsStore.saveSecret(key, value);
    }

    private String requiredText(JsonNode root, String parent, String child) {
        JsonNode value = root.path(parent).path(child);
        if (!value.isTextual() || value.asText().isBlank()) {
            throw new ConfigurationException("Missing text configuration at " + parent + "." + child);
        }
        return value.asText();
    }

    private int requiredInt(JsonNode root, String parent, String child) {
        JsonNode value = root.path(parent).path(child);
        if (!value.isInt()) {
            throw new ConfigurationException("Missing integer configuration at " + parent + "." + child);
        }
        return value.asInt();
    }

    private boolean requiredBoolean(JsonNode root, String parent, String child) {
        JsonNode value = root.path(parent).path(child);
        if (!value.isBoolean()) {
            throw new ConfigurationException("Missing boolean configuration at " + parent + "." + child);
        }
        return value.asBoolean();
    }

    private Optional<String> optionalText(JsonNode root, String parent, String child) {
        JsonNode value = root.path(parent).path(child);
        if (value.isTextual() && !value.asText().isBlank()) {
            return Optional.of(value.asText());
        }
        return Optional.empty();
    }
}
