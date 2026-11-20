package score;

/**
 * Một dòng thành tích trong bảng xếp hạng.
 * levelsCleared: số màn đã vượt qua (1..10).
 * mines: số bom của màn cao nhất đạt được (để biết "độ bom").
 * totalSeconds: tổng thời gian cộng dồn của cả lượt chơi.
 */
public record ScoreEntry(String playerName, String difficulty, int levelsCleared,
                         int totalSeconds, int mines, long timestamp) {

    /** Độ dài tên hiển thị tối đa, tránh làm vỡ bố cục bảng. */
    public static final int MAX_NAME_LENGTH = 20;

    public ScoreEntry {
        playerName = sanitizeName(playerName);
        difficulty = (difficulty != null) ? difficulty.trim() : "";
        levelsCleared = Math.max(0, levelsCleared);
        totalSeconds = Math.max(0, totalSeconds);
        mines = Math.max(0, mines);
    }

    /**
     * Chuẩn hóa tên: bỏ ký tự phân tách của file và ký tự xuống dòng,
     * cắt khoảng trắng thừa và giới hạn độ dài.
     */
    public static String sanitizeName(String name) {
        if (name == null) {
            return "Ẩn danh";
        }
        String cleaned = name.replace("|", " ")
                .replace("\r", " ")
                .replace("\n", " ")
                .trim();
        if (cleaned.isEmpty()) {
            return "Ẩn danh";
        }
        if (cleaned.length() > MAX_NAME_LENGTH) {
            cleaned = cleaned.substring(0, MAX_NAME_LENGTH).trim();
        }
        return cleaned;
    }

    /** Khóa so khớp "một người chơi trong một chế độ" (không phân biệt hoa thường). */
    public String key() {
        return playerName.toLowerCase() + "|" + difficulty.toLowerCase();
    }

    /** Thời gian dạng mm:ss để hiển thị trên bảng. */
    public String formattedTime() {
        return String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60);
    }
}
