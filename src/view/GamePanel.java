package view;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.MouseListener;

/**
 * Khu vực bàn cờ: lưới các CellButton. Chỉ hiển thị theo yêu cầu của controller,
 * không chứa logic dò mìn.
 */
public class GamePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private CellButton[][] buttons = new CellButton[0][0];
    private int rows;
    private int cols;

    public GamePanel() {
        setLayout(new GridLayout(1, 1));
        setBackground(new Color(40, 80, 45));
        setBorder(new EmptyBorder(4, 4, 4, 4));
    }

    public void rebuildGrid(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        removeAll();
        setLayout(new GridLayout(rows, cols, 0, 0));
        buttons = new CellButton[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                CellButton button = new CellButton(r, c);
                buttons[r][c] = button;
                add(button);
            }
        }
        revalidate();
        repaint();
    }

    public void setCellMouseListener(MouseListener listener) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                buttons[r][c].addMouseListener(listener);
            }
        }
    }

    public void showCell(int row, int col, CellButton.State state, boolean mine,
                         boolean exploded, boolean wrongFlag, boolean defused, int adjacent) {
        if (row < 0 || row >= rows || col < 0 || col >= cols) {
            return;
        }
        buttons[row][col].setCellData(state, mine, exploded, wrongFlag, defused, adjacent);
    }
}
