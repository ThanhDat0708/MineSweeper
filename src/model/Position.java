package model;

/**
 * Tọa độ một ô trên bàn cờ (row, col đều bắt đầu từ 0).
 * Dùng record để bất biến và thay thế java.awt.Point trong tầng model.
 */
public record Position(int row, int col) implements java.io.Serializable {

    private static final long serialVersionUID = 1L;
}
