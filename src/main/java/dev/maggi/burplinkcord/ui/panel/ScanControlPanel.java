package dev.maggi.burplinkcord.ui.panel;

import dev.maggi.burplinkcord.api.request.StartScanRequest;
import dev.maggi.burplinkcord.config.RuntimeSettings;
import dev.maggi.burplinkcord.domain.model.Scan;
import dev.maggi.burplinkcord.domain.model.ScanStatus;
import dev.maggi.burplinkcord.domain.service.ScanService;
import dev.maggi.burplinkcord.domain.service.SettingsService;
import dev.maggi.burplinkcord.domain.service.StatusService;
import dev.maggi.burplinkcord.logging.AuditLogger;
import dev.maggi.burplinkcord.ui.style.UiStyle;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;
import java.util.Objects;

public class ScanControlPanel extends JPanel {

    private final ScanService scanService;
    private final SettingsService settingsService;
    private final StatusService statusService;
    private final AuditLogger auditLogger;
    private final JTextField targetField;
    private final JTable scanTable;
    private final DefaultTableModel tableModel;
    private final JLabel statusLabel;

    public ScanControlPanel(
            ScanService scanService,
            SettingsService settingsService,
            StatusService statusService,
            AuditLogger auditLogger
    ) {
        super(new BorderLayout(12, 12));
        setBackground(UiStyle.BG_PANEL);
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        this.scanService = Objects.requireNonNull(scanService, "scanService");
        this.settingsService = Objects.requireNonNull(settingsService, "settingsService");
        this.statusService = Objects.requireNonNull(statusService, "statusService");
        this.auditLogger = Objects.requireNonNull(auditLogger, "auditLogger");

        targetField = new JTextField();
        UiStyle.styleField(targetField);

        tableModel = new DefaultTableModel(new Object[]{"ID", "Target", "Status", "Profile", "Configuration", "Updated"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        scanTable = new JTable(tableModel);
        scanTable.setAutoCreateRowSorter(true);
        scanTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UiStyle.styleTable(scanTable);

        statusLabel = new JLabel();
        statusLabel.setForeground(UiStyle.FG_MUTED);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));

        JScrollPane tableScroll = UiStyle.darkScroll(scanTable);

        add(buildFormCard(), BorderLayout.NORTH);
        add(tableScroll, BorderLayout.CENTER);
        add(buildActions(), BorderLayout.SOUTH);
        refresh();
    }

    public final void refresh() {
        tableModel.setRowCount(0);
        List<Scan> scans = visibleScans();
        scans.forEach(scan -> tableModel.addRow(new Object[]{
                scan.id(), scan.target(), scan.status(),
                scan.profileName(), scan.configurationName(), scan.updatedAt()
        }));
        long active = scans.stream().filter(s -> s.status() != ScanStatus.COMPLETED).count();
        statusLabel.setText("Active scans: %d  |  Tracked: %d"
                .formatted(statusService.currentStatus().activeScanCount(), active));
    }

    private JPanel buildFormCard() {
        JPanel card = UiStyle.darkCard(new BorderLayout(8, 8));
        card.add(UiStyle.createHeader("Launch New Scan", "scan"), BorderLayout.NORTH);

        JPanel input = new JPanel(new GridLayout(0, 1, 0, 6));
        input.setOpaque(false);
        input.add(UiStyle.fieldLabel("Target URL"));
        input.add(targetField);
        card.add(input, BorderLayout.CENTER);

        JButton startButton = UiStyle.primaryButton("Start Scan", "start");
        startButton.addActionListener(e -> startScan());
        JPanel action = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 4));
        action.setOpaque(false);
        action.add(startButton);
        card.add(action, BorderLayout.EAST);
        return card;
    }

    private JPanel buildActions() {
        JButton pauseButton  = UiStyle.actionButton("Pause",  "pause");
        JButton resumeButton = UiStyle.actionButton("Resume", "resume");
        JButton stopButton   = UiStyle.dangerButton("Stop",   "stop");
        JButton deleteButton = UiStyle.dangerButton("Delete", "delete");

        pauseButton.addActionListener(e  -> withSelectedScan(scan -> scanService.pauseScan(scan.id())));
        resumeButton.addActionListener(e -> withSelectedScan(scan -> scanService.resumeScan(scan.id())));
        stopButton.addActionListener(e   -> withSelectedScan(scan -> scanService.stopScan(scan.id())));
        deleteButton.addActionListener(e -> withSelectedScan(scan -> scanService.deleteScan(scan.id())));

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(statusLabel, BorderLayout.WEST);
        footer.add(UiStyle.rightAligned(pauseButton, resumeButton, stopButton, deleteButton), BorderLayout.EAST);
        return footer;
    }

    private void startScan() {
        RuntimeSettings settings = settingsService.getSettings();
        StartScanRequest request = new StartScanRequest(
                targetField.getText().trim(),
                settings.allowedDomains(),
                settings.defaultScanProfile(),
                settings.defaultScanConfiguration(),
                true,
                true
        );
        scanService.startScan(request);
        auditLogger.log("ui.scan.start");
        refresh();
    }

    private void withSelectedScan(ScanAction action) {
        int selectedRow = scanTable.getSelectedRow();
        if (selectedRow < 0) {
            statusLabel.setText("Select a scan first");
            return;
        }
        String scanId = String.valueOf(tableModel.getValueAt(selectedRow, 0));
        visibleScans().stream()
                .filter(scan -> scan.id().equals(scanId))
                .findFirst()
                .ifPresentOrElse(scan -> {
                    try {
                        action.execute(scan);
                        auditLogger.log("ui.scan.action scanId=" + scan.id());
                        refresh();
                    } catch (UnsupportedOperationException ex) {
                        statusLabel.setText(ex.getMessage());
                    }
                }, () -> statusLabel.setText("Selected scan no longer exists"));
    }

    private List<Scan> visibleScans() {
        return scanService.getScans().stream()
                .filter(scan -> scan.status() != ScanStatus.DELETED)
                .toList();
    }

    @FunctionalInterface
    private interface ScanAction {
        void execute(Scan scan);
    }
}
