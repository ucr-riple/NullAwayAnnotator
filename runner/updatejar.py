import os

os.system("cd /Users/nima/Developer/NullAwayFixer/NullAway && ./gradlew installArchives --rerun-tasks" + " > /dev/null 2>&1")
os.system("cd /Users/nima/Developer/NullAwayFixer/NullAwayAutoFixer && ./gradlew fatJar --rerun-tasks" + " > /dev/null 2>&1")
os.system("mv /Users/nima/Developer/NullAwayFixer/NullAwayAutoFixer/build/libs/NullAwayAutoFixer-1.1.1-LOCAL.jar /Users/nima/Developer/NullAwayFixer/Scripts/Diagnoser/jars/NullAwayAutoFixer.jar" + " > /dev/null 2>&1")