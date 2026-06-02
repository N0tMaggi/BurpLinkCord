package dev.maggi.burplinkcord.domain.model;

import java.time.Instant;

/**
 * Represents a scan entity exposed to the API and UI layers.
 *
 * @param id scan identifier
 * @param target target descriptor
 * @param status scan status
 * @param profileName selected profile
 * @param configurationName selected configuration
 * @param createdAt creation timestamp
 * @param updatedAt last update timestamp
 */
public record Scan(
        String id,
        String target,
        ScanStatus status,
        String profileName,
        String configurationName,
        Instant createdAt,
        Instant updatedAt
) {
}
