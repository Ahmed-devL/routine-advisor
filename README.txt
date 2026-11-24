DAILY ROUTINE ADVISOR - JAVA SWING PBL

WHAT IT DOES
The user enters details of their day: wake-up time, bedtime (sleep hours are
calculated automatically), glasses of water, exercise minutes, leisure screen
time, stress level and a main goal. The app ANALYSES the input with simple
IF-THEN rules (no AI) and recommends the best-matching PRESET routine, shown as
an hour-by-hour timetable built around the user's own wake-up time.

REQUIREMENTS
JDK 8 or newer (tested on 21; compiles with --release 8). Windows, macOS, Linux.

RUN
  Windows : double-click run.bat   (or run it in a terminal)
  Mac/Linux: ./run.sh
  Manually:
      mkdir out
      javac -d out src/*.java
      java -cp out RoutineAdvisorGUI

RUN THE TESTS (no GUI needed)
      javac -d out src/*.java test/SelfTest.java
      java -cp out SelfTest

HOW TO USE
1. Tab "Daily Check-in": set the times, sliders and goal, press "Analyze & Get My Routine".
2. Tab "Your Routine": overall score, a score per area, what was found, the
   recommended routine with reasons and timetable. The drop-down lets you look at
   every other preset and how many rule points it earned.
3. Tab "History": all saved check-ins. View routine / Load into form / Delete.

FILES (src/)
  Status, Metric, Goal      enums (the vocabulary of the rules)
  DailyEntry                the user's input for one day (validated, immutable)
  RoutineAdvisor            the RULE ENGINE: classify -> score -> findings -> match
  Routine, RoutineLibrary   preset routines + the rules that select them
  RoutineMatcher, RoutineMatch   ranks presets by rule points
  AnalysisResult            everything the engine returns
  EntryStore                saves/loads history.csv
  TimePicker, ScoreBar      reusable Swing components
  RoutineAdvisorGUI         main window (3 tabs)
See WALKTHROUGH.md for a complete explanation of how everything works.

DATA
Check-ins are saved in history.csv in the folder the app is run from (one line per day).

LIMITATIONS
General wellness guidance only, not medical advice. Rule thresholds are common
guideline values (7-9 h sleep, ~8 glasses water, 30 min activity/day).
