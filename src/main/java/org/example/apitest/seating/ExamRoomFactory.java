package org.example.apitest.seating;

import java.util.ArrayList;
import java.util.List;

/**
 * 按“楼栋 → 楼层 → 教室”批量生成考场，并给每间教室配两名监考教师。
 *
 * <p>教室编号规则：楼栋字母 + 楼层 + 楼层内两位序号，例如 A 栋 1 层第 2 间为 A102。</p>
 */
public class ExamRoomFactory {

    private static final char[] BUILDING_LETTERS = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H'};

    private final int buildingCount;
    private final int floorsPerBuilding;
    private final int roomsPerFloor;
    private final int rows;
    private final int columns;

    public ExamRoomFactory(int buildingCount, int floorsPerBuilding, int roomsPerFloor, int rows, int columns) {
        if (buildingCount < 1 || buildingCount > BUILDING_LETTERS.length) {
            throw new IllegalArgumentException("楼栋数必须在 1 到 " + BUILDING_LETTERS.length + " 之间");
        }
        if (floorsPerBuilding < 1 || roomsPerFloor < 1) {
            throw new IllegalArgumentException("每栋楼层数和每层教室数必须大于 0");
        }
        this.buildingCount = buildingCount;
        this.floorsPerBuilding = floorsPerBuilding;
        this.roomsPerFloor = roomsPerFloor;
        this.rows = rows;
        this.columns = columns;
    }

    /** 生成考点的全部教室（按楼栋、楼层、编号顺序）。 */
    public List<ExamRoom> createAll() {
        List<ExamRoom> rooms = new ArrayList<>();
        int teacherSeq = 1001;
        for (int buildingIndex = 0; buildingIndex < buildingCount; buildingIndex++) {
            char letter = BUILDING_LETTERS[buildingIndex];
            String building = letter + "栋";
            for (int floor = 1; floor <= floorsPerBuilding; floor++) {
                for (int roomIndex = 1; roomIndex <= roomsPerFloor; roomIndex++) {
                    String roomNumber = "" + letter + floor + String.format("%02d", roomIndex);
                    List<String> invigilators = List.of(String.format("T%04d", teacherSeq),
                            String.format("T%04d", teacherSeq + 1));
                    teacherSeq += 2;
                    rooms.add(new ExamRoom(building, floor, roomNumber, rows, columns, invigilators));
                }
            }
        }
        return rooms;
    }

    /** 单间接待容量。 */
    public int roomCapacity() {
        return rows * columns;
    }

    /** 整个考点的座位总数。 */
    public int totalCapacity() {
        return buildingCount * floorsPerBuilding * roomsPerFloor * roomCapacity();
    }
}
