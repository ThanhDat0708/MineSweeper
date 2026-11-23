package view;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Cửa sổ chính: bảng nhân vật bên trái, thanh điều khiển và bàn cờ bên phải.
 * View chỉ hiển thị; mọi thao tác game do controller xử lý.
 */
public class MainFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    /** Tên chế độ hiển thị; chỉ số khớp thứ tự Difficulty.values() ở tầng model. */
    private static final String[] MODE_NAMES = {"Beginner", "Intermediate", "Expert"};

    private final ThemeManager themeManager;
    private final CharacterPanel characterPanel;
    private final BoardPanel boardPanel;
    private final LedCounter mineCounter;
    private final LedCounter timeCounter;
    private final RoundedButton newGameButton;
    private final RoundedButton leaderboardButton;
    private final RoundedButton hintButton;
    private final RoundedButton pauseButton;
    private final RoundedButton saveButton;
    private final RoundedButton themeButton;
    private final RoundedButton soundButton;
    private final JComboBox<String> modeBox;
    private final JSlider volumeSlider;
    private final JLabel volumeLabel;
    private final ControlBar topBar;
    private final ControlBar toolBar;
    private final JPanel contentPane;
    private boolean suppressModeEvents;
    private boolean soundEnabled = true;

    public MainFrame() {
        super("Minesweeper - Khu rừng bom");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        themeManager = new ThemeManager();
        characterPanel = new CharacterPanel(themeManager);
        boardPanel = new BoardPanel(themeManager);

        mineCounter = new LedCounter("MÌN", themeManager);
        mineCounter.setToolTipText("Số bom còn lại");
        timeCounter = new LedCounter("THỜI GIAN", themeManager);
        timeCounter.setToolTipText("Thời gian (giây)");

        newGameButton = new RoundedButton("Mới", themeManager, RoundedButton.Variant.PRIMARY);
        newGameButton.setToolTipText("Bắt đầu lượt chơi mới từ màn 1");
        leaderboardButton = new RoundedButton("Xếp hạng", themeManager, RoundedButton.Variant.SECONDARY);
        leaderboardButton.setToolTipText("Xem bảng xếp hạng");
        hintButton = new RoundedButton("Gợi ý", themeManager, RoundedButton.Variant.SECONDARY);
        hintButton.setToolTipText("Gợi ý một ô an toàn (H)");
        pauseButton = new RoundedButton("Tạm dừng", themeManager, RoundedButton.Variant.SECONDARY);
        pauseButton.setToolTipText("Tạm dừng / tiếp tục (Space)");
        saveButton = new RoundedButton("Lưu", themeManager, RoundedButton.Variant.SECONDARY);
        saveButton.setToolTipText("Lưu tiến trình (Ctrl+S)");
        themeButton = new RoundedButton("Đổi theme", themeManager, RoundedButton.Variant.SECONDARY);
        themeButton.setToolTipText("Đổi bộ màu giao diện");
        soundButton = new RoundedButton("Âm thanh: Bật", themeManager,
                RoundedButton.Variant.SECONDARY);
        soundButton.setToolTipText("Bật/tắt âm thanh");

        modeBox = new JComboBox<>(MODE_NAMES);
        modeBox.setSelectedIndex(0);
        modeBox.setFont(ThemeManager.font(Font.BOLD, 13));
        modeBox.setOpaque(true);
        modeBox.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected,
                                                          boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                ThemeManager.Palette p = themeManager.getPalette();
                label.setOpaque(true);
                label.setFont(ThemeManager.font(Font.BOLD, 13));
                label.setBorder(new EmptyBorder(4, 10, 4, 10));
                label.setBackground(isSelected ? p.accent() : p.controlBackground());
                label.setForeground(isSelected ? p.buttonText() : p.text());
                return label;
            }
        });

        volumeLabel = new JLabel("Âm lượng");
        volumeLabel.setFont(ThemeManager.font(Font.PLAIN, 11));
        volumeSlider = new JSlider(0, 100, 80);
        volumeSlider.setOpaque(false);
        volumeSlider.setPreferredSize(new Dimension(96, 24));
        volumeSlider.setToolTipText("Chỉnh âm lượng");

        volumeSlider.getAccessibleContext().setAccessibleName("Âm lượng");
        JLabel modeLabel = new JLabel("Chế độ");
        modeLabel.setFont(ThemeManager.font(Font.BOLD, 12));
        modeLabel.setName("modeLabel");

        topBar = new ControlBar(themeManager);
        topBar.add(mineCounter);
        topBar.add(newGameButton);
        topBar.add(timeCounter);
        topBar.add(modeLabel);
        topBar.add(modeBox);

        toolBar = new ControlBar(themeManager);
        toolBar.add(hintButton);
        toolBar.add(pauseButton);
        toolBar.add(saveButton);
        toolBar.add(themeButton);
        toolBar.add(leaderboardButton);
        toolBar.add(soundButton);
        toolBar.add(volumeLabel);
        toolBar.add(volumeSlider);

        JPanel controls = new JPanel(new BorderLayout());
        controls.setOpaque(false);
        controls.add(topBar, BorderLayout.NORTH);
        controls.add(toolBar, BorderLayout.SOUTH);

        // Giữ bàn cờ ở kích thước cố định và căn giữa khu vực còn trống.
        JPanel boardWrap = new JPanel(new java.awt.GridBagLayout());
        boardWrap.setOpaque(false);
        boardWrap.add(boardPanel);

        JPanel boardArea = new JPanel(new BorderLayout());
        boardArea.setOpaque(false);
        boardArea.add(controls, BorderLayout.NORTH);
        boardArea.add(boardWrap, BorderLayout.CENTER);

        contentPane = new JPanel(new BorderLayout());
        contentPane.add(characterPanel, BorderLayout.WEST);
        contentPane.add(boardArea, BorderLayout.CENTER);
        setContentPane(contentPane);

        themeButton.addActionListener(e -> {
            themeManager.nextTheme();
            applyTheme();
        });
        soundButton.addActionListener(e -> {
            soundEnabled = !soundEnabled;
            soundButton.setText(soundEnabled ? "Âm thanh: Bật" : "Âm thanh: Tắt");
            soundButton.revalidate();
            soundButton.repaint();
        });
        installShortcuts();
        applyTheme();
        pack();
        setLocationRelativeTo(null);
    }

    private void installShortcuts() {
        bindShortcut("SPACE", () -> pauseButton.doClick());
        bindShortcut("H", () -> hintButton.doClick());
        bindShortcut("control S", () -> saveButton.doClick());
    }

    private void bindShortcut(String keyStroke, Runnable action) {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(keyStroke), keyStroke);
        getRootPane().getActionMap().put(keyStroke, new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    /** Áp dụng màu/font của theme hiện tại cho toàn bộ khung. */
    public void applyTheme() {
        ThemeManager.Palette p = themeManager.getPalette();
        contentPane.setBackground(p.appBackground());
        characterPanel.setBackground(p.appBackground());
        boardPanel.setBackground(p.boardBackground());
        topBar.repaint();
        toolBar.repaint();
        modeBox.setBackground(p.controlBackground());
        modeBox.setForeground(p.text());
        volumeLabel.setForeground(p.textMuted());
        for (Component component : topBar.getComponents()) {
            if (component instanceof JLabel label) {
                label.setForeground(p.text());
                label.setFont(ThemeManager.font(Font.BOLD, 12));
            }
        }
        themeButton.setToolTipText("Đổi bộ màu giao diện - đang dùng: "
                + themeManager.getThemeName());
        repaint();
    }

    public void setMineCounter(int value) {
        mineCounter.setValue(value);
    }

    public void setTime(int seconds) {
        timeCounter.setValue(Math.max(0, Math.min(999, seconds)));
    }

    public void setHud(int lives, int maxLives, int level, int maxLevel, String modeName) {
        characterPanel.setHud(lives, maxLives, level, maxLevel, modeName);
    }

    public void setExpression(CharacterPanel.Expression expression) {
        characterPanel.setExpression(expression);
    }

    public void setExpression(CharacterPanel.Expression expression, int durationMs) {
        characterPanel.setExpression(expression, durationMs);
    }

    public void rebuildGrid(int rows, int cols) {
        boardPanel.rebuildGrid(rows, cols);
    }

    public void setBoardListener(BoardPanel.Listener listener) {
        boardPanel.setListener(listener);
    }

    public void showCell(int row, int col, BoardPanel.Mark state, boolean mine,
                         boolean exploded, boolean wrongFlag, boolean defused, int adjacent) {
        boardPanel.showCell(row, col, state, mine, exploded, wrongFlag, defused, adjacent, true);
    }

    public void showCell(int row, int col, BoardPanel.Mark state, boolean mine,
                         boolean exploded, boolean wrongFlag, boolean defused, int adjacent,
                         boolean animate) {
        boardPanel.showCell(row, col, state, mine, exploded, wrongFlag, defused, adjacent, animate);
    }

    public void setPaused(boolean paused) {
        boardPanel.setPaused(paused);
        pauseButton.setText(paused ? "Tiếp tục" : "Tạm dừng");
        pauseButton.setToolTipText(paused
                ? "Tiếp tục ván chơi (Space)"
                : "Tạm dừng ván chơi (Space)");
        pauseButton.revalidate();
        pauseButton.repaint();
    }

    public void showHint(int row, int col) {
        boardPanel.showHint(row, col);
    }

    public void clearHint() {
        boardPanel.clearHint();
    }

    public void showExplosion(int row, int col) {
        boardPanel.showExplosion(row, col);
    }

    public int getSelectedModeIndex() {
        return modeBox.getSelectedIndex();
    }

    /** Đặt chế độ đang chọn mà không cần người dùng thao tác trên combo. */
    public void setSelectedModeIndex(int index) {
        if (index >= 0 && index < MODE_NAMES.length) {
            suppressModeEvents = true;
            try {
                modeBox.setSelectedIndex(index);
            } finally {
                suppressModeEvents = false;
            }
        }
    }

    /** Hiển thị tên người chơi trên tiêu đề và bảng nhân vật. */
    public void setPlayerName(String playerName) {
        String name = (playerName != null) ? playerName.trim() : "";
        characterPanel.setPlayerName(name);
        setTitle(name.isEmpty()
                ? "Minesweeper - Khu rừng bom"
                : "Minesweeper - Khu rừng bom - " + name);
    }

    public void addNewGameListener(ActionListener listener) {
        newGameButton.addActionListener(listener);
    }

    public void addDifficultyListener(ActionListener listener) {
        modeBox.addActionListener(e -> {
            if (!suppressModeEvents) {
                listener.actionPerformed(e);
            }
        });
    }

    public void addLeaderboardListener(ActionListener listener) {
        leaderboardButton.addActionListener(listener);
    }

    public void addSoundToggleListener(ActionListener listener) {
        soundButton.addActionListener(listener);
    }

    public void addHintListener(ActionListener listener) {
        hintButton.addActionListener(listener);
    }

    public void addPauseListener(ActionListener listener) {
        pauseButton.addActionListener(listener);
    }

    public void addSaveListener(ActionListener listener) {
        saveButton.addActionListener(listener);
    }

    /** Cập nhật số lượt gợi ý còn lại của màn hiện tại. */
    public void setHintsRemaining(int remaining) {
        int left = Math.max(0, remaining);
        hintButton.setText("Gợi ý (" + left + ")");
        hintButton.setToolTipText(left > 0
                ? "Gợi ý một ô an toàn (H) - còn " + left + " lần"
                : "Đã dùng hết gợi ý ở màn này");
    }

    public void addVolumeListener(ChangeListener listener) {
        volumeSlider.addChangeListener(listener);
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    public double getVolume() {
        return volumeSlider.getValue() / 100.0;
    }

    /** pack() lại và căn giữa màn hình sau khi đổi kích thước bàn cờ. */
    public void refreshLayout() {
        Dimension before = getSize();
        pack();
        if (!isVisible() || !getSize().equals(before)) {
            setLocationRelativeTo(null);
        }
    }

    /**
     * Hiện câu hỏi thách đố. Trả về chỉ số đáp án người chơi chọn,
     * hoặc -1 nếu đóng hộp thoại.
     */
    public int askQuiz(String question, String[] options) {
        JOptionPane pane = new JOptionPane(
                question + "\n\nTrả lời đúng để vô hiệu hóa quả bom!",
                JOptionPane.QUESTION_MESSAGE,
                JOptionPane.DEFAULT_OPTION,
                null,
                options,
                null);
        pane.setFocusable(true);
        JDialog dialog = pane.createDialog(this, "Thử thách gỡ bom");
        removeButtonFocusRing(pane);
        // Đưa tiêu điểm ra khỏi các nút để không nút nào bị tô sáng,
        // tránh người chơi hiểu nhầm đó là gợi ý đáp án đúng.
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                SwingUtilities.invokeLater(pane::requestFocusInWindow);
            }
        });
        dialog.setVisible(true);
        dialog.dispose();

        Object selected = pane.getValue();
        if (selected instanceof Integer index) {
            return (index >= 0 && index < options.length) ? index : -1;
        }
        if (selected != null) {
            for (int i = 0; i < options.length; i++) {
                if (options[i].equals(selected)) {
                    return i;
                }
            }
        }
        return -1;
    }

    /** Tắt viền tiêu điểm của các nút đáp án để không trông như đang gợi ý. */
    private static void removeButtonFocusRing(Component component) {
        if (component instanceof AbstractButton button) {
            button.setFocusPainted(false);
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                removeButtonFocusRing(child);
            }
        }
    }

    public void showInfoMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "Thông báo",
                JOptionPane.INFORMATION_MESSAGE);
    }

    public void showLevelClearMessage(int level, int seconds) {
        JOptionPane.showMessageDialog(this,
                "Qua màn " + level + " sau " + seconds + " giây!\nChuẩn bị sang màn tiếp theo...",
                "Hoàn thành màn",
                JOptionPane.INFORMATION_MESSAGE);
    }

    public void showRunCompleteMessage(String mode, int seconds) {
        JOptionPane.showMessageDialog(this,
                "Chúc mừng! Bạn đã chinh phục 10 màn của chế độ " + mode
                        + " trong " + seconds + " giây ở màn cuối.\n"
                        + "Nhấn Mới để chơi lại từ màn 1.",
                "Phá đảo",
                JOptionPane.INFORMATION_MESSAGE);
    }

    public void showLoseMessage() {
        JOptionPane.showMessageDialog(this,
                "Bạn đã hết 3 mạng!\nNhấn Mới để chơi lại từ màn 1.",
                "Thua cuộc", JOptionPane.ERROR_MESSAGE);
    }

    /** Thanh điều khiển bo góc, nền gradient theo theme rừng. */
    private static final class ControlBar extends JPanel {

        private static final long serialVersionUID = 1L;

        private final ThemeManager theme;

        ControlBar(ThemeManager theme) {
            super(new FlowLayout(FlowLayout.CENTER, 8, 5));
            this.theme = theme;
            setOpaque(false);
            setBorder(new EmptyBorder(2, 10, 4, 10));
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
                g2.setPaint(new GradientPaint(0, 0,
                        p.controlBackground().brighter(), 0, h, p.controlBackground()));
                g2.fill(new RoundRectangle2D.Float(2, 1, w - 4, h - 3, 18, 18));
                g2.setColor(new Color(255, 255, 255, 45));
                g2.draw(new RoundRectangle2D.Float(2.5f, 1.5f, w - 5, h - 4, 18, 18));
                g2.setColor(new Color(p.controlBorder().getRed(),
                        p.controlBorder().getGreen(), p.controlBorder().getBlue(), 150));
                g2.draw(new RoundRectangle2D.Float(2.5f, 1.5f, w - 5, h - 4, 18, 18));
            } finally {
                g2.dispose();
            }
        }
    }

}
