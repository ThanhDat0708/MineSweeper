package view;

import javax.swing.JButton;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/**
 * Nút bo góc tự vẽ, đổi màu theo {@link ThemeManager.Palette}.
 * Có hiệu ứng nổi, hover và nhấn xuống nhưng vẫn giữ nguyên hành vi JButton.
 */
public final class RoundedButton extends JButton {

    private static final long serialVersionUID = 1L;

    public enum Variant {
        PRIMARY,
        SECONDARY,
        DANGER
    }

    private final ThemeManager theme;
    private final Variant variant;

    public RoundedButton(String text, ThemeManager theme, Variant variant) {
        super(text);
        this.theme = theme;
        this.variant = (variant != null) ? variant : Variant.SECONDARY;
        setFont(ThemeManager.font(Font.BOLD, 13));
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        int width = Math.max(84, fm.stringWidth(getText()) + 28);
        return new Dimension(width, 32);
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
            boolean hover = getModel().isRollover();
            boolean pressed = getModel().isPressed() && getModel().isArmed();
            boolean enabled = isEnabled();

            Color base;
            Color text;
            switch (variant) {
                case PRIMARY:
                    base = hover ? brighten(p.accent(), 0.12) : p.accent();
                    text = darken(p.appBackground(), 0.15);
                    break;
                case DANGER:
                    base = hover ? brighten(p.flagRed(), 0.1) : p.flagRed();
                    text = Color.WHITE;
                    break;
                case SECONDARY:
                default:
                    base = hover ? p.buttonHover() : p.button();
                    text = p.buttonText();
                    break;
            }
            if (pressed) {
                base = darken(base, 0.16);
            }
            if (!enabled) {
                base = desaturate(base);
                text = p.textMuted();
            }

            int w = getWidth();
            int h = getHeight();
            int arc = 14;
            int shadow = 3;

            // Bóng đổ phía dưới tạo cảm giác khối nổi.
            g2.setColor(new Color(0, 0, 0, 70));
            g2.fill(new RoundRectangle2D.Float(1, shadow, w - 2, h - shadow - 1, arc, arc));

            int y = pressed ? 1 : 0;
            RoundRectangle2D shape = new RoundRectangle2D.Float(
                    1, y + 1, w - 2, h - shadow - 1, arc, arc);

            g2.setPaint(new GradientPaint(0, y + 1, brighten(base, 0.18),
                    0, h - shadow, base));
            g2.fill(shape);

            // Viền sáng phía trên và viền tối phía dưới.
            g2.setStroke(new BasicStroke(1.2f));
            g2.setColor(new Color(255, 255, 255, hover ? 100 : 65));
            g2.draw(new RoundRectangle2D.Float(2.2f, y + 2.2f, w - 4.4f,
                    h - shadow - 3.4f, arc - 2, arc - 2));
            g2.setColor(new Color(0, 0, 0, 55));
            g2.draw(shape);

            if (hasFocus()) {
                g2.setColor(new Color(p.accentLight().getRed(), p.accentLight().getGreen(),
                        p.accentLight().getBlue(), 170));
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new RoundRectangle2D.Float(0.8f, y + 0.8f, w - 1.6f,
                        h - shadow - 0.6f, arc, arc));
            }

            String label = getText();
            if (label != null && !label.isEmpty()) {
                g2.setFont(getFont());
                g2.setColor(text);
                FontMetrics fm = g2.getFontMetrics();
                int tx = (w - fm.stringWidth(label)) / 2;
                int ty = y + (h - shadow - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(label, tx, ty);
            }
        } finally {
            g2.dispose();
        }
    }

    private static Color brighten(Color color, double amount) {
        int r = (int) Math.round(color.getRed() + (255 - color.getRed()) * amount);
        int g = (int) Math.round(color.getGreen() + (255 - color.getGreen()) * amount);
        int b = (int) Math.round(color.getBlue() + (255 - color.getBlue()) * amount);
        return new Color(clamp(r), clamp(g), clamp(b), color.getAlpha());
    }

    private static Color darken(Color color, double amount) {
        int r = (int) Math.round(color.getRed() * (1.0 - amount));
        int g = (int) Math.round(color.getGreen() * (1.0 - amount));
        int b = (int) Math.round(color.getBlue() * (1.0 - amount));
        return new Color(clamp(r), clamp(g), clamp(b), color.getAlpha());
    }

    private static Color desaturate(Color color) {
        int gray = (int) (color.getRed() * 0.3 + color.getGreen() * 0.59 + color.getBlue() * 0.11);
        return new Color(gray, gray, gray, color.getAlpha());
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
