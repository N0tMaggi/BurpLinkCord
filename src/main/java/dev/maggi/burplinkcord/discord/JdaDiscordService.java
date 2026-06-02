package dev.maggi.burplinkcord.discord;

import dev.maggi.burplinkcord.config.ApplicationConfiguration;
import dev.maggi.burplinkcord.config.RuntimeSettings;
import dev.maggi.burplinkcord.domain.model.Finding;
import dev.maggi.burplinkcord.domain.model.Scan;
import dev.maggi.burplinkcord.domain.model.ScanStatus;
import dev.maggi.burplinkcord.domain.service.DiscordService;
import dev.maggi.burplinkcord.domain.service.FindingService;
import dev.maggi.burplinkcord.domain.service.InMemoryStatusService;
import dev.maggi.burplinkcord.domain.service.ScanService;
import dev.maggi.burplinkcord.domain.service.SettingsService;
import dev.maggi.burplinkcord.events.EventBus;
import dev.maggi.burplinkcord.events.RuntimeEvent;
import dev.maggi.burplinkcord.exception.BurpLinkCordException;
import dev.maggi.burplinkcord.logging.AuditLogger;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * JDA-backed Discord bot runtime for BurpLinkCord.
 */
public class JdaDiscordService implements DiscordService {

    private final ApplicationConfiguration applicationConfiguration;
    private final SettingsService settingsService;
    private final ScanService scanService;
    private final FindingService findingService;
    private final InMemoryStatusService statusService;
    private final EventBus eventBus;
    private final AuditLogger auditLogger;
    private final DiscordAccessPolicy accessPolicy;
    private final List<AutoCloseable> subscriptions = new ArrayList<>();

    private volatile DiscordRuntimeStatus runtimeStatus = DiscordRuntimeStatus.disconnected();
    private JDA jda;

    /**
     * Creates a Discord service.
     *
     * @param applicationConfiguration bootstrap configuration
     * @param settingsService settings service
     * @param scanService scan service
     * @param findingService finding service
     * @param statusService status service
     * @param eventBus event bus
     * @param auditLogger audit logger
     */
    public JdaDiscordService(
            ApplicationConfiguration applicationConfiguration,
            SettingsService settingsService,
            ScanService scanService,
            FindingService findingService,
            InMemoryStatusService statusService,
            EventBus eventBus,
            AuditLogger auditLogger
    ) {
        this.applicationConfiguration = Objects.requireNonNull(applicationConfiguration, "applicationConfiguration");
        this.settingsService = Objects.requireNonNull(settingsService, "settingsService");
        this.scanService = Objects.requireNonNull(scanService, "scanService");
        this.findingService = Objects.requireNonNull(findingService, "findingService");
        this.statusService = Objects.requireNonNull(statusService, "statusService");
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
        this.auditLogger = Objects.requireNonNull(auditLogger, "auditLogger");
        this.accessPolicy = new DiscordAccessPolicy(settingsService);
    }

    @Override
    public synchronized void start() {
        if (jda != null) {
            return;
        }

        RuntimeSettings settings = settingsService.getSettings();
        String token = settingsService.loadDiscordBotToken();
        if (!settings.discordIntegrationEnabled()) {
            runtimeStatus = new DiscordRuntimeStatus(!token.isBlank(), false, false, "", "Discord integration disabled in settings");
            statusService.markDiscordConnected(false);
            statusService.recordActivity("Discord integration disabled in settings");
            return;
        }
        if (token == null || token.isBlank()) {
            throw new BurpLinkCordException("Discord bot token is not configured.");
        }

        try {
            DiscordCommandListener commandListener = new DiscordCommandListener(
                    accessPolicy,
                    scanService,
                    findingService,
                    statusService,
                    settingsService,
                    auditLogger
            );
            JDA instance = JDABuilder.createDefault(token, GatewayIntent.GUILD_MESSAGES)
                    .setStatus(OnlineStatus.ONLINE)
                    .setActivity(resolveActivity())
                    .addEventListeners(commandListener, new PresenceListener())
                    .build()
                    .awaitReady();

            registerCommands(instance, settings);
            jda = instance;
            subscribeNotifications();
            publishControlPanelIfConfigured();
            runtimeStatus = new DiscordRuntimeStatus(true, true, true, instance.getSelfUser().getAsTag(), "Discord bot connected");
            statusService.markDiscordConnected(true);
            statusService.setMessage(runtimeStatus.statusMessage());
            statusService.recordActivity(runtimeStatus.statusMessage());
            eventBus.publish("runtime.notification", new RuntimeEvent(
                    "Discord Connected",
                    "BurpLinkCord connected the Discord bot as " + instance.getSelfUser().getAsTag() + ".",
                    "INFO"
            ));
            auditLogger.log("discord.start");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            statusService.markDiscordConnected(false);
            throw new BurpLinkCordException("Discord startup was interrupted.", exception);
        } catch (Exception exception) {
            runtimeStatus = new DiscordRuntimeStatus(true, true, false, "", "Discord startup failed: " + exception.getMessage());
            statusService.markDiscordConnected(false);
            statusService.setMessage(runtimeStatus.statusMessage());
            statusService.recordActivity(runtimeStatus.statusMessage());
            eventBus.publish("runtime.notification", new RuntimeEvent(
                    "Discord Startup Failed",
                    runtimeStatus.statusMessage(),
                    "ERROR"
            ));
            throw new BurpLinkCordException("Unable to start Discord integration.", exception);
        }
    }

    @Override
    public synchronized void stop() {
        subscriptions.forEach(subscription -> {
            try {
                subscription.close();
            } catch (Exception ignored) {
            }
        });
        subscriptions.clear();

        if (jda != null) {
            jda.shutdown();
            jda = null;
        }

        RuntimeSettings settings = settingsService.getSettings();
        String token = settingsService.loadDiscordBotToken();
        runtimeStatus = new DiscordRuntimeStatus(!token.isBlank(), settings.discordIntegrationEnabled(), false, "", "Discord bot stopped");
        statusService.markDiscordConnected(false);
        statusService.setMessage(runtimeStatus.statusMessage());
        statusService.recordActivity(runtimeStatus.statusMessage());
        eventBus.publish("runtime.notification", new RuntimeEvent(
                "Discord Stopped",
                "BurpLinkCord stopped the Discord bot runtime.",
                "INFO"
        ));
        auditLogger.log("discord.stop");
    }

    @Override
    public DiscordRuntimeStatus status() {
        return runtimeStatus;
    }

    @Override
    public synchronized void publishControlPanel() {
        if (jda == null) {
            throw new BurpLinkCordException("Discord bot is not connected.");
        }

        publishControlPanelInternal(true);
    }

    private void subscribeNotifications() {
        subscriptions.add(eventBus.subscribe("scan.started", (eventType, payload) -> sendScanNotification("Scan started", (Scan) payload)));
        subscriptions.add(eventBus.subscribe("scan.status.changed", (eventType, payload) -> {
            updatePresence();
            Scan scan = (Scan) payload;
            sendScanNotification(titleForScanStatus(scan.status()), scan);
        }));
        subscriptions.add(eventBus.subscribe("finding.discovered", (eventType, payload) -> sendFindingNotification((Finding) payload)));
        subscriptions.add(eventBus.subscribe("runtime.notification", (eventType, payload) -> sendRuntimeNotification((RuntimeEvent) payload)));
    }

    private void registerCommands(JDA instance, RuntimeSettings settings) {
        List<net.dv8tion.jda.api.interactions.commands.build.CommandData> commands = List.of(
                Commands.slash("dashboard", "Open the BurpLinkCord dashboard"),
                Commands.slash("status", "Show BurpLinkCord runtime status"),
                Commands.slash("scans", "List tracked scans"),
                Commands.slash("activity", "Show recent BurpLinkCord activity"),
                Commands.slash("findings", "List current findings"),
                Commands.slash("targeting", "Show configured targeting and profile settings"),
                Commands.slash("startscan", "Start a Burp scan")
                        .addOptions(
                                new OptionData(OptionType.STRING, "target", "Target URL to scan", true),
                                new OptionData(OptionType.STRING, "profile", "Scan profile", false),
                                new OptionData(OptionType.STRING, "configuration", "Scan configuration", false),
                                new OptionData(OptionType.BOOLEAN, "crawl", "Enable crawl phase", false),
                                new OptionData(OptionType.BOOLEAN, "audit", "Enable audit phase", false)
                        ),
                Commands.slash("pausescan", "Pause an active scan")
                        .addOption(OptionType.STRING, "scanid", "Scan identifier", true),
                Commands.slash("resumescan", "Resume a paused or stopped scan")
                        .addOption(OptionType.STRING, "scanid", "Scan identifier", true),
                Commands.slash("stopscan", "Stop an active scan")
                        .addOption(OptionType.STRING, "scanid", "Scan identifier", true),
                Commands.slash("deletescan", "Delete a tracked scan")
                        .addOption(OptionType.STRING, "scanid", "Scan identifier", true)
        );

        if (settings.discordGuildId() != null && !settings.discordGuildId().isBlank()) {
            Guild guild = requireConfiguredGuild(instance, settings);
            guild.updateCommands().addCommands(commands).queue();
            runtimeStatus = new DiscordRuntimeStatus(true, true, true, instance.getSelfUser().getAsTag(), "Discord bot connected with guild commands");
        } else {
            instance.updateCommands().addCommands(commands).queue();
        }
    }

    private void sendScanNotification(String title, Scan scan) {
        if (jda == null) {
            return;
        }
        resolveUpdateChannels().forEach(channel -> channel.sendMessageEmbeds(buildScanNotificationEmbed(title, scan))
                .setComponents(buildNotificationRows(scan))
                .queue());
    }

    private void sendFindingNotification(Finding finding) {
        if (jda == null) {
            return;
        }
        statusService.recordActivity("Finding discovered: " + finding.title());
        resolveUpdateChannels().forEach(channel -> channel.sendMessageEmbeds(buildFindingNotificationEmbed(finding))
                .setComponents(buildFindingNotificationRows())
                .queue());
    }

    private void sendRuntimeNotification(RuntimeEvent runtimeEvent) {
        if (jda == null) {
            return;
        }
        resolveUpdateChannels().forEach(channel -> channel.sendMessageEmbeds(buildRuntimeNotificationEmbed(runtimeEvent)).queue());
    }

    private List<TextChannel> resolveChannels() {
        if (jda == null) {
            return List.of();
        }
        RuntimeSettings settings = settingsService.getSettings();
        Guild configuredGuild = resolveConfiguredGuild(jda, settings);
        return settingsService.getSettings().allowedDiscordChannelIds().stream()
                .map(jda::getTextChannelById)
                .filter(Objects::nonNull)
                .filter(channel -> configuredGuild == null || channel.getGuild().getId().equals(configuredGuild.getId()))
                .toList();
    }

    private List<TextChannel> resolveUpdateChannels() {
        if (jda == null) {
            return List.of();
        }

        RuntimeSettings settings = settingsService.getSettings();
        if (settings.discordUpdateChannelId() != null && !settings.discordUpdateChannelId().isBlank()) {
            TextChannel updateChannel = jda.getTextChannelById(settings.discordUpdateChannelId());
            Guild configuredGuild = resolveConfiguredGuild(jda, settings);
            if (updateChannel == null) {
                return List.of();
            }
            if (configuredGuild != null && !updateChannel.getGuild().getId().equals(configuredGuild.getId())) {
                return List.of();
            }
            return List.of(updateChannel);
        }

        return resolveChannels();
    }

    private void publishControlPanelIfConfigured() {
        if (resolveChannels().isEmpty()) {
            statusService.recordActivity("Discord connected without allowed command channels; control panel was not published");
            return;
        }

        publishControlPanelInternal(false);
    }

    private void publishControlPanelInternal(boolean emitRuntimeEvent) {
        List<TextChannel> channels = resolveChannels();
        if (channels.isEmpty()) {
            throw new BurpLinkCordException("No allowed Discord channels are configured.");
        }

        channels.forEach(channel -> channel.sendMessageEmbeds(buildControlPanelEmbed())
                .setComponents(buildControlPanelRows())
                .queue());
        statusService.recordActivity("Published Discord control panel to " + channels.size() + " channel(s)");
        if (emitRuntimeEvent) {
            eventBus.publish("runtime.notification", new RuntimeEvent(
                    "Control Panel Published",
                    "BurpLinkCord published the Discord control panel to " + channels.size() + " channel(s).",
                    "INFO"
            ));
        }
        auditLogger.log("discord.panel.publish channels=" + channels.size());
    }

    private Activity resolveActivity() {
        String statusText = applicationConfiguration.discordPresence() + " | " + statusService.currentStatus().activeScanCount() + " scans";
        return Activity.watching(statusText);
    }

    private void updatePresence() {
        if (jda != null) {
            jda.getPresence().setPresence(OnlineStatus.ONLINE, resolveActivity(), false);
        }
    }

    private net.dv8tion.jda.api.entities.MessageEmbed buildControlPanelEmbed() {
        return new EmbedBuilder()
                .setTitle("BurpLinkCord Control Panel")
                .setColor(new Color(0xC0392B))
                .setDescription("Use this panel to open the dashboard, review tracked scans, inspect findings, or queue a new scan.")
                .addField("Bot Status", runtimeStatus.statusMessage(), false)
                .addField("Active Scans", String.valueOf(statusService.currentStatus().activeScanCount()), true)
                .addField("Allowed Channels", String.valueOf(settingsService.getSettings().allowedDiscordChannelIds().size()), true)
                .addField("Update Channel", settingsService.getSettings().discordUpdateChannelId().isBlank()
                        ? "Not configured"
                        : settingsService.getSettings().discordUpdateChannelId(), false)
                .addField("Allowed Domains", settingsService.getSettings().allowedDomains().isEmpty()
                        ? "No domain restrictions configured."
                        : String.join(", ", settingsService.getSettings().allowedDomains()), false)
                .setFooter("This panel can be reposted from the Burp Discord settings tab.")
                .build();
    }

    private List<ActionRow> buildControlPanelRows() {
        return List.of(
                ActionRow.of(
                        Button.primary("refresh_status", "Dashboard"),
                        Button.success("open_startscan_modal", "New Scan"),
                        Button.secondary("list_scans", "Scans"),
                        Button.secondary("show_activity", "Activity"),
                        Button.secondary("list_findings", "Findings")
                ),
                ActionRow.of(
                        Button.secondary("show_targeting", "Targeting")
                )
        );
    }

    private EmbedBuilder scanEmbedBuilder(String title, Scan scan) {
        return new EmbedBuilder()
                .setTitle(title)
                .setColor(resolveScanColor(scan.status()))
                .addField("Target", scan.target(), false)
                .addField("Scan ID", scan.id(), false)
                .addField("Status", scan.status().name(), true)
                .addField("Profile", scan.profileName(), true)
                .addField("Configuration", scan.configurationName(), true)
                .setFooter("Use the buttons below to continue in Discord.")
                .setTimestamp(scan.updatedAt());
    }

    private net.dv8tion.jda.api.entities.MessageEmbed buildScanNotificationEmbed(String title, Scan scan) {
        return scanEmbedBuilder(title, scan).build();
    }

    private net.dv8tion.jda.api.entities.MessageEmbed buildFindingNotificationEmbed(Finding finding) {
        return new EmbedBuilder()
                .setTitle("New finding: " + finding.title())
                .setColor(resolveSeverityColor(finding.severity().name()))
                .addField("Severity", finding.severity().name(), true)
                .addField("Target", finding.target(), false)
                .addField("Finding ID", finding.id(), false)
                .setDescription(truncate(finding.description(), 900))
                .setFooter("Review the findings overview or scan dashboard for more context.")
                .build();
    }

    private net.dv8tion.jda.api.entities.MessageEmbed buildRuntimeNotificationEmbed(RuntimeEvent runtimeEvent) {
        return new EmbedBuilder()
                .setTitle(runtimeEvent.title())
                .setColor(resolveRuntimeColor(runtimeEvent.severity()))
                .setDescription(runtimeEvent.message())
                .setFooter("BurpLinkCord runtime update")
                .build();
    }

    private List<ActionRow> buildNotificationRows(Scan scan) {
        List<Button> primaryButtons = new ArrayList<>();
        primaryButtons.add(Button.secondary("details_scan:" + scan.id(), "View"));

        if (scan.status() == ScanStatus.RUNNING) {
            primaryButtons.add(Button.primary("pause_scan:" + scan.id(), "Pause"));
            primaryButtons.add(Button.danger("stop_scan:" + scan.id(), "Stop"));
        } else if (scan.status() == ScanStatus.PAUSED || scan.status() == ScanStatus.STOPPED) {
            primaryButtons.add(Button.success("resume_scan:" + scan.id(), "Resume"));
        }

        primaryButtons.add(Button.secondary("delete_scan:" + scan.id(), "Delete"));
        return List.of(
                ActionRow.of(primaryButtons),
                ActionRow.of(
                        Button.primary("refresh_status", "Dashboard"),
                        Button.secondary("list_scans", "All Scans"),
                        Button.secondary("show_activity", "Activity"),
                        Button.secondary("list_findings", "Findings")
                )
        );
    }

    private List<ActionRow> buildFindingNotificationRows() {
        return List.of(
                ActionRow.of(
                        Button.primary("refresh_status", "Dashboard"),
                        Button.secondary("show_activity", "Activity"),
                        Button.secondary("list_findings", "Findings"),
                        Button.secondary("list_scans", "Scans")
                )
        );
    }

    private Color resolveScanColor(ScanStatus status) {
        return switch (status) {
            case RUNNING -> new Color(0x1F8B4C);
            case PAUSED -> new Color(0xF1C40F);
            case STOPPED, DELETED -> new Color(0x7F8C8D);
            case COMPLETED -> new Color(0x3498DB);
            case QUEUED -> new Color(0x9B59B6);
        };
    }

    private Color resolveSeverityColor(String severity) {
        return switch (severity) {
            case "HIGH" -> new Color(0xC0392B);
            case "MEDIUM" -> new Color(0xE67E22);
            case "LOW" -> new Color(0xF1C40F);
            default -> new Color(0x3498DB);
        };
    }

    private Color resolveRuntimeColor(String severity) {
        return switch (severity) {
            case "ERROR" -> new Color(0xC0392B);
            case "WARN" -> new Color(0xE67E22);
            default -> new Color(0x3498DB);
        };
    }

    private Guild requireConfiguredGuild(JDA instance, RuntimeSettings settings) {
        Guild guild = resolveConfiguredGuild(instance, settings);
        if (guild == null) {
            throw new BurpLinkCordException("The configured Discord guild could not be found by the bot.");
        }
        return guild;
    }

    private Guild resolveConfiguredGuild(JDA instance, RuntimeSettings settings) {
        if (settings.discordGuildId() == null || settings.discordGuildId().isBlank()) {
            return null;
        }
        return instance.getGuildById(settings.discordGuildId());
    }

    private String titleForScanStatus(ScanStatus status) {
        return switch (status) {
            case QUEUED -> "Scan queued";
            case RUNNING -> "Scan running";
            case PAUSED -> "Scan paused";
            case STOPPED -> "Scan stopped";
            case COMPLETED -> "Scan completed";
            case DELETED -> "Scan deleted";
        };
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "No additional detail was provided by Burp Scanner.";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength) + "...";
    }

    private final class PresenceListener extends ListenerAdapter {

        @Override
        public void onReady(ReadyEvent event) {
            updatePresence();
        }
    }
}
