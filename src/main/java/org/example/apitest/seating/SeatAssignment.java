package org.example.apitest.seating;

import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.variable.PlanningVariable;

/**
 * 排座变量：把一名考生绑定到一个座位上。
 */
@PlanningEntity
public class SeatAssignment {

    private int index;
    private Student student;

    private Seat seat;

    public SeatAssignment() {
    }

    public SeatAssignment(int index, Student student) {
        this.index = index;
        this.student = student;
    }

    public int getIndex() {
        return index;
    }

    public Student getStudent() {
        return student;
    }

    public int getClassId() {
        return student.getClassId();
    }

    @PlanningVariable(valueRangeProviderRefs = "seatRange")
    public Seat getSeat() {
        return seat;
    }

    public void setSeat(Seat seat) {
        this.seat = seat;
    }

    @Override
    public String toString() {
        return student.getCode() + "@" + (seat == null ? "-" : seat.getLabel());
    }
}
