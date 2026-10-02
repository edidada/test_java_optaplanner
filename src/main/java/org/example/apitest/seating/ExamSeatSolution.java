package org.example.apitest.seating;

import java.util.List;

import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty;
import org.optaplanner.core.api.domain.solution.PlanningScore;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.domain.solution.ProblemFactCollectionProperty;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;

/**
 * 一场考试的排座问题：考生列表 + 机房座位 + 座位绑定变量。
 */
@PlanningSolution
public class ExamSeatSolution {

    private List<Student> students;
    private List<Seat> seats;
    private List<SeatAssignment> assignments;
    private HardSoftScore score;

    @ProblemFactCollectionProperty
    public List<Student> getStudents() {
        return students;
    }

    public void setStudents(List<Student> students) {
        this.students = students;
    }

    @ValueRangeProvider(id = "seatRange")
    @ProblemFactCollectionProperty
    public List<Seat> getSeats() {
        return seats;
    }

    public void setSeats(List<Seat> seats) {
        this.seats = seats;
    }

    @PlanningEntityCollectionProperty
    public List<SeatAssignment> getAssignments() {
        return assignments;
    }

    public void setAssignments(List<SeatAssignment> assignments) {
        this.assignments = assignments;
    }

    @PlanningScore
    public HardSoftScore getScore() {
        return score;
    }

    public void setScore(HardSoftScore score) {
        this.score = score;
    }
}
