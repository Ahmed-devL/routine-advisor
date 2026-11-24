#!/bin/sh
# Compile and run the Daily Routine Advisor (macOS / Linux)
mkdir -p out
javac -d out src/*.java && java -cp out RoutineAdvisorGUI
