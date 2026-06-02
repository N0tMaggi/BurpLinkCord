package dev.maggi.burplinkcord.api.response;

/**
 * Response payload for Discord runtime operations.
 *
 * @param configured whether a bot token is configured
 * @param enabled whether Discord integration is enabled in runtime settings
 * @param connected whether the bot is connected
 * @param botTag connected bot tag
 * @param message runtime status message
 */
public record DiscordResponse(
        boolean configured,
        boolean enabled,
        boolean connected,
        String botTag,
        String message
) {
}
