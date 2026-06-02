package dev.maggi.burplinkcord.discord;

import dev.maggi.burplinkcord.api.request.StartScanRequest;
import dev.maggi.burplinkcord.config.RuntimeSettings;
import dev.maggi.burplinkcord.domain.model.Finding;
import dev.maggi.burplinkcord.domain.model.Scan;
import dev.maggi.burplinkcord.domain.model.ScanStatus;
import dev.maggi.burplinkcord.domain.service.FindingService;
import dev.maggi.burplinkcord.domain.service.ScanService;
import dev.maggi.burplinkcord.domain.service.SettingsService;
import dev.maggi.burplinkcord.domain.service.StatusService;
import dev.maggi.burplinkcord.logging.AuditLogger;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

import java.awt.Color;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Handles Discord slash commands, buttons, and modals.
 */
public class DiscordCommandListener extends ListenerAdapter {

    private static final String MODAL_START_SCAN = "startscan_modal";
    private static final int MAX_SUMMARY_SCANS = 3;
    private static final int MAX_SUMMARY_FINDINGS = 3;
    private static final int MAX_SCAN_ROWS = 5;
    private static final int MAX_FINDING_ROWS = 10;
    private static final int MAX_ACTIVITY_ROWS = 10;

    private final DiscordAccessPolicy accessPolicy;
    private final ScanService scanService;
    private final FindingService findingService;
    private final StatusService statusService;
    private final SettingsService settingsService;
    private final AuditLogger auditLogger;

    /**
     * Creates a Discord command listener.
     *
     * @param accessPolicy Discord access policy
     * @param scanService scan service
     * @param findingService finding service
     * @param statusService status service
     * @param settingsService settings service
     * @param auditLogger audit logger
     */
    public DiscordCommandListener(
            DiscordAccessPolicy accessPolicy,
            ScanService scanService,
            FindingService findingService,
            StatusService statusService,
            SettingsService settingsService,
            AuditLogger auditLogger
    ) {
        this.accessPolicy = Objects.requireNonNull(accessPolicy, "accessPolicy");
        this.scanService = Objects.requireNonNull(scanService, "scanService");
        this.findingService = Objects.requireNonNull(findingService, "findingService");
        this.statusService = Objects.requireNonNull(statusService, "statusService");
        this.settingsService = Objects.requireNonNull(settingsService, "settingsService");
        this.auditLogger = Objects.requireNonNull(auditLogger, "auditLogger");
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!authorize(event.getUser().getId(), guildId(event), event.getChannel().getId(), event)) {
            return;
        }

        safeReply(event, () -> handleSlashCommand(event));
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (!authorize(event.getUser().getId(), guildId(event), event.getChannel().getId(), event)) {
            return;
        }

        safeReply(event, () -> handleButtonInteraction(event));
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (!authorize(event.getUser().getId(), guildId(event), event.getChannel().getId(), event)) {
            return;
        }
        if (!MODAL_START_SCAN.equals(event.getModalId())) {
            return;
        }

        safeReply(event, () -> {
            StartScanRequest request = new StartScanRequest(
                    requiredModalValue(event, "target", "A target URL is required."),
                    settingsService.getSettings().allowedDomains(),
                    modalValueOrDefault(event, "profile", settingsService.getSettings().defaultScanProfile()),
                    modalValueOrDefault(event, "configuration", settingsService.getSettings().defaultScanConfiguration()),
                    true,
                    true
            );

            Scan scan = scanService.startScan(request);
            auditLogger.log("discord.scan.start user=" + event.getUser().getId() + " scanId=" + scan.id());
            event.replyEmbeds(buildSingleScanEmbed("Scan started from modal", scan))
                    .addComponents(buildScanControlRows(scan))
                    .setEphemeral(true)
                    .queue();
        });
    }

    private void handleSlashCommand(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "dashboard", "status" -> replyWithDashboard(event);
            case "scans" -> replyWithScans(event);
            case "activity" -> event.replyEmbeds(buildActivityEmbed())
                    .addActionRow(
                            Button.primary("refresh_status", "Dashboard"),
                            Button.secondary("show_activity", "Refresh Activity")
                    )
                    .setEphemeral(true)
                    .queue();
            case "findings" -> event.replyEmbeds(buildFindingsEmbed(findingService.getFindings()))
                    .addActionRow(
                            Button.secondary("list_findings", "Refresh Findings"),
                            Button.primary("refresh_status", "Dashboard")
                    )
                    .setEphemeral(true)
                    .queue();
            case "targeting" -> event.replyEmbeds(buildTargetingEmbed())
                    .addActionRow(
                            Button.primary("refresh_status", "Dashboard"),
                            Button.success("open_startscan_modal", "New Scan")
                    )
                    .setEphemeral(true)
                    .queue();
            case "startscan" -> {
                StartScanRequest request = new StartScanRequest(
                        requiredOption(event.getOption("target"), "A target URL is required."),
                        settingsService.getSettings().allowedDomains(),
                        stringOption(event.getOption("profile"), settingsService.getSettings().defaultScanProfile()),
                        stringOption(event.getOption("configuration"), settingsService.getSettings().defaultScanConfiguration()),
                        booleanOption(event.getOption("crawl"), true),
                        booleanOption(event.getOption("audit"), true)
                );
                Scan scan = scanService.startScan(request);
                auditLogger.log("discord.scan.start user=" + event.getUser().getId() + " scanId=" + scan.id());
                event.replyEmbeds(buildSingleScanEmbed("Scan started", scan))
                        .addComponents(buildScanControlRows(scan))
                        .setEphemeral(true)
                        .queue();
            }
            case "pausescan" -> replyWithUpdatedScan(event, "Scan paused", scanService.pauseScan(requiredOption(event.getOption("scanid"), "A scan id is required.")).orElseThrow());
            case "resumescan" -> replyWithUpdatedScan(event, "Scan resumed", scanService.resumeScan(requiredOption(event.getOption("scanid"), "A scan id is required.")).orElseThrow());
            case "stopscan" -> replyWithUpdatedScan(event, "Scan stopped", scanService.stopScan(requiredOption(event.getOption("scanid"), "A scan id is required.")).orElseThrow());
            case "deletescan" -> replyWithUpdatedScan(event, "Scan deleted", scanService.deleteScan(requiredOption(event.getOption("scanid"), "A scan id is required.")).orElseThrow());
            default -> event.reply("Unknown command.").setEphemeral(true).queue();
        }
    }

    private void handleButtonInteraction(ButtonInteractionEvent event) {
        String componentId = event.getComponentId();
        if ("refresh_status".equals(componentId)) {
            event.replyEmbeds(buildDashboardEmbed())
                    .addComponents(buildDashboardRows())
                    .setEphemeral(true)
                    .queue();
            return;
        }
        if ("list_scans".equals(componentId)) {
            replyWithScans(event);
            return;
        }
        if ("show_activity".equals(componentId)) {
            event.replyEmbeds(buildActivityEmbed())
                    .addActionRow(
                            Button.primary("refresh_status", "Dashboard"),
                            Button.secondary("show_activity", "Refresh Activity")
                    )
                    .setEphemeral(true)
                    .queue();
            return;
        }
        if ("list_findings".equals(componentId)) {
            event.replyEmbeds(buildFindingsEmbed(findingService.getFindings()))
                    .addActionRow(
                            Button.primary("refresh_status", "Dashboard"),
                            Button.secondary("list_scans", "Scans")
                    )
                    .setEphemeral(true)
                    .queue();
            return;
        }
        if ("show_targeting".equals(componentId)) {
            event.replyEmbeds(buildTargetingEmbed())
                    .addActionRow(
                            Button.primary("refresh_status", "Dashboard"),
                            Button.success("open_startscan_modal", "New Scan")
                    )
                    .setEphemeral(true)
                    .queue();
            return;
        }
        if ("open_startscan_modal".equals(componentId)) {
            event.replyModal(buildStartScanModal()).queue();
            return;
        }
        if (componentId.startsWith("details_scan:")) {
            Scan scan = requireScan(componentId, "details_scan:");
            event.replyEmbeds(buildSingleScanEmbed("Scan details", scan))
                    .addComponents(buildScanControlRows(scan))
                    .setEphemeral(true)
                    .queue();
            return;
        }
        if (componentId.startsWith("stop_scan:")) {
            Scan scan = scanService.stopScan(extractScanId(componentId, "stop_scan:")).orElseThrow();
            replyWithUpdatedScan(event, "Scan stopped", scan);
            return;
        }
        if (componentId.startsWith("pause_scan:")) {
            Scan scan = scanService.pauseScan(extractScanId(componentId, "pause_scan:")).orElseThrow();
            replyWithUpdatedScan(event, "Scan paused", scan);
            return;
        }
        if (componentId.startsWith("resume_scan:")) {
            Scan scan = scanService.resumeScan(extractScanId(componentId, "resume_scan:")).orElseThrow();
            replyWithUpdatedScan(event, "Scan resumed", scan);
            return;
        }
        if (componentId.startsWith("delete_scan:")) {
            Scan scan = scanService.deleteScan(extractScanId(componentId, "delete_scan:")).orElseThrow();
            replyWithUpdatedScan(event, "Scan deleted", scan);
            return;
        }

        event.reply("Unknown button action.").setEphemeral(true).queue();
    }

    private void replyWithDashboard(IReplyCallback event) {
        event.replyEmbeds(buildDashboardEmbed())
                .addComponents(buildDashboardRows())
                .setEphemeral(true)
                .queue();
    }

    private void replyWithScans(IReplyCallback event) {
        List<Scan> scans = scanService.getScans();
        List<ActionRow> rows = new ArrayList<>();
        rows.add(ActionRow.of(
                Button.primary("refresh_status", "Dashboard"),
                Button.success("open_startscan_modal", "New Scan"),
                Button.secondary("show_activity", "Activity")
        ));
        rows.addAll(buildScanActionRows(scans));

        event.replyEmbeds(buildScansEmbed(scans))
                .addComponents(rows)
                .setEphemeral(true)
                .queue();
    }

    private void replyWithUpdatedScan(IReplyCallback event, String title, Scan scan) {
        event.replyEmbeds(buildSingleScanEmbed(title, scan))
                .addComponents(buildScanControlRows(scan))
                .setEphemeral(true)
                .queue();
    }

    private boolean authorize(String userId, String guildId, String channelId, IReplyCallback event) {
        if (accessPolicy.isAllowed(userId, guildId, channelId)) {
            return true;
        }
        event.reply("Access denied. Configure the whitelisted Discord user, guild, and channel settings first.")
                .setEphemeral(true)
                .queue();
        return false;
    }

    private String guildId(net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent event) {
        return event.getGuild() == null ? "" : event.getGuild().getId();
    }

    private MessageEmbed buildStatusEmbed() {
        return new EmbedBuilder()
                .setTitle("BurpLinkCord Status")
                .setColor(new Color(0xC0392B))
                .addField("Extension", String.valueOf(statusService.currentStatus().extensionLoaded()), true)
                .addField("API Running", String.valueOf(statusService.currentStatus().apiRunning()), true)
                .addField("Discord Connected", String.valueOf(statusService.currentStatus().discordConnected()), true)
                .addField("Active Scans", String.valueOf(statusService.currentStatus().activeScanCount()), true)
                .addField("Status", statusService.currentStatus().message(), false)
                .addField("Whitelisted Users", String.valueOf(settingsService.getSettings().whitelistedDiscordIds().size()), true)
                .addField("Allowed Channels", String.valueOf(settingsService.getSettings().allowedDiscordChannelIds().size()), true)
                .setTimestamp(Instant.now())
                .build();
    }

    private MessageEmbed buildDashboardEmbed() {
        List<Scan> scans = scanService.getScans();
        List<Finding> findings = findingService.getFindings();

        return new EmbedBuilder()
                .setTitle("BurpLinkCord Dashboard")
                .setColor(new Color(0xC0392B))
                .setDescription("Use the controls below to review status, inspect scans, and manage scans without typing identifiers.")
                .addField("Runtime", summarizeRuntime(), false)
                .addField("Tracked Scans", summarizeScans(scans), false)
                .addField("Latest Findings", summarizeFindings(findings), false)
                .addField("Recent Activity", summarizeActivity(), false)
                .addField("Targeting", summarizeTargeting(settingsService.getSettings()), false)
                .setFooter("Slash commands remain available for direct automation.")
                .setTimestamp(Instant.now())
                .build();
    }

    private List<ActionRow> buildDashboardRows() {
        return List.of(
                ActionRow.of(
                        Button.primary("refresh_status", "Refresh"),
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

    private MessageEmbed buildScansEmbed(List<Scan> scans) {
        EmbedBuilder builder = new EmbedBuilder()
                .setTitle("BurpLinkCord Scans")
                .setColor(new Color(0x1F8B4C))
                .setTimestamp(Instant.now());

        if (scans.isEmpty()) {
            builder.setDescription("No scans are currently tracked.");
            return builder.build();
        }

        scans.stream().limit(10).forEach(scan -> builder.addField(
                abbreviate(scan.id()) + "  " + scan.status(),
                "Target: %s%nProfile: %s%nUpdated: %s".formatted(
                        scan.target(),
                        scan.profileName(),
                        scan.updatedAt()
                ),
                false
        ));
        return builder.build();
    }

    private List<ActionRow> buildScanActionRows(List<Scan> scans) {
        return scans.stream()
                .limit(MAX_SCAN_ROWS)
                .map(this::buildScanActionRow)
                .toList();
    }

    private ActionRow buildScanActionRow(Scan scan) {
        List<Button> buttons = new ArrayList<>();
        buttons.add(Button.secondary("details_scan:" + scan.id(), "View " + abbreviate(scan.id())));

        if (scan.status() == ScanStatus.RUNNING) {
            buttons.add(Button.primary("pause_scan:" + scan.id(), "Pause"));
            buttons.add(Button.danger("stop_scan:" + scan.id(), "Stop"));
        } else if (scan.status() == ScanStatus.PAUSED || scan.status() == ScanStatus.STOPPED) {
            buttons.add(Button.success("resume_scan:" + scan.id(), "Resume"));
        }

        buttons.add(Button.secondary("delete_scan:" + scan.id(), "Delete"));
        return ActionRow.of(buttons);
    }

    private List<ActionRow> buildScanControlRows(Scan scan) {
        return List.of(
                ActionRow.of(
                        Button.primary("refresh_status", "Dashboard"),
                        Button.secondary("list_scans", "All Scans"),
                        Button.secondary("show_activity", "Activity")
                ),
                buildScanActionRow(scan)
        );
    }

    private MessageEmbed buildActivityEmbed() {
        EmbedBuilder builder = new EmbedBuilder()
                .setTitle("BurpLinkCord Activity")
                .setColor(new Color(0x3498DB))
                .setTimestamp(Instant.now());

        List<String> entries = statusService.recentActivity();
        if (entries.isEmpty()) {
            builder.setDescription("No recent activity has been recorded yet.");
            return builder.build();
        }

        entries.stream()
                .limit(MAX_ACTIVITY_ROWS)
                .forEach(entry -> builder.addField("Event", entry, false));
        return builder.build();
    }

    private MessageEmbed buildFindingsEmbed(List<Finding> findings) {
        EmbedBuilder builder = new EmbedBuilder()
                .setTitle("BurpLinkCord Findings")
                .setColor(new Color(0xF1C40F))
                .setTimestamp(Instant.now());

        if (findings.isEmpty()) {
            builder.setDescription("No findings have been collected yet.");
            return builder.build();
        }

        findings.stream().limit(MAX_FINDING_ROWS).forEach(finding -> builder.addField(
                "%s  %s".formatted(finding.severity(), finding.title()),
                "Target: %s%nID: %s".formatted(finding.target(), abbreviate(finding.id())),
                false
        ));
        return builder.build();
    }

    private MessageEmbed buildTargetingEmbed() {
        RuntimeSettings settings = settingsService.getSettings();
        String domains = settings.allowedDomains().isEmpty()
                ? "No domain restrictions configured."
                : String.join("\n", settings.allowedDomains());
        return new EmbedBuilder()
                .setTitle("BurpLinkCord Targeting")
                .setColor(new Color(0x8E44AD))
                .addField("Default Profile", settings.defaultScanProfile(), true)
                .addField("Default Configuration", settings.defaultScanConfiguration(), true)
                .addField("Configured Guild", settings.discordGuildId().isBlank() ? "Global commands" : settings.discordGuildId(), false)
                .addField("Allowed Domains", domains, false)
                .addField(
                        "Allowed Discord Channels",
                        settings.allowedDiscordChannelIds().isEmpty()
                                ? "No channel restrictions configured."
                                : String.join("\n", settings.allowedDiscordChannelIds()),
                        false
                )
                .setTimestamp(Instant.now())
                .build();
    }

    private MessageEmbed buildSingleScanEmbed(String title, Scan scan) {
        return new EmbedBuilder()
                .setTitle(title)
                .setColor(new Color(0x3498DB))
                .addField("Scan ID", scan.id(), false)
                .addField("Target", scan.target(), false)
                .addField("Status", scan.status().name(), true)
                .addField("Profile", scan.profileName(), true)
                .addField("Configuration", scan.configurationName(), true)
                .addField("Updated", scan.updatedAt().toString(), true)
                .setTimestamp(Instant.now())
                .build();
    }

    private Modal buildStartScanModal() {
        TextInput target = TextInput.create("target", "Target URL", TextInputStyle.SHORT)
                .setPlaceholder("https://example.org")
                .setRequired(true)
                .build();
        TextInput profile = TextInput.create("profile", "Profile", TextInputStyle.SHORT)
                .setValue(settingsService.getSettings().defaultScanProfile())
                .setRequired(false)
                .build();
        TextInput configuration = TextInput.create("configuration", "Configuration", TextInputStyle.SHORT)
                .setValue(settingsService.getSettings().defaultScanConfiguration())
                .setRequired(false)
                .build();
        return Modal.create(MODAL_START_SCAN, "Start Burp Scan")
                .addComponents(ActionRow.of(target), ActionRow.of(profile), ActionRow.of(configuration))
                .build();
    }

    private Scan requireScan(String componentId, String prefix) {
        String scanId = extractScanId(componentId, prefix);
        return scanService.getScans().stream()
                .filter(scan -> scan.id().equals(scanId))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("The selected scan is no longer available."));
    }

    private String extractScanId(String componentId, String prefix) {
        if (!componentId.startsWith(prefix)) {
            throw new IllegalArgumentException("Invalid scan action.");
        }
        return componentId.substring(prefix.length());
    }

    private String summarizeRuntime() {
        return """
                Extension: %s
                API: %s
                Discord: %s
                Active scans: %s
                Status: %s
                """.formatted(
                statusService.currentStatus().extensionLoaded(),
                statusService.currentStatus().apiRunning(),
                statusService.currentStatus().discordConnected(),
                statusService.currentStatus().activeScanCount(),
                statusService.currentStatus().message()
        ).trim();
    }

    private String summarizeTargeting(RuntimeSettings settings) {
        String domains = settings.allowedDomains().isEmpty()
                ? "No allowed domains configured."
                : String.join(", ", settings.allowedDomains());
        return "Profile: %s%nConfiguration: %s%nDomains: %s".formatted(
                settings.defaultScanProfile(),
                settings.defaultScanConfiguration(),
                domains
        );
    }

    private String summarizeScans(List<Scan> scans) {
        if (scans.isEmpty()) {
            return "No scans are currently tracked.";
        }

        return scans.stream()
                .limit(MAX_SUMMARY_SCANS)
                .map(scan -> "%s - %s - %s".formatted(abbreviate(scan.id()), scan.status(), scan.target()))
                .reduce((left, right) -> left + "\n" + right)
                .orElse("No scans are currently tracked.");
    }

    private String summarizeFindings(List<Finding> findings) {
        if (findings.isEmpty()) {
            return "No findings have been collected yet.";
        }

        return findings.stream()
                .limit(MAX_SUMMARY_FINDINGS)
                .map(finding -> "%s - %s".formatted(finding.severity(), finding.title()))
                .reduce((left, right) -> left + "\n" + right)
                .orElse("No findings have been collected yet.");
    }

    private String summarizeActivity() {
        List<String> entries = statusService.recentActivity();
        if (entries.isEmpty()) {
            return "No recent activity recorded.";
        }

        return entries.stream()
                .limit(3)
                .reduce((left, right) -> left + "\n" + right)
                .orElse("No recent activity recorded.");
    }

    private String abbreviate(String value) {
        return value.length() <= 8 ? value : value.substring(0, 8);
    }

    private void safeReply(IReplyCallback event, Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException | NoSuchElementException exception) {
            event.reply(exception.getMessage()).setEphemeral(true).queue();
        } catch (RuntimeException exception) {
            String message = exception.getMessage() == null || exception.getMessage().isBlank()
                    ? "The requested action could not be completed."
                    : exception.getMessage();
            event.reply(message).setEphemeral(true).queue();
        }
    }

    private String requiredOption(OptionMapping option, String message) {
        if (option == null || option.getAsString().isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return option.getAsString();
    }

    private String stringOption(OptionMapping option, String defaultValue) {
        return option == null ? defaultValue : valueOrDefault(option.getAsString(), defaultValue);
    }

    private boolean booleanOption(OptionMapping option, boolean defaultValue) {
        return option == null ? defaultValue : option.getAsBoolean();
    }

    private String requiredModalValue(ModalInteractionEvent event, String fieldId, String message) {
        String value = modalValueOrDefault(event, fieldId, "");
        if (value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private String modalValueOrDefault(ModalInteractionEvent event, String fieldId, String defaultValue) {
        if (event.getValue(fieldId) == null) {
            return defaultValue;
        }
        return valueOrDefault(event.getValue(fieldId).getAsString(), defaultValue);
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
