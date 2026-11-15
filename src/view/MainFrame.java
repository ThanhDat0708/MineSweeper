package view;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;

/**
 * Cửa sổ chính: bảng nhân vật bên trái, thanh điều khiển và bàn cờ bên phải.
 * View chỉ hiển thị, mọi thao tác được controller xử lý.
 */
public class MainFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    /** Tên chế độ hiển thị; chỉ số khớp thứ tự Difficulty.values() ở tầng model. */
    private static final String[] MODE_NAMES = {"Beginner", "Intermediate", "Expert"};

    private final CharacterPanel characterPanel;
    private final GamePanel gamePanel;
    private final JLabel mineLabel;
    private final JLabel timeLabel;
    private final JButton newGameButton;
    private final JComboBox<String> modeBox;
    private final JCheckBox soundBox;

    public MainFrame() {
        super("Minesweeper - Khu rừng bom");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        characterPanel = new CharacterPanel();
        gamePanel = new GamePanel();
        mineLabel = createDisplayLabel("000");
        mineLabel.setToolTipText("Số bom còn lại");
        timeLabel = createDisplayLabel("000");
        timeLabel.setToolTipText("Thời gian (giây)");

        newGameButton = new JButton("New Game");
        modeBox = new JComboBox<>(MODE_NAMES);
        modeBox.setSelectedIndex(0);
        soundBox = new JCheckBox("Âm thanh", true);

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 6));
        topBar.add(mineLabel);
        topBar.add(newGameButton);
        topBar.add(timeLabel);
        topBar.add(modeBox);
        topBar.add(soundBox);

        JPanel boardArea = new JPanel(new BorderLayout());
        boardArea.add(topBar, BorderLayout.NORTH);
        boardArea.add(gamePanel, BorderLayout.CENTER);

        JPanel content = new JPanel(new BorderLayout());
        content.add(characterPanel, BorderLayout.WEST);
        content.add(boardArea, BorderLayout.CENTER);
        setContentPane(content);

        pack();
        setLocationRelativeTo(null);
    }

    private static JLabel createDisplayLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font(Font.MONOSPACED, Font.BOLD, 18));
        label.setOpaque(true);
        label.setBackground(Color.WHITE);
        label.setForeground(new Color(200, 0, 0));
        label.setPreferredSize(new Dimension(64, 30));
        Border inner = BorderFactory.createEmptyBorder(2, 8, 2, 8);
        Border outer = BorderFactory.createLineBorder(new Color(130, 130, 130));
        label.setBorder(BorderFactory.createCompoundBorder(outer, inner));
        return label;
    }

    public void setMineCounter(int value) {
        mineLabel.setText(String.format("%03d", value));
    }

    public void setTime(int seconds) {
        int clamped = Math.max(0, Math.min(999, seconds));
        timeLabel.setText(String.format("%03d", clamped));
    }

    public void setHud(int lives, int maxLives, int level, int maxLevel, String modeName) {
        characterPanel.setHud(lives, maxLives, level, maxLevel, modeName);
    }

    public void rebuildGrid(int rows, int cols) {
        gamePanel.rebuildGrid(rows, cols);
    }

    public void setCellMouseListener(MouseListener listener) {
        gamePanel.setCellMouseListener(listener);
    }

    public void showCell(int row, int col, CellButton.State state, boolean mine,
                         boolean exploded, boolean wrongFlag, boolean defused) {
        gamePanel.showCell(row, col, state, mine, exploded, wrongFlag, defused);
    }

    public int getSelectedModeIndex() {
        return modeBox.getSelectedIndex();
    }

    public void addNewGameListener(ActionListener listener) {
        newGameButton.addActionListener(listener);
    }

    public void addDifficultyListener(ActionListener listener) {
        modeBox.addActionListener(listener);
    }

    public void addSoundToggleListener(ActionListener listener) {
        soundBox.addActionListener(listener);
    }

    public boolean isSoundEnabled() {
        return soundBox.isSelected();
    }

    /** pack() lại và căn giữa màn hình sau khi đổi kích thước bàn cờ. */
    public void refreshLayout() {
        pack();
        setLocationRelativeTo(null);
    }

    /**
     * Hiện câu hỏi thách đố. Trả về chỉ số đáp án người chơi chọn,
     * hoặc -1 nếu đóng hộp thoại.
     */
    public int askQuiz(String question, String[] options) {
        return JOptionPane.showOptionDialog(this,
                question + "\n\nTrả lời đúng để vô hiệu hóa quả bom!",
                "Thử thách gỡ bom",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options.length > 0 ? options[0] : null);
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
                        + "Nhấn New Game để chơi lại từ màn 1.",
                "Phá đảo",
                JOptionPane.INFORMATION_MESSAGE);
    }

    public void showLoseMessage() {
        JOptionPane.showMessageDialog(this,
                "Bạn đã hết 3 mạng!\nNhấn New Game để chơi lại từ màn 1.",
                "Thua cuộc", JOptionPane.ERROR_MESSAGE);
    }
}
