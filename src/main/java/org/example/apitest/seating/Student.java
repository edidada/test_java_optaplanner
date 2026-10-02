package org.example.apitest.seating;

/**
 * 考生：年级里的某个班级的一名学生。
 */
public class Student {

    private final int id;
    private final int classId;
    private final int indexInClass;

    public Student(int id, int classId, int indexInClass) {
        this.id = id;
        this.classId = classId;
        this.indexInClass = indexInClass;
    }

    public int getId() {
        return id;
    }

    public int getClassId() {
        return classId;
    }

    public int getIndexInClass() {
        return indexInClass;
    }

    /** 展示用编号，例如 2 班第 5 名学生 -> "2-05"。 */
    public String getCode() {
        return String.format("%d-%02d", classId, indexInClass);
    }

    @Override
    public String toString() {
        return getCode();
    }
}
