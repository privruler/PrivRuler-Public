#!/bin/sh
set -e

SOOTCLASS="$PWD/soot/gson-2.8.5.jar:$PWD/soot/fuzzywuzzy-1.2.0.jar:$PWD/soot/soot-infoflow-cmd-2.9.0-jar-with-dependencies.jar:$PWD/soot/trove-3.0.3.jar"

cd src
echo "Compiling..."
javac -g -cp $SOOTCLASS:. privruler/*.java
javac -g -cp $SOOTCLASS:. privruler/methodsignature/*.java
javac -g -cp $SOOTCLASS:. dfa/util/*.java
javac -g -cp $SOOTCLASS:. dfa/*.java
javac -g -cp $SOOTCLASS:. analysisutils/*.java
cd ..
echo "Installing..."
mkdir -p bin/privruler/methodsignature
mkdir -p bin/dfa/util
mkdir -p bin/analysisutils
mv src/privruler/*.class bin/privruler/
mv src/privruler/methodsignature/*.class bin/privruler/methodsignature/
mv src/dfa/*.class bin/dfa/
mv src/dfa/util/*.class bin/dfa/util/
mv src/analysisutils/*.class bin/analysisutils/