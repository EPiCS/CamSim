#!/bin/sh
STARTTIME=$SECONDS

BASELOGDIR="./logs/current"
SUMMARY_AVG=$BASELOGDIR/SummaryAvg.txt

function generateSummary {
    COUNT=0

    LOGDIR="$BASELOGDIR"
    if [ ! -d "$LOGDIR" ]; then
	echo "No logdir"
	exit
    fi
    
    cat $LOGDIR/*summary.txt | grep -v TIME > $BASELOGDIR"/SummaryAll.txt"

    COUNT=$((COUNT+1))

    echo
    echo "$COUNT experiments done in "$(($SECONDS-$STARTTIME))" seconds"
}

function generateAverageFile {
    echo "AvgCumulativeConf,AvgCumulativeComm" > $SUMMARY_AVG

    TOTAL_CONF_FOR_FILE=0
    TOTAL_COMM_FOR_FILE=0
    COUNT=0

    FILENAME=$BASELOGDIR"/SummaryAll.txt"
    echo "Running on file: $FILENAME"
    
    ORIG_IFS=$IFS
    IFS=','
    while IFS=, read a b c d e f g; do 
	TOTAL_CONF_FOR_FILE=`calc.sh $TOTAL_CONF_FOR_FILE+$c`
	TOTAL_COMM_FOR_FILE=`calc.sh $TOTAL_COMM_FOR_FILE+$e`
	COUNT=$((COUNT+1))
    done < $FILENAME
    IFS=$ORIG_IFS

    AVG_CONF=`calc.sh $TOTAL_CONF_FOR_FILE/$COUNT`
    AVG_COMM=`calc.sh $TOTAL_COMM_FOR_FILE/$COUNT`
    echo "$AVG_CONF,$AVG_COMM" >> $SUMMARY_AVG
}

echo "Generating summary"
generateSummary
echo "Generated summary"
echo "Generating avg file"
generateAverageFile
echo "Generated avg file"

echo Done

