package model;

/**
 * Một ô trên bàn cờ. Chỉ dùng nội bộ trong package model,
 * không lộ ra ngoài để tránh bị sửa đổi tùy tiện.
 */
final class Cell implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private boolean mine;
    private int adjacentMines;
    private CellState state = CellState.HIDDEN;
    private boolean exploded;
    private boolean wrongFlag;
    private boolean defused;

    boolean isMine() {
        return mine;
    }

    void setMine(boolean mine) {
        this.mine = mine;
    }

    int getAdjacentMines() {
        return adjacentMines;
    }

    void setAdjacentMines(int adjacentMines) {
        this.adjacentMines = adjacentMines;
    }

    CellState getState() {
        return state;
    }

    void setState(CellState state) {
        this.state = state;
    }

    boolean isExploded() {
        return exploded;
    }

    void setExploded(boolean exploded) {
        this.exploded = exploded;
    }

    boolean isWrongFlag() {
        return wrongFlag;
    }

    void setWrongFlag(boolean wrongFlag) {
        this.wrongFlag = wrongFlag;
    }

    boolean isDefused() {
        return defused;
    }

    void setDefused(boolean defused) {
        this.defused = defused;
    }
}
