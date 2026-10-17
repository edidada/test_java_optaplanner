package org.example.apitest.seating;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class StudentRosterTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void parsesClassesFromTxtAndToleratesCommentsAndFullWidthSeparators() {
        List<String> lines = List.of(
                "# 这是注释",
                "",
                "班级: 高三(1)班",
                "20240101,张伟",
                "20240102，王芳",
                "班级：高三(2)班",
                "20240201\t李娜");

        StudentRoster roster = StudentRoster.parse(lines);

        assertEquals(2, roster.getClasses().size());
        assertEquals(3, roster.getStudentCount());
        assertEquals("王芳", roster.getClasses().get(0).students().get(1).getName());
        assertEquals(2, roster.getClasses().get(0).students().get(1).getIndexInClass());
        assertEquals("20240101", roster.getClasses().get(0).students().get(0).getStudentId());
        assertEquals("高三(2)班", roster.getClasses().get(1).className());
        assertEquals("高三(2)班", roster.getClasses().get(1).students().get(0).getClassName());
        assertEquals(2, roster.getClasses().get(1).classId());
    }

    @Test
    public void rejectsDuplicatedStudentId() {
        List<String> lines = List.of("班级: 一班", "1001,张三", "1001,李四");
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> StudentRoster.parse(lines));
        assertTrue(error.getMessage(), error.getMessage().contains("学号重复"));
    }

    @Test
    public void rejectsStudentLineWithoutClassNameHeader() {
        List<String> lines = List.of("1001,张三");
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> StudentRoster.parse(lines));
        assertTrue(error.getMessage(), error.getMessage().contains("班级"));
    }

    @Test
    public void generatedRosterRoundTripsThroughTxtFile() throws IOException {
        StudentRoster generated = StudentRoster.generate(3, 10, "高二", 2025, 42L);
        Path file = folder.newFile("roster.txt").toPath();
        generated.writeToFile(file);

        StudentRoster reloaded = StudentRoster.readFromFile(file);

        assertEquals(generated.getStudentCount(), reloaded.getStudentCount());
        assertEquals(30, reloaded.getStudentCount());
        for (int index = 0; index < 30; index++) {
            assertEquals(generated.allStudents().get(index).getStudentId(),
                    reloaded.allStudents().get(index).getStudentId());
            assertEquals(generated.allStudents().get(index).getName(),
                    reloaded.allStudents().get(index).getName());
        }
        assertTrue(reloaded.allStudents().get(0).getStudentId().startsWith("2025"));
    }

    @Test
    public void sameSeedGeneratesTheSameRoster() {
        StudentRoster first = StudentRoster.generate(2, 5, "高一", 2024, 7L);
        StudentRoster second = StudentRoster.generate(2, 5, "高一", 2024, 7L);
        assertEquals(first.allStudents().toString(), second.allStudents().toString());
    }
}
