package dev.maggi.burplinkcord.domain.model;

/**
 * Represents the lifecycle status of a scan.
 */
public enum ScanStatus {
    QUEUED,
    RUNNING,
    PAUSED,
    STOPPED,
    COMPLETED,
    DELETED
}
