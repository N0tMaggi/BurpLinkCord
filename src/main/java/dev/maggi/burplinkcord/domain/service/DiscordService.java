package dev.maggi.burplinkcord.domain.service;

import dev.maggi.burplinkcord.discord.DiscordRuntimeStatus;

/**
 * Controls the Discord bot runtime for BurpLinkCord.
 */
public interface DiscordService {

    /**
     * Starts the Discord bot runtime.
     */
    void start();

    /**
     * Stops the Discord bot runtime.
     */
    void stop();

    /**
     * Restarts the Discord bot runtime.
     */
    default void restart() {
        stop();
        start();
    }

    /**
     * Publishes an interactive control panel to the configured Discord channels.
     */
    default void publishControlPanel() {
    }

    /**
     * Returns the current Discord runtime status.
     *
     * @return Discord runtime status
     */
    DiscordRuntimeStatus status();
}
