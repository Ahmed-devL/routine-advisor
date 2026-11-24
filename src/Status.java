/**
 * The three "buckets" a measured value can fall into.
 * The rule engine first converts every number the user typed (e.g. 5.5 hours
 * of sleep) into one of these, and all later rules only talk about these buckets.
 */
public enum Status {
    LOW("Low"),
    OK("Healthy"),
    HIGH("High");

    private final String label;

    Status(String label) { this.label = label; }

    public String getLabel() { return label; }
}
