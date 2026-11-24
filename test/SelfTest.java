import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Simple console tests for the rule engine (no GUI, no JUnit needed).
 * Run:  java -cp out SelfTest
 */
public class SelfTest {
    private static int passed = 0;
    private static int failed = 0;

    private static void check(String name, boolean condition) {
        if (condition) { passed++; System.out.println("  PASS  " + name); }
        else { failed++; System.out.println("  FAIL  " + name); }
    }

    private static DailyEntry entry(String bed, String wake, int water, int exercise, int screen, int stress, Goal goal) {
        return new DailyEntry(LocalDate.of(2026, 1, 1), LocalTime.parse(wake), LocalTime.parse(bed),
                water, exercise, screen, stress, goal);
    }

    private static String best(DailyEntry e) {
        return new RoutineAdvisor().analyze(e).getBestMatch().getRoutine().getName();
    }

    public static void main(String[] args) throws Exception {
        RoutineAdvisor advisor = new RoutineAdvisor();

        System.out.println("Sleep calculation");
        check("23:30 -> 07:00 = 7.5 h", Math.abs(entry("23:30", "07:00", 8, 40, 1, 2, Goal.GENERAL).getSleepHours() - 7.5) < 1e-9);
        check("01:00 -> 06:30 = 5.5 h (crosses midnight)", Math.abs(entry("01:00", "06:30", 8, 40, 1, 2, Goal.GENERAL).getSleepHours() - 5.5) < 1e-9);
        check("22:00 -> 06:00 = 8 h", Math.abs(entry("22:00", "06:00", 8, 40, 1, 2, Goal.GENERAL).getSleepHours() - 8.0) < 1e-9);

        System.out.println("Classification");
        AnalysisResult bad = advisor.analyze(entry("01:00", "06:30", 3, 10, 7, 5, Goal.GENERAL));
        check("5.5 h sleep is LOW", bad.getStatuses().get(Metric.SLEEP_DURATION) == Status.LOW);
        check("bed 01:00 is late (HIGH)", bad.getStatuses().get(Metric.BEDTIME) == Status.HIGH);
        check("3 glasses is LOW", bad.getStatuses().get(Metric.HYDRATION) == Status.LOW);
        check("10 min exercise is LOW", bad.getStatuses().get(Metric.ACTIVITY) == Status.LOW);
        check("7 h screen is HIGH", bad.getStatuses().get(Metric.SCREEN) == Status.HIGH);
        check("stress 5 is HIGH", bad.getStatuses().get(Metric.STRESS) == Status.HIGH);
        AnalysisResult good = advisor.analyze(entry("22:30", "06:30", 9, 45, 2, 2, Goal.GENERAL));
        check("healthy day: every metric OK or LOW-stress", good.getStatuses().get(Metric.SLEEP_DURATION) == Status.OK
                && good.getStatuses().get(Metric.BEDTIME) == Status.OK && good.getStatuses().get(Metric.HYDRATION) == Status.OK
                && good.getStatuses().get(Metric.ACTIVITY) == Status.OK && good.getStatuses().get(Metric.SCREEN) == Status.OK);
        check("bed 23:30 is not late", advisor.analyze(entry("23:30", "07:30", 8, 40, 1, 2, Goal.GENERAL)).getStatuses().get(Metric.BEDTIME) == Status.OK);
        check("bed 00:00 is not late (boundary)", advisor.analyze(entry("00:00", "08:00", 8, 40, 1, 2, Goal.GENERAL)).getStatuses().get(Metric.BEDTIME) == Status.OK);
        check("bed 00:05 is late", advisor.analyze(entry("00:05", "08:00", 8, 40, 1, 2, Goal.GENERAL)).getStatuses().get(Metric.BEDTIME) == Status.HIGH);
        check("10 h sleep is HIGH", advisor.analyze(entry("22:00", "08:00", 8, 40, 1, 2, Goal.GENERAL)).getStatuses().get(Metric.SLEEP_DURATION) == Status.HIGH);

        System.out.println("Scores");
        check("healthy day scores >= 95", good.getOverallScore() >= 95);
        check("bad day scores < 40", bad.getOverallScore() < 40);
        for (Metric m : Metric.values()) {
            int s = bad.getScores().get(m);
            check("score for " + m + " within 0..100 (" + s + ")", s >= 0 && s <= 100);
        }

        System.out.println("Routine matching");
        check("all healthy -> Balanced Maintenance", best(entry("22:30", "06:30", 9, 45, 2, 2, Goal.GENERAL)).startsWith("Balanced"));
        check("all healthy + FOCUS goal still Balanced (goal alone never qualifies)", best(entry("22:30", "06:30", 9, 45, 2, 2, Goal.FOCUS)).startsWith("Balanced"));
        check("only low water -> Hydration Kickstart", best(entry("22:30", "06:30", 3, 45, 2, 2, Goal.GENERAL)).startsWith("Hydration"));
        check("only low exercise -> Move More", best(entry("22:30", "06:30", 9, 5, 2, 2, Goal.GENERAL)).startsWith("Move More"));
        check("short sleep + late bed -> Sleep Reset", best(entry("01:30", "06:30", 9, 45, 2, 2, Goal.GENERAL)).startsWith("Sleep Reset"));
        check("high screen + high stress -> Digital Detox", best(entry("22:30", "06:30", 9, 45, 6, 5, Goal.GENERAL)).startsWith("Digital Detox"));
        check("worst day, GENERAL goal -> Sleep Reset (sleep has priority)", best(entry("01:00", "06:30", 3, 10, 7, 5, Goal.GENERAL)).startsWith("Sleep Reset"));
        check("worst day, ENERGY goal -> Energy Boost (goal bonus tips it)", best(entry("01:00", "06:30", 3, 10, 7, 5, Goal.ENERGY)).startsWith("Energy Boost"));
        check("low exercise + WEIGHT goal -> Weight Balance", best(entry("22:30", "06:30", 9, 5, 2, 2, Goal.WEIGHT)).startsWith("Weight Balance"));
        check("short sleep + FOCUS goal, healthy otherwise -> Student Focus or Sleep Reset",
                best(entry("00:30", "06:00", 9, 45, 2, 2, Goal.FOCUS)).matches("Student Focus.*|Sleep Reset.*"));
        List<RoutineMatch> ranking = bad.getRanking();
        check("ranking contains every preset once", ranking.size() == RoutineLibrary.getAll().size());
        boolean sorted = true;
        for (int i = 1; i < ranking.size(); i++) if (ranking.get(i - 1).getPoints() < ranking.get(i).getPoints()) sorted = false;
        check("ranking is sorted by points (highest first)", sorted);

        System.out.println("Timetable");
        for (Routine r : RoutineLibrary.getAll()) {
            List<Routine.TimedStep> t = r.buildTimetable(LocalTime.of(6, 30));
            check(r.getName() + ": first step is wake-up time 06:30", t.get(0).getTime().equals(LocalTime.of(6, 30)));
            boolean ok = true;
            int last = -1;
            for (Routine.TimedStep s : t) {
                int m = Math.floorMod(s.getTime().getHour() * 60 + s.getTime().getMinute() - (6 * 60 + 30), 1440);
                if (m < last) ok = false;
                last = m;
            }
            check(r.getName() + ": steps are in order through the day", ok);
        }
        Routine sleepReset = RoutineLibrary.getAll().get(0);
        List<Routine.TimedStep> tt = sleepReset.buildTimetable(LocalTime.of(7, 0));
        check("Sleep Reset with wake 07:00 ends with lights out at 23:00", tt.get(tt.size() - 1).getTime().equals(LocalTime.of(23, 0)));
        check("late wake 10:00 (bed 02:00) still sorts lights-out last", sleepReset.buildTimetable(LocalTime.of(10, 0))
                .get(sleepReset.buildTimetable(LocalTime.of(10, 0)).size() - 1).getTime().equals(LocalTime.of(2, 0)));

        System.out.println("Validation");
        check("negative water rejected", throwsIAE(-1, 30, 2, 3));
        check("stress 0 rejected", throwsIAE(5, 30, 2, 0));
        check("stress 6 rejected", throwsIAE(5, 30, 2, 6));
        check("screen 25 rejected", throwsIAE(5, 30, 25, 3));
        boolean impossible = false;
        try { entry("07:00", "07:00", 5, 30, 2, 3, Goal.GENERAL); } catch (IllegalArgumentException ex) { impossible = true; }
        check("bed == wake (24 h sleep) rejected", impossible);

        System.out.println("Storage");
        File f = File.createTempFile("history", ".csv");
        f.delete();
        EntryStore store = new EntryStore(f);
        check("new store is empty", store.getAll().isEmpty());
        DailyEntry a = new DailyEntry(LocalDate.of(2026, 3, 1), LocalTime.of(7, 0), LocalTime.of(23, 0), 6, 20, 3, 3, Goal.FOCUS);
        DailyEntry b = new DailyEntry(LocalDate.of(2026, 3, 2), LocalTime.of(6, 45), LocalTime.of(0, 15), 4, 0, 5, 4, Goal.ENERGY);
        store.save(a); store.save(b);
        EntryStore reloaded = new EntryStore(f);
        check("two entries survive reload", reloaded.getAll().size() == 2);
        check("newest first", reloaded.getLatest().getDate().equals(LocalDate.of(2026, 3, 2)));
        check("values round-trip", reloaded.getLatest().toCsv().equals(b.toCsv()));
        reloaded.save(new DailyEntry(LocalDate.of(2026, 3, 2), LocalTime.of(7, 0), LocalTime.of(23, 0), 8, 30, 2, 2, Goal.GENERAL));
        check("same date replaces (still 2 entries)", reloaded.getAll().size() == 2);
        reloaded.delete(LocalDate.of(2026, 3, 1));
        check("delete works", new EntryStore(f).getAll().size() == 1);
        java.nio.file.Files.write(f.toPath(), "date,wake,bed,water,exercise,screen,stress,goal\ngarbage line\n2026-03-05,07:00,23:00,6,20,3,3,FOCUS\n".getBytes("UTF-8"));
        check("damaged line skipped, good line kept", new EntryStore(f).getAll().size() == 1);
        f.delete();

        System.out.println();
        System.out.println(passed + " passed, " + failed + " failed");
        System.exit(failed == 0 ? 0 : 1);
    }

    private static boolean throwsIAE(int water, int ex, int screen, int stress) {
        try {
            new DailyEntry(LocalDate.now(), LocalTime.of(7, 0), LocalTime.of(23, 0), water, ex, screen, stress, Goal.GENERAL);
            return false;
        } catch (IllegalArgumentException e) { return true; }
    }
}
