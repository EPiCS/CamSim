#!/bin/sh
RUNJAR="build.jar"
LIBJAR="gnuprologjava-0.2.6.jar"

if [ ! -f $RUNJAR ]; then
	echo "Error: $RUNJAR does not exist. Please compile the JAR first"
	exit
fi

if [ ! -f $LIBJAR ]; then
	echo "Error: The required library JAR '$LIBJAR' does not exist"
	exit
fi

chmod u+x $LIBJAR
chmod u+x $RUNJAR

# If args specified, use them, else use default scenario file
if [ $# -gt 0 ]; then
    ARGS=$*
else
    ARGS=scenarios/scenario1_active.xml
    echo "Using default ScenarioFile: $ARGS"
fi

java -jar $RUNJAR $ARGS
