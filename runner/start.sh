set -exu

CURRENT_VERSION="annotator-core-1.3.6-alpha-2.jar"
PROJECT_ROOT=${PROJECT_ROOT:-$(git rev-parse --show-toplevel)}

pushd "$PROJECT_ROOT"
   ./gradlew publishToMavenLocal --rerun-tasks
   mv annotator-core/build/libs/"$CURRENT_VERSION" runner/jars/core.jar

   pushd runner
     pushd jars
         java -jar core.jar "$@"
       popd
     popd
popd
