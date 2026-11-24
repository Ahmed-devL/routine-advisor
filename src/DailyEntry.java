import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * One day's input from the user (the "check-in").
 * The object is immutable: once created, nothing can change it, which makes it
 * safe to pass around between the GUI, the rule engine and the storage class.
 * The constructor validates the data and throws IllegalArgumentException if
 * something is impossible (e.g. negative water glasses).
 */
public class DailyEntry {
    private final LocalDate date;
    private final LocalTime wakeTime;
    private final LocalTime bedTime;      // the time the user went to bed (previous night)
    private final int waterGlasses;
    private final int exerciseMinutes;
    private final int screenHours;        // leisure screen time (not work/study)
    private final int stressLevel;        // 1 = very calm ... 5 = very stressed
    private final Goal goal;

    public DailyEntry(LocalDate date, LocalTime wakeTime, LocalTime bedTime,
                      int waterGlasses, int exerciseMinutes, int screenHours,
                      int stressLevel, Goal goal) {
        if (date == null || wakeTime == null || bedTime == null || goal == null) {
            throw new IllegalArgumentException("Date, times and goal are required.");
        }
        if (waterGlasses < 0 || waterGlasses > 30) {
            throw new IllegalArgumentException("Water glasses must be between 0 and 30.");
        }
        if (exerciseMinutes < 0 || exerciseMinutes > 600) {
            throw new IllegalArgumentException("Exercise minutes must be between 0 and 600.");
        }
        if (screenHours < 0 || screenHours > 24) {
            throw new IllegalArgumentException("Screen time must be between 0 and 24 hours.");
        }
        if (stressLevel < 1 || stressLevel > 5) {
            throw new IllegalArgumentException("Stress level must be between 1 and 5.");
        }
        this.date = date;
        this.wakeTime = wakeTime;
        this.bedTime = bedTime;
        this.waterGlasses = waterGlasses;
        this.exerciseMinutes = exerciseMinutes;
        this.screenHours = screenHours;
        this.stressLevel = stressLevel;
        this.goal = goal;

        double sleep = calculateSleepHours(bedTime, wakeTime);
        if (sleep < 1 || sleep > 16) {
            throw new IllegalArgumentException(String.format(
                    "Bedtime %s and wake-up %s give %.1f hours of sleep, which does not look right. "
                            + "Please check both times.", bedTime, wakeTime, sleep));
        }
    }

    /**
     * Hours slept between going to bed and waking up.
     * If the wake time is "earlier" on the clock than the bedtime (e.g. bed 23:30,
     * wake 06:30) the sleep crossed midnight, so we add 24 hours.
     */
    public static double calculateSleepHours(LocalTime bed, LocalTime wake) {
        long minutes = Duration.between(bed, wake).toMinutes();
        if (minutes <= 0) {
            minutes += 24 * 60;
        }
        return minutes / 60.0;
    }

    public double getSleepHours() { return calculateSleepHours(bedTime, wakeTime); }

    public LocalDate getDate() { return date; }
    public LocalTime getWakeTime() { return wakeTime; }
    public LocalTime getBedTime() { return bedTime; }
    public int getWaterGlasses() { return waterGlasses; }
    public int getExerciseMinutes() { return exerciseMinutes; }
    public int getScreenHours() { return screenHours; }
    public int getStressLevel() { return stressLevel; }
    public Goal getGoal() { return goal; }

    /** One line of the history.csv file. */
    public String toCsv() {
        return date + "," + wakeTime + "," + bedTime + "," + waterGlasses + ","
                + exerciseMinutes + "," + screenHours + "," + stressLevel + "," + goal.name();
    }

    /** Rebuilds an entry from a CSV line. Throws if the line is damaged. */
    public static DailyEntry fromCsv(String line) {
        String[] p = line.trim().split(",");
        if (p.length != 8) {
            throw new IllegalArgumentException("Expected 8 fields but found " + p.length);
        }
        return new DailyEntry(LocalDate.parse(p[0]), LocalTime.parse(p[1]), LocalTime.parse(p[2]),
                Integer.parseInt(p[3]), Integer.parseInt(p[4]), Integer.parseInt(p[5]),
                Integer.parseInt(p[6]), Goal.valueOf(p[7]));
    }
}
