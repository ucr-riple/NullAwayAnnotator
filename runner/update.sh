set -exu

CURRENT_VERSION="core-1.3.4-SNAPSHOT.jar"
PROJECT_ROOT=${PROJECT_ROOT:-$(git rev-parse --show-toplevel)}

pushd "$PROJECT_ROOT"
   ./gradlew publishToMavenLocal --rerun-tasks
   mv core/build/libs/"$CURRENT_VERSION" runner/jars/core.jar
popd
