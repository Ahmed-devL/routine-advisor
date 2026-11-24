import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The knowledge base of the app: all PRESET routines and the IF-rules that say
 * who each one is for.
 *
 * ORDER MATTERS: if two routines earn the same number of points, the one that
 * appears first in this list wins. So the most important health areas (sleep)
 * are listed first. The fallback routine is always last.
 *
 * To add a new routine you only need to add one more block here; no other
 * file has to change.
 */
public class RoutineLibrary {

    private static final List<Routine> ROUTINES = build();

    public static List<Routine> getAll() {
        return Collections.unmodifiableList(ROUTINES);
    }

    private static List<Routine> build() {
        List<Routine> list = new ArrayList<>();

        // 1 ------------------------------------------------------------------
        list.add(new Routine("Sleep Reset Routine",
                "Fix your sleep first - everything else gets easier.",
                "Built for people who sleep too little (or too much) or go to bed very late. "
                        + "It fixes a consistent wake-up time, cuts caffeine early, and adds a one-hour "
                        + "wind-down with no screens so you fall asleep faster.",
                8.0)
                .when(Metric.SLEEP_DURATION, 4, "Your sleep duration is outside the healthy 7-9 hour range.", Status.LOW, Status.HIGH)
                .when(Metric.BEDTIME, 3, "You go to bed late (after midnight).", Status.HIGH)
                .when(Metric.SCREEN, 1, "High screen time often pushes bedtime later.", Status.HIGH)
                .afterWake(0, "Wake up at the same time every day (no snooze). Drink a glass of water")
                .afterWake(10, "Open the curtains / step outside for 10 minutes of daylight")
                .afterWake(30, "Light stretching (5-10 minutes)")
                .afterWake(50, "Healthy breakfast")
                .afterWake(240, "Water break + short walk")
                .afterWake(420, "Last tea / coffee of the day (caffeine stays in the body for hours)")
                .beforeBed(180, "Finish dinner (keep it light)")
                .beforeBed(60, "Screens off, lights dimmed")
                .beforeBed(45, "Wind-down: reading, journaling or slow breathing")
                .beforeBed(15, "Get ready for bed, set the alarm for tomorrow")
                .beforeBed(0, "Lights out - same time every night")
                .tip("Even on weekends, keep your wake-up time within 1 hour of your weekday time.")
                .tip("Keep the bedroom dark, cool and quiet. Charge your phone away from the bed.")
                .tip("Move bedtime earlier in steps of 15 minutes every few days instead of all at once."));

        // 2 ------------------------------------------------------------------
        list.add(new Routine("Hydration Kickstart",
                "Build a water habit around things you already do.",
                "Built for people who drink fewer than 6 glasses of water a day. Water is tied to "
                        + "fixed moments (waking, meals, breaks) so it becomes automatic instead of something to remember.",
                7.5)
                .when(Metric.HYDRATION, 4, "You drink fewer than 6 glasses of water a day.", Status.LOW)
                .when(Metric.ACTIVITY, 1, "Low activity - a walk doubles as a water break.", Status.LOW)
                .goalBonus(Goal.ENERGY, 1)
                .afterWake(0, "Wake up. Drink 1 full glass of water before anything else")
                .afterWake(30, "Breakfast + 1 glass of water")
                .afterWake(120, "Mid-morning glass of water")
                .afterWake(240, "Glass of water before lunch")
                .afterWake(300, "Lunch")
                .afterWake(480, "Afternoon glass of water + 5 minute walk")
                .afterWake(600, "Glass of water before dinner")
                .afterWake(720, "Dinner, then a short walk")
                .beforeBed(90, "Last full glass of water (so you are not woken at night)")
                .beforeBed(30, "Screens off, prepare for bed")
                .beforeBed(0, "Lights out")
                .tip("Keep a bottle on your desk. If it is in sight, you will drink it.")
                .tip("Target about 8 glasses per day. Add a slice of lemon or mint if plain water is boring.")
                .tip("Pale-yellow urine is a simple sign that you are well hydrated."));

        // 3 ------------------------------------------------------------------
        list.add(new Routine("Move More Routine",
                "Little bursts of movement through the day.",
                "Built for people who get less than 30 minutes of physical activity a day. "
                        + "Instead of one big workout, movement is spread across the day, which is easier to stick to.",
                7.5)
                .when(Metric.ACTIVITY, 4, "You get less than 30 minutes of activity a day.", Status.LOW)
                .when(Metric.STRESS, 1, "Exercise is one of the best stress relievers.", Status.HIGH)
                .afterWake(0, "Wake up, glass of water")
                .afterWake(15, "10 minutes of stretching or yoga")
                .afterWake(45, "Breakfast")
                .afterWake(180, "Stand up and walk for 5 minutes (repeat every hour at your desk)")
                .afterWake(300, "Lunch, then a 10 minute walk")
                .afterWake(540, "Main workout: 20-30 minutes (brisk walk, jog, cycling, sports or bodyweight exercises)")
                .afterWake(630, "Cool-down stretching + water")
                .afterWake(720, "Dinner")
                .beforeBed(60, "Screens off, relax")
                .beforeBed(0, "Lights out")
                .tip("Start with 15 minutes if 30 feels like too much. Doing something daily matters more than doing a lot.")
                .tip("Take the stairs, walk during phone calls, and get off one stop early.")
                .tip("The common guideline is 150 minutes of moderate activity per week (about 30 minutes a day)."));

        // 4 ------------------------------------------------------------------
        list.add(new Routine("Digital Detox & Calm Routine",
                "Less screen, less stress, better evenings.",
                "Built for people with a lot of leisure screen time and/or high stress. It adds "
                        + "screen-free blocks, short breathing breaks and an offline evening.",
                7.5)
                .when(Metric.SCREEN, 3, "You spend more than 4 hours a day on leisure screens.", Status.HIGH)
                .when(Metric.STRESS, 3, "Your stress level is high (4 or 5 out of 5).", Status.HIGH)
                .when(Metric.BEDTIME, 1, "Late-night scrolling usually delays bedtime.", Status.HIGH)
                .goalBonus(Goal.CALM, 2)
                .afterWake(0, "Wake up. Do NOT check your phone for the first 30 minutes")
                .afterWake(10, "5 minutes of slow breathing or meditation")
                .afterWake(30, "Breakfast + plan your top 3 tasks for the day")
                .afterWake(240, "Screen break: 10 minutes outside, no phone")
                .afterWake(300, "Lunch away from screens")
                .afterWake(480, "Short walk or stretching break")
                .afterWake(600, "Social media / entertainment limit reached - set an app timer")
                .afterWake(720, "Dinner with phone in another room")
                .beforeBed(90, "Offline hobby: reading, music, drawing, a board game or a chat with family")
                .beforeBed(45, "Phone on charge outside the bedroom")
                .beforeBed(20, "5 minutes of slow breathing")
                .beforeBed(0, "Lights out")
                .tip("Use your phone's screen-time tool to set a daily limit for the 2-3 apps you use most.")
                .tip("Turn off non-essential notifications so the phone stops pulling you back.")
                .tip("Follow the 20-20-20 rule: every 20 minutes look at something 20 feet away for 20 seconds."));

        // 5 ------------------------------------------------------------------
        list.add(new Routine("Student Focus Routine",
                "Structure your day so studying is easier.",
                "Built for people who want better focus and whose sleep, screen time or stress is getting in the way. "
                        + "It uses fixed study blocks with breaks and keeps the evening free for rest.",
                8.0)
                .when(Metric.SLEEP_DURATION, 2, "Short sleep reduces concentration and memory.", Status.LOW)
                .when(Metric.SCREEN, 2, "Heavy screen time is a major source of distraction.", Status.HIGH)
                .when(Metric.STRESS, 1, "High stress makes it harder to concentrate.", Status.HIGH)
                .goalBonus(Goal.FOCUS, 3)
                .afterWake(0, "Wake up, glass of water")
                .afterWake(20, "10 minutes of light exercise or a walk")
                .afterWake(45, "Breakfast with protein (eggs, curd, nuts, sprouts)")
                .afterWake(90, "Study block 1: hardest subject (45-50 minutes), phone in another room")
                .afterWake(145, "Break: 10 minutes, water and a stretch")
                .afterWake(160, "Study block 2 (45-50 minutes)")
                .afterWake(300, "Lunch + 15 minute walk")
                .afterWake(420, "Study block 3: revision or practice problems")
                .afterWake(540, "Exercise or sport (30 minutes)")
                .afterWake(720, "Dinner, then light revision only")
                .beforeBed(60, "Screens off - quickly write tomorrow's to-do list")
                .beforeBed(0, "Lights out")
                .tip("Study your hardest subject when you are most alert, usually the first block after breakfast.")
                .tip("Do not trade sleep for study time. Sleep is when your brain stores what you learned.")
                .tip("Put your phone in another room during study blocks; even a phone on the desk reduces focus."));

        // 6 ------------------------------------------------------------------
        list.add(new Routine("Energy Boost Routine",
                "Lift your energy by fixing several small things at once.",
                "Built for people with more than one weak area (short sleep, low water, low activity) "
                        + "who feel tired. It combines the most effective parts of the sleep, hydration and movement routines.",
                8.0)
                .when(Metric.SLEEP_DURATION, 2, "You are not sleeping enough to feel fully rested.", Status.LOW)
                .when(Metric.HYDRATION, 2, "Even mild dehydration makes you feel tired.", Status.LOW)
                .when(Metric.ACTIVITY, 2, "Regular movement increases energy levels.", Status.LOW)
                .goalBonus(Goal.ENERGY, 3)
                .afterWake(0, "Wake up, 1 glass of water")
                .afterWake(10, "Daylight: 10 minutes outside or by a window")
                .afterWake(20, "Quick 10 minute stretch or walk")
                .afterWake(50, "Breakfast with protein and fruit")
                .afterWake(180, "Glass of water + 5 minute walk")
                .afterWake(300, "Balanced lunch + glass of water")
                .afterWake(420, "Afternoon slump? 10 minute walk instead of another coffee")
                .afterWake(540, "Exercise: 20-30 minutes")
                .afterWake(720, "Light dinner")
                .beforeBed(60, "Screens off, dim the lights")
                .beforeBed(0, "Lights out")
                .tip("Eat regular meals; long gaps without food cause energy crashes.")
                .tip("If you must nap, keep it to 20 minutes and before 3 PM.")
                .tip("Change one thing at a time: first sleep, then water, then movement."));

        // 7 ------------------------------------------------------------------
        list.add(new Routine("Weight Balance Routine",
                "Steady activity and smart meal timing.",
                "Built for people who want to manage weight and are not very active. "
                        + "It combines regular movement, water before meals and a consistent meal schedule.",
                7.5)
                .when(Metric.ACTIVITY, 2, "More daily movement is the base of weight management.", Status.LOW)
                .when(Metric.HYDRATION, 1, "Water before meals helps you feel full sooner.", Status.LOW)
                .when(Metric.SCREEN, 1, "Snacking in front of screens adds hidden calories.", Status.HIGH)
                .goalBonus(Goal.WEIGHT, 3)
                .afterWake(0, "Wake up, 1-2 glasses of water")
                .afterWake(30, "10 minute brisk walk or bodyweight exercises")
                .afterWake(60, "Protein-rich breakfast")
                .afterWake(240, "Fruit / nuts snack + water")
                .afterWake(290, "A glass of water, then lunch (half the plate vegetables)")
                .afterWake(330, "10 minute walk after lunch")
                .afterWake(540, "Workout: 30 minutes (cardio or strength)")
                .afterWake(660, "A glass of water, then an early light dinner")
                .afterWake(700, "Evening walk (15 minutes)")
                .beforeBed(60, "No more snacks, screens off")
                .beforeBed(0, "Lights out")
                .tip("Stop eating 2-3 hours before sleeping.")
                .tip("Eat slowly and without a screen; it takes about 20 minutes to feel full.")
                .tip("Aim for slow, steady progress, not crash diets."));

        // 8 (fallback) -------------------------------------------------------
        list.add(new Routine("Balanced Maintenance Routine",
                "You are doing well - keep the rhythm.",
                "Used when none of your numbers shows a problem. It is a simple, sustainable "
                        + "daily rhythm that protects the good habits you already have.",
                8.0)
                .asFallback()
                .afterWake(0, "Wake up, glass of water")
                .afterWake(15, "Stretch / light movement")
                .afterWake(45, "Balanced breakfast")
                .afterWake(120, "Focused work or study block")
                .afterWake(300, "Lunch + short walk")
                .afterWake(420, "Afternoon work block, water break")
                .afterWake(540, "Exercise or hobby time")
                .afterWake(720, "Dinner with family / friends")
                .beforeBed(60, "Screens off, relax")
                .beforeBed(0, "Lights out")
                .tip("Review your numbers once a week to catch small slips early.")
                .tip("Try one new healthy challenge each month (a new sport, a recipe, a new walking route)."));

        return list;
    }
}
