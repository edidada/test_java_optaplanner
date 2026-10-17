package org.example.apitest.seating;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 排座服务：把名单里的考生先分配到各考场（教室），再用 OptaPlanner 决定每人在自己考场里的座位。
 *
 * <p>两步拆分：
 * <ol>
 *   <li>跨考场：按班级轮流取人（1 班 1 人、2 班 1 人……）拉成一条队列后按考场容量切块，
 *       保证同一考场的考生来自尽量多的班级，且各考场人数尽量均衡；</li>
 *   <li>考场内：OptaPlanner 求解，硬约束“一个座位一人 + 同班不相邻”，软约束“同班尽量不同排”。</li>
 * </ol>
 */
public class ExamSeatingService {

    private final Duration perRoomLimit;

    public ExamSeatingService(Duration perRoomLimit) {
        this.perRoomLimit = perRoomLimit;
    }

    /** 一名考生最终落到某个考场的某个座位。 */
    public record Placement(Student student, ExamRoom room, Seat seat) {
    }

    /** 一个考场的求解结果。 */
    public record RoomResult(ExamRoom room, List<Student> students, ExamSeatSolution solution,
            List<Placement> placements, List<String> problems) {

        public int getUsedSeatCount() {
            return placements.size();
        }
    }

    public List<RoomResult> arrange(StudentRoster roster, List<ExamRoom> rooms) {
        List<Student> queue = interleaveByClass(roster);
        List<List<Student>> groups = splitIntoRooms(queue, rooms);

        ExamSeatPlanner planner = new ExamSeatPlanner(perRoomLimit);
        List<RoomResult> results = new ArrayList<>();
        for (int roomIndex = 0; roomIndex < groups.size(); roomIndex++) {
            ExamRoom room = rooms.get(roomIndex);
            List<Student> group = groups.get(roomIndex);
            ExamSeatSolution solution = planner.solve(group, room.getSeats());
            List<String> problems = SeatConstraintProvider.validate(solution);

            List<Placement> placements = new ArrayList<>();
            for (SeatAssignment assignment : solution.getAssignments()) {
                if (assignment.getSeat() != null) {
                    placements.add(new Placement(assignment.getStudent(), room, assignment.getSeat()));
                }
            }
            placements.sort(Comparator.comparingInt(placement -> placement.seat().getIndex()));
            results.add(new RoomResult(room, group, solution, placements, problems));
        }
        return results;
    }

    /** 按班级轮流取人：先各班第 1 名，再各班第 2 名…… */
    static List<Student> interleaveByClass(StudentRoster roster) {
        List<List<Student>> perClass = roster.getClasses().stream()
                .map(StudentRoster.RosterClass::students)
                .toList();
        List<Student> queue = new ArrayList<>(roster.getStudentCount());
        int maxClassSize = perClass.stream().mapToInt(List::size).max().orElse(0);
        for (int position = 0; position < maxClassSize; position++) {
            for (List<Student> students : perClass) {
                if (position < students.size()) {
                    queue.add(students.get(position));
                }
            }
        }
        return queue;
    }

    /**
     * 决定使用哪些考场、每个考场多少人：考场数取“最少需要的间数”，人数在启用考场之间尽量平均分。
     */
    static List<List<Student>> splitIntoRooms(List<Student> queue, List<ExamRoom> rooms) {
        int total = queue.size();
        if (rooms.isEmpty()) {
            throw new IllegalArgumentException("没有可用考场");
        }
        int totalCapacity = rooms.stream().mapToInt(ExamRoom::getCapacity).sum();
        if (total > totalCapacity) {
            throw new IllegalArgumentException("考生 " + total + " 人，所有考场座位加起来只有 "
                    + totalCapacity + " 个，请增加考场");
        }
        int largestCapacity = rooms.stream().mapToInt(ExamRoom::getCapacity).max().orElse(0);
        if (largestCapacity <= 0) {
            throw new IllegalArgumentException("考场没有座位");
        }
        int roomsNeeded = (int) Math.ceil((double) total / largestCapacity);
        if (roomsNeeded > rooms.size()) {
            throw new IllegalArgumentException("需要 " + roomsNeeded + " 间考场，实际只准备了 " + rooms.size() + " 间");
        }

        int base = total / roomsNeeded;
        int remainder = total % roomsNeeded;

        List<List<Student>> groups = new ArrayList<>();
        int cursor = 0;
        for (int roomIndex = 0; roomIndex < roomsNeeded; roomIndex++) {
            int size = base + (roomIndex < remainder ? 1 : 0);
            if (size > rooms.get(roomIndex).getCapacity()) {
                throw new IllegalArgumentException(rooms.get(roomIndex) + " 容量不足：" + size + " 人 > "
                        + rooms.get(roomIndex).getCapacity() + " 座");
            }
            groups.add(new ArrayList<>(queue.subList(cursor, cursor + size)));
            cursor += size;
        }
        if (cursor != total) {
            throw new IllegalStateException("考生分配不完整：已分配 " + cursor + " / " + total);
        }
        return groups;
    }
}
