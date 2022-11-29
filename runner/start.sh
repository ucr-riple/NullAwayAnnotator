set -exu

CURRENT_VERSION="core-1.3.3-LOCAL.jar"
PROJECT_ROOT=${PROJECT_ROOT:-$(git rev-parse --show-toplevel)}

pushd "$PROJECT_ROOT"
   ./gradlew publishToMavenLocal -x signMavenPublication --rerun-tasks
   mv core/build/libs/"$CURRENT_VERSION" runner/jars/core.jar

   pushd runner
       pushd "$DIR"
         java -jar core.jar "$@"
       popd
     popd
popd
