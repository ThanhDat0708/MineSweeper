package view;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Bộ đếm kiểu LED 7 đoạn tự vẽ. Dùng cho số mìn còn lại và thời gian.
 * Hỗ trợ số âm (ví dụ bộ đếm mìn có thể xuống dưới 0).
 */
public final class LedCounter extends JPanel {

    private static final long serialVersionUID = 1L;

    /** Bật/tắt 7 đoạn theo thứ tự a,b,c,d,e,f,g. */
    private static final int[][] DIGITS = {
            {1, 1, 1, 1, 1, 1, 0},
            {0, 1, 1, 0, 0, 0, 0},
            {1, 1, 0, 1, 1, 0, 1},
            {1, 1, 1, 1, 0, 0, 1},
            {0, 1, 1, 0, 0, 1, 1},
            {1, 0, 1, 1, 0, 1, 1},
            {1, 0, 1, 1, 1, 1, 1},
            {1, 1, 1, 0, 0, 0, 0},
            {1, 1, 1, 1, 1, 1, 1},
            {1, 1, 1, 1, 0, 1, 1}
    };

    private final ThemeManager theme;
    private final String caption;
    private int value;

    public LedCounter(String caption, ThemeManager theme) {
        this.caption = (caption != null) ? caption : "";
        this.theme = theme;
        setOpaque(false);
        setPreferredSize(new Dimension(108, 54));
        setMinimumSize(getPreferredSize());
    }

    public void setValue(int value) {
        int clamped = Math.max(-999, Math.min(999, value));
        if (clamped != this.value) {
            this.value = clamped;
            repaint();
        }
    }

    public int getValue() {
        return value;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            ThemeManager.Palette p = theme.getPalette();
            int w = getWidth();
            int h = getHeight();
            int arc = 12;

            g2.setColor(new Color(0, 0, 0, 80));
            g2.fill(new RoundRectangle2D.Float(1, 2, w - 2, h - 3, arc, arc));
            g2.setPaint(new GradientPaint(0, 0, p.ledBackground().brighter(),
                    0, h, p.ledBackground()));
            g2.fill(new RoundRectangle2D.Float(0, 0, w - 2, h - 3, arc, arc));
            g2.setColor(new Color(p.ledOff().getRed(), p.ledOff().getGreen(),
                    p.ledOff().getBlue(), 150));
            g2.setStroke(new BasicStroke(1f));
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 3, h - 4, arc, arc));

            g2.setFont(ThemeManager.font(Font.BOLD, 9));
            g2.setColor(p.textMuted());
            FontMetrics cfm = g2.getFontMetrics();
            g2.drawString(caption, (w - 2 - cfm.stringWidth(caption)) / 2, 13);

            String text = String.format("%03d", value);
            int slots = 4;
            int digitWidth = 11;
            int digitHeight = 24;
            int gap = 3;
            int totalWidth = slots * digitWidth + (slots - 1) * gap;
            int startX = (w - 2 - totalWidth) / 2;
            int startY = 20;

            Color on = p.ledOn();
            Color off = p.ledOff();
            int offset = slots - text.length();
            for (int i = 0; i < slots; i++) {
                int charIndex = i - offset;
                int x = startX + i * (digitWidth + gap);
                if (charIndex < 0 || charIndex >= text.length()) {
                    drawDigit(g2, ' ', x, startY, digitWidth, digitHeight, on, off);
                } else {
                    drawDigit(g2, text.charAt(charIndex), x, startY,
                            digitWidth, digitHeight, on, off);
                }
            }
        } finally {
            g2.dispose();
        }
    }

    private static void drawDigit(Graphics2D g2, char c, int x, int y,
                                  int width, int height, Color on, Color off) {
        boolean[] segments;
        if (c >= '0' && c <= '9') {
            int index = c - '0';
            segments = new boolean[7];
            for (int i = 0; i < 7; i++) {
                segments[i] = DIGITS[index][i] == 1;
            }
        } else if (c == '-') {
            segments = new boolean[]{false, false, false, false, false, false, true};
        } else {
            segments = new boolean[7];
        }

        // Vẽ các đoạn tắt mờ trước, sau đó đoạn sáng đè lên.
        for (int i = 0; i < 7; i++) {
            if (!segments[i]) {
                drawSegment(g2, i, x, y, width, height,
                        new Color(off.getRed(), off.getGreen(), off.getBlue(), 110), 2.4f);
            }
        }
        for (int i = 0; i < 7; i++) {
            if (segments[i]) {
                drawSegment(g2, i, x, y, width, height,
                        new Color(on.getRed(), on.getGreen(), on.getBlue(), 55), 5.6f);
                drawSegment(g2, i, x, y, width, height, on, 2.4f);
            }
        }
    }

    private static void drawSegment(Graphics2D g2, int segment, int x, int y,
                                    int width, int height, Color color, float thickness) {
        float pad = 1.6f;
        float midY = y + height / 2f;
        float left = x + pad;
        float right = x + width - pad;
        Line2D line;
        switch (segment) {
            case 0: // a
                line = new Line2D.Float(left, y, right, y);
                break;
            case 1: // b
                line = new Line2D.Float(x + width, y + pad, x + width, midY - pad);
                break;
            case 2: // c
                line = new Line2D.Float(x + width, midY + pad, x + width, y + height - pad);
                break;
            case 3: // d
                line = new Line2D.Float(left, y + height, right, y + height);
                break;
            case 4: // e
                line = new Line2D.Float(x, midY + pad, x, y + height - pad);
                break;
            case 5: // f
                line = new Line2D.Float(x, y + pad, x, midY - pad);
                break;
            default: // g
                line = new Line2D.Float(left, midY, right, midY);
                break;
        }
        g2.setColor(color);
        g2.setStroke(new BasicStroke(thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(line);
    }
}
