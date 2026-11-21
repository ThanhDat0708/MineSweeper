package model;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Bàn cờ Minesweeper phiên bản "phiêu lưu":
 * - Số mìn kề do view hiển thị sau khi ô được mở.
 * - Khi mở trúng bom, ván chơi tạm dừng để trả lời câu đố (pending quiz).
 * - Trả lời đúng: quả bom được vô hiệu hóa.
 * - Trả lời sai: bom nổ và người chơi mất 1 mạng.
 * - Có 3 mạng; hết mạng thì thua.
 */
public final class Board implements Serializable {

    private static final long serialVersionUID = 2L;

    /** Số mạng mặc định của mỗi màn. */
    public static final int DEFAULT_LIVES = 3;

    private final int rows;
    private final int cols;
    private final int totalMines;
    private final int maxLives;
    private final Random random;
    private final Cell[][] cells;

    private GameStatus status = GameStatus.READY;
    private boolean minesPlaced;
    private int revealedSafeCount;
    private int flagCount;
    private int defusedCount;
    private int explodedMineCount;
    private int lives;
    private Position pendingMine;

    public Board(int rows, int cols, int totalMines, int maxLives, Random random) {
        this(rows, cols, totalMines, maxLives, maxLives, random);
    }

    public Board(int rows, int cols, int totalMines, int maxLives,
                 int initialLives, Random random) {
        if (rows <= 0 || cols <= 0) {
            throw new IllegalArgumentException("Kích thước bàn cờ phải lớn hơn 0");
        }
        if (totalMines < 0 || totalMines >= rows * cols) {
            throw new IllegalArgumentException("Số mìn không hợp lệ cho kích thước bàn cờ");
        }
        if (maxLives < 1) {
            throw new IllegalArgumentException("Số mạng phải lớn hơn 0");
        }
        if (initialLives < 1) {
            throw new IllegalArgumentException("Số mạng ban đầu phải lớn hơn 0");
        }
        this.rows = rows;
        this.cols = cols;
        this.totalMines = totalMines;
        this.maxLives = maxLives;
        this.lives = Math.min(initialLives, maxLives);
        this.random = (random != null) ? random : new Random();
        this.cells = new Cell[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                cells[r][c] = new Cell();
            }
        }
    }

    /**
     * Mở ô. Nếu trúng bom, KHÔNG thua ngay mà đặt trạng thái chờ trả lời câu đố.
     * Controller kiểm tra {@link #isQuizPending()} rồi gọi {@link #resolveQuiz(boolean)}.
     */
    public List<Position> reveal(int row, int col) {
        List<Position> revealed = new ArrayList<>();
        if (isGameOver() || pendingMine != null) {
            return revealed;
        }
        if (!inBounds(row, col)) {
            return revealed;
        }
        Cell start = cells[row][col];
        // Ô đã mở hoặc đang cắm cờ thì không mở. Ô đánh dấu "?" vẫn mở được.
        if (start.getState() == CellState.REVEALED || start.getState() == CellState.FLAGGED) {
            return revealed;
        }

        // Sinh mìn lười: chỉ sinh sau lần mở ô đầu tiên, bảo đảm ô click an toàn.
        placeMinesIfNeeded(row, col);
        status = GameStatus.PLAYING;

        if (start.isMine()) {
            // Gặp bom: chờ controller đặt câu hỏi.
            pendingMine = new Position(row, col);
            return revealed;
        }

        // Flood Fill bằng BFS (ArrayDeque), không đệ quy.
        ArrayDeque<Position> queue = new ArrayDeque<>();
        queue.add(new Position(row, col));
        while (!queue.isEmpty()) {
            Position p = queue.poll();
            Cell cell = cells[p.row()][p.col()];
            if (cell.getState() == CellState.FLAGGED
                    || cell.getState() == CellState.REVEALED
                    || cell.isMine()) {
                continue;
            }
            cell.setState(CellState.REVEALED);
            revealedSafeCount++;
            revealed.add(p);

            if (cell.getAdjacentMines() == 0) {
                enqueueNeighbors(p, queue);
            }
        }

        checkWin();
        return revealed;
    }

    /**
     * Giải quyết câu đố của quả bom đang chờ.
     *
     * @param correct true nếu người chơi trả lời đúng
     * @return danh sách ô cần vẽ lại (ô bom vừa được vô hiệu hóa hoặc phát nổ)
     */
    public List<Position> resolveQuiz(boolean correct) {
        List<Position> changed = new ArrayList<>();
        if (pendingMine == null || isGameOver()) {
            return changed;
        }
        Position mine = pendingMine;
        pendingMine = null;
        Cell cell = cells[mine.row()][mine.col()];

        if (correct) {
            // Vô hiệu hóa bom: mở ô và đánh dấu đã gỡ.
            cell.setDefused(true);
            cell.setExploded(false);
            cell.setState(CellState.REVEALED);
            defusedCount++;
        } else {
            // Trả lời sai: bom nổ, mất 1 mạng.
            cell.setState(CellState.REVEALED);
            cell.setExploded(true);
            explodedMineCount++;
            lives--;
            if (lives <= 0) {
                lives = 0;
                handleLoss();
            }
        }

        changed.add(mine);
        return changed;
    }

    public boolean toggleFlag(int row, int col) {
        if (isGameOver() || pendingMine != null || !inBounds(row, col)) {
            return false;
        }
        Cell cell = cells[row][col];
        if (cell.getState() == CellState.REVEALED) {
            return false;
        }
        if (cell.getState() == CellState.HIDDEN) {
            cell.setState(CellState.FLAGGED);
            flagCount++;
            return true;
        }
        if (cell.getState() == CellState.FLAGGED) {
            cell.setState(CellState.HIDDEN);
            flagCount--;
            return true;
        }
        return false;
    }

    /**
     * Chu kỳ đánh dấu của chuột phải: HIDDEN -> FLAGGED -> QUESTION -> HIDDEN.
     * Chỉ cờ (FLAGGED) mới được tính vào bộ đếm mìn còn lại.
     *
     * @return trạng thái mới, hoặc null nếu ô không thể đánh dấu
     */
    public CellState cycleMark(int row, int col) {
        if (isGameOver() || pendingMine != null || !inBounds(row, col)) {
            return null;
        }
        Cell cell = cells[row][col];
        switch (cell.getState()) {
            case HIDDEN:
                cell.setState(CellState.FLAGGED);
                flagCount++;
                return CellState.FLAGGED;
            case FLAGGED:
                cell.setState(CellState.QUESTION);
                flagCount--;
                return CellState.QUESTION;
            case QUESTION:
                cell.setState(CellState.HIDDEN);
                return CellState.HIDDEN;
            case REVEALED:
            default:
                return null;
        }
    }

    /**
     * Chord: nếu số cờ quanh một ô đã mở bằng đúng số bom kề, mở toàn bộ
     * các ô còn ẩn quanh nó. Trả về danh sách ô vừa mở.
     */
    public List<Position> chord(int row, int col) {
        List<Position> changed = new ArrayList<>();
        if (isGameOver() || pendingMine != null || !inBounds(row, col)) {
            return changed;
        }
        Cell center = cells[row][col];
        // Chỉ chord trên ô số đã mở. Bom đã gỡ/đã nổ cũng là REVEALED
        // nhưng không hiển thị số nên không được phép chord.
        if (center.getState() != CellState.REVEALED || center.isMine()
                || center.getAdjacentMines() == 0) {
            return changed;
        }

        int flags = 0;
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                int nr = row + dr;
                int nc = col + dc;
                if ((dr != 0 || dc != 0) && inBounds(nr, nc)
                        && cells[nr][nc].getState() == CellState.FLAGGED) {
                    flags++;
                }
            }
        }
        if (flags != center.getAdjacentMines()) {
            return changed;
        }

        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) {
                    continue;
                }
                int nr = row + dr;
                int nc = col + dc;
                if (!inBounds(nr, nc)) {
                    continue;
                }
                CellState state = cells[nr][nc].getState();
                if (state == CellState.HIDDEN || state == CellState.QUESTION) {
                    changed.addAll(reveal(nr, nc));
                    if (pendingMine != null || isGameOver()) {
                        return changed;
                    }
                }
            }
        }
        return changed;
    }

    /**
     * Gợi ý một ô an toàn còn ẩn. Khi chưa sinh mìn, mọi ô đều an toàn
     * vì lần mở đầu tiên luôn được bảo vệ.
     */
    public Position findHint() {
        if (isGameOver() || pendingMine != null) {
            return null;
        }
        List<Position> safe = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Cell cell = cells[r][c];
                if (cell.getState() == CellState.REVEALED
                        || cell.getState() == CellState.FLAGGED) {
                    continue;
                }
                if (!minesPlaced || !cell.isMine()) {
                    safe.add(new Position(r, c));
                }
            }
        }
        if (safe.isEmpty()) {
            return null;
        }
        return safe.get(random.nextInt(safe.size()));
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public int getTotalMines() {
        return totalMines;
    }

    /**
     * Số bom còn nguy hiểm: trừ đi cờ đã cắm, bom đã gỡ và bom đã nổ.
     * Có thể âm nếu người chơi cắm quá nhiều cờ.
     */
    public int getRemainingMines() {
        return totalMines - flagCount - defusedCount - explodedMineCount;
    }

    public GameStatus getStatus() {
        return status;
    }

    public int getLives() {
        return lives;
    }

    public int getMaxLives() {
        return maxLives;
    }

    public boolean isQuizPending() {
        return pendingMine != null;
    }

    /** Trả về vị trí quả bom đang chờ trả lời, hoặc null nếu không có. */
    public Position getPendingMine() {
        return pendingMine;
    }

    public CellState getCellState(int row, int col) {
        checkBounds(row, col);
        return cells[row][col].getState();
    }

    public int getAdjacentMines(int row, int col) {
        checkBounds(row, col);
        return cells[row][col].getAdjacentMines();
    }

    public boolean isMine(int row, int col) {
        checkBounds(row, col);
        return cells[row][col].isMine();
    }

    public boolean isExploded(int row, int col) {
        checkBounds(row, col);
        return cells[row][col].isExploded();
    }

    public boolean isWrongFlag(int row, int col) {
        checkBounds(row, col);
        return cells[row][col].isWrongFlag();
    }

    public boolean isDefused(int row, int col) {
        checkBounds(row, col);
        return cells[row][col].isDefused();
    }

    private void placeMinesIfNeeded(int safeRow, int safeCol) {
        if (minesPlaced) {
            return;
        }

        List<Position> candidates = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                boolean inSafeZone = Math.abs(r - safeRow) <= 1 && Math.abs(c - safeCol) <= 1;
                if (!inSafeZone) {
                    candidates.add(new Position(r, c));
                }
            }
        }

        if (candidates.size() < totalMines) {
            candidates.clear();
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    if (r != safeRow || c != safeCol) {
                        candidates.add(new Position(r, c));
                    }
                }
            }
        }

        Collections.shuffle(candidates, random);
        int mineCount = Math.min(totalMines, candidates.size());
        for (int i = 0; i < mineCount; i++) {
            Position p = candidates.get(i);
            cells[p.row()][p.col()].setMine(true);
        }

        computeAdjacentMines();
        minesPlaced = true;
    }

    private void computeAdjacentMines() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int count = 0;
                for (int dr = -1; dr <= 1; dr++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        if (dr == 0 && dc == 0) {
                            continue;
                        }
                        int nr = r + dr;
                        int nc = c + dc;
                        if (inBounds(nr, nc) && cells[nr][nc].isMine()) {
                            count++;
                        }
                    }
                }
                cells[r][c].setAdjacentMines(count);
            }
        }
    }

    private void enqueueNeighbors(Position p, ArrayDeque<Position> queue) {
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) {
                    continue;
                }
                int nr = p.row() + dr;
                int nc = p.col() + dc;
                if (!inBounds(nr, nc)) {
                    continue;
                }
                Cell neighbor = cells[nr][nc];
                // "?" không phải cờ nên vẫn được Flood Fill mở như ô ẩn.
                boolean openable = neighbor.getState() == CellState.HIDDEN
                        || neighbor.getState() == CellState.QUESTION;
                if (openable && !neighbor.isMine()) {
                    queue.add(new Position(nr, nc));
                }
            }
        }
    }

    /**
     * Thắng khi mọi ô AN TOÀN ban đầu đã được mở.
     * Bom đã gỡ hoặc đã nổ không tính vào tiến độ thắng.
     */
    private void checkWin() {
        int totalSafeCells = rows * cols - totalMines;
        if (revealedSafeCount != totalSafeCells) {
            return;
        }
        status = GameStatus.WON;
        // Tự cắm cờ những bom còn ẩn để bộ đếm về 0.
        // Bỏ qua bom đã gỡ hoặc đã nổ vì chúng đang REVEALED và đã được
        // tính riêng trong getRemainingMines (nếu cắm cờ lại sẽ bị trừ hai lần).
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Cell cell = cells[r][c];
                if (cell.isMine() && cell.getState() != CellState.FLAGGED
                        && cell.getState() != CellState.REVEALED) {
                    cell.setState(CellState.FLAGGED);
                    flagCount++;
                }
            }
        }
    }

    private void handleLoss() {
        status = GameStatus.LOST;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Cell cell = cells[r][c];
                if (cell.getState() == CellState.FLAGGED && !cell.isMine()) {
                    cell.setWrongFlag(true);
                }
            }
        }
    }

    private boolean isGameOver() {
        return status == GameStatus.WON || status == GameStatus.LOST;
    }

    private boolean inBounds(int row, int col) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }

    private void checkBounds(int row, int col) {
        if (!inBounds(row, col)) {
            throw new IndexOutOfBoundsException(
                    "Tọa độ ngoài bàn cờ: (" + row + ", " + col + ")");
        }
    }
}
