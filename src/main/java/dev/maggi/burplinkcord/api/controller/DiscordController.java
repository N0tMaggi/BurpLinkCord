package dev.maggi.burplinkcord.api.controller;

import dev.maggi.burplinkcord.api.response.DiscordResponse;
import dev.maggi.burplinkcord.discord.DiscordRuntimeStatus;
import dev.maggi.burplinkcord.domain.service.DiscordService;
import dev.maggi.burplinkcord.logging.AuditLogger;
import dev.maggi.burplinkcord.security.AccessValidator;

import java.util.Objects;

/**
 * Handles Discord runtime API requests.
 */
public class DiscordController {

    private final AccessValidator accessValidator;
    private final DiscordService discordService;
    private final AuditLogger auditLogger;

    /**
     * Creates a Discord controller.
     *
     * @param accessValidator access validator dependency
     * @param discordService Discord service dependency
     * @param auditLogger audit logger dependency
     */
    public DiscordController(AccessValidator accessValidator, DiscordService discordService, AuditLogger auditLogger) {
        this.accessValidator = Objects.requireNonNull(accessValidator, "accessValidator");
        this.discordService = Objects.requireNonNull(discordService, "discordService");
        this.auditLogger = Objects.requireNonNull(auditLogger, "auditLogger");
    }

    /**
     * Returns the current Discord runtime status.
     *
     * @param authorizationHeader authorization header
     * @return Discord runtime response
     */
    public DiscordResponse getStatus(String authorizationHeader) {
        accessValidator.validate(authorizationHeader, "discord.read");
        auditLogger.log("discord.read");
        return map(discordService.status());
    }

    /**
     * Starts the Discord runtime.
     *
     * @param authorizationHeader authorization header
     * @return Discord runtime response
     */
    public DiscordResponse start(String authorizationHeader) {
        accessValidator.validate(authorizationHeader, "discord.start");
        discordService.start();
        auditLogger.log("discord.start.api");
        return map(discordService.status());
    }

    /**
     * Stops the Discord runtime.
     *
     * @param authorizationHeader authorization header
     * @return Discord runtime response
     */
    public DiscordResponse stop(String authorizationHeader) {
        accessValidator.validate(authorizationHeader, "discord.stop");
        discordService.stop();
        auditLogger.log("discord.stop.api");
        return map(discordService.status());
    }

    /**
     * Publishes an interactive Discord control panel to the configured channels.
     *
     * @param authorizationHeader authorization header
     * @return Discord runtime response
     */
    public DiscordResponse publishControlPanel(String authorizationHeader) {
        accessValidator.validate(authorizationHeader, "discord.publish.panel");
        discordService.publishControlPanel();
        auditLogger.log("discord.panel.publish.api");
        return map(discordService.status());
    }

    private DiscordResponse map(DiscordRuntimeStatus status) {
        return new DiscordResponse(
                status.configured(),
                status.enabled(),
                status.connected(),
                status.botTag(),
                status.statusMessage()
        );
    }
}
