package dev.maggi.burplinkcord.api.response;

/**
 * Response payload describing the extension health state.
 *
 * @param applicationName application display name
 * @param host configured API host
 * @param port configured API port
 * @param extensionLoaded whether the extension is active
 * @param apiRunning whether the API server is running
 * @param discordConfigured whether Discord bootstrap is enabled
 * @param discordConnected whether the Discord bot is connected
 * @param discordStatus current Discord runtime status
 * @param activeScanCount number of active scans
 * @param message user-facing status message
 */
public record HealthResponse(
        String applicationName,
        String host,
        int port,
        boolean extensionLoaded,
        boolean apiRunning,
        boolean discordConfigured,
        boolean discordConnected,
        String discordStatus,
        int activeScanCount,
        String message
) {
}
