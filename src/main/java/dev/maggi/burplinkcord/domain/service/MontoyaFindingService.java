package dev.maggi.burplinkcord.domain.service;

import burp.api.montoya.scanner.audit.AuditIssueHandler;
import burp.api.montoya.scanner.audit.issues.AuditIssue;
import burp.api.montoya.scanner.audit.issues.AuditIssueSeverity;
import dev.maggi.burplinkcord.domain.model.Finding;
import dev.maggi.burplinkcord.domain.model.Severity;
import dev.maggi.burplinkcord.events.EventBus;
import dev.maggi.burplinkcord.logging.AuditLogger;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Collects findings reported by Burp Scanner through the Montoya API.
 */
public class MontoyaFindingService implements FindingService, AuditIssueHandler {

    private final Map<String, Finding> findings = new ConcurrentHashMap<>();
    private final EventBus eventBus;
    private final AuditLogger auditLogger;

    /**
     * Creates a finding service.
     *
     * @param eventBus event bus dependency
     * @param auditLogger audit logger dependency
     */
    public MontoyaFindingService(EventBus eventBus, AuditLogger auditLogger) {
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
        this.auditLogger = Objects.requireNonNull(auditLogger, "auditLogger");
    }

    @Override
    public List<Finding> getFindings() {
        return findings.values().stream()
                .sorted(Comparator.comparing(Finding::severity).reversed().thenComparing(Finding::title))
                .toList();
    }

    @Override
    public void handleNewAuditIssue(AuditIssue auditIssue) {
        Finding finding = new Finding(
                auditIssue.name() + "@" + auditIssue.baseUrl(),
                auditIssue.name(),
                mapSeverity(auditIssue.severity()),
                auditIssue.baseUrl(),
                auditIssue.detail() == null ? "" : auditIssue.detail()
        );
        findings.put(finding.id(), finding);
        auditLogger.log("finding.discovered target=" + finding.target() + " title=" + finding.title());
        eventBus.publish("finding.discovered", finding);
    }

    private Severity mapSeverity(AuditIssueSeverity severity) {
        return switch (severity) {
            case HIGH -> Severity.HIGH;
            case MEDIUM -> Severity.MEDIUM;
            case LOW -> Severity.LOW;
            case INFORMATION, FALSE_POSITIVE -> Severity.INFORMATION;
        };
    }
}
