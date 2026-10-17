package org.example.apitest.seating;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * 考生名单：按班级组织的 txt 名单，支持解析、随机生成和回写。
 *
 * <p>txt 格式（UTF-8）：</p>
 * <pre>
 *   # 以 # 开头的行是注释，空行忽略
 *   班级: 高三(1)班
 *   20240101,张伟
 *   20240102,王芳
 *
 *   班级: 高三(2)班
 *   20240201,李娜
 * </pre>
 *
 * <p>“班级:”行开始一个新班级（也接受全角冒号“班级：”），班级编号按出现顺序从 1 递增；
 * 其后的每一行是“学号,姓名”，分隔符兼容英文逗号、中文逗号、顿号和 Tab。</p>
 */
public class StudentRoster {

    private static final String[] SURNAMES = {
            "赵", "钱", "孙", "李", "周", "吴", "郑", "王", "冯", "陈", "褚", "卫", "蒋", "沈", "韩", "杨",
            "朱", "秦", "尤", "许", "何", "吕", "施", "张", "孔", "曹", "严", "华", "金", "魏", "薛", "雷",
            "贺", "倪", "汤", "滕", "龚", "杜", "阮", "蓝", "季", "路", "尹", "姚", "邵", "汪", "毛", "狄"
    };

    private static final String[] GIVEN_CHARS = {
            "伟", "芳", "娜", "敏", "静", "磊", "军", "洋", "勇", "艳", "杰", "娟", "涛", "明", "超", "秀",
            "霞", "平", "刚", "桂", "华", "强", "丽", "文", "辉", "力", "成", "龙", "健", "波", "斌", "新",
            "瑞", "晨", "嘉", "雨", "倩", "宇", "浩", "欣", "泽", "琳", "瑶", "翔", "悦", "岩", "松", "楠"
    };

    private final List<RosterClass> classes;

    public StudentRoster(List<RosterClass> classes) {
        if (classes == null || classes.isEmpty()) {
            throw new IllegalArgumentException("名单里至少要有一个班级");
        }
        this.classes = List.copyOf(classes);
        assertStudentIdsUnique();
    }

    /** 一个班级的名单：班级编号、班级名称、本班考生。 */
    public record RosterClass(int classId, String className, List<Student> students) {
        public RosterClass {
            students = List.copyOf(students);
        }
    }

    public List<RosterClass> getClasses() {
        return classes;
    }

    /** 全部考生，按班级顺序拼接。 */
    public List<Student> allStudents() {
        List<Student> all = new ArrayList<>();
        classes.forEach(rosterClass -> all.addAll(rosterClass.students()));
        return all;
    }

    public int getStudentCount() {
        return allStudents().size();
    }

    // ------------------------------------------------------------------ 解析

    public static StudentRoster readFromFile(Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            throw new IOException("名单文件不存在：" + file.toAbsolutePath());
        }
        return parse(Files.readAllLines(file, StandardCharsets.UTF_8));
    }

    static StudentRoster parse(List<String> lines) {
        List<RosterClass> classes = new ArrayList<>();
        String className = null;
        List<Student> pendingStudents = null;
        int classId = 0;
        int globalId = 0;

        for (int lineNo = 0; lineNo < lines.size(); lineNo++) {
            String line = lines.get(lineNo).strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            String classPrefix = line.startsWith("班级：") ? "班级：" : (line.startsWith("班级:") ? "班级:" : null);
            if (classPrefix != null) {
                closeClass(classes, classId, className, pendingStudents);
                className = line.substring(classPrefix.length()).strip();
                if (className.isEmpty()) {
                    throw new IllegalArgumentException("第 " + (lineNo + 1) + " 行：班级名为空");
                }
                pendingStudents = new ArrayList<>();
                classId++;
                continue;
            }

            if (pendingStudents == null) {
                throw new IllegalArgumentException("第 " + (lineNo + 1) + " 行之前缺少“班级:”行：" + line);
            }

            String[] parts = splitStudentLine(line);
            if (parts == null) {
                throw new IllegalArgumentException("第 " + (lineNo + 1) + " 行不是“学号,姓名”格式：" + line);
            }
            globalId++;
            pendingStudents.add(new Student(globalId, classId, pendingStudents.size() + 1, parts[0], parts[1],
                    className));
        }

        closeClass(classes, classId, className, pendingStudents);
        if (classes.isEmpty()) {
            throw new IllegalArgumentException("名单里没有解析到任何班级");
        }
        return new StudentRoster(classes);
    }

    private static void closeClass(List<RosterClass> classes, int classId, String className,
            List<Student> students) {
        if (students != null) {
            if (students.isEmpty()) {
                throw new IllegalArgumentException("班级“" + className + "”里没有任何考生");
            }
            classes.add(new RosterClass(classId, className, students));
        }
    }

    private static String[] splitStudentLine(String line) {
        for (String separator : new String[] {",", "，", "、", "\t"}) {
            int idx = line.indexOf(separator);
            if (idx > 0) {
                String studentId = line.substring(0, idx).strip();
                String name = line.substring(idx + separator.length()).strip();
                if (!studentId.isEmpty() && !name.isEmpty()) {
                    return new String[] {studentId, name};
                }
                return null;
            }
        }
        return null;
    }

    private void assertStudentIdsUnique() {
        Set<String> seen = new HashSet<>();
        for (Student student : allStudents()) {
            if (!seen.add(student.getStudentId())) {
                throw new IllegalArgumentException("学号重复：" + student.getStudentId()
                        + "（" + student.getClassName() + " " + student.getName() + "）");
            }
        }
    }

    // ------------------------------------------------------------------ 随机生成

    /**
     * 随机生成一份名单：{@code classCount} 个班级，每班 {@code studentsPerClass} 人，
     * 学号为 入学年份 + 班级序号(2 位) + 座号(2 位)，例如 20240307。
     */
    public static StudentRoster generate(int classCount, int studentsPerClass, String grade, int enrollYear,
            long seed) {
        if (classCount < 1 || studentsPerClass < 1) {
            throw new IllegalArgumentException("班级数和每班人数必须大于 0");
        }
        Random random = new Random(seed);
        List<RosterClass> classes = new ArrayList<>();
        List<Student> students = new ArrayList<>();
        int globalId = 0;

        for (int classId = 1; classId <= classCount; classId++) {
            String className = grade + "(" + classId + ")班";
            students = new ArrayList<>();
            for (int indexInClass = 1; indexInClass <= studentsPerClass; indexInClass++) {
                globalId++;
                String studentId = String.format("%04d%02d%02d", enrollYear, classId, indexInClass);
                students.add(new Student(globalId, classId, indexInClass, studentId,
                        randomName(random), className));
            }
            classes.add(new RosterClass(classId, className, students));
        }
        return new StudentRoster(classes);
    }

    private static String randomName(Random random) {
        String surname = SURNAMES[random.nextInt(SURNAMES.length)];
        int givenLength = random.nextInt(10) < 6 ? 2 : 1;
        StringBuilder given = new StringBuilder();
        for (int i = 0; i < givenLength; i++) {
            given.append(GIVEN_CHARS[random.nextInt(GIVEN_CHARS.length)]);
        }
        return surname + given;
    }

    // ------------------------------------------------------------------ 回写

    /** 写成可再次解析的 txt 名单。 */
    public String toTxt() {
        StringBuilder builder = new StringBuilder();
        builder.append("# 考试考生名单（UTF-8，按班级分组）\n");
        builder.append("# 用法：一行“班级: 班级名”开始一个班级，其后每行“学号,姓名”。\n");
        builder.append("# 以 # 开头的行是注释，空行会被忽略。\n");
        for (RosterClass rosterClass : classes) {
            builder.append('\n').append("班级: ").append(rosterClass.className()).append('\n');
            for (Student student : rosterClass.students()) {
                builder.append(student.getStudentId()).append(',').append(student.getName()).append('\n');
            }
        }
        return builder.toString();
    }

    public void writeToFile(Path file) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        Files.writeString(file, toTxt(), StandardCharsets.UTF_8);
    }
}
