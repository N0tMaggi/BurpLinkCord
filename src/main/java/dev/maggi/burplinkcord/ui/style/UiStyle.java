package dev.maggi.burplinkcord.ui.style;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.text.JTextComponent;
import java.awt.*;

public final class UiStyle {

    public static final Color BG_BASE    = new Color(0x1e1e1e);
    public static final Color BG_PANEL   = new Color(0x252525);
    public static final Color BG_CARD    = new Color(0x2d2d2d);
    public static final Color BG_INPUT   = new Color(0x1a1a1a);
    public static final Color BG_ROW_ALT = new Color(0x2a2a2a);
    public static final Color BG_SEL     = new Color(0x3c3c3c);
    public static final Color FG_PRIMARY = new Color(0xe0e0e0);
    public static final Color FG_LABEL   = new Color(0xaaaaaa);
    public static final Color FG_MUTED   = new Color(0x777777);
    public static final Color ACCENT     = new Color(0xFF6633);
    public static final Color ACCENT_HOV = new Color(0xFF7744);
    public static final Color ACCENT_PRE = new Color(0xCC4422);
    public static final Color DANGER     = new Color(0xDC2626);
    public static final Color DANGER_HOV = new Color(0xEF4444);
    public static final Color BORDER     = new Color(0x3a3a3a);
    public static final Color BORDER_INT = new Color(0x484848);

    private static final int ICON_SIZE = 16;

    private UiStyle() {}

    public static JButton primaryButton(String text, String iconKey) {
        return filledButton(text, iconKey, ACCENT, ACCENT_HOV, ACCENT_PRE, 120);
    }

    public static JButton actionButton(String text, String iconKey) {
        return filledButton(text, iconKey, ACCENT, ACCENT_HOV, ACCENT_PRE, 0);
    }

    public static JButton dangerButton(String text, String iconKey) {
        return filledButton(text, iconKey, DANGER, DANGER_HOV, DANGER.darker(), 0);
    }

    private static JButton filledButton(String text, String iconKey, Color base, Color hov, Color pre, int minW) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed() ? pre : getModel().isRollover() ? hov : base);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setForeground(Color.WHITE);
        btn.setIcon(readIcon(iconKey));
        btn.setIconTextGap(6);
        btn.setMargin(new Insets(6, 14, 6, 14));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (minW > 0) {
            btn.setPreferredSize(new Dimension(Math.max(btn.getPreferredSize().width, minW), 32));
        }
        return btn;
    }

    public static void styleField(JTextComponent c) {
        c.setBackground(BG_INPUT);
        c.setForeground(FG_PRIMARY);
        c.setCaretColor(FG_PRIMARY);
        c.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_INT, 1),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
    }

    public static void styleArea(JTextArea a) {
        a.setBackground(BG_INPUT);
        a.setForeground(FG_PRIMARY);
        a.setCaretColor(FG_PRIMARY);
        a.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    }

    public static JLabel createHeader(String text, String iconKey) {
        JLabel lbl = new JLabel(text);
        lbl.setIcon(readIcon(iconKey));
        lbl.setIconTextGap(8);
        lbl.setForeground(FG_PRIMARY);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 13f));
        lbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        return lbl;
    }

    public static JLabel fieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setForeground(FG_LABEL);
        lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 11f));
        lbl.setBorder(BorderFactory.createEmptyBorder(6, 0, 2, 0));
        return lbl;
    }

    public static JPanel createSection(String title, String iconKey, JComponent content) {
        JPanel p = darkCard(new BorderLayout(0, 8));
        p.add(createHeader(title, iconKey), BorderLayout.NORTH);
        p.add(content, BorderLayout.CENTER);
        return p;
    }

    public static JPanel darkCard() {
        JPanel p = new JPanel();
        p.setBackground(BG_CARD);
        p.setOpaque(true);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));
        return p;
    }

    public static JPanel darkCard(LayoutManager layout) {
        JPanel p = darkCard();
        p.setLayout(layout);
        return p;
    }

    public static void styleTable(JTable table) {
        table.setBackground(BG_CARD);
        table.setForeground(FG_PRIMARY);
        table.setGridColor(BORDER);
        table.setRowHeight(26);
        table.setSelectionBackground(BG_SEL);
        table.setSelectionForeground(Color.WHITE);
        table.setShowGrid(true);
        table.setIntercellSpacing(new Dimension(0, 1));
        if (table.getTableHeader() != null) {
            table.getTableHeader().setBackground(BG_BASE);
            table.getTableHeader().setForeground(FG_MUTED);
            table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
            table.getTableHeader().setOpaque(true);
        }
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                setBackground(sel ? BG_SEL : row % 2 == 0 ? BG_CARD : BG_ROW_ALT);
                setForeground(sel ? Color.WHITE : FG_PRIMARY);
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                setOpaque(true);
                return this;
            }
        });
    }

    public static JPanel rightAligned(Component... components) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        p.setOpaque(false);
        for (Component c : components) {
            p.add(c);
        }
        return p;
    }

    public static JScrollPane darkScroll(Component view) {
        JScrollPane sp = new JScrollPane(view);
        sp.setBackground(BG_CARD);
        sp.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        sp.getViewport().setBackground(BG_INPUT);
        sp.getViewport().setOpaque(true);
        return sp;
    }

    public static Icon readIcon(String iconKey) {
        return AppIcon.of(iconKey, ICON_SIZE);
    }
}
