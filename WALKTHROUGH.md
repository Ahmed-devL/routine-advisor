# Daily Routine Advisor — Complete Walkthrough

This document explains **what the app does, how every part works, and why it was built that way**, so you can explain it to your teacher (and answer viva questions). Read sections 1–4 first; sections 5–9 go file by file.

---

## 1. What changed from the old project (and why)

| | Old: Healthy Habit Builder | New: Daily Routine Advisor |
|---|---|---|
| Idea | User creates habits and ticks them off | User reports their day; the app **analyses it and recommends a routine** |
| Who does the thinking | The user | The app (rule-based engine) |
| Output | A checklist + streaks | Scores, findings, and a ready-made timetable |
| Data | Habit objects (serialization) | One `DailyEntry` per day (CSV file) |
| Sir's feedback | "It's a tracker, not a builder" | The app now **builds** a routine from the user's data |

---

## 2. The big idea in one picture

```
 ┌─────────────────┐   ┌────────────┐   ┌────────────────────────────────────────┐
 │ Check-in form   │   │ DailyEntry │   │            RoutineAdvisor              │
 │ (Swing, tab 1)  │──▶│ (validated │──▶│ STEP 1 classify  numbers -> LOW/OK/HIGH│
 │ wake, bed,      │   │  user data)│   │ STEP 2 score     numbers -> 0..100     │
 │ water, exercise │   └────────────┘   │ STEP 3 findings  status  -> sentences  │
 │ screen, stress, │                    │ STEP 4 matcher   statuses-> best preset│
 │ goal            │                    └───────────────────┬────────────────────┘
 └─────────────────┘                                        ▼
                                                   ┌──────────────────┐
   Tab 2 shows it  ◀───────────────────────────────│  AnalysisResult  │
   Tab 3 history   ◀── EntryStore (history.csv)    └──────────────────┘
```

**Rule-based, not AI.** Every decision is an `if/else` rule or a points total. You can read any result and trace exactly *why* it happened — this is called an **explainable** system, and it is what an "expert system" is. The app even shows the user *why* a routine was chosen.

---

## 3. How the analysis works (the heart of the project)

### Step 1 — Classify: turn numbers into LOW / OK / HIGH
(`RoutineAdvisor.classify`)

| Area | LOW | OK | HIGH |
|---|---|---|---|
| Sleep duration | < 7 h | 7–9 h | > 9 h |
| Bedtime | – | before/at about midnight | **later than 00:00** (meaning "too late") |
| Hydration | < 6 glasses | ≥ 6 | – |
| Activity | < 30 min | ≥ 30 | – |
| Screen (leisure) | – | ≤ 4 h | > 4 h |
| Stress (1–5) | 1–2 (calm, good) | 3 | 4–5 |

The thresholds are constants at the top of `RoutineAdvisor.java` (`SLEEP_MIN_HOURS = 7.0`, `WATER_LOW_BELOW = 6` …). They come from common guidelines (adults 7–9 h sleep, ~8 glasses water, 150 min exercise/week ≈ 30 min/day). To change a rule, change a constant.

**Sleep hours across midnight.** Bed 23:30 → wake 07:00. `Duration.between(bed, wake)` is negative (07:00 is "earlier" on the clock), so the code adds 24 h → 7.5 h. (`DailyEntry.calculateSleepHours`)

**"Late bedtime".** 00:30 looks like a small number, so naive code thinks it is *early*. The fix: measure minutes since **18:00** (so 00:30 = 390) and compare with 23:00 (= 300). Late minutes = 390 − 300 = 90. (`RoutineAdvisor.lateMinutes`)

### Step 2 — Score each area 0–100
(`RoutineAdvisor.score`) — used for the bars and the overall number.

| Area | Formula |
|---|---|
| Sleep | 100 if 7–9 h; otherwise 100 − 20 × (hours short or over) |
| Bedtime | 100 − 0.5 × (minutes after 23:00) |
| Hydration | glasses / 8 × 100 |
| Activity | minutes / 30 × 100 |
| Screen | 100 − 20 × (hours above 2) |
| Stress | (5 − level) × 25 |

All clamped to 0–100. **Overall score** = weighted average, weights from `Metric`: sleep 25, bedtime 10, hydration 15, activity 20, screen 15, stress 15 (sum = 100).

### Step 3 — Findings (rule → sentence)
`RoutineAdvisor.describe` returns one sentence per area, e.g. *"You slept 5.5 hours. Adults need 7–9 hours; you are about 1.5 hours short."* Green dot = good, red dot = needs work.

### Step 4 — Match a preset routine (rule-based matching)
(`RoutineMatcher.rank`, rules in `RoutineLibrary`)

Each preset routine carries its own IF-rules, each worth some points:

```java
new Routine("Sleep Reset Routine", ...)
    .when(Metric.SLEEP_DURATION, 4, "Your sleep is outside 7-9 h.", Status.LOW, Status.HIGH)
    .when(Metric.BEDTIME,        3, "You go to bed late.",          Status.HIGH)
    .when(Metric.SCREEN,         1, "High screen time delays bed.", Status.HIGH)
```
Read it as: *"+4 points if sleep is LOW or HIGH, +3 if bedtime is late, +1 if screen time is HIGH."*

The matcher:
1. For every routine, add up the weights of its satisfied rules.
2. A routine **qualifies** only if at least one rule matched.
3. Qualified routines get a **goal bonus** if the user's goal fits (e.g. goal "More energy" → +3 for Energy Boost). A goal alone can never qualify a routine.
4. Sort by points, highest first. **Ties** are broken by order in `RoutineLibrary` (sleep first — it matters most).
5. If **no** routine qualified (no problems found), the fallback **Balanced Maintenance Routine** is chosen.

The 8 presets: Sleep Reset, Hydration Kickstart, Move More, Digital Detox & Calm, Student Focus, Energy Boost, Weight Balance, Balanced Maintenance (fallback).

### Step 5 — Build the timetable
(`Routine.buildTimetable`)

Preset steps are stored **relative**: `afterWake(30, "Breakfast")`, `beforeBed(60, "Screens off")`. Given the user's wake time and the routine's target sleep (e.g. 8 h) the code computes the real times: wake 06:30 → bedtime 22:30 → breakfast 07:00, screens off 21:30. Steps are sorted by "minutes since waking" so a 00:15 step lands after 23:00. So **the same preset gives each user a personal timetable.**

---

## 4. Worked example (follow this in your viva)

Input: wake 06:30, bed 01:00, water 3, exercise 10 min, screen 7 h, stress 5, goal *More energy*.

1. Sleep = 01:00→06:30 = **5.5 h** → LOW. Bedtime 60+ min late → HIGH. Water 3 → LOW. Exercise 10 → LOW. Screen 7 → HIGH. Stress 5 → HIGH.
2. Scores: sleep 70, bedtime 40, hydration 38, activity 33, screen 0, stress 0.
   Overall = 70×0.25 + 40×0.10 + 37.5×0.15 + 33.3×0.20 + 0 + 0 = **34 / 100** → "Needs attention".
3. Points: Energy Boost 2+2+2 + goal 3 = **9**; Sleep Reset 4+3+1 = 8; Digital Detox 3+3+1 = 7; Hydration Kickstart 4+1+goal 1 = 6; Move More 4+1 = 5; Student Focus 2+2+1 = 5; Weight Balance 2+1+1 = 4.
4. **Winner: Energy Boost Routine (9 of 9 points).** Without the energy goal, Sleep Reset (8) would win.

---

## 5. Files, one by one

| File | Role | Key idea |
|---|---|---|
| `Status.java` | enum LOW / OK / HIGH | the "buckets" |
| `Metric.java` | enum of 6 areas + weights | one place that lists the areas |
| `Goal.java` | enum of 5 goals + a tip each | tie-break bonus + extra tip |
| `DailyEntry.java` | one day's input | **immutable**, validates in constructor, CSV conversion, sleep calc |
| `AnalysisResult.java` | engine output | statuses, scores, findings, ranking, overall score |
| `RoutineAdvisor.java` | the rule engine | steps 1–4 above; thresholds as constants |
| `Routine.java` | a preset | conditions + steps + tips; fluent "builder" methods; `buildTimetable` |
| `RoutineLibrary.java` | the 8 presets | the **knowledge base**; add a routine = add one block |
| `RoutineMatcher.java` | scoring/ranking | points, qualify, goal bonus, stable sort, fallback |
| `RoutineMatch.java` | one routine's result | points, max points, reasons |
| `EntryStore.java` | persistence | `history.csv`, one entry per date, skips damaged lines |
| `TimePicker.java` | custom Swing component | extends `JPanel`; two `JSpinner`s |
| `ScoreBar.java` | custom Swing component | extends `JComponent`; overrides `paintComponent` |
| `RoutineAdvisorGUI.java` | main window | `JTabbedPane`, 3 tabs; **contains no health rules** |
| `test/SelfTest.java` | 64 console tests | classification, scoring, matching, timetable, validation, storage |

---

## 6. The GUI (Java Swing) explained

- **`RoutineAdvisorGUI extends JFrame`** — the window. Header is a `JPanel` (BorderLayout). Body is a `JTabbedPane`.
- **Tab 1 (form):** a `JPanel` using `GridBagLayout` (label | input | value). Inputs: `TimePicker` ×2, `JSlider` ×4, `JComboBox<Goal>`. Each slider has a **`ChangeListener`** that updates its value label live ("6 glasses"). Time pickers also update the live "Sleep duration" label.
- **Button → `analyzeAndShow()`**: reads the widgets → `new DailyEntry(...)` (throws `IllegalArgumentException` if invalid → shown with `JOptionPane`) → `advisor.analyze(entry)` → `store.save(entry)` → `showResult(...)` → switch to tab 2.
- **Tab 2 (results):** rebuilt every time from the `AnalysisResult`: score card (6 `ScoreBar`s), findings card, routine card. Layout is `BoxLayout` (vertical) inside a `JScrollPane`. Text uses HTML inside `JLabel` for automatic wrapping (so user text is escaped by `esc()`). The timetable is a `JTable` with a custom `DefaultTableCellRenderer` (striped rows). A `JComboBox` lets the user open any other preset — an `ActionListener` refills the detail panel.
- **Tab 3 (history):** `JTable` with a non-editable `DefaultTableModel`; each row is re-analysed to show its score and routine. Buttons: View routine, Load into form, Delete (with confirm dialog).
- **Event-driven programming:** nothing runs in a loop; the code reacts to events (button clicks, slider moves, combo selection) through listeners written as lambdas.
- **`SwingUtilities.invokeLater`** in `main` starts the GUI on the Event Dispatch Thread, which is the correct way to start Swing.
- **Separation:** GUI = collects input and shows output. Engine = decides. That is why the engine could be tested without a window (`SelfTest`).

---

## 7. Data storage

`EntryStore` writes `history.csv`:
```
date,wake,bed,water,exercise,screen,stress,goal
2026-11-19,06:30,01:00,3,10,7,5,ENERGY
```
- Loaded in the constructor; saved after every change.
- One entry per date — a second check-in on the same day **replaces** the first.
- `try-with-resources` closes files automatically. A damaged line is skipped (caught exception) instead of crashing the app.
- CSV was chosen over serialization (used in the old project) because it is human-readable and opens in Excel.

---

## 8. OOP and Java concepts used (for the report)

- **Classes/objects:** `DailyEntry`, `Routine`, `AnalysisResult`, …
- **Encapsulation:** private final fields + getters; `DailyEntry` is immutable and validates itself.
- **Enums with fields:** `Metric` (label + weight), `Goal` (label + tip), `Status`.
- **Inheritance:** `TimePicker extends JPanel`, `ScoreBar extends JComponent`, `RoutineAdvisorGUI extends JFrame`.
- **Polymorphism / overriding:** `paintComponent`, `toString` in `Goal`, cell renderer, and `getPercent` overridden for the fallback match.
- **Inner/nested classes:** `Routine.Condition`, `Routine.Step`, `Routine.TimedStep`, `AnalysisResult.Finding`.
- **Collections:** `List`, `EnumMap` (map keyed by an enum), `EnumSet`, `Collections.sort` with a `Comparator`.
- **Fluent/builder style:** `new Routine(...).when(...).afterWake(...).tip(...)` — each method returns `this`.
- **Exception handling:** `IllegalArgumentException` for bad input, `IOException` for files, `try-with-resources`, damaged-line recovery.
- **Java Time API:** `LocalTime`, `LocalDate`, `Duration`.
- **Lambdas and listeners:** event handling in Swing.
- **Separation of concerns (MVC-like):** model (`DailyEntry`, `Routine`), logic (`RoutineAdvisor`), view (GUI).

---

## 9. How to modify it

- **Change a threshold:** edit the constant in `RoutineAdvisor` (e.g. `WATER_LOW_BELOW = 7`).
- **Add a routine:** add a `list.add(new Routine(...).when(...).afterWake(...)...)` block in `RoutineLibrary`. Nothing else changes (the GUI, matcher and history pick it up automatically). Put it before the fallback.
- **Add a new input (e.g. caffeine cups):** add a `Metric`, a field in `DailyEntry` (+ CSV), a case in `classify`, `score`, `describe`, a widget in the GUI, and rules that mention it.
- **Change how important an area is:** change its weight in `Metric`, or a rule's weight in `RoutineLibrary`.

---

## 10. Testing

`java -cp out SelfTest` runs 64 checks: sleep across midnight, boundaries (bedtime 00:00 vs 00:05), every status, score ranges, which routine wins for typical situations, timetable ordering for every preset, input validation, saving/reloading/replacing/deleting history, and recovery from a damaged file. All pass. The GUI was also run and checked visually on all three tabs.

**A real bug found while testing:** two presets had goal bonuses that let the wrong routine win (e.g. a user with the *Weight management* goal got "Move More" instead of "Weight Balance"). Tests caught it and the bonuses were rebalanced so each goal bonus belongs only to the routine built for it.

---

## 11. Likely viva questions

**Why not AI?** The teacher asked for something simpler; rules are transparent, need no training data, and every recommendation can be explained to the user.

**How is this different from a habit tracker?** A tracker stores what the user already decided to do. This app takes raw data about the user's day, evaluates it, and *generates* a routine — the user didn't have to design anything.

**How does it choose between routines?** Points. Each routine has weighted rules; matched rules add points; highest wins; ties go to the earlier (more important) routine; no problems → fallback.

**What if two routines tie?** The stable sort keeps library order, so the earlier one (sleep first) wins.

**How is the timetable personalised?** Steps are stored as offsets from waking up / bedtime and converted using the user's wake time.

**How do you handle sleeping past midnight?** If wake is not after bed on the clock, add 24 hours.

**How would you extend it?** Weekly averages, more inputs (caffeine, meals), user-editable thresholds, export timetable to PDF/CSV, a database.

**Limitations?** General guidance only, not medical advice; thresholds are general-population values; self-reported data; one entry per day.
