package view;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;

/**
 * Bảng nhân vật bên trái: hiển thị tên, chế độ, màn, số mạng và biểu cảm.
 * Nhân vật đổi nét mặt theo trạng thái thắng/thua/mở ô để tăng cảm giác game.
 */
public final class CharacterPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    public enum Expression {
        NORMAL,
        HAPPY,
        SCARED,
        CRYING,
        WIN
    }

    private final ThemeManager theme;

    private int lives = 3;
    private int maxLives = 3;
    private int level = 1;
    private int maxLevel = 10;
    private String modeName = "Beginner";
    private String playerName = "";
    private Expression expression = Expression.NORMAL;
    private Timer expressionTimer;

    public CharacterPanel(ThemeManager theme) {
        this.theme = theme;
        setOpaque(true);
        setPreferredSize(new Dimension(172, 360));
    }

    public void setHud(int lives, int maxLives, int level, int maxLevel, String modeName) {
        this.lives = Math.max(0, lives);
        this.maxLives = Math.max(1, maxLives);
        this.level = level;
        this.maxLevel = Math.max(1, maxLevel);
        this.modeName = (modeName != null) ? modeName : "";
        repaint();
    }

    public void setPlayerName(String playerName) {
        this.playerName = (playerName != null) ? playerName : "";
        repaint();
    }

    /** Biểu cảm tạm thời, tự trở về NORMAL sau 1,4 giây. */
    public void setExpression(Expression expression) {
        setExpression(expression, 1400);
    }

    /**
     * Đổi biểu cảm. durationMs <= 0 nghĩa là giữ nguyên cho đến lần đổi kế tiếp
     * (dùng cho khóc khi thua hoặc vui khi phá đảo).
     */
    public void setExpression(Expression expression, int durationMs) {
        this.expression = (expression != null) ? expression : Expression.NORMAL;
        if (expressionTimer != null) {
            expressionTimer.stop();
            expressionTimer = null;
        }
        if (this.expression != Expression.NORMAL && durationMs > 0) {
            expressionTimer = new Timer(durationMs, e -> {
                this.expression = Expression.NORMAL;
                repaint();
            });
            expressionTimer.setRepeats(false);
            expressionTimer.start();
        }
        repaint();
    }

    public Expression getExpression() {
        return expression;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            ThemeManager.Palette p = theme.getPalette();
            int w = getWidth();
            int h = getHeight();

            paintBackground(g2, p, w, h);
            paintLabels(g2, p, w);
            paintHearts(g2, p, w);
            drawCharacter(g2, p, w / 2, h - 18);
        } finally {
            g2.dispose();
        }
    }

    private void paintBackground(Graphics2D g2, ThemeManager.Palette p, int w, int h) {
        g2.setPaint(new GradientPaint(0, 0, p.skyTop(), 0, h * 0.70f, p.skyBottom()));
        g2.fillRect(0, 0, w, h);

        g2.setColor(new Color(255, 240, 170, 110));
        g2.fillOval(w - 52, 16, 34, 34);
        g2.setColor(new Color(255, 255, 255, 65));
        g2.fillOval(20, 38, 32, 12);
        g2.fillOval(44, 34, 24, 10);

        int groundY = h - 92;
        g2.setColor(p.hill());
        g2.fillOval(-40, groundY - 42, w / 2 + 70, 90);
        g2.fillOval(w / 2 - 22, groundY - 54, w / 2 + 80, 104);

        g2.setColor(p.ground());
        g2.fillRect(0, groundY, w, h - groundY);
        g2.setColor(p.groundLine());
        g2.fillRect(0, groundY, w, 5);

        g2.setColor(new Color(p.grassHighlight().getRed(), p.grassHighlight().getGreen(),
                p.grassHighlight().getBlue(), 80));
        for (int i = 8; i < w; i += 24) {
            g2.drawLine(i, groundY + 9, i + 3, groundY + 2);
            g2.drawLine(i, groundY + 9, i - 2, groundY + 3);
        }
    }

    private void paintLabels(Graphics2D g2, ThemeManager.Palette p, int w) {
        g2.setFont(ThemeManager.font(Font.BOLD, 13));
        g2.setColor(p.text());
        drawCentered(g2, playerName, w, 20);

        g2.setFont(ThemeManager.font(Font.PLAIN, 11));
        g2.setColor(p.textMuted());
        drawCentered(g2, modeName, w, 38);

        g2.setFont(ThemeManager.font(Font.BOLD, 17));
        g2.setColor(p.text());
        drawCentered(g2, "Màn " + level + "/" + maxLevel, w, 64);
    }

    private void paintHearts(Graphics2D g2, ThemeManager.Palette p, int w) {
        int size = 18;
        int gap = 5;
        int totalWidth = maxLives * size + (maxLives - 1) * gap;
        int x = (w - totalWidth) / 2;
        int y = 76;
        for (int i = 0; i < maxLives; i++) {
            Color color = (i < lives) ? new Color(226, 54, 64) : new Color(120, 130, 125);
            drawHeart(g2, x + i * (size + gap), y, size, color);
        }
    }

    private static void drawHeart(Graphics2D g2, int x, int y, int size, Color color) {
        int r = size / 4;
        g2.setColor(new Color(0, 0, 0, 50));
        g2.fillOval(x + 1, y + 2, 2 * r, 2 * r);
        g2.fillOval(x + 2 * r + 1, y + 2, 2 * r, 2 * r);
        g2.setColor(color);
        g2.fillOval(x, y, 2 * r, 2 * r);
        g2.fillOval(x + 2 * r, y, 2 * r, 2 * r);
        g2.fillPolygon(new int[]{x, x + 4 * r, x + 2 * r},
                new int[]{y + r, y + r, y + 3 * r}, 3);
        g2.setColor(new Color(255, 255, 255, 90));
        g2.fillOval(x + r, y + r / 2, r, r);
    }

    private static void drawCentered(Graphics2D g2, String text, int w, int baseline) {
        if (text == null || text.isEmpty()) {
            return;
        }
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, (w - fm.stringWidth(text)) / 2, baseline);
    }

    private void drawCharacter(Graphics2D g2, ThemeManager.Palette p,
                               int cx, int groundY) {
        boolean excited = expression == Expression.HAPPY || expression == Expression.WIN;
        int bounce = excited ? 4 : 0;
        int base = groundY - bounce;

        // Bóng dưới chân.
        g2.setColor(new Color(0, 0, 0, 60));
        g2.fill(new Ellipse2D.Float(cx - 27, groundY - 7, 54, 13));

        // Chân.
        g2.setColor(new Color(55, 65, 80));
        g2.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(new Line2D.Float(cx - 8, base - 32, cx - 13, base - 1));
        g2.draw(new Line2D.Float(cx + 8, base - 32, cx + 13, base - 1));

        // Thân áo.
        g2.setPaint(new GradientPaint(cx - 20, base - 80,
                new Color(82, 156, 222), cx + 20, base - 34, new Color(34, 96, 170)));
        g2.fillRoundRect(cx - 19, base - 80, 38, 51, 14, 14);
        g2.setColor(new Color(30, 78, 140, 180));
        g2.fillRoundRect(cx - 19, base - 49, 38, 8, 8, 8);
        g2.setColor(new Color(245, 198, 92));
        g2.fillRoundRect(cx - 4, base - 49, 8, 8, 3, 3);

        // Tay đổi tư thế theo biểu cảm.
        g2.setColor(new Color(242, 198, 158));
        g2.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (expression) {
            case WIN:
                g2.draw(new Line2D.Float(cx - 16, base - 68, cx - 37, base - 91));
                g2.draw(new Line2D.Float(cx + 16, base - 68, cx + 37, base - 91));
                g2.setColor(new Color(245, 198, 92));
                g2.fillOval(cx - 42, base - 99, 10, 10);
                g2.fillOval(cx + 32, base - 99, 10, 10);
                break;
            case SCARED:
            case CRYING:
                g2.draw(new Line2D.Float(cx - 16, base - 68, cx - 26, base - 92));
                g2.draw(new Line2D.Float(cx + 16, base - 68, cx + 26, base - 92));
                break;
            case HAPPY:
                g2.draw(new Line2D.Float(cx - 17, base - 66, cx - 32, base - 50));
                g2.draw(new Line2D.Float(cx + 17, base - 66, cx + 32, base - 50));
                break;
            case NORMAL:
            default:
                g2.draw(new Line2D.Float(cx - 17, base - 66, cx - 31, base - 46));
                g2.draw(new Line2D.Float(cx + 17, base - 66, cx + 31, base - 46));
                break;
        }

        // Đầu.
        int headY = base - 114;
        g2.setColor(new Color(246, 206, 172));
        g2.fill(new Ellipse2D.Float(cx - 18, headY, 36, 38));
        g2.setColor(new Color(226, 180, 146));
        g2.fill(new Ellipse2D.Float(cx - 21, headY + 16, 5, 8));
        g2.fill(new Ellipse2D.Float(cx + 16, headY + 16, 5, 8));

        // Mũ thám hiểm.
        g2.setColor(new Color(125, 84, 45));
        g2.fillRoundRect(cx - 16, headY - 15, 32, 21, 10, 10);
        g2.setColor(new Color(151, 104, 58));
        g2.fillRoundRect(cx - 13, headY - 12, 26, 10, 8, 8);
        g2.setColor(new Color(104, 67, 34));
        g2.fillRoundRect(cx - 24, headY - 1, 48, 10, 6, 6);

        drawFace(g2, p, cx, headY);

        if (excited) {
            drawSparkle(g2, p, cx - 42, headY + 6);
            drawSparkle(g2, p, cx + 40, headY + 18);
        }
    }

    private void drawFace(Graphics2D g2, ThemeManager.Palette p, int cx, int headY) {
        int eyeY = headY + 22;
        g2.setColor(new Color(45, 45, 45));

        switch (expression) {
            case HAPPY:
            case WIN:
                g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND));
                g2.drawArc(cx - 12, eyeY, 8, 7, 0, 180);
                g2.drawArc(cx + 4, eyeY, 8, 7, 0, 180);
                g2.setColor(new Color(120, 35, 40));
                g2.fillArc(cx - 8, headY + 29, 16, 10, 180, 180);
                g2.setColor(new Color(238, 120, 130));
                g2.fillOval(cx - 4, headY + 34, 8, 4);
                break;
            case SCARED:
                g2.setColor(Color.WHITE);
                g2.fillOval(cx - 13, eyeY - 2, 10, 11);
                g2.fillOval(cx + 3, eyeY - 2, 10, 11);
                g2.setColor(new Color(45, 45, 45));
                g2.fillOval(cx - 10, eyeY + 2, 4, 5);
                g2.fillOval(cx + 6, eyeY + 2, 4, 5);
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND));
                g2.draw(new Line2D.Float(cx - 13, eyeY - 6, cx - 5, eyeY - 9));
                g2.draw(new Line2D.Float(cx + 13, eyeY - 6, cx + 5, eyeY - 9));
                g2.setColor(new Color(90, 30, 35));
                g2.fillOval(cx - 5, headY + 32, 10, 10);
                g2.setColor(new Color(110, 190, 245));
                g2.fillOval(cx + 14, headY + 12, 6, 9);
                break;
            case CRYING:
                g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND));
                g2.draw(new Line2D.Float(cx - 12, eyeY + 1, cx - 4, eyeY + 7));
                g2.draw(new Line2D.Float(cx + 12, eyeY + 1, cx + 4, eyeY + 7));
                g2.setColor(new Color(100, 190, 245));
                g2.fillOval(cx - 11, eyeY + 8, 5, 11);
                g2.fillOval(cx + 6, eyeY + 8, 5, 11);
                g2.fillOval(cx - 10, eyeY + 18, 3, 7);
                g2.fillOval(cx + 7, eyeY + 18, 3, 7);
                g2.setColor(new Color(90, 30, 35));
                g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND));
                g2.drawArc(cx - 8, headY + 32, 16, 9, 0, 180);
                break;
            case NORMAL:
            default:
                g2.fillOval(cx - 10, eyeY, 4, 5);
                g2.fillOval(cx + 6, eyeY, 4, 5);
                g2.setStroke(new BasicStroke(1.9f, BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND));
                g2.drawArc(cx - 7, headY + 27, 14, 8, 200, 140);
                break;
        }

        if (expression == Expression.HAPPY || expression == Expression.WIN) {
            g2.setColor(new Color(245, 130, 140, 110));
            g2.fillOval(cx - 16, headY + 29, 7, 4);
            g2.fillOval(cx + 9, headY + 29, 7, 4);
        }
    }

    private static void drawSparkle(Graphics2D g2, ThemeManager.Palette p, int x, int y) {
        g2.setColor(p.accentLight());
        g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(new Line2D.Float(x, y - 5, x, y + 5));
        g2.draw(new Line2D.Float(x - 5, y, x + 5, y));
        g2.draw(new Line2D.Float(x - 3, y - 3, x + 3, y + 3));
        g2.draw(new Line2D.Float(x + 3, y - 3, x - 3, y + 3));
    }
}
