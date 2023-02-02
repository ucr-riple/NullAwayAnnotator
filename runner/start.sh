#!/bin/bash
set -exu

pushd "/tmp/NullAwayAnnotator/"
   pushd runner
     pushd jars
         java -jar core.jar "$@"
     popd
  popd
popd
