package model;

import java.util.List;
import java.util.Random;

/**
 * Facade duy nhất mà tầng controller được phép gọi.
 * Quản lý chế độ, màn hiện tại và bàn cờ của màn đó.
 */
public final class Game {

    private final Random random;
    private Difficulty difficulty;
    private int level;
    private Board board;

    public Game(Difficulty difficulty, Random random) {
        if (difficulty == null) {
            throw new IllegalArgumentException("Difficulty không được null");
        }
        this.random = (random != null) ? random : new Random();
        this.difficulty = difficulty;
        this.level = 1;
        this.board = createBoard(difficulty, level);
    }

    public void newGame(Difficulty difficulty) {
        newGame(difficulty, 1);
    }

    public void newGame(Difficulty difficulty, int level) {
        if (difficulty == null) {
            throw new IllegalArgumentException("Difficulty không được null");
        }
        this.difficulty = difficulty;
        this.level = Math.max(1, Math.min(Difficulty.MAX_LEVEL, level));
        this.board = createBoard(difficulty, this.level);
    }

    /**
     * Sang màn kế tiếp, giữ nguyên số mạng còn lại của người chơi.
     */
    public void advanceLevel() {
        if (level >= Difficulty.MAX_LEVEL) {
            return;
        }
        int carriedLives = board.getLives();
        level++;
        this.board = new Board(difficulty.getRows(), difficulty.getCols(),
                difficulty.getMinesForLevel(level), Board.DEFAULT_LIVES,
                carriedLives, random);
    }

    public List<Position> reveal(int row, int col) {
        return board.reveal(row, col);
    }

    public List<Position> resolveQuiz(boolean correct) {
        return board.resolveQuiz(correct);
    }

    public boolean toggleFlag(int row, int col) {
        return board.toggleFlag(row, col);
    }

    public int getRows() {
        return board.getRows();
    }

    public int getCols() {
        return board.getCols();
    }

    public int getTotalMines() {
        return board.getTotalMines();
    }

    public int getRemainingMines() {
        return board.getRemainingMines();
    }

    public GameStatus getStatus() {
        return board.getStatus();
    }

    public int getLives() {
        return board.getLives();
    }

    public int getMaxLives() {
        return board.getMaxLives();
    }

    public boolean isQuizPending() {
        return board.isQuizPending();
    }

    public Position getPendingMine() {
        return board.getPendingMine();
    }

    public CellState getCellState(int row, int col) {
        return board.getCellState(row, col);
    }

    public int getAdjacentMines(int row, int col) {
        return board.getAdjacentMines(row, col);
    }

    public boolean isMine(int row, int col) {
        return board.isMine(row, col);
    }

    public boolean isExploded(int row, int col) {
        return board.isExploded(row, col);
    }

    public boolean isWrongFlag(int row, int col) {
        return board.isWrongFlag(row, col);
    }

    public boolean isDefused(int row, int col) {
        return board.isDefused(row, col);
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public int getLevel() {
        return level;
    }

    private Board createBoard(Difficulty difficulty, int level) {
        int mines = difficulty.getMinesForLevel(level);
        return new Board(difficulty.getRows(), difficulty.getCols(), mines,
                Board.DEFAULT_LIVES, random);
    }
}
