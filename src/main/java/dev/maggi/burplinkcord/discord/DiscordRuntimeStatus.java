package dev.maggi.burplinkcord.discord;

/**
 * Represents the current Discord runtime state.
 *
 * @param configured whether a token is configured
 * @param enabled whether Discord integration is enabled in runtime settings
 * @param connected whether the bot is connected
 * @param botTag connected bot tag
 * @param statusMessage current status message
 */
public record DiscordRuntimeStatus(
        boolean configured,
        boolean enabled,
        boolean connected,
        String botTag,
        String statusMessage
) {

    /**
     * Creates a disconnected default status.
     *
     * @return default runtime status
     */
    public static DiscordRuntimeStatus disconnected() {
        return new DiscordRuntimeStatus(false, false, false, "", "Discord integration inactive");
    }
}
