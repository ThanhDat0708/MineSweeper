package controller;

import model.Difficulty;
import model.Game;
import score.Leaderboard;
import view.LeaderboardDialog;
import view.MainFrame;
import view.StartFrame;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Random;

/**
 * Điều khiển màn hình bắt đầu: xác thực tên, chọn độ khó,
 * mở ván chơi mới và hiển thị bảng xếp hạng.
 */
public class StartController {

    private final StartFrame view;
    private final Leaderboard leaderboard;

    public StartController() {
        this.leaderboard = new Leaderboard();
        this.leaderboard.load();
        this.view = new StartFrame();

        view.addStartListener(e -> onStart());
        view.addLeaderboardListener(e -> onShowLeaderboard());
    }

    public void show() {
        view.setVisible(true);
        view.focusNameField();
    }

    private void onStart() {
        String name = view.getPlayerName();
        if (name == null || name.trim().isEmpty()) {
            view.showErrorMessage("Vui lòng nhập tên người chơi trước khi bắt đầu.");
            view.focusNameField();
            return;
        }

        Difficulty difficulty = selectedDifficulty();
        Game game = new Game(difficulty, new Random());
        MainFrame frame = new MainFrame();
        GameController controller = new GameController(game, frame, name, leaderboard, difficulty);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                controller.dispose();
            }
        });

        frame.setVisible(true);
        view.dispose();
    }

    private void onShowLeaderboard() {
        LeaderboardDialog.show(view, leaderboard.toDisplayRows(Leaderboard.MAX_ROWS));
    }

    private Difficulty selectedDifficulty() {
        int index = view.getSelectedModeIndex();
        Difficulty[] values = Difficulty.values();
        if (index < 0 || index >= values.length) {
            return Difficulty.BEGINNER;
        }
        return values[index];
    }
}
