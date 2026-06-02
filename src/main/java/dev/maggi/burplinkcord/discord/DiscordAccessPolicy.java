package dev.maggi.burplinkcord.discord;

import dev.maggi.burplinkcord.config.RuntimeSettings;
import dev.maggi.burplinkcord.domain.service.SettingsService;

import java.util.Objects;

/**
 * Applies Discord allowlist rules for interactive commands.
 */
public class DiscordAccessPolicy {

    private final SettingsService settingsService;

    /**
     * Creates an access policy.
     *
     * @param settingsService settings service dependency
     */
    public DiscordAccessPolicy(SettingsService settingsService) {
        this.settingsService = Objects.requireNonNull(settingsService, "settingsService");
    }

    /**
     * Returns whether a Discord user, guild, and channel are allowed to control the bot.
     *
     * @param userId Discord user identifier
     * @param guildId Discord guild identifier
     * @param channelId Discord channel identifier
     * @return true when access is allowed
     */
    public boolean isAllowed(String userId, String guildId, String channelId) {
        RuntimeSettings settings = settingsService.getSettings();
        boolean userAllowed = !settings.whitelistedDiscordIds().isEmpty()
                && settings.whitelistedDiscordIds().contains(userId);
        boolean guildAllowed = settings.discordGuildId().isBlank()
                || settings.discordGuildId().equals(guildId);
        boolean channelAllowed = settings.allowedDiscordChannelIds().isEmpty()
                || settings.allowedDiscordChannelIds().contains(channelId);
        return userAllowed && guildAllowed && channelAllowed;
    }
}
