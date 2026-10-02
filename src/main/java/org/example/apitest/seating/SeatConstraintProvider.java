package org.example.apitest.seating;

import java.util.List;

import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;
import org.optaplanner.core.api.score.stream.Joiners;

/**
 * 口语考试排座约束：
 * <ul>
 *   <li>硬约束：一个座位只能坐一名考生（防重复占用）</li>
 *   <li>硬约束：同班同学不能前后左右相邻（防作弊）</li>
 *   <li>软约束：同班同学尽量分散在不同排（降低交头接耳风险）</li>
 * </ul>
 */
public class SeatConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[] {
                seatConflict(constraintFactory),
                sameClassAdjacent(constraintFactory),
                sameClassSameRow(constraintFactory)
        };
    }

    private Constraint seatConflict(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SeatAssignment.class)
                .filter(assignment -> assignment.getSeat() != null)
                .join(SeatAssignment.class,
                        Joiners.equal(SeatAssignment::getSeat),
                        Joiners.greaterThan(SeatAssignment::getIndex))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("seatConflict");
    }

    private Constraint sameClassAdjacent(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SeatAssignment.class)
                .filter(assignment -> assignment.getSeat() != null)
                .join(SeatAssignment.class,
                        Joiners.equal(SeatAssignment::getClassId),
                        Joiners.greaterThan(SeatAssignment::getIndex))
                .filter((left, right) -> left.getSeat().isAdjacentTo(right.getSeat()))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("sameClassAdjacent");
    }

    private Constraint sameClassSameRow(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(SeatAssignment.class)
                .filter(assignment -> assignment.getSeat() != null)
                .join(SeatAssignment.class,
                        Joiners.equal(SeatAssignment::getClassId),
                        Joiners.greaterThan(SeatAssignment::getIndex))
                .filter((left, right) -> left.getSeat().isSameRow(right.getSeat()))
                .penalize(HardSoftScore.ONE_SOFT)
                .asConstraint("sameClassSameRow");
    }

    /** 校验最终方案，返回问题列表（为空表示方案合法）。 */
    public static List<String> validate(ExamSeatSolution solution) {
        java.util.ArrayList<String> problems = new java.util.ArrayList<>();
        List<SeatAssignment> assignments = solution.getAssignments();

        java.util.Set<Seat> usedSeats = new java.util.HashSet<>();
        for (SeatAssignment assignment : assignments) {
            if (assignment.getSeat() == null) {
                problems.add("考生 " + assignment.getStudent().getCode() + " 没有分配到座位");
            } else if (!usedSeats.add(assignment.getSeat())) {
                problems.add("座位 " + assignment.getSeat().getLabel() + " 被重复占用");
            }
        }

        for (int i = 0; i < assignments.size(); i++) {
            SeatAssignment left = assignments.get(i);
            if (left.getSeat() == null) {
                continue;
            }
            for (int j = i + 1; j < assignments.size(); j++) {
                SeatAssignment right = assignments.get(j);
                if (right.getSeat() == null) {
                    continue;
                }
                if (left.getClassId() == right.getClassId()
                        && left.getSeat().isAdjacentTo(right.getSeat())) {
                    problems.add("同班考生 " + left.getStudent().getCode() + " 与 "
                            + right.getStudent().getCode() + " 相邻（"
                            + left.getSeat().getLabel() + " / " + right.getSeat().getLabel() + "）");
                }
            }
        }
        return problems;
    }
}
