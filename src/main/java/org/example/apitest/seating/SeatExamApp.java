package org.example.apitest.seating;

import java.time.Duration;
import java.util.List;

/**
 * 演示入口：英语机房登录电脑口语考试排座位。
 *
 * <p>用法：SeatExamApp [班级数 n] [每班人数 m] [机房座位数 x]，默认 4 个班、每班 30 人、60 个座位。</p>
 */
public class SeatExamApp {

    public static void main(String[] args) {
        int classCount = args.length > 0 ? Integer.parseInt(args[0]) : 4;
        int studentsPerClass = args.length > 1 ? Integer.parseInt(args[1]) : 30;
        int seatCount = args.length > 2 ? Integer.parseInt(args[2]) : 60;

        ExamSchedule schedule = new ExamSchedule(classCount, studentsPerClass, seatCount);
        System.out.printf("英语口语考试排座：%d 个班级 × 每班 %d 人 = %d 名考生，机房 %d 个座位，拆分为 %d 场次%n%n",
                schedule.getClassCount(), schedule.getStudentsPerClass(),
                classCount * studentsPerClass, schedule.getSeatCount(), schedule.getSessionCount());

        ExamSeatPlanner planner = new ExamSeatPlanner(Duration.ofSeconds(2));
        for (int sessionIndex = 0; sessionIndex < schedule.getSessionCount(); sessionIndex++) {
            List<Student> students = schedule.getSessionStudents(sessionIndex);
            ExamSeatSolution solution = planner.solve(students, schedule.getSeats());
            List<String> problems = SeatConstraintProvider.validate(solution);

            System.out.printf("=== 第 %d 场：%d 名考生 / %d 个座位，得分 %s ===%n",
                    sessionIndex + 1, students.size(), schedule.getSeatCount(), solution.getScore());
            System.out.print(ExamSeatPlanner.renderGrid(solution));
            if (problems.isEmpty()) {
                System.out.println("校验通过：无空座、无重复占座、同班考生均未相邻");
            } else {
                System.out.println("校验发现问题：");
                problems.forEach(problem -> System.out.println("  - " + problem));
            }
            System.out.println();
        }
    }
}
