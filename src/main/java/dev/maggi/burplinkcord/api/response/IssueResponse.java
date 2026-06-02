package dev.maggi.burplinkcord.api.response;

import dev.maggi.burplinkcord.domain.model.Finding;

import java.util.List;

/**
 * Response payload for finding projections.
 *
 * @param findings exposed finding list
 */
public record IssueResponse(List<Finding> findings) {
}
