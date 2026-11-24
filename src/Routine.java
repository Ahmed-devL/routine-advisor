import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A PRESET routine. It contains three things:
 *   1. Conditions  - the IF-part: "this routine suits someone whose SLEEP is LOW" ...
 *   2. Steps       - the schedule, written relative to the wake-up time or bedtime
 *   3. Tips        - extra advice shown under the schedule
 *
 * Because the steps are relative ("30 minutes after waking up") the same preset
 * can be turned into a real clock timetable for anybody's wake-up time.
 */
public class Routine {

    /** What a step's minutes are measured from. */
    public enum Anchor { AFTER_WAKE, BEFORE_BED }

    /**
     * One IF-rule. It is satisfied when the user's status for 'metric' is one
     * of the 'accepted' statuses. 'weight' is how many points it is worth.
     */
    public static class Condition {
        private final Metric metric;
        private final Set<Status> accepted;
        private final int weight;
        private final String reason;

        Condition(Metric metric, int weight, String reason, Set<Status> accepted) {
            this.metric = metric;
            this.weight = weight;
            this.reason = reason;
            this.accepted = accepted;
        }

        public boolean isSatisfiedBy(Map<Metric, Status> statuses) {
            return accepted.contains(statuses.get(metric));
        }

        public int getWeight() { return weight; }
        public String getReason() { return reason; }
    }

    /** One line of the schedule, still in "relative" form. */
    private static class Step {
        final Anchor anchor;
        final int minutes;
        final String activity;

        Step(Anchor anchor, int minutes, String activity) {
            this.anchor = anchor;
            this.minutes = minutes;
            this.activity = activity;
        }
    }

    /** One line of the final timetable, with a real clock time. */
    public static class TimedStep {
        private final LocalTime time;
        private final String activity;

        TimedStep(LocalTime time, String activity) {
            this.time = time;
            this.activity = activity;
        }

        public LocalTime getTime() { return time; }
        public String getActivity() { return activity; }
    }

    private final String name;
    private final String tagline;
    private final String description;
    private final double targetSleepHours;
    private final List<Condition> conditions = new ArrayList<>();
    private final List<Step> steps = new ArrayList<>();
    private final List<String> tips = new ArrayList<>();
    private Goal bonusGoal = null;
    private int goalBonus = 0;
    private boolean fallback = false;

    public Routine(String name, String tagline, String description, double targetSleepHours) {
        this.name = name;
        this.tagline = tagline;
        this.description = description;
        this.targetSleepHours = targetSleepHours;
    }

    // ---- "builder" methods: each returns 'this' so calls can be chained ----

    /** Adds an IF-rule: this routine fits when 'metric' is one of the given statuses. */
    public Routine when(Metric metric, int weight, String reason, Status... accepted) {
        conditions.add(new Condition(metric, weight, reason, EnumSet.copyOf(Arrays.asList(accepted))));
        return this;
    }

    /** Extra points when the user's goal matches (only counted if at least one condition matched). */
    public Routine goalBonus(Goal goal, int points) {
        this.bonusGoal = goal;
        this.goalBonus = points;
        return this;
    }

    /** Marks this routine as the default one, used when no problems are found. */
    public Routine asFallback() {
        this.fallback = true;
        return this;
    }

    /** Adds a step that happens 'minutes' after waking up. */
    public Routine afterWake(int minutes, String activity) {
        steps.add(new Step(Anchor.AFTER_WAKE, minutes, activity));
        return this;
    }

    /** Adds a step that happens 'minutes' before the target bedtime. */
    public Routine beforeBed(int minutes, String activity) {
        steps.add(new Step(Anchor.BEFORE_BED, minutes, activity));
        return this;
    }

    public Routine tip(String text) {
        tips.add(text);
        return this;
    }

    // ---- getters ----

    public String getName() { return name; }
    public String getTagline() { return tagline; }
    public String getDescription() { return description; }
    public double getTargetSleepHours() { return targetSleepHours; }
    public List<Condition> getConditions() { return conditions; }
    public List<String> getTips() { return tips; }
    public Goal getBonusGoal() { return bonusGoal; }
    public int getGoalBonus() { return goalBonus; }
    public boolean isFallback() { return fallback; }

    /** The most points this routine could ever earn (all conditions + goal bonus). */
    public int getMaxPoints() {
        int total = goalBonus;
        for (Condition c : conditions) {
            total += c.getWeight();
        }
        return total;
    }

    /**
     * Turns the relative steps into a real timetable for this wake-up time.
     *
     * Example: wake = 06:30 and target sleep = 8 h  ->  target bedtime = 22:30.
     * A step "AFTER_WAKE 30"  is 07:00.  A step "BEFORE_BED 60" is 21:30.
     * The list is then sorted by "minutes since waking up", so that a late-night
     * step like 00:15 correctly comes AFTER 23:00 instead of at the top.
     */
    public List<TimedStep> buildTimetable(final LocalTime wake) {
        LocalTime bed = wake.minusMinutes(Math.round(targetSleepHours * 60));
        List<TimedStep> result = new ArrayList<>();
        for (Step s : steps) {
            LocalTime time = (s.anchor == Anchor.AFTER_WAKE)
                    ? wake.plusMinutes(s.minutes)
                    : bed.minusMinutes(s.minutes);
            result.add(new TimedStep(time, s.activity));
        }
        Collections.sort(result, new Comparator<TimedStep>() {
            @Override
            public int compare(TimedStep a, TimedStep b) {
                return Integer.compare(minutesSinceWake(a.getTime(), wake),
                        minutesSinceWake(b.getTime(), wake));
            }
        });
        return result;
    }

    private static int minutesSinceWake(LocalTime t, LocalTime wake) {
        int diff = (t.getHour() * 60 + t.getMinute()) - (wake.getHour() * 60 + wake.getMinute());
        return Math.floorMod(diff, 24 * 60);
    }
}
