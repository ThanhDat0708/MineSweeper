import controller.StartController;

import javax.swing.SwingUtilities;

/**
 * Điểm khởi động ứng dụng. Mọi thao tác giao diện chạy trên EDT.
 */
public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new StartController().show());
    }
}
