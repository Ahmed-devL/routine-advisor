import java.util.Map;

/**
 * The RULE ENGINE. Takes a DailyEntry and produces an AnalysisResult in 4 steps:
 *
 *   STEP 1  classify()  numbers  -> LOW / OK / HIGH           (threshold rules)
 *   STEP 2  score()     numbers  -> 0..100 per area            (simple formulas)
 *   STEP 3  findings    status   -> sentences for the user     (rule -> message)
 *   STEP 4  matcher     statuses -> ranked preset routines     (condition matching)
 *
 * No AI or machine learning: every decision is a plain if/else rule that can be
 * read, explained and changed. All thresholds are constants at the top so they
 * are easy to find and tune.
 */
public class RoutineAdvisor {

    // ---- thresholds (common general-health guidelines) ----
    static final double SLEEP_MIN_HOURS = 7.0;      // adults: 7-9 hours
    static final double SLEEP_MAX_HOURS = 9.0;
    static final int LATE_BEDTIME_AFTER_MINUTES = 60; // later than 1 hour after 23:00 = after midnight
    static final int WATER_LOW_BELOW = 6;            // fewer than 6 glasses = low
    static final int WATER_TARGET = 8;               // ~8 glasses is the common target
    static final int ACTIVITY_TARGET_MINUTES = 30;   // 150 min/week = ~30 min/day
    static final int SCREEN_COMFORT_HOURS = 2;       // full score up to 2 h
    static final int SCREEN_HIGH_ABOVE = 4;          // more than 4 h = high
    static final int STRESS_HIGH_FROM = 4;           // 4 or 5 = high
    static final int STRESS_LOW_UPTO = 2;            // 1 or 2 = low (good)

    public AnalysisResult analyze(DailyEntry e) {
        AnalysisResult r = new AnalysisResult(e);

        // STEP 1 + 2: classify and score every metric
        for (Metric m : Metric.values()) {
            r.getStatuses().put(m, classify(m, e));
            r.getScores().put(m, score(m, e));
        }

        // overall score = weighted average of the six scores
        double total = 0;
        for (Metric m : Metric.values()) {
            total += r.getScores().get(m) * m.getWeight() / 100.0;
        }
        r.setOverallScore((int) Math.round(total));

        // STEP 3: one finding per metric
        for (Metric m : Metric.values()) {
            r.getFindings().add(describe(m, r.getStatuses().get(m), e));
        }

        // STEP 4: rank the preset routines
        r.getRanking().addAll(RoutineMatcher.rank(r.getStatuses(), e.getGoal()));
        return r;
    }

    // ------------------------------------------------------------------ STEP 1
    static Status classify(Metric m, DailyEntry e) {
        switch (m) {
            case SLEEP_DURATION:
                double h = e.getSleepHours();
                if (h < SLEEP_MIN_HOURS) return Status.LOW;
                if (h > SLEEP_MAX_HOURS) return Status.HIGH;
                return Status.OK;
            case BEDTIME:
                // For bedtime "HIGH" means "too late".
                return lateMinutes(e) > LATE_BEDTIME_AFTER_MINUTES ? Status.HIGH : Status.OK;
            case HYDRATION:
                return e.getWaterGlasses() < WATER_LOW_BELOW ? Status.LOW : Status.OK;
            case ACTIVITY:
                return e.getExerciseMinutes() < ACTIVITY_TARGET_MINUTES ? Status.LOW : Status.OK;
            case SCREEN:
                return e.getScreenHours() > SCREEN_HIGH_ABOVE ? Status.HIGH : Status.OK;
            case STRESS:
                if (e.getStressLevel() >= STRESS_HIGH_FROM) return Status.HIGH;
                if (e.getStressLevel() <= STRESS_LOW_UPTO) return Status.LOW;
                return Status.OK;
            default:
                throw new IllegalStateException("Unknown metric " + m);
        }
    }

    /**
     * How many minutes after 23:00 the user went to bed.
     * We measure from 18:00 so that 00:30 counts as "late" (390 min after 18:00)
     * instead of looking "early" because 00:30 is a small clock number.
     * 23:00 is 300 minutes after 18:00, so lateness = that distance minus 300.
     */
    static int lateMinutes(DailyEntry e) {
        int bed = e.getBedTime().getHour() * 60 + e.getBedTime().getMinute();
        int sinceSix = Math.floorMod(bed - 18 * 60, 24 * 60);
        return Math.max(0, sinceSix - 300);
    }

    // ------------------------------------------------------------------ STEP 2
    static int score(Metric m, DailyEntry e) {
        double s;
        switch (m) {
            case SLEEP_DURATION:
                double h = e.getSleepHours();
                if (h < SLEEP_MIN_HOURS) s = 100 - 20 * (SLEEP_MIN_HOURS - h);       // -20 per missing hour
                else if (h > SLEEP_MAX_HOURS) s = 100 - 20 * (h - SLEEP_MAX_HOURS);  // -20 per extra hour
                else s = 100;
                break;
            case BEDTIME:
                s = 100 - lateMinutes(e) * 0.5;                                      // -1 point per 2 late minutes
                break;
            case HYDRATION:
                s = 100.0 * e.getWaterGlasses() / WATER_TARGET;                      // 8 glasses = 100
                break;
            case ACTIVITY:
                s = 100.0 * e.getExerciseMinutes() / ACTIVITY_TARGET_MINUTES;        // 30 min = 100
                break;
            case SCREEN:
                s = 100 - 20 * Math.max(0, e.getScreenHours() - SCREEN_COMFORT_HOURS); // -20 per hour above 2
                break;
            case STRESS:
                s = (5 - e.getStressLevel()) * 25;                                   // 1 -> 100 ... 5 -> 0
                break;
            default:
                throw new IllegalStateException("Unknown metric " + m);
        }
        return (int) Math.round(Math.max(0, Math.min(100, s)));   // keep within 0..100
    }

    // ------------------------------------------------------------------ STEP 3
    static AnalysisResult.Finding describe(Metric m, Status st, DailyEntry e) {
        switch (m) {
            case SLEEP_DURATION:
                if (st == Status.LOW) return bad(String.format(
                        "You slept %.1f hours. Adults need 7-9 hours; you are about %.1f hours short.",
                        e.getSleepHours(), SLEEP_MIN_HOURS - e.getSleepHours()));
                if (st == Status.HIGH) return bad(String.format(
                        "You slept %.1f hours. More than 9 hours regularly can leave you groggy.", e.getSleepHours()));
                return good(String.format("You slept %.1f hours, which is in the healthy 7-9 hour range.", e.getSleepHours()));
            case BEDTIME:
                if (st == Status.HIGH) return bad("You went to bed at " + e.getBedTime()
                        + ", which is late. Aim for bedtime before midnight, ideally around 22:30-23:00.");
                return good("Your bedtime of " + e.getBedTime() + " is reasonable.");
            case HYDRATION:
                if (st == Status.LOW) return bad("You drank " + e.getWaterGlasses()
                        + " glasses of water. Aim for about 8 a day.");
                return good("You drank " + e.getWaterGlasses() + " glasses of water - good hydration.");
            case ACTIVITY:
                if (st == Status.LOW) return bad("You exercised " + e.getExerciseMinutes()
                        + " minutes. Aim for at least 30 minutes of movement a day.");
                return good("You were active for " + e.getExerciseMinutes() + " minutes - you meet the 30 minute target.");
            case SCREEN:
                if (st == Status.HIGH) return bad("You spent " + e.getScreenHours()
                        + " hours on leisure screens. More than 4 hours a day is linked to poorer sleep and focus.");
                return good("Your leisure screen time of " + e.getScreenHours() + " hours is under control.");
            case STRESS:
                if (st == Status.HIGH) return bad("Your stress level is " + e.getStressLevel()
                        + " out of 5. Regular breaks, movement and sleep all help lower it.");
                if (st == Status.LOW) return good("You feel calm today (" + e.getStressLevel() + " out of 5). Great.");
                return good("Your stress level is moderate (" + e.getStressLevel() + " out of 5).");
            default:
                throw new IllegalStateException("Unknown metric " + m);
        }
    }

    private static AnalysisResult.Finding good(String text) { return new AnalysisResult.Finding(true, text); }
    private static AnalysisResult.Finding bad(String text) { return new AnalysisResult.Finding(false, text); }

    /** Convenience for tests: just the statuses. */
    public Map<Metric, Status> statusesOf(DailyEntry e) { return analyze(e).getStatuses(); }
}
