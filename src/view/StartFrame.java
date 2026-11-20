package view;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;

/**
 * Màn hình bắt đầu: nhập tên người chơi, chọn độ khó, vào game hoặc xem bảng xếp hạng.
 * View chỉ thu thập dữ liệu và báo sự kiện, không tự tạo game.
 */
public class StartFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    /** Tên chế độ hiển thị; chỉ số khớp thứ tự Difficulty.values() ở tầng model. */
    private static final String[] MODE_NAMES = {"Beginner", "Intermediate", "Expert"};

    private final JTextField nameField = new JTextField(16);
    private final JComboBox<String> modeBox = new JComboBox<>(MODE_NAMES);
    private final JButton startButton = new JButton("Bắt đầu");
    private final JButton leaderboardButton = new JButton("Bảng xếp hạng");

    public StartFrame() {
        super("Minesweeper - Khu rừng bom");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        BackgroundPanel content = new BackgroundPanel();
        content.setLayout(new GridBagLayout());
        content.add(buildCard());
        setContentPane(content);

        getRootPane().setDefaultButton(startButton);
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel buildCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(true);
        card.setBackground(new Color(255, 255, 255, 235));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(110, 175, 85), 2),
                new EmptyBorder(24, 36, 24, 36)));

        JLabel title = new JLabel("DÒ MÌN");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 32));
        title.setForeground(new Color(35, 95, 55));
        title.setAlignmentX(CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Khu rừng bom");
        subtitle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        subtitle.setForeground(new Color(80, 110, 90));
        subtitle.setAlignmentX(CENTER_ALIGNMENT);

        JLabel nameLabel = new JLabel("Tên người chơi");
        nameLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        nameLabel.setForeground(new Color(45, 80, 60));
        nameLabel.setAlignmentX(LEFT_ALIGNMENT);

        nameField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        nameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        nameField.setAlignmentX(LEFT_ALIGNMENT);

        JLabel modeLabel = new JLabel("Độ khó");
        modeLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        modeLabel.setForeground(new Color(45, 80, 60));
        modeLabel.setAlignmentX(LEFT_ALIGNMENT);

        modeBox.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        modeBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        modeBox.setAlignmentX(LEFT_ALIGNMENT);

        styleButton(startButton, new Color(45, 150, 75));
        styleButton(leaderboardButton, new Color(65, 110, 165));

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

    private static void styleButton(JButton button, Color background) {
        button.setAlignmentX(CENTER_ALIGNMENT);
        button.setBackground(background);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        button.setHorizontalAlignment(SwingConstants.CENTER);
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

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                g2.setPaint(new GradientPaint(0, 0, new Color(168, 216, 238),
                        0, h * 0.72f, new Color(238, 248, 255)));
                g2.fillRect(0, 0, w, h);

                g2.setColor(new Color(150, 200, 140));
                g2.fillOval(-60, h - 210, w / 2 + 90, 170);
                g2.fillOval(w / 2 - 30, h - 230, w / 2 + 110, 190);

                int groundY = h - 110;
                g2.setColor(new Color(110, 175, 85));
                g2.fillRect(0, groundY, w, h - groundY);
                g2.setColor(new Color(92, 150, 70));
                g2.fillRect(0, groundY, w, 6);
            } finally {
                g2.dispose();
            }
        }
    }
}
