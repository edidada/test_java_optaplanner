package org.example.apitest.seating;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;

public class SeatArrangementTest {

    @Test
    public void scheduleSplitsSessionsWhenStudentsExceedSeats() {
        ExamSchedule schedule = new ExamSchedule(4, 30, 60);

        assertEquals(120, schedule.getClassCount() * schedule.getStudentsPerClass());
        assertEquals(60, schedule.getSeats().size());
        assertEquals(2, schedule.getSessionCount());

        Set<Integer> allIds = new HashSet<>();
        for (int session = 0; session < schedule.getSessionCount(); session++) {
            List<Student> students = schedule.getSessionStudents(session);
            assertTrue(students.size() <= schedule.getSeatCount());
            students.forEach(student -> allIds.add(student.getId()));
        }
        assertEquals(120, allIds.size());
    }

    @Test
    public void plannerPutsEveryStudentInAUniqueSeatWithoutClassmateNeighbours() {
        ExamSchedule schedule = new ExamSchedule(4, 8, 32);
        ExamSeatPlanner planner = new ExamSeatPlanner(Duration.ofSeconds(2));

        ExamSeatSolution solution = planner.solve(schedule.getSessionStudents(0), schedule.getSeats());

        assertEquals("硬约束得分必须为 0", 0, solution.getScore().hardScore());
        assertEquals(32, solution.getAssignments().size());
        assertTrue(SeatConstraintProvider.validate(solution).isEmpty());
    }

    @Test
    public void validatorDetectsClassmatesPlacedNextToEachOther() {
        ExamSchedule schedule = new ExamSchedule(2, 2, 4);
        List<Seat> seats = schedule.getSeats();

        SeatAssignment left = new SeatAssignment(0, new Student(1, 1, 1));
        left.setSeat(seats.get(0));
        SeatAssignment right = new SeatAssignment(1, new Student(2, 1, 2));
        right.setSeat(seats.get(1));
        SeatAssignment unassigned = new SeatAssignment(2, new Student(3, 2, 1));

        ExamSeatSolution solution = new ExamSeatSolution();
        solution.setSeats(seats);
        solution.setAssignments(List.of(left, right, unassigned));
        solution.setScore(HardSoftScore.of(-2, 0));

        List<String> problems = SeatConstraintProvider.validate(solution);
        assertEquals(problems.toString(), 2, problems.size());
        assertTrue(problems.stream().anyMatch(problem -> problem.contains("相邻")));
        assertTrue(problems.stream().anyMatch(problem -> problem.contains("没有分配到座位")));
    }
}
