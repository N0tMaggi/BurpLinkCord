package dev.maggi.burplinkcord.domain.service;

import java.util.List;

/**
 * Provides current runtime status data.
 */
public interface StatusService {

    /**
     * Returns the current status snapshot.
     *
     * @return current status
     */
    StatusSnapshot currentStatus();

    /**
     * Returns recent activity messages.
     *
     * @return recent activity list
     */
    default List<String> recentActivity() {
        return List.of();
    }
}
