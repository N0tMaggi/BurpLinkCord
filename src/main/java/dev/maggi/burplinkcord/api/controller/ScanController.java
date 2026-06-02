package dev.maggi.burplinkcord.api.controller;

import dev.maggi.burplinkcord.api.request.StartScanRequest;
import dev.maggi.burplinkcord.api.request.StopScanRequest;
import dev.maggi.burplinkcord.api.response.ScanResponse;
import dev.maggi.burplinkcord.domain.model.Scan;
import dev.maggi.burplinkcord.domain.model.ScanStatus;
import dev.maggi.burplinkcord.domain.service.ScanService;
import dev.maggi.burplinkcord.logging.AuditLogger;
import dev.maggi.burplinkcord.security.AccessValidator;

import java.util.List;
import java.util.Objects;

/**
 * Handles scan-related API requests.
 */
public class ScanController {

    private final AccessValidator accessValidator;
    private final ScanService scanService;
    private final AuditLogger auditLogger;

    /**
     * Creates a scan controller.
     *
     * @param accessValidator access validator dependency
     * @param scanService scan service dependency
     * @param auditLogger audit logger dependency
     */
    public ScanController(AccessValidator accessValidator, ScanService scanService, AuditLogger auditLogger) {
        this.accessValidator = Objects.requireNonNull(accessValidator, "accessValidator");
        this.scanService = Objects.requireNonNull(scanService, "scanService");
        this.auditLogger = Objects.requireNonNull(auditLogger, "auditLogger");
    }

    /**
     * Lists known scans.
     *
     * @param authorizationHeader authorization header
     * @return scan response
     */
    public ScanResponse getScans(String authorizationHeader) {
        accessValidator.validate(authorizationHeader, "scan.read");
        List<Scan> scans = scanService.getScans();
        auditLogger.log("scan.read count=" + scans.size());
        return new ScanResponse(null, null, "Loaded scans", scans);
    }

    /**
     * Starts a scan.
     *
     * @param authorizationHeader authorization header
     * @param request start request
     * @return scan response
     */
    public ScanResponse startScan(String authorizationHeader, StartScanRequest request) {
        accessValidator.validate(authorizationHeader, "scan.start");
        Scan scan = scanService.startScan(request);
        return new ScanResponse(scan.id(), scan.status(), "Started scan", scanService.getScans());
    }

    /**
     * Stops a scan.
     *
     * @param authorizationHeader authorization header
     * @param request stop request
     * @return scan response
     */
    public ScanResponse stopScan(String authorizationHeader, StopScanRequest request) {
        accessValidator.validate(authorizationHeader, "scan.stop");
        ScanStatus status = request.deleteAfterStop() ? ScanStatus.DELETED : ScanStatus.STOPPED;
        Scan scan = request.deleteAfterStop()
                ? scanService.deleteScan(request.scanId()).orElseThrow()
                : scanService.stopScan(request.scanId()).orElseThrow();
        return new ScanResponse(scan.id(), status, "Updated scan", scanService.getScans());
    }

    /**
     * Pauses a scan.
     *
     * @param authorizationHeader authorization header
     * @param scanId scan identifier
     * @return scan response
     */
    public ScanResponse pauseScan(String authorizationHeader, String scanId) {
        accessValidator.validate(authorizationHeader, "scan.pause");
        Scan scan = scanService.pauseScan(scanId).orElseThrow();
        return new ScanResponse(scan.id(), scan.status(), "Paused scan", scanService.getScans());
    }

    /**
     * Resumes a scan.
     *
     * @param authorizationHeader authorization header
     * @param scanId scan identifier
     * @return scan response
     */
    public ScanResponse resumeScan(String authorizationHeader, String scanId) {
        accessValidator.validate(authorizationHeader, "scan.resume");
        Scan scan = scanService.resumeScan(scanId).orElseThrow();
        return new ScanResponse(scan.id(), scan.status(), "Resumed scan", scanService.getScans());
    }

    /**
     * Deletes a scan.
     *
     * @param authorizationHeader authorization header
     * @param scanId scan identifier
     * @return scan response
     */
    public ScanResponse deleteScan(String authorizationHeader, String scanId) {
        accessValidator.validate(authorizationHeader, "scan.delete");
        Scan scan = scanService.deleteScan(scanId).orElseThrow();
        return new ScanResponse(scan.id(), scan.status(), "Deleted scan", scanService.getScans());
    }
}
