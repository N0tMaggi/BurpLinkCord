package dev.maggi.burplinkcord.ui.panel;

import dev.maggi.burplinkcord.domain.service.StatusService;
import dev.maggi.burplinkcord.ui.style.UiStyle;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Font;
import java.util.Objects;

public class ActivityPanel extends JPanel {

    private final StatusService statusService;
    private final JTextArea activityArea;

    public ActivityPanel(StatusService statusService) {
        super(new BorderLayout(12, 12));
        setBackground(UiStyle.BG_PANEL);
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        this.statusService = Objects.requireNonNull(statusService, "statusService");

        this.activityArea = new JTextArea();
        this.activityArea.setEditable(false);
        this.activityArea.setLineWrap(true);
        this.activityArea.setWrapStyleWord(true);
        this.activityArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        UiStyle.styleArea(activityArea);

        JPanel card = UiStyle.darkCard(new BorderLayout(0, 8));
        card.add(UiStyle.createHeader("Event Stream", "events"), BorderLayout.NORTH);
        card.add(UiStyle.darkScroll(activityArea), BorderLayout.CENTER);

        add(card, BorderLayout.CENTER);
        refresh();
    }

    public final void refresh() {
        activityArea.setText(String.join(System.lineSeparator(), statusService.recentActivity()));
    }
}
