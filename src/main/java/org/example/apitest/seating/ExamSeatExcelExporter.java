package org.example.apitest.seating;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * 把排座结果导出成 Excel（.xlsx），共三类 sheet：
 * <ol>
 *   <li><b>考生座位明细</b>：学号、姓名、班级、楼栋、楼层、教室编号、第几排、该排第几号、
 *       教室座位总序号、监考教师编号；</li>
 *   <li><b>考场总览</b>：每间考场的容量、人数、得分与监考安排；</li>
 *   <li><b>座次图-教室号</b>：每间考场一张平面图，单元格内是“姓名 + 学号”。</li>
 * </ol>
 */
public class ExamSeatExcelExporter {

    private static final String[] DETAIL_HEADERS = {
            "序号", "学号", "姓名", "班级", "楼栋", "楼层", "教室编号",
            "第几排", "该排座位号", "教室座位总序号", "座位位置", "监考教师编号"
    };

    private static final String[] ROOM_HEADERS = {
            "楼栋", "楼层", "教室编号", "排数", "每排座数", "座位容量", "考生数",
            "来源班级数", "硬约束得分", "软约束得分", "校验结果", "监考教师编号"
    };

    public Path export(List<ExamSeatingService.RoomResult> roomResults, Path target) throws IOException {
        List<ExamSeatingService.Placement> all = new ArrayList<>();
        roomResults.forEach(result -> all.addAll(result.placements()));
        all.sort(Comparator.comparing((ExamSeatingService.Placement placement) -> placement.room().getBuilding())
                .thenComparingInt(placement -> placement.room().getFloor())
                .thenComparing(placement -> placement.room().getRoomNumber())
                .thenComparingInt(placement -> placement.seat().getIndex()));

        try (Workbook workbook = new XSSFWorkbook()) {
            Styles styles = new Styles(workbook);
            writeDetailSheet(workbook.createSheet("考生座位明细"), all, styles);
            writeRoomSheet(workbook.createSheet("考场总览"), roomResults, styles);
            for (ExamSeatingService.RoomResult result : roomResults) {
                writeGridSheet(workbook, result, styles);
            }
            if (Files.exists(target)) {
                Files.delete(target);
            }
            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }
            try (OutputStream out = Files.newOutputStream(target)) {
                workbook.write(out);
            }
        }
        return target;
    }

    private void writeDetailSheet(Sheet sheet, List<ExamSeatingService.Placement> placements, Styles styles) {
        writeHeader(sheet, DETAIL_HEADERS, styles);
        int rowNo = 1;
        int sequence = 1;
        for (ExamSeatingService.Placement placement : placements) {
            ExamRoom room = placement.room();
            Seat seat = placement.seat();
            Row row = sheet.createRow(rowNo++);
            writeNumber(row.createCell(0), sequence++, styles.plain);
            writeText(row.createCell(1), placement.student().getStudentId(), styles.plain);
            writeText(row.createCell(2), placement.student().getName(), styles.plain);
            writeText(row.createCell(3), placement.student().getClassName(), styles.plain);
            writeText(row.createCell(4), room.getBuilding(), styles.plain);
            writeNumber(row.createCell(5), room.getFloor(), styles.plain);
            writeText(row.createCell(6), room.getRoomNumber(), styles.plain);
            writeNumber(row.createCell(7), seat.getRow(), styles.plain);
            writeNumber(row.createCell(8), seat.getCol(), styles.plain);
            writeNumber(row.createCell(9), seat.getIndex() + 1, styles.plain);
            writeText(row.createCell(10), "第" + seat.getRow() + "排第" + seat.getCol() + "号", styles.plain);
            writeText(row.createCell(11), room.getInvigilatorText(), styles.plain);
        }
        sheet.createFreezePane(0, 1);
        fitColumns(sheet, DETAIL_HEADERS, 4);
    }

    private void writeRoomSheet(Sheet sheet, List<ExamSeatingService.RoomResult> results, Styles styles) {
        writeHeader(sheet, ROOM_HEADERS, styles);
        int rowNo = 1;
        for (ExamSeatingService.RoomResult result : results) {
            ExamRoom room = result.room();
            Set<String> classNames = new LinkedHashSet<>();
            result.students().forEach(student -> classNames.add(student.getClassName()));

            Row row = sheet.createRow(rowNo++);
            writeText(row.createCell(0), room.getBuilding(), styles.plain);
            writeNumber(row.createCell(1), room.getFloor(), styles.plain);
            writeText(row.createCell(2), room.getRoomNumber(), styles.plain);
            writeNumber(row.createCell(3), room.getRows(), styles.plain);
            writeNumber(row.createCell(4), room.getColumns(), styles.plain);
            writeNumber(row.createCell(5), room.getCapacity(), styles.plain);
            writeNumber(row.createCell(6), result.students().size(), styles.plain);
            writeNumber(row.createCell(7), classNames.size(), styles.plain);
            writeText(row.createCell(8), String.valueOf(result.solution().getScore().getHardScore()), styles.plain);
            writeText(row.createCell(9), String.valueOf(result.solution().getScore().getSoftScore()), styles.plain);
            writeText(row.createCell(10), result.problems().isEmpty() ? "通过" : String.join("；", result.problems()),
                    result.problems().isEmpty() ? styles.plain : styles.bad);
            writeText(row.createCell(11), room.getInvigilatorText(), styles.plain);
        }
        sheet.createFreezePane(0, 1);
        fitColumns(sheet, ROOM_HEADERS, 4);
    }

    private void writeGridSheet(Workbook workbook, ExamSeatingService.RoomResult result, Styles styles) {
        ExamRoom room = result.room();
        Sheet sheet = workbook.createSheet(sheetName("座次图-", room));

        Row title = sheet.createRow(0);
        title.createCell(0).setCellValue(room.getBuilding() + " " + room.getRoomNumber()
                + "（" + room.getFloorLabel() + "，" + room.getRows() + "排×" + room.getColumns()
                + "座，考生 " + result.students().size() + " 人，监考 " + room.getInvigilatorText() + "）");

        java.util.Map<Seat, Student> bySeat = new java.util.HashMap<>();
        for (ExamSeatingService.Placement placement : result.placements()) {
            bySeat.put(placement.seat(), placement.student());
        }

        Row corner = sheet.createRow(1);
        corner.createCell(0).setCellValue("排\\座");
        for (int column = 1; column <= room.getColumns(); column++) {
            writeNumber(corner.createCell(column), column, styles.header);
        }

        for (int rowNumber = 1; rowNumber <= room.getRows(); rowNumber++) {
            Row sheetRow = sheet.createRow(rowNumber + 1);
            sheetRow.setHeightInPoints(32);
            writeNumber(sheetRow.createCell(0), rowNumber, styles.header);
            for (int column = 1; column <= room.getColumns(); column++) {
                Seat seat = findSeat(room, rowNumber, column);
                Student student = bySeat.get(seat);
                Cell cell = sheetRow.createCell(column);
                cell.setCellStyle(student == null ? styles.emptySeat : styles.grid);
                if (student == null) {
                    cell.setCellValue("空座");
                } else {
                    cell.setCellValue(student.getName() + "\n" + student.getStudentId()
                            + "\n第" + rowNumber + "排第" + column + "号（总" + (seat.getIndex() + 1) + "号）");
                }
            }
        }

        sheet.setColumnWidth(0, 8 * 256);
        for (int column = 1; column <= room.getColumns(); column++) {
            sheet.setColumnWidth(column, 22 * 256);
        }
        sheet.createFreezePane(1, 2);
    }

    private static Seat findSeat(ExamRoom room, int rowNumber, int column) {
        return room.getSeats().stream()
                .filter(seat -> seat.getRow() == rowNumber && seat.getCol() == column)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("教室 " + room + " 缺少座位 " + rowNumber + "-" + column));
    }

    private static String sheetName(String prefix, ExamRoom room) {
        String name = prefix + room.getRoomNumber();
        return name.length() <= 31 ? name : name.substring(0, 31);
    }

    private void writeHeader(Sheet sheet, String[] headers, Styles styles) {
        Row row = sheet.createRow(0);
        for (int index = 0; index < headers.length; index++) {
            Cell cell = row.createCell(index);
            cell.setCellValue(headers[index]);
            cell.setCellStyle(styles.header);
        }
    }

    private void writeText(Cell cell, String value, CellStyle style) {
        cell.setCellStyle(style);
        cell.setCellValue(value == null ? "" : value);
    }

    private void writeNumber(Cell cell, double value, CellStyle style) {
        cell.setCellStyle(style);
        cell.setCellValue(value);
    }

    /** 中文列宽粗略按 2 个字符计算。 */
    private void fitColumns(Sheet sheet, String[] headers, int extra) {
        for (int index = 0; index < headers.length; index++) {
            int width = 0;
            for (char ch : headers[index].toCharArray()) {
                width += ch > 127 ? 2 : 1;
            }
            sheet.setColumnWidth(index, (width + extra) * 256);
        }
    }

    /** 共享的单元格样式。 */
    private static final class Styles {

        private final CellStyle header;
        private final CellStyle plain;
        private final CellStyle grid;
        private final CellStyle emptySeat;
        private final CellStyle bad;

        private Styles(Workbook workbook) {
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);

            header = workbook.createCellStyle();
            header.setFont(boldFont);
            header.setAlignment(HorizontalAlignment.CENTER);
            header.setVerticalAlignment(VerticalAlignment.CENTER);
            header.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.GREY_25_PERCENT.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            addBorder(header);

            plain = workbook.createCellStyle();
            addBorder(plain);

            bad = workbook.createCellStyle();
            bad.cloneStyleFrom(plain);
            Font badFont = workbook.createFont();
            badFont.setColor(org.apache.poi.ss.usermodel.IndexedColors.DARK_RED.getIndex());
            badFont.setBold(true);
            bad.setFont(badFont);

            grid = workbook.createCellStyle();
            grid.cloneStyleFrom(header);
            grid.setWrapText(true);
            grid.setAlignment(HorizontalAlignment.CENTER);
            grid.setVerticalAlignment(VerticalAlignment.CENTER);

            emptySeat = workbook.createCellStyle();
            emptySeat.cloneStyleFrom(grid);
            emptySeat.setFillForegroundColor(
                    org.apache.poi.ss.usermodel.IndexedColors.LIGHT_TURQUOISE.getIndex());
        }

        private static void addBorder(CellStyle style) {
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
        }
    }
}
