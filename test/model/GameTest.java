package model;

import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Test thuần cho tầng model (không cần thư viện ngoài).
 * Chạy bằng: java -cp out model.GameTest
 */
public class GameTest {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        testFirstClickSafe();
        testMineCountPerLevel();
        testAdjacentCountsAllCells();
        testFloodFillRespectsFlagsAndMines();
        testCannotFlagRevealedOrRevealFlagged();
        testMineTriggersQuizAndDefuse();
        testWrongQuizLosesLife();
        testAdvanceLevelKeepsLives();
        testWinAndNoActionAfterWin();
        testWinAfterDefusedMine();
        testWinAfterExplodedMine();
        testWinFlagsQuestionMarkedMines();
        testRemainingMinesCanBeNegative();
        testInvalidCoordinates();

        System.out.println();
        System.out.println("Passed: " + passed + ", Failed: " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void testFirstClickSafe() {
        for (Difficulty difficulty : Difficulty.values()) {
            for (int seed = 0; seed < 200; seed++) {
                Game game = new Game(difficulty, new Random(seed));
                int row = (seed * 7 + 3) % difficulty.getRows();
                int col = (seed * 11 + 5) % difficulty.getCols();

                List<Position> revealed = game.reveal(row, col);
                check(!revealed.isEmpty(), "Lần mở đầu phải mở được ô: " + difficulty + " seed=" + seed);
                check(!game.isMine(row, col),
                        "Ô click đầu không được có bom: " + difficulty + " seed=" + seed);
                checkEquals(CellState.REVEALED, game.getCellState(row, col),
                        "Ô click đầu phải được mở: " + difficulty + " seed=" + seed);
                check(!game.isQuizPending(), "Click đầu không được kích hoạt câu đố");
                checkEquals(3, game.getLives(), "Mỗi màn bắt đầu với 3 mạng");

                for (int dr = -1; dr <= 1; dr++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        int nr = row + dr;
                        int nc = col + dc;
                        if (nr >= 0 && nr < difficulty.getRows()
                                && nc >= 0 && nc < difficulty.getCols()) {
                            check(!game.isMine(nr, nc),
                                    "Vùng 3x3 quanh click đầu phải an toàn: " + difficulty
                                            + " seed=" + seed + " at (" + nr + "," + nc + ")");
                        }
                    }
                }
            }
        }
    }

    private static void testMineCountPerLevel() {
        for (Difficulty difficulty : Difficulty.values()) {
            int previous = -1;
            for (int level = 1; level <= Difficulty.MAX_LEVEL; level++) {
                Game game = new Game(difficulty, new Random(42));
                game.newGame(difficulty, level);
                game.reveal(0, 0);

                int expected = difficulty.getMinesForLevel(level);
                int mines = 0;
                for (int r = 0; r < game.getRows(); r++) {
                    for (int c = 0; c < game.getCols(); c++) {
                        if (game.isMine(r, c)) {
                            mines++;
                        }
                    }
                }
                checkEquals(expected, mines,
                        "Số bom phải đúng cấu hình " + difficulty + " màn " + level);
                checkEquals(expected, game.getTotalMines(),
                        "getTotalMines phải đúng " + difficulty + " màn " + level);
                check(previous <= expected,
                        "Số bom phải tăng dần theo màn: " + difficulty + " màn " + level);
                previous = expected;
            }
        }
    }

    private static void testAdjacentCountsAllCells() {
        for (Difficulty difficulty : Difficulty.values()) {
            Game game = new Game(difficulty, new Random(2026));
            game.reveal(difficulty.getRows() / 2, difficulty.getCols() / 2);
            for (int r = 0; r < difficulty.getRows(); r++) {
                for (int c = 0; c < difficulty.getCols(); c++) {
                    int expected = countNeighborMines(game, r, c);
                    checkEquals(expected, game.getAdjacentMines(r, c),
                            "Số bom kề đúng tại (" + r + "," + c + ") " + difficulty);
                }
            }
        }
    }

    private static int countNeighborMines(Game game, int row, int col) {
        int count = 0;
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) {
                    continue;
                }
                int nr = row + dr;
                int nc = col + dc;
                if (nr >= 0 && nr < game.getRows() && nc >= 0 && nc < game.getCols()
                        && game.isMine(nr, nc)) {
                    count++;
                }
            }
        }
        return count;
    }

    private static void testFloodFillRespectsFlagsAndMines() {
        Game game = createPlayingGame(12345);
        check(game != null, "Phải tạo được ván đang chơi");
        if (game == null) {
            return;
        }

        for (int r = 0; r < game.getRows(); r++) {
            for (int c = 0; c < game.getCols(); c++) {
                if (game.getCellState(r, c) == CellState.REVEALED) {
                    check(!game.isMine(r, c), "Flood Fill không được mở ô bom");
                }
            }
        }

        Position hidden = findCellWithState(game, CellState.HIDDEN);
        check(hidden != null, "Phải còn ô ẩn để test cắm cờ");
        if (hidden == null) {
            return;
        }
        check(game.toggleFlag(hidden.row(), hidden.col()), "Cắm cờ phải thành công");
        check(game.reveal(hidden.row(), hidden.col()).isEmpty(),
                "Không được mở ô đang cắm cờ");
        checkEquals(CellState.FLAGGED, game.getCellState(hidden.row(), hidden.col()),
                "Ô có cờ phải giữ trạng thái FLAGGED");
    }

    private static void testCannotFlagRevealedOrRevealFlagged() {
        Game game = createPlayingGame(55);
        check(game != null, "Phải tạo được ván đang chơi");
        if (game == null) {
            return;
        }

        Position revealed = findCellWithState(game, CellState.REVEALED);
        check(revealed != null, "Phải có ô đã mở");
        if (revealed != null) {
            check(!game.toggleFlag(revealed.row(), revealed.col()),
                    "Không được cắm cờ lên ô đã mở");
        }

        Position hidden = findCellWithState(game, CellState.HIDDEN);
        check(hidden != null, "Phải có ô ẩn");
        if (hidden != null) {
            check(game.toggleFlag(hidden.row(), hidden.col()), "Cắm cờ hợp lệ");
            check(game.reveal(hidden.row(), hidden.col()).isEmpty(),
                    "Không được mở ô đang có cờ");
            check(game.toggleFlag(hidden.row(), hidden.col()), "Gỡ cờ hợp lệ");
            checkEquals(CellState.HIDDEN, game.getCellState(hidden.row(), hidden.col()),
                    "Sau khi gỡ cờ ô phải HIDDEN");
        }
    }

    private static void testMineTriggersQuizAndDefuse() {
        Game game = createPlayingGame(777);
        check(game != null, "Phải tạo được ván đang chơi");
        if (game == null) {
            return;
        }

        Position mine = findHiddenMine(game);
        check(mine != null, "Phải có bom ẩn");
        if (mine == null) {
            return;
        }

        int remainingBefore = game.getRemainingMines();
        List<Position> result = game.reveal(mine.row(), mine.col());
        check(result.isEmpty(), "Mở trúng bom chưa trả về ô nào (chờ trả lời)");
        check(game.isQuizPending(), "Mở trúng bom phải chờ câu đố");
        checkEquals(GameStatus.PLAYING, game.getStatus(), "Chưa trả lời thì chưa thua");

        List<Position> resolved = game.resolveQuiz(true);
        check(!game.isQuizPending(), "Sau khi trả lời phải hết chờ");
        checkEquals(1, resolved.size(), "Đúng thì trả về ô bom vừa gỡ");
        checkEquals(3, game.getLives(), "Trả lời đúng không mất mạng");
        check(game.isDefused(mine.row(), mine.col()), "Bom phải được đánh dấu đã gỡ");
        check(!game.isExploded(mine.row(), mine.col()), "Bom đã gỡ không được coi là nổ");
        checkEquals(CellState.REVEALED, game.getCellState(mine.row(), mine.col()),
                "Bom đã gỡ phải được mở");
        checkEquals(remainingBefore - 1, game.getRemainingMines(),
                "Gỡ bom làm giảm số bom còn lại");
        checkEquals(GameStatus.PLAYING, game.getStatus(), "Vẫn tiếp tục chơi sau khi gỡ bom");
    }

    private static void testWrongQuizLosesLife() {
        Game game = createPlayingGame(999);
        check(game != null, "Phải tạo được ván đang chơi");
        if (game == null) {
            return;
        }

        for (int attempt = 0; attempt < 3; attempt++) {
            Position mine = findHiddenMine(game);
            check(mine != null, "Phải còn bom ẩn cho lần sai thứ " + (attempt + 1));
            if (mine == null) {
                return;
            }
            game.reveal(mine.row(), mine.col());
            check(game.isQuizPending(), "Phải chờ câu đố ở lần sai thứ " + (attempt + 1));
            List<Position> resolved = game.resolveQuiz(false);
            checkEquals(1, resolved.size(), "Sai thì trả về ô bom vừa nổ");
            check(game.isExploded(mine.row(), mine.col()), "Bom phải nổ khi trả lời sai");
            checkEquals(2 - attempt, game.getLives(),
                    "Mỗi lần sai phải mất 1 mạng");
        }

        checkEquals(GameStatus.LOST, game.getStatus(), "Hết 3 mạng phải thua");
        checkEquals(0, game.getLives(), "Số mạng không được âm");
        check(game.reveal(0, 0).isEmpty(), "Sau khi thua không được reveal");
        check(!game.toggleFlag(0, 0), "Sau khi thua không được toggleFlag");
    }

    private static void testAdvanceLevelKeepsLives() {
        Game game = createPlayingGame(321);
        check(game != null, "Phải tạo được ván đang chơi");
        if (game == null) {
            return;
        }

        Position mine = findHiddenMine(game);
        check(mine != null, "Phải có bom để test mất mạng");
        if (mine == null) {
            return;
        }
        game.reveal(mine.row(), mine.col());
        game.resolveQuiz(false);
        checkEquals(2, game.getLives(), "Sai một câu phải còn 2 mạng");

        int oldLevel = game.getLevel();
        game.advanceLevel();
        checkEquals(oldLevel + 1, game.getLevel(), "advanceLevel phải tăng màn");
        checkEquals(2, game.getLives(), "Sang màn mới phải giữ nguyên số mạng");
        checkEquals(GameStatus.READY, game.getStatus(), "Màn mới phải bắt đầu ở trạng thái READY");
        checkEquals(game.getDifficulty().getMinesForLevel(oldLevel + 1),
                game.getTotalMines(), "Sang màn phải tăng số bom theo cấu hình");
    }

    private static void testWinAndNoActionAfterWin() {
        Game game = new Game(Difficulty.BEGINNER, new Random(7));
        game.reveal(4, 4);
        for (int r = 0; r < game.getRows(); r++) {
            for (int c = 0; c < game.getCols(); c++) {
                if (!game.isMine(r, c)) {
                    game.reveal(r, c);
                }
            }
        }

        checkEquals(GameStatus.WON, game.getStatus(), "Phải thắng khi mở hết ô an toàn");
        checkEquals(0, game.getRemainingMines(), "Khi thắng số bom còn lại phải bằng 0");

        Position mine = findAnyMine(game);
        check(mine != null, "Phải có bom để test thao tác sau khi thắng");
        if (mine != null) {
            check(game.reveal(mine.row(), mine.col()).isEmpty(),
                    "Sau khi thắng không được reveal");
        }
        check(!game.toggleFlag(0, 0), "Sau khi thắng không được toggleFlag");
    }

    private static void testWinAfterDefusedMine() {
        for (Difficulty difficulty : Difficulty.values()) {
            for (int seed = 0; seed < 30; seed++) {
                Game game = new Game(difficulty, new Random(seed));
                game.reveal(0, 0);
                Position mine = findHiddenMine(game);
                check(mine != null, "Phải có bom để gỡ: " + difficulty + " seed=" + seed);
                if (mine == null) {
                    return;
                }

                game.reveal(mine.row(), mine.col());
                check(game.isQuizPending(),
                        "Mở trúng bom phải chờ câu đố: " + difficulty + " seed=" + seed);
                game.resolveQuiz(true);
                check(game.isDefused(mine.row(), mine.col()),
                        "Bom phải được gỡ: " + difficulty + " seed=" + seed);

                revealAllSafeCells(game);
                checkEquals(GameStatus.WON, game.getStatus(),
                        "Phải thắng sau khi gỡ bom và mở hết ô an toàn: "
                                + difficulty + " seed=" + seed);
                checkEquals(0, game.getRemainingMines(),
                        "Thắng sau khi gỡ bom phải còn 0 bom: " + difficulty + " seed=" + seed);
                checkEquals(CellState.REVEALED, game.getCellState(mine.row(), mine.col()),
                        "Bom đã gỡ phải giữ trạng thái REVEALED: "
                                + difficulty + " seed=" + seed);
            }
        }
    }

    private static void testWinAfterExplodedMine() {
        for (Difficulty difficulty : Difficulty.values()) {
            for (int seed = 0; seed < 30; seed++) {
                Game game = new Game(difficulty, new Random(seed));
                game.reveal(0, 0);
                Position mine = findHiddenMine(game);
                check(mine != null, "Phải có bom để làm nổ: " + difficulty + " seed=" + seed);
                if (mine == null) {
                    return;
                }

                game.reveal(mine.row(), mine.col());
                game.resolveQuiz(false);
                check(game.isExploded(mine.row(), mine.col()),
                        "Bom phải nổ khi trả lời sai: " + difficulty + " seed=" + seed);
                checkEquals(2, game.getLives(),
                        "Sai một câu còn 2 mạng: " + difficulty + " seed=" + seed);

                revealAllSafeCells(game);
                checkEquals(GameStatus.WON, game.getStatus(),
                        "Phải thắng sau khi bom nổ và mở hết ô an toàn: "
                                + difficulty + " seed=" + seed);
                checkEquals(0, game.getRemainingMines(),
                        "Thắng sau khi bom nổ phải còn 0 bom: " + difficulty + " seed=" + seed);
                checkEquals(CellState.REVEALED, game.getCellState(mine.row(), mine.col()),
                        "Bom đã nổ phải giữ trạng thái REVEALED: "
                                + difficulty + " seed=" + seed);
            }
        }
    }

    private static void testWinFlagsQuestionMarkedMines() {
        for (Difficulty difficulty : Difficulty.values()) {
            for (int seed = 0; seed < 30; seed++) {
                Game game = new Game(difficulty, new Random(seed));
                game.reveal(0, 0);
                Position mine = findHiddenMine(game);
                check(mine != null, "Phải có bom để đánh dấu ?: " + difficulty + " seed=" + seed);
                if (mine == null) {
                    return;
                }

                // HIDDEN -> FLAGGED -> QUESTION: ô "?" không tính vào bộ đếm cờ.
                game.cycleMark(mine.row(), mine.col());
                game.cycleMark(mine.row(), mine.col());
                checkEquals(CellState.QUESTION, game.getCellState(mine.row(), mine.col()),
                        "Ô bom phải đang ở trạng thái QUESTION: " + difficulty + " seed=" + seed);

                revealAllSafeCells(game);
                checkEquals(GameStatus.WON, game.getStatus(),
                        "Phải thắng dù bom đang đánh dấu ?: " + difficulty + " seed=" + seed);
                checkEquals(CellState.FLAGGED, game.getCellState(mine.row(), mine.col()),
                        "Khi thắng, bom đánh dấu ? phải được tự cắm cờ: "
                                + difficulty + " seed=" + seed);
                checkEquals(0, game.getRemainingMines(),
                        "Thắng phải đưa số bom còn lại về 0: " + difficulty + " seed=" + seed);
            }
        }
    }

    private static void revealAllSafeCells(Game game) {
        for (int r = 0; r < game.getRows(); r++) {
            for (int c = 0; c < game.getCols(); c++) {
                if (!game.isMine(r, c) && game.getCellState(r, c) != CellState.REVEALED) {
                    game.reveal(r, c);
                }
            }
        }
    }

    private static void testRemainingMinesCanBeNegative() {
        Game game = new Game(Difficulty.BEGINNER, new Random(5));
        int flags = 0;
        for (int r = 0; r < game.getRows(); r++) {
            for (int c = 0; c < game.getCols(); c++) {
                if (game.toggleFlag(r, c)) {
                    flags++;
                }
            }
        }
        check(flags > game.getTotalMines(), "Phải cắm được nhiều cờ hơn số bom");
        checkEquals(game.getTotalMines() - flags, game.getRemainingMines(),
                "Số bom còn lại phải bằng tổng bom - số cờ");
        check(game.getRemainingMines() < 0, "Số bom còn lại phải có thể âm");
        checkEquals(GameStatus.READY, game.getStatus(), "Cắm cờ không làm game bắt đầu");
    }

    private static void testInvalidCoordinates() {
        Game game = new Game(Difficulty.BEGINNER, new Random(1));
        check(game.reveal(-1, 0).isEmpty(), "Reveal tọa độ âm phải trả rỗng");
        check(game.reveal(0, -1).isEmpty(), "Reveal tọa độ âm phải trả rỗng");
        check(game.reveal(game.getRows(), 0).isEmpty(), "Reveal ngoài biên phải trả rỗng");
        check(!game.toggleFlag(-1, 0), "ToggleFlag ngoài biên phải trả false");
        check(!game.toggleFlag(0, game.getCols()), "ToggleFlag ngoài biên phải trả false");

        boolean threw = false;
        try {
            game.getCellState(-1, 0);
        } catch (IndexOutOfBoundsException ex) {
            threw = true;
        }
        check(threw, "Truy vấn ngoài biên phải ném IndexOutOfBoundsException");
    }

    private static Game createPlayingGame(int startSeed) {
        for (int seed = startSeed; seed < startSeed + 2000; seed++) {
            Game candidate = new Game(Difficulty.BEGINNER, new Random(seed));
            candidate.reveal(4, 4);
            if (candidate.getStatus() == GameStatus.PLAYING) {
                return candidate;
            }
        }
        return null;
    }

    private static Position findCellWithState(Game game, CellState wanted) {
        for (int r = 0; r < game.getRows(); r++) {
            for (int c = 0; c < game.getCols(); c++) {
                if (game.getCellState(r, c) == wanted) {
                    return new Position(r, c);
                }
            }
        }
        return null;
    }

    private static Position findHiddenMine(Game game) {
        for (int r = 0; r < game.getRows(); r++) {
            for (int c = 0; c < game.getCols(); c++) {
                if (game.isMine(r, c) && game.getCellState(r, c) == CellState.HIDDEN) {
                    return new Position(r, c);
                }
            }
        }
        return null;
    }

    private static Position findAnyMine(Game game) {
        for (int r = 0; r < game.getRows(); r++) {
            for (int c = 0; c < game.getCols(); c++) {
                if (game.isMine(r, c)) {
                    return new Position(r, c);
                }
            }
        }
        return null;
    }

    private static void check(boolean condition, String message) {
        if (condition) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL: " + message);
        }
    }

    private static void checkEquals(Object expected, Object actual, String message) {
        check(Objects.equals(expected, actual),
                message + " (expected=" + expected + ", actual=" + actual + ")");
    }
}
