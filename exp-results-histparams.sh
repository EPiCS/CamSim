#!/bin/sh
STARTTIME=$SECONDS

ScenarioFiles=`ls scenarios/*`
PreInstantiationBidCoefficient=`seq 0.0 2.0 10.0`
OverstayBidCoefficient=`seq 0.0 2.0 10.0`
CommTypes=`seq 0 1 2` # Broadcast, smooth, step

function generateSummaries {
    COUNT=0

    for PreInst in $PreInstantiationBidCoefficient
    do
	for OverStay in $OverstayBidCoefficient
	do
	    LOGDIR="$BASELOGDIR/$PreInst-$OverStay"
	    if [ ! -d "$LOGDIR" ]; then
		echo "No logdir"
		exit
	    fi
	    
	    cat $LOGDIR/*summary.txt | grep -v TIME > $SUMMARY_DIR"/$PreInst-$OverStay-summary.txt"

	    COUNT=$((COUNT+1))
	done
    done

    echo
    echo "$COUNT experiments done in "$(($SECONDS-$STARTTIME))" seconds"
}

function generateAverageFile {
    echo "PreInst,OverStay,AvgCumulativeConf,AvgCumulativeComm" > $SUMMARY_ALL

    for PreInst in $PreInstantiationBidCoefficient
    do
	for OverStay in $OverstayBidCoefficient
	do
	    TOTAL_CONF_FOR_FILE=0
	    TOTAL_COMM_FOR_FILE=0
	    COUNT=0

	    FILENAME=$BASELOGDIR"/Summary/$PreInst-$OverStay-summary.txt"
	    echo "Running on file: $FILENAME"
	    
	    ORIG_IFS=$IFS
	    IFS=','
	    while IFS=, read a b c d e f g; do 
		TOTAL_CONF_FOR_FILE=`echo "scale=10; $TOTAL_CONF_FOR_FILE+$c" | bc`
		TOTAL_COMM_FOR_FILE=`echo "scale=10; $TOTAL_COMM_FOR_FILE+$e" | bc`

		COUNT=$((COUNT+1))
	    done < $FILENAME
	    IFS=$ORIG_IFS

	    AVG_CONF=`echo "scale=10; $TOTAL_CONF_FOR_FILE/$COUNT" | bc`
	    AVG_COMM=`echo "scale=10; $TOTAL_COMM_FOR_FILE/$COUNT" | bc`

	    echo "$PreInst,$OverStay,$AVG_CONF,$AVG_COMM" >> $SUMMARY_ALL
	done
    done
}

STARTDATE=$(date)

DIRS=`find ./logs/current -mindepth 1 -maxdepth 1 -type d`
for BASELOGDIR in $DIRS
do
    echo "Summarising $BASELOGDIR"
    SUMMARY_DIR=$BASELOGDIR/Summary
    SUMMARY_ALL=$SUMMARY_DIR/AllConfComm.csv

    if [ -d $SUMMARY_DIR ]; then
	echo "Summary exists. Skipping $BASELOGDIR"
	continue
    fi

    mkdir $SUMMARY_DIR

    generateSummaries
    generateAverageFile

    echo "Started at $STARTDATE"
    echo "Finished at "$(date)
done

echo Done

