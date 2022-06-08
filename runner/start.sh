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

cd jars && java -jar core.jar "$@"
