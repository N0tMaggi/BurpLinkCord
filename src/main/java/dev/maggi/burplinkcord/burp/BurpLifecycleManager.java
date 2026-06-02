package dev.maggi.burplinkcord.burp;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.Registration;
import dev.maggi.burplinkcord.api.ApiServer;
import dev.maggi.burplinkcord.domain.service.DiscordService;
import dev.maggi.burplinkcord.domain.service.InMemoryStatusService;
import dev.maggi.burplinkcord.domain.service.MontoyaScanService;
import dev.maggi.burplinkcord.domain.service.SettingsService;
import dev.maggi.burplinkcord.events.EventBus;
import dev.maggi.burplinkcord.events.RuntimeEvent;
import dev.maggi.burplinkcord.logging.AuditLogger;
import dev.maggi.burplinkcord.ui.BurpLinkCordTab;

import java.util.Objects;

/**
 * Coordinates startup and shutdown of the BurpLinkCord runtime.
 */
public class BurpLifecycleManager {

    private final MontoyaApi api;
    private final ApiServer apiServer;
    private final BurpLinkCordTab burpLinkCordTab;
    private final BurpEventPublisher burpEventPublisher;
    private final InMemoryStatusService statusService;
    private final SettingsService settingsService;
    private final MontoyaScanService scanService;
    private final DiscordService discordService;
    private final EventBus eventBus;
    private final AuditLogger auditLogger;
    private Registration suiteTabRegistration;
    private Registration unloadingRegistration;

    /**
     * Creates a lifecycle manager.
     *
     * @param api Montoya API instance
     * @param apiServer local API server
     * @param burpLinkCordTab main UI tab
     * @param burpEventPublisher Burp event bridge
     * @param statusService status service
     * @param settingsService settings service
     * @param scanService scan service
     * @param discordService Discord service
     * @param eventBus event bus
     * @param auditLogger audit logger
     */
    public BurpLifecycleManager(
            MontoyaApi api,
            ApiServer apiServer,
            BurpLinkCordTab burpLinkCordTab,
            BurpEventPublisher burpEventPublisher,
            InMemoryStatusService statusService,
            SettingsService settingsService,
            MontoyaScanService scanService,
            DiscordService discordService,
            EventBus eventBus,
            AuditLogger auditLogger
    ) {
        this.api = Objects.requireNonNull(api, "api");
        this.apiServer = Objects.requireNonNull(apiServer, "apiServer");
        this.burpLinkCordTab = Objects.requireNonNull(burpLinkCordTab, "burpLinkCordTab");
        this.burpEventPublisher = Objects.requireNonNull(burpEventPublisher, "burpEventPublisher");
        this.statusService = Objects.requireNonNull(statusService, "statusService");
        this.settingsService = Objects.requireNonNull(settingsService, "settingsService");
        this.scanService = Objects.requireNonNull(scanService, "scanService");
        this.discordService = Objects.requireNonNull(discordService, "discordService");
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
        this.auditLogger = Objects.requireNonNull(auditLogger, "auditLogger");
    }

    /**
     * Starts the extension runtime.
     */
    public void start() {
        api.extension().setName("BurpLinkCord");
        suiteTabRegistration = api.userInterface().registerSuiteTab("BurpLinkCord", burpLinkCordTab);
        unloadingRegistration = api.extension().registerUnloadingHandler(this::shutdown);
        statusService.markExtensionLoaded(true);
        statusService.markDiscordConnected(false);
        statusService.recordActivity("Extension initialized");
        statusService.setMessage("Extension running");
        eventBus.publish("runtime.notification", new RuntimeEvent(
                "Extension Started",
                "BurpLinkCord extension started inside Burp Suite.",
                "INFO"
        ));
        burpEventPublisher.register();

        if (settingsService.getSettings().autostartEnabled()) {
            apiServer.start();
        }
        if (settingsService.getSettings().autostartEnabled() && settingsService.getSettings().discordIntegrationEnabled()) {
            discordService.start();
            statusService.markDiscordConnected(discordService.status().connected());
        }

        burpLinkCordTab.refreshView();
        auditLogger.log("lifecycle.start");
    }

    /**
     * Stops the extension runtime.
     */
    public void shutdown() {
        apiServer.stop();
        discordService.stop();
        scanService.close();
        burpLinkCordTab.shutdown();
        if (suiteTabRegistration != null) {
            suiteTabRegistration.deregister();
        }
        if (unloadingRegistration != null) {
            unloadingRegistration.deregister();
        }
        statusService.markExtensionLoaded(false);
        statusService.markDiscordConnected(false);
        statusService.recordActivity("Extension stopped");
        statusService.setMessage("Extension stopped");
        eventBus.publish("runtime.notification", new RuntimeEvent(
                "Extension Stopped",
                "BurpLinkCord extension stopped inside Burp Suite.",
                "INFO"
        ));
        auditLogger.log("lifecycle.stop");
    }
}
