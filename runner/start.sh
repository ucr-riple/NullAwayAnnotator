set -exu

PROJECT_ROOT=${PROJECT_ROOT:-$(git rev-parse --show-toplevel)}

pushd "$PROJECT_ROOT"
  pushd runner
    DIR=jars
    if [ ! -d "$DIR" ]; then
        echo "$DIR does not exists, creating..."
        mkdir jars
    fi
    FILE=./jars/core.jar
    if [ ! -f "$FILE" ]; then
        echo "$FILE does not exists, creating..."
        ./update.sh
    fi
  popd
popd

pushd "$PROJECT_ROOT"
  pushd runner
    pushd "$DIR"
      java -jar core.jar "$@"
    popd
  popd
popd