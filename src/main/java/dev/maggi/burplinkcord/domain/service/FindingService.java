package dev.maggi.burplinkcord.domain.service;

import dev.maggi.burplinkcord.domain.model.Finding;

import java.util.List;

/**
 * Provides access to collected findings.
 */
public interface FindingService {

    /**
     * Returns findings visible to the foundation layer.
     *
     * @return finding list
     */
    List<Finding> getFindings();
}
