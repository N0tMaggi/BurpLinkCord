package dev.maggi.burplinkcord.api.request;

/**
 * Request payload for stopping a scan.
 *
 * @param scanId scan identifier
 * @param deleteAfterStop whether the scan should be deleted after stopping
 */
public record StopScanRequest(String scanId, boolean deleteAfterStop) {
}
