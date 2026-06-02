package dev.maggi.burplinkcord.api.response;

import dev.maggi.burplinkcord.domain.model.Scan;
import dev.maggi.burplinkcord.domain.model.ScanStatus;

import java.util.List;

/**
 * Response payload for scan operations.
 *
 * @param scanId affected scan identifier
 * @param status resulting scan status
 * @param message response message
 * @param scans current scan projection
 */
public record ScanResponse(
        String scanId,
        ScanStatus status,
        String message,
        List<Scan> scans
) {
}
