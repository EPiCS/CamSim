#!/bin/sh
STARTTIME=$SECONDS
COUNT=0
PARAM_FILE="ExperimentParams.properties"
BASELOGDIR="./logs/current"

ScenarioFiles=`ls scenarios/scenario[456]*hist*`
PreInstantiationBidCoefficient=`seq 0.0 2.0 20.0`
OverstayBidCoefficient=`seq 0.0 2.0 20.0`
CommTypes=`seq 0 1 2` # Broadcast, smooth, step

date

echo "Looping through ScenarioFiles: $ScenarioFiles"
echo "Looping through PreInst: $PreInstantiationBidCoefficient"
echo "Looping through OverStay: $OverstayBidCoefficient"

function writeParamFile {
    echo "# Comments look like this" > $PARAM_FILE
    echo >> $PARAM_FILE
    echo "PreInstantiationBidCoefficient=$1" >> $PARAM_FILE
    echo "OverstayBidCoefficient=$2" >> $PARAM_FILE
}

function getCommTypeName {
    case $1 in
	    0)
	        echo "broadcast";;
	    1)
	        echo "smooth";;
	    2)
	        echo "step";;
    esac
}

function checkFileExists {
    if [ ! -f "$1" ]; then
	    echo "Error: File $1 does not exist"
	    exit
    fi
}

# To make the format of the files clear
touch $BASELOGDIR"/PreInst-OverStay"

for ScenarioFile in $ScenarioFiles
do
    for CommType in $CommTypes
    do
	    for PreInst in $PreInstantiationBidCoefficient
	    do
	        for OverStay in $OverstayBidCoefficient
	        do
		        writeParamFile $PreInst $OverStay
		        checkFileExists $ScenarioFile
		        
		        CommTypeName=$(getCommTypeName $CommType)

		        export ScenarioName=$(basename $ScenarioFile .xml)
		        export LOGDIR="$BASELOGDIR/$ScenarioName-$CommTypeName/$PreInst-$OverStay"
		        mkdir -p $LOGDIR
		        echo "Made $LOGDIR"

		        # Apply param file only to hist scenarios
		        if [[ "$ScenarioName" == *hist* ]]
		        then
		            PARAM_FILE_ARG="--paramfile $PARAM_FILE"
		        fi

		        ./exp.sh $PARAM_FILE_ARG --comm $CommType $ScenarioFile 
		        COUNT=$((COUNT+1))
		        echo "Count is $COUNT at "$(($SECONDS-$STARTTIME))" seconds"
	        done
	    done
    done
done

echo
echo "$COUNT experiments done in "$(($SECONDS-$STARTTIME))" seconds"

