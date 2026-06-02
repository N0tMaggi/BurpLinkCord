package dev.maggi.burplinkcord.config;

/**
 * Immutable application bootstrap configuration loaded from {@code application.yaml}.
 *
 * @param serverHost local API host
 * @param serverPort local API port
 * @param securityConfiguration security configuration
 * @param discordEnabled whether future Discord integration is enabled by bootstrap
 * @param discordPresence bootstrap Discord presence text
 * @param auditLoggingEnabled whether audit logging is enabled
 */
public record ApplicationConfiguration(
        String serverHost,
        int serverPort,
        SecurityConfiguration securityConfiguration,
        boolean discordEnabled,
        String discordPresence,
        boolean auditLoggingEnabled
) {
}
