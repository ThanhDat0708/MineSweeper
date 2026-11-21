package controller;

import model.CellState;
import model.Difficulty;
import model.Game;
import model.GameStatus;
import model.Position;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Lớp xử lý trung gian giữa controller giao diện và model.
 * Bọc {@link Game}, bổ sung trạng thái tạm dừng và lưu/khôi phục tiến trình.
 */
public final class GameLogic {

    private Game game;
    private boolean paused;

    private int loadedElapsedSeconds;
    private int loadedTotalSeconds;
    private boolean loadedTimerStarted;

    public GameLogic(Game game) {
        if (game == null) {
            throw new IllegalArgumentException("Game không được null");
        }
        this.game = game;
    }

    public Game getGame() {
        return game;
    }

    public void newGame(Difficulty difficulty) {
        newGame(difficulty, 1);
    }

    public void newGame(Difficulty difficulty, int level) {
        game.newGame(difficulty, level);
        paused = false;
    }

    public void advanceLevel() {
        game.advanceLevel();
        paused = false;
    }

    public List<Position> reveal(int row, int col) {
        return game.reveal(row, col);
    }

    public List<Position> resolveQuiz(boolean correct) {
        return game.resolveQuiz(correct);
    }

    public List<Position> chord(int row, int col) {
        return game.chord(row, col);
    }

    public CellState cycleMark(int row, int col) {
        return game.cycleMark(row, col);
    }

    public Position findHint() {
        return game.findHint();
    }

    public boolean togglePause() {
        if (game.getStatus() == GameStatus.WON || game.getStatus() == GameStatus.LOST) {
            return false;
        }
        paused = !paused;
        return paused;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public int getRows() {
        return game.getRows();
    }

    public int getCols() {
        return game.getCols();
    }

    public int getRemainingMines() {
        return game.getRemainingMines();
    }

    public GameStatus getStatus() {
        return game.getStatus();
    }

    public int getLives() {
        return game.getLives();
    }

    public int getMaxLives() {
        return game.getMaxLives();
    }

    public int getLevel() {
        return game.getLevel();
    }

    public Difficulty getDifficulty() {
        return game.getDifficulty();
    }

    public boolean isQuizPending() {
        return game.isQuizPending();
    }

    public Position getPendingMine() {
        return game.getPendingMine();
    }

    public CellState getCellState(int row, int col) {
        return game.getCellState(row, col);
    }

    public int getAdjacentMines(int row, int col) {
        return game.getAdjacentMines(row, col);
    }

    public boolean isMine(int row, int col) {
        return game.isMine(row, col);
    }

    public boolean isExploded(int row, int col) {
        return game.isExploded(row, col);
    }

    public boolean isWrongFlag(int row, int col) {
        return game.isWrongFlag(row, col);
    }

    public boolean isDefused(int row, int col) {
        return game.isDefused(row, col);
    }

    /**
     * Lưu toàn bộ tiến trình hiện tại (model + đồng hồ) ra file nhị phân.
     */
    public boolean save(Path file, int elapsedSeconds, int totalRunSeconds,
                        boolean timerStarted) {
        if (file == null) {
            return false;
        }
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (ObjectOutputStream out = new ObjectOutputStream(
                    Files.newOutputStream(file))) {
                out.writeObject(new SaveData(game, elapsedSeconds,
                        totalRunSeconds, timerStarted));
            }
            return true;
        } catch (IOException | RuntimeException ex) {
            return false;
        }
    }

    /**
     * Đọc lại tiến trình đã lưu. Trả false nếu file hỏng hoặc không tồn tại.
     */
    public boolean load(Path file) {
        if (file == null || !Files.exists(file)) {
            return false;
        }
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(file))) {
            Object object = in.readObject();
            if (!(object instanceof SaveData data) || data.game == null) {
                return false;
            }
            this.game = data.game;
            this.paused = false;
            this.loadedElapsedSeconds = Math.max(0, data.elapsedSeconds);
            this.loadedTotalSeconds = Math.max(0, data.totalRunSeconds);
            this.loadedTimerStarted = data.timerStarted;
            return true;
        } catch (IOException | ClassNotFoundException | RuntimeException ex) {
            return false;
        }
    }

    public int getLoadedElapsedSeconds() {
        return loadedElapsedSeconds;
    }

    public int getLoadedTotalSeconds() {
        return loadedTotalSeconds;
    }

    public boolean wasLoadedTimerStarted() {
        return loadedTimerStarted;
    }

    /** Dữ liệu lưu kèm phiên bản để tránh đọc nhầm file lạ. */
    private static final class SaveData implements Serializable {

        private static final long serialVersionUID = 3L;

        final Game game;
        final int elapsedSeconds;
        final int totalRunSeconds;
        final boolean timerStarted;

        SaveData(Game game, int elapsedSeconds, int totalRunSeconds, boolean timerStarted) {
            this.game = game;
            this.elapsedSeconds = elapsedSeconds;
            this.totalRunSeconds = totalRunSeconds;
            this.timerStarted = timerStarted;
        }
    }
}
