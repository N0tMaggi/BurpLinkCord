package dev.maggi.burplinkcord.domain.service;

import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.scanner.AuditConfiguration;
import burp.api.montoya.scanner.BuiltInAuditConfiguration;
import burp.api.montoya.scanner.Crawl;
import burp.api.montoya.scanner.CrawlConfiguration;
import burp.api.montoya.scanner.ScanTask;
import burp.api.montoya.scanner.Scanner;
import burp.api.montoya.scanner.audit.Audit;
import dev.maggi.burplinkcord.api.request.StartScanRequest;
import dev.maggi.burplinkcord.domain.model.Scan;
import dev.maggi.burplinkcord.domain.model.ScanStatus;
import dev.maggi.burplinkcord.events.EventBus;
import dev.maggi.burplinkcord.exception.BurpLinkCordException;
import dev.maggi.burplinkcord.logging.AuditLogger;

import java.net.URI;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Scan service backed by Burp Scanner tasks.
 */
public class MontoyaScanService implements ScanService, AutoCloseable {

    private final Scanner scanner;
    private final EventBus eventBus;
    private final AuditLogger auditLogger;
    private final InMemoryStatusService statusService;
    private final Map<String, ManagedScan> scans = new ConcurrentHashMap<>();
    private final ScheduledExecutorService monitorExecutor;

    /**
     * Creates a scan service.
     *
     * @param scanner Burp scanner API
     * @param eventBus event bus dependency
     * @param auditLogger audit logger dependency
     * @param statusService status service dependency
     */
    public MontoyaScanService(Scanner scanner, EventBus eventBus, AuditLogger auditLogger, InMemoryStatusService statusService) {
        this.scanner = Objects.requireNonNull(scanner, "scanner");
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
        this.auditLogger = Objects.requireNonNull(auditLogger, "auditLogger");
        this.statusService = Objects.requireNonNull(statusService, "statusService");
        this.monitorExecutor = Executors.newSingleThreadScheduledExecutor(new ScanMonitorThreadFactory());
        this.monitorExecutor.scheduleAtFixedRate(this::monitorRunningScans, 2, 2, TimeUnit.SECONDS);
    }

    @Override
    public List<Scan> getScans() {
        return scans.values().stream()
                .map(this::toScan)
                .sorted(Comparator.comparing(Scan::createdAt).reversed())
                .toList();
    }

    @Override
    public Scan startScan(StartScanRequest request) {
        validateRequest(request);
        Instant now = Instant.now();
        String id = UUID.randomUUID().toString();
        ScanExecution execution = launchExecution(request);

        ManagedScan managedScan = new ManagedScan(
                id,
                request.target(),
                request.profileName(),
                request.configurationName(),
                request,
                now,
                now,
                ScanStatus.RUNNING,
                execution.crawl(),
                execution.audit()
        );
        scans.put(id, managedScan);
        refreshRuntimeStatus("Started scan for " + request.target());
        Scan scan = toScan(managedScan);
        eventBus.publish("scan.started", scan);
        auditLogger.log("scan.start target=" + request.target() + " scanId=" + id);
        return scan;
    }

    @Override
    public Optional<Scan> stopScan(String scanId) {
        return transition(scanId, ScanStatus.STOPPED, true);
    }

    @Override
    public Optional<Scan> pauseScan(String scanId) {
        ManagedScan existing = scans.get(scanId);
        if (existing == null) {
            return Optional.empty();
        }

        existing.deleteTasks();
        ManagedScan paused = existing.withExecution(ScanStatus.PAUSED, Instant.now(), null, null);
        scans.put(scanId, paused);
        refreshRuntimeStatus("Paused scan " + paused.id);
        Scan scan = toScan(paused);
        eventBus.publish("scan.status.changed", scan);
        auditLogger.log("scan.pause scanId=" + paused.id + " strategy=task-restart");
        return Optional.of(scan);
    }

    @Override
    public Optional<Scan> resumeScan(String scanId) {
        ManagedScan existing = scans.get(scanId);
        if (existing == null || (existing.status != ScanStatus.PAUSED && existing.status != ScanStatus.STOPPED)) {
            return Optional.empty();
        }

        ScanExecution execution = launchExecution(existing.startScanRequest);
        ManagedScan resumed = existing.withExecution(ScanStatus.RUNNING, Instant.now(), execution.crawl(), execution.audit());
        scans.put(scanId, resumed);
        refreshRuntimeStatus("Resumed scan " + resumed.id);
        Scan scan = toScan(resumed);
        eventBus.publish("scan.status.changed", scan);
        auditLogger.log("scan.resume scanId=" + resumed.id + " strategy=task-restart");
        return Optional.of(scan);
    }

    @Override
    public Optional<Scan> deleteScan(String scanId) {
        return transition(scanId, ScanStatus.DELETED, true);
    }

    @Override
    public void close() {
        monitorExecutor.shutdownNow();
    }

    private Optional<Scan> transition(String scanId, ScanStatus status, boolean terminateTasks) {
        ManagedScan existing = scans.get(scanId);
        if (existing == null) {
            return Optional.empty();
        }

        if (terminateTasks) {
            existing.deleteTasks();
        }

        ManagedScan updated = existing.withExecution(status, Instant.now(), null, null);
        scans.put(scanId, updated);
        refreshRuntimeStatus(status.name() + " scan " + updated.id);
        Scan scan = toScan(updated);
        eventBus.publish("scan.status.changed", scan);
        auditLogger.log("scan.status scanId=" + updated.id + " status=" + status);
        return Optional.of(scan);
    }

    private Scan toScan(ManagedScan managedScan) {
        ScanStatus resolvedStatus = resolveStatus(managedScan);
        return new Scan(
                managedScan.id,
                managedScan.target,
                resolvedStatus,
                managedScan.profileName,
                managedScan.configurationName,
                managedScan.createdAt,
                managedScan.updatedAt
        );
    }

    private ScanStatus resolveStatus(ManagedScan managedScan) {
        if (managedScan.status != ScanStatus.RUNNING) {
            return managedScan.status;
        }

        String statusMessage = Stream.of(managedScan.crawl, managedScan.audit)
                .filter(Objects::nonNull)
                .map(ScanTask::statusMessage)
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .findFirst()
                .orElse("");

        if (statusMessage.contains("complete") || statusMessage.contains("finished")) {
            return ScanStatus.COMPLETED;
        }
        return ScanStatus.RUNNING;
    }

    private void monitorRunningScans() {
        boolean changed = false;
        for (Map.Entry<String, ManagedScan> entry : scans.entrySet()) {
            ManagedScan managedScan = entry.getValue();
            if (managedScan.status != ScanStatus.RUNNING) {
                continue;
            }

            ScanStatus resolvedStatus = resolveStatus(managedScan);
            if (resolvedStatus != ScanStatus.RUNNING) {
                ManagedScan updated = managedScan.withExecution(resolvedStatus, Instant.now(), managedScan.crawl, managedScan.audit);
                scans.put(entry.getKey(), updated);
                Scan scan = toScan(updated);
                eventBus.publish("scan.status.changed", scan);
                auditLogger.log("scan.status scanId=" + updated.id + " status=" + resolvedStatus);
                statusService.recordActivity("Scan " + updated.id + " changed to " + resolvedStatus.name());
                changed = true;
            }
        }

        if (changed) {
            refreshRuntimeStatus("Scan state updated");
        }
    }

    private void refreshRuntimeStatus(String message) {
        long activeScans = scans.values().stream()
                .map(this::resolveStatus)
                .filter(status -> status == ScanStatus.RUNNING)
                .count();
        statusService.setActiveScanCount((int) activeScans);
        statusService.setMessage(message);
        statusService.recordActivity(message);
    }

    private void validateRequest(StartScanRequest request) {
        if (request.target() == null || request.target().isBlank()) {
            throw new BurpLinkCordException("A target URL is required to start a scan.");
        }
        if (!request.crawl() && !request.audit()) {
            throw new BurpLinkCordException("At least crawl or audit must be enabled.");
        }
        if (!request.includedDomains().isEmpty()) {
            String host = URI.create(request.target()).getHost();
            boolean allowed = request.includedDomains().stream()
                    .anyMatch(domain -> domain.equalsIgnoreCase(host) || host.endsWith("." + domain));
            if (!allowed) {
                throw new BurpLinkCordException("Target host is not included in the allowed domains list.");
            }
        }
    }

    private ScanExecution launchExecution(StartScanRequest request) {
        Crawl crawl = request.crawl() ? scanner.startCrawl(CrawlConfiguration.crawlConfiguration(request.target())) : null;
        Audit audit = request.audit()
                ? scanner.startAudit(AuditConfiguration.auditConfiguration(BuiltInAuditConfiguration.LEGACY_ACTIVE_AUDIT_CHECKS))
                : null;

        if (audit != null) {
            audit.addRequest(HttpRequest.httpRequestFromUrl(request.target()));
        }

        return new ScanExecution(crawl, audit);
    }

    private record ScanExecution(Crawl crawl, Audit audit) {
    }

    private static final class ScanMonitorThreadFactory implements ThreadFactory {

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "burplinkcord-scan-monitor");
            thread.setDaemon(true);
            return thread;
        }
    }

    private static final class ManagedScan {

        private final String id;
        private final String target;
        private final String profileName;
        private final String configurationName;
        private final StartScanRequest startScanRequest;
        private final Instant createdAt;
        private final Instant updatedAt;
        private final ScanStatus status;
        private final Crawl crawl;
        private final Audit audit;

        private ManagedScan(
                String id,
                String target,
                String profileName,
                String configurationName,
                StartScanRequest startScanRequest,
                Instant createdAt,
                Instant updatedAt,
                ScanStatus status,
                Crawl crawl,
                Audit audit
        ) {
            this.id = id;
            this.target = target;
            this.profileName = profileName;
            this.configurationName = configurationName;
            this.startScanRequest = startScanRequest;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
            this.status = status;
            this.crawl = crawl;
            this.audit = audit;
        }

        private ManagedScan withExecution(ScanStatus status, Instant updatedAt, Crawl crawl, Audit audit) {
            return new ManagedScan(id, target, profileName, configurationName, startScanRequest, createdAt, updatedAt, status, crawl, audit);
        }

        private void deleteTasks() {
            if (crawl != null) {
                crawl.delete();
            }
            if (audit != null) {
                audit.delete();
            }
        }
    }
}
