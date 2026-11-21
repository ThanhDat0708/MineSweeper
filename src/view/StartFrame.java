package view;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.awt.geom.RoundRectangle2D;

/**
 * Màn hình bắt đầu: nhập tên người chơi, chọn độ khó, vào game hoặc xem bảng xếp hạng.
 * View chỉ thu thập dữ liệu và báo sự kiện, không tự tạo game.
 */
public class StartFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    /** Tên chế độ hiển thị; chỉ số khớp thứ tự Difficulty.values() ở tầng model. */
    private static final String[] MODE_NAMES = {"Beginner", "Intermediate", "Expert"};

    private final ThemeManager themeManager = new ThemeManager();
    private final JTextField nameField = new JTextField(16);
    private final JComboBox<String> modeBox = new JComboBox<>(MODE_NAMES);
    private final RoundedButton startButton =
            new RoundedButton("Bắt đầu", themeManager, RoundedButton.Variant.PRIMARY);
    private final RoundedButton leaderboardButton =
            new RoundedButton("Bảng xếp hạng", themeManager, RoundedButton.Variant.SECONDARY);

    public StartFrame() {
        super("Minesweeper - Khu rừng bom");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        BackgroundPanel content = new BackgroundPanel(themeManager);
        content.setLayout(new GridBagLayout());
        content.add(buildCard());
        setContentPane(content);

        getRootPane().setDefaultButton(startButton);
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel buildCard() {
        CardPanel card = new CardPanel(themeManager);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(24, 36, 24, 36));

        JLabel title = new JLabel("DÒ MÌN");
        title.setFont(ThemeManager.font(Font.BOLD, 32));
        title.setForeground(new Color(35, 95, 55));
        title.setAlignmentX(CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Khu rừng bom");
        subtitle.setFont(ThemeManager.font(Font.PLAIN, 15));
        subtitle.setForeground(new Color(80, 110, 90));
        subtitle.setAlignmentX(CENTER_ALIGNMENT);

        JLabel nameLabel = new JLabel("Tên người chơi");
        nameLabel.setFont(ThemeManager.font(Font.BOLD, 13));
        nameLabel.setForeground(new Color(45, 80, 60));
        nameLabel.setAlignmentX(LEFT_ALIGNMENT);

        nameField.setFont(ThemeManager.font(Font.PLAIN, 15));
        nameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        nameField.setAlignmentX(LEFT_ALIGNMENT);

        JLabel modeLabel = new JLabel("Độ khó");
        modeLabel.setFont(ThemeManager.font(Font.BOLD, 13));
        modeLabel.setForeground(new Color(45, 80, 60));
        modeLabel.setAlignmentX(LEFT_ALIGNMENT);

        modeBox.setFont(ThemeManager.font(Font.PLAIN, 14));
        modeBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        modeBox.setAlignmentX(LEFT_ALIGNMENT);

        styleButton(startButton);
        styleButton(leaderboardButton);

        card.add(title);
        card.add(Box.createVerticalStrut(4));
        card.add(subtitle);
        card.add(Box.createVerticalStrut(22));
        card.add(nameLabel);
        card.add(Box.createVerticalStrut(6));
        card.add(nameField);
        card.add(Box.createVerticalStrut(14));
        card.add(modeLabel);
        card.add(Box.createVerticalStrut(6));
        card.add(modeBox);
        card.add(Box.createVerticalStrut(22));
        card.add(startButton);
        card.add(Box.createVerticalStrut(8));
        card.add(leaderboardButton);
        return card;
    }

    private static void styleButton(RoundedButton button) {
        button.setAlignmentX(CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
    }

    public String getPlayerName() {
        return nameField.getText();
    }

    public int getSelectedModeIndex() {
        return modeBox.getSelectedIndex();
    }

    public void addStartListener(ActionListener listener) {
        startButton.addActionListener(listener);
    }

    public void addLeaderboardListener(ActionListener listener) {
        leaderboardButton.addActionListener(listener);
    }

    public void showErrorMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "Thiếu thông tin",
                JOptionPane.WARNING_MESSAGE);
    }

    public void focusNameField() {
        SwingUtilities.invokeLater(nameField::requestFocusInWindow);
    }

    /** Nền trời - đồi - mặt đất vẽ bằng Graphics2D cho đồng bộ với bảng nhân vật. */
    private static final class BackgroundPanel extends JPanel {

        private static final long serialVersionUID = 1L;

        private final ThemeManager theme;

        BackgroundPanel(ThemeManager theme) {
            this.theme = theme;
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                ThemeManager.Palette p = theme.getPalette();
                int w = getWidth();
                int h = getHeight();

                g2.setPaint(new GradientPaint(0, 0, p.skyTop(), 0, h * 0.72f, p.skyBottom()));
                g2.fillRect(0, 0, w, h);

                g2.setColor(new Color(255, 240, 170, 100));
                g2.fillOval(w - 82, 30, 48, 48);

                g2.setColor(p.hill());
                g2.fillOval(-60, h - 210, w / 2 + 90, 170);
                g2.fillOval(w / 2 - 30, h - 230, w / 2 + 110, 190);

                int groundY = h - 110;
                g2.setColor(p.ground());
                g2.fillRect(0, groundY, w, h - groundY);
                g2.setColor(p.groundLine());
                g2.fillRect(0, groundY, w, 6);

                g2.setColor(new Color(p.grassHighlight().getRed(),
                        p.grassHighlight().getGreen(), p.grassHighlight().getBlue(), 75));
                for (int i = 10; i < w; i += 30) {
                    g2.drawLine(i, groundY + 12, i + 4, groundY + 3);
                    g2.drawLine(i, groundY + 12, i - 3, groundY + 4);
                }

                // Cụm lá trang trí ngoài khung.
                g2.setColor(p.groundLine());
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND));
                g2.drawLine(24, groundY + 6, 12, groundY - 10);
                g2.setColor(p.hill());
                g2.fillOval(2, groundY - 18, 22, 11);
                g2.fillOval(15, groundY - 24, 18, 11);
            } finally {
                g2.dispose();
            }
        }
    }

    /** Thẻ nội dung bo góc, có bóng đổ nhẹ. */
    private static final class CardPanel extends JPanel {

        private static final long serialVersionUID = 1L;

        private final ThemeManager theme;

        CardPanel(ThemeManager theme) {
            this.theme = theme;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                ThemeManager.Palette p = theme.getPalette();
                int w = getWidth();
                int h = getHeight();
                g2.setColor(new Color(0, 0, 0, 60));
                g2.fill(new RoundRectangle2D.Float(3, 5, w - 6, h - 7, 28, 28));
                g2.setColor(new Color(255, 255, 255, 236));
                g2.fill(new RoundRectangle2D.Float(0, 0, w - 3, h - 5, 26, 26));
                g2.setColor(new Color(p.groundLine().getRed(), p.groundLine().getGreen(),
                        p.groundLine().getBlue(), 170));
                g2.setStroke(new BasicStroke(2f));
                g2.draw(new RoundRectangle2D.Float(1, 1, w - 5, h - 7, 26, 26));
            } finally {
                g2.dispose();
            }
        }
    }
}
