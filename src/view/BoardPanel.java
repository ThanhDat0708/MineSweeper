package view;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Bàn cờ 2D cách điệu: tự vẽ toàn bộ ô, cờ, bom và các hiệu ứng animation.
 * Không chứa logic dò mìn; controller quyết định dữ liệu từng ô.
 */
public final class BoardPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    /** Trạng thái hiển thị do view định nghĩa. */
    public enum Mark {
        HIDDEN,
        REVEALED,
        FLAGGED,
        QUESTION
    }

    /** Nhận thao tác chuột đã quy đổi sang tọa độ ô. */
    public interface Listener {
        void onLeftClick(int row, int col);

        void onRightClick(int row, int col);

        void onChord(int row, int col);
    }

    private static final int CELL_SIZE = 34;
    private static final int PADDING = 18;
    private static final int FLIP_MS = 150;
    private static final int HINT_MS = 4200;
    private static final int SHAKE_MS = 420;
    private static final int PARTICLE_MAX_MS = 760;

    private static final class CellView {
        Mark mark = Mark.HIDDEN;
        Mark flipFrom = Mark.HIDDEN;
        boolean mine;
        boolean exploded;
        boolean wrongFlag;
        boolean defused;
        int adjacent;
        long revealStart = -1;
    }

    private static final class Particle {
        final double x;
        final double y;
        final double vx;
        final double vy;
        final int size;
        final Color color;
        final long born;
        final long life;

        Particle(double x, double y, double vx, double vy, int size,
                 Color color, long born, long life) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.size = size;
            this.color = color;
            this.born = born;
            this.life = life;
        }
    }

    private final ThemeManager theme;
    private final Random random = new Random();
    private final Timer animTimer;
    private final List<Particle> particles = new ArrayList<>();

    private CellView[][] cells = new CellView[0][0];
    private int rows;
    private int cols;
    private int hoverRow = -1;
    private int hoverCol = -1;
    private int hintRow = -1;
    private int hintCol = -1;
    private long hintStart;
    private boolean paused;
    private Listener listener;

    private long shakeStart = -1;
    private double shakeStrength;
    private long explosionEnd = -1;

    public BoardPanel(ThemeManager theme) {
        this.theme = theme;
        setOpaque(true);
        setFocusable(true);
        setBackground(theme.getPalette().boardBackground());
        setToolTipText("<html>Chuột trái: mở ô &nbsp;•&nbsp; Chuột phải: cờ/? "
                + "&nbsp;•&nbsp; Cả hai nút hoặc double-click: chord</html>");

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                updateHover(e.getX(), e.getY());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                updateHover(-1, -1);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                handleMousePressed(e);
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);

        animTimer = new Timer(16, e -> onAnimationTick());
        animTimer.setCoalesce(true);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void rebuildGrid(int rows, int cols) {
        this.rows = Math.max(0, rows);
        this.cols = Math.max(0, cols);
        cells = new CellView[this.rows][this.cols];
        for (int r = 0; r < this.rows; r++) {
            for (int c = 0; c < this.cols; c++) {
                cells[r][c] = new CellView();
            }
        }
        hoverRow = -1;
        hoverCol = -1;
        hintRow = -1;
        hintCol = -1;
        particles.clear();
        shakeStart = -1;
        explosionEnd = -1;
        animTimer.stop();
        setPreferredSize(new Dimension(this.cols * CELL_SIZE + PADDING * 2,
                this.rows * CELL_SIZE + PADDING * 2));
        revalidate();
        repaint();
    }

    public void showCell(int row, int col, Mark mark, boolean mine, boolean exploded,
                         boolean wrongFlag, boolean defused, int adjacent) {
        showCell(row, col, mark, mine, exploded, wrongFlag, defused, adjacent, true);
    }

    public void showCell(int row, int col, Mark mark, boolean mine, boolean exploded,
                         boolean wrongFlag, boolean defused, int adjacent, boolean animate) {
        if (!inBounds(row, col)) {
            return;
        }
        CellView cell = cells[row][col];
        Mark previous = cell.mark;
        cell.mark = (mark != null) ? mark : Mark.HIDDEN;
        cell.mine = mine;
        cell.exploded = exploded;
        cell.wrongFlag = wrongFlag;
        cell.defused = defused;
        cell.adjacent = Math.max(0, Math.min(8, adjacent));

        if (animate && cell.mark == Mark.REVEALED && previous != Mark.REVEALED) {
            cell.flipFrom = previous;
            cell.revealStart = System.currentTimeMillis();
            ensureAnimating();
        } else if (!animate) {
            cell.revealStart = -1;
        }
        repaint();
    }

    public void showHint(int row, int col) {
        if (!inBounds(row, col)) {
            return;
        }
        hintRow = row;
        hintCol = col;
        hintStart = System.currentTimeMillis();
        ensureAnimating();
        repaint();
    }

    public void clearHint() {
        if (hintRow >= 0 || hintCol >= 0) {
            hintRow = -1;
            hintCol = -1;
            repaint();
        }
    }

    public void showExplosion(int row, int col) {
        if (!inBounds(row, col)) {
            return;
        }
        ThemeManager.Palette p = theme.getPalette();
        long now = System.currentTimeMillis();
        double cx = cellX(col) + CELL_SIZE / 2.0;
        double cy = cellY(row) + CELL_SIZE / 2.0;
        Color[] colors = {
                p.accentLight(), p.accent(), p.flagRed(),
                new Color(255, 220, 130), new Color(150, 140, 130)
        };
        for (int i = 0; i < 38; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double speed = 0.05 + random.nextDouble() * 0.24;
            double vx = Math.cos(angle) * speed;
            double vy = Math.sin(angle) * speed - 0.08;
            int size = 2 + random.nextInt(4);
            long life = 420 + random.nextInt(320);
            particles.add(new Particle(cx, cy, vx, vy, size,
                    colors[random.nextInt(colors.length)], now, life));
        }
        explosionEnd = now + PARTICLE_MAX_MS;
        shakeStart = now;
        shakeStrength = 8.0;
        ensureAnimating();
        repaint();
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
        if (paused) {
            hoverRow = -1;
            hoverCol = -1;
            animTimer.stop();
        } else if (hasActiveAnimation()) {
            ensureAnimating();
        }
        repaint();
    }

    public boolean isPaused() {
        return paused;
    }

    public int getCellSize() {
        return CELL_SIZE;
    }

    private void handleMousePressed(MouseEvent e) {
        if (paused || listener == null) {
            return;
        }
        int[] cell = cellAt(e.getX(), e.getY());
        if (cell == null) {
            return;
        }
        int row = cell[0];
        int col = cell[1];
        int modifiers = e.getModifiersEx();
        boolean bothButtons = (modifiers & MouseEvent.BUTTON1_DOWN_MASK) != 0
                && (modifiers & MouseEvent.BUTTON3_DOWN_MASK) != 0;
        if (SwingUtilities.isMiddleMouseButton(e) || bothButtons) {
            listener.onChord(row, col);
            return;
        }
        if (SwingUtilities.isLeftMouseButton(e)) {
            if (e.getClickCount() == 2 && cells[row][col].mark == Mark.REVEALED) {
                listener.onChord(row, col);
            } else {
                listener.onLeftClick(row, col);
            }
        } else if (SwingUtilities.isRightMouseButton(e)) {
            listener.onRightClick(row, col);
        }
    }

    private void updateHover(int mx, int my) {
        int[] cell = cellAt(mx, my);
        int newRow = (cell != null) ? cell[0] : -1;
        int newCol = (cell != null) ? cell[1] : -1;
        if (newRow != hoverRow || newCol != hoverCol) {
            hoverRow = newRow;
            hoverCol = newCol;
            repaint();
        }
    }

    private int[] cellAt(int mx, int my) {
        if (rows <= 0 || cols <= 0) {
            return null;
        }
        int x = mx - PADDING;
        int y = my - PADDING;
        if (x < 0 || y < 0) {
            return null;
        }
        int col = x / CELL_SIZE;
        int row = y / CELL_SIZE;
        if (row < 0 || row >= rows || col < 0 || col >= cols) {
            return null;
        }
        return new int[]{row, col};
    }

    private boolean inBounds(int row, int col) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }

    private void ensureAnimating() {
        if (!animTimer.isRunning() && !paused) {
            animTimer.start();
        }
    }

    private boolean hasActiveAnimation() {
        long now = System.currentTimeMillis();
        if (hintRow >= 0 && now - hintStart < HINT_MS) {
            return true;
        }
        if (explosionEnd > now || shakeStart >= 0 && now - shakeStart < SHAKE_MS) {
            return true;
        }
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                long start = cells[r][c].revealStart;
                if (start >= 0 && now - start < FLIP_MS) {
                    return true;
                }
            }
        }
        return false;
    }

    private void onAnimationTick() {
        long now = System.currentTimeMillis();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                CellView cell = cells[r][c];
                if (cell.revealStart >= 0 && now - cell.revealStart >= FLIP_MS) {
                    cell.revealStart = -1;
                }
            }
        }
        if (hintRow >= 0 && now - hintStart >= HINT_MS) {
            hintRow = -1;
            hintCol = -1;
        }
        if (shakeStart >= 0 && now - shakeStart >= SHAKE_MS) {
            shakeStart = -1;
        }
        if (explosionEnd >= 0) {
            particles.removeIf(particle -> now - particle.born > particle.life);
            if (particles.isEmpty()) {
                explosionEnd = -1;
            }
        }
        if (hasActiveAnimation()) {
            repaint();
        } else {
            animTimer.stop();
        }
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
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                    RenderingHints.VALUE_STROKE_PURE);

            ThemeManager.Palette p = theme.getPalette();
            int w = getWidth();
            int h = getHeight();
            g2.setPaint(new GradientPaint(0, 0, p.boardBackground().brighter(),
                    0, h, p.boardBackground().darker()));
            g2.fillRect(0, 0, w, h);

            long now = System.currentTimeMillis();
            paintDecorations(g2, p);

            AffineTransform original = g2.getTransform();
            if (shakeStart >= 0 && now - shakeStart < SHAKE_MS) {
                double progress = 1.0 - (now - shakeStart) / (double) SHAKE_MS;
                int dx = (int) Math.round((random.nextDouble() * 2.0 - 1.0)
                        * shakeStrength * progress);
                int dy = (int) Math.round((random.nextDouble() * 2.0 - 1.0)
                        * shakeStrength * progress);
                g2.translate(dx, dy);
            }

            int boardX = PADDING;
            int boardY = PADDING;
            int boardW = cols * CELL_SIZE;
            int boardH = rows * CELL_SIZE;

            // Bóng và khung gỗ bao quanh lưới.
            g2.setColor(new Color(0, 0, 0, 95));
            g2.fill(new RoundRectangle2D.Float(boardX - 6, boardY - 4,
                    boardW + 12, boardH + 14, 18, 18));
            g2.setPaint(new GradientPaint(0, boardY - 4, p.boardFrame(),
                    0, boardY + boardH + 8, p.boardFrameDark()));
            g2.fill(new RoundRectangle2D.Float(boardX - 4, boardY - 4,
                    boardW + 8, boardH + 8, 16, 16));
            g2.setColor(new Color(0, 0, 0, 70));
            g2.setStroke(new BasicStroke(2f));
            g2.draw(new RoundRectangle2D.Float(boardX - 4, boardY - 4,
                    boardW + 8, boardH + 8, 16, 16));

            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    paintCell(g2, p, cells[r][c], r, c, now);
                }
            }
            paintGrid(g2, p, boardX, boardY, boardW, boardH);
            paintHover(g2, p);
            paintHint(g2, p);
            paintParticles(g2, now);

            g2.setTransform(original);
            if (paused) {
                paintPausedOverlay(g2, p, w, h);
            }
        } finally {
            g2.dispose();
        }
    }

    private void paintDecorations(Graphics2D g2, ThemeManager.Palette p) {
        int boardX = PADDING;
        int boardY = PADDING;
        int boardW = cols * CELL_SIZE;
        int boardH = rows * CELL_SIZE;
        if (boardW <= 0 || boardH <= 0) {
            return;
        }

        // Lá và hoa nhỏ chỉ nằm ngoài khung lưới, không nằm trong ô chơi.
        drawLeafCluster(g2, p, boardX - 9, boardY - 6, 11);
        drawLeafCluster(g2, p, boardX + boardW + 1, boardY + 8, -10);
        drawLeafCluster(g2, p, boardX + 4, boardY + boardH + 2, 8);
        drawLeafCluster(g2, p, boardX + boardW - 2, boardY + boardH + 5, -8);

        g2.setColor(p.hill());
        g2.fillOval(boardX + 4, boardY + boardH + 7, 9, 5);
        g2.fillOval(boardX + boardW - 14, boardY - 9, 7, 4);

        g2.setColor(p.accent());
        g2.fillOval(boardX + 10, boardY + boardH + 8, 4, 4);
        g2.fillOval(boardX + boardW - 10, boardY + 6, 4, 4);
        g2.setColor(p.accentLight());
        g2.fillOval(boardX + 9, boardY + boardH + 7, 2, 2);
        g2.fillOval(boardX + boardW - 11, boardY + 5, 2, 2);
    }

    private static void drawLeafCluster(Graphics2D g2, ThemeManager.Palette p,
                                        int x, int y, int direction) {
        g2.setColor(p.groundLine());
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(x, y + 6, x + direction, y - 6);
        g2.setColor(p.hill());
        g2.fill(new Ellipse2D.Float(x + direction - 7, y - 4, 13, 7));
        g2.setColor(p.ground());
        g2.fill(new Ellipse2D.Float(x + direction - 3, y - 9, 9, 6));
        g2.setColor(new Color(255, 255, 255, 55));
        g2.draw(new Ellipse2D.Float(x + direction - 6, y - 3, 10, 5));
    }

    private void paintCell(Graphics2D g2, ThemeManager.Palette p, CellView cell,
                           int row, int col, long now) {
        int x = cellX(col);
        int y = cellY(row);
        Mark drawMark = cell.mark;
        long start = cell.revealStart;
        boolean flipping = start >= 0 && now - start < FLIP_MS
                && cell.mark == Mark.REVEALED;
        double t = 1.0;
        if (flipping) {
            t = (now - start) / (double) FLIP_MS;
            if (t < 0.5) {
                drawMark = cell.flipFrom;
            } else {
                drawMark = Mark.REVEALED;
            }
        }

        double scaleX = 1.0;
        double alpha = 1.0;
        if (flipping) {
            scaleX = (t < 0.5) ? (1.0 - t * 2.0) : (t * 2.0 - 1.0);
            scaleX = Math.max(0.02, scaleX);
            alpha = 0.55 + 0.45 * t;
        }

        AffineTransform saved = g2.getTransform();
        Composite savedComposite = g2.getComposite();
        if (flipping) {
            g2.translate(x + CELL_SIZE / 2.0, y + CELL_SIZE / 2.0);
            g2.scale(scaleX, 1.0);
            g2.translate(-(x + CELL_SIZE / 2.0), -(y + CELL_SIZE / 2.0));
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                    (float) alpha));
        }

        if (drawMark == Mark.REVEALED) {
            paintRevealedCell(g2, p, cell, x, y);
        } else {
            paintHiddenCell(g2, p, cell, drawMark, x, y);
        }

        g2.setComposite(savedComposite);
        g2.setTransform(saved);
    }

    private void paintHiddenCell(Graphics2D g2, ThemeManager.Palette p,
                                 CellView cell, Mark mark, int x, int y) {
        int size = CELL_SIZE;
        boolean even = ((x / size) + (y / size)) % 2 == 0;
        RoundRectangle2D block = new RoundRectangle2D.Float(
                x + 1.3f, y + 1.3f, size - 3.2f, size - 3.2f, 7, 7);

        g2.setColor(new Color(0, 0, 0, 65));
        g2.fill(new RoundRectangle2D.Float(x + 2f, y + 3.2f,
                size - 3.6f, size - 3.2f, 7, 7));

        Color top = even ? p.grassTop() : brighten(p.grassTop(), 0.06);
        Color bottom = even ? p.grassBottom() : brighten(p.grassBottom(), 0.05);
        g2.setPaint(new GradientPaint(x, y + 1, top, x, y + size, bottom));
        g2.fill(block);

        // Viền sáng phía trên.
        g2.setColor(new Color(p.grassHighlight().getRed(), p.grassHighlight().getGreen(),
                p.grassHighlight().getBlue(), 190));
        g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(new Line2D.Float(x + 4.5f, y + 3f, x + size - 4.5f, y + 3f));
        g2.setColor(new Color(255, 255, 255, 55));
        g2.draw(new Line2D.Float(x + 3f, y + 6f, x + 3f, y + size - 7f));

        // Bóng tối phía dưới và bên phải.
        g2.setColor(new Color(p.grassShadow().getRed(), p.grassShadow().getGreen(),
                p.grassShadow().getBlue(), 220));
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(new Line2D.Float(x + 4.5f, y + size - 2.7f,
                x + size - 4.5f, y + size - 2.7f));
        g2.setColor(new Color(p.grassShadow().getRed(), p.grassShadow().getGreen(),
                p.grassShadow().getBlue(), 130));
        g2.draw(new Line2D.Float(x + size - 2.7f, y + 6f,
                x + size - 2.7f, y + size - 7f));
        g2.setColor(new Color(0, 0, 0, 65));
        g2.setStroke(new BasicStroke(1f));
        g2.draw(block);

        if (mark == Mark.FLAGGED) {
            if (cell.wrongFlag) {
                drawBomb(g2, p, x, y, false);
                drawRedCross(g2, x, y);
            } else {
                drawFlag(g2, p, x, y);
            }
        } else if (cell.mine) {
            // Bom còn ẩn chỉ hiện khi controller báo ván đã kết thúc.
            drawBomb(g2, p, x, y, false);
        } else if (mark == Mark.QUESTION) {
            drawQuestionMark(g2, p, x, y);
        }
    }

    private void paintRevealedCell(Graphics2D g2, ThemeManager.Palette p,
                                   CellView cell, int x, int y) {
        int size = CELL_SIZE;

        // Nền đất lõm: tối ở trên, sáng dần xuống dưới.
        g2.setPaint(new GradientPaint(x, y, p.dirtTop(),
                x, y + size, p.dirtBottom()));
        g2.fillRect(x + 1, y + 1, size - 2, size - 2);

        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillRect(x + 1, y + 1, size - 2, 2);
        g2.fillRect(x + 1, y + 1, 2, size - 2);
        g2.setColor(new Color(p.dirtHighlight().getRed(), p.dirtHighlight().getGreen(),
                p.dirtHighlight().getBlue(), 120));
        g2.fillRect(x + 2, y + size - 3, size - 3, 1);
        g2.fillRect(x + size - 3, y + 2, 1, size - 3);
        g2.setColor(new Color(p.dirtEdge().getRed(), p.dirtEdge().getGreen(),
                p.dirtEdge().getBlue(), 170));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRect(x + 1, y + 1, size - 2, size - 2);

        if (cell.mine) {
            if (cell.exploded) {
                drawExplosionBurst(g2, p, x, y);
                drawBomb(g2, p, x, y, false);
            } else if (cell.defused) {
                drawBomb(g2, p, x, y, true);
                drawDefusedCheck(g2, p, x, y);
            } else {
                drawBomb(g2, p, x, y, false);
            }
        } else {
            drawAdjacentNumber(g2, p, cell.adjacent, x, y);
        }
    }

    private void drawAdjacentNumber(Graphics2D g2, ThemeManager.Palette p,
                                    int adjacent, int x, int y) {
        if (adjacent <= 0) {
            return;
        }
        int size = CELL_SIZE;
        String text = String.valueOf(adjacent);
        g2.setFont(ThemeManager.font(Font.BOLD, (int) (size * 0.58)));
        FontMetrics fm = g2.getFontMetrics();
        int tx = x + (size - fm.stringWidth(text)) / 2;
        int ty = y + (size - fm.getHeight()) / 2 + fm.getAscent();

        Color color = p.numbers()[Math.min(adjacent, 8) - 1];
        g2.setColor(new Color(0, 0, 0, 85));
        g2.drawString(text, tx + 1, ty + 1);
        g2.setColor(color);
        g2.drawString(text, tx, ty);
    }

    private void drawQuestionMark(Graphics2D g2, ThemeManager.Palette p, int x, int y) {
        int size = CELL_SIZE;
        g2.setFont(ThemeManager.font(Font.BOLD, (int) (size * 0.62)));
        FontMetrics fm = g2.getFontMetrics();
        String text = "?";
        int tx = x + (size - fm.stringWidth(text)) / 2;
        int ty = y + (size - fm.getHeight()) / 2 + fm.getAscent();
        g2.setColor(new Color(0, 0, 0, 90));
        g2.drawString(text, tx + 1, ty + 1);
        g2.setColor(p.text());
        g2.drawString(text, tx, ty);
    }

    private void drawFlag(Graphics2D g2, ThemeManager.Palette p, int x, int y) {
        int size = CELL_SIZE;
        float poleX = x + size * 0.37f;
        float topY = y + size * 0.19f;
        float bottomY = y + size * 0.82f;

        g2.setColor(new Color(0, 0, 0, 80));
        g2.setStroke(new BasicStroke(3.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(new Line2D.Float(poleX + 1.3f, topY + 1.2f, poleX + 1.3f, bottomY + 1.2f));

        g2.setColor(p.poleWoodDark());
        g2.setStroke(new BasicStroke(3.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(new Line2D.Float(poleX, topY, poleX, bottomY));
        g2.setColor(p.poleWood());
        g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(new Line2D.Float(poleX, topY + 1, poleX, bottomY - 1));
        g2.setColor(p.poleWoodDark());
        g2.fill(new Ellipse2D.Float(poleX - 2.2f, topY - 2.2f, 4.4f, 4.4f));

        // Lá cờ đỏ có gợn sóng nhẹ.
        Path2D flag = new Path2D.Float();
        flag.moveTo(poleX + 1.2f, topY + 1.0f);
        flag.quadTo(poleX + size * 0.23f, topY - 1.5f,
                poleX + size * 0.41f, topY + size * 0.07f);
        flag.quadTo(poleX + size * 0.28f, topY + size * 0.16f,
                poleX + size * 0.41f, topY + size * 0.25f);
        flag.quadTo(poleX + size * 0.20f, topY + size * 0.30f,
                poleX + 1.2f, topY + size * 0.32f);
        flag.closePath();
        g2.setPaint(new GradientPaint(poleX, topY, p.flagRed(),
                poleX + size * 0.45f, topY + size * 0.3f, p.flagRedDark()));
        g2.fill(flag);
        g2.setColor(new Color(255, 255, 255, 95));
        g2.setStroke(new BasicStroke(0.9f));
        g2.draw(flag);

        g2.setColor(p.poleWoodDark());
        g2.fillOval((int) (poleX - 3), (int) (bottomY - 1), 7, 3);
    }

    private void drawBomb(Graphics2D g2, ThemeManager.Palette p, int x, int y,
                          boolean muted) {
        int size = CELL_SIZE;
        float cx = x + size * 0.50f;
        float cy = y + size * 0.58f;
        float r = size * 0.215f;

        Color body = muted ? desaturate(p.bombBody()) : p.bombBody();
        Color highlight = muted ? desaturate(p.bombHighlight()) : p.bombHighlight();
        Color spark = muted ? new Color(110, 190, 120) : p.spark();

        g2.setColor(new Color(0, 0, 0, 70));
        g2.fill(new Ellipse2D.Float(cx - r, cy + r * 0.78f, r * 2.0f, r * 0.75f));

        // Dây ngòi.
        g2.setColor(muted ? new Color(95, 95, 95) : new Color(92, 74, 55));
        g2.setStroke(new BasicStroke(Math.max(1.5f, size * 0.055f),
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        QuadCurve2D fuse = new QuadCurve2D.Float(
                cx + r * 0.55f, cy - r * 0.65f,
                cx + r * 1.30f, cy - r * 1.05f,
                cx + r * 1.05f, cy - r * 1.45f);
        g2.draw(fuse);

        // Tia lửa.
        float sx = cx + r * 1.05f;
        float sy = cy - r * 1.45f;
        g2.setColor(new Color(spark.getRed(), spark.getGreen(), spark.getBlue(), 80));
        g2.fill(new Ellipse2D.Float(sx - r * 0.42f, sy - r * 0.42f,
                r * 0.84f, r * 0.84f));
        g2.setColor(spark);
        g2.fill(new Ellipse2D.Float(sx - r * 0.24f, sy - r * 0.24f,
                r * 0.48f, r * 0.48f));

        // Thân bom.
        RadialGradientPaint paint = new RadialGradientPaint(
                cx - r * 0.38f, cy - r * 0.40f, r * 1.75f,
                new float[]{0f, 0.55f, 1f},
                new Color[]{highlight, body, darken(body, 0.35)});
        g2.setPaint(paint);
        g2.fill(new Ellipse2D.Float(cx - r, cy - r, r * 2.0f, r * 2.0f));
        g2.setColor(new Color(0, 0, 0, 90));
        g2.setStroke(new BasicStroke(1.1f));
        g2.draw(new Ellipse2D.Float(cx - r, cy - r, r * 2.0f, r * 2.0f));

        g2.setColor(new Color(255, 255, 255, muted ? 70 : 120));
        g2.fill(new Ellipse2D.Float(cx - r * 0.58f, cy - r * 0.62f,
                r * 0.46f, r * 0.34f));
    }

    private void drawExplosionBurst(Graphics2D g2, ThemeManager.Palette p, int x, int y) {
        int size = CELL_SIZE;
        float cx = x + size * 0.50f;
        float cy = y + size * 0.58f;
        float inner = size * 0.20f;
        float outer = size * 0.46f;

        g2.setPaint(new RadialGradientPaint(cx, cy, size * 0.15f,
                new float[]{0f, 0.55f, 1f},
                new Color[]{new Color(255, 245, 170, 230),
                        new Color(255, 140, 40, 190),
                        new Color(200, 40, 30, 0)}));
        g2.fill(new Ellipse2D.Float(cx - outer, cy - outer, outer * 2, outer * 2));

        g2.setColor(p.accentLight());
        g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * i / 4.0;
            g2.draw(new Line2D.Float(
                    cx + (float) Math.cos(angle) * inner,
                    cy + (float) Math.sin(angle) * inner,
                    cx + (float) Math.cos(angle) * outer,
                    cy + (float) Math.sin(angle) * outer));
        }
    }

    private void drawDefusedCheck(Graphics2D g2, ThemeManager.Palette p, int x, int y) {
        int size = CELL_SIZE;
        g2.setColor(new Color(25, 170, 80, 90));
        g2.setStroke(new BasicStroke(Math.max(4f, size * 0.11f),
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(new Line2D.Float(x + size * 0.56f, y + size * 0.64f,
                x + size * 0.69f, y + size * 0.77f));
        g2.draw(new Line2D.Float(x + size * 0.69f, y + size * 0.77f,
                x + size * 0.88f, y + size * 0.42f));
        g2.setColor(new Color(40, 210, 110));
        g2.setStroke(new BasicStroke(Math.max(2.2f, size * 0.075f),
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(new Line2D.Float(x + size * 0.56f, y + size * 0.64f,
                x + size * 0.69f, y + size * 0.77f));
        g2.draw(new Line2D.Float(x + size * 0.69f, y + size * 0.77f,
                x + size * 0.88f, y + size * 0.42f));
    }

    private void drawRedCross(Graphics2D g2, int x, int y) {
        int size = CELL_SIZE;
        float pad = size * 0.19f;
        g2.setColor(new Color(0, 0, 0, 70));
        g2.setStroke(new BasicStroke(Math.max(3f, size * 0.11f),
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(new Line2D.Float(x + pad + 1, y + pad + 1,
                x + size - pad + 1, y + size - pad + 1));
        g2.draw(new Line2D.Float(x + size - pad + 1, y + pad + 1,
                x + pad + 1, y + size - pad + 1));
        g2.setColor(new Color(235, 55, 55));
        g2.setStroke(new BasicStroke(Math.max(2f, size * 0.08f),
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(new Line2D.Float(x + pad, y + pad,
                x + size - pad, y + size - pad));
        g2.draw(new Line2D.Float(x + size - pad, y + pad,
                x + pad, y + size - pad));
    }

    private void paintGrid(Graphics2D g2, ThemeManager.Palette p,
                           int boardX, int boardY, int boardW, int boardH) {
        if (rows <= 0 || cols <= 0) {
            return;
        }
        g2.setColor(new Color(p.gridLine().getRed(), p.gridLine().getGreen(),
                p.gridLine().getBlue(), 210));
        g2.setStroke(new BasicStroke(1.15f));
        for (int c = 1; c < cols; c++) {
            int x = boardX + c * CELL_SIZE;
            g2.draw(new Line2D.Float(x, boardY, x, boardY + boardH));
        }
        for (int r = 1; r < rows; r++) {
            int y = boardY + r * CELL_SIZE;
            g2.draw(new Line2D.Float(boardX, y, boardX + boardW, y));
        }
        g2.setColor(new Color(0, 0, 0, 80));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRect(boardX, boardY, boardW, boardH);
    }

    private void paintHover(Graphics2D g2, ThemeManager.Palette p) {
        if (paused || !inBounds(hoverRow, hoverCol)) {
            return;
        }
        CellView cell = cells[hoverRow][hoverCol];
        int x = cellX(hoverCol);
        int y = cellY(hoverRow);
        RoundRectangle2D glow = new RoundRectangle2D.Float(
                x + 1.5f, y + 1.5f, CELL_SIZE - 3f, CELL_SIZE - 3f, 8, 8);
        g2.setColor(new Color(p.accentLight().getRed(), p.accentLight().getGreen(),
                p.accentLight().getBlue(), 28));
        g2.fill(glow);
        g2.setColor(new Color(p.accentLight().getRed(), p.accentLight().getGreen(),
                p.accentLight().getBlue(), 180));
        g2.setStroke(new BasicStroke(2f));
        g2.draw(glow);

        if (cell.mark == Mark.REVEALED && cell.adjacent > 0) {
            g2.setColor(new Color(p.accentLight().getRed(), p.accentLight().getGreen(),
                    p.accentLight().getBlue(), 30));
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    if (dr == 0 && dc == 0) {
                        continue;
                    }
                    int nr = hoverRow + dr;
                    int nc = hoverCol + dc;
                    if (inBounds(nr, nc) && cells[nr][nc].mark != Mark.REVEALED) {
                        g2.fillRoundRect(cellX(nc) + 2, cellY(nr) + 2,
                                CELL_SIZE - 4, CELL_SIZE - 4, 6, 6);
                    }
                }
            }
        }

    }

    private void paintHint(Graphics2D g2, ThemeManager.Palette p) {
        if (paused || !inBounds(hintRow, hintCol)) {
            return;
        }
        long now = System.currentTimeMillis();
        // Nếu đồng hồ animation bị dừng lúc tạm pause, gợi ý có thể đã hết hạn
        // mà chưa được dọn; kiểm tra thời hạn để không vẽ gợi ý cũ.
        if (now - hintStart >= HINT_MS) {
            return;
        }
        double pulse = 0.5 + 0.5 * Math.sin((now - hintStart) / 150.0);
        int alpha = 90 + (int) (100 * pulse);
        g2.setColor(new Color(p.accent().getRed(), p.accent().getGreen(),
                p.accent().getBlue(), alpha));
        g2.setStroke(new BasicStroke(2.5f));
        int grow = (int) (3 + pulse * 3);
        g2.draw(new RoundRectangle2D.Float(cellX(hintCol) - grow,
                cellY(hintRow) - grow, CELL_SIZE + grow * 2.0f,
                CELL_SIZE + grow * 2.0f, 10, 10));

        // Mũi tên nhỏ chỉ vào ô gợi ý.
        int arrowX = cellX(hintCol) + CELL_SIZE / 2;
        int arrowY = cellY(hintRow) - grow - 7;
        Path2D arrow = new Path2D.Float();
        arrow.moveTo(arrowX - 5, arrowY);
        arrow.lineTo(arrowX + 5, arrowY);
        arrow.lineTo(arrowX, arrowY + 6);
        arrow.closePath();
        g2.setColor(new Color(p.accent().getRed(), p.accent().getGreen(),
                p.accent().getBlue(), Math.max(100, alpha)));
        g2.fill(arrow);
    }

    private void paintParticles(Graphics2D g2, long now) {
        if (particles.isEmpty()) {
            return;
        }
        for (Iterator<Particle> it = particles.iterator(); it.hasNext(); ) {
            Particle particle = it.next();
            long age = now - particle.born;
            if (age < 0 || age > particle.life) {
                it.remove();
                continue;
            }
            double t = age / (double) particle.life;
            double x = particle.x + particle.vx * age;
            double y = particle.y + particle.vy * age + 0.00055 * age * age;
            int alpha = (int) Math.round(255 * (1.0 - t));
            g2.setColor(new Color(particle.color.getRed(), particle.color.getGreen(),
                    particle.color.getBlue(), Math.max(0, alpha)));
            g2.fill(new Ellipse2D.Double(x - particle.size / 2.0,
                    y - particle.size / 2.0, particle.size, particle.size));
            if (particle.size > 3) {
                g2.setColor(new Color(particle.color.getRed(), particle.color.getGreen(),
                        particle.color.getBlue(), Math.max(0, alpha / 2)));
                g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND));
                g2.draw(new Line2D.Double(x - particle.vx * 14,
                        y - particle.vy * 14, x, y));
            }
        }
    }

    private void paintPausedOverlay(Graphics2D g2, ThemeManager.Palette p, int w, int h) {
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(0, 0, w, h);

        int boxW = Math.min(300, w - 40);
        int boxH = 104;
        int x = (w - boxW) / 2;
        int y = (h - boxH) / 2;
        g2.setColor(new Color(p.controlBackground().getRed(), p.controlBackground().getGreen(),
                p.controlBackground().getBlue(), 235));
        g2.fill(new RoundRectangle2D.Float(x, y, boxW, boxH, 18, 18));
        g2.setColor(p.controlBorder());
        g2.setStroke(new BasicStroke(2f));
        g2.draw(new RoundRectangle2D.Float(x, y, boxW, boxH, 18, 18));

        g2.setColor(p.accent());
        g2.fillRoundRect(x + 34, y + 28, 9, 40, 4, 4);
        g2.fillRoundRect(x + 51, y + 28, 9, 40, 4, 4);

        g2.setFont(ThemeManager.font(Font.BOLD, 22));
        g2.setColor(p.text());
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString("TẠM DỪNG", x + 78, y + 48);
        g2.setFont(ThemeManager.font(Font.PLAIN, 12));
        g2.setColor(p.textMuted());
        g2.drawString("Nhấn nút Tiếp tục để chơi tiếp", x + 78, y + 70);
    }

    private int cellX(int col) {
        return PADDING + col * CELL_SIZE;
    }

    private int cellY(int row) {
        return PADDING + row * CELL_SIZE;
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
        int gray = (int) (color.getRed() * 0.3 + color.getGreen() * 0.59
                + color.getBlue() * 0.11);
        return new Color(gray, gray, gray, color.getAlpha());
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
