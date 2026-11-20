package score;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Test thuần cho bảng xếp hạng (không cần thư viện ngoài).
 * Chạy bằng: java -cp out score.LeaderboardTest
 */
public class LeaderboardTest {

    private static int passed;
    private static int failed;

    public static void main(String[] args) throws IOException {
        testRankingOrder();
        testBestPerPlayerReplacement();
        testSaveAndLoadRoundTrip();
        testMalformedLinesIgnored();
        testMissingFileIsEmpty();
        testDifferentDifficultiesSeparate();
        testDisplayRows();
        testNameSanitized();

        System.out.println();
        System.out.println("Passed: " + passed + ", Failed: " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void testRankingOrder() throws IOException {
        Leaderboard board = new Leaderboard(tempFile());
        board.submit(new ScoreEntry("An", "Beginner", 3, 100, 12, 1));
        board.submit(new ScoreEntry("Binh", "Beginner", 5, 400, 15, 2));
        board.submit(new ScoreEntry("Chi", "Beginner", 5, 200, 15, 3));
        board.submit(new ScoreEntry("Dung", "Beginner", 3, 50, 12, 4));

        List<ScoreEntry> top = board.top(10);
        checkEquals(4, top.size(), "Phải giữ đủ 4 người chơi");
        checkEquals("Chi", top.get(0).playerName(), "Màn 5 nhanh nhất phải đứng đầu");
        checkEquals("Binh", top.get(1).playerName(), "Màn 5 chậm hơn đứng nhì");
        checkEquals("Dung", top.get(2).playerName(), "Màn 3 nhanh hơn đứng ba");
        checkEquals("An", top.get(3).playerName(), "Màn 3 chậm nhất đứng cuối");
    }

    private static void testBestPerPlayerReplacement() throws IOException {
        Leaderboard board = new Leaderboard(tempFile());
        board.submit(new ScoreEntry("An", "Beginner", 2, 50, 10, 1));
        board.submit(new ScoreEntry("An", "Beginner", 4, 200, 12, 2));
        checkEquals(4, board.top(10).get(0).levelsCleared(),
                "Màn cao hơn phải thay thành tích cũ");

        board.submit(new ScoreEntry("An", "Beginner", 4, 300, 12, 3));
        checkEquals(200, board.top(10).get(0).totalSeconds(),
                "Cùng màn mà chậm hơn không được thay");

        board.submit(new ScoreEntry("An", "Beginner", 4, 100, 12, 4));
        checkEquals(100, board.top(10).get(0).totalSeconds(),
                "Cùng màn mà nhanh hơn phải thay");

        checkEquals(1, board.top(10).size(),
                "Mỗi người + mỗi chế độ chỉ giữ một dòng");
    }

    private static void testSaveAndLoadRoundTrip() throws IOException {
        Path file = tempFile();
        Leaderboard first = new Leaderboard(file);
        first.submit(new ScoreEntry("An", "Expert", 7, 321, 87, 123456789L));
        first.submit(new ScoreEntry("Binh", "Beginner", 10, 654, 17, 987654321L));

        Leaderboard second = new Leaderboard(file);
        second.load();
        List<ScoreEntry> top = second.top(10);
        checkEquals(2, top.size(), "Đọc lại phải có đủ 2 dòng");
        checkEquals("Binh", top.get(0).playerName(), "Dòng màn cao phải đứng đầu sau khi đọc");
        checkEquals("Expert", top.get(1).difficulty(), "Chế độ phải giữ nguyên");
        checkEquals(321, top.get(1).totalSeconds(), "Thời gian phải giữ nguyên");
        checkEquals(87, top.get(1).mines(), "Số bom phải giữ nguyên");
    }

    private static void testMalformedLinesIgnored() throws IOException {
        Path file = tempFile();
        Files.write(file, List.of(
                "An|Beginner|3|100|12|1",
                "",
                "thieu-truong|Beginner|2",
                "Bad|Beginner|abc|100|12|1",
                "Chi|Expert|4|200|65|2|du-thua"), StandardCharsets.UTF_8);

        Leaderboard board = new Leaderboard(file);
        board.load();
        checkEquals(1, board.top(10).size(),
                "Chỉ dòng hợp lệ mới được nạp");
        checkEquals("An", board.top(10).get(0).playerName(),
                "Dòng hợp lệ phải đọc đúng");
    }

    private static void testMissingFileIsEmpty() throws IOException {
        Path file = tempFile().resolveSibling("khong-ton-tai-" + System.nanoTime() + ".txt");
        Leaderboard board = new Leaderboard(file);
        board.load();
        checkEquals(0, board.top(10).size(), "File chưa có phải là bảng rỗng");
    }

    private static void testDifferentDifficultiesSeparate() throws IOException {
        Leaderboard board = new Leaderboard(tempFile());
        board.submit(new ScoreEntry("An", "Beginner", 10, 300, 17, 1));
        board.submit(new ScoreEntry("An", "Expert", 5, 200, 80, 2));
        checkEquals(2, board.top(10).size(),
                "Cùng tên khác chế độ phải giữ hai dòng");
    }

    private static void testDisplayRows() throws IOException {
        Leaderboard board = new Leaderboard(tempFile());
        board.submit(new ScoreEntry("An", "Beginner", 3, 125, 12, 1));
        String[][] rows = board.toDisplayRows(10);
        checkEquals(1, rows.length, "Phải có một dòng hiển thị");
        checkEquals("1", rows[0][0], "Cột hạng bắt đầu từ 1");
        checkEquals("An", rows[0][1], "Cột tên đúng");
        checkEquals("Beginner", rows[0][2], "Cột chế độ đúng");
        checkEquals("3", rows[0][3], "Cột màn đúng");
        checkEquals("12", rows[0][4], "Cột số bom đúng");
        checkEquals("02:05", rows[0][5], "Thời gian phải dạng mm:ss");
    }

    private static void testNameSanitized() {
        checkEquals("An", ScoreEntry.sanitizeName("  An  "), "Tên phải được trim");
        checkEquals("A B", ScoreEntry.sanitizeName("A|B"), "Ký tự phân tách phải bị loại");
        checkEquals("An", ScoreEntry.sanitizeName("An\n"), "Xuống dòng phải bị loại");
        check(ScoreEntry.sanitizeName(null).length() > 0, "Tên null phải có giá trị mặc định");
    }

    private static Path tempFile() throws IOException {
        Path dir = Files.createTempDirectory("minesweeper-leaderboard");
        return dir.resolve("leaderboard.txt");
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
