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
import view.BoardPanel;
import view.CharacterPanel.Expression;
import view.LeaderboardDialog;
import view.MainFrame;

import javax.swing.Timer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;

/**
 * Cầu nối duy nhất giữa view và model.
 * Xử lý click, chord, đánh dấu "?", gợi ý, tạm dừng, lưu/khôi phục,
 * câu đố khi trúng bom, 3 mạng và chuyển màn.
 */
public class GameController {

    private static final int MAX_TIME = 999;
    private static final Path SAVE_FILE = Paths.get("savegame.dat");

    private final GameLogic logic;
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
    private boolean pauseHeldTimer;

    public GameController(Game game, MainFrame view, String playerName,
                          Leaderboard leaderboard, Difficulty initialDifficulty) {
        this.logic = new GameLogic(game);
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

        view.setBoardListener(new BoardPanel.Listener() {
            @Override
            public void onLeftClick(int row, int col) {
                onLeftClickInternal(row, col);
            }

            @Override
            public void onRightClick(int row, int col) {
                onRightClickInternal(row, col);
            }

            @Override
            public void onChord(int row, int col) {
                onChordInternal(row, col);
            }
        });

        view.addNewGameListener(e -> startRun(selectedDifficulty()));
        view.addDifficultyListener(e -> startRun(selectedDifficulty()));
        view.addLeaderboardListener(e -> showLeaderboard());
        view.addSoundToggleListener(e -> sound.setEnabled(view.isSoundEnabled()));
        view.addVolumeListener(e -> sound.setVolume(view.getVolume()));
        view.addHintListener(e -> showHint());
        view.addPauseListener(e -> togglePause());
        view.addSaveListener(e -> saveProgress());
        view.addLoadListener(e -> loadProgress());

        sound.setEnabled(view.isSoundEnabled());
        sound.setVolume(view.getVolume());

        startRun(start);
    }

    public void dispose() {
        if (logic.getStatus() == GameStatus.PLAYING && !logic.isQuizPending()) {
            logic.save(SAVE_FILE, elapsedSeconds, totalRunSeconds, timerStarted);
        }
        // Dừng đồng hồ trước khi đóng cửa sổ để không còn tick sau khi dispose.
        timer.stop();
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
        pauseHeldTimer = false;

        currentDifficulty = difficulty;
        logic.newGame(difficulty, 1);
        refreshBoard();
        view.setExpression(Expression.NORMAL, 0);
    }

    /** Sang màn kế tiếp, giữ nguyên số mạng còn lại. */
    private void advanceToNextLevel() {
        stopTimer();
        elapsedSeconds = 0;
        timerStarted = false;
        pauseHeldTimer = false;
        logic.advanceLevel();
        refreshBoard();
        view.setExpression(Expression.NORMAL, 0);
    }

    private void refreshBoard() {
        view.rebuildGrid(logic.getRows(), logic.getCols());
        view.setPaused(false);
        view.clearHint();
        updateHud();
        view.setMineCounter(logic.getRemainingMines());
        view.setTime(0);
        view.refreshLayout();
    }

    private void updateHud() {
        view.setHud(logic.getLives(), logic.getMaxLives(), logic.getLevel(),
                Difficulty.MAX_LEVEL, currentDifficulty.getDisplayName());
    }

    private boolean blocked() {
        return logic.isPaused() || isGameOver() || logic.isQuizPending();
    }

    private void onLeftClickInternal(int row, int col) {
        if (blocked()) {
            return;
        }
        CellState state = logic.getCellState(row, col);
        if (state == CellState.FLAGGED || state == CellState.REVEALED) {
            return;
        }

        List<Position> changed = logic.reveal(row, col);

        // Trúng bom: mở hộp thoại câu đố thay vì thua ngay.
        if (logic.isQuizPending()) {
            handleQuiz();
            return;
        }
        if (changed.isEmpty()) {
            return;
        }

        if (logic.getStatus() == GameStatus.PLAYING && !timerStarted) {
            startTimer();
        }

        if (logic.getStatus() == GameStatus.LOST) {
            handleLose();
            return;
        }
        if (logic.getStatus() == GameStatus.WON) {
            handleLevelWin();
            return;
        }

        sound.playReveal();
        view.clearHint();
        int maxAdjacent = 0;
        for (Position position : changed) {
            showCell(position.row(), position.col(), true);
            if (!logic.isMine(position.row(), position.col())) {
                maxAdjacent = Math.max(maxAdjacent,
                        logic.getAdjacentMines(position.row(), position.col()));
            }
        }
        view.setMineCounter(logic.getRemainingMines());
        view.setExpression(maxAdjacent >= 5 ? Expression.SCARED : Expression.HAPPY);
    }

    private void onRightClickInternal(int row, int col) {
        if (blocked()) {
            return;
        }
        CellState newState = logic.cycleMark(row, col);
        if (newState == null) {
            return;
        }
        if (newState == CellState.FLAGGED) {
            sound.playFlag();
        } else {
            sound.playMark();
        }
        showCell(row, col, false);
        view.clearHint();
        view.setMineCounter(logic.getRemainingMines());
        view.setExpression(Expression.NORMAL, 0);
    }

    private void onChordInternal(int row, int col) {
        if (blocked()) {
            return;
        }
        List<Position> changed = logic.chord(row, col);

        // Chord có thể mở vài ô an toàn rồi mới chạm bom. Cần vẽ các ô đó
        // ngay cả khi câu đố đang chờ, tránh ô đã mở nhưng vẫn hiện như ẩn.
        if (!changed.isEmpty()) {
            for (Position position : changed) {
                showCell(position.row(), position.col(), true);
            }
            view.setMineCounter(logic.getRemainingMines());
        }

        if (logic.isQuizPending()) {
            handleQuiz();
            return;
        }
        if (changed.isEmpty()) {
            return;
        }
        if (logic.getStatus() == GameStatus.PLAYING && !timerStarted) {
            startTimer();
        }
        if (logic.getStatus() == GameStatus.LOST) {
            handleLose();
            return;
        }
        if (logic.getStatus() == GameStatus.WON) {
            handleLevelWin();
            return;
        }

        sound.playReveal();
        view.clearHint();
        int maxAdjacent = 0;
        for (Position position : changed) {
            if (!logic.isMine(position.row(), position.col())) {
                maxAdjacent = Math.max(maxAdjacent,
                        logic.getAdjacentMines(position.row(), position.col()));
            }
        }
        view.setExpression(maxAdjacent >= 5 ? Expression.SCARED : Expression.HAPPY);
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

        Position pending = logic.getPendingMine();
        Quiz quiz = quizBank.nextForLevel(logic.getLevel());
        int chosen = view.askQuiz(quiz.question(), quiz.options());
        boolean correct = chosen == quiz.correctIndex();

        if (!correct && pending != null) {
            view.showExplosion(pending.row(), pending.col());
            sound.playExplosion();
            view.setExpression(Expression.CRYING, 0);
        }

        List<Position> changed = logic.resolveQuiz(correct);
        for (Position position : changed) {
            showCell(position.row(), position.col(), true);
        }
        view.setMineCounter(logic.getRemainingMines());
        updateHud();

        if (correct) {
            sound.playDefuse();
            view.setExpression(Expression.HAPPY);
            view.showInfoMessage("Chính xác! Quả bom đã được vô hiệu hóa.");
        } else {
            if (logic.getStatus() == GameStatus.LOST) {
                handleLose();
                return;
            }
            view.showInfoMessage("Sai rồi! Bom nổ và bạn còn " + logic.getLives() + " mạng.");
        }

        if (timerWasRunning && !isGameOver()) {
            timer.start();
            timerStarted = true;
        }
    }

    private void handleLose() {
        stopTimer();
        pauseHeldTimer = false;
        showAllCells(false);
        view.setMineCounter(logic.getRemainingMines());
        updateHud();
        view.setPaused(false);
        view.setExpression(Expression.CRYING, 0);
        sound.playLose();
        view.showLoseMessage();
    }

    private void handleLevelWin() {
        stopTimer();
        pauseHeldTimer = false;
        showAllCells(false);
        view.setMineCounter(logic.getRemainingMines());
        updateHud();
        view.setPaused(false);
        view.setExpression(Expression.WIN, 0);
        sound.playWin();

        int clearedLevel = logic.getLevel();
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

    private void togglePause() {
        if (isGameOver() || logic.isQuizPending()) {
            return;
        }
        boolean paused = logic.togglePause();
        view.setPaused(paused);
        if (paused) {
            pauseHeldTimer = timer.isRunning();
            timer.stop();
        } else {
            if (pauseHeldTimer && !isGameOver()) {
                timer.start();
                timerStarted = true;
            }
            pauseHeldTimer = false;
        }
        sound.playPause();
    }

    private void showHint() {
        if (blocked()) {
            return;
        }
        Position hint = logic.findHint();
        if (hint == null) {
            view.showInfoMessage("Không còn ô an toàn nào để gợi ý.");
            return;
        }
        view.showHint(hint.row(), hint.col());
        view.setExpression(Expression.NORMAL, 0);
        sound.playHint();
    }

    private void saveProgress() {
        boolean saved = logic.save(SAVE_FILE, elapsedSeconds, totalRunSeconds, timerStarted);
        if (saved) {
            sound.playMark();
            view.showInfoMessage("Đã lưu tiến trình vào " + SAVE_FILE.getFileName() + ".");
        } else {
            view.showInfoMessage("Không thể lưu tiến trình.");
        }
    }

    private void loadProgress() {
        if (!logic.load(SAVE_FILE)) {
            view.showInfoMessage("Không tìm thấy tiến trình đã lưu hoặc file bị hỏng.");
            return;
        }

        stopTimer();
        pauseHeldTimer = false;
        timerStarted = false;
        elapsedSeconds = logic.getLoadedElapsedSeconds();
        totalRunSeconds = logic.getLoadedTotalSeconds();
        currentDifficulty = logic.getDifficulty();

        view.setSelectedModeIndex(currentDifficulty.ordinal());
        view.rebuildGrid(logic.getRows(), logic.getCols());
        view.setPaused(false);
        view.clearHint();
        updateHud();
        view.setMineCounter(logic.getRemainingMines());
        view.setTime(elapsedSeconds);
        showAllCells(false);
        view.setExpression(Expression.NORMAL, 0);
        view.refreshLayout();

        if (logic.wasLoadedTimerStarted()
                && logic.getStatus() == GameStatus.PLAYING && !isGameOver()) {
            timerStarted = true;
            timer.start();
        }
        sound.playMark();
        view.showInfoMessage("Đã tải tiến trình đã lưu.");
    }

    private void showCell(int row, int col, boolean animate) {
        CellState state = logic.getCellState(row, col);
        boolean over = isGameOver();
        boolean mine = logic.isMine(row, col) && (over || state == CellState.REVEALED);
        view.showCell(row, col, toViewMark(state), mine,
                logic.isExploded(row, col), logic.isWrongFlag(row, col),
                logic.isDefused(row, col), logic.getAdjacentMines(row, col), animate);
    }

    private void showAllCells(boolean animate) {
        for (int r = 0; r < logic.getRows(); r++) {
            for (int c = 0; c < logic.getCols(); c++) {
                showCell(r, c, animate);
            }
        }
    }

    private static BoardPanel.Mark toViewMark(CellState state) {
        switch (state) {
            case REVEALED:
                return BoardPanel.Mark.REVEALED;
            case FLAGGED:
                return BoardPanel.Mark.FLAGGED;
            case QUESTION:
                return BoardPanel.Mark.QUESTION;
            case HIDDEN:
            default:
                return BoardPanel.Mark.HIDDEN;
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
        GameStatus status = logic.getStatus();
        return status == GameStatus.WON || status == GameStatus.LOST;
    }
}
