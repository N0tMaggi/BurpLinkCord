package dev.maggi.burplinkcord.events;

/**
 * Represents a runtime event that can be surfaced to logs, UI, or Discord.
 *
 * @param title short event title
 * @param message user-facing event description
 * @param severity runtime severity level
 */
public record RuntimeEvent(
        String title,
        String message,
        String severity
) {
}
