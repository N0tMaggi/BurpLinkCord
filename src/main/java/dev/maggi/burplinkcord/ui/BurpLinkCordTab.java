package dev.maggi.burplinkcord.ui;

import dev.maggi.burplinkcord.config.ApplicationConfiguration;
import dev.maggi.burplinkcord.domain.service.DiscordService;
import dev.maggi.burplinkcord.domain.service.ScanService;
import dev.maggi.burplinkcord.domain.service.SettingsService;
import dev.maggi.burplinkcord.domain.service.StatusService;
import dev.maggi.burplinkcord.logging.AuditLogger;
import dev.maggi.burplinkcord.ui.panel.ActivityPanel;
import dev.maggi.burplinkcord.ui.panel.DiscordSettingsPanel;
import dev.maggi.burplinkcord.ui.panel.OverviewPanel;
import dev.maggi.burplinkcord.ui.panel.ScanControlPanel;
import dev.maggi.burplinkcord.ui.panel.TargetingPanel;
import dev.maggi.burplinkcord.ui.style.UiStyle;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.Timer;
import javax.swing.JToolBar;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.Objects;

public class BurpLinkCordTab extends JPanel {

    private final OverviewPanel overviewPanel;
    private final DiscordSettingsPanel discordSettingsPanel;
    private final ScanControlPanel scanControlPanel;
    private final TargetingPanel targetingPanel;
    private final ActivityPanel activityPanel;
    private final Timer autoRefreshTimer;

    public BurpLinkCordTab(
            ApplicationConfiguration applicationConfiguration,
            SettingsService settingsService,
            ScanService scanService,
            StatusService statusService,
            DiscordService discordService,
            AuditLogger auditLogger
    ) {
        super(new BorderLayout());
        Objects.requireNonNull(applicationConfiguration, "applicationConfiguration");

        setBackground(UiStyle.BG_BASE);
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        this.overviewPanel = new OverviewPanel(applicationConfiguration, settingsService, statusService, scanService, discordService);
        this.discordSettingsPanel = new DiscordSettingsPanel(settingsService, statusService, discordService, auditLogger);
        this.scanControlPanel = new ScanControlPanel(scanService, settingsService, statusService, auditLogger);
        this.targetingPanel = new TargetingPanel(settingsService, auditLogger);
        this.activityPanel = new ActivityPanel(statusService);
        this.autoRefreshTimer = new Timer(3000, event -> refreshView());
        this.autoRefreshTimer.setRepeats(true);
        this.autoRefreshTimer.start();

        JPanel header = new JPanel(new BorderLayout(8, 4));
        header.setOpaque(false);
        header.add(buildToolbar(), BorderLayout.NORTH);
        header.add(buildSubheader(), BorderLayout.SOUTH);

        add(header, BorderLayout.NORTH);
        add(buildTabbedContent(), BorderLayout.CENTER);
    }

    public final void refreshView() {
        overviewPanel.refresh();
        discordSettingsPanel.refresh();
        scanControlPanel.refresh();
        targetingPanel.refresh();
        activityPanel.refresh();
    }

    public void shutdown() {
        autoRefreshTimer.stop();
    }

    private JToolBar buildToolbar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setOpaque(false);
        toolBar.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));

        JButton refreshButton = UiStyle.actionButton("Refresh", "refresh");
        refreshButton.addActionListener(event -> refreshView());
        toolBar.add(refreshButton);
        return toolBar;
    }

    private JPanel buildSubheader() {
        JPanel subheader = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        subheader.setOpaque(false);
        JLabel info = new JLabel("Integration control for runtime status, Discord, queue operations, and activity.");
        info.setForeground(UiStyle.FG_MUTED);
        info.setFont(info.getFont().deriveFont(Font.PLAIN, 11f));
        info.setBorder(BorderFactory.createEmptyBorder(0, 2, 8, 2));
        subheader.add(info);
        return subheader;
    }

    private JTabbedPane buildTabbedContent() {
        JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        tabs.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        tabs.setBackground(UiStyle.BG_PANEL);
        tabs.setForeground(UiStyle.FG_PRIMARY);
        tabs.addTab("Status",   UiStyle.readIcon("status"),   overviewPanel);
        tabs.addTab("Discord",  UiStyle.readIcon("discord"),  discordSettingsPanel);
        tabs.addTab("Queues",   UiStyle.readIcon("queues"),   scanControlPanel);
        tabs.addTab("Settings", UiStyle.readIcon("settings"), targetingPanel);
        tabs.addTab("Activity", UiStyle.readIcon("activity"), activityPanel);
        return tabs;
    }
}
