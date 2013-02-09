#!/bin/sh
ComputeStandardDev=true # true or false to compute standard deviations in the avg. file


function generateSummaries {
    COUNT=0
    SUMMARY_STARTTIME=$SECONDS
    if [ ! -d "$BASELOGDIR" ]; then
	    echo "No logdir"
	    exit
    fi
    
    SUMMARY_FILE=$SUMMARY_DIR"/$SCENARIO_NAME-summary.txt"
    cat $BASELOGDIR/*summary.txt | grep -v TIME > $SUMMARY_FILE

    COUNT=$((COUNT+1))

    echo
    echo "$COUNT experiments summarised in "$(($SECONDS-$SUMMARY_STARTTIME))" seconds"
}

function generateAverageFile {
    TOTAL_CONF_FOR_FILE=0
    TOTAL_COMM_FOR_FILE=0
    COUNT=0

    echo "Running on file: $SUMMARY_FILE"
    
    ORIG_IFS=$IFS
    IFS=','
    while IFS=, read a b c d e f g; do 
	    TOTAL_CONF_FOR_FILE="$TOTAL_CONF_FOR_FILE+$c"
	    TOTAL_COMM_FOR_FILE="$TOTAL_COMM_FOR_FILE+$e"

	    COUNT=$((COUNT+1))
    done < $SUMMARY_FILE
    IFS=$ORIG_IFS

    AVG_CONF=`echo "scale=10; ($TOTAL_CONF_FOR_FILE)/$COUNT" | bc`
    AVG_COMM=`echo "scale=10; ($TOTAL_COMM_FOR_FILE)/$COUNT" | bc`

    CONF_SQUARED_ERROR=0 # sum of all (conf-mean)^2
    COMM_SQUARED_ERROR=0 # sum of all (comm-mean)^2
    if $ComputeStandardDev ; then
	    while IFS=, read a b c d e f g; do
	        CONF_SQUARED_ERROR="$CONF_SQUARED_ERROR+($c-$AVG_CONF)^2"
	        COMM_SQUARED_ERROR="$COMM_SQUARED_ERROR+($e-$AVG_COMM)^2"
	    done < $SUMMARY_FILE
	    CONF_STDEV=`echo "scale=10; sqrt(($CONF_SQUARED_ERROR)/$COUNT)" | bc`
	    COMM_STDEV=`echo "scale=10; sqrt(($COMM_SQUARED_ERROR)/$COUNT)" | bc`
	    ENTRY="$SCENARIO_NAME,$AVG_CONF,$AVG_COMM,$CONF_STDEV,$COMM_STDEV"
    else 
	    ENTRY="$SCENARIO_NAME,$AVG_CONF,$AVG_COMM"
    fi
    echo $ENTRY >> $SUMMARY_ALL;
}

STARTTIME=$SECONDS
STARTDATE=$(date)
echo "Started at $STARTDATE"

SUMMARY_DIR=./logs/current/summaries
if $ComputeStandardDev ; then
    SUMMARY_ALL=$SUMMARY_DIR/AllConfCommStdDev.csv
else
    SUMMARY_ALL=$SUMMARY_DIR/AllConfComm.csv
fi

if [ -f $SUMMARY_ALL ]; then
    echo "Summary CSV exists. Exiting..."
    exit
fi

if [ ! -d $SUMMARY_DIR ]; then
    mkdir $SUMMARY_DIR
fi

if $ComputeStandardDev ; then
    HEADER="Scenario,AvgCumulativeConf,AvgCumulativeComm,ConfStdDev,CommStdDev"
else
    HEADER="Scenario,AvgCumulativeConf,AvgCumulativeComm"
fi
echo $HEADER > $SUMMARY_ALL


DIRS=`find ./logs/current -mindepth 1 -maxdepth 1 -type d -name "scenario*"`
for BASELOGDIR in $DIRS
do
    echo "Summarising $BASELOGDIR"
    SCENARIO_NAME=`basename $BASELOGDIR`

    generateSummaries
    generateAverageFile

    echo "Started at $STARTDATE"
    echo "Finished at "$(date)
done

echo "Done in "$(($SECONDS-$STARTTIME))" seconds"

