#!/bin/bash
set -exu

CURRENT_VERSION="annotator-core-0.0.1.jar"

pushd "/tmp/NullAwayAnnotator/"
   rm -rvf runner/jars
   mkdir runner/jars
   ./gradlew publishToMavenLocal -x signMavenPublication -x signShadowPublication --rerun-tasks
   mv annotator-core/build/libs/"$CURRENT_VERSION" runner/jars/core.jar

   pushd runner
     pushd jars
         java -jar core.jar "$@"
     popd
  popd
popd
