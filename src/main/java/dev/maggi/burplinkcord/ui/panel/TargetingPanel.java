package dev.maggi.burplinkcord.ui.panel;

import dev.maggi.burplinkcord.config.RuntimeSettings;
import dev.maggi.burplinkcord.domain.service.SettingsService;
import dev.maggi.burplinkcord.logging.AuditLogger;
import dev.maggi.burplinkcord.ui.style.UiStyle;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class TargetingPanel extends JPanel {

    private final SettingsService settingsService;
    private final AuditLogger auditLogger;
    private final JTextArea domainsArea;
    private final JTextField profileField;
    private final JTextField configurationField;
    private final JLabel statusLabel;

    public TargetingPanel(SettingsService settingsService, AuditLogger auditLogger) {
        super(new BorderLayout(12, 12));
        setBackground(UiStyle.BG_PANEL);
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        this.settingsService = Objects.requireNonNull(settingsService, "settingsService");
        this.auditLogger = Objects.requireNonNull(auditLogger, "auditLogger");

        domainsArea        = new JTextArea(10, 40);
        profileField       = new JTextField();
        configurationField = new JTextField();
        statusLabel        = new JLabel();
        statusLabel.setForeground(UiStyle.FG_MUTED);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));

        UiStyle.styleField(profileField);
        UiStyle.styleField(configurationField);
        UiStyle.styleArea(domainsArea);

        add(buildPrefsCard(), BorderLayout.NORTH);
        add(buildDomainsCard(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
        loadFromSettings();
    }

    public final void refresh() {
        // nothing to auto-refresh on this settings panel
    }

    private void loadFromSettings() {
        RuntimeSettings s = settingsService.getSettings();
        domainsArea.setText(String.join(System.lineSeparator(), s.allowedDomains()));
        profileField.setText(s.defaultScanProfile());
        configurationField.setText(s.defaultScanConfiguration());
        statusLabel.setText("Extension scan settings loaded");
    }

    private JPanel buildPrefsCard() {
        JPanel card = UiStyle.darkCard(new GridLayout(0, 1, 0, 6));
        card.add(UiStyle.createHeader("Default Scan Behavior", "defaults"));
        card.add(UiStyle.fieldLabel("Default Profile"));
        card.add(profileField);
        card.add(UiStyle.fieldLabel("Default Configuration"));
        card.add(configurationField);
        return card;
    }

    private JPanel buildDomainsCard() {
        JPanel card = UiStyle.darkCard(new BorderLayout(0, 6));
        card.add(UiStyle.createHeader("Allowed Domains For Bot and API Control", "scope"), BorderLayout.NORTH);
        card.add(UiStyle.darkScroll(domainsArea), BorderLayout.CENTER);
        return card;
    }

    private JPanel buildFooter() {
        JButton saveButton = UiStyle.primaryButton("Save Settings", "save");
        saveButton.addActionListener(e -> saveSettings());

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(statusLabel, BorderLayout.CENTER);
        footer.add(UiStyle.rightAligned(saveButton), BorderLayout.EAST);
        return footer;
    }

    private void saveSettings() {
        RuntimeSettings current = settingsService.getSettings();
        RuntimeSettings updated = new RuntimeSettings(
                current.discordIntegrationEnabled(),
                current.autostartEnabled(),
                current.discordGuildId(),
                current.whitelistedDiscordIds(),
                current.allowedDiscordChannelIds(),
                current.discordUpdateChannelId(),
                parseLines(domainsArea.getText()),
                profileField.getText().trim(),
                configurationField.getText().trim()
        );
        settingsService.saveSettings(updated);
        auditLogger.log("settings.targeting.save");
        statusLabel.setText("Extension scan settings saved");
    }

    private List<String> parseLines(String value) {
        return Arrays.stream(value.split("\\R"))
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
    }
}
