package org.example.apitest.seating;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 一个考场：某楼栋某楼层的一间教室，座位按“排 × 每排座数”排列。
 *
 * <p>座位的 {@code Seat.getIndex()} 是教室内 0 基序号，导出时的“教室座位总序号”为
 * {@code index + 1}；“第几排”取 {@code Seat.getRow()}，“该排第几号”取 {@code Seat.getCol()}。</p>
 */
public class ExamRoom {

    private final String building;
    private final int floor;
    private final String roomNumber;
    private final int rows;
    private final int columns;
    private final List<Seat> seats;
    private final List<String> invigilators;

    public ExamRoom(String building, int floor, String roomNumber, int rows, int columns,
            List<String> invigilators) {
        if (rows < 1 || columns < 1) {
            throw new IllegalArgumentException("教室的排数和每排座数必须大于 0");
        }
        this.building = building;
        this.floor = floor;
        this.roomNumber = roomNumber;
        this.rows = rows;
        this.columns = columns;
        this.invigilators = List.copyOf(invigilators);

        List<Seat> generated = new ArrayList<>(rows * columns);
        int index = 0;
        for (int row = 1; row <= rows; row++) {
            for (int column = 1; column <= columns; column++) {
                generated.add(new Seat(index++, row, column));
            }
        }
        this.seats = Collections.unmodifiableList(generated);
    }

    /** 楼栋，例如“A栋”。 */
    public String getBuilding() {
        return building;
    }

    /** 楼层。 */
    public int getFloor() {
        return floor;
    }

    /** 教室编号，例如“A101”。 */
    public String getRoomNumber() {
        return roomNumber;
    }

    /** 监考教师编号。 */
    public List<String> getInvigilators() {
        return invigilators;
    }

    public String getInvigilatorText() {
        return String.join("、", invigilators);
    }

    public int getRows() {
        return rows;
    }

    public int getColumns() {
        return columns;
    }

    public List<Seat> getSeats() {
        return seats;
    }

    /** 教室座位容量。 */
    public int getCapacity() {
        return seats.size();
    }

    /** 教学楼层，用于排序与展示。 */
    public String getFloorLabel() {
        return floor + "层";
    }

    @Override
    public String toString() {
        return building + " " + roomNumber + "（" + rows + "排×" + columns + "座，容量 " + getCapacity() + "）";
    }
}
