#!/bin/sh
RUNJAR="build.jar"
LIBJAR="gnuprologjava-0.2.6.jar"
LIBPATH=$RUNJAR";"$LIBJAR

chmod u+x $LIBJAR
chmod u+x $RUNJAR
#echo "LIBPATH is: " $LIBPATH
#echo "Command is: java -cp $LIBPATH -jar $RUNJAR"
#java -cp $LIBPATH -jar $RUNJAR
#java -cp ./build.jar;./gnuprologjava-0.2.6.jar -jar build.jar

if [ -n "$1" ]; then
    SCENARIOFILE=$1
    echo "Using provided ScenarioFile: $SCENARIOFILE"
else
    SCENARIOFILE=scenario_1.xml
    echo "Using default ScenarioFile: $SCENARIOFILE"
fi

java -jar $RUNJAR $SCENARIOFILE # $*
