cd ../ && ./gradlew install --rerun-tasks && ./gradlew :core:fatJar --rerun-tasks
mv core/build/libs/core.jar runner/jars/core.jar
