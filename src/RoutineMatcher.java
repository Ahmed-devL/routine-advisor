import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Rule-based MATCHING: decides which preset routine suits the user.
 *
 * For each routine we look at its conditions one by one:
 *    if condition is satisfied by the user's statuses -> add its weight to the routine's points.
 * Then:
 *    - a routine only "qualifies" if at least one of its conditions matched
 *      (a goal alone is never enough),
 *    - qualified routines also get their goal bonus if the user's goal matches,
 *    - routines are sorted by points (highest first; ties keep library order),
 *    - if NO routine qualified (no problems found) the fallback routine wins.
 */
public class RoutineMatcher {

    public static List<RoutineMatch> rank(Map<Metric, Status> statuses, Goal goal) {
        List<RoutineMatch> matches = new ArrayList<>();
        Routine fallback = null;
        boolean anyQualified = false;

        for (Routine routine : RoutineLibrary.getAll()) {
            if (routine.isFallback()) {
                fallback = routine;
                continue;
            }
            int points = 0;
            List<String> reasons = new ArrayList<>();
            for (Routine.Condition c : routine.getConditions()) {
                if (c.isSatisfiedBy(statuses)) {
                    points += c.getWeight();
                    reasons.add(c.getReason());
                }
            }
            if (points > 0) {
                anyQualified = true;
                if (routine.getBonusGoal() == goal) {
                    points += routine.getGoalBonus();
                    reasons.add("It matches your goal: " + goal.getLabel() + ".");
                }
            }
            matches.add(new RoutineMatch(routine, points, reasons));
        }

        // The fallback: "no problems found" = a perfect match; otherwise 0 points.
        if (fallback != null) {
            List<String> reasons = new ArrayList<>();
            int points = 0;
            if (!anyQualified) {
                points = 1;
                reasons.add("All your numbers are in a healthy range, so no fix is needed.");
            }
            matches.add(new RoutineMatch(fallback, points, reasons) {
                @Override
                public int getPercent() { return getPoints() > 0 ? 100 : 0; }
            });
        }

        // Collections.sort is STABLE: equal points keep their library order.
        Collections.sort(matches, new Comparator<RoutineMatch>() {
            @Override
            public int compare(RoutineMatch a, RoutineMatch b) {
                return Integer.compare(b.getPoints(), a.getPoints());
            }
        });
        return matches;
    }
}
