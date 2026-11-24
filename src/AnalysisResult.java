import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Everything the rule engine produces for one check-in: the bucket (LOW/OK/HIGH)
 * and 0-100 score for each metric, the overall score, human-readable findings,
 * and the ranked list of preset routines.
 */
public class AnalysisResult {

    /** One sentence of feedback. good = true means "you're doing well here". */
    public static class Finding {
        private final boolean good;
        private final String text;

        public Finding(boolean good, String text) {
            this.good = good;
            this.text = text;
        }

        public boolean isGood() { return good; }
        public String getText() { return text; }
    }

    private final DailyEntry entry;
    private final Map<Metric, Status> statuses = new EnumMap<>(Metric.class);
    private final Map<Metric, Integer> scores = new EnumMap<>(Metric.class);
    private final List<Finding> findings = new ArrayList<>();
    private final List<RoutineMatch> ranking = new ArrayList<>();
    private int overallScore;

    public AnalysisResult(DailyEntry entry) { this.entry = entry; }

    public DailyEntry getEntry() { return entry; }
    public Map<Metric, Status> getStatuses() { return statuses; }
    public Map<Metric, Integer> getScores() { return scores; }
    public List<Finding> getFindings() { return findings; }
    public List<RoutineMatch> getRanking() { return ranking; }
    public RoutineMatch getBestMatch() { return ranking.get(0); }
    public int getOverallScore() { return overallScore; }
    public void setOverallScore(int overallScore) { this.overallScore = overallScore; }

    /** Short verdict for the overall score. */
    public String getVerdict() {
        if (overallScore >= 85) return "Excellent - keep it up";
        if (overallScore >= 70) return "Good - a few tweaks will help";
        if (overallScore >= 50) return "Fair - room to improve";
        return "Needs attention";
    }
}
