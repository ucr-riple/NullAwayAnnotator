cd ../ && ./gradlew ./gradlew :css:publishToMavenLocal --rerun-tasks && ./gradlew :core:shadowJar --rerun-tasks
mv core/build/libs/core.jar runner/jars/core.jar
