package view;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Bảng bên trái hiển thị nhân vật nhà thám hiểm, số mạng và màn hiện tại.
 * Toàn bộ hình ảnh được vẽ bằng Graphics2D, không dùng ảnh/font icon.
 */
public class CharacterPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private int lives = 3;
    private int maxLives = 3;
    private int level = 1;
    private int maxLevel = 10;
    private String modeName = "Beginner";
    private String playerName = "";

    public CharacterPanel() {
        setOpaque(true);
        setPreferredSize(new Dimension(160, 320));
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

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();

            paintBackground(g2, w, h);
            paintLabels(g2, w);
            paintHearts(g2, w);
            drawCharacter(g2, w / 2, h - 20);
        } finally {
            g2.dispose();
        }
    }

    private void paintBackground(Graphics2D g2, int w, int h) {
        GradientPaint sky = new GradientPaint(
                0, 0, new Color(168, 216, 238),
                0, h * 0.72f, new Color(238, 248, 255));
        g2.setPaint(sky);
        g2.fillRect(0, 0, w, h);

        // Đồi xa.
        g2.setColor(new Color(150, 200, 140));
        g2.fillOval(-40, h - 150, w / 2 + 60, 120);
        g2.fillOval(w / 2 - 20, h - 165, w / 2 + 70, 135);

        // Mặt đất.
        int groundY = h - 92;
        g2.setColor(new Color(110, 175, 85));
        g2.fillRect(0, groundY, w, h - groundY);
        g2.setColor(new Color(92, 150, 70));
        g2.fillRect(0, groundY, w, 5);
    }

    private void paintLabels(Graphics2D g2, int w) {
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        g2.setColor(new Color(30, 70, 100));
        drawCentered(g2, playerName, w, 20);

        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        g2.setColor(new Color(55, 90, 110));
        drawCentered(g2, modeName, w, 39);

        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
        g2.setColor(new Color(30, 70, 100));
        drawCentered(g2, "Màn " + level + "/" + maxLevel, w, 64);
    }

    private void paintHearts(Graphics2D g2, int w) {
        int size = 18;
        int gap = 5;
        int totalWidth = maxLives * size + (maxLives - 1) * gap;
        int x = (w - totalWidth) / 2;
        int y = 76;
        for (int i = 0; i < maxLives; i++) {
            Color color = (i < lives) ? new Color(220, 45, 60) : new Color(150, 150, 150);
            drawHeart(g2, x + i * (size + gap), y, size, color);
        }
    }

    private static void drawHeart(Graphics2D g2, int x, int y, int size, Color color) {
        int r = size / 4;
        g2.setColor(color);
        g2.fillOval(x, y, 2 * r, 2 * r);
        g2.fillOval(x + 2 * r, y, 2 * r, 2 * r);
        g2.fillPolygon(
                new int[]{x, x + 4 * r, x + 2 * r},
                new int[]{y + r, y + r, y + 3 * r},
                3);
    }

    private static void drawCentered(Graphics2D g2, String text, int w, int baseline) {
        int x = (w - g2.getFontMetrics().stringWidth(text)) / 2;
        g2.drawString(text, x, baseline);
    }

    private void drawCharacter(Graphics2D g2, int cx, int groundY) {
        // Chân.
        g2.setColor(new Color(55, 65, 80));
        g2.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(cx - 8, groundY - 32, cx - 13, groundY - 2);
        g2.drawLine(cx + 8, groundY - 32, cx + 13, groundY - 2);

        // Thân áo.
        g2.setColor(new Color(45, 120, 200));
        g2.fillRoundRect(cx - 19, groundY - 78, 38, 50, 14, 14);

        // Tay.
        g2.setColor(new Color(242, 200, 160));
        g2.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(cx - 17, groundY - 66, cx - 31, groundY - 46);
        g2.drawLine(cx + 17, groundY - 66, cx + 31, groundY - 46);

        // Đầu.
        int headY = groundY - 112;
        g2.setColor(new Color(246, 206, 172));
        g2.fillOval(cx - 17, headY, 34, 36);

        // Mũ thám hiểm.
        g2.setColor(new Color(125, 84, 45));
        g2.fillRoundRect(cx - 15, headY - 14, 30, 20, 10, 10);
        g2.fillOval(cx - 23, headY - 2, 46, 11);

        // Mắt và miệng.
        g2.setColor(new Color(45, 45, 45));
        g2.fillOval(cx - 9, headY + 24, 4, 4);
        g2.fillOval(cx + 5, headY + 24, 4, 4);
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawArc(cx - 7, headY + 26, 14, 8, 200, 140);
    }
}
