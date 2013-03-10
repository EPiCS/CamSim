#!/bin/sh
STARTTIME=$SECONDS
COUNT=0
PARAM_FILE="ExperimentParams.properties"
BASELOGDIR="./logs/histclass"

ScenarioFiles=`ls scenarios/scenario[789]*`
PreInstantiationBidCoefficient=`seq 0.0 20.0 20.0`
OverstayBidCoefficient=`seq 0.0 0.0`
CommTypes=`seq 0 1 2` # Broadcast, smooth, step

Hists="true false"
Classes="true false"

date

echo "Looping through ScenarioFiles: $ScenarioFiles"
echo "Looping through PreInst: $PreInstantiationBidCoefficient"
echo "Looping through OverStay: $OverstayBidCoefficient"
echo "Looping through Hists: $Hists"
echo "Looping through Classes: $Classes"

# Args:
# PreInst, Overstay, Hist, Class
function writeParamFile {
    echo "# Comments look like this" > $PARAM_FILE
    echo >> $PARAM_FILE
    echo "PreInstantiationBidCoefficient=$1" >> $PARAM_FILE
    echo "OverstayBidCoefficient=$2" >> $PARAM_FILE

    echo "HistEnabled=$3" >> $PARAM_FILE
    echo "ClassificationEnabled=$4" >> $PARAM_FILE
    echo "HistPerCategoryEnabled=true" >> $PARAM_FILE

    echo "ObjectCategories=2" >> $PARAM_FILE
    echo "DebugHist=false" >> $PARAM_FILE
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

if [ ! -d $BASELOGDIR ]; then
    mkdir $BASELOGDIR
fi

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
                for Hist in $Hists
                do
                    for Class in $Classes
                    do
		                checkFileExists $ScenarioFile
		                
		                CommTypeName=$(getCommTypeName $CommType)
                        
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

		                export ScenarioName=$(basename $ScenarioFile .xml)
		                export LOGDIR="$BASELOGDIR/$ScenarioName-$CommTypeName/$PreInst-$OverStay$HistString$ClassString"
		                mkdir -p $LOGDIR
		                echo "Made $LOGDIR"

		                # Apply param file only to hist scenarios
		                if [[ "$ScenarioName" == *hist* ]]
		                then
                            writeParamFile $PreInst $OverStay $Hist $Class
		                    PARAM_FILE_ARG="--paramfile $PARAM_FILE"
                        else
                            PARAM_FILE_ARG=""
		                fi

		                ./exp.sh $PARAM_FILE_ARG --comm $CommType $ScenarioFile 
		                COUNT=$((COUNT+1))
		                echo "Count is $COUNT at "$(($SECONDS-$STARTTIME))" seconds"
                    done
                done
	        done
	    done
    done
done

echo
echo "$COUNT experiments done in "$(($SECONDS-$STARTTIME))" seconds"

