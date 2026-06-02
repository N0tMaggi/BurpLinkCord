package dev.maggi.burplinkcord.domain.model;

/**
 * Represents a finding surfaced by BurpLinkCord.
 *
 * @param id finding identifier
 * @param title finding title
 * @param severity severity level
 * @param target associated target
 * @param description finding description
 */
public record Finding(
        String id,
        String title,
        Severity severity,
        String target,
        String description
) {
}
