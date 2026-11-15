import controller.GameController;
import model.Difficulty;
import model.Game;
import view.MainFrame;

import javax.swing.SwingUtilities;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Random;

/**
 * Điểm khởi động ứng dụng. Mọi thao tác giao diện chạy trên EDT.
 */
public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Game game = new Game(Difficulty.BEGINNER, new Random());
            MainFrame frame = new MainFrame();
            GameController controller = new GameController(game, frame);
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    controller.dispose();
                }
            });
            frame.setVisible(true);
        });
    }
}
