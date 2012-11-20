#!/bin/sh
STARTTIME=$SECONDS

BASELOGDIR="./logs/current"

PreInstantiationBidCoefficient=`seq 0.0 0.2 3.0`
OverstayBidCoefficient=`seq 0.0 0.2 3.0`

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
	    
	    cat $LOGDIR/*summary.txt | grep -v TIME > $BASELOGDIR"/Summary/$PreInst-$OverStay-summary.txt"

	    COUNT=$((COUNT+1))
	done
    done

    echo
    echo "$COUNT experiments done in "$(($SECONDS-$STARTTIME))" seconds"
}

function generateAverageFile {
    SUMMARY_ALL=$BASELOGDIR/Summary/AllConfComm.csv
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
		TOTAL_CONF_FOR_FILE=`calc.sh $TOTAL_CONF_FOR_FILE+$c`
		TOTAL_COMM_FOR_FILE=`calc.sh $TOTAL_COMM_FOR_FILE+$e`
		COUNT=$((COUNT+1))
	    done < $FILENAME
	    IFS=$ORIG_IFS

	    AVG_CONF=`calc.sh $TOTAL_CONF_FOR_FILE/$COUNT`
	    AVG_COMM=`calc.sh $TOTAL_COMM_FOR_FILE/$COUNT`
	    echo "$PreInst,$OverStay,$AVG_CONF,$AVG_COMM" >> $SUMMARY_ALL
	done
    done
}

generateAverageFile

echo Done

