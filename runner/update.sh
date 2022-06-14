cd ../ && ./gradlew :css:publishToMavenLocal --rerun-tasks && ./gradlew :core:shadowJar --rerun-tasks
mv core/build/libs/core-1.3.0-SNAPSHOT-all.jar runner/jars/core.jar
