#!/bin/sh
STARTTIME=$SECONDS
COUNT=0
PARAM_FILE="ExperimentParams.properties"
BASELOGDIR="./logs/current"

PreInstantiationBidCoefficient=`seq 0.0 0.2 3.0`
OverstayBidCoefficient=`seq 0.0 0.2 3.0`

echo "Looping through PreInst: $PreInstantiationBidCoefficient"
echo "Looping through OverStay: $OverstayBidCoefficient"

function writeParamFile {
    echo "# Comments look like this" > $PARAM_FILE
    echo "NumOfParams=2" >> $PARAM_FILE
    echo >> $PARAM_FILE
    echo "ParamKey_1=PreInstantiationBidCoefficient" >> $PARAM_FILE
    echo "ParamValue_1=$1" >> $PARAM_FILE
    echo "ParamKey_2=OverstayBidCoefficient" >> $PARAM_FILE
    echo "ParamValue_2=$2" >> $PARAM_FILE
}

touch $BASELOGDIR"/PreInst-OverStay"

for PreInst in $PreInstantiationBidCoefficient
do
    for OverStay in $OverstayBidCoefficient
    do
	writeParamFile $PreInst $OverStay

	export LOGDIR="$BASELOGDIR/$PreInst-$OverStay"
	mkdir $LOGDIR
	echo "Made $LOGDIR"
	./exp.sh --paramfile $PARAM_FILE
	COUNT=$((COUNT+1))
	echo "Count is $COUNT at "$(($SECONDS-$STARTTIME))" seconds"
    done
done

echo
echo "$COUNT experiments done in "$(($SECONDS-$STARTTIME))" seconds"

