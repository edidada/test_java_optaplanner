package org.example.apitest.seating;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.config.solver.SolverConfig;

/**
 * 用 OptaPlanner 求解一场口语考试的座位安排。
 */
public class ExamSeatPlanner {

    private final Duration spentLimit;

    public ExamSeatPlanner(Duration spentLimit) {
        this.spentLimit = spentLimit;
    }

    public ExamSeatSolution solve(List<Student> sessionStudents, List<Seat> seats) {
        List<SeatAssignment> assignments = new ArrayList<>();
        for (int i = 0; i < sessionStudents.size(); i++) {
            assignments.add(new SeatAssignment(i, sessionStudents.get(i)));
        }

        ExamSeatSolution problem = new ExamSeatSolution();
        problem.setStudents(sessionStudents);
        problem.setSeats(new ArrayList<>(seats));
        problem.setAssignments(assignments);

        SolverFactory<ExamSeatSolution> solverFactory = SolverFactory.create(new SolverConfig()
                .withSolutionClass(ExamSeatSolution.class)
                .withEntityClasses(SeatAssignment.class)
                .withConstraintProviderClass(SeatConstraintProvider.class)
                .withTerminationSpentLimit(spentLimit));
        Solver<ExamSeatSolution> solver = solverFactory.buildSolver();
        return solver.solve(problem);
    }

    /** 把座位表渲染成机房平面图，单元格内容为考生编号（班级-序号）。 */
    public static String renderGrid(ExamSeatSolution solution) {
        Map<Seat, String> occupied = new HashMap<>();
        for (SeatAssignment assignment : solution.getAssignments()) {
            if (assignment.getSeat() != null) {
                occupied.put(assignment.getSeat(), assignment.getStudent().getCode());
            }
        }

        int cellWidth = 4;
        for (String code : occupied.values()) {
            cellWidth = Math.max(cellWidth, code.length() + 2);
        }

        Map<Integer, List<Seat>> seatsByRow = new TreeMap<>();
        for (Seat seat : solution.getSeats()) {
            seatsByRow.computeIfAbsent(seat.getRow(), key -> new ArrayList<>()).add(seat);
        }
        seatsByRow.values().forEach(row -> row.sort(Comparator.comparingInt(Seat::getCol)));

        StringBuilder builder = new StringBuilder();
        for (Map.Entry<Integer, List<Seat>> rowEntry : seatsByRow.entrySet()) {
            builder.append(String.format("%4s", "R" + rowEntry.getKey())).append(" |");
            for (Seat seat : rowEntry.getValue()) {
                String content = occupied.getOrDefault(seat, "");
                builder.append(String.format(" %" + (cellWidth - 1) + "s |", content));
            }
            builder.append(System.lineSeparator());
        }
        return builder.toString();
    }
}
