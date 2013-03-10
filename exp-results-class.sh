#!/bin/sh
PreInstantiationBidCoefficient=`seq 0.0 20.0 20.0`
OverstayBidCoefficient=`seq 0.0 0.0`
ComputeStandardDev=true # true or false to compute standard deviations in the avg. file

Hists="true false"
Classes="true false"

function generateSummaries {
    COUNT=0
    SUMMARY_STARTTIME=$SECONDS
    for PreInst in $PreInstantiationBidCoefficient
    do
	    for OverStay in $OverstayBidCoefficient
	    do
            for Hist in $Hists
            do
                for Class in $Classes
                do
                    if [[ "$Hist" == "true" ]]; then
                        HistString="-Hist"
                    else
                        HistString=""
                    fi
                    if [[ "$Class" == "true" ]]; then
                        ClassString="-Class"
                    else
                        ClassString=""
                    fi

	                LOGDIR="$BASELOGDIR/$PreInst-$OverStay$HistString$ClassString"
	                if [ ! -d "$LOGDIR" ]; then
		                echo "No logdir"
		                exit
	                fi
	                
	                cat $LOGDIR/*summary.txt | grep -v TIME > $SUMMARY_DIR"/$PreInst-$OverStay$HistString$ClassString-summary.txt"

	                COUNT=$((COUNT+1))
                done
            done
	    done
    done

    echo
    echo "$COUNT experiments summarised in "$(($SECONDS-$SUMMARY_STARTTIME))" seconds"
}

function generateAverageFile {
    if $ComputeStandardDev ; then
	    HEADER="Scenario,PreInst,OverStay,Hist,Class,AvgCumulativeConf,AvgCumulativeComm,ConfStdDev,CommStdDev"
    else
	    HEADER="Scenario,PreInst,OverStay,Hist,Class,AvgCumulativeConf,AvgCumulativeComm"
    fi
    echo $HEADER > $SUMMARY_ALL

    for PreInst in $PreInstantiationBidCoefficient
    do
	    for OverStay in $OverstayBidCoefficient
	    do
            for Hist in $Hists
            do
                for Class in $Classes
                do
                    if [[ "$Hist" == "true" ]]; then
                        HistString="-Hist"
                    else
                        HistString=""
                    fi
                    if [[ "$Class" == "true" ]]; then
                        ClassString="-Class"
                    else
                        ClassString=""
                    fi


	                TOTAL_CONF_FOR_FILE=0
	                TOTAL_COMM_FOR_FILE=0
	                COUNT=0

	                FILENAME=$BASELOGDIR"/Summary/$PreInst-$OverStay$HistString$ClassString-summary.txt"
	                echo "Running on file: $FILENAME"
	                
	                ORIG_IFS=$IFS
	                IFS=','
	                while IFS=, read a b c d e f g; do 
		                TOTAL_CONF_FOR_FILE="$TOTAL_CONF_FOR_FILE+$c"
		                TOTAL_COMM_FOR_FILE="$TOTAL_COMM_FOR_FILE+$e"

		                COUNT=$((COUNT+1))
	                done < $FILENAME
	                IFS=$ORIG_IFS

	                AVG_CONF=`echo "scale=10; ($TOTAL_CONF_FOR_FILE)/$COUNT" | bc`
	                AVG_COMM=`echo "scale=10; ($TOTAL_COMM_FOR_FILE)/$COUNT" | bc`

	                CONF_SQUARED_ERROR=0 # sum of all (conf-mean)^2
	                COMM_SQUARED_ERROR=0 # sum of all (comm-mean)^2
	                if $ComputeStandardDev ; then
		                while IFS=, read a b c d e f g; do
		                    CONF_SQUARED_ERROR="$CONF_SQUARED_ERROR+($c-$AVG_CONF)^2"
		                    COMM_SQUARED_ERROR="$COMM_SQUARED_ERROR+($e-$AVG_COMM)^2"
		                done < $FILENAME
		                CONF_STDEV=`echo "scale=10; sqrt(($CONF_SQUARED_ERROR)/$COUNT)" | bc`
		                COMM_STDEV=`echo "scale=10; sqrt(($COMM_SQUARED_ERROR)/$COUNT)" | bc`
		                ENTRY="$SCENARIO_NAME,$PreInst,$OverStay,$Hist,$Class,$AVG_CONF,$AVG_COMM,$CONF_STDEV,$COMM_STDEV"
	                else 
		                ENTRY="$SCENARIO_NAME,$PreInst,$OverStay,$Hist,$Class,$AVG_CONF,$AVG_COMM"
	                fi
	                echo $ENTRY >> $SUMMARY_ALL;
                done
            done
	    done
    done
}

STARTTIME=$SECONDS
STARTDATE=$(date)
echo "Started at $STARTDATE"

BASEDIR="./logs/histclass"
ALL_SUMMARIES="$BASEDIR/summaries"
if [ ! -d $ALL_SUMMARIES ]; then
    mkdir $ALL_SUMMARIES
fi

DIRS=`find $BASEDIR -mindepth 1 -maxdepth 1 -type d -name "scenario*"`
for BASELOGDIR in $DIRS
do
    echo "Summarising $BASELOGDIR"
    SCENARIO_NAME=`basename $BASELOGDIR`
    SUMMARY_DIR=$BASELOGDIR/Summary
    if $ComputeStandardDev ; then
	    SUMMARY_ALL=$SUMMARY_DIR/AllConfCommStdDev.csv
    else
	    SUMMARY_ALL=$SUMMARY_DIR/AllConfComm.csv
    fi

    if [ -f $SUMMARY_ALL ]; then
	    echo "Summary CSV exists. Skipping $SUMMARY_ALL"
	    continue
    fi

    if [ ! -d $SUMMARY_DIR ]; then
	    echo "Creating $SUMMARY_DIR"
	    mkdir $SUMMARY_DIR
    fi

    generateSummaries

    generateAverageFile

    cp $SUMMARY_ALL "$ALL_SUMMARIES/$SCENARIO_NAME.csv"

    echo "Started at $STARTDATE"
    echo "Finished at "$(date)
done


if [ ! -z $HEADER ]; then
    echo $HEADER >> $ALL_SUMMARIES/AllResults.csv
fi
SUMFILES=`find $ALL_SUMMARIES -mindepth 1 -maxdepth 1 -type f -name "*.csv"`
for SUMFILE in $SUMFILES
do
    cat $SUMFILE | grep -v Scenario >> $ALL_SUMMARIES/AllResults.csv
done

echo "Done in "$((SECONDS-SUMMARY_STARTTIME))" seconds"

