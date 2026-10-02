package org.example.apitest.seating;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 一场年级英语口语考试的编排：n 个班级、每班 m 名考生、机房 x 个座位。
 * 考生总数超过座位数时自动拆分成多场次（同一套机房座位循环使用）。
 */
public class ExamSchedule {

    private final int classCount;
    private final int studentsPerClass;
    private final int seatCount;
    private final List<Seat> seats = new ArrayList<>();
    private final List<List<Student>> sessions = new ArrayList<>();

    public ExamSchedule(int classCount, int studentsPerClass, int seatCount) {
        if (classCount < 1 || studentsPerClass < 1 || seatCount < 1) {
            throw new IllegalArgumentException("班级数、每班人数、座位数都必须大于 0");
        }
        this.classCount = classCount;
        this.studentsPerClass = studentsPerClass;
        this.seatCount = seatCount;

        int columns = (int) Math.ceil(Math.sqrt(seatCount));
        for (int i = 0; i < seatCount; i++) {
            seats.add(new Seat(i, i / columns + 1, i % columns + 1));
        }

        int totalStudents = classCount * studentsPerClass;
        int sessionCount = (int) Math.ceil((double) totalStudents / seatCount);
        for (int i = 0; i < sessionCount; i++) {
            sessions.add(new ArrayList<>());
        }

        // 交错分配场次，保证每场次里各班考生尽量均匀，避免出现“整场都是一个班”的情况。
        for (int classId = 1; classId <= classCount; classId++) {
            for (int indexInClass = 1; indexInClass <= studentsPerClass; indexInClass++) {
                int id = (classId - 1) * studentsPerClass + indexInClass;
                Student student = new Student(id, classId, indexInClass);
                sessions.get((id - 1) % sessionCount).add(student);
            }
        }
    }

    public int getClassCount() {
        return classCount;
    }

    public int getStudentsPerClass() {
        return studentsPerClass;
    }

    public int getSeatCount() {
        return seatCount;
    }

    public int getSessionCount() {
        return sessions.size();
    }

    public List<Seat> getSeats() {
        return Collections.unmodifiableList(seats);
    }

    public List<Student> getSessionStudents(int sessionIndex) {
        return Collections.unmodifiableList(sessions.get(sessionIndex));
    }
}
