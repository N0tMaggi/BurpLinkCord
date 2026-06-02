package dev.maggi.burplinkcord.ui.style;

import javax.swing.*;
import java.awt.*;

public class ToggleSwitch extends JCheckBox {

    private static final int W   = 42;
    private static final int H   = 22;
    private static final int PAD = 3;

    public ToggleSwitch(String text) {
        super(text);
        setOpaque(false);
        setHorizontalTextPosition(SwingConstants.RIGHT);
        setIconTextGap(10);
        setFocusPainted(false);
        setForeground(UiStyle.FG_PRIMARY);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setIcon(new TrackIcon(false));
        setSelectedIcon(new TrackIcon(true));
        setRolloverIcon(new TrackIcon(false));
        setRolloverSelectedIcon(new TrackIcon(true));
    }

    private static final class TrackIcon implements Icon {

        private final boolean on;

        TrackIcon(boolean on) {
            this.on = on;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(on ? UiStyle.ACCENT : UiStyle.BORDER_INT);
            g2.fillRoundRect(x, y, W, H, H, H);
            g2.setColor(Color.WHITE);
            int ks = H - 2 * PAD;
            int kx = on ? x + W - H + PAD : x + PAD;
            g2.fillOval(kx, y + PAD, ks, ks);
            g2.dispose();
        }

        @Override public int getIconWidth()  { return W; }
        @Override public int getIconHeight() { return H; }
    }
}
