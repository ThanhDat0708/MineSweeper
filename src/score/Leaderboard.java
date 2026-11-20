package score;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Bảng xếp hạng được lưu ra file text để không mất khi tắt game.
 * Mỗi người chơi chỉ giữ thành tích tốt nhất ở từng chế độ.
 * Xếp hạng: màn cao hơn trước, cùng màn thì thời gian ngắn hơn trước.
 */
public final class Leaderboard {

    /** Số dòng tối đa hiển thị mặc định. */
    public static final int MAX_ROWS = 20;

    /** File mặc định nằm cùng thư mục chạy chương trình. */
    private static final String DEFAULT_FILE = "leaderboard.txt";

    private static final String SEPARATOR = "|";

    private static final Comparator<ScoreEntry> RANKING =
            Comparator.comparingInt(ScoreEntry::levelsCleared).reversed()
                    .thenComparingInt(ScoreEntry::totalSeconds)
                    .thenComparingLong(ScoreEntry::timestamp);

    private final Path file;
    private final List<ScoreEntry> entries = new ArrayList<>();

    public Leaderboard() {
        this(Paths.get(DEFAULT_FILE));
    }

    public Leaderboard(Path file) {
        this.file = (file != null) ? file : Paths.get(DEFAULT_FILE);
    }

    public Path getFile() {
        return file;
    }

    /**
     * Đọc file thành tích. File chưa tồn tại hoặc dòng hỏng đều được bỏ qua
     * để game vẫn khởi động bình thường.
     */
    public void load() {
        entries.clear();
        if (!Files.exists(file)) {
            return;
        }
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                ScoreEntry entry = parseLine(line);
                if (entry != null) {
                    entries.add(entry);
                }
            }
        } catch (IOException ex) {
            // Không đọc được file: bắt đầu với bảng rỗng thay vì làm sập game.
        }
        removeDuplicateKeys();
    }

    /**
     * Gửi thành tích mới. Chỉ thay thế khi thành tích cũ kém hơn
     * (màn thấp hơn, hoặc cùng màn mà chậm hơn).
     */
    public void submit(ScoreEntry entry) {
        if (entry == null) {
            return;
        }
        ScoreEntry existing = findByKey(entry.key());
        if (existing == null || isBetter(entry, existing)) {
            entries.remove(existing);
            entries.add(entry);
            entries.sort(RANKING);
            save();
        }
    }

    public List<ScoreEntry> top(int limit) {
        int safeLimit = Math.max(0, limit);
        entries.sort(RANKING);
        int size = Math.min(safeLimit, entries.size());
        return new ArrayList<>(entries.subList(0, size));
    }

    public void clear() {
        entries.clear();
        save();
    }

    /**
     * Chuyển bảng xếp hạng thành các dòng text để view hiển thị,
     * nhờ vậy tầng view không cần phụ thuộc vào package score.
     */
    public String[][] toDisplayRows(int limit) {
        List<ScoreEntry> rows = top(limit);
        String[][] result = new String[rows.size()][6];
        for (int i = 0; i < rows.size(); i++) {
            ScoreEntry entry = rows.get(i);
            result[i][0] = String.valueOf(i + 1);
            result[i][1] = entry.playerName();
            result[i][2] = entry.difficulty();
            result[i][3] = String.valueOf(entry.levelsCleared());
            result[i][4] = String.valueOf(entry.mines());
            result[i][5] = entry.formattedTime();
        }
        return result;
    }

    private static boolean isBetter(ScoreEntry candidate, ScoreEntry existing) {
        if (candidate.levelsCleared() != existing.levelsCleared()) {
            return candidate.levelsCleared() > existing.levelsCleared();
        }
        return candidate.totalSeconds() < existing.totalSeconds();
    }

    private ScoreEntry findByKey(String key) {
        for (ScoreEntry entry : entries) {
            if (entry.key().equals(key)) {
                return entry;
            }
        }
        return null;
    }

    /** Nếu file có nhiều dòng cùng người + chế độ thì giữ thành tích tốt nhất. */
    private void removeDuplicateKeys() {
        List<ScoreEntry> unique = new ArrayList<>();
        for (ScoreEntry entry : entries) {
            ScoreEntry existing = null;
            for (ScoreEntry kept : unique) {
                if (kept.key().equals(entry.key())) {
                    existing = kept;
                    break;
                }
            }
            if (existing == null) {
                unique.add(entry);
            } else if (isBetter(entry, existing)) {
                unique.remove(existing);
                unique.add(entry);
            }
        }
        entries.clear();
        entries.addAll(unique);
        entries.sort(RANKING);
    }

    /**
     * Định dạng: ten|che_do|so_man|tong_giay|so_bom|thoi_diem.
     * Trả null nếu dòng không hợp lệ.
     */
    private static ScoreEntry parseLine(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }
        String[] parts = line.split("\\" + SEPARATOR, -1);
        if (parts.length != 6) {
            return null;
        }
        try {
            String name = parts[0];
            String difficulty = parts[1];
            int levels = Integer.parseInt(parts[2].trim());
            int seconds = Integer.parseInt(parts[3].trim());
            int mines = Integer.parseInt(parts[4].trim());
            long timestamp = Long.parseLong(parts[5].trim());
            return new ScoreEntry(name, difficulty, levels, seconds, mines, timestamp);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** Ghi file an toàn: ghi file tạm rồi thay thế file chính. */
    private void save() {
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path temp = file.resolveSibling(file.getFileName() + ".tmp");
            List<String> lines = new ArrayList<>(entries.size());
            for (ScoreEntry entry : entries) {
                lines.add(entry.playerName() + SEPARATOR + entry.difficulty() + SEPARATOR
                        + entry.levelsCleared() + SEPARATOR + entry.totalSeconds() + SEPARATOR
                        + entry.mines() + SEPARATOR + entry.timestamp());
            }
            Files.write(temp, lines, StandardCharsets.UTF_8);
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            // Không ghi được file: thành tích vẫn còn trong bộ nhớ của phiên hiện tại.
        }
    }
}
