package dev.maggi.burplinkcord.domain.service;

import dev.maggi.burplinkcord.api.request.StartScanRequest;
import dev.maggi.burplinkcord.domain.model.Scan;

import java.util.List;
import java.util.Optional;

/**
 * Provides scan lifecycle operations.
 */
public interface ScanService {

    /**
     * Lists known scans.
     *
     * @return scan list
     */
    List<Scan> getScans();

    /**
     * Starts a scan.
     *
     * @param request start request
     * @return created scan
     */
    Scan startScan(StartScanRequest request);

    /**
     * Stops a scan.
     *
     * @param scanId scan identifier
     * @return updated scan
     */
    Optional<Scan> stopScan(String scanId);

    /**
     * Pauses a scan.
     *
     * @param scanId scan identifier
     * @return updated scan
     */
    Optional<Scan> pauseScan(String scanId);

    /**
     * Resumes a scan.
     *
     * @param scanId scan identifier
     * @return updated scan
     */
    Optional<Scan> resumeScan(String scanId);

    /**
     * Deletes a scan.
     *
     * @param scanId scan identifier
     * @return removed scan
     */
    Optional<Scan> deleteScan(String scanId);
}
