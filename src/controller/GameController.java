package controller;

import model.CellState;
import model.Difficulty;
import model.Game;
import model.GameStatus;
import model.Position;
import quiz.Quiz;
import quiz.QuizBank;
import score.Leaderboard;
import score.ScoreEntry;
import sound.SoundManager;
import view.CellButton;
import view.LeaderboardDialog;
import view.MainFrame;

import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Random;

/**
 * Cầu nối duy nhất giữa view và model.
 * Xử lý click, câu đố khi trúng bom, 3 mạng và chuyển màn.
 */
public class GameController {

    private static final int MAX_TIME = 999;

    private final Game game;
    private final MainFrame view;
    private final Timer timer;
    private final SoundManager sound;
    private final QuizBank quizBank;
    private final Leaderboard leaderboard;
    private final String playerName;

    private Difficulty currentDifficulty;
    private int elapsedSeconds;
    private int totalRunSeconds;
    private boolean timerStarted;

    public GameController(Game game, MainFrame view, String playerName,
                          Leaderboard leaderboard, Difficulty initialDifficulty) {
        this.game = game;
        this.view = view;
        this.playerName = ScoreEntry.sanitizeName(playerName);
        this.leaderboard = (leaderboard != null) ? leaderboard : new Leaderboard();
        this.timer = new Timer(1000, e -> onTimerTick());
        this.sound = new SoundManager();
        this.quizBank = new QuizBank(new Random());

        Difficulty start = (initialDifficulty != null) ? initialDifficulty : selectedDifficulty();
        // Đặt combo trước khi gắn listener để không kích hoạt tạo ván hai lần.
        view.setSelectedModeIndex(start.ordinal());
        view.setPlayerName(this.playerName);

        view.addNewGameListener(e -> startRun(selectedDifficulty()));
        view.addDifficultyListener(e -> startRun(selectedDifficulty()));
        view.addLeaderboardListener(e -> showLeaderboard());
        view.addSoundToggleListener(e -> sound.setEnabled(view.isSoundEnabled()));
        sound.setEnabled(view.isSoundEnabled());

        startRun(start);
    }

    public void dispose() {
        sound.dispose();
    }

    private Difficulty selectedDifficulty() {
        int index = view.getSelectedModeIndex();
        Difficulty[] values = Difficulty.values();
        if (index < 0 || index >= values.length) {
            return Difficulty.BEGINNER;
        }
        return values[index];
    }

    /** Bắt đầu một lượt chơi mới từ màn 1 của chế độ đang chọn (đầy 3 mạng). */
    private void startRun(Difficulty difficulty) {
        stopTimer();
        elapsedSeconds = 0;
        totalRunSeconds = 0;
        timerStarted = false;

        currentDifficulty = difficulty;
        game.newGame(difficulty, 1);
        refreshBoard();
    }

    /** Sang màn kế tiếp, giữ nguyên số mạng còn lại. */
    private void advanceToNextLevel() {
        stopTimer();
        elapsedSeconds = 0;
        timerStarted = false;
        game.advanceLevel();
        refreshBoard();
    }

    private void refreshBoard() {
        view.rebuildGrid(game.getRows(), game.getCols());
        attachMouseHandler();
        updateHud();
        view.setMineCounter(game.getRemainingMines());
        view.setTime(0);
        view.refreshLayout();
    }

    private void updateHud() {
        view.setHud(game.getLives(), game.getMaxLives(), game.getLevel(),
                Difficulty.MAX_LEVEL, currentDifficulty.getDisplayName());
    }

    private void attachMouseHandler() {
        // Chuột trái mở ô, chuột phải cắm/gỡ cờ (dùng MouseAdapter, không dùng ActionListener).
        MouseAdapter handler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                boolean left = SwingUtilities.isLeftMouseButton(e);
                boolean right = SwingUtilities.isRightMouseButton(e);
                if (!left && !right) {
                    return;
                }
                CellButton button = (CellButton) e.getSource();
                if (right) {
                    onRightClick(button.getRow(), button.getCol());
                } else {
                    onLeftClick(button.getRow(), button.getCol());
                }
            }
        };
        view.setCellMouseListener(handler);
    }

    private void onLeftClick(int row, int col) {
        if (isGameOver() || game.isQuizPending()) {
            return;
        }
        if (game.getCellState(row, col) != CellState.HIDDEN) {
            return;
        }

        List<Position> changed = game.reveal(row, col);

        // Trúng bom: mở hộp thoại câu đố thay vì thua ngay.
        if (game.isQuizPending()) {
            handleQuiz();
            return;
        }
        if (changed.isEmpty()) {
            return;
        }

        if (game.getStatus() == GameStatus.PLAYING && !timerStarted) {
            startTimer();
        }

        if (game.getStatus() == GameStatus.LOST) {
            handleLose();
            return;
        }
        if (game.getStatus() == GameStatus.WON) {
            handleLevelWin();
            return;
        }

        sound.playReveal();
        for (Position position : changed) {
            showCell(position.row(), position.col());
        }
        view.setMineCounter(game.getRemainingMines());
    }

    /**
     * Hiện câu đố cho quả bom đang chờ. Đúng thì gỡ bom, sai thì mất 1 mạng.
     */
    private void handleQuiz() {
        sound.playAlert();

        boolean timerWasRunning = timer.isRunning();
        if (timerWasRunning) {
            timer.stop(); // tạm dừng đồng hồ trong lúc trả lời
        }

        Quiz quiz = quizBank.nextForLevel(game.getLevel());
        int chosen = view.askQuiz(quiz.question(), quiz.options());
        boolean correct = chosen == quiz.correctIndex();

        List<Position> changed = game.resolveQuiz(correct);
        for (Position position : changed) {
            showCell(position.row(), position.col());
        }
        view.setMineCounter(game.getRemainingMines());
        updateHud();

        if (correct) {
            sound.playDefuse();
            view.showInfoMessage("Chính xác! Quả bom đã được vô hiệu hóa.");
        } else {
            sound.playExplosion();
            if (game.getStatus() == GameStatus.LOST) {
                handleLose();
                return;
            }
            view.showInfoMessage("Sai rồi! Bom nổ và bạn còn " + game.getLives() + " mạng.");
        }

        if (timerWasRunning && !isGameOver()) {
            timer.start();
        }
    }

    private void onRightClick(int row, int col) {
        if (isGameOver() || game.isQuizPending()) {
            return;
        }
        boolean changed = game.toggleFlag(row, col);
        if (!changed) {
            return;
        }
        sound.playFlag();
        showCell(row, col);
        view.setMineCounter(game.getRemainingMines());
    }

    private void handleLose() {
        stopTimer();
        showAllCells();
        view.setMineCounter(game.getRemainingMines());
        updateHud();
        view.showLoseMessage();
    }

    private void handleLevelWin() {
        stopTimer();
        showAllCells();
        view.setMineCounter(game.getRemainingMines());
        updateHud();
        sound.playWin();

        int clearedLevel = game.getLevel();
        int levelSeconds = elapsedSeconds;
        totalRunSeconds += levelSeconds;
        recordScore(clearedLevel);

        if (clearedLevel >= Difficulty.MAX_LEVEL) {
            view.showRunCompleteMessage(currentDifficulty.getDisplayName(), levelSeconds);
            showLeaderboard();
        } else {
            view.showLevelClearMessage(clearedLevel, levelSeconds);
            advanceToNextLevel();
        }
    }

    /**
     * Ghi thành tích sau mỗi màn đã qua: số màn, tổng thời gian cộng dồn,
     * chế độ và số bom của màn cao nhất đạt được.
     */
    private void recordScore(int clearedLevel) {
        ScoreEntry entry = new ScoreEntry(playerName,
                currentDifficulty.getDisplayName(), clearedLevel, totalRunSeconds,
                currentDifficulty.getMinesForLevel(clearedLevel), System.currentTimeMillis());
        leaderboard.submit(entry);
    }

    private void showLeaderboard() {
        LeaderboardDialog.show(view, leaderboard.toDisplayRows(Leaderboard.MAX_ROWS));
    }

    private void showCell(int row, int col) {
        CellState state = game.getCellState(row, col);
        boolean over = isGameOver();
        boolean mine = game.isMine(row, col) && (over || state == CellState.REVEALED);
        view.showCell(row, col, toViewState(state), mine,
                game.isExploded(row, col), game.isWrongFlag(row, col),
                game.isDefused(row, col), game.getAdjacentMines(row, col));
    }

    private void showAllCells() {
        for (int r = 0; r < game.getRows(); r++) {
            for (int c = 0; c < game.getCols(); c++) {
                showCell(r, c);
            }
        }
    }

    private static CellButton.State toViewState(CellState state) {
        switch (state) {
            case REVEALED:
                return CellButton.State.REVEALED;
            case FLAGGED:
                return CellButton.State.FLAGGED;
            case HIDDEN:
            default:
                return CellButton.State.HIDDEN;
        }
    }

    private void startTimer() {
        if (timerStarted) {
            return;
        }
        timerStarted = true;
        if (!timer.isRunning()) {
            timer.start();
        }
    }

    private void stopTimer() {
        timer.stop();
        timerStarted = false;
    }

    private void onTimerTick() {
        if (elapsedSeconds >= MAX_TIME) {
            timer.stop();
            return;
        }
        elapsedSeconds++;
        view.setTime(elapsedSeconds);
        if (elapsedSeconds >= MAX_TIME) {
            timer.stop();
        }
    }

    private boolean isGameOver() {
        GameStatus status = game.getStatus();
        return status == GameStatus.WON || status == GameStatus.LOST;
    }
}
