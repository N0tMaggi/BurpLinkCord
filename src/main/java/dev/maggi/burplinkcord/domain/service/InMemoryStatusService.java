package dev.maggi.burplinkcord.domain.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * In-memory status service used by the active runtime.
 */
public class InMemoryStatusService implements StatusService {

    private final List<String> activity = new ArrayList<>();
    private boolean extensionLoaded;
    private boolean apiRunning;
    private boolean discordConnected;
    private int activeScanCount;
    private String message = "Initializing";

    /**
     * Updates extension loaded state.
     *
     * @param extensionLoaded loaded flag
     */
    public synchronized void markExtensionLoaded(boolean extensionLoaded) {
        this.extensionLoaded = extensionLoaded;
    }

    /**
     * Updates API server state.
     *
     * @param apiRunning running flag
     */
    public synchronized void markApiRunning(boolean apiRunning) {
        this.apiRunning = apiRunning;
    }

    /**
     * Updates future Discord connectivity state.
     *
     * @param discordConnected connection flag
     */
    public synchronized void markDiscordConnected(boolean discordConnected) {
        this.discordConnected = discordConnected;
    }

    /**
     * Updates the active scan count.
     *
     * @param activeScanCount scan count
     */
    public synchronized void setActiveScanCount(int activeScanCount) {
        this.activeScanCount = activeScanCount;
    }

    /**
     * Updates the current status message.
     *
     * @param message status message
     */
    public synchronized void setMessage(String message) {
        this.message = message;
    }

    /**
     * Records a recent activity entry.
     *
     * @param entry activity text
     */
    public synchronized void recordActivity(String entry) {
        activity.add(0, entry);
        if (activity.size() > 25) {
            activity.remove(activity.size() - 1);
        }
    }

    @Override
    public synchronized StatusSnapshot currentStatus() {
        return new StatusSnapshot(extensionLoaded, apiRunning, discordConnected, activeScanCount, message);
    }

    @Override
    public synchronized List<String> recentActivity() {
        return Collections.unmodifiableList(new ArrayList<>(activity));
    }
}
