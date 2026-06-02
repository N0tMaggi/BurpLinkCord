package dev.maggi.burplinkcord.domain.service;

/**
 * Immutable runtime status projection for UI and API consumers.
 *
 * @param extensionLoaded whether the extension lifecycle is active
 * @param apiRunning whether the local API server is running
 * @param discordConnected whether the future Discord adapter is connected
 * @param activeScanCount number of active scans
 * @param message user-facing status message
 */
public record StatusSnapshot(
        boolean extensionLoaded,
        boolean apiRunning,
        boolean discordConnected,
        int activeScanCount,
        String message
) {
}
