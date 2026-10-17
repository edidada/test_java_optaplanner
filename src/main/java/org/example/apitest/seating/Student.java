package org.example.apitest.seating;

/**
 * 考生：年级里的某个班级的一名学生。
 *
 * <p>{@code studentId} 为学号、{@code name} 为姓名，来自 txt 名单；
 * 旧的 (id, classId, indexInClass) 构造器保留给单元测试使用，
 * 此时学号按 {@code %08d} 自动补齐、姓名用“考生xxx”占位。</p>
 */
public class Student {

    private final int id;
    private final int classId;
    private final int indexInClass;
    private final String studentId;
    private final String name;
    private final String className;

    public Student(int id, int classId, int indexInClass) {
        this(id, classId, indexInClass, String.format("%08d", id), "考生" + indexInClass, classId + "班");
    }

    public Student(int id, int classId, int indexInClass, String studentId, String name, String className) {
        this.id = id;
        this.classId = classId;
        this.indexInClass = indexInClass;
        this.studentId = studentId;
        this.name = name;
        this.className = className;
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

    /** 学号，来自名单原文。 */
    public String getStudentId() {
        return studentId;
    }

    /** 姓名。 */
    public String getName() {
        return name;
    }

    /** 班级名称，例如“高三(1)班”。 */
    public String getClassName() {
        return className;
    }

    /** 展示用编号，例如 2 班第 5 名学生 -> "2-05"。 */
    public String getCode() {
        return String.format("%d-%02d", classId, indexInClass);
    }

    @Override
    public String toString() {
        return getCode() + "(" + name + ")";
    }
}
