package view;

import javax.swing.JButton;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

/**
 * Một ô của khu rừng: ô ẩn là cỏ cây, ô mở là đất trống kèm số bom kề (1-8).
 * Bom, cờ, bom đã gỡ và bom phát nổ đều được vẽ bằng Graphics2D.
 */
public class CellButton extends JButton {

    private static final long serialVersionUID = 1L;

    /** Trạng thái hiển thị do tầng view tự định nghĩa, không phụ thuộc model. */
    public enum State {
        HIDDEN,
        REVEALED,
        FLAGGED
    }

    public static final int CELL_SIZE = 32;

    private static final Color GRASS_A = new Color(96, 163, 74);
    private static final Color GRASS_B = new Color(88, 153, 66);
    private static final Color GRASS_DARK = new Color(70, 130, 55);
    private static final Color DIRT_A = new Color(205, 180, 135);
    private static final Color DIRT_B = new Color(191, 165, 121);
    private static final Color DIRT_DARK = new Color(170, 145, 103);
    private static final Color EXPLODED_FILL = new Color(214, 69, 65);
    private static final Color FLAG_RED = new Color(220, 30, 30);

    /** Màu số 1-8 theo quy ước Minesweeper (theo chỉ số adjacent - 1). */
    private static final Color[] NUMBER_COLORS = {
            new Color(30, 80, 220),    // 1 xanh dương
            new Color(30, 140, 50),    // 2 xanh lá
            new Color(210, 40, 40),    // 3 đỏ
            new Color(20, 30, 110),    // 4 xanh navy
            new Color(130, 55, 35),    // 5 nâu đỏ
            new Color(20, 140, 140),   // 6 teal
            Color.BLACK,               // 7 đen
            new Color(100, 100, 100)   // 8 xám
    };

    private final int row;
    private final int col;

    private State state = State.HIDDEN;
    private boolean mine;
    private boolean exploded;
    private boolean wrongFlag;
    private boolean defused;
    private int adjacent;

    public CellButton(int row, int col) {
        this.row = row;
        this.col = col;
        Dimension size = new Dimension(CELL_SIZE, CELL_SIZE);
        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);
        setFocusable(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(true);
        setMargin(new Insets(0, 0, 0, 0));
        setBorder(null);
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public void setCellData(State state, boolean mine, boolean exploded,
                            boolean wrongFlag, boolean defused, int adjacent) {
        this.state = state;
        this.mine = mine;
        this.exploded = exploded;
        this.wrongFlag = wrongFlag;
        this.defused = defused;
        this.adjacent = Math.max(0, Math.min(8, adjacent));
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            if (state == State.REVEALED) {
                paintRevealed(g2, w, h);
            } else {
                paintHidden(g2, w, h);
            }
        } finally {
            g2.dispose();
        }
    }

    private void paintHidden(Graphics2D g2, int w, int h) {
        g2.setColor(((row + col) % 2 == 0) ? GRASS_A : GRASS_B);
        g2.fillRect(0, 0, w, h);
        drawGrassTexture(g2, w, h);

        if (state == State.FLAGGED) {
            if (wrongFlag) {
                drawBomb(g2, w, h, Color.BLACK, new Color(255, 140, 0));
                drawRedCross(g2, w, h);
            } else {
                drawFlag(g2, w, h);
            }
        } else if (mine) {
            // Bom còn ẩn chỉ được vẽ khi ván đã kết thúc.
            drawBomb(g2, w, h, Color.BLACK, new Color(255, 160, 0));
        }
    }

    private void paintRevealed(Graphics2D g2, int w, int h) {
        g2.setColor(exploded ? EXPLODED_FILL
                : (((row + col) % 2 == 0) ? DIRT_A : DIRT_B));
        g2.fillRect(0, 0, w, h);
        g2.setColor(exploded ? new Color(160, 40, 40) : DIRT_DARK);
        g2.drawRect(0, 0, w - 1, h - 1);

        if (mine) {
            if (defused) {
                drawDefusedBomb(g2, w, h);
            } else if (exploded) {
                drawBurst(g2, w, h);
                drawBomb(g2, w, h, Color.BLACK, new Color(255, 90, 0));
            } else {
                drawBomb(g2, w, h, Color.BLACK, new Color(255, 160, 0));
            }
        } else {
            drawDirtTexture(g2, w, h);
            drawAdjacentNumber(g2, w, h);
        }
    }

    /** Vẽ số bom kề trên ô đất trống để người chơi suy luận vị trí bom. */
    private void drawAdjacentNumber(Graphics2D g2, int w, int h) {
        if (adjacent <= 0) {
            return;
        }
        String text = String.valueOf(adjacent);
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
        g2.setColor(NUMBER_COLORS[Math.min(adjacent, 8) - 1]);
        FontMetrics fm = g2.getFontMetrics();
        int x = (w - fm.stringWidth(text)) / 2;
        int y = (h - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(text, x, y);
    }

    private void drawGrassTexture(Graphics2D g2, int w, int h) {
        int seed = mixSeed(row, col);
        g2.setColor(GRASS_DARK);
        g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 5; i++) {
            int x = 4 + Math.floorMod(seed + i * 13, Math.max(1, w - 8));
            int y = h - 4 - Math.floorMod(seed * 3 + i * 7, 6);
            g2.drawLine(x, y, x - 2, y - 6);
            g2.drawLine(x, y, x + 2, y - 7);
        }

        int kind = Math.floorMod(seed, 12);
        if (kind < 2) {
            drawTree(g2, w, h);
        } else if (kind < 4) {
            drawBush(g2, w, h);
        } else if (kind == 4) {
            drawFlower(g2, w, h);
        } else if (kind < 7) {
            drawRock(g2, w, h);
        }
    }

    private void drawDirtTexture(Graphics2D g2, int w, int h) {
        int seed = mixSeed(col, row);
        g2.setColor(new Color(176, 150, 108));
        for (int i = 0; i < 4; i++) {
            int x = 4 + Math.floorMod(seed + i * 7, Math.max(1, w - 8));
            int y = 5 + Math.floorMod(seed * 5 + i * 11, Math.max(1, h - 10));
            g2.fillOval(x, y, 2, 2);
        }
    }

    private void drawTree(Graphics2D g2, int w, int h) {
        int tx = w / 2;
        int base = h - 4;
        g2.setColor(new Color(109, 74, 44));
        g2.fillRect(tx - 2, base - 9, 4, 9);
        g2.setColor(new Color(47, 121, 52));
        g2.fillOval(tx - 10, base - 22, 17, 14);
        g2.fillOval(tx - 5, base - 28, 13, 13);
        g2.setColor(new Color(62, 143, 62));
        g2.fillOval(tx - 1, base - 22, 12, 11);
    }

    private void drawBush(Graphics2D g2, int w, int h) {
        int bx = w / 2;
        int base = h - 5;
        g2.setColor(new Color(57, 136, 58));
        g2.fillOval(bx - 10, base - 9, 12, 9);
        g2.fillOval(bx - 2, base - 11, 13, 11);
    }

    private void drawFlower(Graphics2D g2, int w, int h) {
        int fx = w / 2;
        int base = h - 5;
        g2.setColor(new Color(58, 140, 58));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(fx, base, fx, base - 8);
        g2.setColor(new Color(240, 200, 70));
        g2.fillOval(fx - 3, base - 12, 6, 6);
        g2.setColor(new Color(235, 80, 80));
        g2.fillOval(fx - 6, base - 13, 4, 4);
        g2.fillOval(fx + 2, base - 13, 4, 4);
        g2.fillOval(fx - 4, base - 16, 4, 4);
    }

    private void drawRock(Graphics2D g2, int w, int h) {
        g2.setColor(new Color(140, 140, 140));
        g2.fillOval(w / 2 - 7, h - 13, 14, 9);
        g2.setColor(new Color(170, 170, 170));
        g2.fillOval(w / 2 - 4, h - 12, 6, 4);
    }

    private void drawFlag(Graphics2D g2, int w, int h) {
        int poleX = (int) (w * 0.4);
        int topY = (int) (h * 0.24);
        int bottomY = (int) (h * 0.8);
        g2.setColor(new Color(50, 50, 50));
        g2.setStroke(new BasicStroke(Math.max(1.5f, h * 0.06f)));
        g2.drawLine(poleX, topY, poleX, bottomY);

        int[] xs = {poleX, (int) (w * 0.76), poleX};
        int[] ys = {topY, topY + (int) (h * 0.16), topY + (int) (h * 0.32)};
        g2.setColor(FLAG_RED);
        g2.fillPolygon(xs, ys, 3);
        g2.setColor(new Color(170, 20, 20));
        g2.drawPolygon(xs, ys, 3);
    }

    private void drawBomb(Graphics2D g2, int w, int h, Color body, Color spark) {
        int cx = w / 2;
        int cy = (int) (h * 0.57);
        int r = (int) (Math.min(w, h) * 0.21);

        // Dây cháy.
        g2.setColor(new Color(80, 70, 60));
        g2.setStroke(new BasicStroke(Math.max(1.4f, h * 0.055f),
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int fuseX = cx + (int) (r * 0.65);
        int fuseY = cy - (int) (r * 0.65);
        g2.drawLine(fuseX, fuseY, cx + (int) (r * 1.05), cy - (int) (r * 1.35));

        // Tia lửa.
        g2.setColor(spark);
        int sparkSize = Math.max(2, (int) (r * 0.4));
        g2.fillOval(cx + (int) (r * 1.05) - sparkSize,
                cy - (int) (r * 1.35) - sparkSize, sparkSize * 2, sparkSize * 2);

        // Thân bom.
        g2.setColor(body);
        g2.fillOval(cx - r, cy - r, 2 * r, 2 * r);
        g2.setColor(new Color(255, 255, 255, 90));
        g2.fillOval(cx - (int) (r * 0.6), cy - (int) (r * 0.65),
                Math.max(2, (int) (r * 0.5)), Math.max(2, (int) (r * 0.5)));
    }

    private void drawDefusedBomb(Graphics2D g2, int w, int h) {
        drawBomb(g2, w, h, new Color(120, 120, 120), new Color(90, 180, 90));
        g2.setColor(new Color(25, 155, 70));
        g2.setStroke(new BasicStroke(Math.max(2f, h * 0.075f),
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine((int) (w * 0.58), (int) (h * 0.62),
                (int) (w * 0.69), (int) (h * 0.74));
        g2.drawLine((int) (w * 0.69), (int) (h * 0.74),
                (int) (w * 0.86), (int) (h * 0.46));
    }

    private void drawBurst(Graphics2D g2, int w, int h) {
        int cx = w / 2;
        int cy = (int) (h * 0.57);
        int inner = (int) (Math.min(w, h) * 0.28);
        int outer = (int) (Math.min(w, h) * 0.44);
        g2.setColor(new Color(255, 190, 50));
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * i / 4.0;
            g2.drawLine(cx + (int) (Math.cos(angle) * inner),
                    cy + (int) (Math.sin(angle) * inner),
                    cx + (int) (Math.cos(angle) * outer),
                    cy + (int) (Math.sin(angle) * outer));
        }
    }

    private void drawRedCross(Graphics2D g2, int w, int h) {
        g2.setColor(FLAG_RED);
        g2.setStroke(new BasicStroke(Math.max(2f, h * 0.09f),
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int pad = (int) (w * 0.18);
        g2.drawLine(pad, pad, w - pad, h - pad);
        g2.drawLine(w - pad, pad, pad, h - pad);
    }

    private static int mixSeed(int a, int b) {
        int seed = a * 73856093 ^ b * 19349663;
        return seed == Integer.MIN_VALUE ? 0 : Math.abs(seed);
    }
}
