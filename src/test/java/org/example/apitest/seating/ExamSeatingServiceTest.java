package org.example.apitest.seating;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

public class ExamSeatingServiceTest {

    @Test
    public void everyStudentGetsAnUniqueSeatAndRoomsStayBelowCapacity() {
        StudentRoster roster = StudentRoster.generate(3, 12, "高三", 2024, 1L);
        List<ExamRoom> rooms = new ExamRoomFactory(1, 2, 2, 4, 5).createAll();

        List<ExamSeatingService.RoomResult> results =
                new ExamSeatingService(Duration.ofSeconds(1)).arrange(roster, rooms);

        List<ExamSeatingService.Placement> placements = new ArrayList<>();
        results.forEach(result -> placements.addAll(result.placements()));
        assertEquals(36, placements.size());

        Set<String> seatKeys = new HashSet<>();
        for (ExamSeatingService.Placement placement : placements) {
            ExamRoom room = placement.room();
            Seat seat = placement.seat();
            assertTrue(seatKeys.add(room.getRoomNumber() + "#" + seat.getIndex()));
            assertTrue(seat.getRow() >= 1 && seat.getRow() <= room.getRows());
            assertTrue(seat.getCol() >= 1 && seat.getCol() <= room.getColumns());
            assertTrue(seat.getIndex() + 1 <= room.getCapacity());
            assertFalse(placement.student().getStudentId().isEmpty());
            assertFalse(placement.student().getName().isEmpty());
        }

        for (ExamSeatingService.RoomResult result : results) {
            assertTrue(result.students().size() <= result.room().getCapacity());
            assertEquals("考场 " + result.room() + " 硬约束未满足：" + result.problems(), 0,
                    result.solution().getScore().getHardScore());
            assertTrue(result.problems().toString(), result.problems().isEmpty());
        }
    }

    @Test
    public void studentsAreSpreadAcrossClassesAndBuildingsHaveFloorsAndRoomCodes() {
        StudentRoster roster = StudentRoster.generate(4, 20, "高三", 2024, 3L);
        List<ExamRoom> rooms = new ExamRoomFactory(2, 2, 2, 5, 4).createAll();

        List<ExamSeatingService.RoomResult> results =
                new ExamSeatingService(Duration.ofSeconds(1)).arrange(roster, rooms);

        // 8 间教室可用，80 人 -> 至少 2 间，且不会把同一个班整体塞进一间
        assertTrue(results.size() >= 2);
        assertEquals('A', results.get(0).room().getBuilding().charAt(0));
        Set<String> roomNumbers = new HashSet<>();
        results.forEach(result -> roomNumbers.add(result.room().getRoomNumber()));
        assertEquals(results.size(), roomNumbers.size());

        for (ExamSeatingService.RoomResult result : results) {
            Set<String> classesInRoom = new HashSet<>();
            result.students().forEach(student -> classesInRoom.add(student.getClassName()));
            assertTrue("考场 " + result.room() + " 只有一个班", classesInRoom.size() > 1);
        }
    }

    @Test
    public void failsFastWhenTheWholeSiteIsTooSmall() {
        StudentRoster roster = StudentRoster.generate(2, 10, "高三", 2024, 5L);
        List<ExamRoom> rooms = new ExamRoomFactory(1, 1, 1, 2, 2).createAll();

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> new ExamSeatingService(Duration.ofSeconds(1)).arrange(roster, rooms));
        assertTrue(error.getMessage(), error.getMessage().contains("请增加考场"));
    }

    @Test
    public void roomFactoryNumbersRoomsByBuildingFloorAndIndex() {
        List<ExamRoom> rooms = new ExamRoomFactory(2, 2, 2, 6, 8).createAll();

        assertEquals(8, rooms.size());
        assertEquals("A栋", rooms.get(0).getBuilding());
        assertEquals(1, rooms.get(0).getFloor());
        assertEquals("A101", rooms.get(0).getRoomNumber());
        assertEquals("A202", rooms.get(3).getRoomNumber());
        assertEquals("B101", rooms.get(4).getRoomNumber());
        assertEquals(48, rooms.get(0).getCapacity());
        assertEquals(List.of("T1001", "T1002"), rooms.get(0).getInvigilators());
        assertEquals("T1015、T1016", rooms.get(7).getInvigilatorText());
    }
}
