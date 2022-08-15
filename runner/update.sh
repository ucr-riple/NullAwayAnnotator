set -exu

CURRENT_VERSION="core-1.3.2-SNAPSHOT-all.jar"
PROJECT_ROOT=${PROJECT_ROOT:-$(git rev-parse --show-toplevel)}

pushd "$PROJECT_ROOT"
   ./gradlew :type-annotator-scanner:publishToMavenLocal --rerun-tasks
   ./gradlew :core:shadowJar --rerun-tasks
   mv core/build/libs/"$CURRENT_VERSION" runner/jars/core.jar
popd
