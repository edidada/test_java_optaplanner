package org.example.apitest.seating;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ExamSeatExcelExporterTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    /** 36 名考生、单间 30 座 -> 用 2 间教室，导出 1 张明细 + 1 张总览 + 2 张座次图。 */
    @Test
    public void exportsDetailOverviewAndOneGridPerRoom() throws IOException {
        StudentRoster roster = StudentRoster.generate(3, 12, "高三", 2024, 11L);
        List<ExamRoom> rooms = new ExamRoomFactory(1, 1, 3, 5, 6).createAll();
        List<ExamSeatingService.RoomResult> results =
                new ExamSeatingService(Duration.ofSeconds(1)).arrange(roster, rooms);
        assertEquals(2, results.size());

        Path target = folder.newFile("seats.xlsx").toPath();
        new ExamSeatExcelExporter().export(results, target);
        assertTrue(Files.size(target) > 0);

        try (Workbook workbook = new XSSFWorkbook(Files.newInputStream(target))) {
            assertEquals(2 + results.size(), workbook.getNumberOfSheets());

            Sheet detail = workbook.getSheet("考生座位明细");
            Row header = detail.getRow(0);
            assertEquals("学号", header.getCell(1).getStringCellValue());
            assertEquals("姓名", header.getCell(2).getStringCellValue());
            assertEquals("楼栋", header.getCell(4).getStringCellValue());
            assertEquals("楼层", header.getCell(5).getStringCellValue());
            assertEquals("教室编号", header.getCell(6).getStringCellValue());
            assertEquals("第几排", header.getCell(7).getStringCellValue());
            assertEquals("该排座位号", header.getCell(8).getStringCellValue());
            assertEquals("教室座位总序号", header.getCell(9).getStringCellValue());
            assertEquals("监考教师编号", header.getCell(11).getStringCellValue());
            assertEquals(36, detail.getLastRowNum());

            Set<String> exportedIds = new HashSet<>();
            for (int rowNo = 1; rowNo <= detail.getLastRowNum(); rowNo++) {
                Row row = detail.getRow(rowNo);
                exportedIds.add(row.getCell(1).getStringCellValue());
                assertEquals("A栋", row.getCell(4).getStringCellValue());
                int columns = 6;
                int expectedTotal = (int) (row.getCell(7).getNumericCellValue() - 1) * columns
                        + (int) row.getCell(8).getNumericCellValue();
                assertEquals("教室座位总序号必须等于 (排-1)×每排座数+该排座号", expectedTotal,
                        (int) row.getCell(9).getNumericCellValue());
                assertEquals("第" + (int) row.getCell(7).getNumericCellValue() + "排第"
                        + (int) row.getCell(8).getNumericCellValue() + "号",
                        row.getCell(10).getStringCellValue());
            }
            assertEquals(36, exportedIds.size());
            roster.allStudents().forEach(student -> assertTrue(exportedIds.contains(student.getStudentId())));

            Sheet overview = workbook.getSheet("考场总览");
            assertEquals(2, overview.getLastRowNum());
            assertEquals("通过", overview.getRow(1).getCell(10).getStringCellValue());

            Sheet grid = workbook.getSheet("座次图-A101");
            assertTrue(grid.getRow(0).getCell(0).getStringCellValue().contains("监考"));
            int filled = 0;
            for (int rowNo = 2; rowNo <= grid.getLastRowNum(); rowNo++) {
                for (int column = 1; column <= 6; column++) {
                    String value = grid.getRow(rowNo).getCell(column).getStringCellValue();
                    if (!"空座".equals(value)) {
                        assertEquals(3, value.split("\n").length);
                        filled++;
                    }
                }
            }
            assertEquals(results.get(0).placements().size(), filled);
        }
    }

    /** 明细表按楼栋 -> 楼层 -> 教室编号 -> 座位序号排序。 */
    @Test
    public void exportedRowsAreOrderedByBuildingFloorRoomAndSeatIndex() throws IOException {
        StudentRoster roster = StudentRoster.generate(3, 14, "高三", 2024, 13L);
        List<ExamRoom> rooms = new ExamRoomFactory(2, 1, 1, 5, 5).createAll();
        List<ExamSeatingService.RoomResult> results =
                new ExamSeatingService(Duration.ofSeconds(1)).arrange(roster, rooms);

        List<String> expected = new ArrayList<>();
        results.sort(new java.util.Comparator<ExamSeatingService.RoomResult>() {
            @Override
            public int compare(ExamSeatingService.RoomResult left, ExamSeatingService.RoomResult right) {
                return left.room().getRoomNumber().compareTo(right.room().getRoomNumber());
            }
        });
        results.forEach(result -> expected.add(result.room().getBuilding() + result.room().getFloor()
                + "-" + result.room().getRoomNumber()));

        Path target = folder.newFile("ordered.xlsx").toPath();
        new ExamSeatExcelExporter().export(results, target);

        try (Workbook workbook = new XSSFWorkbook(Files.newInputStream(target))) {
            Sheet detail = workbook.getSheet("考生座位明细");
            List<String> actual = new ArrayList<>();
            for (int rowNo = 1; rowNo <= detail.getLastRowNum(); rowNo++) {
                Row row = detail.getRow(rowNo);
                String key = row.getCell(4).getStringCellValue() + (int) row.getCell(5).getNumericCellValue()
                        + "-" + row.getCell(6).getStringCellValue();
                if (actual.isEmpty() || !actual.get(actual.size() - 1).equals(key)) {
                    actual.add(key);
                }
            }
            assertEquals(expected, actual);
            assertEquals(2, expected.size());
        }
    }
}
