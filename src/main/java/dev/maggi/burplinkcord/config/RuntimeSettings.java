package dev.maggi.burplinkcord.config;

import java.util.List;

/**
 * Mutable user-managed runtime settings stored through Burp preferences.
 *
 * @param discordIntegrationEnabled whether Discord integration should start when available
 * @param autostartEnabled whether background features should autostart with the extension
 * @param discordGuildId whitelisted Discord guild identifier
 * @param whitelistedDiscordIds allowed Discord user identifiers
 * @param allowedDiscordChannelIds allowed Discord channel identifiers
 * @param discordUpdateChannelId dedicated Discord channel identifier for runtime updates and logs
 * @param allowedDomains allowed target domains
 * @param defaultScanProfile selected default scan profile
 * @param defaultScanConfiguration selected default scan configuration
 */
public record RuntimeSettings(
        boolean discordIntegrationEnabled,
        boolean autostartEnabled,
        String discordGuildId,
        List<String> whitelistedDiscordIds,
        List<String> allowedDiscordChannelIds,
        String discordUpdateChannelId,
        List<String> allowedDomains,
        String defaultScanProfile,
        String defaultScanConfiguration
) {

    /**
     * Creates default runtime settings for a fresh installation.
     *
     * @return default settings instance
     */
    public static RuntimeSettings defaults() {
        return new RuntimeSettings(
                false,
                false,
                "",
                List.of(),
                List.of(),
                "",
                List.of(),
                "Balanced",
                "Default"
        );
    }
}
