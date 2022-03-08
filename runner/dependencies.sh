if [ mvn dependency:get -Dartifact=edu.ucr.cs.riple:nullaway:0.7.12-SNAPSHOT -o -DrepoUrl=file://~/.m2/repository ]; then
    :
else
    pushd /tmp/
    git clone https://github.com/nimakarimipour/NullAway.git
    pushd NullAway
    git checkout autofix

    ./gradlew installArchive
    
    popd
    popd
fi