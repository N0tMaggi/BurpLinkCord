package dev.maggi.burplinkcord.api.controller;

import dev.maggi.burplinkcord.api.response.HealthResponse;
import dev.maggi.burplinkcord.config.ApplicationConfiguration;
import dev.maggi.burplinkcord.domain.service.DiscordService;
import dev.maggi.burplinkcord.domain.service.StatusService;
import dev.maggi.burplinkcord.domain.service.StatusSnapshot;
import dev.maggi.burplinkcord.logging.AuditLogger;

import java.util.Objects;

/**
 * Handles health-related API requests.
 */
public class HealthController {

    private final ApplicationConfiguration applicationConfiguration;
    private final DiscordService discordService;
    private final StatusService statusService;
    private final AuditLogger auditLogger;

    /**
     * Creates a health controller.
     *
     * @param applicationConfiguration bootstrap configuration
     * @param discordService Discord service dependency
     * @param statusService status service dependency
     * @param auditLogger audit logger dependency
     */
    public HealthController(
            ApplicationConfiguration applicationConfiguration,
            DiscordService discordService,
            StatusService statusService,
            AuditLogger auditLogger
    ) {
        this.applicationConfiguration = Objects.requireNonNull(applicationConfiguration, "applicationConfiguration");
        this.discordService = Objects.requireNonNull(discordService, "discordService");
        this.statusService = Objects.requireNonNull(statusService, "statusService");
        this.auditLogger = Objects.requireNonNull(auditLogger, "auditLogger");
    }

    /**
     * Returns the current health response.
     *
     * @return health response
     */
    public HealthResponse getHealth() {
        StatusSnapshot snapshot = statusService.currentStatus();
        auditLogger.log("health.read");
        return new HealthResponse(
                "BurpLinkCord",
                applicationConfiguration.serverHost(),
                applicationConfiguration.serverPort(),
                snapshot.extensionLoaded(),
                snapshot.apiRunning(),
                applicationConfiguration.discordEnabled(),
                discordService.status().connected(),
                discordService.status().statusMessage(),
                snapshot.activeScanCount(),
                snapshot.message()
        );
    }
}
