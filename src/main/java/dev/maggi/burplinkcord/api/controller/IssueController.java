package dev.maggi.burplinkcord.api.controller;

import dev.maggi.burplinkcord.api.response.IssueResponse;
import dev.maggi.burplinkcord.domain.service.FindingService;
import dev.maggi.burplinkcord.logging.AuditLogger;
import dev.maggi.burplinkcord.security.AccessValidator;

import java.util.Objects;

/**
 * Handles finding-related API requests.
 */
public class IssueController {

    private final AccessValidator accessValidator;
    private final FindingService findingService;
    private final AuditLogger auditLogger;

    /**
     * Creates an issue controller.
     *
     * @param accessValidator access validator dependency
     * @param findingService finding service dependency
     * @param auditLogger audit logger dependency
     */
    public IssueController(AccessValidator accessValidator, FindingService findingService, AuditLogger auditLogger) {
        this.accessValidator = Objects.requireNonNull(accessValidator, "accessValidator");
        this.findingService = Objects.requireNonNull(findingService, "findingService");
        this.auditLogger = Objects.requireNonNull(auditLogger, "auditLogger");
    }

    /**
     * Returns available finding projections.
     *
     * @param authorizationHeader authorization header
     * @return issue response
     */
    public IssueResponse getIssues(String authorizationHeader) {
        accessValidator.validate(authorizationHeader, "finding.read");
        auditLogger.log("finding.read");
        return new IssueResponse(findingService.getFindings());
    }
}
