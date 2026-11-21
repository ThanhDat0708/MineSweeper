package model;

/**
 * Trạng thái hiển thị của một ô trên bàn cờ.
 */
public enum CellState {
    HIDDEN,
    REVEALED,
    FLAGGED,
    /** Ô đã đánh dấu hỏi: chưa chắc là bom, không tính vào bộ đếm cờ. */
    QUESTION
}
