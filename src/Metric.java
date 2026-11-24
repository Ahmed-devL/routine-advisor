/**
 * The six areas of daily life that the advisor looks at.
 * "weight" says how much each area counts towards the overall score (total = 100).
 */
public enum Metric {
    SLEEP_DURATION("Sleep duration", 25),
    BEDTIME("Bedtime", 10),
    HYDRATION("Hydration", 15),
    ACTIVITY("Physical activity", 20),
    SCREEN("Screen time", 15),
    STRESS("Stress level", 15);

    private final String label;
    private final int weight;

    Metric(String label, int weight) {
        this.label = label;
        this.weight = weight;
    }

    public String getLabel() { return label; }
    public int getWeight() { return weight; }
}
