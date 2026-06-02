package dev.maggi.burplinkcord.ui.panel;

import dev.maggi.burplinkcord.config.ApplicationConfiguration;
import dev.maggi.burplinkcord.config.RuntimeSettings;
import dev.maggi.burplinkcord.domain.model.ScanStatus;
import dev.maggi.burplinkcord.domain.service.DiscordService;
import dev.maggi.burplinkcord.domain.service.ScanService;
import dev.maggi.burplinkcord.domain.service.SettingsService;
import dev.maggi.burplinkcord.domain.service.StatusService;
import dev.maggi.burplinkcord.domain.service.StatusSnapshot;
import dev.maggi.burplinkcord.ui.style.UiStyle;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Objects;

public class OverviewPanel extends JPanel {

    private final ApplicationConfiguration applicationConfiguration;
    private final SettingsService settingsService;
    private final StatusService statusService;
    private final ScanService scanService;
    private final DiscordService discordService;

    private final JLabel apiEndpointValue;
    private final JLabel extensionStatusValue;
    private final JLabel discordStatusValue;
    private final JLabel activeScansValue;
    private final JLabel queueStatusValue;
    private final JTextArea summaryArea;

    public OverviewPanel(
            ApplicationConfiguration applicationConfiguration,
            SettingsService settingsService,
            StatusService statusService,
            ScanService scanService,
            DiscordService discordService
    ) {
        super(new BorderLayout(12, 12));
        setBackground(UiStyle.BG_PANEL);
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        this.applicationConfiguration = Objects.requireNonNull(applicationConfiguration, "applicationConfiguration");
        this.settingsService = Objects.requireNonNull(settingsService, "settingsService");
        this.statusService = Objects.requireNonNull(statusService, "statusService");
        this.scanService = Objects.requireNonNull(scanService, "scanService");
        this.discordService = Objects.requireNonNull(discordService, "discordService");

        this.apiEndpointValue     = valueLabel();
        this.extensionStatusValue = valueLabel();
        this.discordStatusValue   = valueLabel();
        this.activeScansValue     = valueLabel();
        this.queueStatusValue     = valueLabel();

        this.summaryArea = new JTextArea();
        this.summaryArea.setEditable(false);
        this.summaryArea.setLineWrap(true);
        this.summaryArea.setWrapStyleWord(true);
        this.summaryArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        UiStyle.styleArea(summaryArea);

        JPanel statsGrid = new JPanel(new GridLayout(0, 2, 16, 10));
        statsGrid.setOpaque(false);
        statsGrid.add(statPair("API Endpoint",      apiEndpointValue));
        statsGrid.add(statPair("Extension Status",  extensionStatusValue));
        statsGrid.add(statPair("Discord",           discordStatusValue));
        statsGrid.add(statPair("Scans",             activeScansValue));
        statsGrid.add(statPair("Queue + API Secret", queueStatusValue));
        statsGrid.add(new JPanel() {{ setOpaque(false); }});

        JPanel statusCard = UiStyle.darkCard(new BorderLayout(0, 10));
        statusCard.add(UiStyle.createHeader("Runtime Summary", "status"), BorderLayout.NORTH);
        statusCard.add(statsGrid, BorderLayout.CENTER);

        JPanel summaryCard = UiStyle.darkCard(new BorderLayout(0, 8));
        summaryCard.add(UiStyle.createHeader("Integration Notes", "notes"), BorderLayout.NORTH);
        summaryCard.add(UiStyle.darkScroll(summaryArea), BorderLayout.CENTER);

        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setOpaque(false);
        content.add(statusCard, BorderLayout.NORTH);
        content.add(summaryCard, BorderLayout.CENTER);

        add(content, BorderLayout.CENTER);
        refresh();
    }

    public final void refresh() {
        StatusSnapshot snapshot = statusService.currentStatus();
        RuntimeSettings settings = settingsService.getSettings();

        apiEndpointValue.setText("http://%s:%d".formatted(
                applicationConfiguration.serverHost(), applicationConfiguration.serverPort()));
        extensionStatusValue.setText(snapshot.message());
        discordStatusValue.setText("enabled=%s, connected=%s, users=%d, channels=%d".formatted(
                settings.discordIntegrationEnabled(),
                discordService.status().connected(),
                settings.whitelistedDiscordIds().size(),
                settings.allowedDiscordChannelIds().size()));
        activeScansValue.setText("Active=%d, Autostart=%s".formatted(
                snapshot.activeScanCount(), settings.autostartEnabled()));
        queueStatusValue.setText("Tracked=%d, Secret=%s".formatted(
                scanService.getScans().stream().filter(s -> s.status() != ScanStatus.DELETED).count(),
                settingsService.loadApiSharedSecret().isBlank() ? "Not configured" : "Configured"));

        summaryArea.setText("""
                BurpLinkCord.

                Discord status: %s
                Current bot identity: %s
                Whitelisted guild: %s
                Update channel: %s
                Allowed domains: %s
                Default scan profile: %s
                Default scan configuration: %s
                Discord slash dashboard: %s
                Latest activity:
                %s
                """.formatted(
                discordService.status().statusMessage(),
                discordService.status().botTag().isBlank() ? "Not connected" : discordService.status().botTag(),
                settings.discordGuildId().isBlank() ? "Any guild not allowed until configured via user/channel rules" : settings.discordGuildId(),
                settings.discordUpdateChannelId().isBlank() ? "Falls back to allowed channels" : settings.discordUpdateChannelId(),
                settings.allowedDomains().isEmpty() ? "No domain restrictions configured" : String.join(", ", settings.allowedDomains()),
                settings.defaultScanProfile(),
                settings.defaultScanConfiguration(),
                discordService.status().connected() ? "Ready to accept commands and button actions" : "Unavailable until the bot connects",
                statusService.recentActivity().isEmpty() ? "No recent activity recorded"
                        : String.join(System.lineSeparator(), statusService.recentActivity().stream().limit(5).toList())
        ));
        summaryArea.setCaretPosition(0);
    }

    private static JPanel statPair(String title, JLabel value) {
        JPanel p = new JPanel(new GridLayout(2, 1, 0, 2));
        p.setOpaque(false);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(UiStyle.FG_LABEL);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.PLAIN, 10f));
        p.add(titleLabel);
        p.add(value);
        return p;
    }

    private static JLabel valueLabel() {
        JLabel lbl = new JLabel("-");
        lbl.setForeground(UiStyle.FG_PRIMARY);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 12f));
        return lbl;
    }
}
