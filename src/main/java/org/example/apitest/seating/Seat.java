package org.example.apitest.seating;

/**
 * 机房座位，按行列编号。
 */
public class Seat {

    private final int index;
    private final int row;
    private final int col;

    public Seat(int index, int row, int col) {
        this.index = index;
        this.row = row;
        this.col = col;
    }

    public int getIndex() {
        return index;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public String getLabel() {
        return "R" + row + "C" + col;
    }

    /** 是否是左右或者前后紧邻的座位。 */
    public boolean isAdjacentTo(Seat other) {
        return other != null && Math.abs(row - other.row) + Math.abs(col - other.col) == 1;
    }

    /** 同一排（前后相邻的风险更高）。 */
    public boolean isSameRow(Seat other) {
        return other != null && row == other.row;
    }

    @Override
    public String toString() {
        return getLabel();
    }
}
