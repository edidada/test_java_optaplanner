package org.example.apitest.seating;

import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 演示入口：英语机房登录电脑口语考试排座位。
 *
 * <p>用法：SeatExamApp [班级数 n] [每班人数 m] [机房座位数 x] [求解秒数]，
 * 默认 4 个班、每班 30 人、60 个座位、求解 2 秒。</p>
 */
public class SeatExamApp {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeatExamApp.class);

    public static void main(String[] args) {
        int classCount = args.length > 0 ? Integer.parseInt(args[0]) : 4;
        int studentsPerClass = args.length > 1 ? Integer.parseInt(args[1]) : 30;
        int seatCount = args.length > 2 ? Integer.parseInt(args[2]) : 60;
        int solveSeconds = args.length > 3 ? Integer.parseInt(args[3]) : 2;

        ExamSchedule schedule = new ExamSchedule(classCount, studentsPerClass, seatCount);
        LOGGER.info("英语口语考试排座：{} 个班级 × 每班 {} 人 = {} 名考生，机房 {} 个座位，拆分为 {} 场次，每场求解 {} 秒",
                schedule.getClassCount(), schedule.getStudentsPerClass(),
                classCount * studentsPerClass, schedule.getSeatCount(),
                schedule.getSessionCount(), solveSeconds);

        ExamSeatPlanner planner = new ExamSeatPlanner(Duration.ofSeconds(solveSeconds));
        for (int sessionIndex = 0; sessionIndex < schedule.getSessionCount(); sessionIndex++) {
            List<Student> students = schedule.getSessionStudents(sessionIndex);
            ExamSeatSolution solution = planner.solve(students, schedule.getSeats());
            List<String> problems = SeatConstraintProvider.validate(solution);

            LOGGER.info("=== 第 {} 场：{} 名考生 / {} 个座位，得分 {} ===",
                    sessionIndex + 1, students.size(), schedule.getSeatCount(), solution.getScore());
            LOGGER.info("\n" + ExamSeatPlanner.renderGrid(solution));
            if (problems.isEmpty()) {
                LOGGER.info("校验通过：无空座、无重复占座、同班考生均未相邻");
            } else {
                LOGGER.warn("校验发现问题：{}", problems);
            }
        }
    }
}
