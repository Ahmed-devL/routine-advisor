import java.util.ArrayList;
import java.util.List;

/**
 * The result of testing ONE preset routine against the user's statuses:
 * how many rule points it earned and which rules were satisfied.
 */
public class RoutineMatch {
    private final Routine routine;
    private final int points;
    private final int maxPoints;
    private final List<String> reasons = new ArrayList<>();

    public RoutineMatch(Routine routine, int points, List<String> reasons) {
        this.routine = routine;
        this.points = points;
        this.maxPoints = routine.getMaxPoints();
        this.reasons.addAll(reasons);
    }

    public Routine getRoutine() { return routine; }
    public int getPoints() { return points; }
    public int getMaxPoints() { return maxPoints; }
    public List<String> getReasons() { return reasons; }

    public int getPercent() {
        return maxPoints == 0 ? 0 : (int) Math.round(100.0 * points / maxPoints);
    }
}
