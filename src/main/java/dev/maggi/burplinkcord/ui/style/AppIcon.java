package dev.maggi.burplinkcord.ui.style;

import javax.swing.Icon;
import java.awt.*;
import java.awt.geom.*;

public final class AppIcon implements Icon {

    private final int size;
    private final Color bg;
    private final String type;

    private AppIcon(int size, Color bg, String type) {
        this.size = size;
        this.bg = bg;
        this.type = type;
    }

    public static Icon of(String name, int size) {
        Color bg = switch (name) {
            case "status"                -> new Color(0x2563EB);
            case "discord"               -> new Color(0x5865F2);
            case "queues"                -> new Color(0x475569);
            case "settings", "defaults"  -> new Color(0x7C3AED);
            case "activity", "events"    -> new Color(0x0891B2);
            case "refresh"               -> new Color(0xF97316);
            case "save"                  -> new Color(0x16A34A);
            case "start", "resume"       -> new Color(0x059669);
            case "stop"                  -> new Color(0xDC2626);
            case "publish"               -> new Color(0x2563EB);
            case "pause"                 -> new Color(0xD97706);
            case "delete"                -> new Color(0xDC2626);
            case "scan"                  -> new Color(0xF97316);
            case "notes"                 -> new Color(0x0284C7);
            case "access"                -> new Color(0x6366F1);
            case "scope"                 -> new Color(0x475569);
            default                      -> new Color(0x475569);
        };
        return new AppIcon(size, bg, name);
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            // Colored rounded-square background
            g2.setColor(bg);
            g2.fillRoundRect(x, y, size, size, 5, 5);

            g2.setColor(new Color(255, 255, 255, 230));

            float p  = size * 0.20f;
            float s  = size - 2 * p;
            float cx = x + size / 2f;
            float cy = y + size / 2f;
            float r  = s / 2f;

            switch (type) {
                case "start", "resume" -> drawPlay(g2, x + p, y + p, s);
                case "stop"            -> drawSquare(g2, x + p, y + p, s);
                case "pause"           -> drawPause(g2, x + p, y + p, s);
                case "delete"          -> drawX(g2, x + p, y + p, s);
                case "save"            -> drawCheck(g2, x + p, y + p, s);
                case "status"          -> drawCheck(g2, x + p, y + p, s);
                case "refresh"         -> drawRefresh(g2, cx, cy, r);
                case "publish"         -> drawArrowUp(g2, cx, cy, r);
                case "settings", "defaults" -> drawGear(g2, cx, cy, r);
                case "scan"            -> drawMagnifier(g2, x + p, y + p, s);
                case "queues", "notes" -> drawLines(g2, x + p, y + p, s);
                case "access"          -> drawLock(g2, x + p, y + p, s);
                case "scope"           -> drawCrosshair(g2, cx, cy, r);
                case "activity", "events" -> drawBolt(g2, x + p, y + p, s);
                case "discord"         -> drawBubble(g2, x + p, y + p, s);
                default                -> drawDot(g2, cx, cy, r * 0.5f);
            }
        } finally {
            g2.dispose();
        }
    }

    private static void drawPlay(Graphics2D g, float x, float y, float s) {
        Path2D p = new Path2D.Float();
        p.moveTo(x + s * 0.1f, y);
        p.lineTo(x + s,        y + s / 2f);
        p.lineTo(x + s * 0.1f, y + s);
        p.closePath();
        g.fill(p);
    }

    private static void drawSquare(Graphics2D g, float x, float y, float s) {
        g.fill(new RoundRectangle2D.Float(x, y, s, s, 2, 2));
    }

    private static void drawPause(Graphics2D g, float x, float y, float s) {
        float bw  = s * 0.28f;
        float gap = s * 0.44f;
        g.fill(new RoundRectangle2D.Float(x,            y, bw, s, 2, 2));
        g.fill(new RoundRectangle2D.Float(x + bw + gap, y, bw, s, 2, 2));
    }

    private static void drawX(Graphics2D g, float x, float y, float s) {
        Stroke old = g.getStroke();
        g.setStroke(new BasicStroke(s * 0.22f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Float(x,     y,     x + s, y + s));
        g.draw(new Line2D.Float(x + s, y,     x,     y + s));
        g.setStroke(old);
    }

    private static void drawCheck(Graphics2D g, float x, float y, float s) {
        Stroke old = g.getStroke();
        g.setStroke(new BasicStroke(s * 0.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D p = new Path2D.Float();
        p.moveTo(x,            y + s * 0.50f);
        p.lineTo(x + s * 0.38f, y + s * 0.82f);
        p.lineTo(x + s,          y + s * 0.15f);
        g.draw(p);
        g.setStroke(old);
    }

    private static void drawRefresh(Graphics2D g, float cx, float cy, float r) {
        Stroke old = g.getStroke();
        g.setStroke(new BasicStroke(r * 0.28f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Arc2D.Float(cx - r, cy - r, r * 2, r * 2, 50, -300, Arc2D.OPEN));
        // arrowhead at end of arc (~angle 50°)
        double a = Math.toRadians(50);
        float ax = cx + r * (float) Math.cos(a);
        float ay = cy - r * (float) Math.sin(a);
        float t  = r * 0.42f;
        Path2D head = new Path2D.Float();
        head.moveTo(ax + t * (float) Math.cos(a + 2.3), ay - t * (float) Math.sin(a + 2.3));
        head.lineTo(ax, ay);
        head.lineTo(ax + t * (float) Math.cos(a - 0.8), ay - t * (float) Math.sin(a - 0.8));
        g.draw(head);
        g.setStroke(old);
    }

    private static void drawArrowUp(Graphics2D g, float cx, float cy, float r) {
        Stroke old = g.getStroke();
        g.setStroke(new BasicStroke(r * 0.24f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        float hw = r * 0.7f;
        g.draw(new Line2D.Float(cx, cy - r, cx, cy + r * 0.5f));
        Path2D head = new Path2D.Float();
        head.moveTo(cx - hw / 2f, cy - r * 0.38f);
        head.lineTo(cx, cy - r);
        head.lineTo(cx + hw / 2f, cy - r * 0.38f);
        g.draw(head);
        g.setStroke(old);
    }

    private static void drawGear(Graphics2D g, float cx, float cy, float r) {
        // 6 spokes + center circle, WIND_EVEN_ODD for the center hole
        Stroke old = g.getStroke();
        g.setStroke(new BasicStroke(r * 0.32f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3;
            g.draw(new Line2D.Float(
                    cx + r * 0.22f * (float) Math.cos(a), cy + r * 0.22f * (float) Math.sin(a),
                    cx + r         * (float) Math.cos(a), cy + r         * (float) Math.sin(a)
            ));
        }
        float dotR = r * 0.38f;
        g.fill(new Ellipse2D.Float(cx - dotR, cy - dotR, dotR * 2, dotR * 2));
        g.setStroke(old);
    }

    private static void drawMagnifier(Graphics2D g, float x, float y, float s) {
        Stroke old = g.getStroke();
        float sw = s * 0.18f;
        g.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        float cr   = s * 0.34f;
        float ccx  = x + cr + sw * 0.5f;
        float ccy  = y + cr + sw * 0.5f;
        g.draw(new Ellipse2D.Float(ccx - cr, ccy - cr, cr * 2, cr * 2));
        float diag = (float) (cr / Math.sqrt(2));
        g.draw(new Line2D.Float(ccx + diag, ccy + diag, x + s, y + s));
        g.setStroke(old);
    }

    private static void drawLines(Graphics2D g, float x, float y, float s) {
        float lh  = s * 0.17f;
        float gap = (s - 3 * lh) / 2f;
        g.fill(new RoundRectangle2D.Float(x, y,                   s,        lh, 2, 2));
        g.fill(new RoundRectangle2D.Float(x, y + lh + gap,        s * 0.78f, lh, 2, 2));
        g.fill(new RoundRectangle2D.Float(x, y + 2 * (lh + gap),  s * 0.90f, lh, 2, 2));
    }

    private static void drawLock(Graphics2D g, float x, float y, float s) {
        float bx    = x + s * 0.10f;
        float bw    = s * 0.80f;
        float bodyY = y + s * 0.44f;
        float bodyH = s * 0.52f;
        g.fill(new RoundRectangle2D.Float(bx, bodyY, bw, bodyH, 4, 4));

        Stroke old = g.getStroke();
        float sw = s * 0.19f;
        g.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        float sx   = x + s * 0.24f;
        float sw2  = s * 0.52f;
        float topY = y + s * 0.05f;
        float arcH = (bodyY - topY + sw2 * 0.5f);
        g.draw(new Arc2D.Float(sx, topY, sw2, arcH, 0, 180, Arc2D.OPEN));
        g.setStroke(old);
    }

    private static void drawCrosshair(Graphics2D g, float cx, float cy, float r) {
        Stroke old = g.getStroke();
        float sw  = r * 0.22f;
        float cr  = r * 0.62f;
        float gap = cr + sw * 0.4f;
        g.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Ellipse2D.Float(cx - cr, cy - cr, cr * 2, cr * 2));
        g.draw(new Line2D.Float(cx - r,   cy, cx - gap, cy));
        g.draw(new Line2D.Float(cx + gap, cy, cx + r,   cy));
        g.draw(new Line2D.Float(cx, cy - r,   cx, cy - gap));
        g.draw(new Line2D.Float(cx, cy + gap, cx, cy + r));
        g.setStroke(old);
    }

    private static void drawBolt(Graphics2D g, float x, float y, float s) {
        Path2D bolt = new Path2D.Float();
        bolt.moveTo(x + s * 0.62f, y);
        bolt.lineTo(x + s * 0.18f, y + s * 0.52f);
        bolt.lineTo(x + s * 0.50f, y + s * 0.48f);
        bolt.lineTo(x + s * 0.38f, y + s);
        bolt.lineTo(x + s * 0.82f, y + s * 0.48f);
        bolt.lineTo(x + s * 0.50f, y + s * 0.52f);
        bolt.closePath();
        g.fill(bolt);
    }

    private static void drawBubble(Graphics2D g, float x, float y, float s) {
        float bh = s * 0.76f;
        g.fill(new RoundRectangle2D.Float(x, y, s, bh, s * 0.28f, s * 0.28f));
        Path2D tail = new Path2D.Float();
        tail.moveTo(x + s * 0.16f, y + bh - 1);
        tail.lineTo(x,             y + s);
        tail.lineTo(x + s * 0.42f, y + bh - 1);
        tail.closePath();
        g.fill(tail);
    }

    private static void drawDot(Graphics2D g, float cx, float cy, float r) {
        g.fill(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
    }

    @Override public int getIconWidth()  { return size; }
    @Override public int getIconHeight() { return size; }
}
