@echo off
rem Compile and run the Daily Routine Advisor (Windows)
if not exist out mkdir out
javac -d out src\*.java
if errorlevel 1 pause & exit /b 1
java -cp out RoutineAdvisorGUI
