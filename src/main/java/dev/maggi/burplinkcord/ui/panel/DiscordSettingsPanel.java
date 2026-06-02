package dev.maggi.burplinkcord.ui.panel;

import dev.maggi.burplinkcord.config.RuntimeSettings;
import dev.maggi.burplinkcord.domain.service.DiscordService;
import dev.maggi.burplinkcord.domain.service.InMemoryStatusService;
import dev.maggi.burplinkcord.domain.service.SettingsService;
import dev.maggi.burplinkcord.domain.service.StatusService;
import dev.maggi.burplinkcord.logging.AuditLogger;
import dev.maggi.burplinkcord.ui.style.ToggleSwitch;
import dev.maggi.burplinkcord.ui.style.UiStyle;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class DiscordSettingsPanel extends JPanel {

    private final SettingsService settingsService;
    private final StatusService statusService;
    private final DiscordService discordService;
    private final AuditLogger auditLogger;
    private final ToggleSwitch enabledToggle;
    private final ToggleSwitch autostartToggle;
    private final JPasswordField botTokenField;
    private final JPasswordField apiSecretField;
    private final JTextField guildIdField;
    private final JTextField updateChannelField;
    private final JTextArea whitelistArea;
    private final JTextArea channelArea;
    private final JLabel statusLabel;

    public DiscordSettingsPanel(SettingsService settingsService, StatusService statusService, DiscordService discordService, AuditLogger auditLogger) {
        super(new BorderLayout(12, 12));
        setBackground(UiStyle.BG_PANEL);
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        this.settingsService = Objects.requireNonNull(settingsService, "settingsService");
        this.statusService = Objects.requireNonNull(statusService, "statusService");
        this.discordService = Objects.requireNonNull(discordService, "discordService");
        this.auditLogger = Objects.requireNonNull(auditLogger, "auditLogger");

        enabledToggle   = new ToggleSwitch("Enable Discord integration");
        autostartToggle = new ToggleSwitch("Autostart integration on extension load");

        botTokenField      = new JPasswordField();
        apiSecretField     = new JPasswordField();
        guildIdField       = new JTextField();
        updateChannelField = new JTextField();
        whitelistArea      = new JTextArea(8, 40);
        channelArea        = new JTextArea(6, 40);
        statusLabel        = new JLabel();
        statusLabel.setForeground(UiStyle.FG_MUTED);

        UiStyle.styleField(guildIdField);
        UiStyle.styleField(updateChannelField);
        UiStyle.styleField(botTokenField);
        UiStyle.styleField(apiSecretField);
        UiStyle.styleArea(whitelistArea);
        UiStyle.styleArea(channelArea);

        add(buildConnectionPanel(), BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(1, 2, 8, 0));
        center.setOpaque(false);
        center.add(buildListCard("Access Control", "access", "Whitelisted Discord IDs", whitelistArea));
        center.add(buildListCard("Command Scope",  "scope",  "Allowed Discord Channel IDs", channelArea));
        add(center, BorderLayout.CENTER);

        add(buildFooter(), BorderLayout.SOUTH);
        loadFromSettings();
    }

    public final void refresh() {
        statusLabel.setText("Discord: " + discordService.status().statusMessage());
    }

    private void loadFromSettings() {
        RuntimeSettings s = settingsService.getSettings();
        enabledToggle.setSelected(s.discordIntegrationEnabled());
        autostartToggle.setSelected(s.autostartEnabled());
        guildIdField.setText(s.discordGuildId());
        updateChannelField.setText(s.discordUpdateChannelId());
        whitelistArea.setText(String.join(System.lineSeparator(), s.whitelistedDiscordIds()));
        channelArea.setText(String.join(System.lineSeparator(), s.allowedDiscordChannelIds()));
        botTokenField.setText(settingsService.loadDiscordBotToken());
        apiSecretField.setText(settingsService.loadApiSharedSecret());
        refresh();
    }

    private JPanel buildConnectionPanel() {
        // Header in NORTH so it doesn't participate in GridBagLayout sizing
        JPanel card = UiStyle.darkCard(new BorderLayout(0, 10));
        card.add(UiStyle.createHeader("Connection Settings", "discord"), BorderLayout.NORTH);

        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 6, 0);

        body.add(enabledToggle, gbc);
        gbc.gridy++;
        body.add(autostartToggle, gbc);

        addFieldRow(body, gbc, "Whitelisted Guild ID", guildIdField);
        addFieldRow(body, gbc, "Update / Log Channel ID", updateChannelField);
        addFieldRow(body, gbc, "Bot Token", botTokenField);
        addFieldRow(body, gbc, "Local API Shared Secret", apiSecretField);

        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildListCard(String title, String iconKey, String header, JTextArea area) {
        JPanel card = UiStyle.darkCard(new BorderLayout(0, 6));
        card.add(UiStyle.createHeader(title, iconKey), BorderLayout.NORTH);
        JPanel inner = new JPanel(new BorderLayout(0, 4));
        inner.setOpaque(false);
        inner.add(UiStyle.fieldLabel(header), BorderLayout.NORTH);
        inner.add(UiStyle.darkScroll(area), BorderLayout.CENTER);
        card.add(inner, BorderLayout.CENTER);
        return card;
    }

    private void addFieldRow(JPanel panel, GridBagConstraints gbc, String label, javax.swing.JComponent field) {
        gbc.gridy++;
        gbc.insets = new Insets(6, 0, 2, 0);
        panel.add(UiStyle.fieldLabel(label), gbc);
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 6, 0);
        panel.add(field, gbc);
    }

    private JPanel buildFooter() {
        JButton saveButton    = UiStyle.primaryButton("Save Settings", "save");
        JButton startButton   = UiStyle.actionButton("Start Bot", "start");
        JButton stopButton    = UiStyle.dangerButton("Stop Bot", "stop");
        JButton publishButton = UiStyle.actionButton("Publish Control Panel", "publish");

        saveButton.addActionListener(e -> saveSettings());
        startButton.addActionListener(e -> startBot());
        stopButton.addActionListener(e -> stopBot());
        publishButton.addActionListener(e -> publishControlPanel());

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));
        footer.add(statusLabel, BorderLayout.CENTER);
        footer.add(UiStyle.rightAligned(startButton, stopButton, publishButton, saveButton), BorderLayout.EAST);
        return footer;
    }

    private void saveSettings() {
        RuntimeSettings current = settingsService.getSettings();
        RuntimeSettings updated = new RuntimeSettings(
                enabledToggle.isSelected(),
                autostartToggle.isSelected(),
                guildIdField.getText().trim(),
                parseLines(whitelistArea.getText()),
                parseLines(channelArea.getText()),
                updateChannelField.getText().trim(),
                current.allowedDomains(),
                current.defaultScanProfile(),
                current.defaultScanConfiguration()
        );
        settingsService.saveSettings(updated);
        settingsService.saveDiscordBotToken(new String(botTokenField.getPassword()));
        settingsService.saveApiSharedSecret(new String(apiSecretField.getPassword()));
        auditLogger.log("settings.discord.save");
        statusLabel.setText("Discord settings saved");
    }

    private void startBot() {
        try {
            discordService.start();
            if (statusService instanceof InMemoryStatusService mutable) {
                mutable.markDiscordConnected(discordService.status().connected());
            }
            statusLabel.setText(discordService.status().statusMessage());
        } catch (RuntimeException ex) {
            statusLabel.setText(ex.getMessage());
        }
    }

    private void stopBot() {
        discordService.stop();
        if (statusService instanceof InMemoryStatusService mutable) {
            mutable.markDiscordConnected(false);
        }
        statusLabel.setText(discordService.status().statusMessage());
    }

    private void publishControlPanel() {
        try {
            discordService.publishControlPanel();
            statusLabel.setText("Discord control panel published");
        } catch (RuntimeException ex) {
            statusLabel.setText(ex.getMessage());
        }
    }

    private List<String> parseLines(String value) {
        return Arrays.stream(value.split("\\R"))
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
    }
}
