package model;

/**
 * Các chế độ chơi. Mỗi chế độ có 10 màn (level), càng lên cao càng nhiều mìn.
 */
public enum Difficulty {
    BEGINNER("Beginner", 9, 9, 8, 17),
    INTERMEDIATE("Intermediate", 16, 16, 28, 46),
    EXPERT("Expert", 16, 30, 65, 101);

    /** Số màn tối đa của mỗi chế độ. */
    public static final int MAX_LEVEL = 10;

    private final String displayName;
    private final int rows;
    private final int cols;
    private final int minMines;
    private final int maxMines;

    Difficulty(String displayName, int rows, int cols, int minMines, int maxMines) {
        this.displayName = displayName;
        this.rows = rows;
        this.cols = cols;
        this.minMines = minMines;
        this.maxMines = maxMines;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    /**
     * Số mìn tăng dần theo màn: màn 1 dễ nhất, màn 10 khó nhất.
     */
    public int getMinesForLevel(int level) {
        int clamped = Math.max(1, Math.min(MAX_LEVEL, level));
        return minMines + (maxMines - minMines) * (clamped - 1) / (MAX_LEVEL - 1);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
