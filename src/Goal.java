/**
 * What the user mainly wants out of their routine.
 * A goal does not change the analysis; it only gives a small bonus to presets
 * that are designed for that goal (used to break ties between routines) and
 * adds one extra tip at the end of the recommended routine.
 */
public enum Goal {
    GENERAL("General health", "Keep your wake-up and sleep times the same every day, including weekends. Consistency is the cheapest health upgrade there is."),
    ENERGY("More energy during the day", "Get 10 minutes of outdoor daylight within an hour of waking up. It helps set your body clock and boosts alertness."),
    FOCUS("Better focus / studies", "Study in 45-50 minute blocks with 10 minute breaks, and keep your phone out of reach while you work."),
    WEIGHT("Weight management", "Have your meals at fixed times and stop eating about 2-3 hours before bed. Walking after meals helps too."),
    CALM("Feel calmer / less stress", "Spend 5 minutes on slow breathing (inhale 4 s, exhale 6 s) once in the morning and once before bed.");

    private final String label;
    private final String tip;

    Goal(String label, String tip) {
        this.label = label;
        this.tip = tip;
    }

    public String getLabel() { return label; }
    public String getTip() { return tip; }

    /** Text shown in the combo box. */
    @Override
    public String toString() { return label; }
}
