import controller.StartController;
import view.ThemeManager;

import javax.swing.SwingUtilities;

/**
 * Điểm khởi động ứng dụng. Mọi thao tác giao diện chạy trên EDT.
 * FlatLaf được cài nếu có trong classpath, nếu không sẽ dùng Nimbus/System.
 */
public class Main {

    public static void main(String[] args) {
        ThemeManager.installLookAndFeel();
        SwingUtilities.invokeLater(() -> new StartController().show());
    }
}
