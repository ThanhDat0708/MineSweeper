package view;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;

/**
 * Cửa sổ bảng xếp hạng. Chỉ nhận các dòng text đã được controller chuẩn bị,
 * không phụ thuộc trực tiếp vào tầng dữ liệu.
 */
public class LeaderboardDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private static final String[] COLUMNS =
            {"Hạng", "Người chơi", "Chế độ", "Màn", "Số bom", "Thời gian"};

    private LeaderboardDialog(Window owner, String[][] rows) {
        super(owner, "Bảng xếp hạng", Dialog.ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        JLabel title = new JLabel("BẢNG XẾP HẠNG", JLabel.CENTER);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        title.setForeground(new Color(35, 95, 55));
        title.setBorder(BorderFactory.createEmptyBorder(14, 12, 10, 12));

        DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        if (rows != null) {
            for (String[] row : rows) {
                model.addRow(row);
            }
        }

        JTable table = new JTable(model);
        table.setRowHeight(24);
        table.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);

        DefaultTableCellRenderer centered = new DefaultTableCellRenderer();
        centered.setHorizontalAlignment(JLabel.CENTER);
        for (int i : new int[]{0, 2, 3, 4, 5}) {
            table.getColumnModel().getColumn(i).setCellRenderer(centered);
        }

        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(560, 320));

        JPanel center = new JPanel(new BorderLayout());
        center.add(title, BorderLayout.NORTH);
        center.add(scroll, BorderLayout.CENTER);
        if (model.getRowCount() == 0) {
            JLabel empty = new JLabel("Chưa có thành tích nào. Hãy chơi để ghi tên lên bảng!",
                    JLabel.CENTER);
            empty.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 13));
            empty.setForeground(new Color(110, 110, 110));
            empty.setBorder(BorderFactory.createEmptyBorder(8, 12, 4, 12));
            center.add(empty, BorderLayout.SOUTH);
        }

        JButton closeButton = new JButton("Đóng");
        closeButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        closeButton.addActionListener(e -> dispose());
        JPanel south = new JPanel();
        south.setBorder(BorderFactory.createEmptyBorder(8, 12, 12, 12));
        south.add(closeButton);

        setLayout(new BorderLayout());
        add(center, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(closeButton);

        pack();
        setLocationRelativeTo(owner);
    }

    /** Mở hộp thoại bảng xếp hạng modal với dữ liệu đã chuẩn bị sẵn. */
    public static void show(Component parent, String[][] rows) {
        Window owner = (parent instanceof Window)
                ? (Window) parent
                : SwingUtilities.getWindowAncestor(parent);
        new LeaderboardDialog(owner, rows).setVisible(true);
    }
}
