package dev.maggi.burplinkcord.bootstrap;

import burp.api.montoya.MontoyaApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.maggi.burplinkcord.api.ApiServer;
import dev.maggi.burplinkcord.api.controller.DiscordController;
import dev.maggi.burplinkcord.api.controller.HealthController;
import dev.maggi.burplinkcord.api.controller.IssueController;
import dev.maggi.burplinkcord.api.controller.ScanController;
import dev.maggi.burplinkcord.burp.BurpEventPublisher;
import dev.maggi.burplinkcord.burp.BurpLifecycleManager;
import dev.maggi.burplinkcord.config.ApplicationConfiguration;
import dev.maggi.burplinkcord.config.ConfigurationManager;
import dev.maggi.burplinkcord.config.SettingsStore;
import dev.maggi.burplinkcord.domain.service.DefaultSettingsService;
import dev.maggi.burplinkcord.domain.service.DiscordService;
import dev.maggi.burplinkcord.domain.service.FindingService;
import dev.maggi.burplinkcord.domain.service.InMemoryStatusService;
import dev.maggi.burplinkcord.domain.service.MontoyaFindingService;
import dev.maggi.burplinkcord.domain.service.MontoyaScanService;
import dev.maggi.burplinkcord.domain.service.ScanService;
import dev.maggi.burplinkcord.domain.service.SettingsService;
import dev.maggi.burplinkcord.discord.JdaDiscordService;
import dev.maggi.burplinkcord.events.EventBus;
import dev.maggi.burplinkcord.logging.AuditLogger;
import dev.maggi.burplinkcord.security.AccessValidator;
import dev.maggi.burplinkcord.security.AllowAllAuthorizationService;
import dev.maggi.burplinkcord.security.AuthenticationService;
import dev.maggi.burplinkcord.security.AuthorizationService;
import dev.maggi.burplinkcord.security.HeaderAuthenticationService;
import dev.maggi.burplinkcord.ui.BurpLinkCordTab;

/**
 * Assembles the BurpLinkCord application graph.
 */
public class ApplicationBootstrap {

    /**
     * Creates and wires the application lifecycle manager.
     *
     * @param api Montoya API instance
     * @return lifecycle manager
     */
    public BurpLifecycleManager initialize(MontoyaApi api) {
        SettingsStore settingsStore = SettingsStore.burpPreferences(api.persistence().preferences());
        ConfigurationManager configurationManager = new ConfigurationManager(settingsStore);
        ApplicationConfiguration applicationConfiguration = configurationManager.loadApplicationConfiguration();

        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        AuditLogger auditLogger = AuditLogger.create(applicationConfiguration.auditLoggingEnabled());
        EventBus eventBus = EventBus.inMemory();
        InMemoryStatusService statusService = new InMemoryStatusService();
        statusService.setMessage("Bootstrapped");

        SettingsService settingsService = new DefaultSettingsService(configurationManager);
        AuthenticationService authenticationService = new HeaderAuthenticationService(settingsService);
        AuthorizationService authorizationService = new AllowAllAuthorizationService();
        AccessValidator accessValidator = new AccessValidator(applicationConfiguration, authenticationService, authorizationService);

        FindingService findingService = new MontoyaFindingService(eventBus, auditLogger);
        api.scanner().registerAuditIssueHandler((MontoyaFindingService) findingService);
        MontoyaScanService scanService = new MontoyaScanService(api.scanner(), eventBus, auditLogger, statusService);
        DiscordService discordService = new JdaDiscordService(
                applicationConfiguration,
                settingsService,
                scanService,
                findingService,
                statusService,
                eventBus,
                auditLogger
        );

        HealthController healthController = new HealthController(applicationConfiguration, discordService, statusService, auditLogger);
        DiscordController discordController = new DiscordController(accessValidator, discordService, auditLogger);
        ScanController scanController = new ScanController(accessValidator, scanService, auditLogger);
        IssueController issueController = new IssueController(accessValidator, findingService, auditLogger);

        ApiServer apiServer = new ApiServer(
                applicationConfiguration,
                objectMapper,
                healthController,
                discordController,
                scanController,
                issueController,
                statusService,
                eventBus,
                auditLogger
        );

        BurpLinkCordTab burpLinkCordTab = new BurpLinkCordTab(
                applicationConfiguration,
                settingsService,
                scanService,
                statusService,
                discordService,
                auditLogger
        );

        BurpEventPublisher burpEventPublisher = new BurpEventPublisher(eventBus, auditLogger);

        return new BurpLifecycleManager(
                api,
                apiServer,
                burpLinkCordTab,
                burpEventPublisher,
                statusService,
                settingsService,
                scanService,
                discordService,
                eventBus,
                auditLogger
        );
    }
}
