package dev.maggi.burplinkcord;

import dev.maggi.burplinkcord.api.controller.HealthController;
import dev.maggi.burplinkcord.api.response.HealthResponse;
import dev.maggi.burplinkcord.config.ApplicationConfiguration;
import dev.maggi.burplinkcord.config.SecurityConfiguration;
import dev.maggi.burplinkcord.discord.DiscordRuntimeStatus;
import dev.maggi.burplinkcord.domain.service.DiscordService;
import dev.maggi.burplinkcord.domain.service.StatusService;
import dev.maggi.burplinkcord.domain.service.StatusSnapshot;
import dev.maggi.burplinkcord.logging.AuditLogger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HealthControllerTest {

    @Test
    void shouldReturnHealthPayloadFromInjectedCollaborators() {
        StatusService statusService = () -> new StatusSnapshot(true, true, false, 2, "Ready");
        DiscordService discordService = new DiscordService() {
            @Override
            public void start() {
            }

            @Override
            public void stop() {
            }

            @Override
            public DiscordRuntimeStatus status() {
                return new DiscordRuntimeStatus(true, true, true, "bot#0001", "Connected");
            }
        };
        AuditLogger auditLogger = action -> { };
        HealthController controller = new HealthController(
                new ApplicationConfiguration("localhost", 8765, new SecurityConfiguration(true), false, "Watching BurpLinkCord", true),
                discordService,
                statusService,
                auditLogger
        );

        HealthResponse response = controller.getHealth();

        assertEquals("BurpLinkCord", response.applicationName());
        assertEquals("localhost", response.host());
        assertEquals(8765, response.port());
        assertTrue(response.apiRunning());
        assertTrue(response.extensionLoaded());
        assertTrue(response.discordConnected());
        assertEquals("Connected", response.discordStatus());
        assertEquals("Ready", response.message());
    }
}
