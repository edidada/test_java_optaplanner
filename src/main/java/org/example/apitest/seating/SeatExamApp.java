package org.example.apitest.seating;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 考试排座位命令行入口：txt 名单（按班级）→ OptaPlanner 排座 → 导出 Excel。
 *
 * <pre>
 * 用法：SeatExamApp [选项]
 *   --input &lt;txt&gt;          考生名单，默认 data/students.txt
 *   --generate             随机生成一份名单写入 --input（已存在则覆盖）
 *   --classes &lt;n&gt;          随机生成的班级数，默认 6
 *   --per-class &lt;n&gt;        随机生成的每班人数，默认 45
 *   --grade &lt;名称&gt;         随机生成的年级前缀，默认 高三
 *   --seed &lt;n&gt;             随机种子，默认 20261017（同种子结果可复现）
 *   --buildings &lt;n&gt;        考点楼栋数，默认 2
 *   --floors &lt;n&gt;           每栋楼层数，默认 3
 *   --rooms-per-floor &lt;n&gt;  每层教室数，默认 3
 *   --rows &lt;n&gt;             每间教室排数，默认 8
 *   --cols &lt;n&gt;             每排座位数，默认 5
 *   --seconds &lt;n&gt;          每个考场求解秒数，默认 2
 *   --out &lt;xlsx&gt;           Excel 输出路径，默认 target/exam-seat-arrangement.xlsx
 * </pre>
 *
 * <p>例：{@code SeatExamApp --generate --classes 6 --per-class 45 --seconds 2}</p>
 */
public class SeatExamApp {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeatExamApp.class);

    public static void main(String[] args) throws IOException {
        Options options = Options.parse(args);

        Path input = options.input;
        if (options.generate) {
            StudentRoster generated = StudentRoster.generate(options.classes, options.perClass, options.grade,
                    options.enrollYear, options.seed);
            generated.writeToFile(input);
            LOGGER.info("已随机生成名单：{} 个班级 × 每班 {} 人 = {} 名考生 -> {}",
                    options.classes, options.perClass, generated.getStudentCount(), input.toAbsolutePath());
        } else if (!Files.isRegularFile(input)) {
            throw new IOException("名单文件不存在：" + input.toAbsolutePath() + "（可加 --generate 随机生成）");
        }

        StudentRoster roster = StudentRoster.readFromFile(input);
        ExamRoomFactory factory = new ExamRoomFactory(options.buildings, options.floors, options.roomsPerFloor,
                options.rows, options.cols);
        List<ExamRoom> rooms = factory.createAll();

        LOGGER.info("名单：{} 个班级、{} 名考生；考点：{} 栋 × 每栋 {} 层 × 每层 {} 间，每间 {}排×{}座（单间容量 {}，总容量 {}）",
                roster.getClasses().size(), roster.getStudentCount(), options.buildings, options.floors,
                options.roomsPerFloor, options.rows, options.cols, factory.roomCapacity(),
                factory.totalCapacity());

        ExamSeatingService service = new ExamSeatingService(Duration.ofSeconds(options.seconds));
        List<ExamSeatingService.RoomResult> results = service.arrange(roster, rooms);

        int problems = 0;
        for (ExamSeatingService.RoomResult result : results) {
            problems += result.problems().size();
            LOGGER.info("考场 {} {}：{} 人 / {} 座，得分 {}，{}", result.room().getBuilding(),
                    result.room().getRoomNumber(), result.students().size(), result.room().getCapacity(),
                    result.solution().getScore(),
                    result.problems().isEmpty() ? "校验通过" : "校验问题 " + result.problems());
        }
        if (problems > 0) {
            LOGGER.warn("共 {} 处校验问题，Excel 的“考场总览”sheet 已标红", problems);
        } else {
            LOGGER.info("全部考场校验通过：无空考生、无重复占座、同班考生均未相邻");
        }

        LOGGER.info("\n" + ExamSeatPlanner.renderPreview(results.get(0)));

        Path target = new ExamSeatExcelExporter().export(results, options.out);
        LOGGER.info("Excel 已导出：{}（1 张明细表 + 1 张考场总览 + {} 张座次图）",
                target.toAbsolutePath(), results.size());
    }

    /** 解析命令行选项。 */
    static final class Options {

        Path input = Path.of("data", "students.txt");
        Path out = Path.of("target", "exam-seat-arrangement.xlsx");
        boolean generate;
        int classes = 6;
        int perClass = 45;
        String grade = "高三";
        int enrollYear = 2024;
        long seed = 20261017L;
        int buildings = 2;
        int floors = 3;
        int roomsPerFloor = 3;
        int rows = 8;
        int cols = 5;
        int seconds = 2;

        static Options parse(String[] args) {
            Options options = new Options();
            for (int i = 0; i < args.length; i++) {
                String flag = args[i];
                switch (flag) {
                    case "--generate" -> options.generate = true;
                    case "--input" -> options.input = Path.of(next(args, ++i, flag));
                    case "--out" -> options.out = Path.of(next(args, ++i, flag));
                    case "--grade" -> options.grade = next(args, ++i, flag);
                    case "--classes" -> options.classes = Integer.parseInt(next(args, ++i, flag));
                    case "--per-class" -> options.perClass = Integer.parseInt(next(args, ++i, flag));
                    case "--seed" -> options.seed = Long.parseLong(next(args, ++i, flag));
                    case "--buildings" -> options.buildings = Integer.parseInt(next(args, ++i, flag));
                    case "--floors" -> options.floors = Integer.parseInt(next(args, ++i, flag));
                    case "--rooms-per-floor" -> options.roomsPerFloor = Integer.parseInt(next(args, ++i, flag));
                    case "--rows" -> options.rows = Integer.parseInt(next(args, ++i, flag));
                    case "--cols" -> options.cols = Integer.parseInt(next(args, ++i, flag));
                    case "--seconds" -> options.seconds = Integer.parseInt(next(args, ++i, flag));
                    default -> throw new IllegalArgumentException("未知选项：" + flag);
                }
            }
            return options;
        }

        private static String next(String[] args, int index, String flag) {
            if (index >= args.length) {
                throw new IllegalArgumentException(flag + " 缺少参数值");
            }
            return args[index];
        }
    }
}
